package app.veshinantam.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.veshinantam.data.ScheduleRepository
import app.veshinantam.data.local.TodayTaskRow
import app.veshinantam.domain.model.TaskType
import app.veshinantam.domain.model.MaterialType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

enum class TodaySection { NEW_LEARNING, CHAZARAH_TODAY, OVERDUE_LEARNING, OVERDUE_CHAZARAH, COMPLETED_TODAY }

data class TodayTaskUi(
    val id: String,
    val labelEnglish: String,
    val labelHebrew: String,
    val plannedDate: LocalDate,
    val section: TodaySection,
    val isCompleted: Boolean,
    val materialType: MaterialType = MaterialType.CUSTOM_UNIT,
    val presetId: String? = null,
)

data class TodayScheduleUi(
    val id: String,
    val nameEnglish: String,
    val nameHebrew: String,
    val tasks: List<TodayTaskUi>,
) {
    val completedCount: Int get() = tasks.count { it.isCompleted }
}

sealed interface TodayUiState {
    data object Loading : TodayUiState
    data class Ready(val today: LocalDate, val schedules: List<TodayScheduleUi>) : TodayUiState
    data class Error(val today: LocalDate) : TodayUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val repository: ScheduleRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val zoneProvider: () -> ZoneId = ZoneId::systemDefault,
) : ViewModel() {
    private val currentDate = MutableStateFlow(localToday())

    init {
        viewModelScope.launch {
            repository.rollOverMissedLearning(currentDate.value)
            while (true) {
                val zone = zoneProvider()
                val now = clock.instant()
                val nextMidnight = now.atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant()
                delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1_000L))
                refreshDate()
            }
        }
    }

    val uiState = currentDate
        .flatMapLatest { today ->
            repository.observeTodayTasks(today).map { rows ->
                TodayUiState.Ready(today, mapTodayRows(rows, today)) as TodayUiState
            }
        }
        .catch { emit(TodayUiState.Error(currentDate.value)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState.Loading)

    fun refreshDate() {
        val today = localToday()
        viewModelScope.launch {
            repository.rollOverMissedLearning(today)
            currentDate.value = today
        }
    }

    fun setCompleted(taskId: String, completed: Boolean) {
        viewModelScope.launch { repository.setCompleted(taskId, completed) }
    }

    private fun localToday(): LocalDate = LocalDate.now(clock.withZone(zoneProvider()))

    class Factory(private val repository: ScheduleRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = TodayViewModel(repository) as T
    }
}

internal fun mapTodayRows(rows: List<TodayTaskRow>, today: LocalDate): List<TodayScheduleUi> =
        rows.groupBy { it.scheduleId }
            .values
            .sortedBy { rows -> rows.first().scheduleCreatedAt }
            .map { rows ->
                val first = rows.first()
                TodayScheduleUi(
                    id = first.scheduleId,
                    nameEnglish = first.scheduleNameEnglish,
                    nameHebrew = first.scheduleNameHebrew,
                    tasks = rows.map { row ->
                        TodayTaskUi(
                            id = row.taskId,
                            labelEnglish = row.labelEnglish,
                            labelHebrew = row.labelHebrew,
                            plannedDate = row.plannedDate,
                            section = when {
                                row.plannedDate == today && row.type == TaskType.LEARNING -> TodaySection.NEW_LEARNING
                                row.plannedDate == today -> TodaySection.CHAZARAH_TODAY
                                row.completedAt != null -> TodaySection.COMPLETED_TODAY
                                row.type == TaskType.LEARNING -> TodaySection.OVERDUE_LEARNING
                                else -> TodaySection.OVERDUE_CHAZARAH
                            },
                            isCompleted = row.completedAt != null,
                            materialType = row.materialType,
                            presetId = row.presetId,
                        )
                    }.sortedWith(compareBy<TodayTaskUi>({ it.section.ordinal }, { it.plannedDate }, { it.id })),
                )
            }

internal fun sortTodayTasks(
    tasks: List<TodayTaskUi>,
    order: TodaySortOrder,
    preferHebrewReference: Boolean = false,
): List<TodayTaskUi> {
    val reference = Comparator<TodayTaskUi> { left, right ->
        compareReferenceLabels(left, right, preferHebrewReference).takeIf { it != 0 }
            ?: left.id.compareTo(right.id)
    }
    return when (order) {
        TodaySortOrder.SCHEDULED_FIRST -> tasks.sortedWith(
            compareBy<TodayTaskUi>({ it.plannedDate }, { it.labelEnglish }, { it.labelHebrew }, { it.id }),
        )
        TodaySortOrder.NEWEST_DUE_FIRST -> tasks.sortedWith(
            compareByDescending<TodayTaskUi> { it.plannedDate }
                .thenBy { it.labelEnglish }
                .thenBy { it.labelHebrew }
                .thenBy { it.id },
        )
        TodaySortOrder.REFERENCE_ASCENDING -> tasks.sortedWith(reference)
        TodaySortOrder.REFERENCE_DESCENDING -> tasks.sortedWith(reference.reversed())
    }
}

private fun compareReferenceLabels(
    left: TodayTaskUi,
    right: TodayTaskUi,
    preferHebrew: Boolean,
): Int {
    val leftDisplayed = if (preferHebrew) left.labelHebrew.ifBlank { left.labelEnglish } else left.labelEnglish.ifBlank { left.labelHebrew }
    val rightDisplayed = if (preferHebrew) right.labelHebrew.ifBlank { right.labelEnglish } else right.labelEnglish.ifBlank { right.labelHebrew }
    val titleComparison = referenceTitle(leftDisplayed).compareTo(referenceTitle(rightDisplayed), ignoreCase = true)
    if (titleComparison != 0) return titleComparison

    val leftNumbers = NUMBER.findAll(left.labelEnglish).map { it.value.toInt() }.toList()
    val rightNumbers = NUMBER.findAll(right.labelEnglish).map { it.value.toInt() }.toList()
    compareIntegerLists(leftNumbers, rightNumbers).takeIf { it != 0 }?.let { return it }

    val leftSides = AMUD_SIDE.findAll(left.labelEnglish).map { if (it.groupValues[1].equals("a", true)) 0 else 1 }.toList()
    val rightSides = AMUD_SIDE.findAll(right.labelEnglish).map { if (it.groupValues[1].equals("a", true)) 0 else 1 }.toList()
    compareIntegerLists(leftSides, rightSides).takeIf { it != 0 }?.let { return it }
    return naturalTextCompare(leftDisplayed, rightDisplayed)
}

private fun referenceTitle(label: String): String {
    val marker = REFERENCE_LOCATION_MARKER.find(label)?.range?.first ?: label.length
    return label.substring(0, marker).trim().trimEnd(',', '–', '-')
}

private fun compareIntegerLists(left: List<Int>, right: List<Int>): Int {
    repeat(minOf(left.size, right.size)) { index ->
        left[index].compareTo(right[index]).takeIf { it != 0 }?.let { return it }
    }
    return left.size.compareTo(right.size)
}

private fun naturalTextCompare(left: String, right: String): Int {
    val leftParts = NATURAL_PART.findAll(left.lowercase()).map { it.value }.toList()
    val rightParts = NATURAL_PART.findAll(right.lowercase()).map { it.value }.toList()
    repeat(minOf(leftParts.size, rightParts.size)) { index ->
        val leftPart = leftParts[index]
        val rightPart = rightParts[index]
        val comparison = if (leftPart.all(Char::isDigit) && rightPart.all(Char::isDigit)) {
            leftPart.toLong().compareTo(rightPart.toLong())
        } else {
            leftPart.compareTo(rightPart)
        }
        if (comparison != 0) return comparison
    }
    return leftParts.size.compareTo(rightParts.size)
}

private val NUMBER = Regex("\\d+")
private val AMUD_SIDE = Regex("\\d+([ab])", RegexOption.IGNORE_CASE)
private val NATURAL_PART = Regex("\\d+|\\D+")
private val REFERENCE_LOCATION_MARKER = Regex(
    "(?i)\\s+(?:daf|page|perek|mishnah|siman|seif|chelek|דף|עמוד|פרק|משנה|סימן|סעיף|חלק)\\s+|\\s+\\d",
)

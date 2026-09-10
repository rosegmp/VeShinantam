package app.veshinantam.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

enum class ScheduleKind { PRESET, CUSTOM }

enum class MaterialType {
    DAF,
    AMUD,
    MISHNAH,
    PEREK,
    PAGE,
    SEIF,
    SIMAN,
    CUSTOM_UNIT,
}

enum class MissedWorkBehavior { SHIFT_FORWARD, KEEP_FIXED_OVERDUE }

enum class ScheduleState { ACTIVE, PAUSED, ARCHIVED }

enum class TaskType { LEARNING, CHAZARAH }

data class BilingualLabel(
    val english: String,
    val hebrew: String,
) {
    init {
        require(english.isNotBlank() || hebrew.isNotBlank()) { "A material label cannot be empty" }
    }
}

data class MaterialUnit(
    val id: String,
    val ordinal: Int,
    val label: BilingualLabel,
    val quantity: Double = 1.0,
)

data class ChazarahPattern(
    val dayOffsets: List<Int> = ChazarahDefaults.BUILT_IN_OFFSETS,
    val repeatsAnnually: Boolean = true,
) {
    init {
        require(dayOffsets.all { it > 0 }) { "Chazarah offsets must be positive" }
        require(dayOffsets.distinct().size == dayOffsets.size) { "Chazarah offsets must be unique" }
    }
}

object ChazarahDefaults {
    val BUILT_IN_OFFSETS = listOf(1, 7, 30, 90)

    fun format(offsets: List<Int>): String = offsets.joinToString(", ")

    fun parse(value: String): List<Int>? {
        val parts = value.split(',')
        val offsets = parts.mapNotNull { it.trim().toIntOrNull() }
        return offsets.takeIf {
            it.isNotEmpty() && it.all { offset -> offset > 0 } && it.distinct().size == it.size && it.size == parts.size
        }
    }
}

data class ScheduleRules(
    val selectedWeekdays: Set<DayOfWeek>,
    val excludedDates: Set<LocalDate> = emptySet(),
) {
    init {
        require(selectedWeekdays.isNotEmpty()) { "At least one weekday must be selected" }
    }

    fun isEligible(date: LocalDate): Boolean =
        date.dayOfWeek in selectedWeekdays && date !in excludedDates
}

data class PlannedTask(
    val stableKey: String,
    val material: MaterialUnit,
    val type: TaskType,
    val plannedDate: LocalDate,
    val originalLearningDate: LocalDate,
    val reviewIdentity: String? = null,
)

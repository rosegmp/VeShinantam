package app.veshinantam.web

import app.veshinantam.shared.LearningSchedule
import app.veshinantam.shared.LearningTask
import app.veshinantam.shared.LearningTaskType
import app.veshinantam.shared.IsoDate
import app.veshinantam.shared.GregorianCalendar
import app.veshinantam.shared.CanonicalDataSet
import app.veshinantam.shared.CanonicalDataValidator
import app.veshinantam.shared.CanonicalMaterialType
import app.veshinantam.shared.CanonicalMissedWorkBehavior
import app.veshinantam.shared.CanonicalPreferences
import app.veshinantam.shared.CanonicalPrimaryCalendar
import app.veshinantam.shared.CanonicalSchedule
import app.veshinantam.shared.CanonicalScheduleKind
import app.veshinantam.shared.CanonicalScheduleState
import app.veshinantam.shared.CanonicalSefarimLanguage
import app.veshinantam.shared.CanonicalTask
import app.veshinantam.shared.CanonicalTaskType
import app.veshinantam.shared.SharedMaterialUnit
import app.veshinantam.shared.SharedScheduleEngine
import app.veshinantam.shared.SharedScheduleRules
import kotlinx.serialization.Serializable

@Serializable
data class StoredSchedule(
    val id: String,
    val name: String,
    val material: String,
    val pace: Int,
    val weekdays: Set<Int> = (0..6).toSet(),
    val chazarahOffsets: List<Int> = listOf(1, 7),
    val active: Boolean = true,
    val archived: Boolean = false,
    val nameHebrew: String = "",
    val presetId: String? = null,
    val startDate: String? = null,
    val targetDate: String? = null,
    val missedWorkBehavior: String = "KEEP_FIXED_OVERDUE",
    val repeatsAnnually: Boolean = false,
    val officialOraysaChazarah: Boolean = false,
    val generationRevision: Int = 1,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val revision: Long = 0,
    val sourceType: String = material,
    val materialType: String = material,
) {
    fun domain() = LearningSchedule(id, name, material, pace, weekdays, chazarahOffsets, active, archived)
}

@Serializable
data class StoredTask(
    val id: String,
    val scheduleId: String,
    val referenceEnglish: String,
    val referenceHebrew: String,
    val dueDate: String,
    val type: String,
    val completed: Boolean = false,
    val stableKey: String = id,
    val materialType: String = "CUSTOM_UNIT",
    val quantity: Double = 1.0,
    val originalLearningDate: String = dueDate,
    val reviewIdentity: String? = null,
    val generationRevision: Int = 1,
    val completedAt: String? = null,
    val completionLocalDate: String? = null,
    val completionZoneId: String? = null,
    val updatedAt: String? = null,
    val revision: Long = 0,
) {
    fun domain() = LearningTask(
        id, scheduleId, referenceEnglish, referenceHebrew, dueDate,
        LearningTaskType.valueOf(type), completed,
    )
}

@Serializable
data class WebAppState(
    val schedules: List<StoredSchedule> = emptyList(),
    val tasks: List<StoredTask> = emptyList(),
    val language: String = "en",
    val sefarimLanguage: String = "BOTH",
    val primaryCalendar: String = "GREGORIAN",
    val defaultChazarahOffsets: List<Int> = listOf(1, 7, 30, 90),
    val todaySortOrder: String = "SCHEDULED_FIRST",
    val preferencesRevision: Long = 0,
) {
    companion object {
        fun sample(today: String) = WebAppState(
            schedules = listOf(
                StoredSchedule("daf-yomi", "Daf Yomi Bavli", "Gemara", 1),
                StoredSchedule("mishnah", "Mishnah Yomis", "Mishnah", 2),
            ),
            tasks = listOf(
                StoredTask("daf-today", "daf-yomi", "Berachos 18", "ברכות דף י״ח", today, "LEARNING"),
                StoredTask("daf-review", "daf-yomi", "Berachos 17", "ברכות דף י״ז", today, "CHAZARAH"),
                StoredTask("mishnah-today", "mishnah", "Peah 2:3–4", "פאה ב׳:ג׳–ד׳", today, "LEARNING"),
                StoredTask("mishnah-review", "mishnah", "Peah 2:1–2", "פאה ב׳:א׳–ב׳", today, "CHAZARAH", true),
            ),
        )
    }
}

data class WebFutureScheduleEdit(
    val startDate: String,
    val dailyQuantity: Int,
    val targetCompletionDate: String? = null,
    val selectedWeekdays: Set<Int>,
    val includeWeekendChazarah: Boolean = false,
)

@Serializable
data class WebBackup(
    val version: Int = 2,
    val state: WebAppState,
    val canonical: CanonicalDataSet? = null,
)

sealed interface BackupImportResult {
    data object None : BackupImportResult
    data object Invalid : BackupImportResult
    data class Ready(val state: WebAppState) : BackupImportResult
}

fun WebBackup.validStateOrNull(): WebAppState? {
    if (version !in 1..2 || state.language !in setOf("en", "he")) return null
    if (state.sefarimLanguage !in CanonicalSefarimLanguage.entries.map { it.name } ||
        state.primaryCalendar !in CanonicalPrimaryCalendar.entries.map { it.name } ||
        state.todaySortOrder !in WebTodaySortOrder.entries.map { it.name } ||
        state.defaultChazarahOffsets.isEmpty() || state.defaultChazarahOffsets.any { it <= 0 }
    ) return null
    if (version >= 2 && (canonical == null || CanonicalDataValidator.validate(canonical).isNotEmpty())) return null
    if (state.schedules.size > 500 || state.tasks.size > 50_000) return null
    val scheduleIds = state.schedules.map { it.id }
    if (scheduleIds.any { it.isBlank() } || scheduleIds.distinct().size != scheduleIds.size) return null
    if (state.schedules.any { schedule ->
            schedule.name.isBlank() || schedule.material.isBlank() || schedule.pace !in 1..20 ||
                schedule.weekdays.isEmpty() || schedule.weekdays.any { it !in 0..6 } ||
                schedule.chazarahOffsets.any { it !in 1..3650 }
        }
    ) return null
    val taskIds = state.tasks.map { it.id }
    if (taskIds.any { it.isBlank() } || taskIds.distinct().size != taskIds.size) return null
    val knownSchedules = scheduleIds.toSet()
    if (state.tasks.any { task ->
            task.scheduleId !in knownSchedules || task.referenceEnglish.length > 500 || task.referenceHebrew.length > 500 ||
                task.type !in LearningTaskType.entries.map { it.name } || IsoDate.parse(task.dueDate) == null
        }
    ) return null
    if (version >= 2 && canonical?.let { value ->
            value.schedules.map { it.id }.toSet() != scheduleIds.toSet() ||
                value.tasks.map { it.id }.toSet() != taskIds.toSet()
        } == true
    ) return null
    return state
}

fun WebAppState.toCanonical(now: String, today: String): CanonicalDataSet {
    val tasksBySchedule = tasks.groupBy { it.scheduleId }
    return CanonicalDataSet(
        schedules = schedules.map { schedule ->
            val scheduleTasks = tasksBySchedule[schedule.id].orEmpty()
            CanonicalSchedule(
                id = schedule.id,
                nameEnglish = schedule.name,
                nameHebrew = schedule.nameHebrew,
                kind = if (schedule.presetId == null) CanonicalScheduleKind.CUSTOM else CanonicalScheduleKind.PRESET,
                sourceType = schedule.sourceType,
                materialType = canonicalMaterialType(schedule.materialType),
                presetId = schedule.presetId,
                startDate = schedule.startDate ?: scheduleTasks.minOfOrNull { it.dueDate } ?: today,
                targetDate = schedule.targetDate ?: scheduleTasks.filter { it.type == "LEARNING" }.maxOfOrNull { it.dueDate },
                dailyQuantity = schedule.pace,
                selectedWeekdays = schedule.weekdays,
                chazarahDayOffsets = schedule.chazarahOffsets,
                repeatsAnnually = schedule.repeatsAnnually,
                officialOraysaChazarah = schedule.officialOraysaChazarah,
                missedWorkBehavior = runCatching { CanonicalMissedWorkBehavior.valueOf(schedule.missedWorkBehavior) }
                    .getOrDefault(CanonicalMissedWorkBehavior.KEEP_FIXED_OVERDUE),
                state = when {
                    schedule.archived -> CanonicalScheduleState.ARCHIVED
                    schedule.active -> CanonicalScheduleState.ACTIVE
                    else -> CanonicalScheduleState.PAUSED
                },
                generationRevision = schedule.generationRevision,
                createdAt = schedule.createdAt ?: now,
                updatedAt = schedule.updatedAt ?: now,
                revision = schedule.revision,
            )
        },
        tasks = tasks.map { task ->
            val completionDate = task.completionLocalDate ?: task.dueDate.takeIf { task.completed }
            CanonicalTask(
                id = task.id,
                stableKey = task.stableKey,
                scheduleId = task.scheduleId,
                type = runCatching { CanonicalTaskType.valueOf(task.type) }.getOrDefault(CanonicalTaskType.LEARNING),
                labelEnglish = task.referenceEnglish,
                labelHebrew = task.referenceHebrew,
                materialType = runCatching { CanonicalMaterialType.valueOf(task.materialType) }
                    .getOrDefault(CanonicalMaterialType.CUSTOM_UNIT),
                quantity = task.quantity,
                plannedDate = task.dueDate,
                originalLearningDate = task.originalLearningDate,
                reviewIdentity = task.reviewIdentity,
                generationRevision = task.generationRevision,
                completedAt = task.completedAt ?: now.takeIf { task.completed },
                completionLocalDate = completionDate,
                completionZoneId = task.completionZoneId ?: "browser-local".takeIf { task.completed },
                updatedAt = task.updatedAt ?: now,
                revision = task.revision,
            )
        },
        preferences = CanonicalPreferences(
            appLanguage = language,
            sefarimLanguage = runCatching { CanonicalSefarimLanguage.valueOf(sefarimLanguage) }
                .getOrDefault(CanonicalSefarimLanguage.BOTH),
            primaryCalendar = runCatching { CanonicalPrimaryCalendar.valueOf(primaryCalendar) }
                .getOrDefault(CanonicalPrimaryCalendar.GREGORIAN),
            defaultChazarahOffsets = defaultChazarahOffsets,
            todaySortOrder = todaySortOrder,
            updatedAt = now,
            revision = preferencesRevision,
        ),
    )
}

enum class WebTodaySection { NEW_LEARNING, CHAZARAH_TODAY, OVERDUE_LEARNING, OVERDUE_CHAZARAH, COMPLETED_TODAY }

enum class WebTodaySortOrder { SCHEDULED_FIRST, NEWEST_DUE_FIRST, REFERENCE_ASCENDING, REFERENCE_DESCENDING }

enum class WebCalendarDayStatus { NONE, INCOMPLETE, PARTIAL, COMPLETE }

data class WebCalendarDaySummary(
    val date: String,
    val completedCount: Int,
    val taskCount: Int,
    val status: WebCalendarDayStatus,
)

@Serializable
data class WebCalendarPeriod(val startDate: String, val endDate: String, val title: String)

data class WebCalendarCell(val isoDate: String?, val gregorianDay: Int?)

data class WebTodayTask(val task: StoredTask, val section: WebTodaySection)

fun todayTasks(tasks: List<StoredTask>, today: String, sortOrder: String, preferHebrew: Boolean): List<WebTodayTask> {
    val order = runCatching { WebTodaySortOrder.valueOf(sortOrder) }.getOrDefault(WebTodaySortOrder.SCHEDULED_FIRST)
    return tasks.asSequence()
        .filter { task ->
            task.dueDate <= today &&
                (task.dueDate == today || !task.completed || task.completionLocalDate == today)
        }
        .map { task ->
            WebTodayTask(
                task = task,
                section = when {
                    task.dueDate == today && task.type == LearningTaskType.LEARNING.name -> WebTodaySection.NEW_LEARNING
                    task.dueDate == today -> WebTodaySection.CHAZARAH_TODAY
                    task.completed -> WebTodaySection.COMPLETED_TODAY
                    task.type == LearningTaskType.LEARNING.name -> WebTodaySection.OVERDUE_LEARNING
                    else -> WebTodaySection.OVERDUE_CHAZARAH
                },
            )
        }
        .sortedWith(compareBy<WebTodayTask> { it.section.ordinal }.thenComparator { left, right ->
            compareTodayTasks(left.task, right.task, order, preferHebrew)
        })
        .toList()
}

private fun compareTodayTasks(left: StoredTask, right: StoredTask, order: WebTodaySortOrder, preferHebrew: Boolean): Int {
    val result = when (order) {
        WebTodaySortOrder.SCHEDULED_FIRST -> left.dueDate.compareTo(right.dueDate)
        WebTodaySortOrder.NEWEST_DUE_FIRST -> right.dueDate.compareTo(left.dueDate)
        WebTodaySortOrder.REFERENCE_ASCENDING,
        WebTodaySortOrder.REFERENCE_DESCENDING,
        -> compareReferenceLabels(left, right, preferHebrew)
    }
    val directed = if (order == WebTodaySortOrder.REFERENCE_DESCENDING) -result else result
    return directed.takeIf { it != 0 } ?: left.id.compareTo(right.id)
}

private fun displayedReference(task: StoredTask, preferHebrew: Boolean): String =
    if (preferHebrew) task.referenceHebrew.ifBlank { task.referenceEnglish }
    else task.referenceEnglish.ifBlank { task.referenceHebrew }

private fun compareReferenceLabels(left: StoredTask, right: StoredTask, preferHebrew: Boolean): Int {
    val leftDisplayed = displayedReference(left, preferHebrew)
    val rightDisplayed = displayedReference(right, preferHebrew)
    referenceTitle(leftDisplayed).compareTo(referenceTitle(rightDisplayed), ignoreCase = true).takeIf { it != 0 }?.let { return it }
    compareIntegerLists(NUMBER.findAll(left.referenceEnglish).map { it.value.toInt() }.toList(), NUMBER.findAll(right.referenceEnglish).map { it.value.toInt() }.toList())
        .takeIf { it != 0 }?.let { return it }
    compareIntegerLists(AMUD_SIDE.findAll(left.referenceEnglish).map { if (it.groupValues[1].equals("a", true)) 0 else 1 }.toList(), AMUD_SIDE.findAll(right.referenceEnglish).map { if (it.groupValues[1].equals("a", true)) 0 else 1 }.toList())
        .takeIf { it != 0 }?.let { return it }
    return naturalTextCompare(leftDisplayed, rightDisplayed)
}

private fun referenceTitle(label: String): String {
    val marker = REFERENCE_LOCATION_MARKER.find(label)?.range?.first ?: label.length
    return label.substring(0, marker).trim().trimEnd(',', '–', '-')
}

private fun compareIntegerLists(left: List<Int>, right: List<Int>): Int {
    repeat(minOf(left.size, right.size)) { index -> left[index].compareTo(right[index]).takeIf { it != 0 }?.let { return it } }
    return left.size.compareTo(right.size)
}

private fun naturalTextCompare(left: String, right: String): Int {
    val leftParts = NATURAL_PART.findAll(left.lowercase()).map { it.value }.toList()
    val rightParts = NATURAL_PART.findAll(right.lowercase()).map { it.value }.toList()
    repeat(minOf(leftParts.size, rightParts.size)) { index ->
        val leftPart = leftParts[index]
        val rightPart = rightParts[index]
        val comparison = if (leftPart.all(Char::isDigit) && rightPart.all(Char::isDigit)) leftPart.toLong().compareTo(rightPart.toLong()) else leftPart.compareTo(rightPart)
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

fun calendarDaySummaries(
    tasks: List<StoredTask>,
    scheduleId: String? = null,
    taskType: String? = null,
): Map<String, WebCalendarDaySummary> = tasks.asSequence()
    .filter { scheduleId == null || it.scheduleId == scheduleId }
    .filter { taskType == null || it.type == taskType }
    .groupBy { it.dueDate }
    .mapValues { (date, dateTasks) ->
        val completed = dateTasks.count { it.completed }
        WebCalendarDaySummary(
            date = date,
            completedCount = completed,
            taskCount = dateTasks.size,
            status = when {
                completed == 0 -> WebCalendarDayStatus.INCOMPLETE
                completed == dateTasks.size -> WebCalendarDayStatus.COMPLETE
                else -> WebCalendarDayStatus.PARTIAL
            },
        )
    }

fun calendarRangeCells(startDate: String, endDate: String): List<WebCalendarCell> {
    val start = requireNotNull(IsoDate.parse(startDate))
    val end = requireNotNull(IsoDate.parse(endDate))
    require(start <= end)
    val cells = MutableList(GregorianCalendar.dayOfWeek(start.year, start.month, start.day)) { WebCalendarCell(null, null) }
    var date = start
    while (date <= end) {
        cells += WebCalendarCell(date.toString(), date.day)
        date = date.plusDays(1)
    }
    while (cells.size % 7 != 0) cells += WebCalendarCell(null, null)
    return cells
}

fun completePastTasks(
    state: WebAppState,
    scheduleId: String,
    taskType: String,
    today: String,
    completedAt: String,
    zoneId: String,
): WebAppState = state.copy(
    tasks = state.tasks.map { task ->
        if (task.scheduleId == scheduleId && task.type == taskType && task.dueDate < today && !task.completed) {
            task.copy(
                completed = true,
                completedAt = completedAt,
                completionLocalDate = today,
                completionZoneId = zoneId,
                updatedAt = completedAt,
            )
        } else {
            task
        }
    },
)

/**
 * Regenerates only unfinished work scheduled for today or later. Completed and historical tasks
 * are immutable, matching Android's future-edit contract.
 */
fun editFutureSchedule(
    state: WebAppState,
    scheduleId: String,
    today: String,
    edit: WebFutureScheduleEdit,
    updatedAt: String,
): WebAppState {
    val todayDate = requireNotNull(IsoDate.parse(today))
    val startDate = requireNotNull(IsoDate.parse(edit.startDate))
    val targetDate = edit.targetCompletionDate?.let { requireNotNull(IsoDate.parse(it)) }
    require(startDate >= todayDate) { "Future start date cannot precede today" }
    require(targetDate == null || targetDate >= startDate) { "Completion date cannot precede the future start date" }
    require(targetDate != null || edit.dailyQuantity > 0) { "Daily quantity must be positive" }
    require(edit.selectedWeekdays.isNotEmpty() && edit.selectedWeekdays.all { it in 0..6 }) {
        "At least one valid weekday must be selected"
    }

    val schedule = requireNotNull(state.schedules.firstOrNull { it.id == scheduleId })
    val scheduleTasks = state.tasks.filter { it.scheduleId == scheduleId }
    val editableLearning = scheduleTasks.filter { task ->
        task.type == LearningTaskType.LEARNING.name && !task.completed && task.dueDate >= today
    }
    if (editableLearning.isEmpty()) return state

    val editableStableKeys = editableLearning.mapTo(mutableSetOf()) { it.stableKey }
    val editableReviewSources = editableLearning.mapTo(mutableSetOf()) {
        listOf(it.originalLearningDate, it.referenceEnglish, it.referenceHebrew)
    }
    val affectedWeeks = editableLearning.mapTo(mutableSetOf()) { task ->
        val learningDate = requireNotNull(IsoDate.parse(task.originalLearningDate))
        learningDate.minusDays(GregorianCalendar.dayOfWeek(learningDate.year, learningDate.month, learningDate.day))
    }
    val reviewsToReplace = scheduleTasks.filter { task ->
        if (task.type != LearningTaskType.CHAZARAH.name || task.completed || task.dueDate < today) {
            false
        } else if (
            task.reviewIdentity?.startsWith("oraysa:weekly") == true ||
            task.reviewIdentity?.startsWith("weekend:weekly") == true
        ) {
            val learningDate = requireNotNull(IsoDate.parse(task.originalLearningDate))
            learningDate.minusDays(GregorianCalendar.dayOfWeek(learningDate.year, learningDate.month, learningDate.day)) in affectedWeeks
        } else {
            editableStableKeys.any { learningKey -> task.stableKey.startsWith("review:$learningKey:") } ||
                listOf(task.originalLearningDate, task.referenceEnglish, task.referenceHebrew) in editableReviewSources
        }
    }

    val revision = schedule.generationRevision + 1
    val rules = SharedScheduleRules(edit.selectedWeekdays)
    val material = editableLearning.mapIndexed { index, task ->
        SharedMaterialUnit(
            id = "$scheduleId-edit-$revision-unit-$index",
            ordinal = index,
            labelEnglish = task.referenceEnglish,
            labelHebrew = task.referenceHebrew,
            quantity = task.quantity,
        )
    }
    val engine = SharedScheduleEngine()
    val learning = targetDate?.let { engine.generateByCompletionDate(material, startDate, it, rules) }
        ?: engine.generateByDailyQuantity(material, startDate, edit.dailyQuantity, rules)
    val additionalReviews = engine.generateChazarah(
        learningTasks = learning,
        dayOffsets = schedule.chazarahOffsets,
        repeatsAnnually = schedule.repeatsAnnually,
        rules = rules,
        annualReviewsThroughYear = startDate.year + if (schedule.repeatsAnnually) 10 else 0,
    )
    val weekendReviews = if (edit.includeWeekendChazarah) {
        if (schedule.presetId == "oraysa") engine.generateOfficialOraysaChazarah(learning, rules)
        else engine.generateWeekendChazarah(learning)
    } else {
        emptyList()
    }
    val replacements = (learning + weekendReviews + additionalReviews)
        .distinctBy { task ->
            listOf(task.type.name, task.originalLearningDate.toString(), task.plannedDate.toString(), task.material.labelEnglish)
        }
        .mapIndexed { index, task ->
            StoredTask(
                id = "$scheduleId-edit-$revision-${task.type.name.lowercase()}-${index + 1}",
                scheduleId = scheduleId,
                referenceEnglish = task.material.labelEnglish,
                referenceHebrew = task.material.labelHebrew,
                dueDate = task.plannedDate.toString(),
                type = task.type.name,
                stableKey = task.stableKey,
                materialType = schedule.materialType,
                quantity = task.material.quantity,
                originalLearningDate = task.originalLearningDate.toString(),
                reviewIdentity = task.reviewIdentity,
                generationRevision = revision,
                updatedAt = updatedAt,
            )
        }
    val removedIds = (editableLearning + reviewsToReplace).mapTo(mutableSetOf()) { it.id }
    val preservedTasks = state.tasks.filterNot { it.id in removedIds }
    val target = (preservedTasks.asSequence()
        .filter { it.scheduleId == scheduleId && it.type == LearningTaskType.LEARNING.name }
        .map { it.dueDate } + learning.asSequence().map { it.plannedDate.toString() })
        .maxOrNull()
    val updatedSchedule = schedule.copy(
        pace = edit.dailyQuantity.coerceAtLeast(1),
        weekdays = edit.selectedWeekdays,
        startDate = edit.startDate,
        targetDate = target,
        officialOraysaChazarah = edit.includeWeekendChazarah,
        generationRevision = revision,
        updatedAt = updatedAt,
    )
    return state.copy(
        schedules = state.schedules.map { if (it.id == scheduleId) updatedSchedule else it },
        tasks = preservedTasks + replacements,
    )
}

private fun canonicalMaterialType(value: String): CanonicalMaterialType = when (value.lowercase()) {
    "gemara", "daf" -> CanonicalMaterialType.DAF
    "amud" -> CanonicalMaterialType.AMUD
    "mishnah", "mishnayos" -> CanonicalMaterialType.MISHNAH
    "perek", "perakim" -> CanonicalMaterialType.PEREK
    "page" -> CanonicalMaterialType.PAGE
    "seif" -> CanonicalMaterialType.SEIF
    "siman", "kitzur" -> CanonicalMaterialType.SIMAN
    else -> CanonicalMaterialType.CUSTOM_UNIT
}

interface BrowserStore {
    fun load(): WebAppState
    fun save(state: WebAppState)
    fun currentLocalDate(): String
    fun hebrewDateLabel(date: String, hebrewUi: Boolean): String
    fun hebrewDayLabel(date: String, hebrewUi: Boolean): String
    fun hebrewCalendarPeriod(date: String, hebrewUi: Boolean): WebCalendarPeriod
    fun currentInstant(): String
    fun currentZoneId(): String
    fun exportBackup(state: WebAppState)
    fun requestBackupImport()
    fun consumeBackupImport(): BackupImportResult
}

@Serializable
data class CloudAccountState(
    val configured: Boolean,
    val email: String? = null,
    val status: String? = null,
    val conflict: Boolean = false,
    val syncing: Boolean = false,
)

interface CloudAccount {
    fun state(): CloudAccountState
    fun signIn(email: String, password: String)
    fun createAccount(email: String, password: String)
    fun sync(state: WebAppState)
    fun useCloudCopy()
    fun replaceCloudCopy(state: WebAppState)
    fun signOut()
}

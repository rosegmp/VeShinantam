package app.veshinantam.web

import app.veshinantam.shared.IsoDate
import app.veshinantam.shared.LearningTaskType
import app.veshinantam.shared.SharedMaterialUnit
import app.veshinantam.shared.SharedPlannedTask
import app.veshinantam.shared.SharedScheduleEngine
import app.veshinantam.shared.SharedScheduleRules
import app.veshinantam.shared.preset.MaterialCatalog

internal const val MONTHLY_TEHILLIM_ID = "tehillim-monthly"
internal const val MONTHLY_TEHILLIM_MARKER = ":HEBREW_MONTH_V2"
private const val HORIZON_DAYS = 120

internal fun monthlyTehillimDay(date: IsoDate, period: WebCalendarPeriod): Int {
    val first = requireNotNull(IsoDate.parse(period.startDate))
    var day = 1
    var cursor = first
    while (cursor < date) { cursor = cursor.plusDays(1); day++ }
    return day
}

internal fun monthlyTehillimReference(date: IsoDate, period: WebCalendarPeriod) = run {
    val last = requireNotNull(IsoDate.parse(period.endDate))
    val day = monthlyTehillimDay(date, period)
    var cursor = date
    var monthLength = day
    while (cursor < last) { cursor = cursor.plusDays(1); monthLength++ }
    MaterialCatalog.monthlyTehillimForDay(day, monthLength)
}

internal fun monthlyTehillimLearning(start: IsoDate, periodFor: (String) -> WebCalendarPeriod): List<SharedPlannedTask> =
    (0..HORIZON_DAYS).map { offset ->
        val date = start.plusDays(offset)
        val reference = monthlyTehillimReference(date, periodFor(date.toString()))
        val material = SharedMaterialUnit("monthly-tehillim:$date", 0, reference.english, reference.hebrew)
        SharedPlannedTask("learning:monthly-tehillim:$date", material, LearningTaskType.LEARNING, date, date)
    }

internal fun refreshMonthlyTehillim(
    state: WebAppState,
    today: IsoDate,
    periodFor: (String) -> WebCalendarPeriod,
    now: String,
): WebAppState {
    var schedules = state.schedules
    var tasks = state.tasks
    for (schedule in schedules.filter { it.presetId == MONTHLY_TEHILLIM_ID && it.active && !it.archived }) {
        val ownTasks = tasks.filter { it.scheduleId == schedule.id }
        val lastLearning = ownTasks.filter { it.type == LearningTaskType.LEARNING.name }
            .maxOfOrNull { it.dueDate }
        if (schedule.sourceType.endsWith(MONTHLY_TEHILLIM_MARKER) &&
            lastLearning != null && lastLearning >= today.plusDays(60).toString()) continue

        val migrating = !schedule.sourceType.endsWith(MONTHLY_TEHILLIM_MARKER)
        val revision = schedule.generationRevision + 1
        val start = maxOf(IsoDate.parse(schedule.startDate.orEmpty()) ?: today, today)
        val learning = monthlyTehillimLearning(start, periodFor)
        val reviews = SharedScheduleEngine().generateChazarah(
            learning, schedule.chazarahOffsets, schedule.repeatsAnnually,
            SharedScheduleRules((0..6).toSet()), start.year + 10,
        )
        val removedIds = ownTasks.filter { !it.completed && (migrating || it.dueDate >= today.toString()) }
            .mapTo(mutableSetOf()) { it.id }
        val stableKeys = ownTasks.filter { it.id !in removedIds }.mapTo(mutableSetOf()) { it.stableKey }
        val completedLearningDates = ownTasks.filter { it.completed && it.type == LearningTaskType.LEARNING.name }
            .mapTo(mutableSetOf()) { it.dueDate }
        val replacements = (learning + reviews).filter {
            (it.type != LearningTaskType.LEARNING || it.plannedDate.toString() !in completedLearningDates) &&
                stableKeys.add(it.stableKey)
        }.map { planned ->
            StoredTask(
                id = "${schedule.id}-${planned.stableKey}", scheduleId = schedule.id,
                referenceEnglish = planned.material.labelEnglish, referenceHebrew = planned.material.labelHebrew,
                dueDate = planned.plannedDate.toString(), type = planned.type.name,
                stableKey = planned.stableKey, materialType = schedule.materialType,
                originalLearningDate = planned.originalLearningDate.toString(), reviewIdentity = planned.reviewIdentity,
                generationRevision = revision, updatedAt = now,
            )
        }
        tasks = tasks.filter { it.id !in removedIds } + replacements
        schedules = schedules.map {
            if (it.id == schedule.id) it.copy(
                sourceType = it.sourceType.substringBefore(MONTHLY_TEHILLIM_MARKER) + MONTHLY_TEHILLIM_MARKER,
                targetDate = null, generationRevision = revision, updatedAt = now,
            )
            else it
        }
    }
    return if (schedules === state.schedules) state else state.copy(schedules = schedules, tasks = tasks)
}

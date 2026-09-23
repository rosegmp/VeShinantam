package app.veshinantam.web

import app.veshinantam.shared.IsoDate

val PrintableScheduleDayCounts = listOf(7, 14, 30, 60, 90)

data class PrintableScheduleRow(
    val date: String,
    val scheduleName: String,
    val taskType: String,
    val reference: String,
    val completed: Boolean,
)

data class PrintableSchedule(
    val startDate: String,
    val endDate: String,
    val dayCount: Int,
    val rows: List<PrintableScheduleRow>,
)

fun buildPrintableSchedule(state: WebAppState, startDate: String, dayCount: Int): PrintableSchedule {
    require(dayCount in PrintableScheduleDayCounts)
    val start = requireNotNull(IsoDate.parse(startDate))
    val end = start.plusDays(dayCount - 1)
    val scheduleOrder = state.schedules.withIndex().associate { it.value.id to it.index }
    val activeSchedules = state.schedules.filter { it.active && !it.archived }.associateBy { it.id }
    val rows = state.tasks
        .asSequence()
        .filter { task -> task.scheduleId in activeSchedules && task.dueDate >= start.toString() && task.dueDate <= end.toString() }
        .sortedWith(compareBy<StoredTask>(
            { it.dueDate },
            { activeSchedules[it.scheduleId]?.createdAt.orEmpty() },
            { scheduleOrder[it.scheduleId] ?: Int.MAX_VALUE },
            { it.type },
            { it.stableKey },
        ))
        .map { task ->
            val schedule = requireNotNull(activeSchedules[task.scheduleId])
            val hebrewReference = normalizedHebrewReference(task.referenceEnglish, task.referenceHebrew)
            PrintableScheduleRow(
                date = task.dueDate,
                scheduleName = if (state.language == "he") schedule.nameHebrew.ifBlank { schedule.name } else schedule.name.ifBlank { schedule.nameHebrew },
                taskType = if (task.type == "LEARNING") {
                    if (state.language == "he") "לימוד חדש" else "New learning"
                } else {
                    if (state.language == "he") "חזרה" else "Chazarah"
                },
                reference = when (state.sefarimLanguage) {
                    "ENGLISH" -> task.referenceEnglish.ifBlank { task.referenceHebrew }
                    "HEBREW" -> hebrewReference.ifBlank { task.referenceEnglish }
                    else -> listOf(task.referenceEnglish, hebrewReference).filter { it.isNotBlank() }.distinct().joinToString(" · ")
                },
                completed = task.completed,
            )
        }
        .toList()
    return PrintableSchedule(start.toString(), end.toString(), dayCount, rows)
}

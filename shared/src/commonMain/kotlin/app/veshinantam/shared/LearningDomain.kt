package app.veshinantam.shared

enum class LearningTaskType { LEARNING, CHAZARAH }

data class LearningSchedule(
    val id: String,
    val name: String,
    val material: String,
    val pace: Int,
    val active: Boolean = true,
)

data class LearningTask(
    val id: String,
    val scheduleId: String,
    val referenceEnglish: String,
    val referenceHebrew: String,
    val dueDate: String,
    val type: LearningTaskType,
    val completed: Boolean = false,
)

data class LearningProgress(
    val completed: Int,
    val total: Int,
    val learningCompleted: Int,
    val chazarahCompleted: Int,
) {
    val fraction: Float get() = if (total == 0) 0f else completed.toFloat() / total
    val percent: Int get() = (fraction * 100).toInt()
}

object LearningPlanner {
    fun progress(tasks: List<LearningTask>): LearningProgress = LearningProgress(
        completed = tasks.count { it.completed },
        total = tasks.size,
        learningCompleted = tasks.count { it.completed && it.type == LearningTaskType.LEARNING },
        chazarahCompleted = tasks.count { it.completed && it.type == LearningTaskType.CHAZARAH },
    )

    fun tasksForDate(tasks: List<LearningTask>, date: String): List<LearningTask> =
        tasks.filter { it.dueDate <= date }.sortedWith(
            compareBy<LearningTask> { it.completed }.thenBy { it.dueDate }.thenBy { it.type.ordinal },
        )

    fun createFirstAssignment(
        scheduleId: String,
        material: String,
        referenceEnglish: String,
        referenceHebrew: String,
        date: String,
    ): LearningTask = LearningTask(
        id = "$scheduleId-learning-1",
        scheduleId = scheduleId,
        referenceEnglish = referenceEnglish.ifBlank { material },
        referenceHebrew = referenceHebrew.ifBlank { referenceEnglish.ifBlank { material } },
        dueDate = date,
        type = LearningTaskType.LEARNING,
    )
}

data class MonthCell(val day: Int?, val isoDate: String?)

object GregorianCalendar {
    fun daysInMonth(year: Int, month: Int): Int = when (month) {
        2 -> if (isLeapYear(year)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

    fun monthCells(year: Int, month: Int): List<MonthCell> {
        val leading = dayOfWeek(year, month, 1)
        val cells = MutableList(leading) { MonthCell(null, null) }
        for (day in 1..daysInMonth(year, month)) {
            val iso = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
            cells += MonthCell(day, iso)
        }
        while (cells.size % 7 != 0) cells += MonthCell(null, null)
        return cells
    }

    fun previous(year: Int, month: Int): Pair<Int, Int> =
        if (month == 1) year - 1 to 12 else year to month - 1

    fun next(year: Int, month: Int): Pair<Int, Int> =
        if (month == 12) year + 1 to 1 else year to month + 1

    private fun isLeapYear(year: Int) = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

    // 0 = Sunday. Sakamoto's algorithm is deterministic and needs no platform date API.
    private fun dayOfWeek(yearValue: Int, month: Int, day: Int): Int {
        val offsets = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
        val year = if (month < 3) yearValue - 1 else yearValue
        return (year + year / 4 - year / 100 + year / 400 + offsets[month - 1] + day) % 7
    }
}

package app.veshinantam.shared

enum class LearningTaskType { LEARNING, CHAZARAH }

data class LearningSchedule(
    val id: String,
    val name: String,
    val material: String,
    val pace: Int,
    val weekdays: Set<Int> = (0..6).toSet(),
    val chazarahOffsets: List<Int> = listOf(1, 7),
    val active: Boolean = true,
    val archived: Boolean = false,
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

data class DailyWorkload(
    val date: String,
    val learning: Int,
    val chazarah: Int,
) {
    val total: Int get() = learning + chazarah
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

    fun scheduledDayStreak(tasks: List<LearningTask>, today: String): Int {
        val groups = tasks.filter { it.dueDate <= today }.groupBy { it.dueDate }.entries.sortedByDescending { it.key }
        var streak = 0
        for ((date, dayTasks) in groups) {
            if (date == today && dayTasks.any { !it.completed }) continue
            if (dayTasks.all { it.completed }) streak++ else break
        }
        return streak
    }

    fun upcomingWorkload(tasks: List<LearningTask>, startDate: String, days: Int = 7): List<DailyWorkload> {
        val start = IsoDate.parse(startDate) ?: return emptyList()
        return (0 until days.coerceAtLeast(0)).map { offset ->
            val date = start.plusDays(offset).toString()
            val due = tasks.filter { it.dueDate == date && !it.completed }
            DailyWorkload(
                date = date,
                learning = due.count { it.type == LearningTaskType.LEARNING },
                chazarah = due.count { it.type == LearningTaskType.CHAZARAH },
            )
        }
    }

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

    fun generatePlan(
        schedule: LearningSchedule,
        startDate: String,
        startingReferenceEnglish: String,
        startingReferenceHebrew: String,
        assignmentCount: Int = 14,
    ): List<LearningTask> {
        val firstDate = IsoDate.parse(startDate) ?: return emptyList()
        val eligibleWeekdays = schedule.weekdays.ifEmpty { (0..6).toSet() }
        val learning = buildList {
            var date = firstDate
            var assignment = 1
            while (size < assignmentCount) {
                if (GregorianCalendar.dayOfWeek(date.year, date.month, date.day) in eligibleWeekdays) {
                    val range = if (schedule.pace <= 1) "$assignment" else "$assignment–${assignment + schedule.pace - 1}"
                    add(
                        LearningTask(
                            id = "${schedule.id}-learning-$assignment",
                            scheduleId = schedule.id,
                            referenceEnglish = "$startingReferenceEnglish $range".trim(),
                            referenceHebrew = "$startingReferenceHebrew $range".trim(),
                            dueDate = date.toString(),
                            type = LearningTaskType.LEARNING,
                        ),
                    )
                    assignment += schedule.pace
                }
                date = date.plusDays(1)
            }
        }
        val reviews = learning.flatMapIndexed { learningIndex, task ->
            schedule.chazarahOffsets.distinct().filter { it > 0 }.mapIndexed { reviewIndex, offset ->
                var reviewDate = IsoDate.parse(task.dueDate)!!.plusDays(offset)
                while (GregorianCalendar.dayOfWeek(reviewDate.year, reviewDate.month, reviewDate.day) !in eligibleWeekdays) {
                    reviewDate = reviewDate.plusDays(1)
                }
                task.copy(
                    id = "${schedule.id}-review-$learningIndex-$reviewIndex",
                    dueDate = reviewDate.toString(),
                    type = LearningTaskType.CHAZARAH,
                )
            }
        }
        return learning + reviews
    }
}

data class IsoDate(val year: Int, val month: Int, val day: Int) : Comparable<IsoDate> {
    override fun toString(): String =
        "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

    fun plusDays(count: Int): IsoDate {
        if (count < 0) return minusDays(-count)
        var result = this
        repeat(count) {
            result = if (result.day < GregorianCalendar.daysInMonth(result.year, result.month)) {
                result.copy(day = result.day + 1)
            } else if (result.month < 12) {
                IsoDate(result.year, result.month + 1, 1)
            } else {
                IsoDate(result.year + 1, 1, 1)
            }
        }
        return result
    }

    fun minusDays(count: Int): IsoDate {
        if (count < 0) return plusDays(-count)
        var result = this
        repeat(count) {
            result = when {
                result.day > 1 -> result.copy(day = result.day - 1)
                result.month > 1 -> {
                    val month = result.month - 1
                    IsoDate(result.year, month, GregorianCalendar.daysInMonth(result.year, month))
                }
                else -> IsoDate(result.year - 1, 12, 31)
            }
        }
        return result
    }

    override operator fun compareTo(other: IsoDate): Int =
        compareValuesBy(this, other, IsoDate::year, IsoDate::month, IsoDate::day)

    companion object {
        fun parse(value: String): IsoDate? {
            val parts = value.split("-").mapNotNull { it.toIntOrNull() }
            if (parts.size != 3 || parts[1] !in 1..12 || parts[2] !in 1..daysInMonthSafe(parts[0], parts[1])) return null
            return IsoDate(parts[0], parts[1], parts[2])
        }

        private fun daysInMonthSafe(year: Int, month: Int) = GregorianCalendar.daysInMonth(year, month)
    }
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
    fun dayOfWeek(yearValue: Int, month: Int, day: Int): Int {
        val offsets = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
        val year = if (month < 3) yearValue - 1 else yearValue
        return (year + year / 4 - year / 100 + year / 400 + offsets[month - 1] + day) % 7
    }
}

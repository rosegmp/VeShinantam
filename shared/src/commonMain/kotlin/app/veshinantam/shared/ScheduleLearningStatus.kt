package app.veshinantam.shared

enum class ScheduleLearningFilter {
    COMPLETED,
    MISSED,
    TODAY,
    FUTURE,
    ALL,
}

data class ScheduleLearningCounts(
    val completed: Int,
    val missed: Int,
    val today: Int,
    val future: Int,
) {
    val all: Int get() = completed + missed + today + future

    fun count(filter: ScheduleLearningFilter): Int = when (filter) {
        ScheduleLearningFilter.COMPLETED -> completed
        ScheduleLearningFilter.MISSED -> missed
        ScheduleLearningFilter.TODAY -> today
        ScheduleLearningFilter.FUTURE -> future
        ScheduleLearningFilter.ALL -> all
    }
}

fun scheduleLearningFilter(dueDate: String, completed: Boolean, today: String): ScheduleLearningFilter = when {
    completed -> ScheduleLearningFilter.COMPLETED
    dueDate < today -> ScheduleLearningFilter.MISSED
    dueDate == today -> ScheduleLearningFilter.TODAY
    else -> ScheduleLearningFilter.FUTURE
}

fun scheduleLearningCounts(tasks: Iterable<Pair<String, Boolean>>, today: String): ScheduleLearningCounts {
    var completed = 0
    var missed = 0
    var dueToday = 0
    var future = 0
    tasks.forEach { (dueDate, isCompleted) ->
        when (scheduleLearningFilter(dueDate, isCompleted, today)) {
            ScheduleLearningFilter.COMPLETED -> completed += 1
            ScheduleLearningFilter.MISSED -> missed += 1
            ScheduleLearningFilter.TODAY -> dueToday += 1
            ScheduleLearningFilter.FUTURE -> future += 1
            ScheduleLearningFilter.ALL -> Unit
        }
    }
    return ScheduleLearningCounts(completed, missed, dueToday, future)
}


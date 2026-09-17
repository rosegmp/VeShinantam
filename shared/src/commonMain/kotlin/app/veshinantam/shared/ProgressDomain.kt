package app.veshinantam.shared

enum class SharedProgressMilestoneKind { STREAK, LEARNING_UNITS, REVIEWS }

enum class SharedProgressGoalKind { COMPLETION, STREAK, LEARNING_UNITS }

data class SharedProgressTask(
    val id: String,
    val plannedDate: String,
    val type: LearningTaskType,
    val quantity: Double = 1.0,
    val completed: Boolean = false,
    val scheduleActive: Boolean = true,
)

data class SharedSavedGoal(val kind: String, val target: Double)

data class SharedProgressMilestone(
    val kind: SharedProgressMilestoneKind,
    val target: Int,
    val current: Double,
    val unlocked: Boolean,
)

data class SharedProgressGoal(
    val kind: SharedProgressGoalKind,
    val target: Double,
    val current: Double,
)

data class SharedReviewWorkload(val date: String, val count: Int)

data class SharedProgressSummary(
    val dueCount: Int,
    val completedDueCount: Int,
    val completionPercent: Int,
    val completedLearningUnits: Double,
    val completedReviews: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val milestones: List<SharedProgressMilestone>,
    val goals: List<SharedProgressGoal>,
    val upcomingReviews: List<SharedReviewWorkload>,
)

object SharedProgressCalculator {
    fun calculate(
        tasks: List<SharedProgressTask>,
        today: String,
        savedGoals: List<SharedSavedGoal> = emptyList(),
    ): SharedProgressSummary {
        val todayDate = requireNotNull(IsoDate.parse(today)) { "Invalid progress date: $today" }
        val due = tasks.filter { it.plannedDate <= today }
        val completedDue = due.filter { it.completed }
        val scheduledDays = due.filter { it.scheduleActive }
            .groupBy { it.plannedDate }
            .entries
            .sortedByDescending { it.key }
        var currentStreak = 0
        for ((_, dayTasks) in scheduledDays) {
            if (dayTasks.all { it.completed }) currentStreak++ else break
        }
        var runningStreak = 0
        var longestStreak = 0
        scheduledDays.asReversed().forEach { (_, dayTasks) ->
            if (dayTasks.all { it.completed }) {
                runningStreak++
                longestStreak = maxOf(longestStreak, runningStreak)
            } else {
                runningStreak = 0
            }
        }
        val completedLearningUnits = completedDue
            .filter { it.type == LearningTaskType.LEARNING }
            .sumOf { it.quantity }
        val completedReviews = completedDue.count { it.type == LearningTaskType.CHAZARAH }
        val completion = if (due.isEmpty()) 0.0 else completedDue.size * 100.0 / due.size
        val milestones = buildList {
            listOf(7, 30, 100).forEach { target ->
                add(SharedProgressMilestone(SharedProgressMilestoneKind.STREAK, target, longestStreak.toDouble(), longestStreak >= target))
            }
            listOf(10, 50, 100, 500).forEach { target ->
                add(SharedProgressMilestone(SharedProgressMilestoneKind.LEARNING_UNITS, target, completedLearningUnits, completedLearningUnits >= target))
            }
            listOf(10, 50, 100, 500).forEach { target ->
                add(SharedProgressMilestone(SharedProgressMilestoneKind.REVIEWS, target, completedReviews.toDouble(), completedReviews >= target))
            }
        }
        val lastReviewDate = todayDate.plusDays(30).toString()
        val upcomingReviews = tasks.asSequence()
            .filter {
                it.scheduleActive && it.type == LearningTaskType.CHAZARAH && !it.completed &&
                    it.plannedDate > today && it.plannedDate <= lastReviewDate
            }
            .groupingBy { it.plannedDate }
            .eachCount()
            .entries
            .sortedBy { it.key }
            .map { SharedReviewWorkload(it.key, it.value) }
        val goals = savedGoals.mapNotNull { saved ->
            val kind = runCatching { SharedProgressGoalKind.valueOf(saved.kind) }.getOrNull() ?: return@mapNotNull null
            val current = when (kind) {
                SharedProgressGoalKind.COMPLETION -> completion
                SharedProgressGoalKind.STREAK -> currentStreak.toDouble()
                SharedProgressGoalKind.LEARNING_UNITS -> completedLearningUnits
            }
            SharedProgressGoal(kind, saved.target, current)
        }
        return SharedProgressSummary(
            dueCount = due.size,
            completedDueCount = completedDue.size,
            completionPercent = completion.toInt(),
            completedLearningUnits = completedLearningUnits,
            completedReviews = completedReviews,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            milestones = milestones,
            goals = goals,
            upcomingReviews = upcomingReviews,
        )
    }
}

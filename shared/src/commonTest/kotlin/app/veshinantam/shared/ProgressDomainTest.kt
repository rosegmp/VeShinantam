package app.veshinantam.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class ProgressDomainTest {
    private val today = "2026-09-08"

    @Test
    fun matchesDueTotalsUnitsStreaksAndMilestones() {
        val tasks = listOf(
            task("today-learning", today, LearningTaskType.LEARNING, true, 1.5),
            task("today-review", today, LearningTaskType.CHAZARAH, true),
            task("yesterday", "2026-09-07", LearningTaskType.LEARNING, true, 2.0),
            task("break", "2026-09-06", LearningTaskType.LEARNING, false),
            task("older", "2026-09-05", LearningTaskType.LEARNING, true, 9.0),
            task("future", "2026-09-09", LearningTaskType.LEARNING, true, 100.0),
        )

        val result = SharedProgressCalculator.calculate(tasks, today)

        assertEquals(5, result.dueCount)
        assertEquals(4, result.completedDueCount)
        assertEquals(80, result.completionPercent)
        assertEquals(12.5, result.completedLearningUnits)
        assertEquals(1, result.completedReviews)
        assertEquals(2, result.currentStreak)
        assertEquals(2, result.longestStreak)
        assertEquals(true, result.milestones.first { it.kind == SharedProgressMilestoneKind.LEARNING_UNITS && it.target == 10 }.unlocked)
    }

    @Test
    fun calculatesGoalsAndThirtyDayActiveReviewWorkload() {
        val tasks = listOf(
            task("complete", today, LearningTaskType.LEARNING, true, 4.5),
            task("incomplete", today, LearningTaskType.LEARNING),
            task("review", "2026-09-09", LearningTaskType.CHAZARAH),
            task("paused", "2026-09-10", LearningTaskType.CHAZARAH, active = false),
            task("late", "2026-10-09", LearningTaskType.CHAZARAH),
        )
        val goals = listOf(
            SharedSavedGoal("COMPLETION", 80.0),
            SharedSavedGoal("STREAK", 3.0),
            SharedSavedGoal("LEARNING_UNITS", 10.0),
            SharedSavedGoal("REMOVED", 1.0),
        )

        val result = SharedProgressCalculator.calculate(tasks, today, goals)

        assertEquals(listOf(SharedReviewWorkload("2026-09-09", 1)), result.upcomingReviews)
        assertEquals(3, result.goals.size)
        assertEquals(50.0, result.goals.first { it.kind == SharedProgressGoalKind.COMPLETION }.current)
        assertEquals(4.5, result.goals.first { it.kind == SharedProgressGoalKind.LEARNING_UNITS }.current)
    }

    private fun task(
        id: String,
        date: String,
        type: LearningTaskType,
        completed: Boolean = false,
        quantity: Double = 1.0,
        active: Boolean = true,
    ) = SharedProgressTask(id, date, type, quantity, completed, active)
}

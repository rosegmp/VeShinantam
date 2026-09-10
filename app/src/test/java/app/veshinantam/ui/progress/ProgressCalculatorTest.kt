package app.veshinantam.ui.progress

import app.veshinantam.data.local.ProgressGoalEntity
import app.veshinantam.data.local.ProgressTaskRow
import app.veshinantam.domain.model.ScheduleState
import app.veshinantam.domain.model.TaskType
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressCalculatorTest {
    private val today = LocalDate.of(2026, 9, 8)

    @Test
    fun `calculates completion totals and consecutive completed scheduled days`() {
        val rows = listOf(
            row("today-learning", today, TaskType.LEARNING, completed = true, quantity = 1.5),
            row("today-review", today, TaskType.CHAZARAH, completed = true),
            row("yesterday", today.minusDays(1), TaskType.LEARNING, completed = true, quantity = 2.0),
            row("streak-break", today.minusDays(2), TaskType.LEARNING, completed = false),
            row("older", today.minusDays(3), TaskType.LEARNING, completed = true, quantity = 9.0),
        )

        val result = ProgressCalculator.calculate(rows, today)

        assertEquals(5, result.dueCount)
        assertEquals(4, result.completedDueCount)
        assertEquals(80, result.completionPercent)
        assertEquals(12.5, result.completedLearningUnits, 0.0)
        assertEquals(1, result.completedReviews)
        assertEquals(2, result.currentStreak)
        assertEquals(2, result.longestStreak)
    }

    @Test
    fun `milestones use longest completed streak and aggregate totals`() {
        val rows = (0..6).map { offset ->
            row("streak-$offset", today.minusDays(offset.toLong()), TaskType.LEARNING, completed = true, quantity = 2.0)
        } + row("break", today.minusDays(8), TaskType.LEARNING) + List(10) { index ->
            row("review-$index", today.minusDays(10), TaskType.CHAZARAH, completed = true)
        }

        val result = ProgressCalculator.calculate(rows, today)

        assertEquals(7, result.longestStreak)
        assertEquals(true, result.milestones.first { it.kind == MilestoneKind.STREAK && it.target == 7 }.unlocked)
        assertEquals(true, result.milestones.first { it.kind == MilestoneKind.LEARNING_UNITS && it.target == 10 }.unlocked)
        assertEquals(true, result.milestones.first { it.kind == MilestoneKind.REVIEWS && it.target == 10 }.unlocked)
        assertEquals(false, result.milestones.first { it.kind == MilestoneKind.STREAK && it.target == 30 }.unlocked)
    }

    @Test
    fun `upcoming workload includes only active incomplete reviews in next thirty days`() {
        val rows = listOf(
            row("review-1", today.plusDays(1), TaskType.CHAZARAH),
            row("review-2", today.plusDays(1), TaskType.CHAZARAH),
            row("learning", today.plusDays(2), TaskType.LEARNING),
            row("completed", today.plusDays(3), TaskType.CHAZARAH, completed = true),
            row("paused", today.plusDays(4), TaskType.CHAZARAH, state = ScheduleState.PAUSED),
            row("too-late", today.plusDays(31), TaskType.CHAZARAH),
        )

        assertEquals(listOf(ReviewWorkload(today.plusDays(1), 2)), ProgressCalculator.calculate(rows, today).upcomingReviews)
    }

    @Test
    fun `saved goals map to current progress and unknown goal kinds are ignored`() {
        val rows = listOf(
            row("complete", today, TaskType.LEARNING, completed = true, quantity = 4.5),
            row("incomplete", today, TaskType.LEARNING),
        )
        val savedGoals = listOf(
            ProgressGoalEntity(ProgressGoalKind.COMPLETION.name, 80.0),
            ProgressGoalEntity(ProgressGoalKind.STREAK.name, 3.0),
            ProgressGoalEntity(ProgressGoalKind.LEARNING_UNITS.name, 10.0),
            ProgressGoalEntity("REMOVED_KIND", 1.0),
        )

        val goals = ProgressCalculator.calculate(rows, today, savedGoals).goals.associateBy { it.kind }

        assertEquals(3, goals.size)
        assertEquals(50.0, goals.getValue(ProgressGoalKind.COMPLETION).current, 0.0)
        assertEquals(80.0, goals.getValue(ProgressGoalKind.COMPLETION).target, 0.0)
        assertEquals(0.0, goals.getValue(ProgressGoalKind.STREAK).current, 0.0)
        assertEquals(4.5, goals.getValue(ProgressGoalKind.LEARNING_UNITS).current, 0.0)
    }

    private fun row(
        id: String,
        date: LocalDate,
        type: TaskType,
        completed: Boolean = false,
        quantity: Double = 1.0,
        state: ScheduleState = ScheduleState.ACTIVE,
    ) = ProgressTaskRow(
        taskId = id,
        scheduleId = "schedule",
        scheduleState = state,
        type = type,
        quantity = quantity,
        plannedDate = date,
        completedAt = if (completed) Instant.parse("2026-09-08T12:00:00Z") else null,
    )
}

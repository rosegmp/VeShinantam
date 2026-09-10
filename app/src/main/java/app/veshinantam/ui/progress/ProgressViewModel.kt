package app.veshinantam.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.veshinantam.data.ScheduleRepository
import app.veshinantam.data.local.ProgressTaskRow
import app.veshinantam.data.local.ProgressGoalEntity
import app.veshinantam.domain.model.ScheduleState
import app.veshinantam.domain.model.TaskType
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReviewWorkload(val date: LocalDate, val count: Int)

enum class MilestoneKind { STREAK, LEARNING_UNITS, REVIEWS }

data class ProgressMilestone(
    val kind: MilestoneKind,
    val target: Int,
    val current: Double,
    val unlocked: Boolean,
)

enum class ProgressGoalKind { COMPLETION, STREAK, LEARNING_UNITS }

data class ProgressGoal(val kind: ProgressGoalKind, val target: Double, val current: Double)

data class ProgressUiState(
    val dueCount: Int = 0,
    val completedDueCount: Int = 0,
    val completionPercent: Int = 0,
    val completedLearningUnits: Double = 0.0,
    val completedReviews: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val milestones: List<ProgressMilestone> = emptyList(),
    val goals: List<ProgressGoal> = emptyList(),
    val upcomingReviews: List<ReviewWorkload> = emptyList(),
)

object ProgressCalculator {
    fun calculate(rows: List<ProgressTaskRow>, today: LocalDate, savedGoals: List<ProgressGoalEntity> = emptyList()): ProgressUiState {
        val due = rows.filter { it.plannedDate <= today }
        val completedDue = due.filter { it.completedAt != null }
        val scheduledDays = due.filter { it.scheduleState == ScheduleState.ACTIVE }
            .groupBy { it.plannedDate }
            .toSortedMap(reverseOrder())
        var streak = 0
        for ((_, tasks) in scheduledDays) {
            if (tasks.all { it.completedAt != null }) streak++ else break
        }
        var runningStreak = 0
        var longestStreak = 0
        scheduledDays.toSortedMap().values.forEach { tasks ->
            if (tasks.all { it.completedAt != null }) {
                runningStreak++
                longestStreak = maxOf(longestStreak, runningStreak)
            } else {
                runningStreak = 0
            }
        }
        val completedLearningUnits = completedDue.filter { it.type == TaskType.LEARNING }.sumOf { it.quantity }
        val completedReviews = completedDue.count { it.type == TaskType.CHAZARAH }
        val milestones = buildList {
            listOf(7, 30, 100).forEach { add(ProgressMilestone(MilestoneKind.STREAK, it, longestStreak.toDouble(), longestStreak >= it)) }
            listOf(10, 50, 100, 500).forEach { add(ProgressMilestone(MilestoneKind.LEARNING_UNITS, it, completedLearningUnits, completedLearningUnits >= it)) }
            listOf(10, 50, 100, 500).forEach { add(ProgressMilestone(MilestoneKind.REVIEWS, it, completedReviews.toDouble(), completedReviews >= it)) }
        }
        val upcoming = rows.asSequence()
            .filter {
                it.scheduleState == ScheduleState.ACTIVE && it.type == TaskType.CHAZARAH &&
                    it.completedAt == null && it.plannedDate > today && it.plannedDate <= today.plusDays(30)
            }
            .groupingBy { it.plannedDate }
            .eachCount()
            .toSortedMap()
            .map { ReviewWorkload(it.key, it.value) }
        val goals = savedGoals.mapNotNull { saved ->
            val kind = runCatching { ProgressGoalKind.valueOf(saved.kind) }.getOrNull() ?: return@mapNotNull null
            val current = when (kind) {
                ProgressGoalKind.COMPLETION -> if (due.isEmpty()) 0.0 else completedDue.size * 100.0 / due.size
                ProgressGoalKind.STREAK -> streak.toDouble()
                ProgressGoalKind.LEARNING_UNITS -> completedLearningUnits
            }
            ProgressGoal(kind, saved.target, current)
        }
        return ProgressUiState(
            dueCount = due.size,
            completedDueCount = completedDue.size,
            completionPercent = if (due.isEmpty()) 0 else (completedDue.size * 100.0 / due.size).toInt(),
            completedLearningUnits = completedLearningUnits,
            completedReviews = completedReviews,
            currentStreak = streak,
            longestStreak = longestStreak,
            milestones = milestones,
            goals = goals,
            upcomingReviews = upcoming,
        )
    }
}

class ProgressViewModel(private val repository: ScheduleRepository) : ViewModel() {
    val state = combine(repository.observeProgressTasks(), repository.observeProgressGoals()) { tasks, goals ->
            ProgressCalculator.calculate(tasks, LocalDate.now(), goals)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun saveGoals(completion: Double?, streak: Double?, learningUnits: Double?) {
        val goals = listOfNotNull(
            completion?.takeIf { it > 0 }?.let { ProgressGoalEntity(ProgressGoalKind.COMPLETION.name, it.coerceAtMost(100.0)) },
            streak?.takeIf { it > 0 }?.let { ProgressGoalEntity(ProgressGoalKind.STREAK.name, it) },
            learningUnits?.takeIf { it > 0 }?.let { ProgressGoalEntity(ProgressGoalKind.LEARNING_UNITS.name, it) },
        )
        viewModelScope.launch { repository.replaceProgressGoals(goals) }
    }

    class Factory(private val repository: ScheduleRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ProgressViewModel(repository) as T
    }
}

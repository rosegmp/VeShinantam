package app.veshinantam.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.veshinantam.data.ScheduleRepository
import app.veshinantam.data.local.ProgressTaskRow
import app.veshinantam.data.local.ProgressGoalEntity
import app.veshinantam.domain.model.ScheduleState
import app.veshinantam.domain.model.TaskType
import app.veshinantam.shared.LearningTaskType
import app.veshinantam.shared.SharedProgressCalculator
import app.veshinantam.shared.SharedProgressTask
import app.veshinantam.shared.SharedSavedGoal
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
        val shared = SharedProgressCalculator.calculate(
            tasks = rows.map { row ->
                SharedProgressTask(
                    id = row.taskId,
                    plannedDate = row.plannedDate.toString(),
                    type = if (row.type == TaskType.LEARNING) LearningTaskType.LEARNING else LearningTaskType.CHAZARAH,
                    quantity = row.quantity,
                    completed = row.completedAt != null,
                    scheduleActive = row.scheduleState == ScheduleState.ACTIVE,
                )
            },
            today = today.toString(),
            savedGoals = savedGoals.map { SharedSavedGoal(it.kind, it.target) },
        )
        return ProgressUiState(
            dueCount = shared.dueCount,
            completedDueCount = shared.completedDueCount,
            completionPercent = shared.completionPercent,
            completedLearningUnits = shared.completedLearningUnits,
            completedReviews = shared.completedReviews,
            currentStreak = shared.currentStreak,
            longestStreak = shared.longestStreak,
            milestones = shared.milestones.map {
                ProgressMilestone(MilestoneKind.valueOf(it.kind.name), it.target, it.current, it.unlocked)
            },
            goals = shared.goals.map {
                ProgressGoal(ProgressGoalKind.valueOf(it.kind.name), it.target, it.current)
            },
            upcomingReviews = shared.upcomingReviews.map { ReviewWorkload(LocalDate.parse(it.date), it.count) },
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

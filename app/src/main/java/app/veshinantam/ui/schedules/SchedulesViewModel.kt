package app.veshinantam.ui.schedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.veshinantam.data.CustomScheduleDraft
import app.veshinantam.data.FutureScheduleEditDraft
import app.veshinantam.data.PendingSchedulePlan
import app.veshinantam.data.PresetScheduleDraft
import app.veshinantam.data.ScheduleRepository
import app.veshinantam.data.local.ScheduleEntity
import app.veshinantam.data.local.TaskEntity
import app.veshinantam.domain.model.ScheduleState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SchedulesViewModel(private val repository: ScheduleRepository) : ViewModel() {
    private val selectedLearningScheduleId = MutableStateFlow<String?>(null)

    val schedules = repository.observeSchedules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val exclusions = repository.observeExclusions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val selectedLearningTasks = selectedLearningScheduleId
        .flatMapLatest { scheduleId ->
            if (scheduleId == null) flowOf(emptyList<TaskEntity>()) else repository.observeLearningTasks(scheduleId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectLearningSchedule(scheduleId: String?) {
        selectedLearningScheduleId.value = scheduleId
    }

    fun setCompleted(taskId: String, completed: Boolean) {
        viewModelScope.launch { repository.setCompleted(taskId, completed) }
    }

    fun preview(draft: CustomScheduleDraft): PendingSchedulePlan = repository.buildCustomPlan(draft)

    fun previewPreset(draft: PresetScheduleDraft): PendingSchedulePlan = repository.buildPresetPlan(draft)

    fun save(plan: PendingSchedulePlan) {
        viewModelScope.launch { repository.savePlan(plan) }
    }

    fun setState(scheduleId: String, state: ScheduleState) {
        viewModelScope.launch { repository.setScheduleState(scheduleId, state) }
    }

    fun editFuture(scheduleId: String, draft: FutureScheduleEditDraft) {
        viewModelScope.launch { repository.editFutureSchedule(scheduleId, draft) }
    }

    fun delete(scheduleId: String) {
        viewModelScope.launch { repository.deleteSchedule(scheduleId) }
    }

    fun completePastLearning(scheduleId: String) {
        viewModelScope.launch { repository.completePastLearning(scheduleId) }
    }

    fun completePastChazarah(scheduleId: String) {
        viewModelScope.launch { repository.completePastChazarah(scheduleId) }
    }

    fun addExclusion(scheduleId: String, date: java.time.LocalDate) {
        viewModelScope.launch { repository.addExclusion(scheduleId, date) }
    }

    fun removeExclusion(scheduleId: String, date: java.time.LocalDate) {
        viewModelScope.launch { repository.removeExclusion(scheduleId, date) }
    }

    class Factory(private val repository: ScheduleRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SchedulesViewModel(repository) as T
    }
}

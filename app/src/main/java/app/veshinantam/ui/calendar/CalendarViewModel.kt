package app.veshinantam.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.veshinantam.data.ScheduleRepository
import app.veshinantam.data.local.TodayTaskRow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

enum class CalendarDayStatus { NONE, INCOMPLETE, PARTIAL, COMPLETE }

data class CalendarTaskStatus(val date: LocalDate, val completed: Boolean)

data class CalendarDaySummary(
    val date: LocalDate,
    val completedCount: Int,
    val taskCount: Int,
    val status: CalendarDayStatus,
)

object CalendarMonthCalculator {
    fun build(month: YearMonth, tasks: List<CalendarTaskStatus>): List<CalendarDaySummary?> {
        return build(month.atDay(1), month.atEndOfMonth(), tasks)
    }

    fun build(start: LocalDate, endInclusive: LocalDate, tasks: List<CalendarTaskStatus>): List<CalendarDaySummary?> {
        require(!endInclusive.isBefore(start))
        val byDate = tasks.groupBy { it.date }
        val leadingEmptyDays = start.dayOfWeek.value % 7
        val cells = MutableList<CalendarDaySummary?>(leadingEmptyDays) { null }
        var date = start
        while (!date.isAfter(endInclusive)) {
            val dateTasks = byDate[date].orEmpty()
            val completed = dateTasks.count { it.completed }
            val status = when {
                dateTasks.isEmpty() -> CalendarDayStatus.NONE
                completed == 0 -> CalendarDayStatus.INCOMPLETE
                completed == dateTasks.size -> CalendarDayStatus.COMPLETE
                else -> CalendarDayStatus.PARTIAL
            }
            cells += CalendarDaySummary(date, completed, dateTasks.size, status)
            date = date.plusDays(1)
        }
        while (cells.size % 7 != 0) cells += null
        return cells
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(private val repository: ScheduleRepository) : ViewModel() {
    private val currentMonth = YearMonth.now()
    private val _range = MutableStateFlow(currentMonth.atDay(1) to currentMonth.atEndOfMonth())
    val tasks = _range.flatMapLatest { range ->
        repository.observeCalendarTasks(range.first, range.second)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun showRange(start: LocalDate, endInclusive: LocalDate) {
        _range.value = start to endInclusive
    }

    fun setCompleted(taskId: String, completed: Boolean) {
        viewModelScope.launch { repository.setCompleted(taskId, completed) }
    }

    class Factory(private val repository: ScheduleRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = CalendarViewModel(repository) as T
    }
}

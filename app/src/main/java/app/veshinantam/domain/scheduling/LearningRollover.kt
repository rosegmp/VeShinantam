package app.veshinantam.domain.scheduling

import java.time.DayOfWeek
import java.time.LocalDate

data class LearningPlacement(
    val id: String,
    val stableKey: String,
    val plannedDate: LocalDate,
    val completed: Boolean,
)

data class LearningDateChange(
    val id: String,
    val plannedDate: LocalDate,
)

object LearningRollover {
    fun reschedule(
        tasks: List<LearningPlacement>,
        selectedWeekdays: Set<DayOfWeek>,
        excludedDates: Set<LocalDate>,
        dailyQuantity: Int,
        shiftOverdueTo: LocalDate? = null,
    ): List<LearningDateChange> {
        require(selectedWeekdays.isNotEmpty())
        val capacity = dailyQuantity.coerceAtLeast(1)
        val usedByCompleted = tasks.asSequence()
            .filter(LearningPlacement::completed)
            .groupingBy(LearningPlacement::plannedDate)
            .eachCount()
            .toMutableMap()
        val changes = mutableListOf<LearningDateChange>()

        tasks.asSequence()
            .filterNot(LearningPlacement::completed)
            .sortedWith(compareBy(LearningPlacement::plannedDate, LearningPlacement::stableKey))
            .forEach { task ->
                var candidate = if (shiftOverdueTo != null && task.plannedDate < shiftOverdueTo) {
                    shiftOverdueTo
                } else {
                    task.plannedDate
                }
                while (
                    candidate.dayOfWeek !in selectedWeekdays ||
                    candidate in excludedDates ||
                    usedByCompleted.getOrDefault(candidate, 0) >= capacity
                ) {
                    candidate = candidate.plusDays(1)
                }
                usedByCompleted[candidate] = usedByCompleted.getOrDefault(candidate, 0) + 1
                if (candidate != task.plannedDate) changes += LearningDateChange(task.id, candidate)
            }
        return changes
    }
}

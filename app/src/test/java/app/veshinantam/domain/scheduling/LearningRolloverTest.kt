package app.veshinantam.domain.scheduling

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class LearningRolloverTest {
    private val weekdays = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
    )

    @Test
    fun `overdue learning rolls forward in order across exclusions and rest days`() {
        val changes = LearningRollover.reschedule(
            tasks = listOf(
                placement("a", "2026-09-07"),
                placement("b", "2026-09-08"),
                placement("c", "2026-09-09"),
            ),
            selectedWeekdays = weekdays,
            excludedDates = setOf(LocalDate.parse("2026-09-10")),
            dailyQuantity = 1,
            shiftOverdueTo = LocalDate.parse("2026-09-09"),
        )

        assertEquals(
            listOf(
                LearningDateChange("a", LocalDate.parse("2026-09-09")),
                LearningDateChange("b", LocalDate.parse("2026-09-11")),
                LearningDateChange("c", LocalDate.parse("2026-09-14")),
            ),
            changes,
        )
    }

    @Test
    fun `completed learning remains fixed and consumes its daily slot`() {
        val changes = LearningRollover.reschedule(
            tasks = listOf(
                placement("past", "2026-09-08"),
                placement("done", "2026-09-09", completed = true),
            ),
            selectedWeekdays = weekdays,
            excludedDates = emptySet(),
            dailyQuantity = 1,
            shiftOverdueTo = LocalDate.parse("2026-09-09"),
        )

        assertEquals(
            listOf(LearningDateChange("past", LocalDate.parse("2026-09-10"))),
            changes,
        )
    }

    @Test
    fun `adding an exclusion shifts unfinished learning but does not pull other tasks earlier`() {
        val changes = LearningRollover.reschedule(
            tasks = listOf(
                placement("a", "2026-09-09"),
                placement("b", "2026-09-10"),
                placement("c", "2026-09-11"),
            ),
            selectedWeekdays = weekdays,
            excludedDates = setOf(LocalDate.parse("2026-09-10")),
            dailyQuantity = 1,
        )

        assertEquals(
            listOf(
                LearningDateChange("b", LocalDate.parse("2026-09-11")),
                LearningDateChange("c", LocalDate.parse("2026-09-14")),
            ),
            changes,
        )
    }

    @Test
    fun `daily quantity allows multiple rollover tasks in one slot`() {
        val changes = LearningRollover.reschedule(
            tasks = listOf(
                placement("a", "2026-09-07"),
                placement("b", "2026-09-08"),
            ),
            selectedWeekdays = weekdays,
            excludedDates = emptySet(),
            dailyQuantity = 2,
            shiftOverdueTo = LocalDate.parse("2026-09-09"),
        )

        assertEquals(
            listOf(
                LearningDateChange("a", LocalDate.parse("2026-09-09")),
                LearningDateChange("b", LocalDate.parse("2026-09-09")),
            ),
            changes,
        )
    }

    private fun placement(id: String, date: String, completed: Boolean = false) = LearningPlacement(
        id = id,
        stableKey = id,
        plannedDate = LocalDate.parse(date),
        completed = completed,
    )
}

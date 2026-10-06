package app.veshinantam.web

import app.veshinantam.shared.IsoDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MonthlyTehillimWebTest {
    private val periodFor: (String) -> WebCalendarPeriod = { date ->
        val current = requireNotNull(IsoDate.parse(date))
        if (date < "2026-10-30") WebCalendarPeriod("2026-10-01", "2026-10-29", "Test month")
        else {
            var first = IsoDate(2026, 10, 30)
            while (current > first.plusDays(29)) first = first.plusDays(30)
            WebCalendarPeriod(first.toString(), first.plusDays(29).toString(), "Test month")
        }
    }

    @Test
    fun combinesTheLastPortionsThenRestartsOnTheNewHebrewMonth() {
        val tasks = monthlyTehillimLearning(IsoDate(2026, 10, 28), periodFor)
        assertEquals("Tehillim 135-139", tasks[0].material.labelEnglish)
        assertEquals("Tehillim 140-150", tasks[1].material.labelEnglish)
        assertEquals("Tehillim 1-9", tasks[2].material.labelEnglish)
    }

    @Test
    fun migratesAnExistingFiniteScheduleWithoutRemovingCompletedHistory() {
        val schedule = StoredSchedule(
            id = "monthly", name = "Monthly Tehillim", material = "PEREK", pace = 1,
            presetId = MONTHLY_TEHILLIM_ID, startDate = "2026-10-01", generationRevision = 2,
        )
        val completed = StoredTask("done", "monthly", "Old reading", "", "2026-10-28", "LEARNING", completed = true)
        val incorrect = StoredTask("wrong", "monthly", "Tehillim 140-150", "", "2026-10-29", "LEARNING")
        val refreshed = refreshMonthlyTehillim(
            WebAppState(schedules = listOf(schedule), tasks = listOf(completed, incorrect)),
            IsoDate(2026, 10, 29), periodFor, "2026-10-29T12:00:00Z",
        )
        assertTrue(refreshed.tasks.any { it.id == "done" && it.completed })
        assertTrue(refreshed.tasks.none { it.id == "wrong" })
        assertEquals("Tehillim 140-150", refreshed.tasks.first { it.dueDate == "2026-10-29" && it.type == "LEARNING" }.referenceEnglish)
        assertEquals(3, refreshed.schedules.single().generationRevision)
        assertTrue(refreshed.schedules.single().sourceType.endsWith(MONTHLY_TEHILLIM_MARKER))
    }
}

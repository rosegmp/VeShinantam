package app.veshinantam.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class LearningDomainTest {
    @Test
    fun progressSeparatesLearningAndChazarah() {
        val tasks = listOf(
            LearningTask("1", "s", "A", "א", "2026-09-11", LearningTaskType.LEARNING, true),
            LearningTask("2", "s", "B", "ב", "2026-09-11", LearningTaskType.CHAZARAH, true),
            LearningTask("3", "s", "C", "ג", "2026-09-11", LearningTaskType.LEARNING, false),
        )
        assertEquals(LearningProgress(2, 3, 1, 1), LearningPlanner.progress(tasks))
    }

    @Test
    fun september2026StartsOnTuesday() {
        val cells = GregorianCalendar.monthCells(2026, 9)
        assertEquals(2, cells.indexOfFirst { it.day == 1 })
        assertEquals(30, cells.count { it.day != null })
    }
}

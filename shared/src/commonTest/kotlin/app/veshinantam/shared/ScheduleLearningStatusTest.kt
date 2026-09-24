package app.veshinantam.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class ScheduleLearningStatusTest {
    @Test
    fun statusesAreMutuallyExclusiveAndCompletedTakesPriority() {
        val today = "2026-09-23"

        assertEquals(ScheduleLearningFilter.COMPLETED, scheduleLearningFilter("2026-09-20", true, today))
        assertEquals(ScheduleLearningFilter.COMPLETED, scheduleLearningFilter("2026-09-30", true, today))
        assertEquals(ScheduleLearningFilter.MISSED, scheduleLearningFilter("2026-09-22", false, today))
        assertEquals(ScheduleLearningFilter.TODAY, scheduleLearningFilter(today, false, today))
        assertEquals(ScheduleLearningFilter.FUTURE, scheduleLearningFilter("2026-09-24", false, today))
    }

    @Test
    fun countsAddUpToAllLearning() {
        val counts = scheduleLearningCounts(
            listOf(
                "2026-09-20" to true,
                "2026-09-22" to false,
                "2026-09-23" to false,
                "2026-09-24" to false,
                "2026-09-30" to true,
            ),
            today = "2026-09-23",
        )

        assertEquals(ScheduleLearningCounts(completed = 2, missed = 1, today = 1, future = 1), counts)
        assertEquals(5, counts.all)
        ScheduleLearningFilter.entries.forEach { filter ->
            assertEquals(
                counts.count(filter),
                if (filter == ScheduleLearningFilter.ALL) counts.all else 1 + if (filter == ScheduleLearningFilter.COMPLETED) 1 else 0,
            )
        }
    }
}

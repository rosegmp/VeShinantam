package app.veshinantam.widget

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.LocalDate
import app.veshinantam.data.local.WidgetTaskRow
import app.veshinantam.domain.model.TaskType

class WidgetUpdaterTest {
    @Test
    fun `next widget refresh is just after local midnight`() {
        val zone = ZoneId.of("America/New_York")
        val now = ZonedDateTime.of(2026, 11, 1, 22, 15, 0, 0, zone)

        assertEquals(
            ZonedDateTime.of(2026, 11, 2, 0, 0, 2, 0, zone),
            WidgetUpdater.nextRefresh(now),
        )
    }

    @Test
    fun `summary lists only today and counts overdue work by type`() {
        val today = LocalDate.of(2026, 9, 8)
        val rows = listOf(
            row(TaskType.LEARNING, today, "Today learning"),
            row(TaskType.CHAZARAH, today, "Today review"),
            row(TaskType.LEARNING, today.minusDays(1), "Late learning"),
            row(TaskType.LEARNING, today.minusDays(2), "Older learning"),
            row(TaskType.CHAZARAH, today.minusDays(1), "Late review"),
        )

        val summary = WidgetSummaryBuilder.build(rows, today)

        assertEquals(listOf("Today learning", "Today review"), summary.todayTasks.map { it.labelEnglish })
        assertEquals(2, summary.overdueLearningCount)
        assertEquals(1, summary.overdueChazarahCount)
        assertEquals(3, summary.overdueCount)
    }

    @Test
    fun `widget height controls how many task rows are displayed`() {
        assertEquals(0, WidgetSizePolicy.visibleTaskCount(300, 0))
        assertEquals(1, WidgetSizePolicy.visibleTaskCount(150, 10))
        assertEquals(6, WidgetSizePolicy.visibleTaskCount(240, 10))
        assertEquals(10, WidgetSizePolicy.visibleTaskCount(500, 10))
        assertEquals(24, WidgetSizePolicy.visibleTaskCount(1_000, 50))
    }

    @Test
    fun `today learning is listed before chazarah when widget space is limited`() {
        val today = LocalDate.of(2026, 9, 8)
        val summary = WidgetSummaryBuilder.build(
            listOf(
                row(TaskType.CHAZARAH, today, "Review"),
                row(TaskType.LEARNING, today, "Learning"),
            ),
            today,
        )

        assertEquals(listOf("Learning", "Review"), summary.todayTasks.map { it.labelEnglish })
    }

    private fun row(type: TaskType, date: LocalDate, label: String) = WidgetTaskRow(
        scheduleNameEnglish = "Program",
        scheduleNameHebrew = "תוכנית",
        type = type,
        labelEnglish = label,
        labelHebrew = "",
        plannedDate = date,
    )
}

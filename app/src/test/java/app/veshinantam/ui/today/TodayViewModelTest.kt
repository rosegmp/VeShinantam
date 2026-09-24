package app.veshinantam.ui.today

import app.veshinantam.data.local.TodayTaskRow
import app.veshinantam.data.local.TaskEntity
import app.veshinantam.domain.model.MaterialType
import app.veshinantam.domain.model.TaskType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class TodayViewModelTest {
    private val today = LocalDate.of(2026, 9, 7)

    @Test
    fun `maps sections in required order and overdue tasks oldest first`() {
        val rows = listOf(
            row("new", TaskType.LEARNING, today),
            row("review", TaskType.CHAZARAH, today),
            row("newer-overdue", TaskType.LEARNING, today.minusDays(1)),
            row("oldest-overdue", TaskType.LEARNING, today.minusDays(3)),
            row("review-overdue", TaskType.CHAZARAH, today.minusDays(2)),
        )

        val tasks = mapTodayRows(rows, today).single().tasks

        assertEquals(
            listOf(
                TodaySection.NEW_LEARNING,
                TodaySection.CHAZARAH_TODAY,
                TodaySection.OVERDUE_LEARNING,
                TodaySection.OVERDUE_LEARNING,
                TodaySection.OVERDUE_CHAZARAH,
            ),
            tasks.map { it.section },
        )
        assertEquals(listOf("oldest-overdue", "newer-overdue"), tasks.filter { it.section == TodaySection.OVERDUE_LEARNING }.map { it.id })
    }

    @Test
    fun `past tasks completed today are not shown as overdue`() {
        val rows = listOf(
            row("completed-learning", TaskType.LEARNING, today.minusDays(2), completed = true),
            row("completed-review", TaskType.CHAZARAH, today.minusDays(1), completed = true),
            row("still-overdue", TaskType.LEARNING, today.minusDays(3)),
            row("today-completed", TaskType.LEARNING, today, completed = true),
        )

        val tasks = mapTodayRows(rows, today).single().tasks

        assertEquals(TodaySection.COMPLETED_TODAY, tasks.first { it.id == "completed-learning" }.section)
        assertEquals(TodaySection.COMPLETED_TODAY, tasks.first { it.id == "completed-review" }.section)
        assertEquals(TodaySection.OVERDUE_LEARNING, tasks.first { it.id == "still-overdue" }.section)
        assertEquals(TodaySection.NEW_LEARNING, tasks.first { it.id == "today-completed" }.section)
    }

    @Test
    fun `sorts section tasks by due date in either direction`() {
        val tasks = listOf(
            task("middle", "Berachos 3a", "ברכות ג.", today.minusDays(2)),
            task("oldest", "Berachos 2a", "ברכות ב.", today.minusDays(3)),
            task("newest", "Berachos 4a", "ברכות ד.", today.minusDays(1)),
        )

        assertEquals(
            listOf("oldest", "middle", "newest"),
            sortTodayTasks(tasks, TodaySortOrder.SCHEDULED_FIRST).map { it.id },
        )
        assertEquals(
            listOf("newest", "middle", "oldest"),
            sortTodayTasks(tasks, TodaySortOrder.NEWEST_DUE_FIRST).map { it.id },
        )
    }

    @Test
    fun `date sorting uses the synced task id instead of lexical reference order for ties`() {
        val tasks = listOf(
            task("04", "Yevamos 105b", "יבמות דף קה:", today),
            task("01", "Yevamos 90b", "יבמות דף צ:", today),
            task("03", "Yevamos 109b", "יבמות דף קט:", today),
            task("02", "Yevamos 45b", "יבמות דף מה:", today),
        )

        assertEquals(
            listOf("01", "02", "03", "04"),
            sortTodayTasks(tasks, TodaySortOrder.SCHEDULED_FIRST).map { it.id },
        )
        assertEquals(
            listOf("01", "02", "03", "04"),
            sortTodayTasks(tasks, TodaySortOrder.NEWEST_DUE_FIRST).map { it.id },
        )
    }

    @Test
    fun `reader navigation uses full learning sequence and resolves chazarah source`() {
        val learning = listOf(
            learningTask("learn-1", "Yevamos 44b", "יבמות דף מד:", today.minusDays(2)),
            learningTask("learn-2", "Yevamos 45a", "יבמות דף מה.", today.minusDays(1)),
            learningTask("learn-3", "Yevamos 45b", "יבמות דף מה:", today),
        )
        val review = task("review", "Yevamos 45a", "יבמות דף מה.", today).copy(
            scheduleId = "schedule",
            originalLearningDate = today.minusDays(1),
        )

        val neighbors = readerTaskNeighbors(learning, review)

        assertEquals("learn-1", neighbors.previous?.id)
        assertEquals("learn-3", neighbors.next?.id)
    }

    @Test
    fun `reference sorting follows the selected sefarim language`() {
        val tasks = listOf(
            task("english-first", "Berachos", "שבת", today),
            task("hebrew-first", "Shabbos", "ברכות", today),
        )

        assertEquals(
            listOf("english-first", "hebrew-first"),
            sortTodayTasks(tasks, TodaySortOrder.REFERENCE_ASCENDING).map { it.id },
        )
        assertEquals(
            listOf("hebrew-first", "english-first"),
            sortTodayTasks(tasks, TodaySortOrder.REFERENCE_ASCENDING, preferHebrewReference = true).map { it.id },
        )
    }

    @Test
    fun `reference sorting compares page numbers and amud sides naturally`() {
        val tasks = listOf(
            task("104b", "Yevamos 104b", "יבמות דף קד:", today),
            task("20a", "Yevamos 20a", "יבמות דף כ.", today),
            task("3b", "Yevamos 3b", "יבמות דף ג:", today),
            task("3a", "Yevamos 3a", "יבמות דף ג.", today),
        )

        assertEquals(
            listOf("3a", "3b", "20a", "104b"),
            sortTodayTasks(tasks, TodaySortOrder.REFERENCE_ASCENDING).map { it.id },
        )
        assertEquals(
            listOf("104b", "20a", "3b", "3a"),
            sortTodayTasks(tasks, TodaySortOrder.REFERENCE_DESCENDING, preferHebrewReference = true).map { it.id },
        )
    }

    @Test
    fun `reference sorting compares chapter and item numbers independently`() {
        val tasks = listOf(
            task("ten", "Berachos 1:10", "ברכות א:י", today),
            task("two", "Berachos 1:2", "ברכות א:ב", today),
            task("next", "Berachos 2:1", "ברכות ב:א", today),
        )

        assertEquals(
            listOf("two", "ten", "next"),
            sortTodayTasks(tasks, TodaySortOrder.REFERENCE_ASCENDING).map { it.id },
        )
    }

    private fun task(id: String, english: String, hebrew: String, date: LocalDate) = TodayTaskUi(
        id = id,
        labelEnglish = english,
        labelHebrew = hebrew,
        plannedDate = date,
        section = TodaySection.OVERDUE_LEARNING,
        isCompleted = false,
    )

    private fun row(id: String, type: TaskType, date: LocalDate, completed: Boolean = false) = TodayTaskRow(
        taskId = id,
        scheduleId = "schedule",
        scheduleNameEnglish = "Daf Yomi Bavli",
        scheduleNameHebrew = "דף יומי",
        scheduleCreatedAt = Instant.EPOCH,
        type = type,
        labelEnglish = "Berachos 2a",
        labelHebrew = "ברכות ב.",
        plannedDate = date,
        completedAt = if (completed) Instant.parse("2026-09-07T12:00:00Z") else null,
    )

    private fun learningTask(id: String, english: String, hebrew: String, date: LocalDate) = TaskEntity(
        id = id,
        stableKey = id,
        scheduleId = "schedule",
        type = TaskType.LEARNING,
        labelEnglish = english,
        labelHebrew = hebrew,
        materialType = MaterialType.AMUD,
        quantity = 1.0,
        plannedDate = date,
        originalLearningDate = date,
        reviewIdentity = null,
        generationRevision = 1,
        completedAt = null,
        completionLocalDate = null,
        completionZoneId = null,
    )
}

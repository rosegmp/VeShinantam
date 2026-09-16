package app.veshinantam.web

import app.veshinantam.shared.CanonicalDataValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WebStateTest {
    private val today = "2026-09-14"
    private val now = "2026-09-14T12:00:00Z"

    @Test
    fun legacyWebStateMapsToValidCanonicalData() {
        val state = WebAppState.sample(today)
        val canonical = state.toCanonical(now, today)

        assertTrue(CanonicalDataValidator.validate(canonical).isEmpty())
        assertEquals(state.schedules.map { it.id }, canonical.schedules.map { it.id })
        assertEquals(state.tasks.map { it.id }, canonical.tasks.map { it.id })
        assertNotNull(canonical.tasks.last().completedAt)
    }

    @Test
    fun versionTwoBackupRequiresMatchingCanonicalRecords() {
        val state = WebAppState.sample(today)
        val backup = WebBackup(state = state, canonical = state.toCanonical(now, today))

        assertEquals(state, backup.validStateOrNull())
        assertEquals(null, backup.copy(canonical = backup.canonical?.copy(tasks = emptyList())).validStateOrNull())
    }

    @Test
    fun versionOneBackupRemainsImportable() {
        val state = WebAppState.sample(today)
        assertEquals(state, WebBackup(version = 1, state = state).validStateOrNull())
    }

    @Test
    fun todayTasksMatchAndroidSectionsAndHideOldCompletions() {
        val tasks = listOf(
            StoredTask("today-learning", "daf-yomi", "Berachos 18", "ברכות יח", today, "LEARNING"),
            StoredTask("today-review", "daf-yomi", "Berachos 17", "ברכות יז", today, "CHAZARAH"),
            StoredTask("overdue-learning", "daf-yomi", "Berachos 16", "ברכות טז", "2026-09-12", "LEARNING"),
            StoredTask("overdue-review", "daf-yomi", "Berachos 15", "ברכות טו", "2026-09-11", "CHAZARAH"),
            StoredTask("completed-today", "daf-yomi", "Berachos 14", "ברכות יד", "2026-09-10", "LEARNING", true, completionLocalDate = today),
            StoredTask("completed-earlier", "daf-yomi", "Berachos 13", "ברכות יג", "2026-09-09", "LEARNING", true, completionLocalDate = "2026-09-13"),
        )

        val result = todayTasks(tasks, today, "SCHEDULED_FIRST", preferHebrew = false)

        assertEquals(
            listOf(
                WebTodaySection.NEW_LEARNING,
                WebTodaySection.CHAZARAH_TODAY,
                WebTodaySection.OVERDUE_LEARNING,
                WebTodaySection.OVERDUE_CHAZARAH,
                WebTodaySection.COMPLETED_TODAY,
            ),
            result.map { it.section },
        )
        assertTrue(result.none { it.task.id == "completed-earlier" })
    }

    @Test
    fun todayTasksHonorPersistedSortOrderAndReferenceLanguage() {
        val tasks = listOf(
            StoredTask("older", "daf-yomi", "Shabbos 2", "ברכות ב", "2026-09-10", "LEARNING"),
            StoredTask("newer", "daf-yomi", "Berachos 3", "שבת ג", "2026-09-12", "LEARNING"),
        )

        assertEquals(listOf("newer", "older"), todayTasks(tasks, today, "NEWEST_DUE_FIRST", false).map { it.task.id })
        assertEquals(listOf("newer", "older"), todayTasks(tasks, today, "REFERENCE_ASCENDING", false).map { it.task.id })
        assertEquals(listOf("older", "newer"), todayTasks(tasks, today, "REFERENCE_ASCENDING", true).map { it.task.id })
    }

    @Test
    fun todayReferenceSortingUsesNaturalDafAndAmudOrder() {
        val tasks = listOf(
            StoredTask("104b", "daf-yomi", "Yevamos 104b", "יבמות דף קד:", "2026-09-10", "LEARNING"),
            StoredTask("3b", "daf-yomi", "Yevamos 3b", "יבמות דף ג:", "2026-09-10", "LEARNING"),
            StoredTask("20a", "daf-yomi", "Yevamos 20a", "יבמות דף כ.", "2026-09-10", "LEARNING"),
            StoredTask("3a", "daf-yomi", "Yevamos 3a", "יבמות דף ג.", "2026-09-10", "LEARNING"),
        )

        assertEquals(listOf("3a", "3b", "20a", "104b"), todayTasks(tasks, today, "REFERENCE_ASCENDING", false).map { it.task.id })
    }

    @Test
    fun calendarSummariesDistinguishStatusAndHonorFilters() {
        val tasks = listOf(
            StoredTask("learning-done", "daf-yomi", "Berachos 2", "ברכות ב", today, "LEARNING", completed = true),
            StoredTask("review-open", "daf-yomi", "Berachos 2", "ברכות ב", today, "CHAZARAH"),
            StoredTask("other-open", "mishnah", "Peah 1", "פאה א", today, "LEARNING"),
            StoredTask("tomorrow-done", "daf-yomi", "Berachos 3", "ברכות ג", "2026-09-15", "LEARNING", completed = true),
        )

        val all = calendarDaySummaries(tasks)
        assertEquals(WebCalendarDayStatus.PARTIAL, all.getValue(today).status)
        assertEquals(1, all.getValue(today).completedCount)
        assertEquals(3, all.getValue(today).taskCount)
        assertEquals(WebCalendarDayStatus.COMPLETE, all.getValue("2026-09-15").status)

        val learning = calendarDaySummaries(tasks, scheduleId = "daf-yomi", taskType = "LEARNING")
        assertEquals(WebCalendarDayStatus.COMPLETE, learning.getValue(today).status)
        assertEquals(1, learning.getValue(today).taskCount)

        val chazarah = calendarDaySummaries(tasks, scheduleId = "daf-yomi", taskType = "CHAZARAH")
        assertEquals(WebCalendarDayStatus.INCOMPLETE, chazarah.getValue(today).status)
    }

    @Test
    fun calendarRangeCellsSupportsHebrewMonthsCrossingGregorianBoundaries() {
        val cells = calendarRangeCells("2026-09-12", "2026-10-10")
        val dates = cells.mapNotNull { it.isoDate }

        assertEquals("2026-09-12", dates.first())
        assertEquals("2026-10-10", dates.last())
        assertEquals(29, dates.size)
        assertEquals(35, cells.size)
    }

    @Test
    fun bulkCompletionOnlyCompletesPastTasksOfRequestedType() {
        val state = WebAppState(
            schedules = listOf(StoredSchedule("schedule", "Daf Yomi", "Gemara", 1)),
            tasks = listOf(
                StoredTask("past-learning", "schedule", "Berachos 2", "ברכות ב", "2026-09-13", "LEARNING"),
                StoredTask("today-learning", "schedule", "Berachos 3", "ברכות ג", today, "LEARNING"),
                StoredTask("past-review", "schedule", "Berachos 1", "ברכות א", "2026-09-12", "CHAZARAH"),
                StoredTask("already-done", "schedule", "Berachos 1", "ברכות א", "2026-09-11", "LEARNING", completed = true, completedAt = "2026-09-12T10:00:00Z", completionLocalDate = "2026-09-12", completionZoneId = "UTC"),
            ),
        )

        val updated = completePastTasks(state, "schedule", "LEARNING", today, now, "America/New_York")

        val completed = updated.tasks.associateBy { it.id }
        assertTrue(completed.getValue("past-learning").completed)
        assertEquals(today, completed.getValue("past-learning").completionLocalDate)
        assertEquals("America/New_York", completed.getValue("past-learning").completionZoneId)
        assertTrue(!completed.getValue("today-learning").completed)
        assertTrue(!completed.getValue("past-review").completed)
        assertEquals("2026-09-12T10:00:00Z", completed.getValue("already-done").completedAt)
    }
}

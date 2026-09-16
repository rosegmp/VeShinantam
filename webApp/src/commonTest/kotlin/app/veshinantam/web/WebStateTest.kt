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
}

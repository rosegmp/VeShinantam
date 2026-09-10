package app.veshinantam.data.backup

import app.veshinantam.data.local.ProgressGoalEntity
import app.veshinantam.data.local.ScheduleEntity
import app.veshinantam.data.local.ScheduleExclusionEntity
import app.veshinantam.data.local.TaskEntity
import app.veshinantam.domain.model.MaterialType
import app.veshinantam.domain.model.MissedWorkBehavior
import app.veshinantam.domain.model.ScheduleKind
import app.veshinantam.domain.model.ScheduleState
import app.veshinantam.domain.model.TaskType
import app.veshinantam.localization.AppLanguage
import app.veshinantam.localization.PrimaryCalendar
import app.veshinantam.localization.SefarimLanguage
import app.veshinantam.notifications.ReminderPreference
import app.veshinantam.ui.today.TodaySortOrder
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupJsonTest {
    @Test
    fun `current backup round trips all persisted fields`() {
        val schedule = ScheduleEntity(
            id = "schedule-1", nameEnglish = "Yevamos", nameHebrew = "יבמות", kind = ScheduleKind.CUSTOM,
            sourceType = "GEMARA", materialType = MaterialType.AMUD, presetId = null,
            startDate = LocalDate.parse("2025-11-26"), targetDate = LocalDate.parse("2026-12-01"), dailyQuantity = 1,
            selectedWeekdays = "SUNDAY,MONDAY", chazarahDayOffsets = "1,7,30,90", repeatsAnnually = true,
            officialOraysaChazarah = true, missedWorkBehavior = MissedWorkBehavior.SHIFT_FORWARD,
            state = ScheduleState.ACTIVE, generationRevision = 2, createdAt = Instant.parse("2025-11-01T12:00:00Z"),
        )
        val task = TaskEntity(
            id = "task-1", stableKey = "learning:1", scheduleId = schedule.id, type = TaskType.LEARNING,
            labelEnglish = "Yevamos 2a", labelHebrew = "יבמות ב.", materialType = MaterialType.AMUD, quantity = 1.0,
            plannedDate = schedule.startDate, originalLearningDate = schedule.startDate, reviewIdentity = null,
            generationRevision = 2, completedAt = Instant.parse("2025-11-26T14:00:00Z"),
            completionLocalDate = schedule.startDate, completionZoneId = "America/New_York",
        )
        val expected = BackupPayload(
            schedules = listOf(schedule), exclusions = listOf(ScheduleExclusionEntity(schedule.id, LocalDate.parse("2025-12-25"))),
            tasks = listOf(task), goals = listOf(ProgressGoalEntity("STREAK", 30.0)),
            preferences = BackupPreferences(
                AppLanguage.HEBREW, SefarimLanguage.BOTH, PrimaryCalendar.HEBREW, listOf(1, 7, 30, 90),
                ReminderPreference(true, 19, 15), TodaySortOrder.REFERENCE_ASCENDING, true,
            ),
        )

        val actual = BackupJson.decode(BackupJson.encode(expected))

        assertEquals(expected, actual)
    }

    @Test
    fun `version one backup restores with safe defaults for newer settings`() {
        val restored = BackupJson.decode(fixture("backups/version-1.json"))

        assertEquals(false, restored.preferences.automaticPresetUpdates)
        assertEquals(AppLanguage.ENGLISH, restored.preferences.appLanguage)
        assertEquals(emptyList<ScheduleEntity>(), restored.schedules)
    }

    @Test
    fun `version two fixture retains schedules tasks history exclusions goals and preferences`() {
        val restored = BackupJson.decode(fixture("backups/version-2.json"))

        assertEquals(1, restored.schedules.size)
        assertEquals("legacy-schedule", restored.schedules.single().id)
        assertEquals(true, restored.schedules.single().officialOraysaChazarah)
        assertEquals(1, restored.tasks.size)
        assertEquals(Instant.parse("2025-11-26T14:00:00Z"), restored.tasks.single().completedAt)
        assertEquals(LocalDate.parse("2025-12-25"), restored.exclusions.single().date)
        assertEquals(30.0, restored.goals.single().target, 0.0)
        assertEquals(true, restored.preferences.automaticPresetUpdates)
        assertEquals(restored, BackupJson.decode(BackupJson.encode(restored)))
    }

    @Test
    fun `future backup version is rejected`() {
        val error = assertThrows(BackupException::class.java) {
            BackupJson.decode("{\"format\":\"app.veshinantam.backup\",\"version\":3}")
        }
        assertEquals("Backup version 3 is not supported by this app version.", error.message)
    }

    @Test
    fun `unrelated json file is rejected`() {
        assertThrows(BackupException::class.java) { BackupJson.decode("{\"hello\":\"world\"}") }
    }

    private fun emptyPayload() = BackupPayload(
        schedules = emptyList(), exclusions = emptyList(), tasks = emptyList(), goals = emptyList(),
        preferences = BackupPreferences(
            AppLanguage.ENGLISH, SefarimLanguage.BOTH, PrimaryCalendar.GREGORIAN, listOf(1, 7, 30, 90),
            ReminderPreference(), TodaySortOrder.SCHEDULED_FIRST,
        ),
    )

    private fun fixture(path: String): String = requireNotNull(javaClass.classLoader?.getResource(path)) {
        "Missing backup compatibility fixture: $path"
    }.readText()
}

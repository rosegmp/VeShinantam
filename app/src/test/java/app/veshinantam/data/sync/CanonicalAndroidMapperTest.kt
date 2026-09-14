package app.veshinantam.data.sync

import app.veshinantam.data.local.ScheduleEntity
import app.veshinantam.data.local.TaskEntity
import app.veshinantam.domain.model.MaterialType
import app.veshinantam.domain.model.MissedWorkBehavior
import app.veshinantam.domain.model.ScheduleKind
import app.veshinantam.domain.model.ScheduleState
import app.veshinantam.domain.model.TaskType
import app.veshinantam.shared.CanonicalDataValidator
import app.veshinantam.shared.CanonicalPreferences
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalAndroidMapperTest {
    @Test
    fun exportPreservesAndroidScheduleAndCompletionDetails() {
        val now = Instant.parse("2026-09-14T12:00:00Z")
        val schedule = ScheduleEntity(
            id = "schedule-1", nameEnglish = "Daf Yomi", nameHebrew = "דף יומי",
            kind = ScheduleKind.PRESET, sourceType = "GEMARA", materialType = MaterialType.DAF,
            presetId = "daf-yomi-bavli", startDate = LocalDate.parse("2026-09-14"), targetDate = null,
            dailyQuantity = 1, selectedWeekdays = "SUNDAY,MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY",
            chazarahDayOffsets = "1,7,30", repeatsAnnually = true, officialOraysaChazarah = false,
            missedWorkBehavior = MissedWorkBehavior.KEEP_FIXED_OVERDUE, state = ScheduleState.ACTIVE,
            generationRevision = 3, createdAt = now,
        )
        val task = TaskEntity(
            id = "task-1", stableKey = "learning:1", scheduleId = schedule.id, type = TaskType.LEARNING,
            labelEnglish = "Berachos 2", labelHebrew = "ברכות דף ב׳", materialType = MaterialType.DAF,
            quantity = 1.0, plannedDate = LocalDate.parse("2026-09-14"), originalLearningDate = LocalDate.parse("2026-09-14"),
            reviewIdentity = null, generationRevision = 3, completedAt = now,
            completionLocalDate = LocalDate.parse("2026-09-14"), completionZoneId = "America/New_York",
        )

        val exported = CanonicalAndroidMapper.export(
            schedules = listOf(schedule), exclusions = emptyList(), tasks = listOf(task), goals = emptyList(),
            preferences = CanonicalPreferences(updatedAt = now.toString()), now = now,
        )

        assertTrue(CanonicalDataValidator.validate(exported).isEmpty())
        assertEquals(3, exported.schedules.single().generationRevision)
        assertEquals("America/New_York", exported.tasks.single().completionZoneId)
        assertEquals(listOf(1, 7, 30), exported.schedules.single().chazarahDayOffsets)
        assertEquals(schedule, CanonicalAndroidMapper.schedule(exported.schedules.single()))
        assertEquals(task, CanonicalAndroidMapper.task(exported.tasks.single()))
    }
}

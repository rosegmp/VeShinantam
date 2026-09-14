package app.veshinantam.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CanonicalDataTest {
    private val timestamp = "2026-09-14T12:00:00Z"

    @Test
    fun canonicalDataRoundTripsWithoutDroppingCompletionMetadata() {
        val schedule = CanonicalSchedule(
            id = "schedule-1",
            nameEnglish = "Daf Yomi Bavli",
            nameHebrew = "דף יומי בבלי",
            kind = CanonicalScheduleKind.PRESET,
            materialType = CanonicalMaterialType.DAF,
            presetId = "daf-yomi-bavli",
            startDate = "2026-09-14",
            createdAt = timestamp,
            updatedAt = timestamp,
        )
        val task = CanonicalTask(
            id = "task-1",
            stableKey = "schedule-1:learning:1",
            scheduleId = schedule.id,
            type = CanonicalTaskType.LEARNING,
            labelEnglish = "Berachos 2",
            labelHebrew = "ברכות דף ב׳",
            materialType = CanonicalMaterialType.DAF,
            plannedDate = "2026-09-14",
            originalLearningDate = "2026-09-14",
            completedAt = timestamp,
            completionLocalDate = "2026-09-14",
            completionZoneId = "America/New_York",
            updatedAt = timestamp,
        )
        val value = CanonicalDataSet(
            schedules = listOf(schedule),
            tasks = listOf(task),
            preferences = CanonicalPreferences(updatedAt = timestamp),
        )

        assertEquals(value, CanonicalDataCodec.decode(CanonicalDataCodec.encode(value)))
    }

    @Test
    fun validationRejectsOrphansAndPartialCompletionMetadata() {
        val invalid = CanonicalDataSet(
            tasks = listOf(
                CanonicalTask(
                    id = "task-1",
                    stableKey = "task-1",
                    scheduleId = "missing",
                    type = CanonicalTaskType.LEARNING,
                    labelEnglish = "Unit",
                    labelHebrew = "יחידה",
                    plannedDate = "2026-09-14",
                    originalLearningDate = "2026-09-14",
                    completedAt = timestamp,
                    updatedAt = timestamp,
                ),
            ),
            preferences = CanonicalPreferences(updatedAt = timestamp),
        )

        val errors = CanonicalDataValidator.validate(invalid)
        assertTrue(errors.any { "missing schedule" in it })
        assertTrue(errors.any { "Incomplete completion metadata" in it })
        assertFailsWith<IllegalArgumentException> { CanonicalDataCodec.decode(CanonicalDataCodec.encode(invalid)) }
    }
}

package app.veshinantam.web

import app.veshinantam.shared.CanonicalDataValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString

class WebStateTest {
    private val today = "2026-09-14"
    private val now = "2026-09-14T12:00:00Z"

    @Test
    fun appBadgeMatchesAndroidActiveIncompleteDueCount() {
        val state = WebAppState(
            schedules = listOf(
                StoredSchedule("active", "Active", "Mishnah", 2),
                StoredSchedule("paused", "Paused", "Mishnah", 2, active = false),
                StoredSchedule("archived", "Archived", "Mishnah", 2, archived = true),
            ),
            tasks = listOf(
                StoredTask("overdue", "active", "A", "א", "2026-09-13", "LEARNING"),
                StoredTask("today", "active", "B", "ב", today, "CHAZARAH"),
                StoredTask("done", "active", "C", "ג", today, "LEARNING", completed = true),
                StoredTask("future", "active", "D", "ד", "2026-09-15", "LEARNING"),
                StoredTask("paused-task", "paused", "E", "ה", today, "LEARNING"),
                StoredTask("archived-task", "archived", "F", "ו", today, "LEARNING"),
            ),
        )

        assertEquals(2, dueBadgeCount(state, today))
    }

    @Test
    fun printableScheduleMatchesAndroidRangeAndActiveScheduleRules() {
        val state = WebAppState(
            schedules = listOf(
                StoredSchedule("active", "Mishnah Yomis", "Mishnah", 2, nameHebrew = "משנה יומית"),
                StoredSchedule("paused", "Paused", "Mishnah", 2, active = false),
                StoredSchedule("archived", "Archived", "Mishnah", 2, archived = true),
            ),
            tasks = listOf(
                StoredTask("before", "active", "Before", "לפני", "2026-09-13", "LEARNING"),
                StoredTask("review", "active", "Peah 2:1–2", "פאה ב׳:א׳–ב׳", today, "CHAZARAH", completed = true),
                StoredTask("learning", "active", "Peah 2:3–4", "פאה ב׳:ג׳–ד׳", today, "LEARNING"),
                StoredTask("paused-task", "paused", "Hidden", "מוסתר", today, "LEARNING"),
                StoredTask("archived-task", "archived", "Hidden", "מוסתר", today, "LEARNING"),
                StoredTask("last", "active", "Peah 3:1–2", "פאה ג׳:א׳–ב׳", "2026-09-20", "LEARNING"),
                StoredTask("after", "active", "After", "אחרי", "2026-09-21", "LEARNING"),
            ),
            sefarimLanguage = "BOTH",
        )

        val printable = buildPrintableSchedule(state, today, 7)

        assertEquals("2026-09-20", printable.endDate)
        assertEquals(listOf("review", "learning", "last"), printable.rows.map { row ->
            state.tasks.single { it.dueDate == row.date && it.referenceEnglish in row.reference }.id
        })
        assertEquals("Mishnah Yomis", printable.rows.first().scheduleName)
        assertEquals("Peah 2:1–2 · פאה ב׳:א׳–ב׳", printable.rows.first().reference)
        assertEquals(listOf(true, false, false), printable.rows.map { it.completed })
    }

    @Test
    fun printableScheduleLocalizesHebrewNamesAndReferences() {
        val sample = WebAppState.sample(today)
        val state = sample.copy(
            schedules = sample.schedules.map { schedule ->
                if (schedule.id == "daf-yomi") schedule.copy(nameHebrew = "דף יומי") else schedule
            },
            language = "he",
            sefarimLanguage = "HEBREW",
        )

        val printable = buildPrintableSchedule(state, today, 30)
        val learning = printable.rows.single { it.reference == "ברכות דף י״ח" }

        assertEquals("דף יומי", learning.scheduleName)
        assertEquals("לימוד חדש", learning.taskType)
    }

    @Test
    fun printableScheduleNormalizesLegacyHebrewReferences() {
        val state = WebAppState(
            schedules = listOf(StoredSchedule("daf", "Daf Yomi", "Gemara", 1)),
            tasks = listOf(StoredTask("legacy", "daf", "Yevamos 104b", "יבמות 104:", today, "LEARNING")),
            language = "he",
            sefarimLanguage = "HEBREW",
        )

        assertEquals("יבמות דף קד:", buildPrintableSchedule(state, today, 7).rows.single().reference)
        assertEquals("יבמות דף קד:", normalizedHebrewReference("Yevamos 104b", "יבמות 104:"))
    }

    @Test
    fun portableCanonicalBackupRebuildsWebState() {
        val source = WebAppState.sample(today).copy(
            reminderEnabled = true,
            reminderHour = 7,
            reminderMinute = 30,
            automaticPresetUpdates = true,
            goals = listOf(StoredGoal("STREAK", 30.0, now)),
        )
        val canonical = source.toCanonical(now, today)
        val raw = app.veshinantam.shared.CanonicalDataCodec.encode(canonical)

        val restored = assertNotNull(decodeImportedBackup(raw, now, today))

        assertEquals(source.schedules.map { it.id }, restored.schedules.map { it.id })
        assertEquals(source.tasks.map { it.id }, restored.tasks.map { it.id })
        assertEquals(source.goals, restored.goals)
        assertEquals(true, restored.reminderEnabled)
        assertEquals(true, restored.automaticPresetUpdates)
        assertEquals(canonical, restored.toCanonical(now, today))
    }

    @Test
    fun importsLegacyAndroidBackupIntoWebState() {
        val raw = """
            {
              "format":"app.veshinantam.backup","version":2,"createdAt":"$now",
              "schedules":[{
                "id":"legacy","nameEnglish":"Yevamos","nameHebrew":"יבמות","kind":"CUSTOM",
                "sourceType":"GEMARA","materialType":"AMUD","presetId":null,"startDate":"$today","targetDate":null,
                "dailyQuantity":1,"selectedWeekdays":"SUNDAY,MONDAY","chazarahDayOffsets":"1,7",
                "repeatsAnnually":false,"officialOraysaChazarah":false,"missedWorkBehavior":"KEEP_FIXED_OVERDUE",
                "state":"ACTIVE","generationRevision":1,"createdAt":"$now"
              }],
              "exclusions":[],"tasks":[],"goals":[{"kind":"STREAK","target":7.0}],
              "preferences":{
                "appLanguage":"HEBREW","sefarimLanguage":"BOTH","primaryCalendar":"HEBREW",
                "defaultChazarahOffsets":[1,7],"reminderEnabled":true,"reminderHour":19,"reminderMinute":15,
                "todaySortOrder":"REFERENCE_ASCENDING","automaticPresetUpdates":true
              }
            }
        """.trimIndent()

        val restored = assertNotNull(decodeImportedBackup(raw, now, today))

        assertEquals("legacy", restored.schedules.single().id)
        assertEquals(setOf(0, 1), restored.schedules.single().weekdays)
        assertEquals("he", restored.language)
        assertEquals(true, restored.reminderEnabled)
        assertEquals(listOf(StoredGoal("STREAK", 7.0, now)), restored.goals)
    }

    @Test
    fun importStillAcceptsLegacyWebEnvelope() {
        val state = WebAppState.sample(today)
        val raw = kotlinx.serialization.json.Json.encodeToString(
            WebBackup(state = state, canonical = state.toCanonical(now, today)),
        )

        assertEquals(state, decodeImportedBackup(raw, now, today))
    }

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
    fun allCanonicalPreferencesRoundTripWithoutPlatformClobbering() {
        val state = WebAppState.sample(today).copy(
            language = "he",
            sefarimLanguage = "HEBREW",
            primaryCalendar = "HEBREW",
            defaultChazarahOffsets = listOf(2, 14, 45),
            reminderEnabled = true,
            reminderHour = 6,
            reminderMinute = 35,
            todaySortOrder = "REFERENCE_ASCENDING",
            automaticPresetUpdates = true,
            preferencesRevision = 12,
        )

        val canonical = state.toCanonical(now, today).preferences

        assertEquals(true, canonical.reminderEnabled)
        assertEquals(6, canonical.reminderHour)
        assertEquals(35, canonical.reminderMinute)
        assertEquals(true, canonical.automaticPresetUpdates)
        assertEquals(12, canonical.revision)
        assertEquals(state, WebBackup(state = state, canonical = state.toCanonical(now, today)).validStateOrNull())
    }

    @Test
    fun olderVersionTwoStateRecoversPlatformPreferencesFromCanonicalData() {
        val state = WebAppState.sample(today)
        val canonical = state.toCanonical(now, today).copy(
            preferences = state.toCanonical(now, today).preferences.copy(
                reminderEnabled = true,
                reminderHour = 7,
                reminderMinute = 15,
                automaticPresetUpdates = true,
                revision = 9,
            ),
        )

        val restored = WebBackup(state = state, canonical = canonical).validStateOrNull()

        assertEquals(true, restored?.reminderEnabled)
        assertEquals(7, restored?.reminderHour)
        assertEquals(15, restored?.reminderMinute)
        assertEquals(true, restored?.automaticPresetUpdates)
        assertEquals(9, restored?.preferencesRevision)
    }

    @Test
    fun backupRejectsInvalidReminderAndChazarahPreferences() {
        assertEquals(null, WebBackup(version = 1, state = WebAppState.sample(today).copy(reminderHour = 24)).validStateOrNull())
        assertEquals(null, WebBackup(version = 1, state = WebAppState.sample(today).copy(defaultChazarahOffsets = listOf(7, 7))).validStateOrNull())
    }

    @Test
    fun olderVersionTwoBackupRecoversCanonicalExclusions() {
        val state = WebAppState.sample(today)
        val canonical = state.toCanonical(now, today).copy(
            exclusions = listOf(app.veshinantam.shared.CanonicalExclusion("daf-yomi", "2026-09-20", now)),
        )

        val restored = WebBackup(version = 2, state = state, canonical = canonical).validStateOrNull()

        assertEquals(listOf("daf-yomi:2026-09-20"), restored?.exclusions?.map { it.id })
    }

    @Test
    fun goalsRoundTripThroughCanonicalBackup() {
        val state = WebAppState.sample(today).copy(
            goals = listOf(
                StoredGoal("COMPLETION", 85.0, now),
                StoredGoal("STREAK", 30.0, now),
                StoredGoal("LEARNING_UNITS", 100.0, now),
            ),
        )

        val canonical = state.toCanonical(now, today)

        assertEquals(state.goals.map { it.kind }, canonical.goals.map { it.kind })
        assertEquals(state, WebBackup(state = state, canonical = canonical).validStateOrNull())
    }

    @Test
    fun olderVersionTwoBackupRecoversCanonicalGoals() {
        val state = WebAppState.sample(today)
        val canonical = state.toCanonical(now, today).copy(
            goals = listOf(app.veshinantam.shared.CanonicalGoal("STREAK", kind = "STREAK", target = 7.0, updatedAt = now)),
        )

        val restored = WebBackup(version = 2, state = state, canonical = canonical).validStateOrNull()

        assertEquals(listOf(StoredGoal("STREAK", 7.0, now)), restored?.goals)
    }

    @Test
    fun backupRejectsInvalidProgressGoals() {
        val state = WebAppState.sample(today).copy(goals = listOf(StoredGoal("COMPLETION", 101.0, now)))

        assertEquals(null, WebBackup(version = 1, state = state).validStateOrNull())
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
    fun readerNavigationUsesTheFullLearningSequenceAndResolvesChazarahSource() {
        val learning = listOf(
            StoredTask("learn-1", "daf-yomi", "Yevamos 44b", "יבמות דף מד:", "2026-09-10", "LEARNING", originalLearningDate = "2026-09-10"),
            StoredTask("learn-2", "daf-yomi", "Yevamos 45a", "יבמות דף מה.", "2026-09-11", "LEARNING", originalLearningDate = "2026-09-11"),
            StoredTask("learn-3", "daf-yomi", "Yevamos 45b", "יבמות דף מה:", "2026-09-12", "LEARNING", originalLearningDate = "2026-09-12"),
        )
        val review = StoredTask(
            "review", "daf-yomi", "Yevamos 45a", "יבמות דף מה.", today, "CHAZARAH",
            originalLearningDate = "2026-09-11",
        )

        val neighbors = readerTaskNeighbors(learning + review, review)

        assertEquals("learn-1", neighbors.previous?.id)
        assertEquals("learn-3", neighbors.next?.id)
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

    @Test
    fun presetMetadataUsesCanonicalSourceAndMaterialType() {
        val state = WebAppState(
            schedules = listOf(
                StoredSchedule(
                    id = "preset",
                    name = "Daf Yomi Bavli",
                    material = "DAF",
                    pace = 1,
                    presetId = "daf-yomi-bavli",
                    startDate = today,
                    sourceType = "PRESET:2026.09.10-8",
                    materialType = "DAF",
                ),
            ),
        )

        val canonical = state.toCanonical(now, today).schedules.single()

        assertEquals("PRESET:2026.09.10-8", canonical.sourceType)
        assertEquals(app.veshinantam.shared.CanonicalMaterialType.DAF, canonical.materialType)
        assertEquals("daf-yomi-bavli", canonical.presetId)
    }

    @Test
    fun futureEditPreservesHistoryAndRegeneratesOnlyOpenFutureWork() {
        val schedule = StoredSchedule(
            id = "schedule",
            name = "Mishnah Yomis",
            material = "Mishnah",
            pace = 2,
            weekdays = (0..6).toSet(),
            chazarahOffsets = listOf(1),
            materialType = "MISHNAH",
        )
        val state = WebAppState(
            schedules = listOf(schedule),
            tasks = listOf(
                StoredTask("history", "schedule", "Ohalos 2:1", "אהלות ב:א", "2026-09-13", "LEARNING", stableKey = "learning:u1:2026-09-13"),
                StoredTask("completed", "schedule", "Ohalos 2:2", "אהלות ב:ב", today, "LEARNING", completed = true, stableKey = "learning:u2:$today"),
                StoredTask("future-1", "schedule", "Ohalos 2:3", "אהלות ב:ג", today, "LEARNING", stableKey = "learning:u3:$today"),
                StoredTask("future-2", "schedule", "Ohalos 2:4", "אהלות ב:ד", "2026-09-15", "LEARNING", stableKey = "learning:u4:2026-09-15"),
                StoredTask("review-1", "schedule", "Ohalos 2:3", "אהלות ב:ג", "2026-09-15", "CHAZARAH", stableKey = "legacy-review", originalLearningDate = today, reviewIdentity = "day:1"),
            ),
        )

        val updated = editFutureSchedule(
            state = state,
            scheduleId = "schedule",
            today = today,
            edit = WebFutureScheduleEdit(
                startDate = "2026-09-16",
                dailyQuantity = 1,
                selectedWeekdays = setOf(1, 2, 3, 4, 5),
            ),
            updatedAt = now,
        )

        assertEquals(listOf("history", "completed"), updated.tasks.take(2).map { it.id })
        assertEquals(
            listOf("2026-09-16", "2026-09-17"),
            updated.tasks.filter { it.type == "LEARNING" && !it.completed && it.dueDate >= today }.map { it.dueDate },
        )
        assertEquals(listOf("2026-09-17", "2026-09-18"), updated.tasks.filter { it.type == "CHAZARAH" }.map { it.dueDate })
        assertEquals(2, updated.schedules.single().generationRevision)
        assertEquals(setOf(1, 2, 3, 4, 5), updated.schedules.single().weekdays)
    }

    @Test
    fun futureEditSupportsFinishByDistribution() {
        val state = WebAppState(
            schedules = listOf(StoredSchedule("schedule", "Plan", "Other", 1, chazarahOffsets = emptyList())),
            tasks = (1..5).map { index ->
                StoredTask("task-$index", "schedule", "Unit $index", "יחידה $index", today, "LEARNING")
            },
        )

        val updated = editFutureSchedule(
            state,
            "schedule",
            today,
            WebFutureScheduleEdit(today, 1, "2026-09-16", setOf(1, 2, 3, 4, 5)),
            now,
        )

        assertEquals(
            listOf("2026-09-14", "2026-09-14", "2026-09-15", "2026-09-15", "2026-09-16"),
            updated.tasks.map { it.dueDate },
        )
        assertEquals("2026-09-16", updated.schedules.single().targetDate)
        assertEquals(0, updated.schedules.single().pace)
    }

    @Test
    fun exclusionsRoundTripAndRemainActiveDuringFutureEdits() {
        val state = WebAppState(
            schedules = listOf(StoredSchedule("schedule", "Plan", "Other", 1, chazarahOffsets = emptyList())),
            tasks = listOf(
                StoredTask("task-1", "schedule", "Unit 1", "יחידה 1", today, "LEARNING"),
                StoredTask("task-2", "schedule", "Unit 2", "יחידה 2", today, "LEARNING"),
            ),
            exclusions = listOf(StoredExclusion("schedule", "2026-09-15", now)),
        )

        val canonical = state.toCanonical(now, today)
        val backup = WebBackup(state = state, canonical = canonical)
        val updated = editFutureSchedule(
            state,
            "schedule",
            today,
            WebFutureScheduleEdit(today, 1, selectedWeekdays = (0..6).toSet()),
            now,
        )

        assertEquals(listOf("schedule:2026-09-15"), canonical.exclusions.map { it.id })
        assertEquals(state, backup.validStateOrNull())
        assertEquals(listOf("2026-09-14", "2026-09-16"), updated.tasks.map { it.dueDate })
    }
}

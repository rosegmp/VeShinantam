package app.veshinantam.data

import app.veshinantam.data.local.ScheduleDao
import app.veshinantam.data.local.ScheduleEntity
import app.veshinantam.data.local.TaskEntity
import app.veshinantam.domain.model.ChazarahPattern
import app.veshinantam.domain.model.MaterialType
import app.veshinantam.domain.model.MissedWorkBehavior
import app.veshinantam.domain.model.ScheduleKind
import app.veshinantam.domain.model.ScheduleState
import app.veshinantam.domain.model.TaskType
import app.veshinantam.domain.material.PresetCatalog
import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.reflect.Proxy
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking

class ScheduleRepositoryTest {
    @Test
    fun `bulk completion sends learning and chazarah as distinct atomic DAO updates`() = runBlocking {
        val capturedCalls = mutableListOf<List<Any?>>()
        val dao = Proxy.newProxyInstance(
            ScheduleDao::class.java.classLoader,
            arrayOf(ScheduleDao::class.java),
        ) { _, method, arguments ->
            if (method.name == "completePastTasks") {
                capturedCalls += arguments.orEmpty().dropLast(1)
                if (arguments?.get(1) == TaskType.LEARNING) 3 else 2
            } else {
                error("Unexpected DAO method ${method.name}")
            }
        } as ScheduleDao
        val completionInstant = Instant.parse("2026-09-08T12:00:00Z")
        val repository = ScheduleRepository(
            dao = dao,
            clock = Clock.fixed(completionInstant, ZoneOffset.UTC),
            zoneProvider = { ZoneOffset.UTC },
        )

        assertEquals(3, repository.completePastLearning("program-1"))
        assertEquals(2, repository.completePastChazarah("program-1"))
        assertEquals(
            listOf(
                listOf("program-1", TaskType.LEARNING, LocalDate.of(2026, 9, 8), completionInstant, LocalDate.of(2026, 9, 8), "Z"),
                listOf("program-1", TaskType.CHAZARAH, LocalDate.of(2026, 9, 8), completionInstant, LocalDate.of(2026, 9, 8), "Z"),
            ),
            capturedCalls,
        )
    }

    @Test
    fun `custom plan previews learning and immediate review workload`() {
        val unusedDao = Proxy.newProxyInstance(
            ScheduleDao::class.java.classLoader,
            arrayOf(ScheduleDao::class.java),
        ) { _, method, _ -> error("DAO method ${method.name} should not be called while previewing") } as ScheduleDao
        val repository = ScheduleRepository(
            dao = unusedDao,
            clock = Clock.fixed(Instant.parse("2026-09-07T12:00:00Z"), ZoneOffset.UTC),
            zoneProvider = { ZoneOffset.UTC },
        )

        val plan = repository.buildCustomPlan(
            CustomScheduleDraft(
                name = "  הספר שלי  ",
                units = (1..3).map { ScheduleUnitInput("יחידה $it") },
                startDate = LocalDate.of(2026, 9, 7),
                dailyQuantity = 2,
                selectedWeekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
                chazarahPattern = ChazarahPattern(listOf(1), repeatsAnnually = false),
            ),
        )

        assertEquals("הספר שלי", plan.schedule.nameEnglish)
        assertEquals("", plan.schedule.nameHebrew)
        assertEquals("יחידה 1", plan.tasks.first().labelEnglish)
        assertEquals("", plan.tasks.first().labelHebrew)
        assertEquals(3, plan.learningCount)
        assertEquals(3, plan.reviewCount)
        assertEquals(LocalDate.of(2026, 9, 8), plan.completionDate)
        assertEquals(
            listOf(LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 14)),
            plan.tasks.filter { it.reviewIdentity != null }.map { it.plannedDate },
        )
    }

    @Test
    fun `preset plan stores catalog identity and bilingual references`() {
        val unusedDao = Proxy.newProxyInstance(
            ScheduleDao::class.java.classLoader,
            arrayOf(ScheduleDao::class.java),
        ) { _, method, _ -> error("DAO method ${method.name} should not be called while previewing") } as ScheduleDao
        val repository = ScheduleRepository(
            dao = unusedDao,
            clock = Clock.fixed(Instant.parse("2026-09-08T12:00:00Z"), ZoneOffset.UTC),
            zoneProvider = { ZoneOffset.UTC },
        )
        val source = PresetCatalog.programs.first()
        val program = source.copy(units = source.units.subList(source.currentIndex, source.currentIndex + 2), currentIndex = 0)

        val plan = repository.buildPresetPlan(
            PresetScheduleDraft(
                program = program,
                startDate = LocalDate.of(2026, 9, 8),
                chazarahPattern = ChazarahPattern(listOf(1), repeatsAnnually = false),
            ),
        )

        assertEquals(ScheduleKind.PRESET, plan.schedule.kind)
        assertEquals("daf-yomi-bavli", plan.schedule.presetId)
        assertEquals("PRESET:${PresetCatalog.VERSION}", plan.schedule.sourceType)
        assertEquals("1", plan.schedule.chazarahDayOffsets)
        assertEquals(false, plan.schedule.repeatsAnnually)
        assertEquals("Chullin 131", plan.tasks.first().labelEnglish)
        assertEquals("חולין דף קלא.", plan.tasks.first().labelHebrew)
        assertEquals(2, plan.learningCount)
        assertEquals(2, plan.reviewCount)

        val noChazarahPlan = repository.buildPresetPlan(
            PresetScheduleDraft(
                program = program,
                startDate = LocalDate.of(2026, 9, 8),
                chazarahPattern = ChazarahPattern(emptyList(), repeatsAnnually = false),
            ),
        )
        assertEquals(0, noChazarahPlan.reviewCount)
        assertEquals("", noChazarahPlan.schedule.chazarahDayOffsets)
        assertEquals(false, noChazarahPlan.schedule.repeatsAnnually)
    }

    @Test
    fun `Oraysa plan combines official and additional chazarah`() {
        val unusedDao = Proxy.newProxyInstance(
            ScheduleDao::class.java.classLoader,
            arrayOf(ScheduleDao::class.java),
        ) { _, method, _ -> error("DAO method ${method.name} should not be called while previewing") } as ScheduleDao
        val repository = ScheduleRepository(
            dao = unusedDao,
            clock = Clock.fixed(Instant.parse("2026-09-06T12:00:00Z"), ZoneOffset.UTC),
            zoneProvider = { ZoneOffset.UTC },
        )
        val source = PresetCatalog.programs.first { it.id == "oraysa" }
        val program = source.copy(
            units = source.units.subList(source.currentIndex, source.currentIndex + 5),
            currentIndex = 0,
        )

        val plan = repository.buildPresetPlan(
            PresetScheduleDraft(
                program = program,
                startDate = LocalDate.of(2026, 9, 6),
                chazarahPattern = ChazarahPattern(listOf(2), repeatsAnnually = false),
                includeWeekendChazarah = true,
            ),
        )

        assertEquals(true, plan.schedule.officialOraysaChazarah)
        assertEquals("2", plan.schedule.chazarahDayOffsets)
        assertEquals(5, plan.learningCount)
        assertEquals(12, plan.reviewCount)
        assertEquals(2, plan.tasks.count { it.reviewIdentity?.startsWith("weekend:weekly") == true })

        val overlappingPlan = repository.buildPresetPlan(
            PresetScheduleDraft(
                program = program,
                startDate = LocalDate.of(2026, 9, 6),
                chazarahPattern = ChazarahPattern(listOf(1), repeatsAnnually = false),
                includeWeekendChazarah = true,
            ),
        )
        assertEquals(7, overlappingPlan.reviewCount)

        val standardProgram = PresetCatalog.programs.first { it.id == "daf-yomi-bavli" }.copy(
            units = PresetCatalog.programs.first { it.id == "daf-yomi-bavli" }.units.take(5),
            currentIndex = 0,
        )
        val weekendOnly = repository.buildPresetPlan(
            PresetScheduleDraft(
                program = standardProgram,
                startDate = LocalDate.of(2026, 9, 6),
                chazarahPattern = ChazarahPattern(emptyList(), repeatsAnnually = false),
                includeWeekendChazarah = true,
            ),
        )
        assertEquals(2, weekendOnly.reviewCount)
        assertEquals(2, weekendOnly.tasks.count { it.reviewIdentity?.startsWith("weekend:weekly") == true })
    }

    @Test
    fun `future edit preserves history and completed tasks while replacing future plan`() {
        val unusedDao = Proxy.newProxyInstance(
            ScheduleDao::class.java.classLoader,
            arrayOf(ScheduleDao::class.java),
        ) { _, method, _ -> error("DAO method ${method.name} should not be called while planning an edit") } as ScheduleDao
        val repository = ScheduleRepository(unusedDao)
        val today = LocalDate.of(2026, 9, 8)
        val completedAt = Instant.parse("2026-09-08T12:00:00Z")
        val schedule = scheduleEntity()
        val oldLearning = task("old", "learning:old", TaskType.LEARNING, LocalDate.of(2026, 9, 7))
        val completedToday = task("done", "learning:done", TaskType.LEARNING, today, completedAt)
        val editableToday = task("edit-1", "learning:edit-1", TaskType.LEARNING, today)
        val editableTomorrow = task("edit-2", "learning:edit-2", TaskType.LEARNING, today.plusDays(1))
        val oldReview = task("old-review", "review:learning:old:day:1", TaskType.CHAZARAH, today.plusDays(1))
        val completedLearningReview = task("done-review", "review:learning:done:day:1", TaskType.CHAZARAH, today.plusDays(1))
        val editableReview = task("edit-review", "review:learning:edit-1:day:1", TaskType.CHAZARAH, today.plusDays(1))
        val completedEditableReview = task(
            "edit-review-done", "review:learning:edit-2:day:1", TaskType.CHAZARAH, today.plusDays(2), completedAt,
        )

        val edit = repository.buildFutureEdit(
            schedule = schedule,
            tasks = listOf(
                oldLearning, completedToday, editableToday, editableTomorrow,
                oldReview, completedLearningReview, editableReview, completedEditableReview,
            ),
            excludedDates = emptySet(),
            draft = FutureScheduleEditDraft(
                startDate = today.plusDays(1),
                dailyQuantity = 1,
                selectedWeekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
            ),
            today = today,
        )

        assertEquals(setOf("edit-1", "edit-2", "edit-review"), edit.taskIdsToDelete.toSet())
        assertEquals(listOf(LocalDate.of(2026, 9, 9), LocalDate.of(2026, 9, 14)), edit.replacementTasks.filter { it.type == TaskType.LEARNING }.map { it.plannedDate })
        assertEquals(listOf(LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 16)), edit.replacementTasks.filter { it.type == TaskType.CHAZARAH }.map { it.plannedDate })
        assertEquals(2, edit.schedule.generationRevision)
        assertEquals(1, edit.schedule.dailyQuantity)
        assertEquals(LocalDate.of(2026, 9, 14), edit.schedule.targetDate)
        assertEquals("MONDAY,WEDNESDAY", edit.schedule.selectedWeekdays)

        val finishByEdit = repository.buildFutureEdit(
            schedule = schedule.copy(dailyQuantity = 0),
            tasks = listOf(editableToday, editableTomorrow),
            excludedDates = emptySet(),
            draft = FutureScheduleEditDraft(
                startDate = today,
                dailyQuantity = 1,
                targetCompletionDate = today.plusDays(2),
                selectedWeekdays = DayOfWeek.entries.toSet(),
            ),
            today = today,
        )
        assertEquals(0, finishByEdit.schedule.dailyQuantity)
        assertEquals(listOf(today, today.plusDays(2)), finishByEdit.replacementTasks.filter { it.type == TaskType.LEARNING }.map { it.plannedDate })
    }

    @Test
    fun `future edit preserves a completed weekend review without stable key collision`() {
        val unusedDao = Proxy.newProxyInstance(
            ScheduleDao::class.java.classLoader,
            arrayOf(ScheduleDao::class.java),
        ) { _, method, _ -> error("DAO method ${method.name} should not be called while planning an edit") } as ScheduleDao
        val repository = ScheduleRepository(unusedDao)
        val today = LocalDate.of(2026, 9, 8)
        val learning = task("edit", "learning:edit", TaskType.LEARNING, today)
        val completedWeekend = task(
            "weekend-done",
            "review:weekend:2026-09-06:shabbos",
            TaskType.CHAZARAH,
            LocalDate.of(2026, 9, 12),
            Instant.parse("2026-09-08T12:00:00Z"),
        ).copy(
            originalLearningDate = today,
            reviewIdentity = "weekend:weekly:shabbos",
        )

        val edit = repository.buildFutureEdit(
            schedule = scheduleEntity().copy(chazarahDayOffsets = "", repeatsAnnually = false),
            tasks = listOf(learning, completedWeekend),
            excludedDates = emptySet(),
            draft = FutureScheduleEditDraft(
                startDate = today,
                dailyQuantity = 1,
                selectedWeekdays = DayOfWeek.entries.toSet(),
                includeWeekendChazarah = true,
            ),
            today = today,
        )

        assertEquals(listOf("edit"), edit.taskIdsToDelete)
        assertEquals(
            "review:weekend:2026-09-06:shabbos:revision:2",
            edit.replacementTasks.single { it.type == TaskType.CHAZARAH }.stableKey,
        )
    }

    private fun scheduleEntity() = ScheduleEntity(
        id = "schedule-1",
        nameEnglish = "Test schedule",
        nameHebrew = "",
        kind = ScheduleKind.CUSTOM,
        sourceType = "OTHER",
        materialType = MaterialType.CUSTOM_UNIT,
        presetId = null,
        startDate = LocalDate.of(2026, 9, 1),
        targetDate = LocalDate.of(2026, 9, 9),
        dailyQuantity = 2,
        selectedWeekdays = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,SUNDAY",
        chazarahDayOffsets = "1",
        repeatsAnnually = false,
        officialOraysaChazarah = false,
        missedWorkBehavior = MissedWorkBehavior.KEEP_FIXED_OVERDUE,
        state = ScheduleState.ACTIVE,
        generationRevision = 1,
        createdAt = Instant.parse("2026-09-01T12:00:00Z"),
    )

    private fun task(
        id: String,
        stableKey: String,
        type: TaskType,
        date: LocalDate,
        completedAt: Instant? = null,
    ) = TaskEntity(
        id = id,
        stableKey = stableKey,
        scheduleId = "schedule-1",
        type = type,
        labelEnglish = "Unit $id",
        labelHebrew = "",
        materialType = MaterialType.CUSTOM_UNIT,
        quantity = 1.0,
        plannedDate = date,
        originalLearningDate = if (type == TaskType.LEARNING) date else LocalDate.of(2026, 9, 7),
        reviewIdentity = if (type == TaskType.CHAZARAH) "day:1" else null,
        generationRevision = 1,
        completedAt = completedAt,
        completionLocalDate = completedAt?.let { LocalDate.of(2026, 9, 8) },
        completionZoneId = completedAt?.let { "Z" },
    )
}

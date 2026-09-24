package app.veshinantam.data

import app.veshinantam.data.local.ScheduleDao
import app.veshinantam.data.local.TaskEntity
import app.veshinantam.data.local.TodayTaskRow
import app.veshinantam.data.local.ScheduleEntity
import app.veshinantam.data.local.ScheduleExclusionEntity
import app.veshinantam.data.local.TaskDateUpdate
import app.veshinantam.data.local.ProgressTaskRow
import app.veshinantam.data.local.ProgressGoalEntity
import app.veshinantam.domain.model.BilingualLabel
import app.veshinantam.domain.model.ChazarahPattern
import app.veshinantam.domain.model.MaterialType
import app.veshinantam.domain.model.MaterialUnit
import app.veshinantam.domain.model.MissedWorkBehavior
import app.veshinantam.domain.model.ScheduleKind
import app.veshinantam.domain.model.ScheduleRules
import app.veshinantam.domain.model.ScheduleState
import app.veshinantam.domain.model.TaskType
import app.veshinantam.domain.material.PresetCatalog
import app.veshinantam.domain.material.PresetProgram
import app.veshinantam.domain.scheduling.ScheduleEngine
import app.veshinantam.domain.scheduling.LearningPlacement
import app.veshinantam.domain.scheduling.LearningRollover
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class CustomScheduleDraft(
    val name: String,
    val units: List<ScheduleUnitInput>,
    val startDate: LocalDate,
    val dailyQuantity: Int,
    val targetCompletionDate: LocalDate? = null,
    val selectedWeekdays: Set<DayOfWeek>,
    val chazarahPattern: ChazarahPattern = ChazarahPattern(),
    val includeWeekendChazarah: Boolean = false,
    val missedWorkBehavior: MissedWorkBehavior = MissedWorkBehavior.KEEP_FIXED_OVERDUE,
    val materialType: MaterialType = MaterialType.CUSTOM_UNIT,
    val sourceType: String = "OTHER",
)

data class ScheduleUnitInput(val primary: String, val secondary: String = "")

data class PresetScheduleDraft(
    val program: PresetProgram,
    val startIndex: Int = program.currentIndex,
    val startDate: LocalDate,
    val chazarahPattern: ChazarahPattern = ChazarahPattern(),
    val includeWeekendChazarah: Boolean = program.id == "oraysa",
    val missedWorkBehavior: MissedWorkBehavior = MissedWorkBehavior.KEEP_FIXED_OVERDUE,
)

data class PendingSchedulePlan(
    val schedule: ScheduleEntity,
    val tasks: List<TaskEntity>,
) {
    val learningCount: Int get() = tasks.count { it.type == app.veshinantam.domain.model.TaskType.LEARNING }
    val reviewCount: Int get() = tasks.size - learningCount
    val completionDate: LocalDate? get() = tasks.filter { it.type == app.veshinantam.domain.model.TaskType.LEARNING }.maxOfOrNull { it.plannedDate }
}

data class FutureScheduleEditDraft(
    val startDate: LocalDate,
    val dailyQuantity: Int,
    val targetCompletionDate: LocalDate? = null,
    val selectedWeekdays: Set<DayOfWeek>,
    val includeWeekendChazarah: Boolean = false,
)

data class PendingFutureScheduleEdit(
    val schedule: ScheduleEntity,
    val taskIdsToDelete: List<String>,
    val replacementTasks: List<TaskEntity>,
) {
    val learningCount: Int get() = replacementTasks.count { it.type == TaskType.LEARNING }
    val reviewCount: Int get() = replacementTasks.count { it.type == TaskType.CHAZARAH }
}

class ScheduleRepository(
    private val dao: ScheduleDao,
    private val clock: Clock = Clock.systemUTC(),
    private val zoneProvider: () -> ZoneId = ZoneId::systemDefault,
    private val onDataChanged: () -> Unit = {},
) {
    private val engine = ScheduleEngine()

    fun observeSchedules(): Flow<List<ScheduleEntity>> = dao.observeSchedules()
    fun observeExclusions(): Flow<List<ScheduleExclusionEntity>> = dao.observeExclusions()
    fun observeDueTasks(): Flow<List<TaskEntity>> =
        dao.observeTasksThrough(LocalDate.now(clock.withZone(zoneProvider())))

    fun observeTodayTasks(today: LocalDate): Flow<List<TodayTaskRow>> = dao.observeTodayTasks(today)

    fun observeCalendarTasks(fromDate: LocalDate, toDate: LocalDate): Flow<List<TodayTaskRow>> =
        dao.observeCalendarTasks(fromDate, toDate)

    fun observeProgressTasks(): Flow<List<ProgressTaskRow>> = dao.observeProgressTasks()
    fun observeProgressGoals(): Flow<List<ProgressGoalEntity>> = dao.observeProgressGoals()
    suspend fun learningTasks(scheduleId: String): List<TaskEntity> = dao.getLearningTasks(scheduleId)

    suspend fun replaceProgressGoals(goals: List<ProgressGoalEntity>) {
        dao.replaceProgressGoals(goals)
        onDataChanged()
    }

    suspend fun setCompleted(taskId: String, completed: Boolean) {
        val zone = zoneProvider()
        val completionDate = LocalDate.now(clock.withZone(zone))
        dao.updateCompletion(
            taskId = taskId,
            completedAt = if (completed) clock.instant() else null,
            completionLocalDate = if (completed) completionDate else null,
            zoneId = if (completed) zone.id else null,
        )
        onDataChanged()
    }

    suspend fun completePastLearning(scheduleId: String): Int = completePastTasks(scheduleId, TaskType.LEARNING)

    suspend fun completePastChazarah(scheduleId: String): Int = completePastTasks(scheduleId, TaskType.CHAZARAH)

    private suspend fun completePastTasks(scheduleId: String, taskType: TaskType): Int {
        val zone = zoneProvider()
        val today = LocalDate.now(clock.withZone(zone))
        val completedCount = dao.completePastTasks(
            scheduleId = scheduleId,
            taskType = taskType,
            today = today,
            completedAt = clock.instant(),
            completionLocalDate = today,
            zoneId = zone.id,
        )
        if (completedCount > 0) onDataChanged()
        return completedCount
    }

    fun buildCustomPlan(draft: CustomScheduleDraft): PendingSchedulePlan {
        require(draft.name.isNotBlank())
        require(draft.units.isNotEmpty())
        val scheduleId = UUID.randomUUID().toString()
        val rules = ScheduleRules(draft.selectedWeekdays)
        val material = draft.units.mapIndexed { index, label ->
            MaterialUnit("$scheduleId-unit-$index", index, BilingualLabel(label.primary.trim(), label.secondary.trim()))
        }
        val learning = draft.targetCompletionDate?.let { target ->
            engine.generateByCompletionDate(material, draft.startDate, target, rules)
        } ?: engine.generateByDailyQuantity(material, draft.startDate, draft.dailyQuantity, rules)
        val additionalReviews = engine.generateChazarah(
            learning,
            draft.chazarahPattern,
            rules,
            annualReviewsThroughYear = draft.startDate.year + 10,
        )
        val weekendReviews = if (draft.includeWeekendChazarah) engine.generateWeekendChazarah(learning) else emptyList()
        val reviews = (weekendReviews + additionalReviews).distinctBy { review ->
            listOf(review.originalLearningDate, review.plannedDate, review.material.label.english, review.material.label.hebrew)
        }
        val schedule = ScheduleEntity(
            id = scheduleId,
            nameEnglish = draft.name.trim(),
            nameHebrew = "",
            kind = ScheduleKind.CUSTOM,
            sourceType = draft.sourceType,
            materialType = draft.materialType,
            presetId = null,
            startDate = draft.startDate,
            targetDate = learning.maxOfOrNull { it.plannedDate },
            dailyQuantity = if (draft.targetCompletionDate == null) draft.dailyQuantity else 0,
            selectedWeekdays = draft.selectedWeekdays.joinToString(",") { it.name },
            chazarahDayOffsets = draft.chazarahPattern.dayOffsets.joinToString(","),
            repeatsAnnually = draft.chazarahPattern.repeatsAnnually,
            officialOraysaChazarah = draft.includeWeekendChazarah,
            missedWorkBehavior = draft.missedWorkBehavior,
            state = ScheduleState.ACTIVE,
            generationRevision = 1,
            createdAt = clock.instant(),
        )
        val entities = (learning + reviews).map { planned ->
            TaskEntity(
                id = UUID.randomUUID().toString(),
                stableKey = planned.stableKey,
                scheduleId = scheduleId,
                type = planned.type,
                labelEnglish = planned.material.label.english,
                labelHebrew = planned.material.label.hebrew,
                materialType = draft.materialType,
                quantity = planned.material.quantity,
                plannedDate = planned.plannedDate,
                originalLearningDate = planned.originalLearningDate,
                reviewIdentity = planned.reviewIdentity,
                generationRevision = 1,
                completedAt = null,
                completionLocalDate = null,
                completionZoneId = null,
            )
        }
        return PendingSchedulePlan(schedule, entities)
    }

    fun buildPresetPlan(draft: PresetScheduleDraft): PendingSchedulePlan {
        require(draft.startIndex in 0..draft.program.currentIndex)
        val scheduleId = UUID.randomUUID().toString()
        val rules = ScheduleRules(draft.program.selectedWeekdays, draft.program.excludedDates)
        val material = draft.program.units.subList(draft.startIndex, draft.program.units.size).mapIndexed { index, reference ->
            MaterialUnit("$scheduleId-unit-$index", index, BilingualLabel(reference.english, reference.hebrew))
        }
        val learning = engine.generateByDailyQuantity(material, draft.startDate, draft.program.dailyQuantity, rules)
        val additionalReviews = engine.generateChazarah(
            learning,
            draft.chazarahPattern,
            rules,
            annualReviewsThroughYear = draft.startDate.year + 10,
        )
        val weekendReviews = if (draft.includeWeekendChazarah) {
            if (draft.program.id == "oraysa") engine.generateOfficialOraysaChazarah(learning, rules)
            else engine.generateWeekendChazarah(learning)
        } else {
            emptyList()
        }
        val reviews = (weekendReviews + additionalReviews).distinctBy { review ->
            listOf(review.originalLearningDate, review.plannedDate, review.material.label.english, review.material.label.hebrew)
        }
        val schedule = ScheduleEntity(
            id = scheduleId,
            nameEnglish = draft.program.nameEnglish,
            nameHebrew = draft.program.nameHebrew,
            kind = ScheduleKind.PRESET,
            sourceType = "PRESET:${PresetCatalog.VERSION}",
            materialType = draft.program.materialType,
            presetId = draft.program.id,
            startDate = draft.startDate,
            targetDate = learning.maxOfOrNull { it.plannedDate },
            dailyQuantity = draft.program.dailyQuantity,
            selectedWeekdays = draft.program.selectedWeekdays.joinToString(",") { it.name },
            chazarahDayOffsets = draft.chazarahPattern.dayOffsets.joinToString(","),
            repeatsAnnually = draft.chazarahPattern.repeatsAnnually,
            officialOraysaChazarah = draft.includeWeekendChazarah,
            missedWorkBehavior = draft.missedWorkBehavior,
            state = ScheduleState.ACTIVE,
            generationRevision = 1,
            createdAt = clock.instant(),
        )
        val entities = (learning + reviews).map { planned ->
            TaskEntity(
                id = UUID.randomUUID().toString(),
                stableKey = planned.stableKey,
                scheduleId = scheduleId,
                type = planned.type,
                labelEnglish = planned.material.label.english,
                labelHebrew = planned.material.label.hebrew,
                materialType = draft.program.materialType,
                quantity = planned.material.quantity,
                plannedDate = planned.plannedDate,
                originalLearningDate = planned.originalLearningDate,
                reviewIdentity = planned.reviewIdentity,
                generationRevision = 1,
                completedAt = null,
                completionLocalDate = null,
                completionZoneId = null,
            )
        }
        return PendingSchedulePlan(schedule, entities)
    }

    suspend fun savePlan(plan: PendingSchedulePlan) {
        dao.createSchedule(plan.schedule, plan.tasks)
        onDataChanged()
    }

    suspend fun setScheduleState(scheduleId: String, state: ScheduleState) {
        dao.updateScheduleState(scheduleId, state)
        onDataChanged()
    }

    suspend fun editFutureSchedule(scheduleId: String, draft: FutureScheduleEditDraft) {
        val today = LocalDate.now(clock.withZone(zoneProvider()))
        val schedule = requireNotNull(dao.getSchedule(scheduleId))
        val tasks = dao.getTasks(scheduleId)
        val userExclusions = dao.getExclusions(scheduleId).mapTo(mutableSetOf()) { it.date }
        val presetExclusions = schedule.presetId
            ?.let { id -> PresetCatalog.programs.firstOrNull { it.id == id } }
            ?.excludedDates
            .orEmpty()
        val edit = buildFutureEdit(
            schedule = schedule,
            tasks = tasks,
            excludedDates = userExclusions + presetExclusions,
            draft = draft,
            today = today,
        )
        dao.replaceFuturePlan(edit.schedule, edit.taskIdsToDelete, edit.replacementTasks)
        onDataChanged()
    }

    internal fun buildFutureEdit(
        schedule: ScheduleEntity,
        tasks: List<TaskEntity>,
        excludedDates: Set<LocalDate>,
        draft: FutureScheduleEditDraft,
        today: LocalDate,
    ): PendingFutureScheduleEdit {
        require(!draft.startDate.isBefore(today)) { "Future start date cannot precede today" }
        require(draft.targetCompletionDate != null || draft.dailyQuantity > 0) { "Daily quantity must be positive" }
        require(draft.targetCompletionDate?.isBefore(draft.startDate) != true) {
            "Completion date cannot precede the future start date"
        }
        require(draft.selectedWeekdays.isNotEmpty()) { "At least one weekday must be selected" }

        val editableLearning = tasks.filter {
            it.type == TaskType.LEARNING && it.completedAt == null && !it.plannedDate.isBefore(today)
        }
        val editableStableKeys = editableLearning.mapTo(mutableSetOf()) { it.stableKey }
        val affectedOraysaWeeks = editableLearning.mapTo(mutableSetOf()) { task ->
            task.originalLearningDate.minusDays((task.originalLearningDate.dayOfWeek.value % 7).toLong())
        }
        val reviewsToReplace = tasks.filter { task ->
            if (task.type != TaskType.CHAZARAH || task.completedAt != null || task.plannedDate.isBefore(today)) {
                false
            } else if (
                task.reviewIdentity?.startsWith("oraysa:weekly") == true ||
                task.reviewIdentity?.startsWith("weekend:weekly") == true
            ) {
                val week = task.originalLearningDate.minusDays((task.originalLearningDate.dayOfWeek.value % 7).toLong())
                week in affectedOraysaWeeks
            } else {
                editableStableKeys.any { learningKey -> task.stableKey.startsWith("review:$learningKey:") }
            }
        }

        val revision = schedule.generationRevision + 1
        val rules = ScheduleRules(draft.selectedWeekdays, excludedDates)
        val material = editableLearning.mapIndexed { index, task ->
            MaterialUnit(
                id = "${schedule.id}-edit-$revision-unit-$index",
                ordinal = index,
                label = BilingualLabel(task.labelEnglish, task.labelHebrew),
                quantity = task.quantity,
            )
        }
        val learning = draft.targetCompletionDate?.let { target ->
            engine.generateByCompletionDate(material, draft.startDate, target, rules)
        } ?: engine.generateByDailyQuantity(material, draft.startDate, draft.dailyQuantity, rules)
        val pattern = ChazarahPattern(
            dayOffsets = schedule.chazarahDayOffsets.split(',').mapNotNull { it.trim().toIntOrNull() },
            repeatsAnnually = schedule.repeatsAnnually,
        )
        val additionalReviews = engine.generateChazarah(
            learning,
            pattern,
            rules,
            annualReviewsThroughYear = draft.startDate.year + 10,
        )
        val weekendReviews = if (draft.includeWeekendChazarah) {
            if (schedule.presetId == "oraysa") engine.generateOfficialOraysaChazarah(learning, rules)
            else engine.generateWeekendChazarah(learning)
        } else {
            emptyList()
        }
        val reviews = (weekendReviews + additionalReviews).distinctBy { review ->
            listOf(review.originalLearningDate, review.plannedDate, review.material.label.english, review.material.label.hebrew)
        }
        val taskIdsToDelete = (editableLearning + reviewsToReplace).map { it.id }
        val preservedStableKeys = tasks.asSequence()
            .filter { it.id !in taskIdsToDelete }
            .mapTo(mutableSetOf()) { it.stableKey }
        val replacements = (learning + reviews).map { planned ->
            val stableKey = planned.stableKey.let { key ->
                if (key in preservedStableKeys) "$key:revision:$revision" else key
            }
            TaskEntity(
                id = UUID.randomUUID().toString(),
                stableKey = stableKey,
                scheduleId = schedule.id,
                type = planned.type,
                labelEnglish = planned.material.label.english,
                labelHebrew = planned.material.label.hebrew,
                materialType = schedule.materialType,
                quantity = planned.material.quantity,
                plannedDate = planned.plannedDate,
                originalLearningDate = planned.originalLearningDate,
                reviewIdentity = planned.reviewIdentity,
                generationRevision = revision,
                completedAt = null,
                completionLocalDate = null,
                completionZoneId = null,
            )
        }
        val preservedLearningTarget = tasks.asSequence()
            .filter { it.type == TaskType.LEARNING && it !in editableLearning }
            .maxOfOrNull { it.plannedDate }
        val regeneratedTarget = learning.maxOfOrNull { it.plannedDate }
        val targetDate = listOfNotNull(preservedLearningTarget, regeneratedTarget).maxOrNull()
        return PendingFutureScheduleEdit(
            schedule = schedule.copy(
                startDate = draft.startDate,
                targetDate = targetDate,
                dailyQuantity = if (draft.targetCompletionDate == null) draft.dailyQuantity else 0,
                selectedWeekdays = draft.selectedWeekdays.sortedBy { it.value }.joinToString(",") { it.name },
                officialOraysaChazarah = draft.includeWeekendChazarah,
                generationRevision = revision,
            ),
            taskIdsToDelete = taskIdsToDelete,
            replacementTasks = replacements,
        )
    }

    suspend fun addExclusion(scheduleId: String, date: LocalDate) {
        val today = LocalDate.now(clock.withZone(zoneProvider()))
        require(!date.isBefore(today))
        val schedule = requireNotNull(dao.getSchedule(scheduleId))
        dao.insertExclusion(ScheduleExclusionEntity(scheduleId, date))
        rescheduleLearning(
            schedule = schedule,
            excludedDates = dao.getExclusions(scheduleId).mapTo(mutableSetOf()) { it.date },
            shiftOverdueTo = today.takeIf { schedule.missedWorkBehavior == MissedWorkBehavior.SHIFT_FORWARD },
        )
        onDataChanged()
    }

    suspend fun removeExclusion(scheduleId: String, date: LocalDate) {
        dao.deleteExclusion(scheduleId, date)
        onDataChanged()
    }

    suspend fun rollOverMissedLearning(
        today: LocalDate = LocalDate.now(clock.withZone(zoneProvider())),
        notifyDataChanged: Boolean = true,
    ) {
        var changed = false
        dao.getActiveShiftForwardSchedules().forEach { schedule ->
            changed = rescheduleLearning(
                schedule = schedule,
                excludedDates = dao.getExclusions(schedule.id).mapTo(mutableSetOf()) { it.date },
                shiftOverdueTo = today,
            ) || changed
        }
        if (changed && notifyDataChanged) onDataChanged()
    }

    private suspend fun rescheduleLearning(
        schedule: ScheduleEntity,
        excludedDates: Set<LocalDate>,
        shiftOverdueTo: LocalDate?,
    ): Boolean {
        val tasks = dao.getLearningTasks(schedule.id)
        val weekdays = schedule.selectedWeekdays.split(',')
            .mapNotNullTo(mutableSetOf()) { value -> runCatching { DayOfWeek.valueOf(value.trim()) }.getOrNull() }
            .ifEmpty { DayOfWeek.entries.toSet() }
        val existingCapacity = tasks.asSequence()
            .filter { it.completedAt == null }
            .groupingBy { it.plannedDate }
            .eachCount()
            .values
            .maxOrNull()
            ?: 1
        val changes = LearningRollover.reschedule(
            tasks = tasks.map { task ->
                LearningPlacement(task.id, task.stableKey, task.plannedDate, task.completedAt != null)
            },
            selectedWeekdays = weekdays,
            excludedDates = excludedDates,
            dailyQuantity = schedule.dailyQuantity.takeIf { it > 0 } ?: existingCapacity,
            shiftOverdueTo = shiftOverdueTo,
        )
        dao.applyLearningDateUpdates(
            schedule.id,
            changes.map { TaskDateUpdate(it.id, it.plannedDate) },
        )
        return changes.isNotEmpty()
    }

    suspend fun deleteSchedule(scheduleId: String) {
        dao.deleteSchedule(scheduleId)
        onDataChanged()
    }
}

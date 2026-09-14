package app.veshinantam.data.sync

import app.veshinantam.data.local.ProgressGoalEntity
import app.veshinantam.data.local.ScheduleEntity
import app.veshinantam.data.local.ScheduleExclusionEntity
import app.veshinantam.data.local.TaskEntity
import app.veshinantam.shared.CanonicalDataSet
import app.veshinantam.shared.CanonicalExclusion
import app.veshinantam.shared.CanonicalGoal
import app.veshinantam.shared.CanonicalMaterialType
import app.veshinantam.shared.CanonicalMissedWorkBehavior
import app.veshinantam.shared.CanonicalPreferences
import app.veshinantam.shared.CanonicalSchedule
import app.veshinantam.shared.CanonicalScheduleKind
import app.veshinantam.shared.CanonicalScheduleState
import app.veshinantam.shared.CanonicalTask
import app.veshinantam.shared.CanonicalTaskType
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate

/** Lossless boundary between Android Room records and the shared sync/backup contract. */
object CanonicalAndroidMapper {
    fun export(
        schedules: List<ScheduleEntity>,
        exclusions: List<ScheduleExclusionEntity>,
        tasks: List<TaskEntity>,
        goals: List<ProgressGoalEntity>,
        preferences: CanonicalPreferences,
        now: Instant = Instant.now(),
        syncCursor: Long = 0,
    ): CanonicalDataSet {
        val timestamp = now.toString()
        return CanonicalDataSet(
            schedules = schedules.map { value ->
                CanonicalSchedule(
                    id = value.id,
                    nameEnglish = value.nameEnglish,
                    nameHebrew = value.nameHebrew,
                    kind = CanonicalScheduleKind.valueOf(value.kind.name),
                    sourceType = value.sourceType,
                    materialType = CanonicalMaterialType.valueOf(value.materialType.name),
                    presetId = value.presetId,
                    startDate = value.startDate.toString(),
                    targetDate = value.targetDate?.toString(),
                    dailyQuantity = value.dailyQuantity,
                    selectedWeekdays = value.selectedWeekdays.split(',').mapNotNull { weekday ->
                        runCatching { java.time.DayOfWeek.valueOf(weekday.trim()).value % 7 }.getOrNull()
                    }.toSet(),
                    chazarahDayOffsets = value.chazarahDayOffsets.split(',').mapNotNull { it.trim().toIntOrNull() },
                    repeatsAnnually = value.repeatsAnnually,
                    officialOraysaChazarah = value.officialOraysaChazarah,
                    missedWorkBehavior = CanonicalMissedWorkBehavior.valueOf(value.missedWorkBehavior.name),
                    state = CanonicalScheduleState.valueOf(value.state.name),
                    generationRevision = value.generationRevision,
                    createdAt = value.createdAt.toString(),
                    updatedAt = timestamp,
                )
            },
            tasks = tasks.map { value ->
                CanonicalTask(
                    id = value.id,
                    stableKey = value.stableKey,
                    scheduleId = value.scheduleId,
                    type = CanonicalTaskType.valueOf(value.type.name),
                    labelEnglish = value.labelEnglish,
                    labelHebrew = value.labelHebrew,
                    materialType = CanonicalMaterialType.valueOf(value.materialType.name),
                    quantity = value.quantity,
                    plannedDate = value.plannedDate.toString(),
                    originalLearningDate = value.originalLearningDate.toString(),
                    reviewIdentity = value.reviewIdentity,
                    generationRevision = value.generationRevision,
                    completedAt = value.completedAt?.toString(),
                    completionLocalDate = value.completionLocalDate?.toString(),
                    completionZoneId = value.completionZoneId,
                    updatedAt = value.completedAt?.toString() ?: timestamp,
                )
            },
            exclusions = exclusions.map { value ->
                CanonicalExclusion(value.scheduleId, value.date.toString(), timestamp)
            },
            goals = goals.map { value ->
                CanonicalGoal(
                    id = value.kind,
                    kind = value.kind,
                    target = value.target,
                    updatedAt = timestamp,
                )
            },
            preferences = preferences,
            syncCursor = syncCursor,
        )
    }

    fun schedule(value: CanonicalSchedule) = ScheduleEntity(
        id = value.id,
        nameEnglish = value.nameEnglish,
        nameHebrew = value.nameHebrew,
        kind = app.veshinantam.domain.model.ScheduleKind.valueOf(value.kind.name),
        sourceType = value.sourceType,
        materialType = app.veshinantam.domain.model.MaterialType.valueOf(value.materialType.name),
        presetId = value.presetId,
        startDate = LocalDate.parse(value.startDate),
        targetDate = value.targetDate?.let(LocalDate::parse),
        dailyQuantity = value.dailyQuantity,
        selectedWeekdays = value.selectedWeekdays.sorted().joinToString(",") { day ->
            if (day == 0) DayOfWeek.SUNDAY.name else DayOfWeek.of(day).name
        },
        chazarahDayOffsets = value.chazarahDayOffsets.joinToString(","),
        repeatsAnnually = value.repeatsAnnually,
        officialOraysaChazarah = value.officialOraysaChazarah,
        missedWorkBehavior = app.veshinantam.domain.model.MissedWorkBehavior.valueOf(value.missedWorkBehavior.name),
        state = app.veshinantam.domain.model.ScheduleState.valueOf(value.state.name),
        generationRevision = value.generationRevision,
        createdAt = Instant.parse(value.createdAt),
    )

    fun task(value: CanonicalTask) = TaskEntity(
        id = value.id,
        stableKey = value.stableKey,
        scheduleId = value.scheduleId,
        type = app.veshinantam.domain.model.TaskType.valueOf(value.type.name),
        labelEnglish = value.labelEnglish,
        labelHebrew = value.labelHebrew,
        materialType = app.veshinantam.domain.model.MaterialType.valueOf(value.materialType.name),
        quantity = value.quantity,
        plannedDate = LocalDate.parse(value.plannedDate),
        originalLearningDate = LocalDate.parse(value.originalLearningDate),
        reviewIdentity = value.reviewIdentity,
        generationRevision = value.generationRevision,
        completedAt = value.completedAt?.let(Instant::parse),
        completionLocalDate = value.completionLocalDate?.let(LocalDate::parse),
        completionZoneId = value.completionZoneId,
    )

    fun exclusion(value: CanonicalExclusion) = ScheduleExclusionEntity(
        scheduleId = value.scheduleId,
        date = LocalDate.parse(value.date),
    )

    fun goal(value: CanonicalGoal) = ProgressGoalEntity(value.kind, value.target)
}

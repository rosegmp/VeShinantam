package app.veshinantam.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.veshinantam.domain.model.MaterialType
import app.veshinantam.domain.model.MissedWorkBehavior
import app.veshinantam.domain.model.ScheduleKind
import app.veshinantam.domain.model.ScheduleState
import app.veshinantam.domain.model.TaskType
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey val id: String,
    val nameEnglish: String,
    val nameHebrew: String,
    val kind: ScheduleKind,
    val sourceType: String,
    val materialType: MaterialType,
    val presetId: String?,
    val startDate: LocalDate,
    val targetDate: LocalDate?,
    val dailyQuantity: Int,
    val selectedWeekdays: String,
    val chazarahDayOffsets: String,
    val repeatsAnnually: Boolean,
    val officialOraysaChazarah: Boolean,
    val missedWorkBehavior: MissedWorkBehavior,
    val state: ScheduleState,
    val generationRevision: Int,
    val createdAt: Instant,
)

@Entity(
    tableName = "schedule_exclusions",
    primaryKeys = ["scheduleId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = ScheduleEntity::class,
            parentColumns = ["id"],
            childColumns = ["scheduleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("scheduleId")],
)
data class ScheduleExclusionEntity(
    val scheduleId: String,
    val date: LocalDate,
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ScheduleEntity::class,
            parentColumns = ["id"],
            childColumns = ["scheduleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("scheduleId"),
        Index(value = ["plannedDate", "completedAt"]),
        Index(value = ["scheduleId", "stableKey"], unique = true),
    ],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val stableKey: String,
    val scheduleId: String,
    val type: TaskType,
    val labelEnglish: String,
    val labelHebrew: String,
    val materialType: MaterialType,
    val quantity: Double,
    val plannedDate: LocalDate,
    val originalLearningDate: LocalDate,
    val reviewIdentity: String?,
    val generationRevision: Int,
    val completedAt: Instant?,
    val completionLocalDate: LocalDate?,
    val completionZoneId: String?,
)

@Entity(tableName = "progress_goals")
data class ProgressGoalEntity(
    @PrimaryKey val kind: String,
    val target: Double,
)

@Entity(tableName = "sync_outbox", primaryKeys = ["entityType", "entityId"])
data class SyncOutboxEntity(
    val entityType: String,
    val entityId: String,
    val mutationId: String,
    val baseRevision: Long,
    val payload: String?,
    val deleted: Boolean,
    val createdAt: String,
)

@Entity(tableName = "sync_shadow", primaryKeys = ["entityType", "entityId"])
data class SyncShadowEntity(
    val entityType: String,
    val entityId: String,
    val payload: String?,
    val revision: Long,
    val deleted: Boolean,
)

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val key: String,
    val longValue: Long,
)

data class TodayTaskRow(
    val taskId: String,
    val scheduleId: String,
    val scheduleNameEnglish: String,
    val scheduleNameHebrew: String,
    val scheduleCreatedAt: Instant,
    val type: TaskType,
    val labelEnglish: String,
    val labelHebrew: String,
    val plannedDate: LocalDate,
    val originalLearningDate: LocalDate = plannedDate,
    val completedAt: Instant?,
    val materialType: MaterialType = MaterialType.CUSTOM_UNIT,
    val presetId: String? = null,
)

data class DueCounts(
    val learningCount: Int,
    val chazarahCount: Int,
)

data class WidgetTaskRow(
    val scheduleNameEnglish: String,
    val scheduleNameHebrew: String,
    val type: TaskType,
    val labelEnglish: String,
    val labelHebrew: String,
    val plannedDate: LocalDate,
)

data class ProgressTaskRow(
    val taskId: String,
    val scheduleId: String,
    val scheduleState: ScheduleState,
    val type: TaskType,
    val quantity: Double,
    val plannedDate: LocalDate,
    val completedAt: Instant?,
)

data class TaskDateUpdate(
    val taskId: String,
    val plannedDate: LocalDate,
)

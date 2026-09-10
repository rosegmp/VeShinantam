package app.veshinantam.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import app.veshinantam.domain.model.ScheduleState
import app.veshinantam.domain.model.TaskType
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY createdAt")
    suspend fun getAllSchedules(): List<ScheduleEntity>

    @Query("SELECT * FROM schedule_exclusions ORDER BY scheduleId, date")
    suspend fun getAllExclusions(): List<ScheduleExclusionEntity>

    @Query("SELECT * FROM tasks ORDER BY scheduleId, plannedDate, type, stableKey")
    suspend fun getAllTasks(): List<TaskEntity>

    @Query("SELECT * FROM progress_goals ORDER BY kind")
    suspend fun getAllProgressGoals(): List<ProgressGoalEntity>

    @Query("SELECT * FROM schedules ORDER BY createdAt")
    fun observeSchedules(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedule_exclusions ORDER BY date")
    fun observeExclusions(): Flow<List<ScheduleExclusionEntity>>

    @Query("SELECT * FROM schedules WHERE id = :scheduleId")
    suspend fun getSchedule(scheduleId: String): ScheduleEntity?

    @Query("SELECT * FROM schedules WHERE state = 'ACTIVE' AND missedWorkBehavior = 'SHIFT_FORWARD'")
    suspend fun getActiveShiftForwardSchedules(): List<ScheduleEntity>

    @Query("SELECT * FROM schedule_exclusions WHERE scheduleId = :scheduleId ORDER BY date")
    suspend fun getExclusions(scheduleId: String): List<ScheduleExclusionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExclusion(exclusion: ScheduleExclusionEntity)

    @Query("DELETE FROM schedule_exclusions WHERE scheduleId = :scheduleId AND date = :date")
    suspend fun deleteExclusion(scheduleId: String, date: LocalDate)

    @Query("SELECT * FROM tasks WHERE scheduleId = :scheduleId AND type = 'LEARNING' ORDER BY plannedDate, stableKey")
    suspend fun getLearningTasks(scheduleId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE scheduleId = :scheduleId ORDER BY plannedDate, type, stableKey")
    suspend fun getTasks(scheduleId: String): List<TaskEntity>

    @Query("UPDATE tasks SET plannedDate = :plannedDate WHERE id = :taskId AND completedAt IS NULL AND type = 'LEARNING'")
    suspend fun updateLearningTaskDate(taskId: String, plannedDate: LocalDate)

    @Query("UPDATE schedules SET targetDate = (SELECT MAX(plannedDate) FROM tasks WHERE scheduleId = :scheduleId AND type = 'LEARNING') WHERE id = :scheduleId")
    suspend fun refreshScheduleTargetDate(scheduleId: String)

    @Transaction
    suspend fun applyLearningDateUpdates(scheduleId: String, updates: List<TaskDateUpdate>) {
        updates.forEach { updateLearningTaskDate(it.taskId, it.plannedDate) }
        if (updates.isNotEmpty()) refreshScheduleTargetDate(scheduleId)
    }

    @Query("UPDATE schedules SET state = :state WHERE id = :scheduleId")
    suspend fun updateScheduleState(scheduleId: String, state: ScheduleState)

    @Query("DELETE FROM schedules WHERE id = :scheduleId")
    suspend fun deleteSchedule(scheduleId: String)

    @Query(
        """
        SELECT * FROM tasks
        WHERE plannedDate <= :today
          AND scheduleId IN (SELECT id FROM schedules WHERE state = 'ACTIVE')
        ORDER BY scheduleId, plannedDate, type, stableKey
        """,
    )
    fun observeTasksThrough(today: LocalDate): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT
          tasks.id AS taskId,
          schedules.id AS scheduleId,
          schedules.nameEnglish AS scheduleNameEnglish,
          schedules.nameHebrew AS scheduleNameHebrew,
          schedules.createdAt AS scheduleCreatedAt,
          tasks.type AS type,
          tasks.labelEnglish AS labelEnglish,
          tasks.labelHebrew AS labelHebrew,
          tasks.plannedDate AS plannedDate,
          tasks.completedAt AS completedAt
        FROM tasks
        INNER JOIN schedules ON schedules.id = tasks.scheduleId
        WHERE schedules.state = 'ACTIVE'
          AND tasks.plannedDate <= :today
          AND (
            tasks.plannedDate = :today
            OR tasks.completedAt IS NULL
            OR tasks.completionLocalDate = :today
          )
        ORDER BY schedules.createdAt, tasks.plannedDate, tasks.type, tasks.stableKey
        """,
    )
    fun observeTodayTasks(today: LocalDate): Flow<List<TodayTaskRow>>

    @Query(
        """
        SELECT
          tasks.id AS taskId,
          schedules.id AS scheduleId,
          schedules.nameEnglish AS scheduleNameEnglish,
          schedules.nameHebrew AS scheduleNameHebrew,
          schedules.createdAt AS scheduleCreatedAt,
          tasks.type AS type,
          tasks.labelEnglish AS labelEnglish,
          tasks.labelHebrew AS labelHebrew,
          tasks.plannedDate AS plannedDate,
          tasks.completedAt AS completedAt
        FROM tasks
        INNER JOIN schedules ON schedules.id = tasks.scheduleId
        WHERE tasks.plannedDate BETWEEN :fromDate AND :toDate
        ORDER BY tasks.plannedDate, schedules.createdAt, tasks.type, tasks.stableKey
        """,
    )
    fun observeCalendarTasks(fromDate: LocalDate, toDate: LocalDate): Flow<List<TodayTaskRow>>

    @Query(
        """
        SELECT
          tasks.id AS taskId,
          schedules.id AS scheduleId,
          schedules.nameEnglish AS scheduleNameEnglish,
          schedules.nameHebrew AS scheduleNameHebrew,
          schedules.createdAt AS scheduleCreatedAt,
          tasks.type AS type,
          tasks.labelEnglish AS labelEnglish,
          tasks.labelHebrew AS labelHebrew,
          tasks.plannedDate AS plannedDate,
          tasks.completedAt AS completedAt
        FROM tasks
        INNER JOIN schedules ON schedules.id = tasks.scheduleId
        WHERE schedules.state = 'ACTIVE'
          AND tasks.plannedDate BETWEEN :fromDate AND :toDate
        ORDER BY tasks.plannedDate, schedules.createdAt, tasks.type, tasks.stableKey
        """,
    )
    suspend fun getPrintableTasks(fromDate: LocalDate, toDate: LocalDate): List<TodayTaskRow>

    @Query(
        """
        SELECT
          tasks.id AS taskId,
          schedules.id AS scheduleId,
          schedules.state AS scheduleState,
          tasks.type AS type,
          tasks.quantity AS quantity,
          tasks.plannedDate AS plannedDate,
          tasks.completedAt AS completedAt
        FROM tasks
        INNER JOIN schedules ON schedules.id = tasks.scheduleId
        WHERE schedules.state != 'ARCHIVED'
        ORDER BY tasks.plannedDate, tasks.stableKey
        """,
    )
    fun observeProgressTasks(): Flow<List<ProgressTaskRow>>

    @Query("SELECT * FROM progress_goals ORDER BY kind")
    fun observeProgressGoals(): Flow<List<ProgressGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressGoals(goals: List<ProgressGoalEntity>)

    @Query("DELETE FROM progress_goals")
    suspend fun deleteProgressGoals()

    @Transaction
    suspend fun replaceProgressGoals(goals: List<ProgressGoalEntity>) {
        deleteProgressGoals()
        if (goals.isNotEmpty()) insertProgressGoals(goals)
    }

    @Query(
        """
        SELECT
          COALESCE(SUM(CASE WHEN type = 'LEARNING' THEN 1 ELSE 0 END), 0) AS learningCount,
          COALESCE(SUM(CASE WHEN type = 'CHAZARAH' THEN 1 ELSE 0 END), 0) AS chazarahCount
        FROM tasks
        WHERE plannedDate <= :today
          AND completedAt IS NULL
          AND scheduleId IN (SELECT id FROM schedules WHERE state = 'ACTIVE')
        """,
    )
    suspend fun dueCounts(today: LocalDate): DueCounts

    @Query(
        """
        SELECT
          schedules.nameEnglish AS scheduleNameEnglish,
          schedules.nameHebrew AS scheduleNameHebrew,
          tasks.type AS type,
          tasks.labelEnglish AS labelEnglish,
          tasks.labelHebrew AS labelHebrew,
          tasks.plannedDate AS plannedDate
        FROM tasks
        INNER JOIN schedules ON schedules.id = tasks.scheduleId
        WHERE schedules.state = 'ACTIVE'
          AND tasks.plannedDate <= :today
          AND tasks.completedAt IS NULL
        ORDER BY tasks.plannedDate DESC, tasks.type, schedules.createdAt, tasks.stableKey
        """,
    )
    suspend fun widgetTasks(today: LocalDate): List<WidgetTaskRow>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSchedule(schedule: ScheduleEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExclusions(exclusions: List<ScheduleExclusionEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGoalsForRestore(goals: List<ProgressGoalEntity>)

    @Query("DELETE FROM schedules")
    suspend fun deleteAllSchedules()

    @Query("DELETE FROM progress_goals")
    suspend fun deleteAllGoalsForRestore()

    @Transaction
    suspend fun replaceAllData(
        schedules: List<ScheduleEntity>,
        exclusions: List<ScheduleExclusionEntity>,
        tasks: List<TaskEntity>,
        goals: List<ProgressGoalEntity>,
    ) {
        deleteAllSchedules()
        deleteAllGoalsForRestore()
        schedules.forEach { insertSchedule(it) }
        if (exclusions.isNotEmpty()) insertExclusions(exclusions)
        if (tasks.isNotEmpty()) insertTasks(tasks)
        if (goals.isNotEmpty()) insertGoalsForRestore(goals)
    }

    @Update
    suspend fun updateSchedule(schedule: ScheduleEntity)

    @Query("DELETE FROM tasks WHERE id IN (:taskIds)")
    suspend fun deleteTasks(taskIds: List<String>)

    @Transaction
    suspend fun replaceFuturePlan(
        schedule: ScheduleEntity,
        taskIdsToDelete: List<String>,
        replacementTasks: List<TaskEntity>,
    ) {
        updateSchedule(schedule)
        if (taskIdsToDelete.isNotEmpty()) deleteTasks(taskIdsToDelete)
        if (replacementTasks.isNotEmpty()) insertTasks(replacementTasks)
    }

    @Query(
        """
        UPDATE tasks
        SET completedAt = :completedAt,
            completionLocalDate = :completionLocalDate,
            completionZoneId = :zoneId
        WHERE id = :taskId
        """,
    )
    suspend fun updateCompletion(
        taskId: String,
        completedAt: Instant?,
        completionLocalDate: LocalDate?,
        zoneId: String?,
    )

    @Query(
        """
        UPDATE tasks
        SET completedAt = :completedAt,
            completionLocalDate = :completionLocalDate,
            completionZoneId = :zoneId
        WHERE scheduleId = :scheduleId
          AND type = :taskType
          AND plannedDate < :today
          AND completedAt IS NULL
        """,
    )
    suspend fun completePastTasks(
        scheduleId: String,
        taskType: TaskType,
        today: LocalDate,
        completedAt: Instant,
        completionLocalDate: LocalDate,
        zoneId: String,
    ): Int

    @Transaction
    suspend fun createSchedule(schedule: ScheduleEntity, tasks: List<TaskEntity>) {
        insertSchedule(schedule)
        insertTasks(tasks)
    }
}

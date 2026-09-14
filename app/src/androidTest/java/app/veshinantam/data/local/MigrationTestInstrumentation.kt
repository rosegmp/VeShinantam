package app.veshinantam.data.local

import android.app.Activity
import android.app.Instrumentation
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import androidx.room.Room
import app.veshinantam.data.CustomScheduleDraft
import app.veshinantam.data.ScheduleRepository
import app.veshinantam.data.ScheduleUnitInput
import app.veshinantam.data.backup.BackupJson
import app.veshinantam.data.backup.BackupPayload
import app.veshinantam.data.backup.BackupPreferences
import app.veshinantam.domain.model.ChazarahPattern
import app.veshinantam.domain.model.MaterialType
import app.veshinantam.localization.AppLanguage
import app.veshinantam.localization.PrimaryCalendar
import app.veshinantam.localization.SefarimLanguage
import app.veshinantam.notifications.ReminderPreference
import app.veshinantam.ui.today.TodaySortOrder
import java.io.File
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

/**
 * Dependency-free on-device migration verification. Using the platform Instrumentation API keeps
 * these tests runnable in restricted/offline build environments without weakening schema checks.
 */
class MigrationTestInstrumentation : Instrumentation() {
    private val createdDatabases = mutableSetOf<String>()

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        start()
    }

    override fun onStart() {
        val results = Bundle()
        try {
            everyExportedSchemaMigratesToCurrentVersion()
            versionOneRowsSurviveWithSafeDefaults()
            endToEndScheduleTodayCompletionAndBackupSmoke()
            results.putString(
                REPORT_KEY_STREAMRESULT,
                "VeShinantam database migrations 1-8 passed.\n" +
                    "VeShinantam end-to-end schedule, Today, completion, and backup smoke passed.\n",
            )
            finish(Activity.RESULT_OK, results)
        } catch (error: Throwable) {
            val stackTrace = Log.getStackTraceString(error)
            results.putString("shortMsg", error.message ?: error.javaClass.simpleName)
            results.putString("stack", stackTrace)
            results.putString(REPORT_KEY_STREAMRESULT, "VeShinantam database migration failure:\n$stackTrace\n")
            finish(Activity.RESULT_CANCELED, results)
        } finally {
            createdDatabases.forEach(targetContext::deleteDatabase)
        }
    }

    private fun everyExportedSchemaMigratesToCurrentVersion() {
        for (startVersion in 1 until CURRENT_VERSION) {
            val databaseName = "migration-$startVersion.db"
            createDatabaseFromExportedSchema(databaseName, startVersion)
            val database = openMigratedDatabase(databaseName)
            try {
                val sqlite = database.openHelper.writableDatabase
                check(sqlite.version == CURRENT_VERSION) {
                    "Schema $startVersion finished at database version ${sqlite.version}."
                }
                val actualTables = sqlite.query(
                    "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'android_%' AND name NOT LIKE 'room_%' ORDER BY name",
                ).use { cursor -> buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) } }
                check(actualTables == EXPECTED_TABLES) {
                    "Schema $startVersion produced tables $actualTables instead of $EXPECTED_TABLES."
                }
            } finally {
                database.close()
            }
        }
    }

    private fun versionOneRowsSurviveWithSafeDefaults() {
        val databaseName = "migration-with-data.db"
        val file = createDatabaseFromExportedSchema(databaseName, 1)
        SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { sqlite ->
            sqlite.execSQL(
                """
                INSERT INTO schedules (
                    id, nameEnglish, nameHebrew, kind, materialType, presetId, startDate,
                    selectedWeekdays, missedWorkBehavior, state, generationRevision, createdAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    "legacy-schedule", "Yevamos", "יבמות", "CUSTOM", "AMUD", null,
                    "2025-11-26", "SUNDAY,MONDAY", "SHIFT_FORWARD", "ACTIVE", 1,
                    "2025-11-01T12:00:00Z",
                ),
            )
            sqlite.execSQL(
                """
                INSERT INTO tasks (
                    id, stableKey, scheduleId, type, labelEnglish, labelHebrew, materialType,
                    quantity, plannedDate, originalLearningDate, reviewIdentity,
                    generationRevision, completedAt, completionZoneId
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    "legacy-task", "learning:1", "legacy-schedule", "LEARNING", "Yevamos 2a",
                    "יבמות ב.", "AMUD", 1.0, "2025-11-26", "2025-11-26", null, 1, null, null,
                ),
            )
        }

        val database = openMigratedDatabase(databaseName)
        try {
            database.openHelper.writableDatabase.query(
                "SELECT targetDate, dailyQuantity, chazarahDayOffsets, repeatsAnnually, sourceType, officialOraysaChazarah FROM schedules WHERE id = ?",
                arrayOf("legacy-schedule"),
            ).use { cursor ->
                check(cursor.moveToFirst()) { "The legacy schedule was lost during migration." }
                check(cursor.isNull(0))
                check(cursor.getInt(1) == 1)
                check(cursor.getString(2) == "1,7,30,90")
                check(cursor.getInt(3) == 1)
                check(cursor.getString(4) == "OTHER")
                check(cursor.getInt(5) == 0)
                check(!cursor.moveToNext())
            }
            database.openHelper.writableDatabase.query(
                "SELECT completionLocalDate FROM tasks WHERE id = ?",
                arrayOf("legacy-task"),
            ).use { cursor ->
                check(cursor.moveToFirst()) { "The legacy task was lost during migration." }
                check(cursor.isNull(0))
            }
        } finally {
            database.close()
        }
    }

    private fun endToEndScheduleTodayCompletionAndBackupSmoke() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(targetContext, VeShinantamDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val today = LocalDate.parse("2026-09-10")
            val zone = ZoneId.of("America/New_York")
            val clock = Clock.fixed(Instant.parse("2026-09-10T14:00:00Z"), zone)
            var dataChangeCount = 0
            val dao = database.scheduleDao()
            val repository = ScheduleRepository(dao, clock, { zone }) { dataChangeCount++ }
            val plan = repository.buildCustomPlan(
                CustomScheduleDraft(
                    name = "Yevamos",
                    units = listOf(
                        ScheduleUnitInput("Yevamos 2a", "יבמות ב."),
                        ScheduleUnitInput("Yevamos 2b", "יבמות ב:"),
                    ),
                    startDate = today,
                    dailyQuantity = 1,
                    selectedWeekdays = setOf(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                    chazarahPattern = ChazarahPattern(dayOffsets = listOf(1), repeatsAnnually = false),
                    materialType = MaterialType.AMUD,
                    sourceType = "GEMARA",
                ),
            )

            repository.savePlan(plan)
            check(dao.getAllSchedules().single().nameEnglish == "Yevamos")
            check(dao.getAllTasks().count { it.type.name == "LEARNING" } == 2)
            val todayTask = repository.observeTodayTasks(today).first().single { it.plannedDate == today }
            check(todayTask.completedAt == null)

            repository.setCompleted(todayTask.taskId, true)
            check(dao.getAllTasks().single { it.id == todayTask.taskId }.let {
                it.completedAt == clock.instant() && it.completionLocalDate == today && it.completionZoneId == zone.id
            })
            repository.setCompleted(todayTask.taskId, false)
            check(dao.getAllTasks().single { it.id == todayTask.taskId }.completedAt == null)
            repository.setCompleted(todayTask.taskId, true)

            dao.insertExclusion(ScheduleExclusionEntity(plan.schedule.id, today.plusWeeks(1)))
            repository.replaceProgressGoals(listOf(ProgressGoalEntity("STREAK", 30.0)))
            val payload = BackupPayload(
                schedules = dao.getAllSchedules(),
                exclusions = dao.getAllExclusions(),
                tasks = dao.getAllTasks(),
                goals = dao.getAllProgressGoals(),
                preferences = BackupPreferences(
                    appLanguage = AppLanguage.ENGLISH,
                    sefarimLanguage = SefarimLanguage.BOTH,
                    primaryCalendar = PrimaryCalendar.GREGORIAN,
                    defaultChazarahOffsets = listOf(1, 7, 30, 90),
                    reminder = ReminderPreference(enabled = true, hour = 20, minute = 0),
                    todaySortOrder = TodaySortOrder.SCHEDULED_FIRST,
                    automaticPresetUpdates = false,
                ),
            )
            val decoded = BackupJson.decode(BackupJson.encode(payload))
            dao.replaceAllData(emptyList(), emptyList(), emptyList(), emptyList())
            check(dao.getAllSchedules().isEmpty() && dao.getAllTasks().isEmpty())
            dao.replaceAllData(decoded.schedules, decoded.exclusions, decoded.tasks, decoded.goals)

            check(dao.getAllSchedules() == payload.schedules)
            check(dao.getAllExclusions() == payload.exclusions)
            check(dao.getAllTasks() == payload.tasks)
            check(dao.getAllProgressGoals() == payload.goals)
            check(dataChangeCount == 5) { "Expected five repository change notifications, got $dataChangeCount." }
        } finally {
            database.close()
        }
    }

    private fun openMigratedDatabase(name: String) = Room.databaseBuilder(
        targetContext,
        VeShinantamDatabase::class.java,
        name,
    ).addMigrations(*VeShinantamDatabase.ALL_MIGRATIONS).allowMainThreadQueries().build().also {
        it.openHelper.writableDatabase
    }

    private fun createDatabaseFromExportedSchema(name: String, version: Int): File {
        targetContext.deleteDatabase(name)
        createdDatabases += name
        val file = targetContext.getDatabasePath(name)
        file.parentFile?.mkdirs()
        val schema = context.assets.open("$SCHEMA_ASSET_DIRECTORY/$version.json").bufferedReader().use { reader ->
            JSONObject(reader.readText()).getJSONObject("database")
        }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { sqlite ->
            sqlite.beginTransaction()
            try {
                val entities = schema.getJSONArray("entities")
                for (index in 0 until entities.length()) {
                    val entity = entities.getJSONObject(index)
                    val tableName = entity.getString("tableName")
                    sqlite.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", tableName))
                    val indices = entity.optJSONArray("indices") ?: continue
                    for (indexIndex in 0 until indices.length()) {
                        sqlite.execSQL(
                            indices.getJSONObject(indexIndex).getString("createSql").replace("\${TABLE_NAME}", tableName),
                        )
                    }
                }
                sqlite.version = version
                sqlite.setTransactionSuccessful()
            } finally {
                sqlite.endTransaction()
            }
        }
        return file
    }

    private companion object {
        const val CURRENT_VERSION = 8
        const val SCHEMA_ASSET_DIRECTORY = "app.veshinantam.data.local.VeShinantamDatabase"
        val EXPECTED_TABLES = setOf(
            "progress_goals",
            "schedule_exclusions",
            "schedules",
            "sync_metadata",
            "sync_outbox",
            "sync_shadow",
            "tasks",
        )
    }
}

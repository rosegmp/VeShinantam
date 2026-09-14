package app.veshinantam.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ScheduleEntity::class,
        ScheduleExclusionEntity::class,
        TaskEntity::class,
        ProgressGoalEntity::class,
        SyncOutboxEntity::class,
        SyncShadowEntity::class,
        SyncMetadataEntity::class,
    ],
    version = 8,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class VeShinantamDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        fun create(context: Context): VeShinantamDatabase = Room.databaseBuilder(
            context.applicationContext,
            VeShinantamDatabase::class.java,
            "veshinantam.db",
        ).addMigrations(*ALL_MIGRATIONS).build()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN completionLocalDate TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE schedules ADD COLUMN targetDate TEXT")
                db.execSQL("ALTER TABLE schedules ADD COLUMN dailyQuantity INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE schedules ADD COLUMN chazarahDayOffsets TEXT NOT NULL DEFAULT '1,7,30,90'")
                db.execSQL("ALTER TABLE schedules ADD COLUMN repeatsAnnually INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE schedules ADD COLUMN sourceType TEXT NOT NULL DEFAULT 'OTHER'")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS progress_goals (kind TEXT NOT NULL, target REAL NOT NULL, PRIMARY KEY(kind))")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS schedule_exclusions (scheduleId TEXT NOT NULL, date TEXT NOT NULL, PRIMARY KEY(scheduleId, date), FOREIGN KEY(scheduleId) REFERENCES schedules(id) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_exclusions_scheduleId ON schedule_exclusions(scheduleId)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE schedules ADD COLUMN officialOraysaChazarah INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS sync_outbox (entityType TEXT NOT NULL, entityId TEXT NOT NULL, mutationId TEXT NOT NULL, baseRevision INTEGER NOT NULL, payload TEXT, deleted INTEGER NOT NULL, createdAt TEXT NOT NULL, PRIMARY KEY(entityType, entityId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS sync_shadow (entityType TEXT NOT NULL, entityId TEXT NOT NULL, payload TEXT, revision INTEGER NOT NULL, deleted INTEGER NOT NULL, PRIMARY KEY(entityType, entityId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS sync_metadata (`key` TEXT NOT NULL, longValue INTEGER NOT NULL, PRIMARY KEY(`key`))")
            }
        }

        internal val ALL_MIGRATIONS = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
        )
    }
}

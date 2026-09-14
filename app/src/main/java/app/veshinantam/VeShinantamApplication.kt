package app.veshinantam

import android.app.Application
import app.veshinantam.data.ScheduleRepository
import app.veshinantam.data.local.VeShinantamDatabase
import app.veshinantam.data.preset.PresetCatalogUpdateScheduler
import app.veshinantam.data.preset.PresetCatalogUpdateStore
import app.veshinantam.notifications.ReminderNotifications
import app.veshinantam.notifications.ReminderScheduler
import app.veshinantam.data.sync.SupabaseSyncService
import app.veshinantam.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VeShinantamApplication : Application() {
    val database: VeShinantamDatabase by lazy { VeShinantamDatabase.create(this) }
    val scheduleRepository: ScheduleRepository by lazy {
        ScheduleRepository(database.scheduleDao(), onDataChanged = { WidgetUpdater.enqueueImmediate(this) })
    }
    val supabaseSyncService: SupabaseSyncService by lazy {
        SupabaseSyncService(this, database.scheduleDao())
    }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        PresetCatalogUpdateStore(this).loadCached()
        PresetCatalogUpdateScheduler.sync(this)
        ReminderNotifications.createChannel(this)
        ReminderScheduler.sync(this)
        applicationScope.launch {
            database.scheduleDao().deleteSchedule(LEGACY_DEBUG_SCHEDULE_ID)
            scheduleRepository.rollOverMissedLearning(notifyDataChanged = false)
            WidgetUpdater.enqueueImmediate(this@VeShinantamApplication)
        }
    }

    private companion object {
        const val LEGACY_DEBUG_SCHEDULE_ID = "debug-daf-yomi"
    }
}

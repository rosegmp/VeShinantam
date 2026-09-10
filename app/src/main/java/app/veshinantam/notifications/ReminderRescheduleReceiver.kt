package app.veshinantam.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.veshinantam.widget.WidgetUpdater
import app.veshinantam.data.preset.PresetCatalogUpdateScheduler

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (shouldReschedule(intent.action)) {
            ReminderScheduler.sync(context.applicationContext)
            PresetCatalogUpdateScheduler.sync(context.applicationContext)
            WidgetUpdater.enqueueImmediate(context.applicationContext)
        }
    }

    companion object {
        fun shouldReschedule(action: String?): Boolean = action in setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}

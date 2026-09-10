package app.veshinantam.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

data class ReminderPreference(
    val enabled: Boolean = false,
    val hour: Int = 20,
    val minute: Int = 0,
)

class ReminderSettings(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): ReminderPreference = ReminderPreference(
        enabled = preferences.getBoolean(KEY_ENABLED, false),
        hour = preferences.getInt(KEY_HOUR, 20),
        minute = preferences.getInt(KEY_MINUTE, 0),
    )

    fun save(preference: ReminderPreference) {
        preferences.edit()
            .putBoolean(KEY_ENABLED, preference.enabled)
            .putInt(KEY_HOUR, preference.hour)
            .putInt(KEY_MINUTE, preference.minute)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "reminder_settings"
        const val KEY_ENABLED = "enabled"
        const val KEY_HOUR = "hour"
        const val KEY_MINUTE = "minute"
    }
}

object ReminderScheduler {
    private const val UNIQUE_WORK_NAME = "daily_learning_reminder"

    fun sync(
        context: Context,
        preference: ReminderPreference = ReminderSettings(context).read(),
        now: ZonedDateTime = ZonedDateTime.now(),
    ) {
        val workManager = WorkManager.getInstance(context)
        if (!preference.enabled) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }

        val delay = Duration.between(
            now,
            nextOccurrence(now, preference.hour, preference.minute),
        ).toMillis().coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun nextOccurrence(now: ZonedDateTime, hour: Int, minute: Int): ZonedDateTime {
        require(hour in 0..23)
        require(minute in 0..59)
        val today = now.toLocalDate().atTime(hour, minute).atZone(now.zone)
        return if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(hour, minute).atZone(now.zone)
    }
}

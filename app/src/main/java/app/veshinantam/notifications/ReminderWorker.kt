package app.veshinantam.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.veshinantam.MainActivity
import app.veshinantam.R
import app.veshinantam.VeShinantamApplication
import app.veshinantam.localization.AppLocale
import java.time.LocalDate

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val preference = ReminderSettings(applicationContext).read()
        if (!preference.enabled) return Result.success()

        val result = sendNotificationIfNeeded()
        ReminderScheduler.sync(applicationContext, preference)
        return result
    }

    private suspend fun sendNotificationIfNeeded(): Result {
        val app = applicationContext as VeShinantamApplication
        val today = LocalDate.now()
        app.scheduleRepository.rollOverMissedLearning(today, notifyDataChanged = false)
        val counts = app.database.scheduleDao().dueCounts(today)
        if (counts.learningCount + counts.chazarahCount == 0) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val displayContext = AppLocale.wrap(applicationContext)
        val resources = displayContext.resources
        val learning = resources.getQuantityString(
            R.plurals.learning_tasks_due,
            counts.learningCount,
            counts.learningCount,
        )
        val reviews = resources.getQuantityString(
            R.plurals.review_tasks_due,
            counts.chazarahCount,
            counts.chazarahCount,
        )
        val intent = PendingIntent.getActivity(
            applicationContext,
            0,
            MainActivity.todayIntent(applicationContext),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, ReminderNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(displayContext.getString(R.string.app_name))
            .setContentText("$learning • $reviews")
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(ReminderNotifications.NOTIFICATION_ID, notification)
        return Result.success()
    }
}

object ReminderNotifications {
    const val CHANNEL_ID = "daily_due_work"
    const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

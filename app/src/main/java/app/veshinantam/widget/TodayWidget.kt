package app.veshinantam.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.veshinantam.MainActivity
import app.veshinantam.R
import app.veshinantam.VeShinantamApplication
import app.veshinantam.data.local.WidgetTaskRow
import app.veshinantam.domain.model.TaskType
import app.veshinantam.localization.AppLocale
import app.veshinantam.localization.BidiText
import app.veshinantam.localization.LanguageSettings
import app.veshinantam.localization.SefarimDisplay
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class TodayWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetUpdater.enqueueImmediate(context)
    }

    override fun onEnabled(context: Context) {
        WidgetUpdater.enqueueImmediate(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle,
    ) {
        WidgetUpdater.enqueueImmediate(context)
    }

    override fun onDisabled(context: Context) {
        WidgetUpdater.cancel(context)
    }
}

class WidgetRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val app = applicationContext as VeShinantamApplication
        val today = LocalDate.now()
        app.scheduleRepository.rollOverMissedLearning(today, notifyDataChanged = false)
        val summary = WidgetSummaryBuilder.build(app.database.scheduleDao().widgetTasks(today), today)
        WidgetUpdater.updateAll(applicationContext, summary)
        WidgetUpdater.scheduleNextDay(applicationContext)
        return Result.success()
    }
}

object WidgetUpdater {
    private const val UNIQUE_WORK_NAME = "today_widget_refresh"
    private const val OPEN_TODAY_REQUEST_CODE = 2001

    fun enqueueImmediate(context: Context) {
        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun scheduleNextDay(context: Context, now: ZonedDateTime = ZonedDateTime.now()) {
        if (!hasWidgets(context)) return
        val nextDay = nextRefresh(now)
        val delay = Duration.between(now, nextDay).toMillis().coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    fun nextRefresh(now: ZonedDateTime): ZonedDateTime =
        now.toLocalDate().plusDays(1).atStartOfDay(now.zone).plusSeconds(2)

    fun updateAll(context: Context, summary: WidgetSummary) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = widgetIds(context, manager)
        if (ids.isEmpty()) return
        val displayContext = AppLocale.wrap(context)
        val displaySettings = LanguageSettings(context)
        val uiLanguage = displaySettings.read()
        val sefarimLanguage = displaySettings.readSefarimLanguage()
        val overdueLearning = displayContext.resources.getQuantityString(
            R.plurals.overdue_learning_tasks,
            summary.overdueLearningCount,
            summary.overdueLearningCount,
        )
        val overdueChazarah = displayContext.resources.getQuantityString(
            R.plurals.overdue_chazarah_tasks,
            summary.overdueChazarahCount,
            summary.overdueChazarahCount,
        )
        val pendingIntent = PendingIntent.getActivity(
            context,
            OPEN_TODAY_REQUEST_CODE,
            MainActivity.todayIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        ids.forEach { id ->
            val maxHeightDp = manager.getAppWidgetOptions(id).getInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
                WidgetSizePolicy.DEFAULT_HEIGHT_DP,
            )
            val visibleTaskCount = WidgetSizePolicy.visibleTaskCount(maxHeightDp, summary.todayTasks.size)
            val views = RemoteViews(context.packageName, R.layout.widget_today).apply {
                setTextViewText(R.id.widget_today_count, displayContext.getString(R.string.widget_today_count, summary.todayTasks.size))
                removeAllViews(R.id.widget_task_container)
                summary.todayTasks.take(visibleTaskCount).forEach { task ->
                    val taskView = RemoteViews(context.packageName, R.layout.widget_task_row).apply {
                        setTextViewText(R.id.widget_task_text, taskLine(displayContext, task, sefarimLanguage, uiLanguage))
                    }
                    addView(R.id.widget_task_container, taskView)
                }
                setViewVisibility(R.id.widget_task_container, if (visibleTaskCount == 0) View.GONE else View.VISIBLE)
                setTextViewText(R.id.widget_overdue_count, displayContext.getString(R.string.widget_overdue_count, overdueLearning, overdueChazarah))
                setTextViewText(
                    R.id.widget_status,
                    displayContext.getString(
                        if (summary.todayTasks.isEmpty() && summary.overdueCount == 0) R.string.widget_all_complete else R.string.widget_tap_to_open,
                    ),
                )
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }
            manager.updateAppWidget(id, views)
        }
    }

    private fun taskLine(
        context: Context,
        task: WidgetTaskRow,
        sefarimLanguage: app.veshinantam.localization.SefarimLanguage,
        uiLanguage: app.veshinantam.localization.AppLanguage,
    ): String {
        val scheduleName = if (uiLanguage == app.veshinantam.localization.AppLanguage.HEBREW) {
            task.scheduleNameHebrew.ifBlank { task.scheduleNameEnglish }
        } else {
            task.scheduleNameEnglish.ifBlank { task.scheduleNameHebrew }
        }
        val schedule = BidiText.isolateForUi(scheduleName, uiLanguage)
        val label = SefarimDisplay.label(task.labelEnglish, task.labelHebrew, sefarimLanguage, uiLanguage)
        return context.getString(
            if (task.type == TaskType.LEARNING) R.string.widget_learning_line else R.string.widget_chazarah_line,
            schedule,
            label,
        )
    }

    private fun hasWidgets(context: Context): Boolean = widgetIds(context).isNotEmpty()

    private fun widgetIds(
        context: Context,
        manager: AppWidgetManager = AppWidgetManager.getInstance(context),
    ): IntArray = manager.getAppWidgetIds(ComponentName(context, TodayWidgetProvider::class.java))
}

data class WidgetSummary(
    val todayTasks: List<WidgetTaskRow>,
    val overdueLearningCount: Int,
    val overdueChazarahCount: Int,
) {
    val overdueCount: Int get() = overdueLearningCount + overdueChazarahCount
}

object WidgetSizePolicy {
    const val DEFAULT_HEIGHT_DP = 150
    private const val FIXED_CONTENT_HEIGHT_DP = 126
    private const val TASK_ROW_HEIGHT_DP = 19
    private const val MAX_TASK_ROWS = 24

    fun visibleTaskCount(widgetHeightDp: Int, totalTaskCount: Int): Int {
        if (totalTaskCount <= 0) return 0
        val capacity = ((widgetHeightDp - FIXED_CONTENT_HEIGHT_DP) / TASK_ROW_HEIGHT_DP)
            .coerceIn(1, MAX_TASK_ROWS)
        return totalTaskCount.coerceAtMost(capacity)
    }
}

object WidgetSummaryBuilder {
    fun build(rows: List<WidgetTaskRow>, today: LocalDate): WidgetSummary = WidgetSummary(
        todayTasks = rows.filter { it.plannedDate == today }.sortedBy { if (it.type == TaskType.LEARNING) 0 else 1 },
        overdueLearningCount = rows.count { it.plannedDate < today && it.type == TaskType.LEARNING },
        overdueChazarahCount = rows.count { it.plannedDate < today && it.type == TaskType.CHAZARAH },
    )
}

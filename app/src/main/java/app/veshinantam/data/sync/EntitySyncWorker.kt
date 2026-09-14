package app.veshinantam.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.veshinantam.VeShinantamApplication
import java.util.concurrent.TimeUnit

class EntitySyncWorker(
    appContext: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result {
        val service = (applicationContext as VeShinantamApplication).supabaseSyncService
        return if (service.automaticSync()) Result.success() else Result.retry()
    }
}

object EntitySyncScheduler {
    private const val UNIQUE_WORK_NAME = "account_entity_sync"

    fun enqueue(context: Context) {
        val request = OneTimeWorkRequestBuilder<EntitySyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInitialDelay(2, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}

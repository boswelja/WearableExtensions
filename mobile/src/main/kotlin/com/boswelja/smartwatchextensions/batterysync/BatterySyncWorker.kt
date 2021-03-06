package com.boswelja.smartwatchextensions.batterysync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.boswelja.smartwatchextensions.watchmanager.WatchManager
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber

class BatterySyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    @ExperimentalCoroutinesApi
    override suspend fun doWork(): Result {
        Timber.i("doWork() called")
        val watchId = UUID.fromString(inputData.getString(EXTRA_WATCH_ID))
        WatchManager.getInstance(applicationContext).getWatchById(watchId).firstOrNull()?.let {
            Utils.updateBatteryStats(applicationContext, it)
            return Result.success()
        }
        Timber.w("watchId null or empty")
        return Result.retry()
    }

    companion object {
        private const val SYNC_INTERVAL_MINUTES = 15L
        private const val EXTRA_WATCH_ID: String = "extra_watch_id"

        /** Starts a battery sync worker for the watch with a given ID. */
        fun startWorker(context: Context, watchId: UUID): Boolean {
            Timber.d("Starting BatterySyncWorker for %s", watchId)
            val data = Data.Builder().putString(EXTRA_WATCH_ID, watchId.toString()).build()
            val request = PeriodicWorkRequestBuilder<BatterySyncWorker>(
                SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES,
                PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS, TimeUnit.MILLISECONDS
            ).apply {
                setInputData(data)
            }.build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "$watchId-batterysync",
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )
            return true
        }

        /** Stops the battery sync worker for the watch with a given ID. */
        fun stopWorker(context: Context, watchId: UUID) {
            Timber.d("Stopping BatterySyncWorker for %s", watchId)
            WorkManager.getInstance(context).cancelUniqueWork("$watchId-batterysync")
        }
    }
}

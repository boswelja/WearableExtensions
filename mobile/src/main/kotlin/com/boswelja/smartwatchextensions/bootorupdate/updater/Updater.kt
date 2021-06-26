package com.boswelja.smartwatchextensions.bootorupdate.updater

import android.content.Context
import com.boswelja.migration.Migrator
import com.boswelja.smartwatchextensions.BuildConfig
import com.boswelja.smartwatchextensions.appStateStore
import com.boswelja.smartwatchextensions.appmanager.AppCacheUpdateWorker
import com.boswelja.smartwatchextensions.batterysync.BatterySyncWorker
import com.boswelja.smartwatchextensions.common.preference.PreferenceKey.BATTERY_SYNC_ENABLED_KEY
import com.boswelja.smartwatchextensions.watchmanager.database.WatchDatabase
import com.boswelja.smartwatchextensions.watchmanager.database.WatchSettingsDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class Updater(private val context: Context) : Migrator(
    currentVersion = 1,
    migrations = listOf()
) {

    override suspend fun getOldVersion(): Int {
        return context.appStateStore.data.map { it.lastAppVersion }.first()
    }

    private var lastAppVersion: Int = BuildConfig.VERSION_CODE

    suspend fun checkNeedsUpdate(): Boolean {
        lastAppVersion = context.appStateStore.data.map { it.lastAppVersion }.first()
        context.appStateStore.updateData {
            it.copy(lastAppVersion = BuildConfig.VERSION_CODE)
        }
        return lastAppVersion > 0 && lastAppVersion < BuildConfig.VERSION_CODE
    }

    /**
     * Update the app's working environment.
     * @return The [Result] of the update
     */
    suspend fun doUpdate(): Result {
        if (lastAppVersion <= 401011) {
            val watches = WatchDatabase.getInstance(context)
                .watchDao().getAll().first()
            watches.forEach { watch ->
                AppCacheUpdateWorker.enqueueWorkerFor(context, watch.id)
            }
            return Result.COMPLETED
        }
        // Restart Battery Sync workers to ensure the correct interval is set
        if (lastAppVersion <= 402011) {
            val database = WatchSettingsDatabase.getInstance(context)
            val watchIds = database
                .boolSettings().getByKey(BATTERY_SYNC_ENABLED_KEY)
                .map { settings -> settings.filter { it.value }.map { it.watchId } }
                .first()
            watchIds.forEach { id ->
                BatterySyncWorker.startWorker(context, id)
                BatterySyncWorker.startWorker(context, id)
            }
            database.intSettings().deleteByKey("battery_sync_interval")
            return Result.COMPLETED
        }
        return Result.NOT_NEEDED
    }
}

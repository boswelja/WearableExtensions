package com.boswelja.smartwatchextensions.batterysync

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.boswelja.smartwatchextensions.NotificationChannelHelper
import com.boswelja.smartwatchextensions.R
import com.boswelja.smartwatchextensions.batterysync.database.WatchBatteryStats.Companion.toWatchBatteryStats
import com.boswelja.smartwatchextensions.batterysync.database.WatchBatteryStatsDatabase
import com.boswelja.smartwatchextensions.batterysync.quicksettings.WatchBatteryTileService
import com.boswelja.smartwatchextensions.common.batterysync.BatteryStats
import com.boswelja.smartwatchextensions.common.batterysync.References.BATTERY_STATUS_PATH
import com.boswelja.smartwatchextensions.common.preference.PreferenceKey.BATTERY_CHARGED_NOTI_SENT
import com.boswelja.smartwatchextensions.common.preference.PreferenceKey.BATTERY_CHARGE_THRESHOLD_KEY
import com.boswelja.smartwatchextensions.common.preference.PreferenceKey.BATTERY_LOW_NOTI_SENT
import com.boswelja.smartwatchextensions.common.preference.PreferenceKey.BATTERY_LOW_THRESHOLD_KEY
import com.boswelja.smartwatchextensions.common.preference.PreferenceKey.BATTERY_SYNC_ENABLED_KEY
import com.boswelja.smartwatchextensions.common.preference.PreferenceKey.BATTERY_WATCH_CHARGE_NOTI_KEY
import com.boswelja.smartwatchextensions.common.preference.PreferenceKey.BATTERY_WATCH_LOW_NOTI_KEY
import com.boswelja.smartwatchextensions.common.ui.BaseWidgetProvider
import com.boswelja.smartwatchextensions.main.MainActivity
import com.boswelja.smartwatchextensions.messages.Message
import com.boswelja.smartwatchextensions.messages.sendMessage
import com.boswelja.smartwatchextensions.watchmanager.WatchManager
import com.boswelja.smartwatchextensions.watchmanager.database.WatchDatabase
import com.boswelja.smartwatchextensions.watchmanager.database.WatchSettingsDatabase
import com.boswelja.watchconnection.core.Watch
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import timber.log.Timber

@ExperimentalCoroutinesApi
object Utils {

    private const val BATTERY_CHARGED_NOTI_ID = 408565
    private const val BATTERY_LOW_NOTI_ID = 408566

    const val BATTERY_STATS_NOTI_CHANNEL_ID = "companion_device_charged"

    /**
     * Get up to date battery stats for this device and send it to a specified watch, or all watches
     * with battery sync enabled.
     * @param context [Context].
     * @param watch The [Watch] to send the updated stats to, or null if it should be sent to all
     * possible watches.
     */
    suspend fun updateBatteryStats(context: Context, watch: Watch? = null) {
        withContext(Dispatchers.IO) {
            Timber.i("Updating battery stats for ${watch?.id}")
            val batteryStats = BatteryStats.createForDevice(context)
            Timber.d(
                "percent = %s, isCharging = %s",
                batteryStats?.percent,
                batteryStats?.isCharging
            )
            if (batteryStats != null) {
                val watchManager = WatchManager.getInstance(context)
                if (watch != null) {
                    watchManager.sendMessage(watch, BATTERY_STATUS_PATH, batteryStats.toByteArray())
                } else {
                    watchManager.registeredWatches.first()
                        .filter {
                            watchManager.getBoolSetting(
                                BATTERY_SYNC_ENABLED_KEY, it
                            ).first()
                        }.forEach {
                            watchManager.sendMessage(
                                it, BATTERY_STATUS_PATH, batteryStats.toByteArray()
                            )
                        }
                }
            } else {
                Timber.w("batteryStats null, skipping...")
            }
        }
    }

    suspend fun handleBatteryStats(
        context: Context,
        watchId: UUID,
        batteryStats: BatteryStats
    ) {
        Timber.d("handleBatteryStats(%s, %s) called", watchId, batteryStats)
        withContext(Dispatchers.IO) {
            val database = WatchDatabase.getInstance(context)
            database.watchDao().get(watchId).firstOrNull()?.let { watch ->
                val notificationManager = context.getSystemService<NotificationManager>()!!
                val settingsDb =
                    WatchSettingsDatabase.getInstance(context)
                if (batteryStats.isCharging) {
                    dismissLowNoti(
                        notificationManager,
                        settingsDb,
                        watch
                    )
                    handleWatchChargeNoti(
                        context,
                        notificationManager,
                        batteryStats,
                        settingsDb,
                        watch
                    )
                } else {
                    dismissChargeNoti(
                        notificationManager,
                        settingsDb,
                        watch
                    )
                    handleWatchLowNoti(
                        context,
                        notificationManager,
                        batteryStats,
                        settingsDb,
                        watch
                    )
                }
            }
            // Update stats in database
            WatchBatteryStatsDatabase.getInstance(context)
                .batteryStatsDao().updateStats(batteryStats.toWatchBatteryStats(watchId))

            // Update battery stat widgets
            BaseWidgetProvider.updateWidgets(context)

            // Update QS Tile
            WatchBatteryTileService.requestTileUpdate(context)
        }
    }

    /**
     * Checks if we can send the watch charge notification, and either send or cancel it
     * appropriately.
     * @param batteryStats The [BatteryStats] to send a notification for.
     * @param database The [WatchSettingsDatabase] to access for settings.
     * @param watch The [Watch] to send a notification for.
     */
    private suspend fun handleWatchChargeNoti(
        context: Context,
        notificationManager: NotificationManager,
        batteryStats: BatteryStats,
        database: WatchSettingsDatabase,
        watch: Watch
    ) {
        Timber.d("handleWatchChargeNoti called")
        val chargeThreshold = database.intSettings()
            .get(watch.id, BATTERY_CHARGE_THRESHOLD_KEY).firstOrNull()?.value ?: 90
        val shouldSendChargeNotis = database.boolSettings()
            .get(watch.id, BATTERY_WATCH_CHARGE_NOTI_KEY).firstOrNull()?.value ?: false
        val hasSentNoti = database.boolSettings()
            .get(watch.id, BATTERY_CHARGED_NOTI_SENT).firstOrNull()?.value ?: false
        Timber.d(
            "chargeThreshold = %s, shouldSendChargeNotis = %s, hasSentNoti = %s, percent = %s",
            chargeThreshold,
            shouldSendChargeNotis,
            hasSentNoti,
            batteryStats.percent
        )
        // We can send a charge noti if the user has enabled them, we haven't already sent it and
        // the watch is sufficiently charged.
        val canSendChargeNoti =
            shouldSendChargeNotis && !hasSentNoti && batteryStats.percent >= chargeThreshold
        if (canSendChargeNoti) {
            NotificationChannelHelper.createForBatteryStats(context, notificationManager)
            if (areNotificationsEnabled(context)) {
                Timber.i("Sending charged notification")
                NotificationCompat.Builder(
                    context,
                    BATTERY_STATS_NOTI_CHANNEL_ID
                )
                    .setSmallIcon(R.drawable.battery_full)
                    .setContentTitle(
                        context.getString(R.string.device_battery_charged_noti_title, watch.name)
                    )
                    .setContentText(
                        context.getString(R.string.device_battery_charged_noti_desc)
                            .format(Locale.getDefault(), watch.name, chargeThreshold)
                    )
                    .setContentIntent(getNotiPendingIntent(context))
                    .setLocalOnly(true)
                    .also { notificationManager.notify(BATTERY_CHARGED_NOTI_ID, it.build()) }
            } else {
                Timber.w("Failed to send battery charged notification")
                context.sendMessage(
                    Message(
                        Message.Icon.ERROR,
                        context.getString(R.string.battery_charge_noti_issue_title),
                        context.getString(R.string.battery_charge_noti_issue_summary),
                        Message.Action.LAUNCH_NOTIFICATION_SETTINGS
                    )
                )
                database.boolSettings().updateByKey(BATTERY_WATCH_CHARGE_NOTI_KEY, false)
            }
            database.updateSetting(watch.id, BATTERY_CHARGED_NOTI_SENT, true)
        }
    }

    /**
     * Checks if we can send the watch low notification, and either send or cancel it appropriately.
     * @param batteryStats The [BatteryStats] to send a notification for.
     * @param database The [WatchSettingsDatabase] to access for settings.
     * @param watch The [Watch] to send a notification for.
     */
    private suspend fun handleWatchLowNoti(
        context: Context,
        notificationManager: NotificationManager,
        batteryStats: BatteryStats,
        database: WatchSettingsDatabase,
        watch: Watch
    ) {
        Timber.d("handleWatchLowNoti called")
        val lowThreshold = database.intSettings()
            .get(watch.id, BATTERY_LOW_THRESHOLD_KEY).firstOrNull()?.value ?: 15
        val shouldSendLowNoti = database.boolSettings()
            .get(watch.id, BATTERY_WATCH_LOW_NOTI_KEY).firstOrNull()?.value ?: false
        val hasSentNoti = database.boolSettings()
            .get(watch.id, BATTERY_LOW_NOTI_SENT).firstOrNull()?.value ?: false
        Timber.d(
            "lowThreshold = %s, shouldSendLowNoti = %s, hasSentNoti = %s, batteryPercent = %s",
            lowThreshold,
            shouldSendLowNoti,
            hasSentNoti,
            batteryStats.percent
        )

        // We can send a low noti if the user has enabled them, we haven't already sent it and
        // the watch is sufficiently discharged.
        val canSendLowNoti =
            shouldSendLowNoti && !hasSentNoti && batteryStats.percent <= lowThreshold
        if (canSendLowNoti) {
            NotificationChannelHelper.createForBatteryStats(context, notificationManager)
            if (areNotificationsEnabled(context)) {
                Timber.i("Sending low notification")
                NotificationCompat.Builder(
                    context,
                    BATTERY_STATS_NOTI_CHANNEL_ID
                )
                    .setSmallIcon(R.drawable.battery_alert)
                    .setContentTitle(
                        context.getString(R.string.device_battery_low_noti_title, watch.name)
                    )
                    .setContentIntent(getNotiPendingIntent(context))
                    .setContentText(
                        context.getString(R.string.device_battery_low_noti_desc)
                            .format(Locale.getDefault(), watch.name, lowThreshold)
                    )
                    .setLocalOnly(true)
                    .also { notificationManager.notify(BATTERY_LOW_NOTI_ID, it.build()) }
            } else {
                Timber.w("Failed to send battery low notification")
                context.sendMessage(
                    Message(
                        Message.Icon.ERROR,
                        context.getString(R.string.battery_low_noti_issue_title),
                        context.getString(R.string.battery_low_noti_issue_summary),
                        Message.Action.LAUNCH_NOTIFICATION_SETTINGS
                    )
                )
                database.boolSettings().updateByKey(BATTERY_WATCH_LOW_NOTI_KEY, false)
            }
            database.updateSetting(watch.id, BATTERY_LOW_NOTI_SENT, true)
        }
    }

    private suspend fun dismissChargeNoti(
        notificationManager: NotificationManager,
        database: WatchSettingsDatabase,
        watch: Watch
    ) {
        Timber.d("Dismissing charge notification for %s", watch.id)
        notificationManager.cancel(BATTERY_CHARGED_NOTI_ID)
        database.updateSetting(watch.id, BATTERY_CHARGED_NOTI_SENT, false)
    }

    private suspend fun dismissLowNoti(
        notificationManager: NotificationManager,
        database: WatchSettingsDatabase,
        watch: Watch
    ) {
        Timber.d("Dismissing low notification for %s", watch.id)
        notificationManager.cancel(BATTERY_LOW_NOTI_ID)
        database.updateSetting(watch.id, BATTERY_LOW_NOTI_SENT, false)
    }

    private fun getNotiPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(context, 123, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    /**
     * Checks whether notifications are enabled for the required channel.
     * @return true if notifications are enabled, false otherwise.
     */
    private fun areNotificationsEnabled(context: Context): Boolean {
        val notificationManager = context.getSystemService<NotificationManager>()!!
        notificationManager.getNotificationChannel(BATTERY_STATS_NOTI_CHANNEL_ID).let {
            return it != null && it.importance != NotificationManager.IMPORTANCE_NONE
        }
    }
}

package com.boswelja.devicemanager.batterysync

import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.boswelja.devicemanager.NotificationChannelHelper
import com.boswelja.devicemanager.R
import com.boswelja.devicemanager.batterysync.database.WatchBatteryStats
import com.boswelja.devicemanager.batterysync.database.WatchBatteryStatsDatabase
import com.boswelja.devicemanager.batterysync.widget.WatchBatteryWidget
import com.boswelja.devicemanager.common.batterysync.References.BATTERY_STATUS_PATH
import com.boswelja.devicemanager.common.preference.PreferenceKey.BATTERY_CHARGED_NOTI_SENT
import com.boswelja.devicemanager.common.preference.PreferenceKey.BATTERY_CHARGE_THRESHOLD_KEY
import com.boswelja.devicemanager.common.preference.PreferenceKey.BATTERY_WATCH_CHARGE_NOTI_KEY
import com.boswelja.devicemanager.common.ui.BaseWidgetProvider
import com.boswelja.devicemanager.watchmanager.database.WatchDatabase
import com.boswelja.devicemanager.watchmanager.database.WatchSettingsDatabase
import com.boswelja.devicemanager.watchmanager.item.BoolPreference
import com.boswelja.devicemanager.watchmanager.item.Watch
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class WatchBatteryUpdateReceiver : WearableListenerService() {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val notificationManager: NotificationManager by lazy { getSystemService()!! }

    private lateinit var watchBatteryStats: WatchBatteryStats

    override fun onMessageReceived(messageEvent: MessageEvent?) {
        if (messageEvent?.path == BATTERY_STATUS_PATH) {
            Timber.i("Got ${messageEvent.path}")
            watchBatteryStats = WatchBatteryStats.fromMessage(messageEvent)

            // TODO We shouldn't need to launch a coroutine scope here
            coroutineScope.launch(Dispatchers.IO) {
                val database = WatchDatabase.getInstance(this@WatchBatteryUpdateReceiver)
                database.getById(watchBatteryStats.watchId)?.let {
                    handleNoti(
                        WatchSettingsDatabase.getInstance(this@WatchBatteryUpdateReceiver), it
                    )
                }
                updateStatsInDatabase()
                updateWidgetsForWatch()
            }
        }
    }

    private fun updateStatsInDatabase() {
        WatchBatteryStatsDatabase.getInstance(this).batteryStatsDao().updateStats(watchBatteryStats)
    }

    /**
     * Update all instances of [WatchBatteryWidget] associated with a given watch ID.
     */
    private fun updateWidgetsForWatch() {
        Timber.d("updateWidgetsForWatch called")
        // Fallback to updating all widgets if database isn't open
        BaseWidgetProvider.updateWidgets(this)
    }

    private fun handleNoti(database: WatchSettingsDatabase, watch: Watch) {
        val chargedThreshold =
            database.intPrefDao().get(watch.id, BATTERY_CHARGE_THRESHOLD_KEY)?.value ?: 90
        if (canSendChargedNoti(database, watch.id, chargedThreshold)) {
            notifyWatchCharged(watch, chargedThreshold)
            database.boolPrefDao().update(BoolPreference(watch.id, BATTERY_CHARGED_NOTI_SENT, true))
        } else {
            notificationManager.cancel(BATTERY_CHARGED_NOTI_ID)
            database
                .boolPrefDao()
                .update(BoolPreference(watch.id, BATTERY_CHARGED_NOTI_SENT, false))
        }
    }

    /**
     * Checks whether we can notify the user their watch is charged.
     * @return true if we can send a charged notification, false otherwise.
     */
    private fun canSendChargedNoti(
        database: WatchSettingsDatabase,
        watchId: String,
        chargedThreshold: Int
    ): Boolean {
        val sendChargeNotis =
            database.boolPrefDao().get(watchId, BATTERY_WATCH_CHARGE_NOTI_KEY)?.value == true
        val chargedNotiSent =
            database.boolPrefDao().get(watchId, BATTERY_CHARGED_NOTI_SENT)?.value == true
        return watchBatteryStats.isCharging &&
            sendChargeNotis &&
            (watchBatteryStats.percent >= chargedThreshold) &&
            !chargedNotiSent
    }

    /**
     * Notify the user their watch is charged.
     * @param watch The [Watch] to send a notification for.
     */
    private fun notifyWatchCharged(watch: Watch, chargeThreshold: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            NotificationChannelHelper.createForBatteryCharged(this, notificationManager)

        if (areNotificationsEnabled()) {
            Timber.i("Sending charged notification")
            NotificationCompat.Builder(this, BATTERY_CHARGED_NOTI_CHANNEL_ID)
                .setSmallIcon(R.drawable.battery_full)
                .setContentTitle(getString(R.string.device_charged_noti_title, watch.name))
                .setContentText(
                    getString(R.string.device_charged_noti_desc)
                        .format(Locale.getDefault(), watch.name, chargeThreshold)
                )
                .setLocalOnly(true)
                .also { notificationManager.notify(BATTERY_CHARGED_NOTI_ID, it.build()) }
        } else {
            Timber.w("Failed to send charged notification")
        }
    }

    /**
     * Checks whether notifications are enabled for the required channel.
     * @return true if notifications are enabled, false otherwise.
     */
    private fun areNotificationsEnabled(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.getNotificationChannel(BATTERY_CHARGED_NOTI_CHANNEL_ID).let {
                return it != null && it.importance != NotificationManager.IMPORTANCE_NONE
            }
        } else {
            return NotificationManagerCompat.from(this).areNotificationsEnabled()
        }
    }

    companion object {
        const val BATTERY_CHARGED_NOTI_CHANNEL_ID = "companion_device_charged"
        const val BATTERY_CHARGED_NOTI_ID = 408565
    }
}

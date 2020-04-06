/* Copyright (C) 2019 Jack Boswell <boswelja@outlook.com>
 *
 * This file is part of Wearable Extensions
 *
 * This file, and any part of the Wearable Extensions app/s cannot be copied and/or distributed
 * without permission from Jack Boswell (boswelja) <boswela@outlook.com>
 */
package com.boswelja.devicemanager.batterysync

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.boswelja.devicemanager.R
import com.boswelja.devicemanager.batterysync.database.WatchBatteryStatsDatabase
import com.boswelja.devicemanager.common.Compat
import com.boswelja.devicemanager.common.PreferenceKey.BATTERY_CHARGED_NOTI_SENT
import com.boswelja.devicemanager.common.PreferenceKey.BATTERY_CHARGE_THRESHOLD_KEY
import com.boswelja.devicemanager.common.PreferenceKey.BATTERY_WATCH_CHARGE_NOTI_KEY
import com.boswelja.devicemanager.common.batterysync.References.BATTERY_STATUS_PATH
import com.boswelja.devicemanager.messages.Action
import com.boswelja.devicemanager.messages.Message
import com.boswelja.devicemanager.messages.database.MessageDatabase
import com.boswelja.devicemanager.watchconnectionmanager.WatchConnectionService
import com.boswelja.devicemanager.widgetdb.WidgetDatabase
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WatchBatteryUpdateReceiver : WearableListenerService() {

    private val watchConnectionManagerConnection = object : WatchConnectionService.Connection() {
        override fun onWatchManagerBound(service: WatchConnectionService) {
            coroutineScope.launch {
                val watch = service.getWatchById(watchId)
                withContext(Dispatchers.Default) {
                    if (watch != null) {
                        if (isCharging and
                                (watch.boolPrefs[BATTERY_WATCH_CHARGE_NOTI_KEY] == true) and
                                (batteryPercent >= (watch.intPrefs[BATTERY_CHARGE_THRESHOLD_KEY] ?: 90)) and
                                (watch.boolPrefs[BATTERY_CHARGED_NOTI_SENT] != true)) {
                            sendChargedNoti(watch.name, watch.intPrefs[BATTERY_CHARGE_THRESHOLD_KEY] ?: 90)
                            service.updatePrefInDatabase(watch.id, BATTERY_CHARGED_NOTI_SENT, true)
                        } else {
                            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.cancel(BATTERY_CHARGED_NOTI_ID)
                            service.updatePrefInDatabase(watch.id, BATTERY_CHARGED_NOTI_SENT, false)
                        }
                    }
                }
                unbindService()
            }
        }

        override fun onWatchManagerUnbound() {}
    }

    private val coroutineScope = MainScope()

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var watchId: String
    private var batteryPercent: Int = -1
    private var isCharging: Boolean = false

    override fun onMessageReceived(messageEvent: MessageEvent?) {
        if (messageEvent?.path == BATTERY_STATUS_PATH) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            watchId = messageEvent.sourceNodeId
            val message = String(messageEvent.data, Charsets.UTF_8)
            val messageSplit = message.split("|")
            batteryPercent = messageSplit[0].toInt()
            isCharging = messageSplit[1] == true.toString()

            WatchConnectionService.bind(this, watchConnectionManagerConnection)

            coroutineScope.launch {
                WatchBatteryStatsDatabase.open(this@WatchBatteryUpdateReceiver).also {
                    it.updateWatchBatteryStats(watchId, batteryPercent)
                    it.close()
                }
                WidgetDatabase.updateWatchWidgets(this@WatchBatteryUpdateReceiver, watchId)
            }
        }
    }

    private fun sendChargedNoti(watchName: String, chargeThreshold: Int) {
        if (Compat.areNotificationsEnabled(this, BATTERY_CHARGED_NOTI_CHANNEL_ID)) {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).also { notificationManager ->
                NotificationCompat.Builder(this, BATTERY_CHARGED_NOTI_CHANNEL_ID)
                        .setSmallIcon(R.drawable.battery_full)
                        .setContentTitle(getString(R.string.device_charged_noti_title, watchName))
                        .setContentText(getString(R.string.device_charged_noti_desc).format(watchName, chargeThreshold))
                        .setLocalOnly(true)
                        .build().also { notification ->
                            notificationManager.notify(BATTERY_CHARGED_NOTI_ID, notification)
                        }
            }
        } else {
            MessageDatabase.open(this).apply {
                val message = Message(
                        iconRes = R.drawable.pref_ic_warning,
                        label = getString(R.string.message_watch_charge_noti_warning_label),
                        shortLabel = getString(R.string.message_watch_charge_noti_warning_label_short),
                        desc = getString(R.string.message_watch_charge_noti_warning_desc),
                        buttonLabel = getString(R.string.message_watch_charge_noti_warning_button_label),
                        action = Action.LAUNCH_NOTIFICATION_SETTINGS
                )
                sendMessage(sharedPreferences, message)
            }.also {
                it.close()
            }
        }
    }

    private fun unbindService() {
        unbindService(watchConnectionManagerConnection)
    }

    companion object {

        const val BATTERY_CHARGED_NOTI_CHANNEL_ID = "companion_device_charged"
        const val BATTERY_CHARGED_NOTI_ID = 408565
    }
}

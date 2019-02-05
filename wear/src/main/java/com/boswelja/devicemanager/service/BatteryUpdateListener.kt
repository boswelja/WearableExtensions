/* Copyright (C) 2018 Jack Boswell <boswelja@outlook.com>
 *
 * This file is part of Wearable Extensions
 *
 * This file, and any part of the Wearable Extensions app/s cannot be copied and/or distributed
 * without permission from Jack Boswell (boswelja) <boswela@outlook.com>
 */
package com.boswelja.devicemanager.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.boswelja.devicemanager.R
import com.boswelja.devicemanager.common.CommonUtils
import com.boswelja.devicemanager.common.PreferenceKey
import com.boswelja.devicemanager.common.References
import com.google.android.gms.wearable.*

class BatteryUpdateListener : WearableListenerService() {

//    override fun onDataChanged(dataEvents: DataEventBuffer?) {
//        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(this)
//        for (event: DataEvent in dataEvents!!) {
//            when (event.type) {
//                DataEvent.TYPE_CHANGED -> {
//                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
//                    val percent = dataMap.getInt(References.BATTERY_PERCENT_PATH)
//                    val charging = dataMap.getBoolean(References.BATTERY_CHARGING)
//                    preferenceManager.edit().putInt(References.BATTERY_PERCENT_KEY, percent).apply()
//
//                    if (preferenceManager.getBoolean(PreferenceKey.BATTERY_PHONE_FULL_CHARGE_NOTI_KEY, false) && percent > 90 && charging) {
//                        sendChargedNoti()
//                    }
//                }
//                DataEvent.TYPE_DELETED -> {
//                    preferenceManager.edit().remove(References.BATTERY_PERCENT_KEY).apply()
//                }
//            }
//        }
//        val providerUpdateRequester = ProviderUpdateRequester(this, ComponentName(packageName, PhoneBatteryComplicationProvider::class.java.name))
//        providerUpdateRequester.requestUpdateAll()
//    }

    override fun onMessageReceived(messageEvent: MessageEvent?) {
        Log.d("BatteryUpdateListener", "Message received")
        if (messageEvent?.path == References.BATTERY_STATUS_PATH) {
            val preferenceManager = PreferenceManager.getDefaultSharedPreferences(this)
            val message = String(messageEvent.data, Charsets.UTF_8)
            val messageSplit = message.split("|")
            val percent = messageSplit[0].toInt()
            val charging = messageSplit[1] == "true"
            preferenceManager.edit().putInt(References.BATTERY_PERCENT_KEY, percent).apply()

            Log.d("BatteryUpdateListener", preferenceManager.getBoolean(PreferenceKey.BATTERY_PHONE_FULL_CHARGE_NOTI_KEY, false).toString())
            if (preferenceManager.getBoolean(PreferenceKey.BATTERY_PHONE_FULL_CHARGE_NOTI_KEY, false) && percent > 90 && charging) {
                sendChargedNoti()
            }
            CommonUtils.updateBatteryStats(this)
        }
    }

    private fun sendChargedNoti() {
        Log.d("BatteryUpdateListener", "Phone charged, notifying...")
        val notificationManager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("phone_charged", "Phone Fully Charged", NotificationManager.IMPORTANCE_HIGH)
            channel.enableVibration(true)
            notificationManager.createNotificationChannel(channel)
        }
        val noti = NotificationCompat.Builder(this, "phone_charged")
        noti.setSmallIcon(R.drawable.ic_battery_full)
        noti.setContentTitle("Phone fully charged")
        noti.setContentText("Your phone is fully charged")
        noti.setLocalOnly(true)
        notificationManager.notify(123, noti.build())
    }
}
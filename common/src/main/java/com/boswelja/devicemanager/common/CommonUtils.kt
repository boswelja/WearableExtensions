/* Copyright (C) 2018 Jack Boswell <boswelja@outlook.com>
 *
 * This file is part of Wearable Extensions
 *
 * This file, and any part of the Wearable Extensions app/s cannot be copied and/or distributed
 * without permission from Jack Boswell (boswelja) <boswela@outlook.com>
 */
package com.boswelja.devicemanager.common

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

/*
 * A collection of utility functions used by most, if not all modules
 */
object CommonUtils {

    /**
     * Sends a battery status update to connected devices.
     */
    fun updateBatteryStats(context: Context) {
        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, iFilter)
        val batteryPct = ((batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)!! / batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1).toFloat()) * 100).toInt()
        val charging = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING
        val message = "$batteryPct|$charging"

        Wearable.getCapabilityClient(context)
                .getCapability(References.CAPABILITY_APP, CapabilityClient.FILTER_REACHABLE)
                .addOnSuccessListener { capabilityInfo ->
                    val nodeId = capabilityInfo.nodes.firstOrNull { it.isNearby }?.id ?: capabilityInfo.nodes.firstOrNull()?.id
                    if (nodeId != null) {
                        val messageClient = Wearable.getMessageClient(context)
                        messageClient.sendMessage(
                                nodeId,
                                References.BATTERY_STATUS_PATH,
                                message.toByteArray(Charsets.UTF_8))
                    }
                }
    }

    /**
     * Ensure Interruption Filter state is properly synced between devices.
     */
    fun updateInterruptionFilter(context: Context) {
        val interruptionFilterEnabled = Compat.interruptionFilterEnabled(context)
        updateInterruptionFilter(context, interruptionFilterEnabled)
    }

    /**
     * Sets a new Interruption Filter state across devices.
     * @param interruptionFilterEnabled Whether Interruption Filter should be enabled.
     */
    fun updateInterruptionFilter(context: Context, interruptionFilterEnabled: Boolean) {
        val dataClient = Wearable.getDataClient(context)
        val putDataMapReq = PutDataMapRequest.create(References.DND_STATUS_KEY)
        putDataMapReq.dataMap.putBoolean(References.NEW_DND_STATE_KEY, interruptionFilterEnabled)
        putDataMapReq.setUrgent()
        dataClient.putDataItem(putDataMapReq.asPutDataRequest())
    }
}
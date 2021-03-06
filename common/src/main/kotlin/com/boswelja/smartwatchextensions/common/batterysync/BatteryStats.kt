package com.boswelja.smartwatchextensions.common.batterysync

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/**
 * A data class containing information related to a device's battery.
 * @param percent The device's battery percent.
 * @param isCharging true if the device is charging, false otherwise.
 * @param lastUpdatedMillis The time in milliseconds this data was fetched.
 */
open class BatteryStats(
    open val percent: Int,
    open val isCharging: Boolean,
    open val lastUpdatedMillis: Long = System.currentTimeMillis()
) {

    /**
     * Convert this [BatteryStats] to a [ByteArray].
     */
    fun toByteArray(): ByteArray {
        return "$percent|$isCharging|$lastUpdatedMillis".toByteArray(Charsets.UTF_8)
    }

    companion object {
        /**
         * Get a [BatteryStats] from a [ByteArray].
         */
        fun fromByteArray(byteArray: ByteArray): BatteryStats {
            val message = String(byteArray, Charsets.UTF_8)
            val messageSplit = message.split("|")
            val batteryPercent = messageSplit[0].toInt()
            val isWatchCharging = messageSplit[1] == true.toString()
            val lastUpdatedMillis = messageSplit[2].toLong()
            return BatteryStats(batteryPercent, isWatchCharging, lastUpdatedMillis)
        }

        /**
         * Get an up to date [BatteryStats] for this device.
         * @param context [Context].
         * @return The [BatteryStats] for this device, or null if there was an issue.
         */
        fun createForDevice(context: Context): BatteryStats? {
            val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(null, iFilter)?.let {
                val batteryLevel = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val batteryScale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val percent = (batteryLevel * 100) / batteryScale
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val charging = (status == BatteryManager.BATTERY_STATUS_CHARGING) ||
                    (status == BatteryManager.BATTERY_STATUS_FULL)
                return BatteryStats(percent, charging)
            }
            return null
        }
    }
}

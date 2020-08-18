/* Copyright (C) 2020 Jack Boswell <boswelja@outlook.com>
 *
 * This file is part of Wearable Extensions
 *
 * This file, and any part of the Wearable Extensions app/s cannot be copied and/or distributed
 * without permission from Jack Boswell (boswelja) <boswela@outlook.com>
 */
package com.boswelja.devicemanager.batterysync.ui

import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.content.edit
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreference
import com.boswelja.devicemanager.R
import com.boswelja.devicemanager.batterysync.BatterySyncWorker
import com.boswelja.devicemanager.batterysync.ui.Utils.updateBatteryStats
import com.boswelja.devicemanager.common.PreferenceKey.BATTERY_CHARGE_THRESHOLD_KEY
import com.boswelja.devicemanager.common.PreferenceKey.BATTERY_PHONE_CHARGE_NOTI_KEY
import com.boswelja.devicemanager.common.PreferenceKey.BATTERY_SYNC_ENABLED_KEY
import com.boswelja.devicemanager.common.PreferenceKey.BATTERY_SYNC_INTERVAL_KEY
import com.boswelja.devicemanager.common.PreferenceKey.BATTERY_WATCH_CHARGE_NOTI_KEY
import com.boswelja.devicemanager.common.ui.BasePreferenceFragment
import com.boswelja.devicemanager.watchmanager.WatchManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class BatterySyncPreferenceFragment :
    BasePreferenceFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener,
    Preference.OnPreferenceChangeListener {

    private val coroutineScope = MainScope()
    private val watchManager by lazy { WatchManager.get(requireContext()) }

    private lateinit var batterySyncEnabledPreference: SwitchPreference
    private lateinit var batterySyncIntervalPreference: SeekBarPreference
    private lateinit var batterySyncPhoneChargedNotiPreference: CheckBoxPreference
    private lateinit var batterySyncWatchChargedNotiPreference: CheckBoxPreference
    private lateinit var batteryChargeThresholdPreference: SeekBarPreference

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Timber.d("onSharedPreferenceChanged() called")
        when (key) {
            BATTERY_SYNC_ENABLED_KEY -> {
                batterySyncEnabledPreference.isChecked = sharedPreferences?.getBoolean(key, false)!!
                setBatteryChargeThresholdEnabled()
            }
            BATTERY_PHONE_CHARGE_NOTI_KEY -> {
                batterySyncPhoneChargedNotiPreference.isChecked = sharedPreferences?.getBoolean(key, false)!!
                setBatteryChargeThresholdEnabled()
            }
            BATTERY_WATCH_CHARGE_NOTI_KEY -> {
                batterySyncWatchChargedNotiPreference.isChecked = sharedPreferences?.getBoolean(key, false)!!
                setBatteryChargeThresholdEnabled()
            }
            BATTERY_CHARGE_THRESHOLD_KEY -> {
                updateChargeNotiPrefSummaries()
                coroutineScope.launch(Dispatchers.IO) {
                    watchManager.updatePreferenceOnWatch(key)
                }
            }
        }
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        Timber.d("onPreferenceChange() called")
        return when (val key = preference?.key) {
            BATTERY_SYNC_ENABLED_KEY -> {
                val newBool = newValue == true
                setBatteryChargeThresholdEnabled()
                setBatterySyncEnabled(newBool)
                false
            }
            BATTERY_SYNC_INTERVAL_KEY -> {
                val value = (newValue as Int)
                setBatterySyncInterval(value)
                false
            }
            BATTERY_PHONE_CHARGE_NOTI_KEY,
            BATTERY_WATCH_CHARGE_NOTI_KEY -> {
                val value = newValue == true
                sharedPreferences.edit().putBoolean(key, value).apply()
                coroutineScope.launch(Dispatchers.IO) {
                    watchManager.updatePreferenceOnWatch(key)
                }
                false
            }
            else -> true
        }
    }

    override fun onStart() {
        super.onStart()
        Timber.d("onStart() called")
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        super.onStop()
        Timber.d("onStop() called")
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs_battery_sync)

        batterySyncEnabledPreference = findPreference(BATTERY_SYNC_ENABLED_KEY)!!
        batterySyncIntervalPreference = findPreference(BATTERY_SYNC_INTERVAL_KEY)!!
        batterySyncPhoneChargedNotiPreference = findPreference(BATTERY_PHONE_CHARGE_NOTI_KEY)!!
        batterySyncWatchChargedNotiPreference = findPreference(BATTERY_WATCH_CHARGE_NOTI_KEY)!!
        batteryChargeThresholdPreference = findPreference(BATTERY_CHARGE_THRESHOLD_KEY)!!

        batterySyncEnabledPreference.onPreferenceChangeListener = this
        batterySyncIntervalPreference.onPreferenceChangeListener = this
        batterySyncPhoneChargedNotiPreference.onPreferenceChangeListener = this
        batterySyncWatchChargedNotiPreference.onPreferenceChangeListener = this

        updateChargeNotiPrefSummaries()
        setBatteryChargeThresholdEnabled()
    }

    /**
     * Update preference summaries to reflect changes in [BATTERY_CHARGE_THRESHOLD_KEY].
     */
    private fun updateChargeNotiPrefSummaries() {
        Timber.d("updateChargeNotiPrefSummaries() called")
        val chargeThreshold = batteryChargeThresholdPreference.value
        batterySyncPhoneChargedNotiPreference.summary = getString(R.string.pref_battery_sync_phone_charged_noti_summary).format(chargeThreshold)
        batterySyncWatchChargedNotiPreference.summary = getString(R.string.pref_battery_sync_watch_charged_noti_summary).format(chargeThreshold)
    }

    /**
     * Sets whether the [BATTERY_CHARGE_THRESHOLD_KEY] preference should be enabled.
     */
    private fun setBatteryChargeThresholdEnabled() {
        val sharedPreferences = batteryChargeThresholdPreference.sharedPreferences
        batteryChargeThresholdPreference.isEnabled =
            sharedPreferences.getBoolean(BATTERY_SYNC_ENABLED_KEY, false) &&
            (
                sharedPreferences.getBoolean(BATTERY_PHONE_CHARGE_NOTI_KEY, false) ||
                    sharedPreferences.getBoolean(BATTERY_WATCH_CHARGE_NOTI_KEY, false)
                )
        Timber.i("Battery charge threshold enabled = ${batteryChargeThresholdPreference.isEnabled}")
    }

    /**
     * Sets whether battery sync is enabled or disabled. Handles everything that needs doing
     * when battery sync is toggled.
     * @param enabled true if battery sync should be enabled, false otherwise.
     */
    private fun setBatterySyncEnabled(enabled: Boolean) {
        Timber.i("Setting battery sync enabled to $enabled")
        coroutineScope.launch(Dispatchers.IO) {
            if (enabled) {
                val workerStartSuccessful =
                    BatterySyncWorker.startWorker(requireContext(), watchManager.connectedWatch.value?.id!!)
                if (workerStartSuccessful) {
                    sharedPreferences.edit(commit = true) {
                        putBoolean(BATTERY_SYNC_ENABLED_KEY, enabled).apply()
                    }
                    watchManager.updatePreferenceOnWatch(BATTERY_SYNC_ENABLED_KEY)
                    updateBatteryStats(requireContext(), watchManager.connectedWatch.value?.id)
                } else {
                    withContext(Dispatchers.Main) {
                        Snackbar.make(requireView(), R.string.battery_sync_enable_failed, Snackbar.LENGTH_LONG).show()
                    }
                }
            } else {
                sharedPreferences.edit(commit = true) {
                    putBoolean(BATTERY_SYNC_ENABLED_KEY, enabled)
                }
                watchManager.updatePreferenceOnWatch(BATTERY_SYNC_ENABLED_KEY)
                BatterySyncWorker.stopWorker(requireContext(), watchManager.connectedWatch.value?.id!!)
            }
        }
    }

    /**
     * Sets a new battery sync interval.
     * @param newInterval The new battery sync interval in minutes.
     */
    private fun setBatterySyncInterval(newInterval: Int) {
        Timber.i("Setting new battery sync interval to $newInterval minutes")
        batterySyncIntervalPreference.value = newInterval
        coroutineScope.launch(Dispatchers.IO) {
            sharedPreferences.edit(commit = true) {
                putInt(BATTERY_SYNC_INTERVAL_KEY, newInterval)
            }
            watchManager.updatePreferenceInDatabase(BATTERY_SYNC_INTERVAL_KEY, newInterval)
            BatterySyncWorker.stopWorker(requireContext(), watchManager.connectedWatch.value?.id!!)
            BatterySyncWorker.startWorker(requireContext(), watchManager.connectedWatch.value?.id!!)
        }
    }
}
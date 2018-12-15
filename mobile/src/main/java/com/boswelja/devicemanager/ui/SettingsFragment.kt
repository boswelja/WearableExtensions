/* Copyright (C) 2018 Jack Boswell <boswelja@outlook.com>
 *
 * This file is part of Wearable Extensions
 *
 * This file, and any part of the Wearable Extensions app/s cannot be copied and/or distributed
 * without permission from Jack Boswell (boswelja) <boswela@outlook.com>
 */
package com.boswelja.devicemanager.ui

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.*
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import com.boswelja.devicemanager.R
import com.boswelja.devicemanager.Utils
import com.boswelja.devicemanager.common.DnDHandler
import com.boswelja.devicemanager.common.PreferenceKey
import com.google.android.material.snackbar.Snackbar

class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private lateinit var mainActivity: MainActivity
    private lateinit var lockPhoneEnabledPref: SwitchPreference
    private lateinit var batterySyncIntervalPref: ListPreference
    private lateinit var dndSyncPhoneToWatchPref: CheckBoxPreference
    private lateinit var dndSyncWatchToPhonePref: CheckBoxPreference
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var notificationManager: NotificationManager
    private var isGrantingAdminPerms = false

    override fun onPreferenceClick(preference: Preference?): Boolean {
        return when (preference?.key) {
            PreferenceKey.BATTERY_SYNC_NOW_KEY -> {
                Utils.updateBatteryStats(context!!)
                Snackbar.make(view!!, getString(R.string.pref_battery_sync_resync_complete), Snackbar.LENGTH_SHORT).show()
                true
            }
            PreferenceKey.NOTIFICATION_SETTINGS_KEY -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val settingsIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context!!.packageName)
                    startActivity(settingsIntent)
                }
                true
            }
            else -> false
        }
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        return when (preference?.key) {
            PreferenceKey.HIDE_APP_ICON_KEY -> {
                mainActivity.changeAppIconVisibility(newValue!! == true)
                true
            }
            PreferenceKey.LOCK_PHONE_ENABLED -> {
                val value = newValue == true
                if (!mainActivity.isDeviceAdmin()) {
                    AlertDialog.Builder(context!!)
                            .setTitle(R.string.grant_device_admin_perm_dialog_title)
                            .setMessage(R.string.grant_device_admin_perm_dialog_message)
                            .setPositiveButton(R.string.dialog_button_grant) { _, _ ->
                                isGrantingAdminPerms = true
                                Utils.requestDeviceAdminPerms(context!!)
                            }
                            .setNegativeButton(R.string.dialog_button_cancel) { _, _ ->
                                preference.sharedPreferences.edit()
                                        .putBoolean(preference.key, false)
                                        .apply()
                                (preference as SwitchPreference).isChecked = false
                            }
                            .show()
                } else {
                    preference.sharedPreferences.edit()
                            .putBoolean(preference.key, value)
                            .apply()
                    (preference as SwitchPreference).isChecked = value
                    Utils.updateWatchPrefs(context!!)
                }
                false
            }
            PreferenceKey.BATTERY_SYNC_INTERVAL_KEY -> {
                val listPref = preference as ListPreference
                val value = newValue.toString().toLong()
                listPref.summary = listPref.entries[listPref.entryValues.indexOf(value.toString())]
                mainActivity.createBatterySyncJob(value)
                true
            }
            PreferenceKey.BATTERY_SYNC_ENABLED_KEY -> {
                if (newValue!! == true) {
                    mainActivity.createBatterySyncJob(batterySyncIntervalPref.value.toLong())
                } else {
                    mainActivity.stopBatterySyncJob()
                }
                true
            }
            PreferenceKey.BATTERY_PHONE_FULL_CHARGE_NOTI_KEY -> {
                Utils.updateWatchPrefs(context!!)
                true
            }
            PreferenceKey.DND_SYNC_ENABLED_KEY -> {
                if (newValue!! == true) {
                    //TODO Actually check if watch has correct permissions
                    AlertDialog.Builder(context)
                            .setTitle(R.string.dnd_sync_adb_dialog_title)
                            .setMessage(String.format(getString(R.string.dnd_sync_adb_dialog_message), getString(R.string.dnd_sync_adb_command)))
                            .setPositiveButton(R.string.dialog_button_done) { _, _ ->
                                preference.sharedPreferences.edit().putBoolean(PreferenceKey.DND_SYNC_ENABLED_KEY, true)
                                (preference as SwitchPreference).isChecked = true
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    activity?.startForegroundService(Intent(context, DnDHandler::class.java))
                                } else {
                                    activity?.startService(Intent(context, DnDHandler::class.java))
                                }
                                Utils.updateWatchPrefs(context!!)
                            }
                            .setNegativeButton(R.string.dialog_button_cancel) { _, _ ->
                                preference.sharedPreferences.edit().putBoolean(PreferenceKey.DND_SYNC_ENABLED_KEY, false)
                                (preference as SwitchPreference).isChecked = false
                                Utils.updateWatchPrefs(context!!)
                            }
                            .setNeutralButton(R.string.dialog_button_copy) { _, _ ->
                                preference.sharedPreferences.edit().putBoolean(PreferenceKey.DND_SYNC_ENABLED_KEY, false)
                                (preference as SwitchPreference).isChecked = false
                                val clipboardManager = context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("DnD sync ADB command", context!!.getString(R.string.dnd_sync_adb_command))
                                clipboardManager.primaryClip = clip
                                Toast.makeText(context, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                                Utils.updateWatchPrefs(context!!)
                            }
                            .show()
                } else {
                    preference.sharedPreferences.edit().putBoolean(PreferenceKey.DND_SYNC_ENABLED_KEY, false)
                    (preference as SwitchPreference).isChecked = false
                    Utils.updateWatchPrefs(context!!)
                }
                false
            }
            PreferenceKey.DND_SYNC_SEND_KEY -> {
                val value = newValue == true
                preference.sharedPreferences.edit().putBoolean(preference.key, value).apply()
                dndSyncPhoneToWatchPref.isChecked = value
                Utils.updateWatchPrefs(context!!)
                false
            }
            PreferenceKey.DND_SYNC_RECEIVE_KEY -> {
                val value = newValue == true
                preference.sharedPreferences.edit().putBoolean(preference.key, value).apply()
                dndSyncWatchToPhonePref.isChecked = value
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || notificationManager.isNotificationPolicyAccessGranted) {
                    Utils.updateWatchPrefs(context!!)
                } else {
                    Toast.makeText(context, getString(R.string.request_noti_policy_access_message), Toast.LENGTH_SHORT).show()
                    startActivityForResult(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS), 12345)
                }
                false
            }
            else -> false
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        mainActivity = activity as MainActivity

        addPreferencesFromResource(R.xml.prefs_general)
        setupGeneralPrefs()

        addPreferencesFromResource(R.xml.prefs_lock_phone)
        setupPhoneLockPrefs()

        addPreferencesFromResource(R.xml.prefs_battery_sync)
        setupBatterySyncPrefs()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            addPreferencesFromResource(R.xml.prefs_dnd_sync)
            setupDnDPrefs()
        }
    }

    private fun setupGeneralPrefs() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            findPreference(PreferenceKey.NOTIFICATION_SETTINGS_KEY).onPreferenceClickListener = this
        }

        val hideAppIconPref = findPreference(PreferenceKey.HIDE_APP_ICON_KEY)
        hideAppIconPref.onPreferenceChangeListener = this
    }

    private fun setupPhoneLockPrefs() {
        lockPhoneEnabledPref = findPreference(PreferenceKey.LOCK_PHONE_ENABLED) as SwitchPreference
        lockPhoneEnabledPref.onPreferenceChangeListener = this
    }

    private fun setupBatterySyncPrefs() {
        batterySyncIntervalPref = findPreference(PreferenceKey.BATTERY_SYNC_INTERVAL_KEY) as ListPreference
        batterySyncIntervalPref.onPreferenceChangeListener = this
        batterySyncIntervalPref.summary = batterySyncIntervalPref.entry

        val batterySyncEnabledPref = findPreference(PreferenceKey.BATTERY_SYNC_ENABLED_KEY)
        batterySyncEnabledPref.onPreferenceChangeListener = this

        val batterySyncForcePref = findPreference(PreferenceKey.BATTERY_SYNC_NOW_KEY)
        batterySyncForcePref.onPreferenceClickListener = this
    }

    private fun setupDnDPrefs() {
        val dndSyncEnabledPref = findPreference(PreferenceKey.DND_SYNC_ENABLED_KEY)
        dndSyncEnabledPref.onPreferenceChangeListener = this
        dndSyncEnabledPref.onPreferenceClickListener = this

        dndSyncPhoneToWatchPref = findPreference(PreferenceKey.DND_SYNC_SEND_KEY) as CheckBoxPreference
        dndSyncPhoneToWatchPref.onPreferenceChangeListener = this

        dndSyncWatchToPhonePref = findPreference(PreferenceKey.DND_SYNC_RECEIVE_KEY) as CheckBoxPreference
        dndSyncWatchToPhonePref.onPreferenceChangeListener = this
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 12345) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || notificationManager.isNotificationPolicyAccessGranted) {
                Utils.updateWatchPrefs(context!!)
            } else {
                sharedPrefs.edit().putBoolean(PreferenceKey.DND_SYNC_RECEIVE_KEY, false).apply()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isGrantingAdminPerms) {
            val isAdmin = mainActivity.isDeviceAdmin()
            sharedPrefs.edit().putBoolean(PreferenceKey.LOCK_PHONE_ENABLED, isAdmin).apply()
            lockPhoneEnabledPref.isChecked = isAdmin
            isGrantingAdminPerms = false
            Utils.updateWatchPrefs(context!!)
        }
    }
}
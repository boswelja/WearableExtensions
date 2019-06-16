/* Copyright (C) 2019 Jack Boswell <boswelja@outlook.com>
 *
 * This file is part of Wearable Extensions
 *
 * This file, and any part of the Wearable Extensions app/s cannot be copied and/or distributed
 * without permission from Jack Boswell (boswelja) <boswela@outlook.com>
 */
package com.boswelja.devicemanager.ui.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.boswelja.devicemanager.R
import com.boswelja.devicemanager.ui.base.BaseDayNightActivity.Companion.DAYNIGHT_MODE_KEY
import com.boswelja.devicemanager.ui.base.BasePreferenceFragment

class AppSettingsFragment :
        BasePreferenceFragment(),
        Preference.OnPreferenceClickListener {

    private lateinit var openNotiSettingsPreference: Preference
    private lateinit var daynightModePreference: ListPreference

    override fun onPreferenceClick(preference: Preference?): Boolean {
        return when (preference?.key) {
            OPEN_NOTI_SETTINGS_KEY -> {
                Intent().apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                        putExtra(Settings.EXTRA_APP_PACKAGE, context?.packageName!!)
                    } else {
                        action = "android.settings.APP_NOTIFICATION_SETTINGS"
                        putExtra("app_package", context?.packageName!!)
                        putExtra("app_uid", context?.applicationInfo?.uid!!)
                    }
                }.also { startActivity(it) }
                true
            }
            else -> false
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs_app_settings)
        openNotiSettingsPreference = findPreference(OPEN_NOTI_SETTINGS_KEY)!!
        openNotiSettingsPreference.onPreferenceClickListener = this
        daynightModePreference = findPreference(DAYNIGHT_MODE_KEY)!!
    }

    override fun onResume() {
        super.onResume()
        val notificationsAllowed = NotificationManagerCompat.from(context!!).areNotificationsEnabled()
        if (notificationsAllowed) {
            openNotiSettingsPreference.apply {
                summary = getString(R.string.pref_noti_status_summary_enabled)
                setIcon(R.drawable.pref_ic_notifications_enabled)
            }
        } else {
            openNotiSettingsPreference.apply {
                summary = getString(R.string.pref_noti_status_summary_disabled)
                setIcon(R.drawable.pref_ic_notifications_disabled)
            }
        }

        daynightModePreference.summary = daynightModePreference.entry
    }

    companion object {
        const val OPEN_NOTI_SETTINGS_KEY = "show_noti_settings"
    }
}

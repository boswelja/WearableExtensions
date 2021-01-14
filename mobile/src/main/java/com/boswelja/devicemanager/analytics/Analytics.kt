package com.boswelja.devicemanager.analytics

import android.content.Context
import androidx.preference.PreferenceManager
import com.boswelja.devicemanager.common.preference.SyncPreferences
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent

class Analytics(context: Context) {
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(context)
    }

    fun logSettingChanged(key: String, value: Any) {
        if (sharedPreferences.getBoolean(ANALYTICS_ENABLED_KEY, false)) {
            if (key in SyncPreferences.ALL_PREFS) {
                logExtensionSettingChanged(key, value)
            } else {
                logAppSettingChanged(key, value)
            }
        }
    }

    fun logExtensionSettingChanged(key: String, value: Any) {
        if (sharedPreferences.getBoolean(ANALYTICS_ENABLED_KEY, false)) {
            firebaseAnalytics.logEvent(EVENT_EXTENSION_SETTING_CHANGED) {
                param(FirebaseAnalytics.Param.ITEM_ID, key)
                param(FirebaseAnalytics.Param.VALUE, value.toString())
            }
        }
    }

    fun logAppSettingChanged(key: String, value: Any) {
        if (sharedPreferences.getBoolean(ANALYTICS_ENABLED_KEY, false)) {
            firebaseAnalytics.logEvent(EVENT_APP_SETTING_CHANGED) {
                param(FirebaseAnalytics.Param.ITEM_ID, key)
                param(FirebaseAnalytics.Param.VALUE, value.toString())
            }
        }
    }

    fun logWatchRegistered() {
        if (sharedPreferences.getBoolean(ANALYTICS_ENABLED_KEY, false)) {
            firebaseAnalytics.logEvent(EVENT_WATCH_REGISTERED, null)
        }
    }

    fun logWatchRemoved() {
        if (sharedPreferences.getBoolean(ANALYTICS_ENABLED_KEY, false)) {
            firebaseAnalytics.logEvent(EVENT_WATCH_REMOVED, null)
        }
    }

    fun logStorageManagerAction(action: String) {
        if (sharedPreferences.getBoolean(ANALYTICS_ENABLED_KEY, false)) {
            firebaseAnalytics.logEvent(EVENT_STORAGE_MANAGER) {
                param(FirebaseAnalytics.Param.METHOD, action)
            }
        }
    }

    companion object {
        const val ANALYTICS_ENABLED_KEY = "send_analytics"

        private const val EVENT_EXTENSION_SETTING_CHANGED = "extension_setting_changed"
        private const val EVENT_APP_SETTING_CHANGED = "app_setting_changed"
        private const val EVENT_WATCH_REGISTERED = "watch_registered"
        private const val EVENT_WATCH_REMOVED = "watch_registered"
        private const val EVENT_STORAGE_MANAGER = "storage_manager"
    }
}
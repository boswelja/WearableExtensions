package com.boswelja.devicemanager.dndsync.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import com.boswelja.devicemanager.R
import com.boswelja.devicemanager.common.Compat
import com.boswelja.devicemanager.common.connection.Capability
import com.boswelja.devicemanager.common.preference.PreferenceKey.DND_SYNC_TO_PHONE_KEY
import com.boswelja.devicemanager.common.preference.PreferenceKey.DND_SYNC_TO_WATCH_KEY
import com.boswelja.devicemanager.common.preference.PreferenceKey.DND_SYNC_WITH_THEATER_KEY
import com.boswelja.devicemanager.common.ui.activity.BasePreferenceFragment
import com.boswelja.devicemanager.dndsync.DnDLocalChangeService
import com.boswelja.devicemanager.dndsync.ui.helper.DnDSyncHelperActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class DnDSyncPreferenceFragment :
    BasePreferenceFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener,
    Preference.OnPreferenceChangeListener {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val dndSyncToWatchPreference: SwitchPreference by lazy {
        findPreference(DND_SYNC_TO_WATCH_KEY)!!
    }
    private val dndSyncToPhonePreference: SwitchPreference by lazy {
        findPreference(DND_SYNC_TO_PHONE_KEY)!!
    }
    private val dndSyncWithTheaterPreference: SwitchPreference by lazy {
        findPreference(DND_SYNC_WITH_THEATER_KEY)!!
    }

    private val notiPolicyAccessLauncher: ActivityResultLauncher<Intent>

    private var changingKey: String? = null

    init {
        notiPolicyAccessLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            changingKey?.let {
                if (Compat.canSetDnD(requireContext())) {
                    findPreference<SwitchPreference>(it)!!.isChecked = true
                    coroutineScope.launch {
                        sharedPreferences.edit(commit = true) { putBoolean(it, true) }
                        watchManager.updatePreference(connectedWatch!!, it, true)
                    }
                }
                changingKey = null
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            DND_SYNC_TO_WATCH_KEY -> {
                dndSyncToWatchPreference.isChecked = sharedPreferences?.getBoolean(key, false)!!
            }
            DND_SYNC_TO_PHONE_KEY -> {
                dndSyncToPhonePreference.isChecked = sharedPreferences?.getBoolean(key, false)!!
            }
            DND_SYNC_WITH_THEATER_KEY -> {
                dndSyncWithTheaterPreference.isChecked = sharedPreferences?.getBoolean(key, false)!!
            }
        }
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        return when (
            val key = preference?.key
        ) {
            DND_SYNC_TO_WATCH_KEY -> {
                val enabled = newValue == true
                if (enabled) {
                    startDnDSyncHelper()
                } else {
                    setDnDSyncToWatch(enabled)
                }
                false
            }
            DND_SYNC_TO_PHONE_KEY, DND_SYNC_WITH_THEATER_KEY -> {
                val enabled = newValue == true
                setDnDSyncFromWatchPreference(key, enabled)
                false
            }
            else -> true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeConnectedWatchId()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Timber.d("onCreatePreferences() called")
        addPreferencesFromResource(R.xml.prefs_dnd_sync)

        dndSyncToWatchPreference.onPreferenceChangeListener = this
        dndSyncToPhonePreference.onPreferenceChangeListener = this
        dndSyncWithTheaterPreference.onPreferenceChangeListener = this
    }

    override fun onStart() {
        super.onStart()
        Timber.d("onStart() called")
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        watchManager.selectedWatch.observe(viewLifecycleOwner) {
            dndSyncToWatchPreference.isEnabled = it?.hasCapability(Capability.RECEIVE_DND) == true
        }
    }

    override fun onStop() {
        super.onStop()
        Timber.d("onStop() called")
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    /**
     * Sets whether DnD Sync to watch is enabled.
     * @param enabled true if DnD Sync to Watch should be enabled, false otherwise.
     */
    private fun setDnDSyncToWatch(enabled: Boolean) {
        Timber.i("Setting DnD Sync to watch to $enabled")
        dndSyncToWatchPreference.isChecked = enabled
        coroutineScope.launch {
            sharedPreferences.edit(commit = true) { putBoolean(DND_SYNC_TO_WATCH_KEY, enabled) }
            watchManager.updatePreference(
                connectedWatch!!, DND_SYNC_TO_WATCH_KEY, enabled
            )
        }
        if (enabled) {
            Timber.i("Starting DnDLocalChangeService")
            Intent(requireContext(), DnDLocalChangeService::class.java).also {
                ContextCompat.startForegroundService(requireContext(), it)
            }
        }
    }

    /**
     * Sets whether a DnD Sync from watch preference is enabled.
     * @param key The key of the preference to set.
     * @param enabled true if the preference should be enabled, false otherwise.
     */
    private fun setDnDSyncFromWatchPreference(key: String, enabled: Boolean) {
        Timber.i("Setting $key to $enabled")
        var updateState = false
        if (enabled) {
            if (Compat.canSetDnD(requireContext())) {
                updateState = true
            } else {
                changingKey = key
                Toast.makeText(
                    context,
                    getString(R.string.dnd_sync_request_policy_access_message),
                    Toast.LENGTH_SHORT
                ).show()
                notiPolicyAccessLauncher
                    .launch(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }
        }
        if (updateState) {
            coroutineScope.launch {
                sharedPreferences.edit(commit = true) { putBoolean(key, enabled) }
                watchManager.updatePreference(connectedWatch!!, key, enabled)
            }
        }
    }

    /** Starts a new [DnDSyncHelperActivity] instance and shows it. */
    private fun startDnDSyncHelper() {
        Intent(context, DnDSyncHelperActivity::class.java).also { startActivity(it) }
    }
}

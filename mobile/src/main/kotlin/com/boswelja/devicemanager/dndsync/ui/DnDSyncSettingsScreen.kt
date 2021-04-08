package com.boswelja.devicemanager.dndsync.ui

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boswelja.devicemanager.R
import com.boswelja.devicemanager.common.Compat
import com.boswelja.devicemanager.common.preference.PreferenceKey.DND_SYNC_TO_PHONE_KEY
import com.boswelja.devicemanager.common.preference.PreferenceKey.DND_SYNC_WITH_THEATER_KEY
import com.boswelja.devicemanager.common.ui.SwitchPreference
import com.boswelja.devicemanager.dndsync.ui.helper.DnDSyncHelperActivity

@ExperimentalMaterialApi
@Composable
fun DnDSyncSettingsScreen() {
    Column {
        val viewModel: DnDSyncSettingsViewModel = viewModel()
        val context = LocalContext.current

        val dndSyncToWatch by viewModel.syncToWatch.observeAsState()
        val dndSyncToPhone by viewModel.syncToPhone.observeAsState()
        val dndSyncWithTheater by viewModel.syncWithTheater.observeAsState()

        var changingKey = remember<String?> { null }
        val notiPolicyAccessLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                changingKey?.let {
                    if (Compat.canSetDnD(context)) {
                        when (it) {
                            DND_SYNC_TO_PHONE_KEY -> viewModel.setSyncToPhone(true)
                            DND_SYNC_WITH_THEATER_KEY -> viewModel.setSyncWithTheater(true)
                        }
                    }
                    changingKey = null
                }
            }

        SwitchPreference(
            text = stringResource(R.string.pref_dnd_sync_to_watch_title),
            secondaryText = stringResource(R.string.pref_dnd_sync_to_watch_summary),
            isChecked = dndSyncToWatch == true,
            onCheckChanged = {
                if (!it) {
                    viewModel.setSyncToWatch(false)
                } else {
                    Intent(context, DnDSyncHelperActivity::class.java)
                        .also { intent -> context.startActivity(intent) }
                }
            }
        )
        SwitchPreference(
            text = stringResource(R.string.pref_dnd_sync_to_phone_title),
            secondaryText = stringResource(R.string.pref_dnd_sync_to_phone_summary),
            isChecked = dndSyncToPhone == true,
            onCheckChanged = {
                if ((it && Compat.canSetDnD(context)) || !it) {
                    viewModel.setSyncToPhone(it)
                } else {
                    changingKey = DND_SYNC_TO_PHONE_KEY
                    Toast.makeText(
                        context,
                        context.getString(R.string.dnd_sync_request_policy_access_message),
                        Toast.LENGTH_SHORT
                    ).show()
                    notiPolicyAccessLauncher
                        .launch(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                }
            }
        )
        SwitchPreference(
            text = stringResource(R.string.pref_dnd_sync_with_theater_title),
            secondaryText = stringResource(R.string.pref_dnd_sync_with_theater_summary),
            isChecked = dndSyncWithTheater == true,
            onCheckChanged = {
                if ((it && Compat.canSetDnD(context)) || !it) {
                    viewModel.setSyncWithTheater(it)
                } else {
                    changingKey = DND_SYNC_WITH_THEATER_KEY
                    Toast.makeText(
                        context,
                        context.getString(R.string.dnd_sync_request_policy_access_message),
                        Toast.LENGTH_SHORT
                    ).show()
                    notiPolicyAccessLauncher
                        .launch(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                }
            }
        )
    }
}

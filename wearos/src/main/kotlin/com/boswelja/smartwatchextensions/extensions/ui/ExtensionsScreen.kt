package com.boswelja.smartwatchextensions.extensions.ui

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ChipDefaults.secondaryChipColors
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import androidx.wear.widget.ConfirmationOverlay
import com.boswelja.smartwatchextensions.R
import com.boswelja.smartwatchextensions.about.ui.AboutActivity
import com.boswelja.smartwatchextensions.batterysync.ui.BatteryStatsCard
import com.boswelja.smartwatchextensions.common.InsetDefaults.RoundScreenInset
import com.boswelja.smartwatchextensions.common.RotaryHandler
import com.boswelja.smartwatchextensions.common.isScreenRound
import com.boswelja.smartwatchextensions.common.roundScreenPadding
import com.boswelja.smartwatchextensions.common.showConfirmationOverlay
import com.boswelja.smartwatchextensions.common.startActivity
import com.boswelja.smartwatchextensions.phonelocking.ui.PhoneLockingCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
@Composable
fun ExtensionsScreen() {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    RotaryHandler { delta ->
        coroutineScope.launch {
            scrollState.scrollBy(delta)
        }
    }

    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .padding(8.dp)
            .roundScreenPadding(isScreenRound(), PaddingValues(vertical = RoundScreenInset)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Extensions(Modifier.fillMaxWidth())
        Links(Modifier.fillMaxWidth())
    }
}

@ExperimentalCoroutinesApi
@Composable
fun Extensions(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val view = LocalView.current
        val coroutineScope = rememberCoroutineScope()
        val viewModel: ExtensionsViewModel = viewModel()

        val batterySyncEnabled by viewModel.batterySyncEnabled.collectAsState(false, Dispatchers.IO)
        val phoneLockingEnabled by viewModel.phoneLockingEnabled
            .collectAsState(false, Dispatchers.IO)
        val batteryPercent by viewModel.batteryPercent.collectAsState(0, Dispatchers.IO)
        val phoneName by viewModel.phoneName
            .collectAsState(stringResource(R.string.default_phone_name), Dispatchers.IO)

        val cardModifier = Modifier.fillMaxWidth()
        BatteryStatsCard(
            modifier = cardModifier,
            enabled = batterySyncEnabled,
            percent = batteryPercent,
            phoneName = phoneName
        ) {
            coroutineScope.launch {
                val result = viewModel.requestBatteryStats()
                if (result) {
                    view.showConfirmationOverlay(
                        type = ConfirmationOverlay.SUCCESS_ANIMATION,
                        message = view.context.getString(R.string.battery_sync_refresh_success)
                    )
                } else {
                    view.showConfirmationOverlay(
                        type = ConfirmationOverlay.FAILURE_ANIMATION,
                        message = view.context.getString(R.string.phone_not_connected)
                    )
                }
            }
        }
        PhoneLockingCard(
            modifier = cardModifier,
            enabled = phoneLockingEnabled,
            phoneName = phoneName
        ) {
            coroutineScope.launch {
                val result = viewModel.requestLockPhone()
                if (result) {
                    view.showConfirmationOverlay(
                        type = ConfirmationOverlay.SUCCESS_ANIMATION,
                        message = view.context.getString(R.string.lock_phone_success)
                    )
                } else {
                    view.showConfirmationOverlay(
                        type = ConfirmationOverlay.FAILURE_ANIMATION,
                        message = view.context.getString(R.string.phone_not_connected)
                    )
                }
            }
        }
    }
}

@Composable
fun Links(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Chip(
            colors = secondaryChipColors(),
            label = {
                Text(stringResource(R.string.about_app_title))
            },
            icon = {
                Icon(
                    modifier = Modifier.size(ChipDefaults.IconSize),
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null
                )
            },
            onClick = {
                context.startActivity<AboutActivity>()
            }
        )
    }
}

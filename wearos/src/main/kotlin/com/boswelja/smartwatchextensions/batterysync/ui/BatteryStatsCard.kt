package com.boswelja.smartwatchextensions.batterysync.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.boswelja.smartwatchextensions.R
import com.boswelja.smartwatchextensions.common.BatteryIcon
import com.boswelja.smartwatchextensions.common.ExtensionCard

@Composable
fun BatteryStatsCard(
    modifier: Modifier = Modifier,
    percent: Int,
    phoneName: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    ExtensionCard(
        modifier = modifier,
        icon = { size ->
            val iconModifier = Modifier.size(size)
            if (enabled) {
                BatteryIcon(percent, modifier = iconModifier)
            } else {
                Icon(
                    modifier = iconModifier,
                    painter = painterResource(R.drawable.battery_unknown),
                    contentDescription = null
                )
            }
        },
        hintText = {
            if (enabled) {
                Text(
                    stringResource(R.string.battery_sync_hint_text, phoneName),
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    stringResource(R.string.battery_sync_disabled),
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Center
                )
            }
        },
        content = {
            if (enabled) {
                Text(
                    stringResource(R.string.battery_percent, percent.toString()),
                    style = MaterialTheme.typography.display3,
                    textAlign = TextAlign.Center
                )
            }
        },
        onClick = onClick,
        enabled = enabled
    )
}

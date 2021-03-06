package com.boswelja.smartwatchextensions.appmanager.ui

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.boswelja.smartwatchextensions.R
import com.boswelja.smartwatchextensions.common.ui.FeatureSummarySmall

@Composable
fun AppSummarySmall(
    modifier: Modifier = Modifier,
    appCount: Int
) {
    val context = LocalContext.current
    FeatureSummarySmall(
        modifier = modifier,
        icon = {
            Icon(
                Icons.Outlined.Apps,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.33f)
                    .aspectRatio(1f)
            )
        },
        text = {
            Text(
                context.resources.getQuantityString(
                    R.plurals.app_manager_app_count, appCount, appCount
                ),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.h5
            )
        }
    )
}

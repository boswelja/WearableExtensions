package com.boswelja.devicemanager.dndsync.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boswelja.devicemanager.common.ui.AppTheme
import com.boswelja.devicemanager.common.ui.UpNavigationWatchPickerAppBar

class DnDSyncPreferenceActivity : AppCompatActivity() {

    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val viewModel: DnDSyncPreferenceViewModel = viewModel()
                val registeredWatches by viewModel.watchManager.registeredWatches.observeAsState()
                val selectedWatch by viewModel.watchManager.selectedWatch.observeAsState()
                Scaffold(
                    topBar = {
                        UpNavigationWatchPickerAppBar(
                            onNavigateUp = { finish() },
                            watches = registeredWatches,
                            selectedWatch = selectedWatch,
                            onWatchSelected = { viewModel.watchManager.selectWatchById(it.id) }
                        )
                    }
                ) {
                    DnDSyncPreferences()
                }
            }
        }
    }
}

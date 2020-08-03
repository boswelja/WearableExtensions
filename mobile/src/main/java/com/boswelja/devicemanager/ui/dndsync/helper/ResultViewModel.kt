package com.boswelja.devicemanager.ui.dndsync.helper

import android.app.Application
import android.content.Intent
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import com.boswelja.devicemanager.common.Compat
import com.boswelja.devicemanager.common.PreferenceKey
import com.boswelja.devicemanager.dndsync.DnDLocalChangeService
import com.boswelja.devicemanager.watchmanager.WatchManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class ResultViewModel(application: Application) : AndroidViewModel(application) {

    private val coroutineJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + coroutineJob)

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
    private val watchManager = WatchManager.get(application)

    override fun onCleared() {
        Timber.i("onCleared() called")
        super.onCleared()
        coroutineJob.cancel()
    }

    fun setSyncToWatch(isEnabled: Boolean) {
        Timber.i("enableSyncToWatch() called")
        coroutineScope.launch {
            sharedPreferences.edit(commit = true) { putBoolean(PreferenceKey.DND_SYNC_TO_WATCH_KEY, isEnabled) }
            watchManager.updatePreferenceOnWatch(PreferenceKey.DND_SYNC_TO_WATCH_KEY)
            if (isEnabled) {
                val context = getApplication<Application>()
                Compat.startForegroundService(context, Intent(context, DnDLocalChangeService::class.java))
            }
        }
    }

}
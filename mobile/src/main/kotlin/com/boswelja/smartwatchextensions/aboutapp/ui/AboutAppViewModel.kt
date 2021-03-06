package com.boswelja.smartwatchextensions.aboutapp.ui

import android.app.Application
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.boswelja.smartwatchextensions.common.connection.Messages.REQUEST_APP_VERSION
import com.boswelja.smartwatchextensions.watchmanager.WatchManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import timber.log.Timber

@ExperimentalCoroutinesApi
class AboutAppViewModel internal constructor(
    application: Application,
    private val watchManager: WatchManager,
    val customTabsIntent: CustomTabsIntent,
) : AndroidViewModel(application) {

    @Suppress("unused")
    constructor(application: Application) : this(
        application,
        WatchManager.getInstance(application),
        CustomTabsIntent.Builder().setShowTitle(true).build()
    )

    private val _watchAppVersion = MutableStateFlow<Pair<String?, String?>?>(null)
    val watchAppVersion: Flow<Pair<String?, String?>?>
        get() = _watchAppVersion

    init {
        // Send app version request to selected watches
        viewModelScope.launch {
            watchManager.selectedWatch.collect { watch ->
                if (watch?.id != null) {
                    val result = watchManager.sendMessage(watch, REQUEST_APP_VERSION, null)
                    if (result) {
                        // Successfully sent message
                        _watchAppVersion.emit(Pair(null, null))
                    } else {
                        // Failed to send message
                        _watchAppVersion.emit(null)
                    }
                } else {
                    Timber.w("Selected watch null")
                    _watchAppVersion.emit(null)
                }
            }
        }

        // Listen for app version responses
        viewModelScope.launch {
            watchManager.incomingMessages().filter {
                it.message == REQUEST_APP_VERSION && it.data != null
            }.collect { message ->
                val versionInfo = parseWatchVersionInfo(message.data!!)
                _watchAppVersion.tryEmit(versionInfo)
            }
        }
    }

    /**
     * Parse watch app version info from a given ByteArray.
     * @param byteArray The [ByteArray] received from the connected watch.
     * @return A [Pair] of [String] objects containing the watch version name and version code in
     * first and second respectively.
     */
    private fun parseWatchVersionInfo(byteArray: ByteArray): Pair<String, String> {
        val data = String(byteArray, Charsets.UTF_8).split("|")
        val versionName = data[0]
        val versionCode = data[1]
        return Pair(versionName, versionCode)
    }
}

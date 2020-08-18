/* Copyright (C) 2020 Jack Boswell <boswelja@outlook.com>
 *
 * This file is part of Wearable Extensions
 *
 * This file, and any part of the Wearable Extensions app/s cannot be copied and/or distributed
 * without permission from Jack Boswell (boswelja) <boswela@outlook.com>
 */
package com.boswelja.devicemanager.appinfo.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.boswelja.devicemanager.common.References
import com.google.android.gms.wearable.MessageClient
import timber.log.Timber

class AppInfoViewModel(private val messageClient: MessageClient) : ViewModel() {

    private val messageListener = MessageClient.OnMessageReceivedListener {
        when (it.path) {
            References.REQUEST_APP_VERSION -> {
                Timber.i("Got watch app version")
                val versionInfo = parseWatchVersionInfo(it.data)
                _watchAppVersion.postValue(versionInfo)
            }
        }
    }

    private val _watchAppVersion = MutableLiveData<Pair<String?, String?>?>()
    val watchAppVersion: LiveData<Pair<String?, String?>?>
        get() = _watchAppVersion

    init {
        messageClient.addListener(messageListener)
    }

    override fun onCleared() {
        super.onCleared()
        messageClient.removeListener(messageListener)
    }

    /**
     * Requests the current app version info from the connected watch.
     * Result received in [messageListener] if sending the message was successful.
     */
    fun requestUpdateWatchVersion(connectedWatchId: String) {
        Timber.d("requestUpdateWatchVersionPreference")
        if (connectedWatchId.isNotEmpty()) {
            messageClient.sendMessage(connectedWatchId, References.REQUEST_APP_VERSION, null)
                .addOnFailureListener {
                    Timber.w(it)
                    _watchAppVersion.postValue(null)
                }
                .addOnSuccessListener {
                    Timber.i("Message sent successfully")
                    _watchAppVersion.postValue(Pair(null, null))
                }
        } else {
            Timber.w("connectedWatchId null or empty")
            _watchAppVersion.postValue(null)
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

@Suppress("UNCHECKED_CAST")
class AppInfoViewModelFactory(private val messageClient: MessageClient) :
    ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when (modelClass) {
            AppInfoViewModel::class -> {
                AppInfoViewModel(messageClient) as T
            }
            else -> super.create(modelClass)
        }
    }
}
package com.boswelja.smartwatchextensions.appmanager

import com.boswelja.smartwatchextensions.common.appmanager.Messages.REQUEST_OPEN_PACKAGE
import com.boswelja.smartwatchextensions.common.appmanager.Messages.REQUEST_UNINSTALL_PACKAGE
import com.boswelja.smartwatchextensions.common.appmanager.Messages.VALIDATE_CACHE
import com.boswelja.smartwatchextensions.common.fromByteArray
import com.boswelja.smartwatchextensions.phoneStateStore
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class AppManagerMessageReceiver : WearableListenerService() {

    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            REQUEST_UNINSTALL_PACKAGE -> {
                event.data.toPackageName()?.also { packageName ->
                    requestUninstallPackage(packageName)
                }
            }
            REQUEST_OPEN_PACKAGE -> {
                event.data.toPackageName()?.also { packageName ->
                    openPackage(packageName)
                }
            }
            VALIDATE_CACHE -> {
                val currentPackages = packageManager.getInstalledPackages(0)
                    .map {
                        it.packageName
                    }
                    .hashCode()
                val cacheHash = Int.fromByteArray(event.data)
                if (cacheHash == currentPackages) {
                    Timber.d("Cache appears to be up to date")
                } else {
                    Timber.d(
                        "Received cache hash %s, but %s was expected",
                        cacheHash,
                        currentPackages
                    )
                    // Since WearableListenerService runs on another thread, we can use runBlocking
                    runBlocking {
                        val phoneId = phoneStateStore.data.map { it.id }.first()
                        sendAllApps(phoneId)
                    }
                }
            }
        }
    }
}

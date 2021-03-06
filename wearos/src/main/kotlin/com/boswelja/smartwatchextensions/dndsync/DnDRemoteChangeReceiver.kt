package com.boswelja.smartwatchextensions.dndsync

import android.app.NotificationManager
import android.app.NotificationManager.INTERRUPTION_FILTER_ALL
import android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY
import androidx.core.content.getSystemService
import com.boswelja.smartwatchextensions.common.dndsync.References.DND_STATUS_PATH
import com.boswelja.smartwatchextensions.common.fromByteArray
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import timber.log.Timber

/**
 * A [WearableListenerService] that receives DnD changes from the connected phone.
 */
class DnDRemoteChangeReceiver : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent?) {
        if (messageEvent?.path != DND_STATUS_PATH) return
        val interruptFilterEnabled = Boolean.fromByteArray(messageEvent.data)
        val notificationManager = getSystemService<NotificationManager>()!!
        notificationManager.setDnD(interruptFilterEnabled)
        // TODO Let phone know if there's as an issue
    }

    /**
     * Try to set the system DnD status. This will fail if permission is not granted.
     * @param isEnabled Whether DnD should be enabled.
     * @return true if setting DnD succeeds, false otherwise.
     */
    private fun NotificationManager.setDnD(isEnabled: Boolean): Boolean {
        return if (isNotificationPolicyAccessGranted) {
            val newFilter = if (isEnabled)
                INTERRUPTION_FILTER_PRIORITY
            else
                INTERRUPTION_FILTER_ALL
            setInterruptionFilter(newFilter)
            true
        } else {
            Timber.w("No permission to set DnD state")
            false
        }
    }
}

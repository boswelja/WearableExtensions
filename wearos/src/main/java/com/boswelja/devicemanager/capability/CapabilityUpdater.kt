package com.boswelja.devicemanager.capability

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.boswelja.devicemanager.common.connection.Capability
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable

/**
 * A class for handling adding and removing local capabilities based on what permissions the watch
 * has.
 */
class CapabilityUpdater(
    private val context: Context,
    private val capabilityClient: CapabilityClient
) {
    constructor(context: Context) : this(
        context,
        Wearable.getCapabilityClient(context)
    )

    /**
     * Update all capabilities.
     */
    fun updateCapabilities() {
        updateSendDnD()
        updateReceiveDnD()
        updateSendBattery()
        updateManageApps()
    }

    /**
     * Update [Capability.SEND_DND].
     */
    private fun updateSendDnD() {
        // We can always read DnD state
        capabilityClient.addLocalCapability(Capability.SEND_DND)
    }

    /**
     * Update [Capability.RECEIVE_DND].
     */
    private fun updateReceiveDnD() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O ||
            hasPermission(Manifest.permission.ACCESS_NOTIFICATION_POLICY)
        ) {
            capabilityClient.addLocalCapability(Capability.RECEIVE_DND)
        } else {
            capabilityClient.removeLocalCapability(Capability.RECEIVE_DND)
        }
    }

    /**
     * Update [Capability.SYNC_BATTERY].
     */
    private fun updateSendBattery() {
        // We can always get battery stats
        capabilityClient.addLocalCapability(Capability.SYNC_BATTERY)
    }

    /**
     * Update [Capability.MANAGE_APPS].
     */
    private fun updateManageApps() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R ||
            hasPermission(Manifest.permission.QUERY_ALL_PACKAGES)
        ) {
            capabilityClient.addLocalCapability(Capability.MANAGE_APPS)
        } else {
            capabilityClient.removeLocalCapability(Capability.MANAGE_APPS)
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
}

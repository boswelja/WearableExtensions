package com.boswelja.smartwatchextensions.extensions

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import java.io.InputStream
import java.io.OutputStream

val Context.extensionSettingsStore: DataStore<ExtensionSettings> by dataStore(
    "extensionSettings.pb",
    SettingsSerializer()
)

@Suppress("BlockingMethodInNonBlockingContext")
class SettingsSerializer : Serializer<ExtensionSettings> {
    override val defaultValue = ExtensionSettings(
        phoneLockingEnabled = false,
        batterySyncEnabled = false,
        phoneChargeNotiEnabled = false,
        phoneLowNotiEnabled = false,
        batteryLowThreshold = 15,
        batteryChargeThreshold = 90,
        dndSyncToPhone = false,
        dndSyncWithTheater = false,
        phoneSeparationNotis = false
    )

    override suspend fun readFrom(input: InputStream): ExtensionSettings {
        return ExtensionSettings.ADAPTER.decode(input)
    }

    override suspend fun writeTo(t: ExtensionSettings, output: OutputStream) {
        ExtensionSettings.ADAPTER.encode(output, t)
    }
}

package com.boswelja.smartwatchextensions.common.appmanager

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.graphics.drawable.toBitmap
import com.boswelja.smartwatchextensions.common.SerializableBitmap
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

/**
 * A data class to store information we want to use from [PackageInfo] in a serializable format.
 * @param icon A [SerializableBitmap] representing the package icon, or null if no icon is found.
 * @param version The version of the package. Will default to [PackageInfo.versionName], and fall
 * back to [PackageInfoCompat.getLongVersionCode] if we can't get the version name.
 * @param packageName The [PackageInfo.packageName] of the package.
 * @param label The user-facing name for the package. See [PackageManager.getApplicationLabel].
 * @param isSystemApp A boolean to determine whether the package is a system app.
 * @param hasLaunchActivity A boolean to determine whether the package is launchable.
 * @param isEnabled A boolean to indicate whether the app is enabled.
 * @param installTime The time in milliseconds this package was first installed.
 * @param lastUpdateTime The time in milliseconds this package was last updated.
 * @param requestedPermissions An [Array] of [android.Manifest.permission]s this package requests.
 */
data class App(
    val icon: SerializableBitmap?,
    val version: String,
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean,
    val hasLaunchActivity: Boolean,
    val isEnabled: Boolean,
    val installTime: Long,
    val lastUpdateTime: Long,
    val requestedPermissions: List<String>
) : Serializable {

    constructor(packageManager: PackageManager, packageInfo: PackageInfo) : this(
        SerializableBitmap(packageManager.getApplicationIcon(packageInfo.packageName).toBitmap()),
        packageInfo.version,
        packageInfo.packageName,
        packageManager.getApplicationLabel(packageInfo.applicationInfo).toString(),
        packageInfo.applicationInfo.isSystemApp,
        packageManager.getLaunchIntentForPackage(packageInfo.packageName) != null,
        packageInfo.applicationInfo.enabled,
        packageInfo.firstInstallTime,
        packageInfo.lastUpdateTime,
        packageManager.getLocalizedPermissions(packageInfo)
    )

    override fun toString(): String {
        return label
    }

    override fun equals(other: Any?): Boolean {
        return if (other is App) {
            packageName == other.packageName &&
                label == other.label &&
                version == other.version &&
                isSystemApp == other.isSystemApp &&
                hasLaunchActivity == other.hasLaunchActivity &&
                isEnabled == other.isEnabled &&
                installTime == other.installTime &&
                lastUpdateTime == other.lastUpdateTime
        } else {
            super.equals(other)
        }
    }

    override fun hashCode(): Int {
        var result = version.hashCode()
        result = 31 * result + icon.hashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + isSystemApp.hashCode()
        result = 31 * result + hasLaunchActivity.hashCode()
        result = 31 * result + isEnabled.hashCode()
        result = 31 * result + installTime.hashCode()
        result = 31 * result + lastUpdateTime.hashCode()
        return result
    }

    @Throws(IOException::class)
    fun toByteArray(): ByteArray {
        ByteArrayOutputStream().use {
            ObjectOutputStream(it).use { objectOutputStream ->
                objectOutputStream.writeObject(this)
            }
            return it.toByteArray()
        }
    }

    companion object {
        const val serialVersionUID: Long = 9

        @Throws(IOException::class, ClassNotFoundException::class)
        fun fromByteArray(byteArray: ByteArray): App {
            ObjectInputStream(ByteArrayInputStream(byteArray)).use {
                return it.readObject() as App
            }
        }

        /**
         * Attempts to convert system permissions strings into something meaningful to the user.
         * Fallback to the standard permission string.
         */
        private fun PackageManager.getLocalizedPermissions(
            packageInfo: PackageInfo
        ): List<String> {
            return packageInfo.requestedPermissions?.map { permission ->
                try {
                    val permissionInfo = getPermissionInfo(permission, PackageManager.GET_META_DATA)
                    permissionInfo?.loadLabel(this)?.toString() ?: permission
                } catch (e: Exception) {
                    permission
                }
            }?.sorted() ?: emptyList()
        }

        private val ApplicationInfo.isSystemApp: Boolean
            get() = flags.and(
                ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
            ) != 0

        private val PackageInfo.version: String
            get() = versionName ?: PackageInfoCompat.getLongVersionCode(this).toString()
    }
}

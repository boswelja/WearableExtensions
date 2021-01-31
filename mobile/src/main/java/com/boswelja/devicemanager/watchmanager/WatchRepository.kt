package com.boswelja.devicemanager.watchmanager

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.boswelja.devicemanager.common.References.REQUEST_RESET_APP
import com.boswelja.devicemanager.common.preference.SyncPreferences
import com.boswelja.devicemanager.common.setup.References
import com.boswelja.devicemanager.watchmanager.connection.WatchConnectionInterface
import com.boswelja.devicemanager.watchmanager.connection.WearOSConnectionInterface
import com.boswelja.devicemanager.watchmanager.database.WatchDatabase
import com.boswelja.devicemanager.watchmanager.item.BoolPreference
import com.boswelja.devicemanager.watchmanager.item.IntPreference
import com.boswelja.devicemanager.watchmanager.item.Watch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * A repository to handle passing requests on to the appropriate connection manager, and collect
 * data from the connection managers.
 */
class WatchRepository(
    context: Context,
    private val database: WatchDatabase
) {

    constructor(context: Context) : this(context, WatchDatabase.getInstance(context))

    private val connectionManagers = HashMap<String, WatchConnectionInterface>()
    private val _registeredWatches: MediatorLiveData<List<Watch>> = MediatorLiveData()
    private val _availableWatches: MediatorLiveData<List<Watch>> = MediatorLiveData()

    /**
     * An observable list of watches registered in the database, saturated with statuses.
     */
    val registeredWatches: LiveData<List<Watch>>
        get() = _registeredWatches

    /**
     * A list of all available watches, saturated with statuses.
     */
    val availableWatches: LiveData<List<Watch>>
        get() = _availableWatches

    init {
        Timber.d("Creating new repository")
        // Create Wear OS connection manager
        val wearOS = WearOSConnectionInterface(context)
        connectionManagers[wearOS.getPlatformIdentifier()] = wearOS

        // Set up _availableWatches
        connectionManagers.values.forEach {
            _availableWatches.addSource(it.availableWatches) { newAvailableWatches ->
                val newWatches = replaceForPlatform(
                    _availableWatches.value ?: emptyList(),
                    newAvailableWatches
                )
                _availableWatches.postValue(newWatches)
            }
        }

        // Set up _registeredWatches
        _registeredWatches.addSource(database.watchDao().getAllObservable()) { watches ->
            watches.forEach {
                val connectionManager = it.connectionManager
                if (connectionManager != null) {
                    it.status = connectionManager.getWatchStatus(it, true)
                } else {
                    Timber.w("Platform ${it.platform} not registered")
                }
            }
            _registeredWatches.postValue(watches)
        }
        connectionManagers.values.forEach { connectionManager ->
            _registeredWatches.addSource(connectionManager.dataChanged) {
                if (it) {
                    val watches = updateStatusForPlatform(
                        _registeredWatches.value ?: emptyList(),
                        connectionManager.getPlatformIdentifier()
                    )
                    _registeredWatches.postValue(watches)
                }
            }
        }
    }

    private val Watch.connectionManager: WatchConnectionInterface?
        get() = connectionManagers[this.platform]

    private val Watch.isRegistered: Boolean
        get() = registeredWatches.value?.contains(this) == true

    /**
     * Takes a list of watches from varying platforms and an additional list of watches from a
     * single platform, and replaces all watches in the original list from the same platform with
     * the new list.
     * @param existingWatches The [List] of [Watch]es from varying platforms.
     * @param newWatches The new [List] of [Watch] from a single platform.
     * @return A [List] of [Watch]es with all watches from the platform matching [newWatches]
     * replaced with [newWatches].
     */
    private fun replaceForPlatform(
        existingWatches: List<Watch>,
        newWatches: List<Watch>
    ): List<Watch>? {
        newWatches.firstOrNull()?.platform?.let { platform ->
            val watchesWithoutPlatform = existingWatches.filterNot { it.platform == platform }
            // Since we've removed all watches from the platform, we don't need union.
            return watchesWithoutPlatform + newWatches
        }
        return null
    }

    /**
     * Takes a list of watches from varying platforms and updates the [Watch.Status] of all watches
     * from a specified platform.
     * @param watches The [List] of [Watch] from varying platforms.
     * @param platform The [Watch.platform] to update [Watch.Status] for.
     * @return The [List] of [Watch] with newly added [Watch.Status]
     */
    private fun updateStatusForPlatform(watches: List<Watch>, platform: String): List<Watch> {
        val platformWatches = watches.filter { it.platform == platform }
        val connectionManager = connectionManagers[platform]
        if (connectionManager == null) {
            Timber.w("Platform $platform not registered")
            return watches
        }
        platformWatches.forEach {
            it.status = connectionManager.getWatchStatus(it, it.isRegistered)
        }
        return replaceForPlatform(watches, platformWatches) ?: emptyList()
    }

    /**
     * Register a given watch, and let it know it's been registered.
     * @param watch The [Watch] to register.
     */
    suspend fun registerWatch(watch: Watch) {
        withContext(Dispatchers.IO) {
            database.watchDao().add(watch)
            val connectionManager = watch.connectionManager
            connectionManager?.sendMessage(watch.id, References.WATCH_REGISTERED_PATH)
        }
    }

    /**
     * Removes a given watch from the database, and lets it know it's been removed.
     * @param watch The [Watch] to remove.
     */
    suspend fun forgetWatch(watch: Watch) {
        withContext(Dispatchers.IO) {
            database.watchDao().remove(watch.id)
            resetWatch(watch)
        }
    }

    /**
     * Changes a watches stored name.
     * @param watch The [Watch] to rename.
     * @param newName The new name to set for the watch.
     */
    suspend fun renameWatch(watch: Watch, newName: String) {
        withContext(Dispatchers.IO) {
            database.watchDao().setName(watch.id, newName)
        }
    }

    suspend fun getIntPreferences(watch: Watch): List<IntPreference> {
        return withContext(Dispatchers.IO) {
            return@withContext database.intPrefDao().getAllForWatch(watch.id)
        }
    }

    suspend fun getBoolPreferences(watch: Watch): List<BoolPreference> {
        return withContext(Dispatchers.IO) {
            return@withContext database.boolPrefDao().getAllForWatch(watch.id)
        }
    }

    /**
     * Sends the app request message to a given watch.
     * @param watch The [Watch] to reset Wearable Extensions on.
     */
    fun resetWatch(watch: Watch) {
        watch.connectionManager?.sendMessage(watch.id, REQUEST_RESET_APP)
    }

    /**
     * Notify the watch a specified preference has been changed, and updates the preference in the
     * database.
     * @param watch The target [Watch].
     * @param key The preference key to send to the watch.
     * @param value The new value of the preference.
     */
    suspend fun updatePreference(watch: Watch, key: String, value: Any) {
        withContext(Dispatchers.IO) {
            if (key in SyncPreferences.ALL_PREFS) {
                database.updatePrefInDatabase(watch.id, key, value)
                watch.connectionManager?.updatePreferenceOnWatch(watch, key, value)
            } else {
                Timber.w("Tried to update a non-synced preference")
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T> getPreference(watch: Watch, key: String): T? {
        return withContext(Dispatchers.IO) {
            return@withContext when (key) {
                in SyncPreferences.INT_PREFS -> database.intPrefDao().get(watch.id, key)
                in SyncPreferences.BOOL_PREFS -> database.boolPrefDao().get(watch.id, key)
                else -> null
            }
        } as T?
    }
}

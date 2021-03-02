package com.boswelja.devicemanager.appmanager

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.boswelja.devicemanager.common.DelayedFunction
import com.boswelja.devicemanager.common.Extensions.fromByteArray
import com.boswelja.devicemanager.common.appmanager.App
import com.boswelja.devicemanager.common.appmanager.Messages
import com.boswelja.devicemanager.common.appmanager.Messages.START_SERVICE
import com.boswelja.devicemanager.common.appmanager.Messages.STOP_SERVICE
import com.boswelja.devicemanager.watchmanager.WatchManager
import com.boswelja.devicemanager.watchmanager.item.Watch
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import timber.log.Timber

/**
 * Class for handling the connection with an App Manager service on a connected watch.
 */
class AppManager internal constructor(
    private val messageClient: MessageClient,
    private val watchManager: WatchManager
) {

    constructor(context: Context) : this(
        Wearable.getMessageClient(context),
        WatchManager.getInstance(context)
    )

    private val stateDisconnectedDelay = DelayedFunction(15) {
        _state.postValue(State.DISCONNECTED)
    }

    private val _state = MutableLiveData(State.CONNECTING)
    private val _appsObservable = MutableLiveData<List<App>>(emptyList())
    private val _progress = MutableLiveData(-1)

    private val _apps = ArrayList<App>()

    private var watchId: String? = null
    private var expectedPackageCount: Int = 0

    /**
     * The current [State] of the App Manager.
     */
    val state: LiveData<State>
        get() = _state

    /**
     * A [List] of all [App]s found on the selected watch.
     */
    val apps: LiveData<List<App>>
        get() = _appsObservable

    /**
     * The progress of any ongoing operations, or -1 if progress can't be reported.
     */
    val progress: LiveData<Int>
        get() = _progress

    private val messageListener = MessageClient.OnMessageReceivedListener {
        if (it.sourceNodeId == watchId) {
            try {
                when (it.path) {
                    // Package change messages
                    Messages.PACKAGE_ADDED -> addPackage(App.fromByteArray(it.data))
                    Messages.PACKAGE_UPDATED -> updatePackage(App.fromByteArray(it.data))
                    Messages.PACKAGE_REMOVED -> removePackage(App.fromByteArray(it.data))

                    // Service state messages
                    Messages.SERVICE_RUNNING -> serviceRunning()
                    Messages.EXPECTED_APP_COUNT -> expectPackages(Int.fromByteArray(it.data))

                    else -> Timber.w("Unknown path received, ignoring")
                }
            } catch (e: Exception) {
                Timber.e(e)
                _state.postValue(State.ERROR)
            }
        } else if (it.path == Messages.SERVICE_RUNNING) {
            // If we get SERVICE_RUNNING from any watch that's not the selected watch, stop it
            Timber.w("Non-selected watch still has App Manager running, stopping...")
            messageClient.sendMessage(it.sourceNodeId, STOP_SERVICE, null)
        }
    }

    private val selectedWatchObserver = Observer<Watch?> {
        if (it?.id != null && it.id != watchId) {
            Timber.d("selectedWatch changed, reconnecting App Manager service")
            _state.postValue(State.CONNECTING)
            _progress.postValue(-1)
            watchId?.let { watchId ->
                messageClient.sendMessage(watchId, STOP_SERVICE, null)
            }
            watchId = it.id
            startAppManagerService()
        }
    }

    init {
        messageClient.addListener(messageListener)
        watchManager.selectedWatch.observeForever(selectedWatchObserver)
    }

    internal fun expectPackages(count: Int) {
        _state.postValue(State.LOADING_APPS)
        _progress.postValue(0)
        expectedPackageCount = count
    }

    internal fun addPackage(app: App) {
        Timber.d("Adding app to list")
        _apps.add(app)
        if (_apps.count() >= expectedPackageCount) {
            Timber.d("Got all expected packages, updating LiveData")
            _progress.postValue(-1)
            _appsObservable.postValue(_apps)
        } else {
            val progress = ((_apps.count() / expectedPackageCount.toFloat()) * 100).toInt()
            Timber.d("Progress $progress")
            _progress.postValue(progress)
        }
    }

    internal fun updatePackage(app: App) {
        Timber.d("Updating app in list")
        _apps.removeAll { it.packageName == app.packageName }
        _apps.add(app)
        _appsObservable.postValue(_apps)
    }

    internal fun removePackage(app: App) {
        Timber.d("Removing app from list")
        _apps.removeAll { it.packageName == app.packageName }
        _appsObservable.postValue(_apps)
    }

    internal fun serviceRunning() {
        Timber.d("App Manager service is running")
        stateDisconnectedDelay.reset()
        if (_apps.count() >= expectedPackageCount) {
            Timber.d("No more expected packages, setting State to READY")
            _state.postValue(State.READY)
        }
    }

    /** Start the App Manager service on the connected watch. */
    fun startAppManagerService() {
        Timber.d("startAppManagerService() called")
        watchId?.let {
            _state.postValue(State.CONNECTING)
            _apps.clear()
            _appsObservable.postValue(_apps)
            messageClient.sendMessage(it, START_SERVICE, null)
            stateDisconnectedDelay.reset()
        }
    }

    /** Stop the App Manager service on the connected watch. */
    fun stopAppManagerService() {
        Timber.d("stopAppManagerService() called")
        watchId?.let {
            messageClient.sendMessage(it, STOP_SERVICE, null)
        }
    }

    fun destroy() {
        messageClient.removeListener(messageListener)
        watchManager.selectedWatch.removeObserver(selectedWatchObserver)
        stopAppManagerService()
    }
}
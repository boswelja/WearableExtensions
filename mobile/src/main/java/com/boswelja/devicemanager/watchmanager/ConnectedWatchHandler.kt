package com.boswelja.devicemanager.watchmanager

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.boswelja.devicemanager.watchmanager.database.WatchDatabase
import com.boswelja.devicemanager.watchmanager.item.Watch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConnectedWatchHandler private constructor(context: Context) {

  private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
  private val coroutineScope = CoroutineScope(Dispatchers.IO)

  val connectedWatch = MutableLiveData<Watch?>()
  val database = WatchDatabase.get(context)

  init {
    sharedPreferences.getString(LAST_CONNECTED_NODE_ID_KEY, "")?.let { setConnectedWatchById(it) }
  }

  /**
   * Sets the currently connected watch by a given [Watch.id].
   * @param watchId The ID of the [Watch] to set as connected.
   */
  fun setConnectedWatchById(watchId: String) {
    coroutineScope.launch {
      val newWatch = database.watchDao().get(watchId)
      connectedWatch.postValue(newWatch)
      sharedPreferences.edit { putString(LAST_CONNECTED_NODE_ID_KEY, newWatch?.id) }
    }
  }

  companion object {
    const val LAST_CONNECTED_NODE_ID_KEY = "last_connected_id"

    private var INSTANCE: ConnectedWatchHandler? = null
    fun get(context: Context): ConnectedWatchHandler {
      if (INSTANCE != null) return INSTANCE!!
      synchronized(this) {
        if (INSTANCE == null) {
          INSTANCE = ConnectedWatchHandler(context)
        }
        return INSTANCE!!
      }
    }
  }
}
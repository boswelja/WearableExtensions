/* Copyright (C) 2020 Jack Boswell <boswelja@outlook.com>
 *
 * This file is part of Wearable Extensions
 *
 * This file, and any part of the Wearable Extensions app/s cannot be copied and/or distributed
 * without permission from Jack Boswell (boswelja) <boswela@outlook.com>
 */
package com.boswelja.devicemanager.ui.watchmanager

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boswelja.devicemanager.R
import com.boswelja.devicemanager.ui.base.BaseToolbarActivity
import com.boswelja.devicemanager.ui.watchsetup.WatchSetupActivity
import com.boswelja.devicemanager.ui.watchsetup.WatchSetupActivity.Companion.EXTRA_SKIP_WELCOME
import com.boswelja.devicemanager.watchmanager.Watch
import com.boswelja.devicemanager.watchmanager.WatchConnectionListener
import com.boswelja.devicemanager.watchmanager.WatchManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class WatchManagerActivity :
        BaseToolbarActivity(),
        WatchConnectionListener {

    private val watchConnectionManagerConnection = object : WatchManager.Connection() {
        override fun onWatchManagerBound(watchManager: WatchManager) {
            Timber.i("Watch manager bound")
            watchConnectionManager = watchManager
            setWatches()
        }

        override fun onWatchManagerUnbound() {
            Timber.w("Watch manager unbound")
            watchConnectionManager = null
        }
    }

    private val coroutineScope = MainScope()

    private var watchConnectionManager: WatchManager? = null

    private lateinit var watchManagerRecyclerView: RecyclerView

    override fun onConnectedWatchChanged(isSuccess: Boolean) {} // Do nothing

    override fun onConnectedWatchChanging() {} // Do nothing

    override fun onWatchAdded(watch: Watch) {
        (watchManagerRecyclerView.adapter as WatchManagerAdapter).addWatch(watch)
    }

    override fun getContentViewId(): Int = R.layout.activity_watch_manager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_WATCH_LIST_UNCHANGED)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        setupRecyclerView()

        WatchManager.bind(this, watchConnectionManagerConnection)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(watchConnectionManagerConnection)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            WATCH_SETUP_ACTIVITY_REQUEST_CODE -> {
                Timber.i("Got WatchSetupActivity result")
                when (resultCode) {
                    WatchSetupActivity.RESULT_WATCH_ADDED -> {
                        setResult(RESULT_WATCH_LIST_CHANGED)
                        setWatches()
                    }
                }
            }
            WATCH_INFO_ACTIVITY_REQUEST_CODE -> {
                Timber.i("Got WatchInfoActivity result")
                when (resultCode) {
                    WatchInfoActivity.RESULT_WATCH_NAME_CHANGED -> {
                        setResult(RESULT_WATCH_LIST_CHANGED)
                        val watchId = data?.getStringExtra(WatchInfoActivity.EXTRA_WATCH_ID)
                        updateWatch(watchId)
                    }
                    WatchInfoActivity.RESULT_WATCH_REMOVED -> {
                        setResult(RESULT_WATCH_LIST_CHANGED)
                        val watchId = data?.getStringExtra(WatchInfoActivity.EXTRA_WATCH_ID)
                        removeWatch(watchId)
                    }
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    /**
     * Set up [watchManagerRecyclerView].
     */
    private fun setupRecyclerView() {
        watchManagerRecyclerView = findViewById(R.id.watch_manager_recycler_view)
        watchManagerRecyclerView.apply {
            adapter = WatchManagerAdapter(this@WatchManagerActivity)
            layoutManager = LinearLayoutManager(
                    this@WatchManagerActivity,
                    LinearLayoutManager.VERTICAL,
                    false)
        }
    }

    /**
     * Sets the watches in the [WatchManagerAdapter] to the
     * list of registered watches from [WatchManager].
     */
    private fun setWatches() {
        Timber.d("updateRegisteredWatches() called")
        coroutineScope.launch(Dispatchers.IO) {
            if (watchConnectionManager != null) {
                val registeredWatches = watchConnectionManager!!.getRegisteredWatches()
                withContext(Dispatchers.Main) {
                    (watchManagerRecyclerView.adapter as WatchManagerAdapter)
                            .setWatches(registeredWatches)
                }
            }
        }
    }

    /**
     * Update a watch in [WatchManagerAdapter] by it's ID.
     * @param watchId The ID of the watch to update.
     */
    private fun updateWatch(watchId: String?) {
        Timber.d("updateWatch($watchId) called")
        coroutineScope.launch(Dispatchers.IO) {
            val newWatchInfo = watchConnectionManager?.getWatchById(watchId)
            if (newWatchInfo != null) {
                Timber.i("Updating watch info for $watchId")
                withContext(Dispatchers.Main) {
                    (watchManagerRecyclerView.adapter as WatchManagerAdapter)
                            .updateWatch(newWatchInfo)
                }
            } else {
                Timber.w("newWatchInfo null")
            }
        }
    }

    /**
     * Removes a watch from the [WatchManagerAdapter].
     * @param watchId The ID of the watch to remove.
     */
    private fun removeWatch(watchId: String?) {
        Timber.d("removeWatch($watchId) called")
        if (!watchId.isNullOrEmpty()) {
            (watchManagerRecyclerView.adapter as WatchManagerAdapter).removeWatch(watchId)
        }
    }

    /**
     * Opens a [WatchSetupActivity].
     */
    fun openWatchSetupActivity() {
        Intent(this, WatchSetupActivity::class.java).apply {
            putExtra(EXTRA_SKIP_WELCOME, true)
        }.also {
            startActivityForResult(it, WATCH_SETUP_ACTIVITY_REQUEST_CODE)
        }
    }

    /**
     * Opens a [WatchInfoActivity].
     */
    fun openWatchInfoActivity(watch: Watch) {
        Intent(this, WatchInfoActivity::class.java).apply {
            putExtra(WatchInfoActivity.EXTRA_WATCH_ID, watch.id)
        }.also {
            startActivityForResult(it, WATCH_INFO_ACTIVITY_REQUEST_CODE)
        }
    }

    companion object {
        private const val WATCH_SETUP_ACTIVITY_REQUEST_CODE = 54321
        private const val WATCH_INFO_ACTIVITY_REQUEST_CODE = 65432

        const val RESULT_WATCH_LIST_CHANGED = 1
        const val RESULT_WATCH_LIST_UNCHANGED = 0
    }
}

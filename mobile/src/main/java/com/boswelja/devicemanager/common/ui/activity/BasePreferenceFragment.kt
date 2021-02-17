/* Copyright (C) 2020 Jack Boswell <boswelja@outlook.com>
 *
 * This file is part of Wearable Extensions
 *
 * This file, and any part of the Wearable Extensions app/s cannot be copied and/or distributed
 * without permission from Jack Boswell (boswelja) <boswela@outlook.com>
 */
package com.boswelja.devicemanager.common.ui.activity

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.boswelja.devicemanager.R
import com.boswelja.devicemanager.watchmanager.WatchManager
import com.boswelja.devicemanager.watchmanager.item.Watch

/**
 * A [PreferenceFragmentCompat] that automatically adjusts it's [RecyclerView] properties to fit the
 * app theme better, and also automatically initializes a [SharedPreferences] instance.
 */
abstract class BasePreferenceFragment : PreferenceFragmentCompat() {

    protected val sharedPreferences: SharedPreferences
        get() = preferenceManager.sharedPreferences

    protected lateinit var watchManager: WatchManager

    protected var connectedWatch: Watch? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        watchManager = WatchManager.getInstance(requireContext())
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater?,
        parent: ViewGroup?,
        savedInstanceState: Bundle?
    ): RecyclerView {
        val padding = resources.getDimension(R.dimen.recyclerview_vertical_padding)
        return super.onCreateRecyclerView(inflater, parent, savedInstanceState).apply {
            clipToPadding = false
            setPadding(0, padding.toInt(), 0, padding.toInt())
        }
    }

    fun observeConnectedWatchId() {
        watchManager.selectedWatch.observe(viewLifecycleOwner) { newWatch ->
            newWatch?.let { connectedWatch = it }
        }
    }
}
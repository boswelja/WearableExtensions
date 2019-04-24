package com.boswelja.devicemanager.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.boswelja.devicemanager.Utils

abstract class BasePreferenceFragment : PreferenceFragmentCompat() {

    override fun onCreateRecyclerView(inflater: LayoutInflater?, parent: ViewGroup?, savedInstanceState: Bundle?): RecyclerView {
        val recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        val padding = Utils.complexTypeDp(resources, 8f)
        recyclerView.clipToPadding = false
        recyclerView.setPadding(0, padding.toInt(), 0, padding.toInt())
        return recyclerView
    }
}
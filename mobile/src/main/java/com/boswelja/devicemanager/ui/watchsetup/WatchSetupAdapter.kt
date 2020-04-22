/* Copyright (C) 2020 Jack Boswell <boswelja@outlook.com>
 *
 * This file is part of Wearable Extensions
 *
 * This file, and any part of the Wearable Extensions app/s cannot be copied and/or distributed
 * without permission from Jack Boswell (boswelja) <boswela@outlook.com>
 */
package com.boswelja.devicemanager.ui.watchsetup

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.boswelja.devicemanager.R
import com.boswelja.devicemanager.ui.common.WatchViewHolder
import com.boswelja.devicemanager.watchconnectionmanager.Watch
import com.boswelja.devicemanager.watchconnectionmanager.WatchStatus

class WatchSetupAdapter(private val watchSetupFragment: WatchSetupFragment) :
        RecyclerView.Adapter<WatchViewHolder>() {

    private val watches = ArrayList<Watch>()

    override fun getItemCount(): Int = watches.count()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.common_recyclerview_item_icon_two_line, parent, false)
        return WatchViewHolder(view)
    }

    override fun onBindViewHolder(holder: WatchViewHolder, position: Int) {
        val watch = watches[position]
        val context = holder.itemView.context
        holder.apply {
            icon.setImageResource(R.drawable.ic_watch)
            topLine.text = watch.name
            bottomLine.text = context.getString(when (watch.status) {
                WatchStatus.NOT_REGISTERED -> R.string.watch_status_not_registered
                WatchStatus.MISSING_APP -> R.string.watch_status_missing_app
                else -> R.string.watch_status_error
            })
            itemView.setOnClickListener {
                watchSetupFragment.confirmRegisterWatch(watch)
            }
        }
    }

    /**
     * Set the [List] of [Watch] objects to show.
     * @param newWatches The new [Watch] objects to show.
     */
    fun setWatches(newWatches: List<Watch>) {
        watches.apply {
            clear()
            addAll(newWatches)
        }
        notifyDataSetChanged()
    }
}

/* Copyright (C) 2020 Jack Boswell <boswelja@outlook.com>
 *
 * This file is part of Wearable Extensions
 *
 * This file, and any part of the Wearable Extensions app/s cannot be copied and/or distributed
 * without permission from Jack Boswell (boswelja) <boswela@outlook.com>
 */
package com.boswelja.devicemanager.watchmanager.ui

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.boswelja.devicemanager.R
import com.boswelja.devicemanager.common.WatchDiffCallback
import com.boswelja.devicemanager.common.recyclerview.item.IconOneLineViewHolder
import com.boswelja.devicemanager.common.recyclerview.item.IconTwoLineViewHolder
import com.boswelja.devicemanager.common.recyclerview.item.SectionHeaderViewHolder
import com.boswelja.devicemanager.watchmanager.WatchStatus
import com.boswelja.devicemanager.watchmanager.item.Watch

class WatchManagerAdapter(private val clickListener: (watch: Watch?) -> Unit) :
    ListAdapter<Watch, RecyclerView.ViewHolder>(WatchDiffCallback()) {

    override fun getItemCount(): Int = super.getItemCount() + 2

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> VIEW_TYPE_ADD_WATCH
            1 -> VIEW_TYPE_SECTION_HEADER
            else -> VIEW_TYPE_WATCH
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ADD_WATCH -> IconOneLineViewHolder.from(parent)
            VIEW_TYPE_SECTION_HEADER -> SectionHeaderViewHolder.from(parent)
            else -> IconTwoLineViewHolder.from(parent)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val context = holder.itemView.context
        when (holder) {
            is IconOneLineViewHolder -> {
                holder.bind(R.drawable.ic_add, context.getString(R.string.watch_manager_add_watch_title))
                holder.itemView.setOnClickListener {
                    clickListener(null)
                }
            }
            is SectionHeaderViewHolder -> {
                holder.bind(context.getString(R.string.watch_manager_registered_watch_header))
            }
            is IconTwoLineViewHolder -> {
                val watch = getWatch(position)
                val summary = context.getString(
                    when (watch.status) {
                        WatchStatus.CONNECTED -> R.string.watch_status_connected
                        WatchStatus.DISCONNECTED -> R.string.watch_status_disconnected
                        WatchStatus.MISSING_APP -> R.string.watch_status_missing_app
                        else -> R.string.watch_status_error
                    }
                )
                holder.apply {
                    bind(R.drawable.ic_watch, watch.name, summary)
                    itemView.setOnClickListener {
                        clickListener(watch)
                    }
                }
            }
        }
    }

    private fun getWatch(position: Int) = currentList[position - 2]

    companion object {
        private const val VIEW_TYPE_ADD_WATCH = -1
        private const val VIEW_TYPE_SECTION_HEADER = 0
        private const val VIEW_TYPE_WATCH = 1
    }
}
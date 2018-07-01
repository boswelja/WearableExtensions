/* Copyright (C) 2018 Jack Boswell <boswelja@outlook.com>
 *
 * This file is part of Wearable Extensions
 *
 * This file, and any part of the Wearable Extensions app/s cannot be copied and/or distributed
 * without permission from Jack Boswell (boswelja) <boswela@outlook.com>
 */
package com.boswelja.devicemanager.ui

import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.boswelja.devicemanager.common.References
import com.boswelja.devicemanager.MainOption
import com.boswelja.devicemanager.R
import com.boswelja.devicemanager.complications.ActionService

class MainAdapter(private val options: ArrayList<MainOption>) : RecyclerView.Adapter<MainAdapter.ViewHolder>() {

    override fun getItemCount(): Int {
        return options.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = options[position]
        when (option.type) {
            References.TYPE_LOCK_PHONE -> {
                holder.label.text = option.label
                holder.icon.setImageResource(option.iconRes)
                holder.itemView.setOnClickListener {
                    val intent = Intent(holder.itemView.context, ActionService::class.java)
                    intent.putExtra(References.INTENT_ACTION_EXTRA, References.LOCK_PHONE_PATH)
                    holder.itemView.context.startService(intent)
                }
            }
            References.TYPE_PHONE_BATTERY -> {
                val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(holder.itemView.context)
                sharedPrefs.registerOnSharedPreferenceChangeListener { _, key ->
                    if (key == References.BATTERY_PERCENT_KEY) {
                        updateBatteryPercent(holder, sharedPrefs)
                    }
                }
                updateBatteryPercent(holder, sharedPrefs)

            }
        }
    }

    private fun updateBatteryPercent(holder: ViewHolder, sharedPrefs: SharedPreferences) {
        val phoneBattery = sharedPrefs.getInt(References.BATTERY_PERCENT_KEY, -1)
        if (phoneBattery > -1) {
            holder.label.text = "Phone at $phoneBattery%"
        } else {
            holder.label.text = "Phone battery info not available"
        }
        when (phoneBattery) {
            -1 -> holder.icon.setImageResource(R.drawable.ic_battery_unknown)
            in 1..24 -> holder.icon.setImageResource(R.drawable.ic_battery_20)
            in 25..44 -> holder.icon.setImageResource(R.drawable.ic_battery_30)
            in 45..54 -> holder.icon.setImageResource(R.drawable.ic_battery_50)
            in 55..64 -> holder.icon.setImageResource(R.drawable.ic_battery_60)
            in 65..84 -> holder.icon.setImageResource(R.drawable.ic_battery_80)
            in 85..94 -> holder.icon.setImageResource(R.drawable.ic_battery_90)
            in 95..100 -> holder.icon.setImageResource(R.drawable.ic_battery_full)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.model_main_option, parent, false)
        return ViewHolder(view)
    }

    class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.icon)
        val label: TextView = itemView.findViewById(R.id.label)
    }
}

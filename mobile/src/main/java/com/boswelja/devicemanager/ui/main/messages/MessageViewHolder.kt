/* Copyright (C) 2020 Jack Boswell <boswelja@outlook.com>
 *
 * This file is part of Wearable Extensions
 *
 * This file, and any part of the Wearable Extensions app/s cannot be copied and/or distributed
 * without permission from Jack Boswell (boswelja) <boswela@outlook.com>
 */
package com.boswelja.devicemanager.ui.main.messages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.boswelja.devicemanager.databinding.MessageItemBinding
import com.boswelja.devicemanager.messages.Message

class MessageViewHolder private constructor(val binding: MessageItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(message: Message) {
        binding.apply {
            messageIcon.setImageResource(message.iconRes)
            messageLabel.text = message.label
            messageDesc.text = message.desc
            if (message.hasAction) {
                messageActionButton.text = message.buttonLabel
                messageActionButton.visibility = View.VISIBLE
                divider.visibility = View.VISIBLE
            }
        }
    }

    companion object {
        fun from(parent: ViewGroup): MessageViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = MessageItemBinding.inflate(layoutInflater, parent, false)
            return MessageViewHolder(binding)
        }
    }
}
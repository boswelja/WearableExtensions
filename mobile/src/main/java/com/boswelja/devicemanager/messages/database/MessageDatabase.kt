/* Copyright (C) 2019 Jack Boswell <boswelja@outlook.com>
 *
 * This file is part of Wearable Extensions
 *
 * This file, and any part of the Wearable Extensions app/s cannot be copied and/or distributed
 * without permission from Jack Boswell (boswelja) <boswela@outlook.com>
 */
package com.boswelja.devicemanager.messages.database

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.boswelja.devicemanager.messages.Message

@Database(entities = [Message::class], version = 4)
abstract class MessageDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao

    fun countMessages(): Int {
        return messageDao().getActiveMessages().size
    }

    fun updateMessageCount(sharedPreferences: SharedPreferences) {
        val trueCount = countMessages()
        sharedPreferences.edit {
            putInt(MESSAGE_COUNT_KEY, trueCount)
        }
    }

    fun getActiveMessages(): List<Message> {
        return messageDao().getActiveMessages().sortedBy { it.timestamp }
    }

    fun getDeletedMessages(): List<Message> {
        return messageDao().getDeletedMessages().sortedBy { it.timestamp }
    }

    fun messageExists(messageId: Int): Boolean {
        return messageDao().getMessage(messageId) != null
    }

    fun sendMessage(sharedPreferences: SharedPreferences, message: Message): Boolean {
        val messageSent = sendMessage(message)
        sharedPreferences.edit {
            putInt(MESSAGE_COUNT_KEY, sharedPreferences.getInt(MESSAGE_COUNT_KEY, 0) + 1)
        }
        return messageSent
    }

    private fun sendMessage(message: Message): Boolean {
        if (isOpen) {
            messageDao().sendMessage(message)
            return true
        }
        return false
    }

    fun deleteMessage(sharedPreferences: SharedPreferences, message: Message): Boolean {
        val messageDeleted = deleteMessage(message)
        sharedPreferences.edit {
            putInt(MESSAGE_COUNT_KEY, sharedPreferences.getInt(MESSAGE_COUNT_KEY, 1) - 1)
        }
        return messageDeleted
    }

    private fun deleteMessage(message: Message): Boolean {
        if (isOpen) {
            messageDao().deleteMessage(message.id)
            return true
        }
        return false
    }

    companion object {

        const val MESSAGE_COUNT_KEY = "message_count"

        fun open(context: Context): MessageDatabase {
            return Room.databaseBuilder(context, MessageDatabase::class.java, "messages-db")
                    .build()
        }
    }
}

/* Copyright (C) 2020 Jack Boswell <boswelja@outlook.com>
 *
 * This file is part of Wearable Extensions
 *
 * This file, and any part of the Wearable Extensions app/s cannot be copied and/or distributed
 * without permission from Jack Boswell (boswelja) <boswela@outlook.com>
 */
package com.boswelja.devicemanager.main

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.boswelja.devicemanager.messages.database.MessageDatabase.Companion.MESSAGE_COUNT_KEY

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { preferences, key ->
        when (key) {
            MESSAGE_COUNT_KEY -> _messageCount.postValue(preferences.getInt(key, 0))
        }
    }

    private val _messageCount = MutableLiveData(sharedPreferences.getInt(MESSAGE_COUNT_KEY, 0))
    val messageCount: LiveData<Int>
        get() = _messageCount

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onCleared() {
        super.onCleared()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }
}
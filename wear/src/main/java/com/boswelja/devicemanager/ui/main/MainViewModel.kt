package com.boswelja.devicemanager.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.boswelja.devicemanager.phoneconnectionmanager.References.PHONE_ID_KEY

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

    private val _isRegistered = MutableLiveData<Boolean?>(null)
    val isRegistered: LiveData<Boolean?>
        get() = _isRegistered

    init {
        _isRegistered.postValue(!sharedPreferences.getString(PHONE_ID_KEY, "").isNullOrBlank())
    }
}
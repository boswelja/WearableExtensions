/* Copyright (C) 2020 Jack Boswell <boswelja@outlook.com>
 *
 * This file is part of Wearable Extensions
 *
 * This file, and any part of the Wearable Extensions app/s cannot be copied and/or distributed
 * without permission from Jack Boswell (boswelja) <boswela@outlook.com>
 */
package com.boswelja.devicemanager.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.boswelja.devicemanager.BuildConfig
import com.boswelja.devicemanager.R
import com.boswelja.devicemanager.common.References.CAPABILITY_PHONE_APP
import com.boswelja.devicemanager.common.setup.References.CHECK_WATCH_REGISTERED_PATH
import com.boswelja.devicemanager.common.setup.References.WATCH_NOT_REGISTERED_PATH
import com.boswelja.devicemanager.common.setup.References.WATCH_REGISTERED_PATH
import com.boswelja.devicemanager.phoneconnectionmanager.References.PHONE_ID_KEY
import com.boswelja.devicemanager.ui.common.LoadingFragment
import com.boswelja.devicemanager.ui.common.NoConnectionFragment
import com.boswelja.devicemanager.ui.main.MainFragment
import com.boswelja.devicemanager.ui.setup.SetupFragment
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity :
        AppCompatActivity(),
    MessageClient.OnMessageReceivedListener {

    private val coroutineScope = MainScope()

    private var currentFragment: Fragment? = null

    private lateinit var sharedPreferences: SharedPreferences

    private var shouldAnimateFragmentChanges: Boolean = false

    private lateinit var capabilityClient: CapabilityClient
    private lateinit var messageClient: MessageClient

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            WATCH_REGISTERED_PATH -> {
                showExtensionsFragment()
            }
            WATCH_NOT_REGISTERED_PATH -> {
                showSetupFragment(true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        showLoadingFragment()

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        capabilityClient = Wearable.getCapabilityClient(this)
        messageClient = Wearable.getMessageClient(this)

        coroutineScope.launch(Dispatchers.IO) {
            val phoneNodeId = sharedPreferences.getString(PHONE_ID_KEY, "")
            val phoneNode = if (!phoneNodeId.isNullOrEmpty()) {
                Tasks.await(Wearable.getNodeClient(this@MainActivity).connectedNodes).firstOrNull { it.id == phoneNodeId }
            } else {
                Tasks.await(Wearable.getNodeClient(this@MainActivity).connectedNodes).firstOrNull().also {
                    if (it != null) {
                        sharedPreferences.edit {
                            putString(PHONE_ID_KEY, it.id)
                        }
                    }
                }
            }

            if (!BuildConfig.DEBUG) {
                if (phoneNode != null) {
                    val isCapable = Tasks.await(capabilityClient.getCapability(CAPABILITY_PHONE_APP, CapabilityClient.FILTER_ALL)).nodes.any { it.id == phoneNode.id }
                    if (isCapable) {
                        messageClient.sendMessage(phoneNode.id, CHECK_WATCH_REGISTERED_PATH, null)
                                .addOnFailureListener {
                                    showNoConnectionFragment()
                                }
                    } else {
                        showSetupFragment(false)
                    }
                } else {
                    showNoConnectionFragment()
                }
            } else {
                showExtensionsFragment()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        messageClient.addListener(this)
    }

    override fun onPause() {
        super.onPause()
        messageClient.removeListener(this)
    }

    private fun showLoadingFragment() {
        if (currentFragment !is LoadingFragment) {
            showFragment(LoadingFragment())
        }
    }

    private fun showNoConnectionFragment() {
        if (currentFragment !is NoConnectionFragment) {
            showFragment(NoConnectionFragment())
        }
    }

    private fun showExtensionsFragment() {
        if (currentFragment !is MainFragment) {
            showFragment(MainFragment())
        }
    }

    private fun showSetupFragment(phoneHasApp: Boolean) {
        if (currentFragment !is SetupFragment) {
            val setupFragment = SetupFragment().apply {
                setPhoneSetupHelperVisibility(phoneHasApp)
            }
            showFragment(setupFragment)
        }
    }

    private fun showFragment(fragment: Fragment) {
        try {
            supportFragmentManager.beginTransaction().apply {
                if (shouldAnimateFragmentChanges) {
                    setCustomAnimations(R.anim.slide_in_right, R.anim.fade_out)
                } else {
                    shouldAnimateFragmentChanges = true
                }
                replace(R.id.content, fragment)
            }.also {
                it.commit()
            }
        } catch (e: IllegalStateException) {
            Log.e("MainActivity", "Tried to commit a FragmentTransaction after onSaveInstanceState")
        }
    }
}

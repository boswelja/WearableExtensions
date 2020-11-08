/* Copyright (C) 2020 Jack Boswell <boswelja@outlook.com>
 *
 * This file is part of Wearable Extensions
 *
 * This file, and any part of the Wearable Extensions app/s cannot be copied and/or distributed
 * without permission from Jack Boswell (boswelja) <boswela@outlook.com>
 */
package com.boswelja.devicemanager.watchmanager

import android.content.SharedPreferences
import android.os.Build
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.boswelja.devicemanager.watchmanager.database.WatchDatabase
import com.boswelja.devicemanager.watchmanager.item.Watch
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.common.truth.Truth.assertThat
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.impl.annotations.SpyK
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class WatchManagerTest {

    private val coroutineScope = TestCoroutineScope()
    private val dummyWatch = Watch("an-id-1234", "Watch 1", null)
    private val dummyWatchNode =
        object : Node {
            override fun getDisplayName(): String = dummyWatch.name
            override fun getId(): String = dummyWatch.id
            override fun isNearby(): Boolean = true
        }

    @RelaxedMockK lateinit var watchPreferenceManager: WatchPreferenceManager
    @RelaxedMockK lateinit var selectedWatchHandler: SelectedWatchHandler
    @RelaxedMockK lateinit var capabilityClient: CapabilityClient
    @RelaxedMockK lateinit var nodeClient: NodeClient
    @RelaxedMockK lateinit var messageClient: MessageClient
    @RelaxedMockK lateinit var database: WatchDatabase

    private lateinit var watchManager: WatchManager

    @SpyK
    var sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext())

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { database.isOpen } returns true

        watchManager =
            spyk(
                WatchManager(
                    ApplicationProvider.getApplicationContext(),
                    watchPreferenceManager,
                    selectedWatchHandler,
                    capabilityClient,
                    nodeClient,
                    messageClient,
                    database
                )
            )
    }

    @After
    fun tearDown() {
        coroutineScope.cleanupTestCoroutines()
        sharedPreferences.edit().clear().commit()
    }

    @Test
    fun `getAvailableWatches returns an empty list when all watches are registered`() {
        coEvery { watchManager.getRegisteredWatches() } returns listOf(dummyWatch)
        coEvery { Utils.getConnectedNodes(nodeClient) } returns listOf(dummyWatchNode)
        coEvery { Utils.getCapableNodes(capabilityClient) } returns setOf(dummyWatchNode)

        val result = runBlocking { watchManager.getAvailableWatches() }
        assertThat(result).isEmpty()
    }

    @Test
    fun `getAvailableWatches returns null when getConnectedWatches fails`() {
        coEvery { Utils.getConnectedNodes(nodeClient) } returns null

        val result = runBlocking { watchManager.getAvailableWatches() }
        assertThat(result).isNull()
    }

    @Test
    fun `getAvailableWatches returns all unregistered watches`() {
        coEvery { watchManager.getRegisteredWatches() } returns listOf()
        coEvery { Utils.getConnectedNodes(nodeClient) } returns listOf(dummyWatchNode)
        coEvery { Utils.getCapableNodes(capabilityClient) } returns setOf(dummyWatchNode)

        val result = runBlocking { watchManager.getAvailableWatches() }
        assertThat(result).isNotNull()
        assertThat(result!!.any { it.id == dummyWatch.id }).isTrue()
    }
}

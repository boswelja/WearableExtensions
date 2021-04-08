package com.boswelja.devicemanager.watchmanager

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.boswelja.devicemanager.AppState
import com.boswelja.devicemanager.analytics.Analytics
import com.boswelja.devicemanager.batterysync.database.WatchBatteryStatsDatabase
import com.boswelja.devicemanager.common.connection.Messages.CLEAR_PREFERENCES
import com.boswelja.devicemanager.common.connection.Messages.REQUEST_UPDATE_CAPABILITIES
import com.boswelja.devicemanager.getOrAwaitValue
import com.boswelja.devicemanager.watchmanager.database.WatchSettingsDatabase
import com.boswelja.devicemanager.watchmanager.item.Watch
import com.google.common.truth.Truth.assertThat
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.R])
@ExperimentalCoroutinesApi
class WatchManagerTest {

    @get:Rule val taskExecutorRule = InstantTaskExecutorRule()

    private val platformIdentifier = "dummy"

    private val dummyWatch1 = Watch("an-id-1234", "Watch 1", platformIdentifier)
    private val dummyWatch2 = Watch("an-id-2345", "Watch 2", platformIdentifier)
    private val dummyWatch3 = Watch("an-id-3456", "Watch 3", platformIdentifier)
    private val dummyWatches = listOf(dummyWatch1, dummyWatch2, dummyWatch3)
    private val appState = MutableStateFlow(AppState())

    @RelaxedMockK private lateinit var widgetIdStore: DataStore<Preferences>
    @RelaxedMockK private lateinit var repository: WatchRepository
    @RelaxedMockK private lateinit var analytics: Analytics
    @RelaxedMockK private lateinit var batteryStatsDatabase: WatchBatteryStatsDatabase
    @RelaxedMockK private lateinit var dataStore: DataStore<AppState>
    @RelaxedMockK private lateinit var settingsDatabase: WatchSettingsDatabase

    private lateinit var coroutineScope: CoroutineScope
    private lateinit var watchManager: WatchManager

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { dataStore.data } returns appState

        coroutineScope = TestCoroutineScope()
    }

    @Test
    fun `selectedWatch is updated correctly on init`(): Unit = runBlocking {
        every { repository.registeredWatches } returns liveData { emit(dummyWatches) }
        appState.emit(AppState(lastSelectedWatchId = dummyWatch1.id))
        watchManager = getWatchManager()

        assertThat(watchManager.selectedWatch.getOrAwaitValue()).isEqualTo(dummyWatch1)
    }

    @Test
    fun `selectedWatch is updated when registeredWatches is updated`(): Unit = runBlocking {
        // Initialise WatchManager
        val registeredWatches = MutableLiveData(dummyWatches)
        every { repository.registeredWatches } returns registeredWatches
        appState.emit(AppState(lastSelectedWatchId = dummyWatch1.id))
        watchManager = getWatchManager()

        // Modify the selected watches status
        val modifiedWatch = dummyWatch1
        modifiedWatch.status = Watch.Status.CONNECTED
        val newDummyWatches = registeredWatches.value!!.toMutableList()
        newDummyWatches.removeIf { it.id == modifiedWatch.id }
        newDummyWatches.add(modifiedWatch)
        registeredWatches.value = newDummyWatches

        // Check the selected watches status has updated
        watchManager.selectedWatch.getOrAwaitValue {
            assertThat(it?.status).isEquivalentAccordingToCompareTo(modifiedWatch.status)
        }
    }

    @Test
    fun `selectWatchById updates selectedWatch`(): Unit = runBlocking {
        // Initialise WatchManager
        every { repository.registeredWatches } returns liveData { emit(dummyWatches) }
        appState.emit(AppState(lastSelectedWatchId = dummyWatch1.id))
        watchManager = getWatchManager()
        watchManager.selectWatchById(dummyWatch2.id)

        // Check the selected watch has been updated
        assertThat(watchManager.selectedWatch.getOrAwaitValue()).isEqualTo(dummyWatch2)
    }

    @Test
    fun `requestRefreshCapabilities calls repository`(): Unit = runBlocking {
        watchManager = getWatchManager()
        watchManager.requestRefreshCapabilities(dummyWatch1)
        coVerify(exactly = 1) { repository.sendMessage(dummyWatch1, REQUEST_UPDATE_CAPABILITIES) }
    }

    @Test
    fun `registerWatch calls repository and logs analytics event`(): Unit = runBlocking {
        watchManager = getWatchManager()
        watchManager.registerWatch(dummyWatch1)
        verify(exactly = 1) { analytics.logWatchRegistered() }
        coVerify(exactly = 1) { repository.registerWatch(dummyWatch1) }
    }

    @Test
    fun `forgetWatch calls repository, databases and logs analytics event`(): Unit = runBlocking {
        watchManager = getWatchManager()
        watchManager.forgetWatch(
            widgetIdStore,
            batteryStatsDatabase,
            dummyWatch1
        )
        verify(exactly = 1) { analytics.logWatchRemoved() }
        coVerify(exactly = 1) { repository.resetWatch(dummyWatch1) }
        verify(exactly = 1) { batteryStatsDatabase.batteryStatsDao() }
    }

    @Test
    fun `renameWatch calls repository and logs analytics event`(): Unit = runBlocking {
        val newName = "dummy name"
        watchManager = getWatchManager()
        watchManager.renameWatch(dummyWatch1, newName)
        verify(exactly = 1) { analytics.logWatchRenamed() }
        coVerify(exactly = 1) { repository.renameWatch(dummyWatch1, newName) }
    }

    @Test
    fun `resetWatchPreferences calls repository and databases`(): Unit = runBlocking {
        watchManager = getWatchManager()
        watchManager.resetWatchPreferences(
            widgetIdStore,
            batteryStatsDatabase,
            dummyWatch1
        )
        coVerify(exactly = 1) { repository.sendMessage(dummyWatch1, CLEAR_PREFERENCES) }
        coVerify(exactly = 1) { settingsDatabase.clearWatchPreferences(dummyWatch1) }
        verify(exactly = 1) { batteryStatsDatabase.batteryStatsDao() }
    }

    @Test
    fun `refreshData calls WatchRepository`() {
        watchManager = getWatchManager()
        watchManager.refreshData()
        verify(exactly = 1) { repository.refreshData() }
    }

    @Test
    fun `selectedWatch is null if there are no registered watches`() {
        every { repository.registeredWatches } returns liveData { emit(emptyList<Watch>()) }
        watchManager = getWatchManager()
        try {
            // We expect this to throw an exception, so use a shorter timeout
            assertThat(
                watchManager.selectedWatch.getOrAwaitValue(
                    time = 500, timeUnit = TimeUnit.MILLISECONDS
                )
            ).isNull()
        } catch (e: Exception) {
            assertThat(watchManager.selectedWatch.value).isNull()
        }
    }

    @Test
    fun `selectedWatch is not null if there are registered watches and last watch ID is known`():
        Unit = runBlocking {
            appState.emit(AppState(lastSelectedWatchId = dummyWatch1.id))
            every { repository.registeredWatches } returns liveData { emit(dummyWatches) }
            watchManager = getWatchManager()
            try {
                assertThat(
                    watchManager.selectedWatch.getOrAwaitValue()
                ).isNotNull()
            } catch (e: Exception) {
                assertThat(watchManager.selectedWatch.value).isNotNull()
            }
        }

    @Test
    fun `selectedWatch is not null if there are registered watches and no last watch ID is known`():
        Unit = runBlocking {
            appState.emit(AppState(lastSelectedWatchId = ""))
            every { repository.registeredWatches } returns liveData { emit(dummyWatches) }
            watchManager = getWatchManager()
            try {
                assertThat(
                    watchManager.selectedWatch.getOrAwaitValue()
                ).isNotNull()
            } catch (e: Exception) {
                assertThat(watchManager.selectedWatch.value).isNotNull()
            }
        }

    private fun getWatchManager(): WatchManager {
        return WatchManager(
            repository,
            settingsDatabase,
            analytics,
            dataStore,
            coroutineScope
        )
    }
}

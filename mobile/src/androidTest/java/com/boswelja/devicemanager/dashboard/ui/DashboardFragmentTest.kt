package com.boswelja.devicemanager.dashboard.ui

import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.boswelja.devicemanager.R
import com.boswelja.devicemanager.watchmanager.WatchManager
import com.boswelja.devicemanager.watchmanager.connection.WearOSConnectionInterface
import com.boswelja.devicemanager.watchmanager.item.Watch
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockkObject
import org.junit.Before
import org.junit.Test

class DashboardFragmentTest {

    private val dummyWatch1 = Watch("an-id-1234", "Watch 1", WearOSConnectionInterface.PLATFORM)

    @RelaxedMockK
    private lateinit var watchManager: WatchManager

    private lateinit var scenario: FragmentScenario<DashboardFragment>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(WatchManager.Companion)
        every { WatchManager.Companion.getInstance(any()) } returns watchManager
        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_App,
            initialState = Lifecycle.State.INITIALIZED
        )
    }

    @Test
    fun watchStatusUpdatesCorrectlyWithInitialValue() {
        val dummySelectedWatch = MutableLiveData(dummyWatch1)
        every { watchManager.selectedWatch } returns dummySelectedWatch
        // Move the fragment to State.STARTED
        scenario.moveToState(Lifecycle.State.STARTED)
        Thread.sleep(500)
        onView(withId(R.id.watch_status_text))
            .check(matches(withText(dummyWatch1.status.stringRes)))
        onView(withId(R.id.watch_status_icon))
            .check(matches(withText(dummyWatch1.status.iconRes)))
    }

    @Test
    fun watchStatusUpdatesCorrectlyWithUpdatingValue() {
        val dummySelectedWatch = MutableLiveData<Watch>()
        every { watchManager.selectedWatch } returns dummySelectedWatch
        // Move the fragment to State.STARTED
        scenario.moveToState(Lifecycle.State.STARTED)

        // Check with updated watch
        dummySelectedWatch.value = dummyWatch1
        Thread.sleep(500)
        onView(withId(R.id.watch_status_text))
            .check(matches(withText(dummyWatch1.status.stringRes)))
        onView(withId(R.id.watch_status_icon))
            .check(matches(withText(dummyWatch1.status.iconRes)))

        // Check with status change
        val newDummy = dummyWatch1
        newDummy.status = Watch.Status.CONNECTED
        dummySelectedWatch.value = newDummy
        Thread.sleep(500)
        onView(withId(R.id.watch_status_text))
            .check(matches(withText(dummyWatch1.status.stringRes)))
        onView(withId(R.id.watch_status_icon))
            .check(matches(withText(dummyWatch1.status.iconRes)))
    }

    @Test
    fun watchStatusIsCompletelyVisible() {
        val dummySelectedWatch = MutableLiveData(dummyWatch1)
        every { watchManager.selectedWatch } returns dummySelectedWatch
        // Move the fragment to State.STARTED
        scenario.moveToState(Lifecycle.State.STARTED)

        // Ensure watch status is populated first
        Thread.sleep(500)
        onView(withId(R.id.watch_status_text))
            .check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.watch_status_icon))
            .check(matches(isCompletelyDisplayed()))
    }
}
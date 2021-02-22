package com.boswelja.devicemanager.common.ui

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.launchActivity
import com.boswelja.devicemanager.widget.database.WatchWidgetAssociation
import com.boswelja.devicemanager.widget.database.WidgetDatabase
import com.google.common.truth.Truth.assertThat
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class BaseWidgetConfigActivityTest {

    private val dummyWatchId = "watch-id"
    private val dummyWidgetId = 1234
    private lateinit var dummyIntent: Intent

    private lateinit var scenario: ActivityScenario<ConcreteBaseWidgetConfigActivity>

    @RelaxedMockK
    lateinit var widgetDatabase: WidgetDatabase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        dummyIntent = Intent(
            ApplicationProvider.getApplicationContext(),
            ConcreteBaseWidgetConfigActivity::class.java
        ).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, dummyWidgetId)
        }

        mockkObject(WidgetDatabase.Companion)
        every { WidgetDatabase.Companion.getInstance(any()) } returns widgetDatabase

        scenario = launchActivity(intent = dummyIntent)
    }

    @After
    fun tearDown() {
        unmockkObject(WidgetDatabase.Companion)
    }

    @Test
    fun finishWithoutConfigReturnsID() {
        scenario.onActivity { it.finish() }
        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(
            scenario.result.resultData.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                0
            )
        ).isEqualTo(dummyWidgetId)
    }

    @Test
    fun finishWithConfigReturnsOK() {
        checkCommonFinishWithConfig(WatchWidgetAssociation(dummyWatchId, dummyWidgetId))
    }

    @Test
    fun finishWithWatchBatteryWidgetUpdatesDatabase() {
        val watchWidgetAssociation = WatchWidgetAssociation(dummyWatchId, dummyWidgetId)
        checkCommonFinishWithConfig(watchWidgetAssociation)
        verify(exactly = 1) { widgetDatabase.addWidget(watchWidgetAssociation) }
    }

    private fun checkCommonFinishWithConfig(config: WatchWidgetAssociation) {
        scenario.onActivity {
            it.finishWidgetConfig(config)
        }
        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        assertThat(
            scenario.result.resultData.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                0
            )
        ).isEqualTo(dummyWidgetId)
    }
}

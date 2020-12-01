package com.boswelja.devicemanager.messages

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.boswelja.devicemanager.getOrAwaitValue
import com.boswelja.devicemanager.messages.database.MessageDatabase
import com.google.common.truth.Truth.assertThat
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.N, Build.VERSION_CODES.O])
class MessageHandlerTest {

    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

    private val context = spyk(InstrumentationRegistry.getInstrumentation().context)
    private val coroutineScope = TestCoroutineScope()
    private val message = Message(
        Message.Icon.HELP,
        "Test Message",
        "This is a test message",
        Message.Action.LAUNCH_CHANGELOG
    )
    private lateinit var messageDatabase: MessageDatabase
    @RelaxedMockK private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        messageDatabase = Room.inMemoryDatabaseBuilder(context, MessageDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager
    }

    @After
    fun tearDown() {
        messageDatabase.clearAllTables()
        messageDatabase.close()
    }

    @Test
    fun `Low priority message only posts the message to the database`() {
        MessageHandler.postMessage(
            context,
            message,
            Priority.LOW,
            messageDatabase,
            coroutineScope
        )
        verify(exactly = 0) { notificationManager.notify(any(), any()) }
        val count = messageDatabase.messageDao().getActiveMessagesCount()
        count.getOrAwaitValue {
            assertThat(it).isEqualTo(1)
        }
    }

    @Test
    fun `High priority message only posts the message to the database`() {
        MessageHandler.postMessage(
            context,
            message,
            Priority.HIGH,
            messageDatabase,
            coroutineScope
        )
        verify(exactly = 1) { notificationManager.notify(any(), any()) }
        val count = messageDatabase.messageDao().getActiveMessagesCount()
        count.getOrAwaitValue {
            assertThat(it).isEqualTo(1)
        }
    }

    @Test
    fun `Notification channel is created if needed`() {
        MessageHandler.postMessage(
            context,
            message,
            Priority.HIGH,
            messageDatabase,
            coroutineScope
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            verify(exactly = 1) { notificationManager.createNotificationChannel(any()) }
        }
    }
}

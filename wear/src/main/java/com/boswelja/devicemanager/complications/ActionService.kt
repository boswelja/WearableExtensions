package com.boswelja.devicemanager.complications

import android.app.Notification
import android.app.NotificationChannel
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.wearable.activity.ConfirmationActivity
import android.util.Log
import com.boswelja.devicemanager.common.Config
import com.boswelja.devicemanager.common.Utils
import com.boswelja.devicemanager.ui.MainActivity
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable

class ActionService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private lateinit var action: String

    override fun onCreate() {
        super.onCreate()
        val notification: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification
                    .Builder(this, NotificationChannel.DEFAULT_CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }
        notification.setContentTitle("Locking your phone...")
        startForeground(312, notification.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        action = intent!!.getStringExtra(Config.INTENT_ACTION_EXTRA)
        val capabilityCallback = object: Utils.CapabilityCallbacks {
            override fun noCapableDevices() {
                val activityIntent = Intent(this@ActionService, MainActivity::class.java)
                startActivity(activityIntent)
            }

            override fun capableDeviceFound(node: Node?) {
                lockDevice(node)
            }
        }
        Utils.isCompanionAppInstalled(this, capabilityCallback)
        return START_NOT_STICKY
    }

    private fun onFailed() {
        Log.d("ActionService", "Failed to lock phone")
        val intent = Intent(this, ConfirmationActivity::class.java)
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.FAILURE_ANIMATION)
        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, "Failed to lock your phone")
        startActivity(intent)
        stopForeground(true)
        stopSelf()
    }

    private fun onSuccess() {
        Log.d("ActionService", "Phone locked")
        val intent = Intent(this, ConfirmationActivity::class.java)
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION)
        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, "Locked your phone")
        startActivity(intent)
        stopForeground(true)
        stopSelf()
    }

    private fun lockDevice(node: Node?) {
        Wearable.getMessageClient(this)
                .sendMessage(
                    node!!.id,
                    action,
                    null
                )
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener {
                    onFailed()
                }
    }
}
@file:Suppress("DEPRECATION")

package com.boswelja.devicemanager.common

import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.text.Html
import android.text.Spanned

object Compat {

    fun getPendingJob(jobScheduler: JobScheduler, id: Int) : JobInfo? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            jobScheduler.getPendingJob(id)
        } else {
            val jobs = jobScheduler.allPendingJobs
            jobs.first { j -> j.id == id }
        }
    }

    fun startService(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun setInterruptionFilter(context: Context, dndEnabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (dndEnabled) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            } else {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        } else {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (dndEnabled) {
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            } else {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            }
        }
    }
}
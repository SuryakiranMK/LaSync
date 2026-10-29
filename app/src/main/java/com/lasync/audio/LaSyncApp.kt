package com.lasync.audio

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class LaSyncApp : Application() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "lasync_channel"
        const val NOTIFICATION_CHANNEL_NAME = "LaSync CyberShade"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps LaSync audio synchronization active and controllable in background"
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}

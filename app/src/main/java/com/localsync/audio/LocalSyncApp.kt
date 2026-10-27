package com.localsync.audio

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class LocalSyncApp : Application() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "localsync_channel"
        const val NOTIFICATION_CHANNEL_NAME = "LocalSync Background Service"
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
                description = "Keeps LocalSync network synchronization active in background"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}

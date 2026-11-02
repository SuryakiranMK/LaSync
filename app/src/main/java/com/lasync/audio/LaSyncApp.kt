package com.lasync.audio

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.lasync.audio.core.network.HostNetworkEngine
import com.lasync.audio.core.network.ReceiverNetworkEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class LaSyncApp : Application() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "lasync_channel"
        const val NOTIFICATION_CHANNEL_NAME = "LaSync CyberShade"

        lateinit var instance: LaSyncApp
            private set
    }

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val hostEngine: HostNetworkEngine by lazy {
        HostNetworkEngine(applicationScope)
    }

    val receiverEngine: ReceiverNetworkEngine by lazy {
        ReceiverNetworkEngine(applicationScope, applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
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

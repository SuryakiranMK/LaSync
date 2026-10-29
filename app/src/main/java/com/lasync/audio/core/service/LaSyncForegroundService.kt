package com.lasync.audio.core.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.lasync.audio.LaSyncApp
import com.lasync.audio.MainActivity

class LaSyncForegroundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    companion object {
        private const val TAG = "LaSyncService"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_START_PROJECTION = "ACTION_START_PROJECTION"
        const val ACTION_STOP_PROJECTION = "ACTION_STOP_PROJECTION"
        const val ACTION_UPDATE = "ACTION_UPDATE"
        const val ACTION_STOP_AUDIO = "ACTION_STOP_AUDIO"

        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_MESSAGE = "EXTRA_MESSAGE"
        const val EXTRA_MEDIA_PROJECTION = "EXTRA_MEDIA_PROJECTION"
        const val EXTRA_IS_AUDIO_ACTIVE = "EXTRA_IS_AUDIO_ACTIVE"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_DATA = "EXTRA_RESULT_DATA"

        var onSessionStopRequested: (() -> Unit)? = null
        var onStopAudioRequested: (() -> Unit)? = null

        private var onProjectionReadyCallback: ((MediaProjection?) -> Unit)? = null
        private var activeMediaProjection: MediaProjection? = null

        fun start(
            context: Context,
            title: String,
            message: String,
            withMediaProjection: Boolean = false
        ) {
            val intent = Intent(context, LaSyncForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_MESSAGE, message)
                putExtra(EXTRA_MEDIA_PROJECTION, withMediaProjection)
            }
            context.startForegroundService(intent)
        }

        fun updateNotification(
            context: Context,
            title: String,
            message: String,
            isAudioActive: Boolean = false
        ) {
            val intent = Intent(context, LaSyncForegroundService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_MESSAGE, message)
                putExtra(EXTRA_IS_AUDIO_ACTIVE, isAudioActive)
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }

        fun startWithMediaProjection(
            context: Context,
            resultCode: Int,
            resultData: Intent,
            onProjectionReady: (MediaProjection?) -> Unit
        ) {
            onProjectionReadyCallback = onProjectionReady
            val intent = Intent(context, LaSyncForegroundService::class.java).apply {
                action = ACTION_START_PROJECTION
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
            }
            context.startForegroundService(intent)
        }

        fun stopMediaProjection(context: Context) {
            try {
                activeMediaProjection?.stop()
            } catch (_: Exception) {}
            activeMediaProjection = null

            val intent = Intent(context, LaSyncForegroundService::class.java).apply {
                action = ACTION_STOP_PROJECTION
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            try {
                activeMediaProjection?.stop()
            } catch (_: Exception) {}
            activeMediaProjection = null
            onProjectionReadyCallback = null

            val intent = Intent(context, LaSyncForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "LaSync Audio"
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Synchronizing over local Wi-Fi"
                val withMediaProjection = intent.getBooleanExtra(EXTRA_MEDIA_PROJECTION, false)

                acquireLocks()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val fgsType = if (withMediaProjection) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    } else {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                    }
                    startForeground(NOTIFICATION_ID, buildNotification(title, message), fgsType)
                } else {
                    startForeground(NOTIFICATION_ID, buildNotification(title, message))
                }
            }
            ACTION_START_PROJECTION -> {
                acquireLocks()

                // Step 1: Promote Foreground Service to FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION synchronously
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val fgsType = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    startForeground(
                        NOTIFICATION_ID,
                        buildNotification("LaSync Music Share Active", "Streaming system audio across LAN"),
                        fgsType
                    )
                } else {
                    startForeground(
                        NOTIFICATION_ID,
                        buildNotification("LaSync Music Share Active", "Streaming system audio across LAN")
                    )
                }

                // Step 2: Now that startForeground is completed synchronously with MEDIA_PROJECTION type,
                // retrieve the MediaProjection token safely!
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val resultData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    try {
                        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        val projection = projectionManager.getMediaProjection(resultCode, resultData)
                        if (projection != null) {
                            // Register required callback for Android 14+
                            projection.registerCallback(object : MediaProjection.Callback() {
                                override fun onStop() {
                                    activeMediaProjection = null
                                }
                            }, Handler(Looper.getMainLooper()))
                            activeMediaProjection = projection
                            onProjectionReadyCallback?.invoke(projection)
                        } else {
                            Log.e(TAG, "MediaProjectionManager returned null projection")
                            onProjectionReadyCallback?.invoke(null)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error obtaining MediaProjection in service: ${e.message}", e)
                        onProjectionReadyCallback?.invoke(null)
                    } finally {
                        onProjectionReadyCallback = null
                    }
                } else {
                    onProjectionReadyCallback?.invoke(null)
                    onProjectionReadyCallback = null
                }
            }
            ACTION_STOP_PROJECTION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        buildNotification("LaSync Host Active", "Broadcasting master session on LAN", isAudioActive = false),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                    )
                } else {
                    startForeground(
                        NOTIFICATION_ID,
                        buildNotification("LaSync Host Active", "Broadcasting master session on LAN", isAudioActive = false)
                    )
                }
            }
            ACTION_UPDATE -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "LaSync Audio"
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Synchronizing over local Wi-Fi"
                val isAudioActive = intent.getBooleanExtra(EXTRA_IS_AUDIO_ACTIVE, false)
                val notification = buildNotification(title, message, isAudioActive)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, notification)
            }
            ACTION_STOP_AUDIO -> {
                onStopAudioRequested?.invoke()
            }
            ACTION_STOP -> {
                onSessionStopRequested?.invoke()
                releaseLocks()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun buildNotification(
        title: String,
        message: String,
        isAudioActive: Boolean = false
    ): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopSessionIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, LaSyncForegroundService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, LaSyncApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
                    .setSummaryText("LaSync CyberShade")
            )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }

        // Action 1: Stop Session directly from notification shade / lock screen
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Stop Session",
            stopSessionIntent
        )

        // Action 2: Stop Audio (if music or tone test is streaming)
        if (isAudioActive) {
            val stopAudioIntent = PendingIntent.getService(
                this,
                2,
                Intent(this, LaSyncForegroundService::class.java).apply {
                    action = ACTION_STOP_AUDIO
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "Stop Audio",
                stopAudioIntent
            )
        }

        return builder.build()
    }

    @Suppress("DEPRECATION")
    private fun acquireLocks() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "LaSync::ServiceWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(24 * 60 * 60 * 1000L) // Safe 24hr max
            }
        }

        if (wifiLock == null) {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiLock = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                wifiManager.createWifiLock(
                    WifiManager.WIFI_MODE_FULL_LOW_LATENCY,
                    "LaSync::LowLatencyWifiLock"
                )
            } else {
                wifiManager.createWifiLock(
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                    "LaSync::HighPerfWifiLock"
                )
            }.apply {
                setReferenceCounted(false)
                acquire()
            }
        }
    }

    private fun releaseLocks() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null

        wifiLock?.let {
            if (it.isHeld) it.release()
        }
        wifiLock = null
    }

    override fun onDestroy() {
        releaseLocks()
        super.onDestroy()
    }
}

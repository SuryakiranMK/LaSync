package com.lasync.audio

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.lasync.audio.core.network.HostNetworkEngine
import com.lasync.audio.core.network.ReceiverNetworkEngine
import com.lasync.audio.core.network.ReceiverState
import com.lasync.audio.core.service.LaSyncForegroundService
import com.lasync.audio.core.updater.UpdateChecker
import com.lasync.audio.core.updater.UpdateInfo
import com.lasync.audio.ui.components.SessionExitDialog
import com.lasync.audio.ui.components.UpdateDialog
import com.lasync.audio.ui.screens.HomeScreen
import com.lasync.audio.ui.screens.HostScreen
import com.lasync.audio.ui.screens.ReceiverScreen
import com.lasync.audio.ui.theme.LaSyncTheme
import com.lasync.audio.ui.theme.VoidBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ScreenState {
    HOME,
    HOST,
    RECEIVER
}

class MainActivity : ComponentActivity() {

    private lateinit var hostEngine: HostNetworkEngine
    private lateinit var receiverEngine: ReceiverNetworkEngine

    private val availableUpdateState = mutableStateOf<UpdateInfo?>(null)
    private val currentScreenState = mutableStateOf(ScreenState.HOME)
    private var pendingMediaProjectionCallback: ((MediaProjection?) -> Unit)? = null

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            try {
                // Pass directly to foreground service to ensure ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                // is active before getMediaProjection() is invoked (mandatory on Android 14 and 15)
                LaSyncForegroundService.startWithMediaProjection(
                    context = this,
                    resultCode = result.resultCode,
                    resultData = result.data!!
                ) { projection ->
                    if (projection != null) {
                        pendingMediaProjectionCallback?.invoke(projection)
                    } else {
                        pendingMediaProjectionCallback?.invoke(null)
                    }
                    pendingMediaProjectionCallback = null
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to start MediaProjection via Service: ${e.message}", e)
                pendingMediaProjectionCallback?.invoke(null)
                pendingMediaProjectionCallback = null
            }
        } else {
            pendingMediaProjectionCallback?.invoke(null)
            pendingMediaProjectionCallback = null
        }
    }

    private val audioRecordPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (pendingMediaProjectionCallback != null) {
                launchMediaProjectionPrompt()
            }
        } else {
            pendingMediaProjectionCallback?.invoke(null)
            pendingMediaProjectionCallback = null
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Notification permission handled
    }

    private fun requestMediaProjection(callback: (MediaProjection?) -> Unit) {
        pendingMediaProjectionCallback = callback
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            launchMediaProjectionPrompt()
        } else {
            audioRecordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun launchMediaProjectionPrompt() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    private fun checkForAppUpdate() {
        lifecycleScope.launch(Dispatchers.IO) {
            val currentVersion = try {
                packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.4"
            } catch (_: Exception) {
                "1.0.4"
            }
            val update = UpdateChecker.checkForUpdate(currentVersion)
            if (update != null) {
                withContext(Dispatchers.Main) {
                    availableUpdateState.value = update
                }
            }
        }
    }

    private fun observeSessionStateForNotifications() {
        lifecycleScope.launch {
            hostEngine.uiState.collect { state ->
                if (currentScreenState.value == ScreenState.HOST && state.isRunning) {
                    val title = when {
                        state.isMegaphoneActive -> "LaSync Master • Ghost Megaphone"
                        state.activeAudioSource == com.lasync.audio.core.audio.HostAudioSource.SYSTEM_CAPTURE -> "LaSync Master • Sharing Music"
                        state.activeAudioSource == com.lasync.audio.core.audio.HostAudioSource.SYNTHETIC_TONE -> "LaSync Master • 440 Hz Tone"
                        state.activeAudioSource == com.lasync.audio.core.audio.HostAudioSource.RAM_JUKEBOX -> "LaSync Master • RAM Jukebox"
                        else -> "LaSync Master Host • ${state.sessionId}"
                    }
                    val msg = when {
                        state.isMegaphoneActive -> "🎙️ Live Walkie-Talkie broadcast to ${state.receivers.size} phone(s) at MAX volume"
                        state.activeAudioSource == com.lasync.audio.core.audio.HostAudioSource.SYSTEM_CAPTURE -> "🎵 Streaming System Music [${state.bufferMode.title}] to ${state.receivers.size} phone(s) • ${state.audioFramesSent} frames"
                        state.activeAudioSource == com.lasync.audio.core.audio.HostAudioSource.SYNTHETIC_TONE -> "🔊 Synchronized 440 Hz tone [${state.bufferMode.title}] • ${state.audioFramesSent} frames"
                        state.activeAudioSource == com.lasync.audio.core.audio.HostAudioSource.RAM_JUKEBOX -> "🎵 RAM Jukebox: ${state.loadedJukeboxTrack?.title ?: "Playing"} (Zero-Traffic) • ${state.receivers.size} phone(s)"
                        else -> if (state.receivers.isEmpty()) "🟢 Beaconing on LAN • Waiting for listeners" else "🟢 ${state.receivers.size} listener(s) phase-locked"
                    }
                    LaSyncForegroundService.updateNotification(
                        this@MainActivity,
                        title,
                        msg,
                        isAudioActive = state.isAudioStreaming
                    )
                }
            }
        }

        lifecycleScope.launch {
            receiverEngine.uiState.collect { receiverState ->
                if (currentScreenState.value == ScreenState.RECEIVER && receiverState.state != com.lasync.audio.core.network.ReceiverState.IDLE) {
                    val isConnected = receiverState.state == com.lasync.audio.core.network.ReceiverState.CONNECTED
                    val title = if (receiverState.isMegaphoneActive) {
                        "🚨 LaSync • Host Megaphone PA"
                    } else if (isConnected) "LaSync Receiver • Phase-Locked" else "LaSync Receiver • Discovering"
                    val msg = if (receiverState.isMegaphoneActive) {
                        "🎙️ Host live voice @ 100% volume from ${receiverState.connectedHostName}"
                    } else if (receiverState.isJukeboxPlaying) {
                        "🎵 RAM Jukebox: ${receiverState.jukeboxTrackTitle} (Zero-Traffic) • RTT ${receiverState.rttMs.toInt()}ms"
                    } else if (receiverState.isJukeboxDownloading) {
                        "⚡ Preloading RAM track: ${(receiverState.jukeboxDownloadProgress * 100).toInt()}%"
                    } else if (receiverState.isJukeboxReady) {
                        "✅ RAM Jukebox Ready: ${receiverState.jukeboxTrackTitle}"
                    } else if (isConnected) {
                        if (receiverState.isAudioPlaying) {
                            "🎵 Playing Master Stream • ${receiverState.audioFramesReceived} frames • RTT ${receiverState.rttMs.toInt()}ms"
                        } else {
                            "🟢 Linked to ${receiverState.connectedHostName} • RTT ${receiverState.rttMs.toInt()}ms • Loss ${String.format(java.util.Locale.US, "%.1f", receiverState.packetLossPercent)}%"
                        }
                    } else {
                        "📡 Scanning Wi-Fi subnet for Master Host beacon..."
                    }
                    LaSyncForegroundService.updateNotification(
                        this@MainActivity,
                        title,
                        msg,
                        isAudioActive = receiverState.isAudioPlaying
                    )
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hostEngine = (application as LaSyncApp).hostEngine
        receiverEngine = (application as LaSyncApp).receiverEngine
        syncScreenWithEngineState()

        LaSyncForegroundService.onSessionStopRequested = {
            lifecycleScope.launch(Dispatchers.Main) {
                hostEngine.stopAllAudio()
                hostEngine.stop()
                receiverEngine.stop()
                currentScreenState.value = ScreenState.HOME
            }
        }

        LaSyncForegroundService.onStopAudioRequested = {
            lifecycleScope.launch(Dispatchers.Main) {
                hostEngine.stopAllAudio()
            }
        }

        checkPermissions()
        checkForAppUpdate()
        observeSessionStateForNotifications()

        setContent {
            LaSyncTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    color = VoidBackground
                ) {
                    var currentScreen by currentScreenState
                    val availableUpdate by availableUpdateState
                    var showExitDialog by remember { mutableStateOf(false) }
                    var lastBackPressTime by remember { mutableStateOf(0L) }
                    val hostState by hostEngine.uiState.collectAsState()
                    val receiverState by receiverEngine.uiState.collectAsState()

                    // Modern Ergonomic BackHandler
                    BackHandler {
                        when (currentScreen) {
                            ScreenState.HOME -> {
                                val now = System.currentTimeMillis()
                                if (now - lastBackPressTime < 2000L) {
                                    finish()
                                } else {
                                    lastBackPressTime = now
                                    Toast.makeText(this@MainActivity, "Press back again to exit", Toast.LENGTH_SHORT).show()
                                }
                            }
                            ScreenState.RECEIVER -> {
                                if (receiverState.state == ReceiverState.CONNECTED) {
                                    showExitDialog = true
                                } else {
                                    // Not connected yet: smoothly return to Home
                                    receiverEngine.stop()
                                    LaSyncForegroundService.stop(this@MainActivity)
                                    currentScreen = ScreenState.HOME
                                }
                            }
                            ScreenState.HOST -> {
                                showExitDialog = true
                            }
                        }
                    }

                    // In-App GitHub Auto-Updater Dialog
                    if (availableUpdate != null) {
                        UpdateDialog(
                            updateInfo = availableUpdate!!,
                            onDismiss = {
                                availableUpdateState.value = null
                            }
                        )
                    }

                    // Active Session Exit / Background Dialog
                    if (showExitDialog) {
                        SessionExitDialog(
                            isHost = currentScreen == ScreenState.HOST,
                            connectedCount = hostState.receivers.size,
                            connectedHostName = receiverState.connectedHostName,
                            onRunInBackground = {
                                showExitDialog = false
                                moveTaskToBack(true)
                            },
                            onEndSession = {
                                showExitDialog = false
                                if (currentScreen == ScreenState.HOST) {
                                    hostEngine.stopAllAudio()
                                    hostEngine.stop()
                                } else {
                                    receiverEngine.stop()
                                }
                                LaSyncForegroundService.stop(this@MainActivity)
                                currentScreen = ScreenState.HOME
                            },
                            onDismiss = {
                                showExitDialog = false
                            }
                        )
                    }

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "screen_transition"
                    ) { screen ->
                        when (screen) {
                            ScreenState.HOME -> {
                                HomeScreen(
                                    onCreateSession = {
                                        LaSyncForegroundService.start(
                                            this@MainActivity,
                                            "LaSync Host Active",
                                            "Broadcasting master session on LAN"
                                        )
                                        hostEngine.start()
                                        currentScreen = ScreenState.HOST
                                    },
                                    onJoinSession = {
                                        LaSyncForegroundService.start(
                                            this@MainActivity,
                                            "LaSync Receiver Active",
                                            "Discovering and syncing with master host"
                                        )
                                        receiverEngine.startDiscovery()
                                        currentScreen = ScreenState.RECEIVER
                                    }
                                )
                            }

                            ScreenState.HOST -> {
                                HostScreen(
                                    engine = hostEngine,
                                    onRequestMediaProjection = { callback ->
                                        requestMediaProjection(callback)
                                    },
                                    onBack = {
                                        showExitDialog = true
                                    },
                                    onStopSession = {
                                        showExitDialog = true
                                    }
                                )
                            }

                            ScreenState.RECEIVER -> {
                                ReceiverScreen(
                                    engine = receiverEngine,
                                    onBack = {
                                        if (receiverState.state == ReceiverState.CONNECTED) {
                                            showExitDialog = true
                                        } else {
                                            receiverEngine.stop()
                                            LaSyncForegroundService.stop(this@MainActivity)
                                            currentScreen = ScreenState.HOME
                                        }
                                    },
                                    onLeave = {
                                        if (receiverState.state == ReceiverState.CONNECTED) {
                                            showExitDialog = true
                                        } else {
                                            receiverEngine.stop()
                                            LaSyncForegroundService.stop(this@MainActivity)
                                            currentScreen = ScreenState.HOME
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            audioRecordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onResume() {
        super.onResume()
        syncScreenWithEngineState()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        syncScreenWithEngineState()
    }

    private fun syncScreenWithEngineState() {
        if (!::hostEngine.isInitialized || !::receiverEngine.isInitialized) return
        when {
            hostEngine.uiState.value.isRunning -> {
                if (currentScreenState.value != ScreenState.HOST) {
                    currentScreenState.value = ScreenState.HOST
                }
            }
            receiverEngine.uiState.value.state != ReceiverState.IDLE -> {
                if (currentScreenState.value != ScreenState.RECEIVER) {
                    currentScreenState.value = ScreenState.RECEIVER
                }
            }
        }
    }


    override fun onDestroy() {
        LaSyncForegroundService.onSessionStopRequested = null
        LaSyncForegroundService.onStopAudioRequested = null
        // Do not stop engines or foreground service here.
        // Active sessions persist in the background until the user explicitly stops them.
        super.onDestroy()
    }
}

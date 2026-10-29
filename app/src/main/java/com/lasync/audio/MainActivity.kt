package com.lasync.audio

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.lasync.audio.core.network.HostNetworkEngine
import com.lasync.audio.core.network.ReceiverNetworkEngine
import com.lasync.audio.core.service.LaSyncForegroundService
import com.lasync.audio.ui.screens.HomeScreen
import com.lasync.audio.ui.screens.HostScreen
import com.lasync.audio.ui.screens.ReceiverScreen
import com.lasync.audio.ui.theme.LaSyncTheme
import com.lasync.audio.ui.theme.VoidBackground

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import com.lasync.audio.core.updater.UpdateChecker
import com.lasync.audio.core.updater.UpdateInfo
import com.lasync.audio.ui.components.UpdateDialog
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
            launchMediaProjectionPrompt()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hostEngine = HostNetworkEngine(lifecycleScope)
        receiverEngine = ReceiverNetworkEngine(lifecycleScope)

        checkPermissions()
        checkForAppUpdate()

        setContent {
            LaSyncTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VoidBackground
                ) {
                    var currentScreen by remember { mutableStateOf(ScreenState.HOME) }
                    val availableUpdate by availableUpdateState

                    // In-App GitHub Auto-Updater Dialog
                    if (availableUpdate != null) {
                        UpdateDialog(
                            updateInfo = availableUpdate!!,
                            onDownload = {
                                try {
                                    val downloadIntent = Intent(Intent.ACTION_VIEW, Uri.parse(availableUpdate!!.downloadUrl))
                                    startActivity(downloadIntent)
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "Failed to launch update link: ${e.message}")
                                }
                                availableUpdateState.value = null
                            },
                            onDismiss = {
                                availableUpdateState.value = null
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
                                    onStopSession = {
                                        hostEngine.stopAllAudio()
                                        hostEngine.stop()
                                        LaSyncForegroundService.stop(this@MainActivity)
                                        currentScreen = ScreenState.HOME
                                    }
                                )
                            }

                            ScreenState.RECEIVER -> {
                                ReceiverScreen(
                                    engine = receiverEngine,
                                    onLeave = {
                                        receiverEngine.stop()
                                        LaSyncForegroundService.stop(this@MainActivity)
                                        currentScreen = ScreenState.HOME
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
    }

    override fun onDestroy() {
        hostEngine.stop()
        receiverEngine.stop()
        LaSyncForegroundService.stop(this)
        super.onDestroy()
    }
}

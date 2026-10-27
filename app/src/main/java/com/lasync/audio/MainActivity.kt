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

enum class ScreenState {
    HOME,
    HOST,
    RECEIVER
}

class MainActivity : ComponentActivity() {

    private lateinit var hostEngine: HostNetworkEngine
    private lateinit var receiverEngine: ReceiverNetworkEngine

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Notification permission handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hostEngine = HostNetworkEngine(lifecycleScope)
        receiverEngine = ReceiverNetworkEngine(lifecycleScope)

        checkPermissions()

        setContent {
            LaSyncTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VoidBackground
                ) {
                    var currentScreen by remember { mutableStateOf(ScreenState.HOME) }

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
                                    onStopSession = {
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

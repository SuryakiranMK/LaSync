package com.localsync.audio

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
import com.localsync.audio.core.network.HostNetworkEngine
import com.localsync.audio.core.network.ReceiverNetworkEngine
import com.localsync.audio.core.service.LocalSyncForegroundService
import com.localsync.audio.ui.screens.HomeScreen
import com.localsync.audio.ui.screens.HostScreen
import com.localsync.audio.ui.screens.ReceiverScreen
import com.localsync.audio.ui.theme.DarkBackground
import com.localsync.audio.ui.theme.LocalSyncTheme

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
            LocalSyncTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
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
                                        LocalSyncForegroundService.start(
                                            this@MainActivity,
                                            "LocalSync Host Active",
                                            "Broadcasting master session on LAN"
                                        )
                                        hostEngine.start()
                                        currentScreen = ScreenState.HOST
                                    },
                                    onJoinSession = {
                                        LocalSyncForegroundService.start(
                                            this@MainActivity,
                                            "LocalSync Receiver Active",
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
                                        LocalSyncForegroundService.stop(this@MainActivity)
                                        currentScreen = ScreenState.HOME
                                    }
                                )
                            }

                            ScreenState.RECEIVER -> {
                                ReceiverScreen(
                                    engine = receiverEngine,
                                    onLeave = {
                                        receiverEngine.stop()
                                        LocalSyncForegroundService.stop(this@MainActivity)
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
        LocalSyncForegroundService.stop(this)
        super.onDestroy()
    }
}

package com.lasync.audio.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjection
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import com.lasync.audio.core.audio.HostAudioSource
import com.lasync.audio.core.audio.SyncBufferMode
import com.lasync.audio.core.hotspot.AutonomousHotspotManager
import com.lasync.audio.core.hotspot.HotspotInfo
import com.lasync.audio.core.network.HostNetworkEngine
import com.lasync.audio.core.network.HostUiState
import com.lasync.audio.ui.components.HotspotQrDialog
import com.lasync.audio.ui.components.LaSyncButton
import com.lasync.audio.ui.components.LaSyncRadar
import com.lasync.audio.ui.components.LaSyncTopBar
import com.lasync.audio.ui.components.LaSyncWaveEqualizer
import com.lasync.audio.ui.components.ReceiverItemCard
import com.lasync.audio.ui.theme.BorderNeon
import com.lasync.audio.ui.theme.CarbonCard
import com.lasync.audio.ui.theme.CarbonCardHighlight
import com.lasync.audio.ui.theme.CrimsonPulse
import com.lasync.audio.ui.theme.HyperCyan
import com.lasync.audio.ui.theme.QuantumMint
import com.lasync.audio.ui.theme.SolarAmber
import com.lasync.audio.ui.theme.TextMuted
import com.lasync.audio.ui.theme.TextPrimary
import com.lasync.audio.ui.theme.TextSecondary
import com.lasync.audio.ui.theme.Ultraviolet
import com.lasync.audio.ui.theme.VoidBackground

@Composable
fun HostScreen(
    engine: HostNetworkEngine,
    hotspotManager: AutonomousHotspotManager? = null,
    onRequestMediaProjection: ((MediaProjection?) -> Unit) -> Unit,
    onStopSession: () -> Unit,
    onBack: () -> Unit = onStopSession,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by engine.uiState.collectAsState()
    var showQrDialog by remember { mutableStateOf(false) }

    val hotspotPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it } ||
                permissions[Manifest.permission.NEARBY_WIFI_DEVICES] == true ||
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) {
            hotspotManager?.startAutonomousHotspot(
                onStarted = { info ->
                    engine.setHotspotInfo(info)
                    showQrDialog = true
                },
                onError = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                }
            )
        } else {
            Toast.makeText(context, "Location or Nearby Wi-Fi permission required for Hotspot", Toast.LENGTH_LONG).show()
        }
    }

    fun launchAutonomousHotspot() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (needed.isEmpty()) {
            hotspotManager?.startAutonomousHotspot(
                onStarted = { info ->
                    engine.setHotspotInfo(info)
                    showQrDialog = true
                },
                onError = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                }
            )
        } else {
            hotspotPermissionsLauncher.launch(needed.toTypedArray())
        }
    }

    if (showQrDialog && state.hotspotInfo != null) {
        HotspotQrDialog(
            hotspotInfo = state.hotspotInfo!!,
            onStopHotspot = {
                hotspotManager?.stopAutonomousHotspot()
                engine.setHotspotInfo(null)
                showQrDialog = false
            },
            onDismiss = { showQrDialog = false }
        )
    }

    val jukeboxPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            scope.launch {
                engine.loadJukeboxFile(context, uri)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBackground)
            .padding(20.dp)
    ) {
        LaSyncTopBar(
            title = "LaSync",
            subtitle = "Master Host • ${state.sessionId}",
            tag = "HOST",
            onBackClick = onBack,
            onCloseClick = onStopSession,
            onLogoPressStart = { engine.startMegaphone(context) },
            onLogoPressEnd = { engine.stopMegaphone() },
            isMegaphoneActive = state.isMegaphoneActive
        )

        Spacer(modifier = Modifier.height(14.dp))

        MasterSocketCard(localIp = state.localIp)

        Spacer(modifier = Modifier.height(10.dp))

        AutonomousHotspotCard(
            isHotspotActive = state.isHotspotActive,
            hotspotInfo = state.hotspotInfo,
            onStartHotspot = { launchAutonomousHotspot() },
            onShowQr = { showQrDialog = true },
            onStopHotspot = {
                hotspotManager?.stopAutonomousHotspot()
                engine.setHotspotInfo(null)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        SoundboardConsoleHeader(
            activeCount = state.receivers.size,
            isCalibrating = state.isCalibrating,
            onAutoCalibrate = { engine.triggerAcousticCalibration() }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (state.receivers.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    LaSyncRadar(size = 170.dp, tint = HyperCyan)
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Broadcasting Beacon on LAN",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Open LaSync on listener phones and tap JOIN",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = state.receivers,
                    key = { it.id }
                ) { receiver ->
                    ReceiverItemCard(
                        receiver = receiver,
                        onAdjustTrim = { delta -> engine.adjustReceiverDelayTrim(receiver.id, delta) },
                        onResetTrim = { engine.setReceiverDelayTrim(receiver.id, 0) },
                        onAdjustVolume = { delta -> engine.adjustReceiverVolume(receiver.id, delta) },
                        onToggleMute = { engine.toggleReceiverMute(receiver.id) },
                        onToggleSolo = { engine.toggleReceiverSolo(receiver.id) },
                        onDisconnect = { engine.disconnectReceiver(receiver.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TruePhaseBufferProfileCard(
            currentMode = state.bufferMode,
            onSelectMode = { engine.setBufferMode(it) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        MultiSourceAudioController(
            state = state,
            engine = engine,
            onRequestMediaProjection = onRequestMediaProjection,
            onPickJukeboxFile = { jukeboxPickerLauncher.launch("audio/*") }
        )

        Spacer(modifier = Modifier.height(10.dp))

        GhostMegaphoneCard(
            isMegaphoneActive = state.isMegaphoneActive,
            onStart = { engine.startMegaphone(context) },
            onStop = { engine.stopMegaphone() }
        )

        Spacer(modifier = Modifier.height(8.dp))

        HostFooterTelemetry(pingsAnswered = state.totalPingsAnswered)
    }
}

@Composable
private fun MasterSocketCard(localIp: String) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CarbonCard)
            .border(1.dp, BorderNeon, RoundedCornerShape(16.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                clipboardManager.setText(AnnotatedString("$localIp:8888"))
                Toast.makeText(context, "Host address $localIp:8888 copied to clipboard", Toast.LENGTH_SHORT).show()
            }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MASTER TIMELINE SOCKET",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$localIp : 8888",
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LaSyncWaveEqualizer(tint = QuantumMint, maxHeight = 16.dp)
                Text(
                    text = "BROADCASTING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = QuantumMint
                )
            }
        }
    }
}

@Composable
private fun AutonomousHotspotCard(
    isHotspotActive: Boolean,
    hotspotInfo: HotspotInfo?,
    onStartHotspot: () -> Unit,
    onShowQr: () -> Unit,
    onStopHotspot: () -> Unit
) {
    if (isHotspotActive && hotspotInfo != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SolarAmber.copy(alpha = 0.12f))
                .border(1.5.dp, SolarAmber, RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LaSyncWaveEqualizer(tint = SolarAmber, maxHeight = 14.dp)
                        Text(
                            text = "AUTONOMOUS HOTSPOT ACTIVE",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = SolarAmber,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SolarAmber.copy(alpha = 0.2f))
                            .border(1.dp, SolarAmber.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ZERO-ROUTER",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = SolarAmber
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SSID: ${hotspotInfo.ssid}",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (hotspotInfo.passphrase.isNotBlank()) "Pass: ${hotspotInfo.passphrase}" else "Open Network (No Pass)",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SolarAmber)
                                .clickable { onShowQr() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = VoidBackground,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "QR CODE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VoidBackground
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CrimsonPulse.copy(alpha = 0.2f))
                                .border(1.dp, CrimsonPulse.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable { onStopHotspot() }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "STOP",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CrimsonPulse
                            )
                        }
                    }
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CarbonCard)
                .border(1.dp, SolarAmber.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .clickable { onStartHotspot() }
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(SolarAmber.copy(alpha = 0.15f))
                            .border(1.dp, SolarAmber.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = SolarAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "NO ROUTER? START AUTONOMOUS HOTSPOT",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = SolarAmber,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Zero-router sync for outdoors & camping • QR connect",
                            fontSize = 10.sp,
                            color = TextMuted,
                            maxLines = 1
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SolarAmber)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "START",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = VoidBackground
                    )
                }
            }
        }
    }
}

@Composable
private fun SoundboardConsoleHeader(
    activeCount: Int,
    isCalibrating: Boolean = false,
    onAutoCalibrate: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = "Listeners",
                tint = HyperCyan,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "SOUNDBOARD MIXER CONSOLE",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextPrimary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (activeCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCalibrating) SolarAmber.copy(alpha = 0.2f) else Ultraviolet.copy(alpha = 0.15f))
                        .border(1.dp, if (isCalibrating) SolarAmber else Ultraviolet, RoundedCornerShape(8.dp))
                        .clickable(enabled = !isCalibrating) { onAutoCalibrate() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isCalibrating) {
                            LaSyncWaveEqualizer(tint = SolarAmber, maxHeight = 12.dp)
                            Text(
                                text = "CALIBRATING",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = SolarAmber
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Auto Calibrate",
                                tint = Ultraviolet,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "AUTO-CALIBRATE",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Ultraviolet
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(HyperCyan.copy(alpha = 0.12f))
                    .border(1.dp, HyperCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$activeCount ACTIVE",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = HyperCyan
                )
            }
        }
    }
}

@Composable
private fun TruePhaseBufferProfileCard(
    currentMode: SyncBufferMode,
    onSelectMode: (SyncBufferMode) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CarbonCard)
            .border(1.dp, BorderNeon, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRUEPHASE BUFFER PROFILE",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = "${currentMode.leadTimeMs}ms Lead • ${currentMode.title}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = when (currentMode) {
                        SyncBufferMode.ULTRA_LOW_LATENCY -> QuantumMint
                        SyncBufferMode.LOW_LATENCY_VIDEO -> HyperCyan
                        SyncBufferMode.ULTRA_STABLE_MUSIC -> SolarAmber
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val isParty = currentMode == SyncBufferMode.ULTRA_LOW_LATENCY
                val isVideo = currentMode == SyncBufferMode.LOW_LATENCY_VIDEO
                val isStable = currentMode == SyncBufferMode.ULTRA_STABLE_MUSIC

                BufferModeOption(
                    title = "⚡ Party Sync",
                    subtitle = "50ms Zero-Echo",
                    isSelected = isParty,
                    accentColor = QuantumMint,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectMode(SyncBufferMode.ULTRA_LOW_LATENCY) }
                )

                BufferModeOption(
                    title = "🎬 Video Lip",
                    subtitle = "100ms Sync",
                    isSelected = isVideo,
                    accentColor = HyperCyan,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectMode(SyncBufferMode.LOW_LATENCY_VIDEO) }
                )

                BufferModeOption(
                    title = "🛡️ Wi-Fi Guard",
                    subtitle = "300ms Cushion",
                    isSelected = isStable,
                    accentColor = SolarAmber,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectMode(SyncBufferMode.ULTRA_STABLE_MUSIC) }
                )
            }
        }
    }
}

@Composable
private fun BufferModeOption(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.15f) else CarbonCardHighlight)
            .border(1.dp, if (isSelected) accentColor else BorderNeon, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) accentColor else TextSecondary
            )
            Text(
                text = subtitle,
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun MultiSourceAudioController(
    state: HostUiState,
    engine: HostNetworkEngine,
    onRequestMediaProjection: ((MediaProjection?) -> Unit) -> Unit,
    onPickJukeboxFile: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CarbonCard)
            .border(
                1.dp,
                when (state.activeAudioSource) {
                    HostAudioSource.MEGAPHONE -> Brush.linearGradient(listOf(CrimsonPulse, SolarAmber))
                    HostAudioSource.SYSTEM_CAPTURE -> Brush.linearGradient(listOf(QuantumMint, HyperCyan))
                    HostAudioSource.SYNTHETIC_TONE -> Brush.linearGradient(listOf(CrimsonPulse, Ultraviolet))
                    HostAudioSource.RAM_JUKEBOX -> Brush.linearGradient(listOf(SolarAmber, HyperCyan))
                    HostAudioSource.IDLE -> Brush.linearGradient(listOf(HyperCyan.copy(alpha = 0.4f), BorderNeon))
                },
                RoundedCornerShape(20.dp)
            )
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (state.activeAudioSource) {
                HostAudioSource.MEGAPHONE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LaSyncWaveEqualizer(tint = CrimsonPulse, maxHeight = 18.dp)
                            Column {
                                Text(
                                    text = "GHOST MEGAPHONE ACTIVE",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = CrimsonPulse
                                )
                                Text(
                                    text = "Broadcasting live microphone @ 100% volume",
                                    fontSize = 9.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Text(
                            text = "${state.audioFramesSent} frames",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LaSyncButton(
                        text = "STOP MEGAPHONE",
                        icon = Icons.Default.Stop,
                        accentColor = CrimsonPulse,
                        onClick = { engine.stopMegaphone() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HostAudioSource.SYSTEM_CAPTURE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LaSyncWaveEqualizer(tint = QuantumMint, maxHeight = 18.dp)
                            Column {
                                Text(
                                    text = "SHARING SYSTEM MUSIC",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = QuantumMint
                                )
                                Text(
                                    text = "Spotify, YouTube, Games • ${state.bufferMode.leadTimeMs}ms Low-Lag Sync",
                                    fontSize = 9.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Text(
                            text = "${state.audioFramesSent} frames",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LaSyncButton(
                        text = "STOP MUSIC SHARING",
                        icon = Icons.Default.Stop,
                        accentColor = CrimsonPulse,
                        onClick = {
                            engine.stopSystemAudio()
                            com.lasync.audio.core.service.LaSyncForegroundService.stopMediaProjection(context)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HostAudioSource.SYNTHETIC_TONE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LaSyncWaveEqualizer(tint = HyperCyan, maxHeight = 18.dp)
                            Text(
                                text = "STREAMING 440 Hz TONE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = HyperCyan
                            )
                        }

                        Text(
                            text = "${state.audioFramesSent} frames",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LaSyncButton(
                        text = "STOP ACOUSTIC TEST",
                        icon = Icons.Default.Stop,
                        accentColor = CrimsonPulse,
                        onClick = { engine.stopToneTest() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HostAudioSource.RAM_JUKEBOX -> {
                    val track = state.loadedJukeboxTrack
                    val totalDurationMs = track?.durationMs ?: 0L
                    val currentPosMs = state.jukeboxPositionMs

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                LaSyncWaveEqualizer(tint = SolarAmber, maxHeight = 18.dp)
                                Column {
                                    Text(
                                        text = "RAM JUKEBOX • ZERO-TRAFFIC",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = SolarAmber,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = track?.title ?: "Local Audio Track",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SolarAmber.copy(alpha = 0.15f))
                                    .border(1.dp, SolarAmber.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${state.readyReceiversCount}/${state.receivers.size} READY",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = SolarAmber
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(currentPosMs),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = SolarAmber,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatTime(totalDurationMs),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Slider(
                            value = state.jukeboxProgress.coerceIn(0f, 1f),
                            onValueChange = { frac ->
                                val targetMs = (frac * totalDurationMs).toLong()
                                engine.seekJukebox(targetMs)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = SolarAmber,
                                activeTrackColor = SolarAmber,
                                inactiveTrackColor = CarbonCardHighlight
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (state.isJukeboxPlaying) {
                                LaSyncButton(
                                    text = "PAUSE",
                                    icon = Icons.Default.Pause,
                                    accentColor = SolarAmber,
                                    onClick = { engine.pauseJukebox() },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                LaSyncButton(
                                    text = "PLAY",
                                    icon = Icons.Default.PlayArrow,
                                    accentColor = QuantumMint,
                                    onClick = { engine.playJukebox(state.jukeboxPositionMs) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            LaSyncButton(
                                text = "STOP",
                                icon = Icons.Default.Stop,
                                accentColor = CrimsonPulse,
                                onClick = { engine.stopJukebox() },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                HostAudioSource.IDLE -> {
                    LaSyncButton(
                        text = "SHARE SYSTEM MUSIC",
                        icon = Icons.Default.MusicNote,
                        accentColor = QuantumMint,
                        onClick = {
                            onRequestMediaProjection { projection ->
                                if (projection != null) {
                                    engine.startSystemAudio(projection)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Streams all phone audio (Spotify, YouTube, Games) • Calls excluded",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LaSyncButton(
                        text = if (state.isJukeboxDecoding) "DECODING AUDIO FILE..." else "PLAY LOCAL FILE (RAM JUKEBOX)",
                        icon = Icons.Default.FolderOpen,
                        accentColor = SolarAmber,
                        onClick = {
                            if (!state.isJukeboxDecoding) {
                                onPickJukeboxFile()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (state.isJukeboxDecoding) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { state.jukeboxDecodeProgress },
                            color = SolarAmber,
                            trackColor = CarbonCardHighlight,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Decoding uncompressed 16-bit PCM: ${(state.jukeboxDecodeProgress * 100).toInt()}%",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = SolarAmber
                        )
                    } else if (state.loadedJukeboxTrack != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CarbonCardHighlight)
                                .border(1.dp, SolarAmber.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Preloaded: ${state.loadedJukeboxTrack!!.title}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SolarAmber,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${formatTime(state.loadedJukeboxTrack!!.durationMs)} • ${state.readyReceiversCount}/${state.receivers.size} listeners ready",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextMuted
                                    )
                                }
                                LaSyncButton(
                                    text = "PLAY",
                                    icon = Icons.Default.PlayArrow,
                                    accentColor = QuantumMint,
                                    onClick = { engine.playJukebox() },
                                    modifier = Modifier.height(34.dp)
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Streams MP3, FLAC, WAV, AAC from device RAM • ZERO Wi-Fi traffic",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LaSyncButton(
                        text = "START 440 Hz ACOUSTIC TEST",
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        accentColor = HyperCyan,
                        onClick = { engine.startToneTest() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun GhostMegaphoneCard(
    isMegaphoneActive: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isMegaphoneActive) CrimsonPulse.copy(alpha = 0.15f) else CarbonCard)
            .border(
                1.dp,
                if (isMegaphoneActive) CrimsonPulse else CrimsonPulse.copy(alpha = 0.35f),
                RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Megaphone",
                        tint = CrimsonPulse,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "GHOST MEGAPHONE • 1-WAY PA",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonPulse,
                        letterSpacing = 0.5.sp
                    )
                }

                if (isMegaphoneActive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LaSyncWaveEqualizer(tint = CrimsonPulse, maxHeight = 12.dp)
                        Text(
                            text = "LIVE @ 100% VOL",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonPulse
                        )
                    }
                } else {
                    Text(
                        text = "STEALTH PTT",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isMegaphoneActive) CrimsonPulse.copy(alpha = 0.3f)
                        else CarbonCardHighlight
                    )
                    .border(
                        1.dp,
                        if (isMegaphoneActive) CrimsonPulse else BorderNeon,
                        RoundedCornerShape(12.dp)
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onStart()
                                tryAwaitRelease()
                                onStop()
                            }
                        )
                    }
                    .padding(vertical = 12.dp, horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isMegaphoneActive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LaSyncWaveEqualizer(tint = CrimsonPulse, maxHeight = 18.dp)
                        Text(
                            text = "TRANSMITTING LIVE TO ALL PHONES",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = CrimsonPulse
                        )
                        LaSyncWaveEqualizer(tint = CrimsonPulse, maxHeight = 18.dp)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Hold to talk",
                                tint = CrimsonPulse,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "HOLD TO TALK (PUSH-TO-TALK)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Or hold glowing logo above • Forces 100% volume on all phones",
                            fontSize = 9.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HostFooterTelemetry(pingsAnswered: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Pings Answered: $pingsAnswered",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = TextMuted
        )

        Text(
            text = "LaSync Core Engine",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = TextMuted
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
}

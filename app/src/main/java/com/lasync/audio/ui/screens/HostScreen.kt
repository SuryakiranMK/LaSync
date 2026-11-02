package com.lasync.audio.ui.screens

import android.media.projection.MediaProjection
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
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.lasync.audio.core.network.HostNetworkEngine
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
    onRequestMediaProjection: ((MediaProjection?) -> Unit) -> Unit,
    onStopSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by engine.uiState.collectAsState()

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
        // Uniform Brand Top Bar with Stealth Megaphone Push-to-Talk on Logo
        LaSyncTopBar(
            title = "LaSync",
            subtitle = "Master Host • ${state.sessionId}",
            tag = "HOST",
            onCloseClick = onStopSession,
            onLogoPressStart = { engine.startMegaphone(context) },
            onLogoPressEnd = { engine.stopMegaphone() },
            isMegaphoneActive = state.isMegaphoneActive
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Session Information Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CarbonCard)
                .border(1.dp, BorderNeon, RoundedCornerShape(16.dp))
                .padding(16.dp)
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
                        text = "${state.localIp} : 8888",
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

        Spacer(modifier = Modifier.height(22.dp))

        // Listeners Header
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

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(HyperCyan.copy(alpha = 0.12f))
                    .border(1.dp, HyperCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${state.receivers.size} ACTIVE",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = HyperCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dynamic Receiver List or Animated Waiting Radar
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

                    Spacer(modifier = Modifier.height(22.dp))

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

        Spacer(modifier = Modifier.height(14.dp))

        // TruePhase Buffer Profile Selector (Ultra-Stable Music vs Low-Latency Video)
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
                        text = "${state.bufferMode.leadTimeMs}ms Lead • ${state.bufferMode.title}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = when (state.bufferMode) {
                            com.lasync.audio.core.audio.SyncBufferMode.ULTRA_LOW_LATENCY -> QuantumMint
                            com.lasync.audio.core.audio.SyncBufferMode.LOW_LATENCY_VIDEO -> HyperCyan
                            com.lasync.audio.core.audio.SyncBufferMode.ULTRA_STABLE_MUSIC -> SolarAmber
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isParty = state.bufferMode == com.lasync.audio.core.audio.SyncBufferMode.ULTRA_LOW_LATENCY
                    val isVideo = state.bufferMode == com.lasync.audio.core.audio.SyncBufferMode.LOW_LATENCY_VIDEO
                    val isStable = state.bufferMode == com.lasync.audio.core.audio.SyncBufferMode.ULTRA_STABLE_MUSIC

                    // Ultra-Low Party Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isParty) QuantumMint.copy(alpha = 0.15f) else CarbonCardHighlight)
                            .border(
                                1.dp,
                                if (isParty) QuantumMint else BorderNeon,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { engine.setBufferMode(com.lasync.audio.core.audio.SyncBufferMode.ULTRA_LOW_LATENCY) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "⚡ Party Sync",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isParty) QuantumMint else TextSecondary
                            )
                            Text(
                                text = "50ms Zero-Echo",
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        }
                    }

                    // Low-Latency Video Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isVideo) HyperCyan.copy(alpha = 0.15f) else CarbonCardHighlight)
                            .border(
                                1.dp,
                                if (isVideo) HyperCyan else BorderNeon,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { engine.setBufferMode(com.lasync.audio.core.audio.SyncBufferMode.LOW_LATENCY_VIDEO) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🎬 Video Lip",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isVideo) HyperCyan else TextSecondary
                            )
                            Text(
                                text = "100ms Sync",
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        }
                    }

                    // Ultra-Stable Music Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isStable) SolarAmber.copy(alpha = 0.15f) else CarbonCardHighlight)
                            .border(
                                1.dp,
                                if (isStable) SolarAmber else BorderNeon,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { engine.setBufferMode(com.lasync.audio.core.audio.SyncBufferMode.ULTRA_STABLE_MUSIC) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🛡️ Wi-Fi Guard",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isStable) SolarAmber else TextSecondary
                            )
                            Text(
                                text = "300ms Cushion",
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Multi-Source Audio Controller (System Music Capture + 440 Hz Benchmark)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CarbonCard)
                .border(
                    1.dp,
                    when (state.activeAudioSource) {
                        com.lasync.audio.core.audio.HostAudioSource.MEGAPHONE ->
                            Brush.linearGradient(listOf(CrimsonPulse, SolarAmber))
                        com.lasync.audio.core.audio.HostAudioSource.SYSTEM_CAPTURE ->
                            Brush.linearGradient(listOf(QuantumMint, HyperCyan))
                        com.lasync.audio.core.audio.HostAudioSource.SYNTHETIC_TONE ->
                            Brush.linearGradient(listOf(CrimsonPulse, Ultraviolet))
                        com.lasync.audio.core.audio.HostAudioSource.RAM_JUKEBOX ->
                            Brush.linearGradient(listOf(SolarAmber, HyperCyan))
                        com.lasync.audio.core.audio.HostAudioSource.IDLE ->
                            Brush.linearGradient(listOf(HyperCyan.copy(alpha = 0.4f), BorderNeon))
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
                    com.lasync.audio.core.audio.HostAudioSource.MEGAPHONE -> {
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

                    com.lasync.audio.core.audio.HostAudioSource.SYSTEM_CAPTURE -> {
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

                    com.lasync.audio.core.audio.HostAudioSource.SYNTHETIC_TONE -> {
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

                    com.lasync.audio.core.audio.HostAudioSource.RAM_JUKEBOX -> {
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

                            // Timeline Progress & Seeking
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

                            // Interactive Seek Slider
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

                            // Play / Pause / Stop Controls Row
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

                    com.lasync.audio.core.audio.HostAudioSource.IDLE -> {
                        // System Music Sharing Button (Hero feature)
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

                        // Offline RAM Jukebox File Picker
                        LaSyncButton(
                            text = if (state.isJukeboxDecoding) "DECODING AUDIO FILE..." else "PLAY LOCAL FILE (RAM JUKEBOX)",
                            icon = Icons.Default.FolderOpen,
                            accentColor = SolarAmber,
                            onClick = {
                                if (!state.isJukeboxDecoding) {
                                    jukeboxPickerLauncher.launch("audio/*")
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

                        // Benchmark 440 Hz Tone Test Button
                        LaSyncButton(
                            text = "START 440 Hz ACOUSTIC TEST",
                            icon = Icons.Default.VolumeUp,
                            accentColor = HyperCyan,
                            onClick = { engine.startToneTest() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Ghost Megaphone (Stealth 1-Way Walkie-Talkie / Room PA)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (state.isMegaphoneActive) CrimsonPulse.copy(alpha = 0.15f) else CarbonCard)
                .border(
                    1.dp,
                    if (state.isMegaphoneActive) CrimsonPulse else CrimsonPulse.copy(alpha = 0.35f),
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

                    if (state.isMegaphoneActive) {
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

                // Push-to-Talk Interactive Trigger Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (state.isMegaphoneActive) CrimsonPulse.copy(alpha = 0.3f)
                            else CarbonCardHighlight
                        )
                        .border(
                            1.dp,
                            if (state.isMegaphoneActive) CrimsonPulse else BorderNeon,
                            RoundedCornerShape(12.dp)
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    engine.startMegaphone(context)
                                    tryAwaitRelease()
                                    engine.stopMegaphone()
                                }
                            )
                        }
                        .padding(vertical = 12.dp, horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isMegaphoneActive) {
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

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Telemetry Summary
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pings Answered: ${state.totalPingsAnswered}",
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
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
}

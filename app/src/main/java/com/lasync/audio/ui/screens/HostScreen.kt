package com.lasync.audio.ui.screens

import android.media.projection.MediaProjection
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lasync.audio.core.network.HostNetworkEngine
import com.lasync.audio.ui.components.LaSyncButton
import com.lasync.audio.ui.components.LaSyncRadar
import com.lasync.audio.ui.components.LaSyncTopBar
import com.lasync.audio.ui.components.LaSyncWaveEqualizer
import com.lasync.audio.ui.components.ReceiverItemCard
import com.lasync.audio.ui.theme.BorderNeon
import com.lasync.audio.ui.theme.CarbonCard
import com.lasync.audio.ui.theme.CrimsonPulse
import com.lasync.audio.ui.theme.HyperCyan
import com.lasync.audio.ui.theme.QuantumMint
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
    val state by engine.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBackground)
            .padding(20.dp)
    ) {
        // Uniform Brand Top Bar
        LaSyncTopBar(
            title = "LaSync",
            subtitle = "Master Host • ${state.sessionId}",
            tag = "HOST",
            onCloseClick = onStopSession
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
                    text = "SYNCHRONIZED LISTENERS",
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
                    ReceiverItemCard(receiver = receiver)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Multi-Source Audio Controller (System Music Capture + 440 Hz Benchmark)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CarbonCard)
                .border(
                    1.dp,
                    when (state.activeAudioSource) {
                        com.lasync.audio.core.audio.HostAudioSource.SYSTEM_CAPTURE ->
                            Brush.linearGradient(listOf(QuantumMint, HyperCyan))
                        com.lasync.audio.core.audio.HostAudioSource.SYNTHETIC_TONE ->
                            Brush.linearGradient(listOf(CrimsonPulse, Ultraviolet))
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
                                        text = "Spotify, YouTube, Games (All audio except calls)",
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

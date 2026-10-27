package com.localsync.audio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localsync.audio.core.network.DiscoveredHost
import com.localsync.audio.core.network.ReceiverNetworkEngine
import com.localsync.audio.core.network.ReceiverState
import com.localsync.audio.ui.components.LiveMetricBadge
import com.localsync.audio.ui.components.PulseRadar
import com.localsync.audio.ui.theme.AmberWarning
import com.localsync.audio.ui.theme.CyanGlow
import com.localsync.audio.ui.theme.DarkBackground
import com.localsync.audio.ui.theme.EmeraldGlow
import com.localsync.audio.ui.theme.RoseError
import com.localsync.audio.ui.theme.SurfaceBorder
import com.localsync.audio.ui.theme.SurfaceCard
import com.localsync.audio.ui.theme.TextMuted
import com.localsync.audio.ui.theme.TextPrimary
import com.localsync.audio.ui.theme.TextSecondary
import com.localsync.audio.ui.theme.VioletAccent
import java.util.Locale

@Composable
fun ReceiverScreen(
    engine: ReceiverNetworkEngine,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by engine.uiState.collectAsState()
    var manualIp by remember { mutableStateOf("") }
    var manualSession by remember { mutableStateOf("") }
    var showManualDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(20.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "RECEIVER MODE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = VioletAccent,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (state.state == ReceiverState.CONNECTED) state.sessionId else "SCANNING LAN",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
            }

            IconButton(
                onClick = onLeave,
                modifier = Modifier
                    .background(SurfaceCard, CircleShape)
                    .border(1.dp, SurfaceBorder, CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Leave",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        when (state.state) {
            ReceiverState.IDLE, ReceiverState.DISCOVERING, ReceiverState.CONNECTING -> {
                // Discovery State
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.45f),
                        contentAlignment = Alignment.Center
                    ) {
                        PulseRadar(
                            size = 170.dp,
                            tint = VioletAccent,
                            isActive = state.state == ReceiverState.DISCOVERING
                        )
                    }

                    Text(
                        text = if (state.state == ReceiverState.CONNECTING) "Negotiating Session..." else "Searching for Active Host...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Make sure the Host phone is on the same Wi-Fi router",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Discovered Hosts List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.55f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = state.discoveredHosts,
                            key = { it.sessionId }
                        ) { host ->
                            DiscoveredHostCard(
                                host = host,
                                onConnect = { engine.joinSession(host) }
                            )
                        }
                    }
                }
            }

            ReceiverState.CONNECTED -> {
                // Connected & Synchronizing State
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Connected Host Info Badge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceCard, RoundedCornerShape(16.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(VioletAccent.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = "Connected",
                                    tint = VioletAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = state.connectedHostName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Locked to Master Timeline",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = EmeraldGlow
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(EmeraldGlow, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Main Clock Synchronization Dial
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceCard, RoundedCornerShape(24.dp))
                            .border(1.5.dp, VioletAccent.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                            .padding(vertical = 32.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "ESTIMATED CLOCK OFFSET",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = String.format(Locale.US, "%+.2f ms", state.offsetMs),
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = CyanGlow
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (state.offsetMs >= 0) "Local clock ahead of host" else "Local clock behind host",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Real-Time Telemetry Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LiveMetricBadge(
                            label = "Round Trip",
                            value = String.format(Locale.US, "%.1f", state.rttMs),
                            unit = "ms",
                            statusColor = if (state.rttMs < 10f) EmeraldGlow else AmberWarning,
                            modifier = Modifier.weight(1f)
                        )
                        LiveMetricBadge(
                            label = "Jitter",
                            value = String.format(Locale.US, "%.1f", state.jitterMs),
                            unit = "ms",
                            modifier = Modifier.weight(1f)
                        )
                        LiveMetricBadge(
                            label = "Packet Loss",
                            value = String.format(Locale.US, "%.1f", state.packetLossPercent),
                            unit = "%",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Disconnect Button
                    Button(
                        onClick = { engine.disconnect() },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
                    ) {
                        Text(
                            text = "DISCONNECT FROM SESSION",
                            color = RoseError,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Bottom Telemetry Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Pings Exchanged: ${state.pingsCount}",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
            Text(
                text = "Sub-ms NTP Engine",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun DiscoveredHostCard(
    host: DiscoveredHost,
    onConnect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, VioletAccent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable(onClick = onConnect)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = host.hostName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Session: ${host.sessionId} • ${host.ipAddress}",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )
            }

            Button(
                onClick = onConnect,
                colors = ButtonDefaults.buttonColors(containerColor = VioletAccent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "JOIN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = DarkBackground
                )
            }
        }
    }
}

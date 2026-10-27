package com.lasync.audio.ui.screens

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
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lasync.audio.core.network.DiscoveredHost
import com.lasync.audio.core.network.ReceiverNetworkEngine
import com.lasync.audio.core.network.ReceiverState
import com.lasync.audio.ui.components.LaSyncButton
import com.lasync.audio.ui.components.LaSyncMetricCard
import com.lasync.audio.ui.components.LaSyncRadar
import com.lasync.audio.ui.components.LaSyncTopBar
import com.lasync.audio.ui.components.LaSyncWaveEqualizer
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
import java.util.Locale

@Composable
fun ReceiverScreen(
    engine: ReceiverNetworkEngine,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            subtitle = if (state.state == ReceiverState.CONNECTED) "Connected • ${state.sessionId}" else "Searching LAN",
            tag = "LISTENER",
            onCloseClick = onLeave
        )

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
                        LaSyncRadar(
                            size = 170.dp,
                            tint = Ultraviolet,
                            isActive = state.state == ReceiverState.DISCOVERING
                        )
                    }

                    Text(
                        text = if (state.state == ReceiverState.CONNECTING) "Negotiating Timeline..." else "Scanning for Active LaSync Host...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Ensure the Host phone is active on the same Wi-Fi router",
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
                            DiscoveredHostItem(
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
                            .clip(RoundedCornerShape(16.dp))
                            .background(CarbonCard)
                            .border(1.dp, BorderNeon, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Ultraviolet.copy(alpha = 0.15f))
                                    .border(1.dp, Ultraviolet.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = "Connected",
                                    tint = Ultraviolet,
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
                                    text = "Phase-Locked to Master Timeline",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = QuantumMint
                                )
                            }

                            LaSyncWaveEqualizer(tint = HyperCyan, maxHeight = 18.dp)
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Hero Clock Synchronization Dial
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(CarbonCard)
                            .border(
                                1.5.dp,
                                Brush.linearGradient(listOf(HyperCyan, Ultraviolet)),
                                RoundedCornerShape(24.dp)
                            )
                            .padding(vertical = 30.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "ESTIMATED CLOCK OFFSET",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = String.format(Locale.US, "%+.2f ms", state.offsetMs),
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = HyperCyan
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (state.offsetMs >= 0) "Local clock ahead of host" else "Local clock behind host",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Real-Time Telemetry Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LaSyncMetricCard(
                            label = "Round Trip",
                            value = String.format(Locale.US, "%.1f", state.rttMs),
                            unit = "ms",
                            statusColor = if (state.rttMs < 10f) QuantumMint else SolarAmber,
                            modifier = Modifier.weight(1f)
                        )
                        LaSyncMetricCard(
                            label = "Jitter",
                            value = String.format(Locale.US, "%.1f", state.jitterMs),
                            unit = "ms",
                            modifier = Modifier.weight(1f)
                        )
                        LaSyncMetricCard(
                            label = "Packet Loss",
                            value = String.format(Locale.US, "%.1f", state.packetLossPercent),
                            unit = "%",
                            statusColor = if (state.packetLossPercent > 2f) SolarAmber else QuantumMint,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Disconnect Button using LaSyncButton
                    LaSyncButton(
                        text = "DISCONNECT FROM SESSION",
                        onClick = { engine.disconnect() },
                        isSecondary = true,
                        modifier = Modifier.fillMaxWidth()
                    )
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
private fun DiscoveredHostItem(
    host: DiscoveredHost,
    onConnect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CarbonCard)
            .border(1.dp, Ultraviolet.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
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
                colors = ButtonDefaults.buttonColors(containerColor = Ultraviolet),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "JOIN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = VoidBackground
                )
            }
        }
    }
}

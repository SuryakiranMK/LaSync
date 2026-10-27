package com.localsync.audio.ui.screens

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
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localsync.audio.core.network.HostNetworkEngine
import com.localsync.audio.ui.components.LaSyncRadar
import com.localsync.audio.ui.components.LaSyncTopBar
import com.localsync.audio.ui.components.LaSyncWaveEqualizer
import com.localsync.audio.ui.components.ReceiverItemCard
import com.localsync.audio.ui.theme.BorderNeon
import com.localsync.audio.ui.theme.CarbonCard
import com.localsync.audio.ui.theme.HyperCyan
import com.localsync.audio.ui.theme.QuantumMint
import com.localsync.audio.ui.theme.TextMuted
import com.localsync.audio.ui.theme.TextPrimary
import com.localsync.audio.ui.theme.TextSecondary
import com.localsync.audio.ui.theme.Ultraviolet
import com.localsync.audio.ui.theme.VoidBackground

@Composable
fun HostScreen(
    engine: HostNetworkEngine,
    onStopSession: () -> Unit,
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

        Spacer(modifier = Modifier.height(12.dp))

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

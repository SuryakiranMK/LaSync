package com.lasync.audio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lasync.audio.core.sync.ReceiverNode
import com.lasync.audio.ui.theme.BorderNeon
import com.lasync.audio.ui.theme.CarbonCard
import com.lasync.audio.ui.theme.CrimsonPulse
import com.lasync.audio.ui.theme.HyperCyan
import com.lasync.audio.ui.theme.QuantumMint
import com.lasync.audio.ui.theme.SolarAmber
import com.lasync.audio.ui.theme.TextMuted
import com.lasync.audio.ui.theme.TextPrimary
import com.lasync.audio.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun ReceiverItemCard(
    receiver: ReceiverNode,
    modifier: Modifier = Modifier
) {
    val rttStatusColor = when {
        receiver.rttMs <= 0f -> TextMuted
        receiver.rttMs < 8f -> QuantumMint
        receiver.rttMs < 20f -> SolarAmber
        else -> CrimsonPulse
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CarbonCard)
            .border(1.dp, BorderNeon, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(HyperCyan.copy(alpha = 0.12f))
                        .border(1.dp, HyperCyan.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = "Device",
                        tint = HyperCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = receiver.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${receiver.ipAddress} • Port ${receiver.port}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }

                // Live Sync Status Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(QuantumMint, CircleShape)
                    )
                    Text(
                        text = if (receiver.isSynchronized) "PHASE-LOCKED" else "ONLINE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = QuantumMint
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Telemetry Metrics Row using uniform LaSyncMetricCard
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LaSyncMetricCard(
                    label = "RTT",
                    value = String.format(Locale.US, "%.1f", receiver.rttMs),
                    unit = "ms",
                    statusColor = rttStatusColor,
                    modifier = Modifier.weight(1f)
                )
                LaSyncMetricCard(
                    label = "Jitter",
                    value = String.format(Locale.US, "%.1f", receiver.jitterMs),
                    unit = "ms",
                    modifier = Modifier.weight(1f)
                )
                val (offsetVal, offsetUnit) = com.lasync.audio.core.network.NetworkUtils.formatClockOffset(receiver.offsetMs)
                LaSyncMetricCard(
                    label = "Offset",
                    value = offsetVal,
                    unit = offsetUnit,
                    modifier = Modifier.weight(1f)
                )
                LaSyncMetricCard(
                    label = "Loss",
                    value = String.format(Locale.US, "%.1f", receiver.packetLossPercent),
                    unit = "%",
                    statusColor = if (receiver.packetLossPercent > 2f) SolarAmber else QuantumMint,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

package com.localsync.audio.ui.components

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localsync.audio.core.sync.ReceiverNode
import com.localsync.audio.ui.theme.AmberWarning
import com.localsync.audio.ui.theme.CyanGlow
import com.localsync.audio.ui.theme.EmeraldGlow
import com.localsync.audio.ui.theme.RoseError
import com.localsync.audio.ui.theme.SurfaceBorder
import com.localsync.audio.ui.theme.SurfaceCard
import com.localsync.audio.ui.theme.TextMuted
import com.localsync.audio.ui.theme.TextPrimary
import com.localsync.audio.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun ReceiverItemCard(
    receiver: ReceiverNode,
    modifier: Modifier = Modifier
) {
    val rttStatusColor = when {
        receiver.rttMs <= 0f -> TextMuted
        receiver.rttMs < 8f -> EmeraldGlow
        receiver.rttMs < 20f -> AmberWarning
        else -> RoseError
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceCard, RoundedCornerShape(16.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
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
                        .background(CyanGlow.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = "Device",
                        tint = CyanGlow,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = receiver.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${receiver.ipAddress} • Port ${receiver.port}",
                        fontSize = 12.sp,
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
                            .background(EmeraldGlow, CircleShape)
                    )
                    Text(
                        text = if (receiver.isSynchronized) "SYNCED" else "ONLINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = EmeraldGlow
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Telemetry Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiveMetricBadge(
                    label = "RTT",
                    value = String.format(Locale.US, "%.1f", receiver.rttMs),
                    unit = "ms",
                    statusColor = rttStatusColor,
                    modifier = Modifier.weight(1f)
                )
                LiveMetricBadge(
                    label = "Jitter",
                    value = String.format(Locale.US, "%.1f", receiver.jitterMs),
                    unit = "ms",
                    modifier = Modifier.weight(1f)
                )
                LiveMetricBadge(
                    label = "Offset",
                    value = String.format(Locale.US, "%+.1f", receiver.offsetMs),
                    unit = "ms",
                    modifier = Modifier.weight(1f)
                )
                LiveMetricBadge(
                    label = "Loss",
                    value = String.format(Locale.US, "%.1f", receiver.packetLossPercent),
                    unit = "%",
                    statusColor = if (receiver.packetLossPercent > 2f) AmberWarning else EmeraldGlow,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

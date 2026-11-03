package com.lasync.audio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lasync.audio.ui.theme.BorderNeon
import com.lasync.audio.ui.theme.CarbonCard
import com.lasync.audio.ui.theme.CarbonCardHighlight
import com.lasync.audio.ui.theme.CrimsonPulse
import com.lasync.audio.ui.theme.HyperCyan
import com.lasync.audio.ui.theme.QuantumMint
import com.lasync.audio.ui.theme.TextMuted
import com.lasync.audio.ui.theme.TextPrimary
import com.lasync.audio.ui.theme.TextSecondary
import com.lasync.audio.ui.theme.Ultraviolet
import com.lasync.audio.ui.theme.VoidBackground

@Composable
fun SessionExitDialog(
    isHost: Boolean,
    connectedCount: Int = 0,
    connectedHostName: String = "",
    onRunInBackground: () -> Unit,
    onEndSession: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CarbonCard)
                .border(
                    1.5.dp,
                    Brush.linearGradient(listOf(HyperCyan.copy(alpha = 0.6f), Ultraviolet)),
                    RoundedCornerShape(24.dp)
                )
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(HyperCyan.copy(alpha = 0.15f))
                        .border(1.dp, HyperCyan.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "Active Session",
                        tint = HyperCyan,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isHost) "ACTIVE HOST BROADCAST" else "ACTIVE LISTENER CONNECTION",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.8.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isHost) {
                        if (connectedCount > 0) {
                            "You are broadcasting live audio to $connectedCount listener(s)."
                        } else {
                            "Host beacon is active on the local Wi-Fi network."
                        }
                    } else {
                        "You are synchronized with $connectedHostName."
                    },
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Option 1: Run in background
                ExitActionCard(
                    title = "RUN IN BACKGROUND",
                    subtitle = "Keeps audio stream and timeline sync active in notification drawer",
                    icon = Icons.Default.Minimize,
                    accentColor = HyperCyan,
                    onClick = onRunInBackground
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Option 2: Stop and disconnect
                ExitActionCard(
                    title = if (isHost) "STOP BROADCAST & CLOSE" else "DISCONNECT & LEAVE",
                    subtitle = if (isHost) "Disconnects all listeners and shuts down master stream" else "Leaves session and closes audio receiver",
                    icon = Icons.Default.Stop,
                    accentColor = CrimsonPulse,
                    onClick = onEndSession
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Cancel / Stay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CANCEL / STAY IN APP",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun ExitActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CarbonCardHighlight)
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp,
                    color = accentColor
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = TextMuted,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

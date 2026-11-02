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
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lasync.audio.core.audio.AudioOutputHardware
import com.lasync.audio.core.sync.ReceiverNode
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
import java.util.Locale

/**
 * SoundBoard Studio Channel Strip:
 * Renders an interactive mixer strip for each connected receiver phone.
 * Features:
 * 1. Hardware audio routing badge (🎧 Bluetooth vs 🔊 Phone Speaker).
 * 2. Real-time RTT / Jitter / Offset / Loss telemetry.
 * 3. Per-device microsecond Delay Trim (+/- ms) to tune out room echoes & Bluetooth delay.
 * 4. Remote gain volume fader steppers, MUTE, SOLO, and DROP controls.
 */
@Composable
fun ReceiverItemCard(
    receiver: ReceiverNode,
    onAdjustTrim: (deltaMs: Int) -> Unit = {},
    onResetTrim: () -> Unit = {},
    onAdjustVolume: (deltaPercent: Int) -> Unit = {},
    onToggleMute: () -> Unit = {},
    onToggleSolo: () -> Unit = {},
    onDisconnect: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val rttStatusColor = when {
        receiver.rttMs <= 0f -> TextMuted
        receiver.rttMs < 8f -> QuantumMint
        receiver.rttMs < 20f -> SolarAmber
        else -> CrimsonPulse
    }

    val (hwLabel, hwColor, hwIcon) = when (receiver.outputHardware) {
        AudioOutputHardware.BLUETOOTH -> Triple("BT AUDIO", Ultraviolet, Icons.Default.Headphones)
        AudioOutputHardware.WIRED -> Triple("WIRED 🎧", HyperCyan, Icons.Default.Headphones)
        AudioOutputHardware.USB -> Triple("USB DAC", HyperCyan, Icons.Default.Headphones)
        AudioOutputHardware.SPEAKER -> Triple("SPEAKER", QuantumMint, Icons.Default.VolumeUp)
        AudioOutputHardware.UNKNOWN -> Triple("SPEAKER", QuantumMint, Icons.Default.VolumeUp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (receiver.isSolo) SolarAmber.copy(alpha = 0.08f) else if (receiver.isMuted) CrimsonPulse.copy(alpha = 0.06f) else CarbonCard)
            .border(
                1.dp,
                if (receiver.isSolo) SolarAmber else if (receiver.isMuted) CrimsonPulse.copy(alpha = 0.6f) else BorderNeon,
                RoundedCornerShape(18.dp)
            )
            .padding(14.dp)
    ) {
        Column {
            // Header Row: Device icon, name, IP and Hardware Output Route Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(HyperCyan.copy(alpha = 0.12f))
                        .border(1.dp, HyperCyan.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = "Device",
                        tint = HyperCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = receiver.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        // Hardware Output Route Badge (e.g. 🎧 BT AUDIO vs 🔊 SPEAKER)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(hwColor.copy(alpha = 0.15f))
                                .border(1.dp, hwColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = hwIcon,
                                    contentDescription = hwLabel,
                                    tint = hwColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = hwLabel,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = hwColor
                                )
                            }
                        }
                    }

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
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(QuantumMint, CircleShape)
                    )
                    Text(
                        text = if (receiver.isSynchronized) "PHASE-LOCKED" else "ONLINE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = QuantumMint
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Telemetry Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
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

            Spacer(modifier = Modifier.height(10.dp))

            // SoundBoard Delay Trim Row (Room & Bluetooth Echo Compensator)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CarbonCardHighlight.copy(alpha = 0.5f))
                    .border(1.dp, BorderNeon, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "TRIM:",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Text(
                        text = "${if (receiver.delayTrimMs > 0) "+${receiver.delayTrimMs}" else "${receiver.delayTrimMs}"} ms",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        color = if (receiver.delayTrimMs != 0) SolarAmber else QuantumMint
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TrimButton("-10") { onAdjustTrim(-10) }
                    TrimButton("-5") { onAdjustTrim(-5) }
                    if (receiver.delayTrimMs != 0) {
                        TrimButton("RESET") { onResetTrim() }
                    }
                    TrimButton("+5") { onAdjustTrim(+5) }
                    TrimButton("+10") { onAdjustTrim(+10) }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mixer Volume & Mute / Solo / Drop Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Volume Steppers
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "VOL:",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Text(
                        text = "${receiver.volumePercent}%",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        color = if (receiver.isMuted) CrimsonPulse else HyperCyan
                    )
                    TrimButton("-10%") { onAdjustVolume(-10) }
                    TrimButton("+10%") { onAdjustVolume(+10) }
                }

                // Channel Strip Controls (Mute, Solo, Drop)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    ChannelActionButton(
                        label = if (receiver.isMuted) "MUTED" else "MUTE",
                        isActive = receiver.isMuted,
                        activeColor = CrimsonPulse,
                        onClick = onToggleMute
                    )
                    ChannelActionButton(
                        label = if (receiver.isSolo) "SOLO ON" else "SOLO",
                        isActive = receiver.isSolo,
                        activeColor = SolarAmber,
                        onClick = onToggleSolo
                    )
                    ChannelActionButton(
                        label = "DROP",
                        isActive = false,
                        activeColor = CrimsonPulse,
                        onClick = onDisconnect
                    )
                }
            }
        }
    }
}

@Composable
private fun TrimButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CarbonCardHighlight)
            .border(1.dp, BorderNeon, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )
    }
}

@Composable
private fun ChannelActionButton(
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) activeColor.copy(alpha = 0.25f) else CarbonCardHighlight)
            .border(1.dp, if (isActive) activeColor else BorderNeon, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            color = if (isActive) activeColor else TextSecondary
        )
    }
}

package com.localsync.audio.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lasync.audio.R
import com.localsync.audio.ui.theme.BorderNeon
import com.localsync.audio.ui.theme.CarbonCard
import com.localsync.audio.ui.theme.CarbonCardHighlight
import com.localsync.audio.ui.theme.CrimsonPulse
import com.localsync.audio.ui.theme.HyperCyan
import com.localsync.audio.ui.theme.HyperCyanGlow
import com.localsync.audio.ui.theme.LaSyncMotion
import com.localsync.audio.ui.theme.QuantumMint
import com.localsync.audio.ui.theme.QuantumMintGlow
import com.localsync.audio.ui.theme.SolarAmber
import com.localsync.audio.ui.theme.SolarAmberGlow
import com.localsync.audio.ui.theme.TextMuted
import com.localsync.audio.ui.theme.TextPrimary
import com.localsync.audio.ui.theme.TextSecondary
import com.localsync.audio.ui.theme.Ultraviolet
import com.localsync.audio.ui.theme.UltravioletGlow
import com.localsync.audio.ui.theme.VoidBackground

/**
 * 1. UNIFORM TOP BAR
 * Consistent branding across all screens: Glowing Logo, Letter-spaced Title, Status Pill & Action.
 */
@Composable
fun LaSyncTopBar(
    title: String,
    subtitle: String,
    tag: String? = null,
    onCloseClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // App Logo with glowing squircle aura
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CarbonCard)
                    .border(1.5.dp, Brush.linearGradient(listOf(HyperCyan, Ultraviolet)), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.lasync_logo),
                    contentDescription = "LaSync Logo",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 1.sp,
                        color = TextPrimary
                    )

                    if (tag != null) {
                        Box(
                            modifier = Modifier
                                .background(HyperCyanGlow, RoundedCornerShape(6.dp))
                                .border(1.dp, HyperCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tag,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = HyperCyan
                            )
                        }
                    }
                }

                Text(
                    text = subtitle.uppercase(),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp,
                    color = TextMuted
                )
            }
        }

        if (onCloseClick != null) {
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier
                    .size(38.dp)
                    .background(CarbonCardHighlight, CircleShape)
                    .border(1.dp, BorderNeon, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * 2. LIVING ACOUSTIC WAVE EQUALIZER
 * 5 animated visualizer bars that pulse smoothly to provide living acoustic feedback.
 */
@Composable
fun LaSyncWaveEqualizer(
    modifier: Modifier = Modifier,
    barCount: Int = 5,
    tint: Color = HyperCyan,
    maxHeight: Dp = 28.dp
) {
    val transition = rememberInfiniteTransition(label = "wave_equalizer")

    val h1 by transition.animateFloat(
        initialValue = 0.2f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(550, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h1"
    )
    val h2 by transition.animateFloat(
        initialValue = 0.7f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h2"
    )
    val h3 by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h3"
    )
    val h4 by transition.animateFloat(
        initialValue = 0.8f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(620, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h4"
    )
    val h5 by transition.animateFloat(
        initialValue = 0.4f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(510, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h5"
    )

    val heights = listOf(h1, h2, h3, h4, h5)

    Row(
        modifier = modifier.height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.take(barCount).forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(maxHeight * fraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(tint, Ultraviolet)
                        )
                    )
            )
        }
    }
}

/**
 * 3. CONCENTRIC ACOUSTIC RADAR RIPPLES
 * Concentric waves expanding outward in neon gradient.
 */
@Composable
fun LaSyncRadar(
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    tint: Color = HyperCyan,
    isActive: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "lasync_radar")

    val p1 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(LaSyncMotion.RADAR_PULSE_MS, easing = FastOutSlowInEasing)), label = "p1"
    )
    val p2 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(LaSyncMotion.RADAR_PULSE_MS, delayMillis = 800, easing = FastOutSlowInEasing)), label = "p2"
    )
    val p3 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(LaSyncMotion.RADAR_PULSE_MS, delayMillis = 1600, easing = FastOutSlowInEasing)), label = "p3"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (isActive) {
            Canvas(modifier = Modifier.size(size)) {
                val maxRadius = size.toPx() / 2f
                listOf(p1, p2, p3).forEach { progress ->
                    val radius = maxRadius * progress
                    val alpha = (1f - progress).coerceIn(0f, 1f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(tint.copy(alpha = alpha * 0.4f), Color.Transparent)
                        ),
                        radius = radius,
                        style = Stroke(width = 2.dp.toPx() * (1f - progress * 0.5f))
                    )
                }
            }
        }

        // Central Chronograph Diamond Hub
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CarbonCard)
                .border(1.5.dp, tint, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            LaSyncWaveEqualizer(tint = tint, maxHeight = 22.dp)
        }
    }
}

/**
 * 4. TACTILE BRAND BUTTON
 * Neo-tactile pill button with smooth scale spring and neon gradient.
 */
@Composable
fun LaSyncButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accentColor: Color = HyperCyan,
    isSecondary: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) 0.97f else 1.0f

    val backgroundBrush = if (isSecondary) {
        Brush.linearGradient(listOf(CarbonCard, CarbonCardHighlight))
    } else {
        Brush.linearGradient(listOf(accentColor, Ultraviolet))
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundBrush)
            .border(
                1.dp,
                if (isSecondary) BorderNeon else accentColor.copy(alpha = 0.6f),
                RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 16.dp, horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSecondary) TextPrimary else VoidBackground,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            Text(
                text = text.uppercase(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                color = if (isSecondary) TextPrimary else VoidBackground
            )
        }
    }
}

/**
 * 5. UNIFORM TELEMETRY METRIC PILL
 * Monospace, color-coded tabular readout for RTT, Jitter, Offset, and Loss.
 */
@Composable
fun LaSyncMetricCard(
    label: String,
    value: String,
    unit: String = "",
    statusColor: Color = QuantumMint,
    modifier: Modifier = Modifier
) {
    val bgGlow = when (statusColor) {
        QuantumMint -> QuantumMintGlow
        SolarAmber -> SolarAmberGlow
        else -> CarbonCardHighlight
    }

    Column(
        modifier = modifier
            .background(CarbonCard, RoundedCornerShape(14.dp))
            .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = TextMuted,
            letterSpacing = 0.6.sp
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = if (unit.isNotEmpty()) "$value $unit" else value,
            fontSize = 15.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (statusColor != QuantumMint) statusColor else TextPrimary
        )
    }
}

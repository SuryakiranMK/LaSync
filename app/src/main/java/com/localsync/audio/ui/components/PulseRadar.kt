package com.localsync.audio.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.localsync.audio.ui.theme.CyanGlow

@Composable
fun PulseRadar(
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    tint: Color = CyanGlow,
    isActive: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "pulse_radar")

    val pulse1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )

    val pulse2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse2"
    )

    val pulse3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse3"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (isActive) {
            Canvas(modifier = Modifier.size(size)) {
                val maxRadius = size.toPx() / 2f

                listOf(pulse1, pulse2, pulse3).forEach { progress ->
                    val radius = maxRadius * progress
                    val alpha = (1f - progress).coerceIn(0f, 1f)
                    drawCircle(
                        color = tint.copy(alpha = alpha * 0.5f),
                        radius = radius,
                        style = Stroke(width = 2.dp.toPx() * (1f - progress * 0.5f))
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.Default.WifiTethering,
            contentDescription = "Radar Beacon",
            tint = tint,
            modifier = Modifier.size(size * 0.28f)
        )
    }
}

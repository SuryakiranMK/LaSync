package com.localsync.audio.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

object LaSyncMotion {
    // Shared Easing Curves
    val SnappyEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val SmoothEasing = FastOutSlowInEasing

    // Spring Animations for Tactile Interactions
    val BouncySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    val SmoothSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    // Rhythm Constants
    const val RADAR_PULSE_MS = 2400
    const val EQUALIZER_CYCLE_MS = 800
    const val HEARTBEAT_PULSE_MS = 1000
    const val TRANSITION_SCREEN_MS = 320
}

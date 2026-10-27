package com.localsync.audio.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LaSyncColorScheme = darkColorScheme(
    primary = HyperCyan,
    onPrimary = VoidBackground,
    primaryContainer = HyperCyanGlow,
    onPrimaryContainer = HyperCyan,
    secondary = Ultraviolet,
    onSecondary = VoidBackground,
    secondaryContainer = UltravioletGlow,
    onSecondaryContainer = Ultraviolet,
    background = VoidBackground,
    onBackground = TextPrimary,
    surface = CarbonCard,
    onSurface = TextPrimary,
    surfaceVariant = CarbonCardHighlight,
    onSurfaceVariant = TextSecondary,
    outline = BorderNeon
)

@Composable
fun LaSyncTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = VoidBackground.toArgb()
            window.navigationBarColor = VoidBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = LaSyncColorScheme,
        typography = Typography,
        content = content
    )
}

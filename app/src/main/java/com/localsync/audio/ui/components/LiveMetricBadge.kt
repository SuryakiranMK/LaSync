package com.localsync.audio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localsync.audio.ui.theme.AmberSoft
import com.localsync.audio.ui.theme.AmberWarning
import com.localsync.audio.ui.theme.EmeraldGlow
import com.localsync.audio.ui.theme.EmeraldSoft
import com.localsync.audio.ui.theme.RoseError
import com.localsync.audio.ui.theme.SurfaceCard
import com.localsync.audio.ui.theme.TextMuted
import com.localsync.audio.ui.theme.TextPrimary

@Composable
fun LiveMetricBadge(
    label: String,
    value: String,
    unit: String = "",
    statusColor: Color = EmeraldGlow,
    modifier: Modifier = Modifier
) {
    val bgColor = when (statusColor) {
        EmeraldGlow -> EmeraldSoft
        AmberWarning -> AmberSoft
        else -> SurfaceCard
    }

    Column(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = TextMuted,
            letterSpacing = 0.5.sp
        )
        Text(
            text = if (unit.isNotEmpty()) "$value $unit" else value,
            fontSize = 15.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (statusColor != EmeraldGlow) statusColor else TextPrimary
        )
    }
}

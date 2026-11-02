package com.lasync.audio.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lasync.audio.core.updater.UpdateDownloader
import com.lasync.audio.core.updater.UpdateInfo
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
import kotlinx.coroutines.launch
import java.io.File

enum class UpdateUiState {
    IDLE,
    DOWNLOADING,
    READY_TO_INSTALL,
    FAILED
}

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var uiState by remember { mutableStateOf(UpdateUiState.IDLE) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedBytes by remember { mutableLongStateOf(0L) }
    var totalBytes by remember { mutableLongStateOf(0L) }
    var downloadedApkFile by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun startDownload() {
        uiState = UpdateUiState.DOWNLOADING
        downloadProgress = 0f
        downloadedBytes = 0L
        totalBytes = 0L
        errorMessage = null

        scope.launch {
            val file = UpdateDownloader.downloadApk(context, updateInfo.downloadUrl) { prog, read, total ->
                downloadProgress = prog
                downloadedBytes = read
                totalBytes = total
            }

            if (file != null && file.exists() && file.length() > 0) {
                downloadedApkFile = file
                uiState = UpdateUiState.READY_TO_INSTALL
            } else {
                uiState = UpdateUiState.FAILED
                errorMessage = "Failed to download update package. Please retry or open in browser."
            }
        }
    }

    Dialog(onDismissRequest = {
        if (uiState != UpdateUiState.DOWNLOADING) {
            onDismiss()
        }
    }) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CarbonCard)
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        when (uiState) {
                            UpdateUiState.DOWNLOADING -> listOf(HyperCyan, SolarAmber)
                            UpdateUiState.READY_TO_INSTALL -> listOf(QuantumMint, HyperCyan)
                            UpdateUiState.FAILED -> listOf(CrimsonPulse, SolarAmber)
                            UpdateUiState.IDLE -> listOf(HyperCyan, QuantumMint, Ultraviolet)
                        }
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top Header Row with Icon and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when (uiState) {
                                        UpdateUiState.DOWNLOADING -> HyperCyan.copy(alpha = 0.15f)
                                        UpdateUiState.READY_TO_INSTALL -> QuantumMint.copy(alpha = 0.15f)
                                        UpdateUiState.FAILED -> CrimsonPulse.copy(alpha = 0.15f)
                                        UpdateUiState.IDLE -> HyperCyan.copy(alpha = 0.15f)
                                    }
                                )
                                .border(
                                    1.dp,
                                    when (uiState) {
                                        UpdateUiState.DOWNLOADING -> HyperCyan.copy(alpha = 0.4f)
                                        UpdateUiState.READY_TO_INSTALL -> QuantumMint.copy(alpha = 0.4f)
                                        UpdateUiState.FAILED -> CrimsonPulse.copy(alpha = 0.4f)
                                        UpdateUiState.IDLE -> HyperCyan.copy(alpha = 0.4f)
                                    },
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (uiState) {
                                    UpdateUiState.READY_TO_INSTALL -> Icons.Default.CheckCircle
                                    UpdateUiState.DOWNLOADING -> Icons.Default.Download
                                    UpdateUiState.FAILED -> Icons.Default.Refresh
                                    UpdateUiState.IDLE -> Icons.Default.ArrowCircleDown
                                },
                                contentDescription = "Update Available",
                                tint = when (uiState) {
                                    UpdateUiState.READY_TO_INSTALL -> QuantumMint
                                    UpdateUiState.DOWNLOADING -> HyperCyan
                                    UpdateUiState.FAILED -> CrimsonPulse
                                    UpdateUiState.IDLE -> HyperCyan
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when (uiState) {
                                            UpdateUiState.DOWNLOADING -> HyperCyan.copy(alpha = 0.15f)
                                            UpdateUiState.READY_TO_INSTALL -> QuantumMint.copy(alpha = 0.15f)
                                            UpdateUiState.FAILED -> CrimsonPulse.copy(alpha = 0.15f)
                                            UpdateUiState.IDLE -> QuantumMint.copy(alpha = 0.15f)
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = when (uiState) {
                                        UpdateUiState.DOWNLOADING -> "DOWNLOADING UPDATE"
                                        UpdateUiState.READY_TO_INSTALL -> "DOWNLOAD COMPLETE"
                                        UpdateUiState.FAILED -> "DOWNLOAD FAILED"
                                        UpdateUiState.IDLE -> "NEW VERSION AVAILABLE"
                                    },
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = when (uiState) {
                                        UpdateUiState.DOWNLOADING -> HyperCyan
                                        UpdateUiState.READY_TO_INSTALL -> QuantumMint
                                        UpdateUiState.FAILED -> CrimsonPulse
                                        UpdateUiState.IDLE -> QuantumMint
                                    },
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Text(
                                text = updateInfo.versionName,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                        }
                    }

                    if (uiState != UpdateUiState.DOWNLOADING) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title
                Text(
                    text = updateInfo.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Release notes / Changelog container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CarbonCardHighlight)
                        .border(1.dp, BorderNeon, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = updateInfo.releaseNotes.ifBlank { "No changelog notes provided." },
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dynamic UI according to download state
                when (uiState) {
                    UpdateUiState.IDLE -> {
                        LaSyncButton(
                            text = "DOWNLOAD & UPDATE",
                            icon = Icons.Default.Download,
                            accentColor = HyperCyan,
                            onClick = { startDownload() },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onDismiss() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "SKIP FOR NOW",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    UpdateUiState.DOWNLOADING -> {
                        // In-App Downloading Animation Container
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(CarbonCardHighlight)
                                .border(1.dp, HyperCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        LaSyncWaveEqualizer(tint = HyperCyan, maxHeight = 16.dp)
                                        Text(
                                            text = "DOWNLOADING APK...",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = HyperCyan
                                        )
                                    }

                                    Text(
                                        text = "${(downloadProgress * 100).toInt()}%",
                                        fontSize = 15.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        color = HyperCyan
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                LinearProgressIndicator(
                                    progress = { downloadProgress },
                                    color = HyperCyan,
                                    trackColor = CarbonCard,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${UpdateDownloader.formatBytes(downloadedBytes)} / ${if (totalBytes > 0) UpdateDownloader.formatBytes(totalBytes) else "-- MB"}",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "Direct In-App Stream",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }

                    UpdateUiState.READY_TO_INSTALL -> {
                        // Success state: Install button
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(QuantumMint.copy(alpha = 0.12f))
                                    .border(1.dp, QuantumMint.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = QuantumMint,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "APK verified (${UpdateDownloader.formatBytes(downloadedApkFile?.length() ?: 0L)}). Ready to install.",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        color = QuantumMint
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LaSyncButton(
                                text = "INSTALL UPDATE",
                                icon = Icons.Default.CheckCircle,
                                accentColor = QuantumMint,
                                onClick = {
                                    downloadedApkFile?.let { apk ->
                                        UpdateDownloader.installApk(context, apk)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    UpdateUiState.FAILED -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = errorMessage ?: "Download failed.",
                                fontSize = 11.sp,
                                color = CrimsonPulse,
                                fontFamily = FontFamily.Monospace
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LaSyncButton(
                                    text = "RETRY",
                                    icon = Icons.Default.Refresh,
                                    accentColor = SolarAmber,
                                    onClick = { startDownload() },
                                    modifier = Modifier.weight(1f)
                                )

                                LaSyncButton(
                                    text = "BROWSER",
                                    icon = Icons.Default.OpenInBrowser,
                                    accentColor = HyperCyan,
                                    onClick = {
                                        try {
                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.htmlUrl))
                                            context.startActivity(browserIntent)
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onDismiss() }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "DISMISS",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMuted,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

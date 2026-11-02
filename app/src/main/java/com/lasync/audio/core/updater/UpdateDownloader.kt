package com.lasync.audio.core.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object UpdateDownloader {
    private const val TAG = "UpdateDownloader"

    /**
     * Downloads the APK file from GitHub release URL directly in-app,
     * following HTTP redirects and notifying real-time byte progress.
     */
    suspend fun downloadApk(
        context: Context,
        urlStr: String,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            var currentUrl = urlStr
            var redirects = 0
            val maxRedirects = 6

            // Follow HTTP redirects (GitHub 302 -> AWS S3 / Fastly CDN)
            while (redirects < maxRedirects) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "LaSync-Android-Updater")
                    setRequestProperty("Accept", "application/octet-stream")
                }

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == HttpURLConnection.HTTP_SEE_OTHER ||
                    status == 307 || status == 308
                ) {
                    val redirectUrl = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (!redirectUrl.isNullOrBlank()) {
                        currentUrl = redirectUrl
                        redirects++
                        continue
                    } else {
                        break
                    }
                } else if (status == HttpURLConnection.HTTP_OK) {
                    break
                } else {
                    Log.e(TAG, "HTTP error during APK download: $status")
                    return@withContext null
                }
            }

            val totalBytes = connection?.contentLengthLong ?: -1L
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updatesDir, "LaSync-update.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            inputStream = BufferedInputStream(connection!!.inputStream, 64 * 1024)
            outputStream = FileOutputStream(apkFile)

            val buffer = ByteArray(32 * 1024)
            var bytesRead: Int
            var totalRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                val progress = if (totalBytes > 0) {
                    (totalRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                onProgress(progress, totalRead, totalBytes)
            }

            outputStream.flush()
            onProgress(1.0f, totalRead, if (totalBytes > 0) totalBytes else totalRead)
            Log.i(TAG, "Downloaded update APK ($totalRead bytes) to ${apkFile.absolutePath}")
            apkFile
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading APK: ${e.message}", e)
            null
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
            try { outputStream?.close() } catch (_: Exception) {}
            connection?.disconnect()
        }
    }

    /**
     * Launches Android Package Installer to install the downloaded APK.
     */
    fun installApk(context: Context, apkFile: File): Boolean {
        return try {
            if (!apkFile.exists() || apkFile.length() <= 0) {
                Log.e(TAG, "APK file does not exist or is empty")
                return false
            }

            // Android 8.0+ (Oreo, API 26+) unknown sources check
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val permissionIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(permissionIntent)
                    // Continue to fire installer; user can grant and proceed
                }
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            }

            context.startActivity(installIntent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer: ${e.message}", e)
            false
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val mb = bytes / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f MB", mb)
    }
}

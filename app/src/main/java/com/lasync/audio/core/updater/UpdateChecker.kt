package com.lasync.audio.core.updater

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val title: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val htmlUrl: String
)

object UpdateChecker {
    private const val TAG = "UpdateChecker"
    private const val GITHUB_API_URL = "https://api.github.com/repos/SuryakiranMK/LaSync/releases/latest"

    /**
     * Checks GitHub Releases API for a newer version than currentVersion.
     * Returns UpdateInfo if a newer release exists, or null otherwise.
     */
    suspend fun checkForUpdate(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(GITHUB_API_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "LaSync-Android-App")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val jsonString = reader.use { it.readText() }
                return@withContext parseAndCheckRelease(jsonString, currentVersion)
            } else {
                Log.w(TAG, "GitHub API check returned HTTP $responseCode")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Update check failed (safe to ignore if offline): ${e.message}")
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Parses the GitHub release JSON and compares against currentVersion.
     */
    fun parseAndCheckRelease(jsonString: String, currentVersion: String): UpdateInfo? {
        try {
            val root = JSONObject(jsonString)
            val tagName = root.optString("tag_name", "").trim()
            val title = root.optString("name", "New Update Available")
            val body = root.optString("body", "A new version of LaSync is available.")
            val htmlUrl = root.optString("html_url", "https://github.com/SuryakiranMK/LaSync/releases/latest")

            var apkDownloadUrl = htmlUrl
            val assets = root.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkDownloadUrl = asset.optString("browser_download_url", apkDownloadUrl)
                        break
                    }
                }
            }

            val cleanRemote = cleanVersionString(tagName)
            val cleanCurrent = cleanVersionString(currentVersion)

            if (isNewerVersion(cleanRemote, cleanCurrent)) {
                return UpdateInfo(
                    versionName = tagName.ifEmpty { cleanRemote },
                    title = title,
                    releaseNotes = body,
                    downloadUrl = apkDownloadUrl,
                    htmlUrl = htmlUrl
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse release JSON: ${e.message}", e)
        }
        return null
    }

    fun cleanVersionString(v: String): String {
        return v.trim()
            .removePrefix("v")
            .removePrefix("V")
            .split("-")[0] // Strip pre-release suffix like "-hotfix"
            .trim()
    }

    /**
     * Compares two semantic version strings (e.g. "1.0.4" vs "1.0.3").
     * Returns true if remote is strictly newer than current.
     */
    fun isNewerVersion(remote: String, current: String): Boolean {
        try {
            val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
        } catch (_: Exception) {}
        return false
    }
}


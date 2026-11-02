package com.lasync.audio.core.jukebox

import android.util.Log
import com.lasync.audio.core.network.NetworkConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * JukeboxClientDownloader:
 * High-speed TCP client that connects to the Master Host on Port 8890,
 * downloads the uncompressed 16-bit PCM track into phone RAM in 1-2 seconds,
 * and reports progress to the UI.
 */
object JukeboxClientDownloader {
    private const val TAG = "JukeboxClientDownloader"

    suspend fun downloadTrackFromHost(
        hostIp: String,
        onProgress: (Float) -> Unit = {}
    ): JukeboxTrack? = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.tcpNoDelay = true
            socket.receiveBufferSize = 128 * 1024
            socket.connect(InetSocketAddress(hostIp, NetworkConstants.PORT_JUKEBOX), 4000)

            val rawIn = socket.getInputStream()
            val bufferedIn = BufferedInputStream(rawIn, 64 * 1024)
            val dataIn = DataInputStream(bufferedIn)

            val title = dataIn.readUTF()
            val artist = dataIn.readUTF()
            val durationMs = dataIn.readLong()
            val sampleRate = dataIn.readInt()
            val channelCount = dataIn.readInt()
            val totalBytes = dataIn.readInt()

            if (totalBytes <= 0 || totalBytes > 120 * 1024 * 1024) { // 120 MB ceiling sanity check
                Log.e(TAG, "Invalid PCM payload size from host: $totalBytes bytes")
                return@withContext null
            }

            val pcmData = ByteArray(totalBytes)
            var bytesReadTotal = 0
            val buffer = ByteArray(64 * 1024)

            while (bytesReadTotal < totalBytes) {
                val toRead = minOf(buffer.size, totalBytes - bytesReadTotal)
                val read = bufferedIn.read(buffer, 0, toRead)
                if (read < 0) break

                System.arraycopy(buffer, 0, pcmData, bytesReadTotal, read)
                bytesReadTotal += read

                val progress = (bytesReadTotal.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                onProgress(progress)
            }

            if (bytesReadTotal < totalBytes) {
                Log.w(TAG, "Transfer truncated: read $bytesReadTotal of $totalBytes bytes")
                return@withContext null
            }

            Log.i(TAG, "Downloaded '$title' ($bytesReadTotal bytes) successfully into RAM")

            JukeboxTrack(
                title = title,
                artist = artist,
                durationMs = durationMs,
                sampleRate = sampleRate,
                channelCount = channelCount,
                pcmBytes = pcmData
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download track from host: ${e.message}", e)
            null
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }
}


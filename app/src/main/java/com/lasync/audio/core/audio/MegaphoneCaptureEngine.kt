package com.lasync.audio.core.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * MegaphoneCaptureEngine:
 * Captures low-latency 48 kHz 16-bit mono PCM microphone audio for the Host Walkie-Talkie / PA feature.
 * Prioritizes VOICE_COMMUNICATION audio source for hardware echo cancellation, falling back to MIC.
 */
class MegaphoneCaptureEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "MegaphoneCaptureEngine"
    }

    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null

    @Volatile
    var isCapturing: Boolean = false
        private set

    @SuppressLint("MissingPermission")
    fun start(onFrameCaptured: (ByteArray) -> Unit): Boolean {
        if (isCapturing) return true

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "RECORD_AUDIO permission not granted")
            return false
        }

        val sampleRate = AudioConstants.SAMPLE_RATE
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val frameBytes = AudioConstants.BYTES_PER_FRAME_MONO // 960 bytes (10ms)

        val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBufSize <= 0) {
            Log.e(TAG, "Invalid AudioRecord minBufferSize: $minBufSize")
            return false
        }
        val bufferSize = maxOf(minBufSize * 2, frameBytes * 4)

        // Attempt 1: VOICE_COMMUNICATION (hardware acoustic echo cancellation)
        var record: AudioRecord? = null
        try {
            record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                record = null
            }
        } catch (e: Exception) {
            Log.w(TAG, "VOICE_COMMUNICATION source failed: ${e.message}, attempting MIC source")
            record = null
        }

        // Attempt 2: MIC source fallback
        if (record == null) {
            try {
                record = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
                if (record.state != AudioRecord.STATE_INITIALIZED) {
                    record.release()
                    Log.e(TAG, "Failed to initialize AudioRecord with MIC fallback")
                    return false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing AudioRecord with MIC: ${e.message}", e)
                return false
            }
        }

        audioRecord = record

        try {
            record.startRecording()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}", e)
            cleanup()
            return false
        }

        isCapturing = true

        captureJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(frameBytes)
            while (isActive && isCapturing) {
                val bytesRead = record.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                if (bytesRead > 0) {
                    onFrameCaptured(buffer.copyOf(bytesRead))
                } else if (bytesRead < 0) {
                    Log.e(TAG, "AudioRecord read error code: $bytesRead")
                    break
                }
            }
        }

        return true
    }

    fun stop() {
        if (!isCapturing) return
        isCapturing = false
        captureJob?.cancel()
        captureJob = null
        cleanup()
    }

    private fun cleanup() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning up AudioRecord: ${e.message}")
        }
        audioRecord = null
    }
}

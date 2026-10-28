package com.lasync.audio.core.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Captures live device audio output on Android 10+ (API 29+) using Google's AudioPlaybackCapture API.
 * Captures all system media, music (Spotify, YouTube, local players) and games.
 * Automatically excludes phone calls (VOICE_COMMUNICATION) per Android privacy requirements.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class AudioPlaybackCaptureEngine(
    private val scope: CoroutineScope,
    private val onFrameCaptured: (ByteArray) -> Unit
) {
    companion object {
        private const val TAG = "AudioCaptureEngine"
    }

    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startCapture(mediaProjection: MediaProjection) {
        if (_isCapturing.value) return

        try {
            val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

            val minBufferSize = AudioRecord.getMinBufferSize(
                AudioConstants.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioConstants.AUDIO_FORMAT
            )
            val bufferSize = maxOf(minBufferSize * 4, AudioConstants.BYTES_PER_FRAME_MONO * 20)

            val record = AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(config)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioConstants.AUDIO_FORMAT)
                        .setSampleRate(AudioConstants.SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()

            record.startRecording()
            audioRecord = record
            _isCapturing.value = true

            captureJob = scope.launch(Dispatchers.IO) {
                val frameBuffer = ByteArray(AudioConstants.BYTES_PER_FRAME_MONO)
                while (isActive && _isCapturing.value) {
                    var bytesRead = 0
                    while (bytesRead < frameBuffer.size && isActive) {
                        val read = record.read(frameBuffer, bytesRead, frameBuffer.size - bytesRead)
                        if (read > 0) {
                            bytesRead += read
                        } else {
                            break
                        }
                    }
                    if (bytesRead == frameBuffer.size) {
                        onFrameCaptured(frameBuffer.copyOf())
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AudioPlaybackCapture: ${e.message}", e)
            stopCapture()
        }
    }

    fun stopCapture() {
        _isCapturing.value = false
        captureJob?.cancel()
        captureJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }
}

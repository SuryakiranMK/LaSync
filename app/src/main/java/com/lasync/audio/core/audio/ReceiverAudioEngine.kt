package com.lasync.audio.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.SystemClock
import android.util.Log
import com.lasync.audio.core.sync.ClockSyncEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * High-Precision Receiver Audio Engine:
 * 1. Dedicated UDP packet listener feeding a thread-safe jitter queue.
 * 2. Dedicated playback loop using AudioTrack.WRITE_BLOCKING in native STEREO mode.
 * 3. Absorbs up to 200ms of Wi-Fi packet jitter, completely eliminating byte-truncation
 *    and buffer-underrun static ("rain noise").
 */
class ReceiverAudioEngine(
    private val scope: CoroutineScope,
    private val clockSyncEngine: ClockSyncEngine
) {
    companion object {
        private const val TAG = "ReceiverAudioEngine"
        private const val STREAM_TIMEOUT_MS = 800L
    }

    private val _isAudioPlaying = MutableStateFlow(false)
    val isAudioPlaying: StateFlow<Boolean> = _isAudioPlaying.asStateFlow()

    private val _framesReceived = MutableStateFlow(0L)
    val framesReceived: StateFlow<Long> = _framesReceived.asStateFlow()

    private var socketJob: Job? = null
    private var playerJob: Job? = null
    private var udpSocket: DatagramSocket? = null
    private var audioTrack: AudioTrack? = null

    private val jitterQueue = ConcurrentLinkedQueue<ByteArray>()

    @Volatile
    private var lastPacketTimeNs = 0L

    fun startListening() {
        if (socketJob != null) return

        jitterQueue.clear()
        _isAudioPlaying.value = false

        socketJob = scope.launch(Dispatchers.IO) {
            runReceiverLoop()
        }

        playerJob = scope.launch(Dispatchers.Default) {
            runPlaybackLoop()
        }
    }

    fun stopListening() {
        socketJob?.cancel()
        socketJob = null
        playerJob?.cancel()
        playerJob = null

        jitterQueue.clear()
        cleanupAudioTrack()

        try {
            udpSocket?.close()
        } catch (_: Exception) {}
        udpSocket = null

        _isAudioPlaying.value = false
    }

    private fun runReceiverLoop() {
        val buffer = ByteArray(2048) // Plenty of room for 990-byte packet

        try {
            udpSocket = DatagramSocket(AudioConstants.AUDIO_PORT).apply {
                receiveBufferSize = 256 * 1024 // 256 KB socket buffer
            }

            while (scope.isActive) {
                val packet = DatagramPacket(buffer, buffer.size)
                udpSocket?.receive(packet)

                val audioPacket = AudioFramePacket.decode(packet.data, packet.length)
                if (audioPacket != null && audioPacket.payload.isNotEmpty()) {
                    lastPacketTimeNs = SystemClock.elapsedRealtimeNanos()
                    _framesReceived.value++
                    jitterQueue.offer(audioPacket.payload)
                }
            }
        } catch (e: Exception) {
            if (scope.isActive) {
                Log.e(TAG, "Receiver UDP socket error: ${e.message}", e)
            }
        }
    }

    private suspend fun runPlaybackLoop() {
        setupAudioTrack()

        val silenceStereoFrame = ByteArray(AudioConstants.BYTES_PER_FRAME_STEREO) // 1920 bytes of zeros
        var isPlayingStarted = false

        while (scope.isActive) {
            val now = SystemClock.elapsedRealtimeNanos()
            val timeSinceLastPacketMs = if (lastPacketTimeNs > 0) (now - lastPacketTimeNs) / 1_000_000L else 0L

            if (!isPlayingStarted) {
                // Wait for the jitter buffer to build up 150-200ms of frames
                if (jitterQueue.size >= AudioConstants.JITTER_BUFFER_FRAMES) {
                    audioTrack?.play()
                    isPlayingStarted = true
                    _isAudioPlaying.value = true
                } else {
                    delay(10)
                    continue
                }
            }

            // Stream timed out: host stopped streaming
            if (timeSinceLastPacketMs > STREAM_TIMEOUT_MS && jitterQueue.isEmpty()) {
                if (isPlayingStarted) {
                    audioTrack?.pause()
                    audioTrack?.flush()
                    isPlayingStarted = false
                    _isAudioPlaying.value = false
                }
                delay(50)
                continue
            }

            val monoFrame = jitterQueue.poll()
            if (monoFrame != null) {
                val stereoFrame = AudioConstants.monoToStereo(monoFrame)
                // WRITE_BLOCKING ensures the full frame is written without sample truncation
                audioTrack?.write(stereoFrame, 0, stereoFrame.size, AudioTrack.WRITE_BLOCKING)
                _isAudioPlaying.value = true
            } else if (isPlayingStarted) {
                // Wi-Fi jitter dip: write silence frame to prevent DAC underrun pop
                audioTrack?.write(silenceStereoFrame, 0, silenceStereoFrame.size, AudioTrack.WRITE_BLOCKING)
            }
        }

        cleanupAudioTrack()
    }

    private fun setupAudioTrack() {
        val minBufferSize = AudioTrack.getMinBufferSize(
            AudioConstants.SAMPLE_RATE,
            AudioConstants.CHANNEL_CONFIG_OUT,
            AudioConstants.AUDIO_FORMAT
        )
        // 100ms * 10 = 1 full second buffer headroom
        val bufferSize = maxOf(minBufferSize * 4, AudioConstants.BYTES_PER_FRAME_STEREO * 50)

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val format = AudioFormat.Builder()
            .setSampleRate(AudioConstants.SAMPLE_RATE)
            .setChannelMask(AudioConstants.CHANNEL_CONFIG_OUT)
            .setEncoding(AudioConstants.AUDIO_FORMAT)
            .build()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
    }

    private fun cleanupAudioTrack() {
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}

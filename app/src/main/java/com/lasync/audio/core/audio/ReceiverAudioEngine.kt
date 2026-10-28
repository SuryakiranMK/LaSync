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
 * Receiver Audio Engine:
 * 1. Listens on UDP Port 8889 for incoming AudioFramePackets.
 * 2. Uses ClockSyncEngine to convert Master presentation timestamps to local monotonic time.
 * 3. Pre-buffers silence to absorb network jitter and precisely schedules local AudioTrack playback
 *    to match the Host's timeline.
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
    private var watchdogJob: Job? = null
    private var udpSocket: DatagramSocket? = null
    private var audioTrack: AudioTrack? = null

    @Volatile
    private var lastPacketTimeNs = 0L

    @Volatile
    private var isTrackInitialized = false

    fun startListening() {
        if (socketJob != null) return

        socketJob = scope.launch(Dispatchers.IO) {
            runReceiverLoop()
        }

        watchdogJob = scope.launch(Dispatchers.IO) {
            runWatchdogLoop()
        }
    }

    fun stopListening() {
        socketJob?.cancel()
        socketJob = null
        watchdogJob?.cancel()
        watchdogJob = null
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
            udpSocket = DatagramSocket(AudioConstants.AUDIO_PORT)
            udpSocket?.receiveBufferSize = 128 * 1024

            while (scope.isActive) {
                val packet = DatagramPacket(buffer, buffer.size)
                udpSocket?.receive(packet)

                val audioPacket = AudioFramePacket.decode(packet.data, packet.length)
                if (audioPacket != null) {
                    processAudioPacket(audioPacket)
                }
            }
        } catch (e: Exception) {
            if (scope.isActive) {
                Log.e(TAG, "Receiver UDP socket error: ${e.message}", e)
            }
        } finally {
            cleanupAudioTrack()
        }
    }

    private fun processAudioPacket(packet: AudioFramePacket) {
        val now = SystemClock.elapsedRealtimeNanos()
        lastPacketTimeNs = now
        _framesReceived.value++

        if (!isTrackInitialized || audioTrack == null) {
            initAudioTrackAndSchedule(packet, now)
        } else {
            audioTrack?.write(packet.payload, 0, packet.payload.size, AudioTrack.WRITE_NON_BLOCKING)
            _isAudioPlaying.value = true
        }
    }

    private fun initAudioTrackAndSchedule(packet: AudioFramePacket, now: Long) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            AudioConstants.SAMPLE_RATE,
            AudioConstants.CHANNEL_CONFIG_OUT,
            AudioConstants.AUDIO_FORMAT
        )
        val bufferSize = maxOf(minBufferSize * 2, AudioConstants.BYTES_PER_FRAME * 50)

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val format = AudioFormat.Builder()
            .setSampleRate(AudioConstants.SAMPLE_RATE)
            .setChannelMask(AudioConstants.CHANNEL_CONFIG_OUT)
            .setEncoding(AudioConstants.AUDIO_FORMAT)
            .build()

        val track = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        // Calculate when this packet is scheduled to play on the local timeline
        val localTargetTimeNs = clockSyncEngine.toLocalTimeNanos(packet.presentationTimestampNanos)
        val leadTimeNs = localTargetTimeNs - now

        // Calculate required silence delay to align with Master timeline
        val silenceSamples = if (leadTimeNs > 0) {
            ((leadTimeNs.toDouble() / 1_000_000_000.0) * AudioConstants.SAMPLE_RATE).toLong()
                .coerceIn(0L, (AudioConstants.SAMPLE_RATE * 2).toLong()) // max 2s safety clamp
        } else {
            0L
        }

        if (silenceSamples > 0) {
            val silenceBytes = ByteArray((silenceSamples * AudioConstants.BYTES_PER_SAMPLE).toInt())
            track.write(silenceBytes, 0, silenceBytes.size)
        }

        // Write first audio payload
        track.write(packet.payload, 0, packet.payload.size)
        track.play()

        audioTrack = track
        isTrackInitialized = true
        _isAudioPlaying.value = true
    }

    private suspend fun runWatchdogLoop() {
        while (scope.isActive) {
            delay(200)
            if (isTrackInitialized) {
                val elapsedSinceLastPacket = (SystemClock.elapsedRealtimeNanos() - lastPacketTimeNs) / 1_000_000L
                if (elapsedSinceLastPacket > STREAM_TIMEOUT_MS) {
                    // Audio stream stopped from host
                    cleanupAudioTrack()
                    _isAudioPlaying.value = false
                }
            }
        }
    }

    private fun cleanupAudioTrack() {
        isTrackInitialized = false
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}

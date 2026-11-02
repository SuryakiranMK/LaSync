package com.lasync.audio.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTimestamp
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

data class QueuedAudioFrame(
    val payload: ByteArray,
    val presentationTimestampNanos: Long,
    val sequenceNumber: Long,
    val bufferMode: Byte,
    val isMegaphone: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as QueuedAudioFrame
        return sequenceNumber == other.sequenceNumber &&
               presentationTimestampNanos == other.presentationTimestampNanos &&
               bufferMode == other.bufferMode &&
               isMegaphone == other.isMegaphone &&
               payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = sequenceNumber.hashCode()
        result = 31 * result + presentationTimestampNanos.hashCode()
        result = 31 * result + bufferMode.hashCode()
        result = 31 * result + isMegaphone.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

/**
 * TruePhase High-Precision Receiver Audio Engine:
 * 1. Dual Buffer Mode: Dynamically switches between Ultra-Stable Music Mode (1.0s deep buffer)
 *    and Low-Latency Video Mode (150ms buffer) based on Host configuration.
 * 2. Hardware Presentation Timestamps: Uses AudioTrack.getTimestamp() to measure the exact
 *    physical moment sound vibrates the speaker DAC, calculating real-time phase alignment.
 * 3. Thread-safe concurrent jitter queue with guaranteed full-frame WRITE_BLOCKING.
 * 4. GhostMegaphone Emergency PA: Overrides receiver volume to 100% and bypasses jitter queue for
 *    instant live voice playback across the room.
 */
class ReceiverAudioEngine(
    private val scope: CoroutineScope,
    private val clockSyncEngine: ClockSyncEngine,
    private val context: android.content.Context? = null
) {
    companion object {
        private const val TAG = "ReceiverAudioEngine"
        private const val STREAM_TIMEOUT_MS = 1200L
    }

    private val _isAudioPlaying = MutableStateFlow(false)
    val isAudioPlaying: StateFlow<Boolean> = _isAudioPlaying.asStateFlow()

    private val _framesReceived = MutableStateFlow(0L)
    val framesReceived: StateFlow<Long> = _framesReceived.asStateFlow()

    private val _activeBufferMode = MutableStateFlow(SyncBufferMode.ULTRA_STABLE_MUSIC)
    val activeBufferMode: StateFlow<SyncBufferMode> = _activeBufferMode.asStateFlow()

    private val _hardwareLatencyMs = MutableStateFlow(0f)
    val hardwareLatencyMs: StateFlow<Float> = _hardwareLatencyMs.asStateFlow()

    private val _phaseErrorMs = MutableStateFlow(0f)
    val phaseErrorMs: StateFlow<Float> = _phaseErrorMs.asStateFlow()

    private val _isPhaseLocked = MutableStateFlow(false)
    val isPhaseLocked: StateFlow<Boolean> = _isPhaseLocked.asStateFlow()

    private val _isHardwareTimestampSupported = MutableStateFlow(false)
    val isHardwareTimestampSupported: StateFlow<Boolean> = _isHardwareTimestampSupported.asStateFlow()

    private val _isMegaphoneActive = MutableStateFlow(false)
    val isMegaphoneActive: StateFlow<Boolean> = _isMegaphoneActive.asStateFlow()

    private val volumeOverrideManager = context?.let { VolumeOverrideManager(it) }

    @Volatile
    private var lastMegaphonePacketTimeNs = 0L

    private var socketJob: Job? = null
    private var playerJob: Job? = null
    private var udpSocket: DatagramSocket? = null
    private var audioTrack: AudioTrack? = null

    private val jitterQueue = ConcurrentLinkedQueue<QueuedAudioFrame>()

    @Volatile
    private var lastPacketTimeNs = 0L

    fun onMegaphoneStarted() {
        if (!_isMegaphoneActive.value) {
            _isMegaphoneActive.value = true
            volumeOverrideManager?.overrideToMaxVolume()
            // Flush old jitter frames so megaphone broadcast is immediate
            jitterQueue.clear()
            try {
                audioTrack?.flush()
            } catch (_: Exception) {}
        }
    }

    fun onMegaphoneStopped() {
        if (_isMegaphoneActive.value) {
            _isMegaphoneActive.value = false
            volumeOverrideManager?.restoreOriginalVolume()
        }
    }

    fun startListening() {
        if (socketJob != null) return

        jitterQueue.clear()
        _isAudioPlaying.value = false
        _isPhaseLocked.value = false

        socketJob = scope.launch(Dispatchers.IO) {
            runReceiverLoop()
        }

        playerJob = scope.launch(Dispatchers.Default) {
            runPlaybackLoop()
        }
    }

    fun stopListening() {
        onMegaphoneStopped()

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
        _isPhaseLocked.value = false
    }

    private fun runReceiverLoop() {
        val buffer = ByteArray(2048) // Room for 990-byte packet

        try {
            udpSocket = DatagramSocket(AudioConstants.AUDIO_PORT).apply {
                receiveBufferSize = 512 * 1024 // 512 KB socket buffer
            }

            while (scope.isActive) {
                val packet = DatagramPacket(buffer, buffer.size)
                udpSocket?.receive(packet)

                val audioPacket = AudioFramePacket.decode(packet.data, packet.length)
                if (audioPacket != null && audioPacket.payload.isNotEmpty()) {
                    lastPacketTimeNs = SystemClock.elapsedRealtimeNanos()
                    _framesReceived.value++

                    val isMegaphone = (audioPacket.packetType == AudioFramePacket.PACKET_TYPE_MEGAPHONE)
                    if (isMegaphone) {
                        lastMegaphonePacketTimeNs = lastPacketTimeNs
                        if (!_isMegaphoneActive.value) {
                            onMegaphoneStarted()
                        }
                    }

                    val mode = SyncBufferMode.fromId(audioPacket.bufferMode)
                    if (_activeBufferMode.value != mode) {
                        _activeBufferMode.value = mode
                    }

                    jitterQueue.offer(
                        QueuedAudioFrame(
                            payload = audioPacket.payload,
                            presentationTimestampNanos = audioPacket.presentationTimestampNanos,
                            sequenceNumber = audioPacket.sequenceNumber,
                            bufferMode = audioPacket.bufferMode,
                            isMegaphone = isMegaphone
                        )
                    )
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
        var totalAudioFramesWritten = 0L
        var lastTargetPresentationTimestampHostNs = 0L
        val audioTimestamp = AudioTimestamp()
        var timestampCheckCounter = 0

        while (scope.isActive) {
            val now = SystemClock.elapsedRealtimeNanos()
            val timeSinceLastPacketMs = if (lastPacketTimeNs > 0) (now - lastPacketTimeNs) / 1_000_000L else 0L
            val requiredBufferFrames = _activeBufferMode.value.bufferFrames

            // Auto-timeout for Megaphone broadcast if no packets arrived for 2.5s
            if (_isMegaphoneActive.value && (now - lastMegaphonePacketTimeNs > 2_500_000_000L)) {
                onMegaphoneStopped()
            }

            if (!isPlayingStarted) {
                // For live Megaphone voice, play immediately with zero jitter delay
                if (_isMegaphoneActive.value || jitterQueue.size >= requiredBufferFrames) {
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
                    _isPhaseLocked.value = false
                    totalAudioFramesWritten = 0L
                }
                delay(50)
                continue
            }

            val queuedFrame = jitterQueue.poll()
            if (queuedFrame != null) {
                lastTargetPresentationTimestampHostNs = queuedFrame.presentationTimestampNanos
                val stereoFrame = AudioConstants.monoToStereo(queuedFrame.payload)

                // WRITE_BLOCKING ensures full frame integrity
                val writtenBytes = audioTrack?.write(stereoFrame, 0, stereoFrame.size, AudioTrack.WRITE_BLOCKING) ?: 0
                if (writtenBytes > 0) {
                    val samplesWritten = writtenBytes / (AudioConstants.CHANNEL_COUNT_STEREO * AudioConstants.BYTES_PER_SAMPLE)
                    totalAudioFramesWritten += samplesWritten
                }
                _isAudioPlaying.value = true

                // Check TruePhase hardware presentation timestamp every 10 frames (~100ms)
                if (++timestampCheckCounter >= 10) {
                    timestampCheckCounter = 0
                    updateHardwarePhaseMetrics(totalAudioFramesWritten, lastTargetPresentationTimestampHostNs, audioTimestamp)
                }
            } else if (isPlayingStarted) {
                // Wi-Fi jitter dip: write silence frame to prevent hardware DAC pop
                audioTrack?.write(silenceStereoFrame, 0, silenceStereoFrame.size, AudioTrack.WRITE_BLOCKING)
            }
        }

        cleanupAudioTrack()
    }

    private fun updateHardwarePhaseMetrics(
        totalFramesWritten: Long,
        targetPresentationTimestampHostNs: Long,
        timestamp: AudioTimestamp
    ) {
        val track = audioTrack ?: return
        try {
            val hasTimestamp = track.getTimestamp(timestamp)
            if (hasTimestamp && timestamp.framePosition > 0) {
                _isHardwareTimestampSupported.value = true

                val presentedFrames = timestamp.framePosition
                val presentedNanoTime = timestamp.nanoTime

                // Frames currently in driver / AudioFlinger queue ahead of speaker DAC
                val framesInDriver = (totalFramesWritten - presentedFrames).coerceAtLeast(0L)
                val driverLatencyNs = (framesInDriver * 1_000_000_000L) / AudioConstants.SAMPLE_RATE
                val driverLatencyMs = driverLatencyNs / 1_000_000f
                _hardwareLatencyMs.value = driverLatencyMs

                // Reconcile monotonic nanoseconds with boot elapsedRealtimeNanos
                val nowBootNs = SystemClock.elapsedRealtimeNanos()
                val nowMonoNs = System.nanoTime()
                val bootDelta = nowBootNs - nowMonoNs
                val localPresentedBootNs = presentedNanoTime + bootDelta
                val localEstPhysicalPresentationNs = localPresentedBootNs + driverLatencyNs

                // Convert local physical presentation to Host Master Timeline
                val hostEstPhysicalPresentationNs = clockSyncEngine.toMasterTimeNanos(localEstPhysicalPresentationNs)

                if (targetPresentationTimestampHostNs > 0) {
                    val errorMs = (hostEstPhysicalPresentationNs - targetPresentationTimestampHostNs) / 1_000_000f
                    _phaseErrorMs.value = errorMs
                    _isPhaseLocked.value = kotlin.math.abs(errorMs) < 25f
                }
            } else {
                // Fallback latency estimate from buffer capacity
                val bufferFrames = track.bufferSizeInFrames
                val estimatedLatencyMs = (bufferFrames * 1000f) / AudioConstants.SAMPLE_RATE
                _hardwareLatencyMs.value = estimatedLatencyMs
            }
        } catch (_: Exception) {}
    }

    private fun setupAudioTrack() {
        val minBufferSize = AudioTrack.getMinBufferSize(
            AudioConstants.SAMPLE_RATE,
            AudioConstants.CHANNEL_CONFIG_OUT,
            AudioConstants.AUDIO_FORMAT
        )
        // Ample headroom (150 frames * 1920 bytes = 288 KB, ~1.5 seconds)
        val bufferSize = maxOf(minBufferSize * 4, AudioConstants.BYTES_PER_FRAME_STEREO * 150)

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

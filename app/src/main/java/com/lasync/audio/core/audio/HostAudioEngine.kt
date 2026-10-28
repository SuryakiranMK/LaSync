package com.lasync.audio.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.projection.MediaProjection
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.lasync.audio.core.network.NetworkUtils
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
import java.net.InetAddress

enum class HostAudioSource {
    IDLE,
    SYNTHETIC_TONE,
    SYSTEM_CAPTURE
}

/**
 * Host Audio Engine:
 * 1. Mode A (Synthetic Tone): Synthesizes 440 Hz test tone and streams with local AudioTrack delay.
 * 2. Mode B (System Audio Capture): Uses AudioPlaybackCapture to capture all music (Spotify, YouTube,
 *    games, local audio - excluding calls) and streams in real-time over UDP to all listeners.
 */
class HostAudioEngine(
    private val scope: CoroutineScope,
    private val getSessionId: () -> String,
    private val getTargetIps: () -> List<String>
) {
    companion object {
        private const val TAG = "HostAudioEngine"
    }

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _activeSource = MutableStateFlow(HostAudioSource.IDLE)
    val activeSource: StateFlow<HostAudioSource> = _activeSource.asStateFlow()

    private val _framesSent = MutableStateFlow(0L)
    val framesSent: StateFlow<Long> = _framesSent.asStateFlow()

    private var audioJob: Job? = null
    private var udpSocket: DatagramSocket? = null
    private var localAudioTrack: AudioTrack? = null
    private val toneGenerator = SineToneGenerator()

    private var playbackCaptureEngine: AudioPlaybackCaptureEngine? = null
    private var captureSequenceNumber = 0L

    fun startTone() {
        if (_isStreaming.value) return
        _isStreaming.value = true
        _activeSource.value = HostAudioSource.SYNTHETIC_TONE
        _framesSent.value = 0L

        audioJob = scope.launch(Dispatchers.IO) {
            runToneAudioLoop()
        }
    }

    fun stopTone() {
        if (_activeSource.value != HostAudioSource.SYNTHETIC_TONE) return
        _isStreaming.value = false
        _activeSource.value = HostAudioSource.IDLE
        audioJob?.cancel()
        audioJob = null
        cleanupResources()
    }

    fun startSystemAudio(mediaProjection: MediaProjection) {
        if (_isStreaming.value) return
        _isStreaming.value = true
        _activeSource.value = HostAudioSource.SYSTEM_CAPTURE
        _framesSent.value = 0L
        captureSequenceNumber = 0L

        try {
            udpSocket = DatagramSocket().apply {
                broadcast = true
                sendBufferSize = 256 * 1024
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening UDP socket for system audio: ${e.message}", e)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            playbackCaptureEngine = AudioPlaybackCaptureEngine(scope) { pcmFrame ->
                val timestamp = SystemClock.elapsedRealtimeNanos() + AudioConstants.PRESENTATION_LEAD_TIME_NS
                val packet = AudioFramePacket(
                    sessionId = getSessionId(),
                    sequenceNumber = captureSequenceNumber++,
                    presentationTimestampNanos = timestamp,
                    payload = pcmFrame
                )
                sendAudioPacket(packet)
                _framesSent.value = captureSequenceNumber
            }
            playbackCaptureEngine?.startCapture(mediaProjection)
        }
    }

    fun stopSystemAudio() {
        if (_activeSource.value != HostAudioSource.SYSTEM_CAPTURE) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            playbackCaptureEngine?.stopCapture()
            playbackCaptureEngine = null
        }
        cleanupResources()
        _isStreaming.value = false
        _activeSource.value = HostAudioSource.IDLE
    }

    fun stopAll() {
        when (_activeSource.value) {
            HostAudioSource.SYNTHETIC_TONE -> stopTone()
            HostAudioSource.SYSTEM_CAPTURE -> stopSystemAudio()
            HostAudioSource.IDLE -> cleanupResources()
        }
    }

    private suspend fun runToneAudioLoop() {
        toneGenerator.reset()

        try {
            udpSocket = DatagramSocket().apply {
                broadcast = true
                sendBufferSize = 256 * 1024
            }

            setupLocalAudioTrack()

            val frameIntervalNs = AudioConstants.FRAME_DURATION_MS * 1_000_000L
            var sequenceNumber = 0L
            var nextFrameTime = SystemClock.elapsedRealtimeNanos()

            while (scope.isActive && _isStreaming.value) {
                val now = SystemClock.elapsedRealtimeNanos()

                if (now >= nextFrameTime) {
                    val presentationTimestamp = nextFrameTime + AudioConstants.PRESENTATION_LEAD_TIME_NS
                    val pcmData = toneGenerator.generateFrame(AudioConstants.SAMPLES_PER_FRAME)

                    // 1. Send to all receivers via UDP
                    val packet = AudioFramePacket(
                        sessionId = getSessionId(),
                        sequenceNumber = sequenceNumber++,
                        presentationTimestampNanos = presentationTimestamp,
                        payload = pcmData
                    )
                    sendAudioPacket(packet)

                    // 2. Feed local AudioTrack with matching stereo frames
                    val stereoPcm = AudioConstants.monoToStereo(pcmData)
                    localAudioTrack?.write(stereoPcm, 0, stereoPcm.size, AudioTrack.WRITE_BLOCKING)

                    _framesSent.value = sequenceNumber
                    nextFrameTime += frameIntervalNs
                } else {
                    val remainingMs = (nextFrameTime - now) / 1_000_000L
                    if (remainingMs > 1) {
                        delay(remainingMs)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Host tone streaming error: ${e.message}", e)
        } finally {
            cleanupResources()
            _isStreaming.value = false
            _activeSource.value = HostAudioSource.IDLE
        }
    }

    private fun setupLocalAudioTrack() {
        val minBufferSize = AudioTrack.getMinBufferSize(
            AudioConstants.SAMPLE_RATE,
            AudioConstants.CHANNEL_CONFIG_OUT,
            AudioConstants.AUDIO_FORMAT
        )
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

        localAudioTrack = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        // Pre-fill with matching lead-time silence (200ms) so Host and Receiver stay locked
        val leadSilenceBytes = ByteArray((AudioConstants.JITTER_BUFFER_FRAMES * AudioConstants.BYTES_PER_FRAME_STEREO))
        localAudioTrack?.write(leadSilenceBytes, 0, leadSilenceBytes.size)

        localAudioTrack?.play()
    }

    private fun sendAudioPacket(packet: AudioFramePacket) {
        val bytes = packet.encode()
        val targets = getTargetIps().toMutableSet()

        // Also broadcast so newly joined or un-registered devices receive stream
        val broadcastIp = NetworkUtils.getBroadcastAddress().hostAddress
        if (broadcastIp != null) {
            targets.add(broadcastIp)
        }

        for (ip in targets) {
            try {
                val address = InetAddress.getByName(ip)
                val datagram = DatagramPacket(bytes, bytes.size, address, AudioConstants.AUDIO_PORT)
                udpSocket?.send(datagram)
            } catch (_: Exception) {}
        }
    }

    private fun cleanupResources() {
        try {
            localAudioTrack?.stop()
            localAudioTrack?.release()
        } catch (_: Exception) {}
        localAudioTrack = null

        try {
            udpSocket?.close()
        } catch (_: Exception) {}
        udpSocket = null
    }
}

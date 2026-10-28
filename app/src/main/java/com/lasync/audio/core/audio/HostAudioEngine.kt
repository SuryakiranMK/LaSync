package com.lasync.audio.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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
import java.util.concurrent.ConcurrentHashMap

/**
 * Host Audio Engine:
 * 1. Synthesizes continuous-phase 440 Hz PCM audio frames (10ms @ 48kHz).
 * 2. Transmits timestamped AudioFramePackets over UDP Port 8889 to all connected receivers.
 * 3. Schedules matching local playback through Host AudioTrack delayed by exactly PRESENTATION_LEAD_TIME_MS (500ms),
 *    ensuring Host and Receivers produce sound at the identical absolute moment.
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

    private val _framesSent = MutableStateFlow(0L)
    val framesSent: StateFlow<Long> = _framesSent.asStateFlow()

    private var audioJob: Job? = null
    private var udpSocket: DatagramSocket? = null
    private var localAudioTrack: AudioTrack? = null
    private val toneGenerator = SineToneGenerator()

    fun startTone() {
        if (_isStreaming.value) return
        _isStreaming.value = true
        _framesSent.value = 0L

        audioJob = scope.launch(Dispatchers.IO) {
            runAudioLoop()
        }
    }

    fun stopTone() {
        if (!_isStreaming.value) return
        _isStreaming.value = false
        audioJob?.cancel()
        audioJob = null
        cleanupResources()
    }

    private suspend fun runAudioLoop() {
        toneGenerator.reset()

        try {
            udpSocket = DatagramSocket()
            udpSocket?.broadcast = true
            udpSocket?.sendBufferSize = 128 * 1024

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

                    // 2. Feed local AudioTrack
                    localAudioTrack?.write(pcmData, 0, pcmData.size, AudioTrack.WRITE_NON_BLOCKING)

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
            Log.e(TAG, "Host audio streaming error: ${e.message}", e)
        } finally {
            cleanupResources()
            _isStreaming.value = false
        }
    }

    private fun setupLocalAudioTrack() {
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

        localAudioTrack = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        // Pre-fill with lead-time silence (500ms) so sound starts at target presentation time
        val leadSilenceSamples = (AudioConstants.SAMPLE_RATE * AudioConstants.PRESENTATION_LEAD_TIME_MS) / 1000
        val leadSilenceBytes = ByteArray((leadSilenceSamples * AudioConstants.BYTES_PER_SAMPLE).toInt())
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
            } catch (e: Exception) {
                // Ignore transient network errors per packet
            }
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

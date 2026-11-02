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
    SYSTEM_CAPTURE,
    MEGAPHONE,
    RAM_JUKEBOX
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

    private val _bufferMode = MutableStateFlow(SyncBufferMode.ULTRA_STABLE_MUSIC)
    val bufferMode: StateFlow<SyncBufferMode> = _bufferMode.asStateFlow()

    private val _isMegaphoneActive = MutableStateFlow(false)
    val isMegaphoneActive: StateFlow<Boolean> = _isMegaphoneActive.asStateFlow()

    val jukeboxPlaybackEngine = com.lasync.audio.core.jukebox.JukeboxPlaybackEngine(scope)
    val jukeboxServer = com.lasync.audio.core.jukebox.JukeboxHostServer(scope)

    private val _loadedJukeboxTrack = MutableStateFlow<com.lasync.audio.core.jukebox.JukeboxTrack?>(null)
    val loadedJukeboxTrack: StateFlow<com.lasync.audio.core.jukebox.JukeboxTrack?> = _loadedJukeboxTrack.asStateFlow()

    private val _isJukeboxDecoding = MutableStateFlow(false)
    val isJukeboxDecoding: StateFlow<Boolean> = _isJukeboxDecoding.asStateFlow()

    private val _jukeboxDecodeProgress = MutableStateFlow(0f)
    val jukeboxDecodeProgress: StateFlow<Float> = _jukeboxDecodeProgress.asStateFlow()

    private var audioJob: Job? = null
    private var udpSocket: DatagramSocket? = null
    private var localAudioTrack: AudioTrack? = null
    private val toneGenerator = SineToneGenerator()

    private var playbackCaptureEngine: AudioPlaybackCaptureEngine? = null
    private var captureSequenceNumber = 0L

    private var megaphoneCaptureEngine: MegaphoneCaptureEngine? = null
    private var previousSourceBeforeMegaphone = HostAudioSource.IDLE
    private var megaphoneSequenceNumber = 0L

    fun setBufferMode(mode: SyncBufferMode) {
        _bufferMode.value = mode
    }

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
                if (_isMegaphoneActive.value) {
                    // Duck system audio while Megaphone / Walkie-Talkie is broadcasting
                    return@AudioPlaybackCaptureEngine
                }
                val mode = _bufferMode.value
                val timestamp = SystemClock.elapsedRealtimeNanos() + mode.leadTimeNs
                val packet = AudioFramePacket(
                    sessionId = getSessionId(),
                    sequenceNumber = captureSequenceNumber++,
                    presentationTimestampNanos = timestamp,
                    payload = pcmFrame,
                    bufferMode = mode.id
                )
                sendAudioPacket(packet)
                _framesSent.value = captureSequenceNumber
            }
            playbackCaptureEngine?.startCapture(mediaProjection)
        }
    }

    fun startMegaphone(context: Context): Boolean {
        if (_isMegaphoneActive.value) return true

        if (udpSocket == null || udpSocket?.isClosed == true) {
            try {
                udpSocket = DatagramSocket().apply {
                    broadcast = true
                    sendBufferSize = 256 * 1024
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error opening UDP socket for megaphone: ${e.message}", e)
                return false
            }
        }

        previousSourceBeforeMegaphone = _activeSource.value
        _activeSource.value = HostAudioSource.MEGAPHONE
        _isMegaphoneActive.value = true
        _isStreaming.value = true
        megaphoneSequenceNumber = 0L

        val engine = MegaphoneCaptureEngine(context, scope)
        val started = engine.start { pcmFrame ->
            val now = SystemClock.elapsedRealtimeNanos()
            // 20ms lead time for immediate live walkie-talkie responsiveness
            val presentationTimestamp = now + 20_000_000L
            val packet = AudioFramePacket(
                sessionId = getSessionId(),
                sequenceNumber = megaphoneSequenceNumber++,
                presentationTimestampNanos = presentationTimestamp,
                payload = pcmFrame,
                bufferMode = _bufferMode.value.id,
                packetType = AudioFramePacket.PACKET_TYPE_MEGAPHONE
            )
            sendAudioPacket(packet)
            _framesSent.value = megaphoneSequenceNumber
        }

        if (!started) {
            _isMegaphoneActive.value = false
            _activeSource.value = previousSourceBeforeMegaphone
            _isStreaming.value = (previousSourceBeforeMegaphone != HostAudioSource.IDLE)
            return false
        }

        megaphoneCaptureEngine = engine
        return true
    }

    fun stopMegaphone() {
        if (!_isMegaphoneActive.value) return
        _isMegaphoneActive.value = false
        megaphoneCaptureEngine?.stop()
        megaphoneCaptureEngine = null

        _activeSource.value = previousSourceBeforeMegaphone
        if (_activeSource.value == HostAudioSource.IDLE) {
            cleanupResources()
            _isStreaming.value = false
        }
    }

    fun stopSystemAudio() {
        if (_activeSource.value != HostAudioSource.SYSTEM_CAPTURE && previousSourceBeforeMegaphone != HostAudioSource.SYSTEM_CAPTURE) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            playbackCaptureEngine?.stopCapture()
            playbackCaptureEngine = null
        }
        cleanupResources()
        _isStreaming.value = false
        _activeSource.value = HostAudioSource.IDLE
        previousSourceBeforeMegaphone = HostAudioSource.IDLE
    }

    suspend fun loadJukeboxTrack(context: Context, uri: android.net.Uri): com.lasync.audio.core.jukebox.JukeboxTrack? {
        stopAll()
        _isJukeboxDecoding.value = true
        _jukeboxDecodeProgress.value = 0f

        val track = com.lasync.audio.core.jukebox.AudioFileDecoder.decodeTrack(context, uri) { progress ->
            _jukeboxDecodeProgress.value = progress
        }

        _isJukeboxDecoding.value = false
        if (track != null) {
            _loadedJukeboxTrack.value = track
            _activeSource.value = HostAudioSource.RAM_JUKEBOX
            jukeboxServer.startServer(track)
        }
        return track
    }

    fun playJukebox(scheduledStartNanos: Long, startPositionMs: Long) {
        val track = _loadedJukeboxTrack.value ?: return
        _activeSource.value = HostAudioSource.RAM_JUKEBOX
        _isStreaming.value = true
        jukeboxPlaybackEngine.startScheduledPlayback(
            track = track,
            scheduledStartNanos = scheduledStartNanos,
            startPositionMs = startPositionMs,
            getNowNanos = { SystemClock.elapsedRealtimeNanos() }
        )
    }

    fun pauseJukebox() {
        jukeboxPlaybackEngine.pausePlayback()
    }

    fun stopJukebox() {
        jukeboxPlaybackEngine.stopPlayback()
        _isStreaming.value = false
        if (_activeSource.value == HostAudioSource.RAM_JUKEBOX) {
            _activeSource.value = HostAudioSource.IDLE
        }
    }

    fun stopAll() {
        stopMegaphone()
        stopJukebox()
        when (_activeSource.value) {
            HostAudioSource.SYNTHETIC_TONE -> stopTone()
            HostAudioSource.SYSTEM_CAPTURE -> stopSystemAudio()
            HostAudioSource.MEGAPHONE -> stopMegaphone()
            HostAudioSource.RAM_JUKEBOX -> stopJukebox()
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
                    if (_isMegaphoneActive.value) {
                        // Duck synthetic tone while Megaphone is active
                        nextFrameTime += frameIntervalNs
                        continue
                    }
                    val mode = _bufferMode.value
                    val presentationTimestamp = nextFrameTime + mode.leadTimeNs
                    val pcmData = toneGenerator.generateFrame(AudioConstants.SAMPLES_PER_FRAME)

                    // 1. Send to all receivers via UDP
                    val packet = AudioFramePacket(
                        sessionId = getSessionId(),
                        sequenceNumber = sequenceNumber++,
                        presentationTimestampNanos = presentationTimestamp,
                        payload = pcmData,
                        bufferMode = mode.id
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

        // Pre-fill with matching lead-time silence so Host and Receiver stay locked
        val leadFrames = _bufferMode.value.bufferFrames
        val leadSilenceBytes = ByteArray(leadFrames * AudioConstants.BYTES_PER_FRAME_STEREO)
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

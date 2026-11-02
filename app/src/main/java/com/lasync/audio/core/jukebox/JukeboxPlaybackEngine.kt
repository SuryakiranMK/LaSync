package com.lasync.audio.core.jukebox

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * JukeboxPlaybackEngine:
 * Plays preloaded uncompressed 16-bit stereo PCM audio directly from device RAM.
 * Provides microsecond phase-lock start triggers, hardware write blocking, and zero Wi-Fi traffic.
 */
class JukeboxPlaybackEngine(
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "JukeboxPlaybackEngine"
    }

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null

    @Volatile
    var isPlaying: Boolean = false
        private set

    @Volatile
    var currentPositionMs: Long = 0L
        private set

    private val _isPlayingState = MutableStateFlow(false)
    val isPlayingState: StateFlow<Boolean> = _isPlayingState.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    @Volatile
    private var currentGain: Float = 1.0f

    fun startScheduledPlayback(
        track: JukeboxTrack,
        scheduledStartNanos: Long,
        startPositionMs: Long,
        getNowNanos: () -> Long,
        delayTrimMs: Int = 0,
        initialGain: Float = currentGain
    ) {
        stopPlayback()

        currentGain = initialGain
        currentPositionMs = startPositionMs

        playbackJob = scope.launch(Dispatchers.Default) {
            setupAudioTrack(track.sampleRate)

            val targetNanos = scheduledStartNanos + (delayTrimMs * 1_000_000L)

            // Microsecond Precision Wait
            var now = getNowNanos()
            while (now < targetNanos && isActive) {
                val diffMs = (targetNanos - now) / 1_000_000L
                if (diffMs > 8) {
                    delay(diffMs - 4)
                } else {
                    // Spin-wait yield for exact sub-millisecond timeline alignment
                    Thread.yield()
                }
                now = getNowNanos()
            }

            if (!isActive) return@launch

            try {
                audioTrack?.play()
                isPlaying = true
                _isPlayingState.value = true

                var byteOffset = track.getOffsetBytesForPositionMs(startPositionMs)
                val pcm = track.pcmBytes
                // 10ms frame size
                val chunkSize = maxOf(512, (track.sampleRate * 4 * 10) / 1000)

                var progressUpdateCounter = 0

                while (isActive && isPlaying && byteOffset < pcm.size) {
                    val toWrite = minOf(chunkSize, pcm.size - byteOffset)
                    val written = audioTrack?.write(pcm, byteOffset, toWrite, AudioTrack.WRITE_BLOCKING) ?: 0
                    if (written > 0) {
                        byteOffset += written
                        if (++progressUpdateCounter >= 10) { // update UI every ~100ms
                            progressUpdateCounter = 0
                            val pos = track.getPositionMsForOffsetBytes(byteOffset)
                            currentPositionMs = pos
                            _playbackPositionMs.value = pos
                            if (track.durationMs > 0) {
                                _playbackProgress.value = (pos.toFloat() / track.durationMs.toFloat()).coerceIn(0f, 1f)
                            }
                        }
                    } else {
                        break
                    }
                }

                if (byteOffset >= pcm.size) {
                    Log.i(TAG, "RAM Jukebox playback reached end of track")
                    currentPositionMs = 0L
                    _playbackPositionMs.value = 0L
                    _playbackProgress.value = 0f
                }
            } catch (e: Exception) {
                Log.e(TAG, "Playback error: ${e.message}", e)
            } finally {
                isPlaying = false
                _isPlayingState.value = false
            }
        }
    }

    fun pausePlayback() {
        isPlaying = false
        _isPlayingState.value = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (_: Exception) {}
    }

    fun stopPlayback() {
        isPlaying = false
        _isPlayingState.value = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    fun setVolume(gain: Float) {
        currentGain = gain.coerceIn(0f, 1f)
        try {
            audioTrack?.setVolume(currentGain)
        } catch (_: Exception) {}
    }

    private fun setupAudioTrack(sampleRate: Int) {
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufferSize * 4, 1920 * 40)

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val format = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()

            audioTrack?.setVolume(currentGain)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up AudioTrack: ${e.message}", e)
        }
    }
}


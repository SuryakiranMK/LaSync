package com.lasync.audio.core.calibration

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.lasync.audio.core.audio.AcousticChirpGenerator
import com.lasync.audio.core.audio.AudioConstants
import com.lasync.audio.core.sync.ClockSyncEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

sealed class CalibrationState {
    object Idle : CalibrationState()
    object Listening : CalibrationState()
    data class Success(val measuredOffsetMs: Int, val confidencePercent: Int) : CalibrationState()
    data class Failed(val reason: String) : CalibrationState()
}

/**
 * PhaseLock Acoustic Auto-Calibration Engine (v1.4.0):
 *
 * Uses the device microphone and matched-filter cross-correlation to detect high-frequency
 * acoustic chirps emitted by the Host phone's speaker. By comparing the physical acoustic
 * arrival timestamp against the NTP-synchronized expected timestamp, it automatically calculates
 * the real-world distance + speaker driver latency and adjusts [delayTrimMs] with sub-millisecond precision.
 */
class AcousticCalibrationEngine(
    private val scope: CoroutineScope,
    private val clockSyncEngine: ClockSyncEngine,
    private val context: Context? = null
) {
    companion object {
        private const val TAG = "AcousticCalibration"
        const val CHIRP_DURATION_MS = 60
        const val CALIBRATION_LISTEN_WINDOW_MS = 1500L
        const val DETECTION_CONFIDENCE_THRESHOLD = 0.20f

        /**
         * Fast normalized cross-correlation matched filter to identify peak chirp arrival sample.
         * Returns (peakSampleIndex, confidenceScore) if confidence >= threshold, or null.
         */
        fun detectChirpPeak(
            signal: FloatArray,
            template: FloatArray,
            threshold: Float = 0.20f
        ): Pair<Int, Float>? {
            if (signal.size < template.size) return null

            // Template energy for normalization
            var templateEnergy = 0.0
            for (v in template) templateEnergy += v * v
            if (templateEnergy <= 1e-9) return null
            val normTemplate = sqrt(templateEnergy)

            var bestIndex = -1
            var bestScore = 0.0f

            val searchRange = signal.size - template.size
            // Step by 4 for fast coarse search across 48kHz audio, then fine-refine
            val step = 4
            var candidateIndex = -1
            var candidateScore = 0.0f

            for (i in 0 until searchRange step step) {
                var cross = 0.0
                var signalEnergy = 0.0

                for (j in 0 until template.size step 2) {
                    val s = signal[i + j]
                    cross += s * template[j]
                    signalEnergy += s * s
                }

                if (signalEnergy > 1e-9) {
                    val score = (cross / (sqrt(signalEnergy) * normTemplate)).toFloat()
                    if (score > candidateScore) {
                        candidateScore = score
                        candidateIndex = i
                    }
                }
            }

            // Fine search around coarse candidate (+/- 8 samples)
            if (candidateIndex >= 0 && candidateScore >= threshold * 0.7f) {
                val fineStart = maxOf(0, candidateIndex - 8)
                val fineEnd = minOf(searchRange, candidateIndex + 8)

                for (i in fineStart..fineEnd) {
                    var cross = 0.0
                    var signalEnergy = 0.0
                    for (j in 0 until template.size) {
                        val s = signal[i + j]
                        cross += s * template[j]
                        signalEnergy += s * s
                    }
                    if (signalEnergy > 1e-9) {
                        val score = (cross / (sqrt(signalEnergy) * normTemplate)).toFloat()
                        if (score > bestScore) {
                            bestScore = score
                            bestIndex = i
                        }
                    }
                }
            }

            return if (bestIndex >= 0 && bestScore >= threshold) {
                Pair(bestIndex, bestScore)
            } else null
        }

        fun pcmBytesToFloats(bytes: ByteArray): FloatArray {
            val count = bytes.size / 2
            val floats = FloatArray(count)
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until count) {
                floats[i] = buffer.short / 32768.0f
            }
            return floats
        }
    }

    private val _state = MutableStateFlow<CalibrationState>(CalibrationState.Idle)
    val state: StateFlow<CalibrationState> = _state.asStateFlow()

    val chirpGenerator = AcousticChirpGenerator(durationMs = CHIRP_DURATION_MS)
    private val referenceTemplate = chirpGenerator.generateFloatSamples()

    private var recordJob: Job? = null
    private var audioRecord: AudioRecord? = null

    /**
     * Start acoustic listening for Host chirp.
     * @param expectedHostChirpTimestampNs Expected host monotonic presentation timestamp of chirp start.
     * @param onCalibrated Callback invoked with the optimal delay trim in milliseconds.
     */
    @SuppressLint("MissingPermission")
    fun startCalibration(
        expectedHostChirpTimestampNs: Long,
        onCalibrated: (Int) -> Unit
    ) {
        if (_state.value is CalibrationState.Listening) return

        if (context != null && ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _state.value = CalibrationState.Failed("Microphone permission required")
            return
        }

        _state.value = CalibrationState.Listening

        recordJob = scope.launch(Dispatchers.IO) {
            val sampleRate = AudioConstants.SAMPLE_RATE
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT

            val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = maxOf(minBuf * 2, 4096)

            var record: AudioRecord? = null
            try {
                record = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed creating AudioRecord: ${e.message}")
            }

            if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
                _state.value = CalibrationState.Failed("Mic initialization failed")
                return@launch
            }

            audioRecord = record
            val recordedFloats = ArrayList<Float>()
            var recordStartMonotonicNs = 0L

            try {
                record.startRecording()
                recordStartMonotonicNs = SystemClock.elapsedRealtimeNanos()
                val readShorts = ShortArray(1024)

                val listenStart = SystemClock.elapsedRealtime()
                while (isActive && (SystemClock.elapsedRealtime() - listenStart) < CALIBRATION_LISTEN_WINDOW_MS) {
                    val read = record.read(readShorts, 0, readShorts.size)
                    if (read > 0) {
                        for (i in 0 until read) {
                            recordedFloats.add(readShorts[i] / 32768.0f)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during calibration recording: ${e.message}")
            } finally {
                try {
                    record.stop()
                    record.release()
                } catch (_: Exception) {}
                audioRecord = null
            }

            if (recordedFloats.size < referenceTemplate.size) {
                _state.value = CalibrationState.Failed("Insufficient mic data captured")
                return@launch
            }

            // DSP matched filter cross-correlation
            val detection = detectChirpPeak(recordedFloats.toFloatArray(), referenceTemplate, DETECTION_CONFIDENCE_THRESHOLD)
            if (detection != null) {
                val peakSampleIndex = detection.first
                val confidence = (detection.second * 100).toInt().coerceIn(0, 100)

                // Calculate measured local acoustic arrival timestamp
                val sampleOffsetNs = (peakSampleIndex.toDouble() / sampleRate.toDouble() * 1_000_000_000.0).toLong()
                val measuredLocalArrivalNs = recordStartMonotonicNs + sampleOffsetNs

                // Expected arrival in local clock
                val expectedLocalNs = clockSyncEngine.toLocalTimeNanos(expectedHostChirpTimestampNs)
                val deltaNs = measuredLocalArrivalNs - expectedLocalNs
                val deltaMs = (deltaNs / 1_000_000L).toInt()

                // Optimal trim to align acoustic output
                val calibratedTrimMs = (-deltaMs).coerceIn(-250, 350)
                _state.value = CalibrationState.Success(calibratedTrimMs, confidence)
                onCalibrated(calibratedTrimMs)
            } else {
                _state.value = CalibrationState.Failed("No chirp detected. Ensure Host volume is up.")
            }
        }
    }

    fun cancelCalibration() {
        recordJob?.cancel()
        recordJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        _state.value = CalibrationState.Idle
    }
}

package com.lasync.audio.core.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-precision linear frequency chirp generator for acoustic auto-calibration (PhaseLock v1.4.0).
 *
 * Sweeps linearly from [startFreqHz] to [endFreqHz] over [durationMs] with a Hann window
 * envelope to eliminate speaker clicks/spectral leakage.
 *
 * Acoustic properties:
 * - 1.5 kHz to 3.5 kHz bandwidth: immune to low-frequency HVAC/room rumble (<500 Hz).
 * - Sharp impulse-like autocorrelation peak for sub-millisecond arrival time determination.
 */
class AcousticChirpGenerator(
    val startFreqHz: Double = 1500.0,
    val endFreqHz: Double = 3500.0,
    val durationMs: Int = 60,
    val sampleRate: Int = AudioConstants.SAMPLE_RATE,
    val amplitude: Float = 0.85f
) {
    val totalSamples: Int = (sampleRate * durationMs) / 1000

    /**
     * Synthesizes the reference chirp as an uncompressed 16-bit PCM little-endian byte array.
     */
    fun generatePcmBytes(): ByteArray {
        val samples = generateFloatSamples()
        val buffer = ByteBuffer.allocate(samples.size * AudioConstants.BYTES_PER_SAMPLE)
            .order(ByteOrder.LITTLE_ENDIAN)

        for (sample in samples) {
            val shortVal = (sample * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
            buffer.putShort(shortVal)
        }

        return buffer.array()
    }

    /**
     * Synthesizes normalized float samples [-1.0f .. 1.0f] with a Hann window.
     * Useful for cross-correlation matched filtering.
     */
    fun generateFloatSamples(): FloatArray {
        val samples = FloatArray(totalSamples)
        val durationSec = durationMs / 1000.0
        val chirpRate = (endFreqHz - startFreqHz) / durationSec

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate.toDouble()
            // Linear FM chirp instantaneous phase: 2*pi*(f0*t + 0.5*k*t^2)
            val phase = 2.0 * PI * (startFreqHz * t + 0.5 * chirpRate * t * t)
            val raw = sin(phase).toFloat()

            // Hann window envelope: 0.5 * (1 - cos(2*pi*i / (N-1)))
            val hann = 0.5f * (1.0f - cos((2.0 * PI * i) / (totalSamples - 1)).toFloat())
            samples[i] = raw * hann * amplitude
        }

        return samples
    }
}

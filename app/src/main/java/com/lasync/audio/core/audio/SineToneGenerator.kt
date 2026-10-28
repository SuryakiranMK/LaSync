package com.lasync.audio.core.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

/**
 * Continuous-phase pure sine tone generator for Phase 2 acoustic synchronization testing.
 * Synthesizes 16-bit little-endian PCM buffers at the target sample rate.
 */
class SineToneGenerator(
    private val frequencyHz: Double = AudioConstants.TONE_FREQUENCY_HZ,
    private val sampleRate: Int = AudioConstants.SAMPLE_RATE,
    private val amplitude: Float = AudioConstants.TONE_AMPLITUDE
) {
    private var phase: Double = 0.0
    private val phaseIncrement: Double = 2.0 * PI * frequencyHz / sampleRate.toDouble()

    /**
     * Generates a single block of PCM audio (default: 480 samples = 10 ms at 48kHz).
     * Output is raw 16-bit signed PCM (Little-Endian).
     */
    fun generateFrame(samples: Int = AudioConstants.SAMPLES_PER_FRAME): ByteArray {
        val byteBuffer = ByteBuffer.allocate(samples * AudioConstants.BYTES_PER_SAMPLE)
            .order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until samples) {
            val sampleVal = (sin(phase) * 32767.0 * amplitude).toInt().coerceIn(-32768, 32767)
            byteBuffer.putShort(sampleVal.toShort())

            phase += phaseIncrement
            if (phase >= 2.0 * PI) {
                phase -= 2.0 * PI
            }
        }

        return byteBuffer.array()
    }

    /**
     * Reset phase to zero.
     */
    fun reset() {
        phase = 0.0
    }
}

package com.lasync.audio.core.audio

import android.media.AudioFormat

object AudioConstants {
    const val AUDIO_PORT = 8889

    const val SAMPLE_RATE = 48000
    const val CHANNEL_COUNT_MONO = 1
    const val CHANNEL_COUNT_STEREO = 2
    const val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_STEREO
    const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    const val BYTES_PER_SAMPLE = 2 // 16-bit PCM = 2 bytes

    // 10ms frame duration -> 480 samples -> 960 bytes payload
    // Combined with 30-byte header = 990 bytes per UDP datagram (Well under 1472-byte Wi-Fi MTU, 0 fragmentation)
    const val FRAME_DURATION_MS = 10
    const val SAMPLES_PER_FRAME = (SAMPLE_RATE * FRAME_DURATION_MS) / 1000 // 480 samples
    const val BYTES_PER_FRAME_MONO = SAMPLES_PER_FRAME * CHANNEL_COUNT_MONO * BYTES_PER_SAMPLE // 960 bytes
    const val BYTES_PER_FRAME_STEREO = SAMPLES_PER_FRAME * CHANNEL_COUNT_STEREO * BYTES_PER_SAMPLE // 1920 bytes

    // Target jitter buffer lead time: 200 ms (absorbs up to 200ms Wi-Fi variance without lag)
    const val JITTER_BUFFER_MS = 200L
    const val JITTER_BUFFER_FRAMES = (JITTER_BUFFER_MS / FRAME_DURATION_MS).toInt() // 20 frames
    const val PRESENTATION_LEAD_TIME_MS = JITTER_BUFFER_MS
    const val PRESENTATION_LEAD_TIME_NS = PRESENTATION_LEAD_TIME_MS * 1_000_000L

    const val TONE_FREQUENCY_HZ = 440.0 // Standard Concert Pitch 'A' (440 Hz)
    const val TONE_AMPLITUDE = 0.5f     // Clear, comfortable 50% non-clipping volume

    /**
     * Converts a 16-bit mono PCM buffer to 16-bit stereo PCM by duplicating each sample to Left and Right channels.
     */
    fun monoToStereo(monoBytes: ByteArray): ByteArray {
        val stereoBytes = ByteArray(monoBytes.size * 2)
        var m = 0
        var s = 0
        while (m + 1 < monoBytes.size) {
            val b0 = monoBytes[m]
            val b1 = monoBytes[m + 1]
            m += 2

            // Left
            stereoBytes[s++] = b0
            stereoBytes[s++] = b1
            // Right
            stereoBytes[s++] = b0
            stereoBytes[s++] = b1
        }
        return stereoBytes
    }
}

package com.lasync.audio.core.audio

import android.media.AudioFormat

object AudioConstants {
    const val AUDIO_PORT = 8889

    const val SAMPLE_RATE = 48000
    const val CHANNEL_COUNT = 1 // Mono stream (duplicated to stereo during AudioTrack playback)
    const val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
    const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    const val BYTES_PER_SAMPLE = 2 // 16-bit PCM = 2 bytes

    // 10ms frame duration -> 480 samples -> 960 bytes payload
    // Combined with 30-byte header = 990 bytes per UDP datagram (Well under 1472-byte Wi-Fi MTU, 0 fragmentation)
    const val FRAME_DURATION_MS = 10
    const val SAMPLES_PER_FRAME = (SAMPLE_RATE * FRAME_DURATION_MS) / 1000 // 480 samples
    const val BYTES_PER_FRAME = SAMPLES_PER_FRAME * CHANNEL_COUNT * BYTES_PER_SAMPLE // 960 bytes

    // 500ms jitter buffer lead time into the future for synchronized playback
    const val PRESENTATION_LEAD_TIME_MS = 500L
    const val PRESENTATION_LEAD_TIME_NS = PRESENTATION_LEAD_TIME_MS * 1_000_000L

    const val TONE_FREQUENCY_HZ = 440.0 // Standard Concert Pitch 'A' (440 Hz)
    const val TONE_AMPLITUDE = 0.5f     // Clear, comfortable 50% non-clipping volume
}

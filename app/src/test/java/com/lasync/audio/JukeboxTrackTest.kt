package com.lasync.audio

import com.lasync.audio.core.jukebox.JukeboxTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JukeboxTrackTest {

    @Test
    fun testFrameCalculationAndByteAlignment() {
        val sampleRate = 48000
        val channelCount = 2
        val bytesPerSecond = sampleRate * channelCount * 2 // 192,000 bytes/sec
        val durationMs = 10_000L // 10 seconds
        val totalBytes = (bytesPerSecond * 10).toInt() // 1,920,000 bytes

        val dummyPcm = ByteArray(totalBytes)
        val track = JukeboxTrack(
            title = "Cyber Ambient",
            artist = "LaSync",
            durationMs = durationMs,
            sampleRate = sampleRate,
            channelCount = channelCount,
            pcmBytes = dummyPcm
        )

        assertEquals(480_000L, track.totalFrames)

        // 1 second seek -> exactly 192,000 bytes
        val offset1s = track.getOffsetBytesForPositionMs(1000L)
        assertEquals(192_000, offset1s)
        assertEquals(0, offset1s % 4) // Must be aligned to 4-byte stereo frame

        // Check inverse
        val calculatedMs = track.getPositionMsForOffsetBytes(offset1s)
        assertEquals(1000L, calculatedMs)
    }

    @Test
    fun testClampingBoundaryConditions() {
        val sampleRate = 44100
        val channelCount = 2
        val bytesPerMs = (sampleRate * channelCount * 2) / 1000.0 // 176.4 bytes/ms
        val durationMs = 5_000L
        val totalBytes = (durationMs * bytesPerMs).toInt()

        val track = JukeboxTrack(
            title = "Test Track",
            artist = "Artist",
            durationMs = durationMs,
            sampleRate = sampleRate,
            channelCount = channelCount,
            pcmBytes = ByteArray(totalBytes)
        )

        // Seeking beyond duration
        val offsetBeyond = track.getOffsetBytesForPositionMs(10_000L)
        assertTrue(offsetBeyond <= totalBytes)
        assertEquals(0, offsetBeyond % 4)

        // Seeking negative
        val offsetNeg = track.getOffsetBytesForPositionMs(-500L)
        assertEquals(0, offsetNeg)
    }
}


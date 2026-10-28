package com.lasync.audio

import com.lasync.audio.core.audio.AudioConstants
import com.lasync.audio.core.audio.SineToneGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SineToneGeneratorTest {

    @Test
    fun testGenerateFrameSizeAndValues() {
        val generator = SineToneGenerator()
        val frameBytes = generator.generateFrame(AudioConstants.SAMPLES_PER_FRAME)

        assertEquals(AudioConstants.BYTES_PER_FRAME_MONO, frameBytes.size)

        // Validate samples are valid 16-bit signed PCM within limits
        val buffer = ByteBuffer.wrap(frameBytes).order(ByteOrder.LITTLE_ENDIAN)
        var hasNonZero = false

        while (buffer.remaining() >= 2) {
            val sample = buffer.short
            if (sample != 0.toShort()) {
                hasNonZero = true
            }
            assertTrue(sample >= -32768 && sample <= 32767)
        }

        assertTrue("Generated tone should contain oscillating non-zero values", hasNonZero)
    }

    @Test
    fun testMonoToStereoConversion() {
        val mono = byteArrayOf(0x12, 0x34, 0x56, 0x78) // 2 16-bit samples
        val stereo = AudioConstants.monoToStereo(mono)

        assertEquals(8, stereo.size)
        // Sample 1 Left & Right
        assertEquals(0x12.toByte(), stereo[0])
        assertEquals(0x34.toByte(), stereo[1])
        assertEquals(0x12.toByte(), stereo[2])
        assertEquals(0x34.toByte(), stereo[3])
        // Sample 2 Left & Right
        assertEquals(0x56.toByte(), stereo[4])
        assertEquals(0x78.toByte(), stereo[5])
        assertEquals(0x56.toByte(), stereo[6])
        assertEquals(0x78.toByte(), stereo[7])
    }
}

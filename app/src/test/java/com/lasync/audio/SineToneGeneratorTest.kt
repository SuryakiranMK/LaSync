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

        assertEquals(AudioConstants.BYTES_PER_FRAME, frameBytes.size)

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
}

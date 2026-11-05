package com.lasync.audio

import com.lasync.audio.core.audio.AcousticChirpGenerator
import com.lasync.audio.core.audio.AudioConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AcousticChirpGeneratorTest {

    @Test
    fun testChirpDimensions() {
        val generator = AcousticChirpGenerator(durationMs = 60, sampleRate = 48000)
        val expectedSamples = (48000 * 60) / 1000 // 2880 samples
        assertEquals(expectedSamples, generator.totalSamples)

        val pcm = generator.generatePcmBytes()
        assertEquals(expectedSamples * AudioConstants.BYTES_PER_SAMPLE, pcm.size)

        val floats = generator.generateFloatSamples()
        assertEquals(expectedSamples, floats.size)
    }

    @Test
    fun testHannWindowTapering() {
        val generator = AcousticChirpGenerator(durationMs = 60, sampleRate = 48000)
        val floats = generator.generateFloatSamples()

        // First and last samples should be near zero due to Hann window
        assertTrue("Start sample should be near 0", abs(floats[0]) < 0.001f)
        assertTrue("End sample should be near 0", abs(floats[floats.size - 1]) < 0.001f)

        // Middle region should have significant energy
        var midMax = 0f
        for (i in (floats.size / 4) until (3 * floats.size / 4)) {
            val a = abs(floats[i])
            if (a > midMax) midMax = a
        }
        assertTrue("Middle region peak amplitude should be significant (>0.5)", midMax > 0.5f)
    }

    @Test
    fun testAutocorrelationPeakAtLagZero() {
        val generator = AcousticChirpGenerator(durationMs = 50, sampleRate = 48000)
        val template = generator.generateFloatSamples()

        // Energy at lag 0
        var peakEnergy = 0.0
        for (v in template) {
            peakEnergy += v * v
        }

        // Test non-zero lags (+/- 10 samples)
        for (lag in listOf(5, 10, 20, 50)) {
            var crossEnergy = 0.0
            for (i in 0 until (template.size - lag)) {
                crossEnergy += template[i] * template[i + lag]
            }
            assertTrue("Autocorrelation at lag $lag should be less than peak energy", crossEnergy < peakEnergy * 0.9)
        }
    }
}

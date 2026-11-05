package com.lasync.audio

import com.lasync.audio.core.audio.AcousticChirpGenerator
import com.lasync.audio.core.calibration.AcousticCalibrationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AcousticCalibrationEngineTest {

    private val generator = AcousticChirpGenerator(durationMs = 40, sampleRate = 48000)
    private val template = generator.generateFloatSamples()

    @Test
    fun testDetectChirpInCleanSignal() {
        val insertIndex = 2400 // Insert at 50ms into a 150ms buffer
        val buffer = FloatArray(48000 * 150 / 1000) // 7200 samples

        // Insert chirp
        for (i in template.indices) {
            buffer[insertIndex + i] = template[i]
        }

        val result = AcousticCalibrationEngine.detectChirpPeak(buffer, template, threshold = 0.5f)
        assertNotNull("Chirp should be detected", result)
        assertEquals("Peak should match insertion index within 1 sample", insertIndex, result!!.first)
        assertTrue("Confidence should be high (>0.8)", result.second > 0.8f)
    }

    @Test
    fun testDetectChirpInNoisySignal() {
        val insertIndex = 3000
        val buffer = FloatArray(8000)

        // Add random background white noise
        val rng = Random(42)
        for (i in buffer.indices) {
            buffer[i] = (rng.nextFloat() - 0.5f) * 0.15f
        }

        // Add chirp on top of noise
        for (i in template.indices) {
            buffer[insertIndex + i] += template[i]
        }

        val result = AcousticCalibrationEngine.detectChirpPeak(buffer, template, threshold = 0.3f)
        assertNotNull("Chirp should be detected in noise", result)
        assertTrue("Detected index should be within 2 samples of insert index",
            kotlin.math.abs(result!!.first - insertIndex) <= 2)
    }

    @Test
    fun testRejectSilenceAndNoiseWithoutChirp() {
        val buffer = FloatArray(5000)
        val rng = Random(123)
        for (i in buffer.indices) {
            buffer[i] = (rng.nextFloat() - 0.5f) * 0.1f
        }

        val result = AcousticCalibrationEngine.detectChirpPeak(buffer, template, threshold = 0.4f)
        assertNull("Should not detect chirp in pure noise", result)
    }

    @Test
    fun testPcmConversion() {
        val pcm = generator.generatePcmBytes()
        val floats = AcousticCalibrationEngine.pcmBytesToFloats(pcm)
        assertEquals(pcm.size / 2, floats.size)
    }
}

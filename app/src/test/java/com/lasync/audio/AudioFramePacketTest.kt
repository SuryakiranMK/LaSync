package com.lasync.audio

import com.lasync.audio.core.audio.AudioFramePacket
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AudioFramePacketTest {

    @Test
    fun testAudioFramePacketEncodeDecodeRoundtrip() {
        val samplePcm = ByteArray(960) { index -> (index % 128).toByte() }
        val originalPacket = AudioFramePacket(
            sessionId = "L9WJRP",
            sequenceNumber = 42L,
            presentationTimestampNanos = 1_500_000_000L,
            payload = samplePcm
        )

        val encodedBytes = originalPacket.encode()
        assertEquals(AudioFramePacket.HEADER_SIZE + 960, encodedBytes.size)

        val decodedPacket = AudioFramePacket.decode(encodedBytes, encodedBytes.size)
        assertNotNull(decodedPacket)
        assertEquals("L9WJRP", decodedPacket!!.sessionId)
        assertEquals(42L, decodedPacket.sequenceNumber)
        assertEquals(1_500_000_000L, decodedPacket.presentationTimestampNanos)
        assertArrayEquals(samplePcm, decodedPacket.payload)
    }

    @Test
    fun testCorruptPacketReturnsNull() {
        val invalidMagic = ByteArray(50) { 0x00 }
        val decoded = AudioFramePacket.decode(invalidMagic, invalidMagic.size)
        assertNull(decoded)
    }
}

package com.lasync.audio

import com.lasync.audio.core.audio.AudioFramePacket
import com.lasync.audio.core.audio.SyncBufferMode
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioFramePacketTest {

    @Test
    fun testAudioFramePacketEncodeDecodeRoundtripV2() {
        val samplePcm = ByteArray(960) { index -> (index % 128).toByte() }
        val originalPacket = AudioFramePacket(
            sessionId = "L9WJRP",
            sequenceNumber = 42L,
            presentationTimestampNanos = 1_500_000_000L,
            payload = samplePcm,
            bufferMode = SyncBufferMode.LOW_LATENCY_VIDEO.id
        )

        val encodedBytes = originalPacket.encode()
        assertEquals(AudioFramePacket.HEADER_SIZE_V2 + 960, encodedBytes.size)

        val decodedPacket = AudioFramePacket.decode(encodedBytes, encodedBytes.size)
        assertNotNull(decodedPacket)
        assertEquals("L9WJRP", decodedPacket!!.sessionId)
        assertEquals(42L, decodedPacket.sequenceNumber)
        assertEquals(1_500_000_000L, decodedPacket.presentationTimestampNanos)
        assertEquals(SyncBufferMode.LOW_LATENCY_VIDEO.id, decodedPacket.bufferMode)
        assertArrayEquals(samplePcm, decodedPacket.payload)
    }

    @Test
    fun testAudioFramePacketV1BackwardCompatibility() {
        val samplePcm = ByteArray(960) { 0x55 }
        val totalSize = AudioFramePacket.HEADER_SIZE_V1 + samplePcm.size
        val buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.BIG_ENDIAN)

        buffer.putShort(AudioFramePacket.MAGIC)
        buffer.put(AudioFramePacket.VERSION_V1)
        buffer.put(AudioFramePacket.PACKET_TYPE_AUDIO_FRAME)

        val sessionBytes = "ABCDEF".toByteArray(Charsets.US_ASCII)
        buffer.put(sessionBytes)
        buffer.putLong(100L) // sequenceNumber
        buffer.putLong(2_000_000_000L) // presentationTimestampNanos
        buffer.putInt(samplePcm.size) // payloadLength
        buffer.put(samplePcm)

        val v1Bytes = buffer.array()
        val decoded = AudioFramePacket.decode(v1Bytes, v1Bytes.size)

        assertNotNull(decoded)
        assertEquals("ABCDEF", decoded!!.sessionId)
        assertEquals(100L, decoded.sequenceNumber)
        assertEquals(2_000_000_000L, decoded.presentationTimestampNanos)
        assertArrayEquals(samplePcm, decoded.payload)
    }

    @Test
    fun testCorruptPacketReturnsNull() {
        val invalidMagic = ByteArray(50) { 0x00 }
        val decoded = AudioFramePacket.decode(invalidMagic, invalidMagic.size)
        assertNull(decoded)
    }

    @Test
    fun testMegaphonePacketEncodeDecodeRoundtrip() {
        val micPcm = ByteArray(960) { (it * 3 % 256).toByte() }
        val megaphonePacket = AudioFramePacket(
            sessionId = "HOST01",
            sequenceNumber = 10L,
            presentationTimestampNanos = 2_000_000_000L,
            payload = micPcm,
            bufferMode = SyncBufferMode.ULTRA_STABLE_MUSIC.id,
            packetType = AudioFramePacket.PACKET_TYPE_MEGAPHONE
        )

        val encoded = megaphonePacket.encode()
        val decoded = AudioFramePacket.decode(encoded, encoded.size)

        assertNotNull(decoded)
        assertEquals("HOST01", decoded!!.sessionId)
        assertEquals(10L, decoded.sequenceNumber)
        assertEquals(AudioFramePacket.PACKET_TYPE_MEGAPHONE, decoded.packetType)
        assertArrayEquals(micPcm, decoded.payload)
    }
}

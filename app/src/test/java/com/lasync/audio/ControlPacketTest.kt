package com.lasync.audio

import com.lasync.audio.core.network.ControlPacket
import com.lasync.audio.core.network.NetworkConstants
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.charset.StandardCharsets

class ControlPacketTest {

    @Test
    fun testSerializationAndDeserializationRoundtrip() {
        val payload = "Redmi Note 12 Pro".toByteArray(StandardCharsets.UTF_8)
        val original = ControlPacket(
            type = NetworkConstants.TYPE_PING,
            sessionId = "SYNC88",
            sequenceNumber = 42L,
            senderId = "CLIENT_01",
            t1 = 1_000_000_000L,
            t2 = 1_005_000_000L,
            t3 = 1_005_200_000L,
            payload = payload
        )

        val serialized = original.toByteArray()
        val deserialized = ControlPacket.fromByteArray(serialized, serialized.size)

        assertNotNull(deserialized)
        assertEquals(original.type, deserialized!!.type)
        assertEquals(original.sessionId, deserialized.sessionId)
        assertEquals(original.sequenceNumber, deserialized.sequenceNumber)
        assertEquals(original.senderId, deserialized.senderId)
        assertEquals(original.t1, deserialized.t1)
        assertEquals(original.t2, deserialized.t2)
        assertEquals(original.t3, deserialized.t3)
        assertArrayEquals(original.payload, deserialized.payload)
    }

    @Test
    fun testCorruptedMagicRejection() {
        val original = ControlPacket(
            type = NetworkConstants.TYPE_PING,
            sessionId = "SYNC88",
            sequenceNumber = 1L,
            senderId = "TEST"
        )
        val bytes = original.toByteArray()
        // Corrupt magic header
        bytes[0] = 0x00
        bytes[1] = 0x00

        val result = ControlPacket.fromByteArray(bytes, bytes.size)
        assertNull(result)
    }

    @Test
    fun testTruncatedPacketRejection() {
        val original = ControlPacket(
            type = NetworkConstants.TYPE_PING,
            sessionId = "SYNC88",
            sequenceNumber = 1L,
            senderId = "TEST"
        )
        val bytes = original.toByteArray()
        // Truncate to less than minimum header size
        val result = ControlPacket.fromByteArray(bytes, 15)
        assertNull(result)
    }

    @Test
    fun testMegaphoneControlPacketsRoundtrip() {
        val startPacket = ControlPacket(
            type = NetworkConstants.TYPE_MEGAPHONE_START,
            sessionId = "HOST99",
            sequenceNumber = 1L,
            senderId = "HOST"
        )
        val startBytes = startPacket.toByteArray()
        val decodedStart = ControlPacket.fromByteArray(startBytes, startBytes.size)
        assertNotNull(decodedStart)
        assertEquals(NetworkConstants.TYPE_MEGAPHONE_START, decodedStart!!.type)
        assertEquals("HOST99", decodedStart.sessionId)

        val stopPacket = ControlPacket(
            type = NetworkConstants.TYPE_MEGAPHONE_STOP,
            sessionId = "HOST99",
            sequenceNumber = 2L,
            senderId = "HOST"
        )
        val stopBytes = stopPacket.toByteArray()
        val decodedStop = ControlPacket.fromByteArray(stopBytes, stopBytes.size)
        assertNotNull(decodedStop)
        assertEquals(NetworkConstants.TYPE_MEGAPHONE_STOP, decodedStop!!.type)
        assertEquals("HOST99", decodedStop.sessionId)
    }
}

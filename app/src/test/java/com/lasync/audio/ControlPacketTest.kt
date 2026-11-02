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

    @Test
    fun testSoundBoardRemoteConfigControlPacketRoundtrip() {
        val bb = java.nio.ByteBuffer.allocate(9).order(java.nio.ByteOrder.BIG_ENDIAN)
        bb.putInt(-25) // delayTrimMs: -25ms (Bluetooth latency compensation)
        bb.putInt(80)  // volumePercent: 80%
        bb.put(1.toByte()) // isMuted: true

        val configPacket = ControlPacket(
            type = NetworkConstants.TYPE_REMOTE_CONFIG,
            sessionId = "MIXER1",
            sequenceNumber = 5L,
            senderId = "HOST",
            payload = bb.array()
        )
        val configBytes = configPacket.toByteArray()
        val decoded = ControlPacket.fromByteArray(configBytes, configBytes.size)

        assertNotNull(decoded)
        assertEquals(NetworkConstants.TYPE_REMOTE_CONFIG, decoded!!.type)
        assertEquals(9, decoded.payload.size)

        val readBb = java.nio.ByteBuffer.wrap(decoded.payload).order(java.nio.ByteOrder.BIG_ENDIAN)
        assertEquals(-25, readBb.int)
        assertEquals(80, readBb.int)
        assertEquals(1.toByte(), readBb.get())
    }

    @Test
    fun testSoundBoardDisconnectPacketRoundtrip() {
        val disconnectPacket = ControlPacket(
            type = NetworkConstants.TYPE_REMOTE_DISCONNECT,
            sessionId = "MIXER1",
            sequenceNumber = 6L,
            senderId = "HOST"
        )
        val bytes = disconnectPacket.toByteArray()
        val decoded = ControlPacket.fromByteArray(bytes, bytes.size)

        assertNotNull(decoded)
        assertEquals(NetworkConstants.TYPE_REMOTE_DISCONNECT, decoded!!.type)
    }

    @Test
    fun testJukeboxControlPacketsRoundtrip() {
        val scheduledStart = 1_500_000_000_000L
        val startOffsetMs = 45_000L

        val playPacket = ControlPacket(
            type = NetworkConstants.TYPE_JUKEBOX_PLAY,
            sessionId = "JUKE01",
            sequenceNumber = 10L,
            senderId = "HOST",
            t1 = scheduledStart,
            t2 = startOffsetMs
        )
        val playBytes = playPacket.toByteArray()
        val decodedPlay = ControlPacket.fromByteArray(playBytes, playBytes.size)

        assertNotNull(decodedPlay)
        assertEquals(NetworkConstants.TYPE_JUKEBOX_PLAY, decodedPlay!!.type)
        assertEquals(scheduledStart, decodedPlay.t1)
        assertEquals(startOffsetMs, decodedPlay.t2)

        val readyPacket = ControlPacket(
            type = NetworkConstants.TYPE_JUKEBOX_READY,
            sessionId = "JUKE01",
            sequenceNumber = 11L,
            senderId = "RCV_01"
        )
        val readyBytes = readyPacket.toByteArray()
        val decodedReady = ControlPacket.fromByteArray(readyBytes, readyBytes.size)

        assertNotNull(decodedReady)
        assertEquals(NetworkConstants.TYPE_JUKEBOX_READY, decodedReady!!.type)
        assertEquals("RCV_01", decodedReady.senderId)
    }

    @Test
    fun testAudioStopPacketRoundtrip() {
        val stopPacket = ControlPacket(
            type = NetworkConstants.TYPE_AUDIO_STOP,
            sessionId = "HOST01",
            sequenceNumber = 20L,
            senderId = "HOST"
        )
        val stopBytes = stopPacket.toByteArray()
        val decodedStop = ControlPacket.fromByteArray(stopBytes, stopBytes.size)

        assertNotNull(decodedStop)
        assertEquals(NetworkConstants.TYPE_AUDIO_STOP, decodedStop!!.type)
        assertEquals("HOST01", decodedStop.sessionId)
    }
}

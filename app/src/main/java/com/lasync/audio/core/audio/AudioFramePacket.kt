package com.lasync.audio.core.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Binary UDP packet for synchronized audio transmission over Port 8889.
 *
 * Header (30 bytes):
 * - Magic: 2 bytes (0x4C53 = "LS")
 * - Version: 1 byte (1)
 * - Packet Type: 1 byte (0x05 = AUDIO_FRAME)
 * - Session ID: 6 ASCII bytes
 * - Sequence Number: 8 bytes (Long)
 * - Presentation Timestamp: 8 bytes (Long, monotonic nanoseconds on Host clock)
 * - Payload Length: 4 bytes (Int)
 * Followed by raw 16-bit PCM payload.
 */
data class AudioFramePacket(
    val sessionId: String,
    val sequenceNumber: Long,
    val presentationTimestampNanos: Long,
    val payload: ByteArray,
    val bufferMode: Byte = SyncBufferMode.ULTRA_STABLE_MUSIC.id
) {
    companion object {
        const val MAGIC: Short = 0x4C53
        const val VERSION_V1: Byte = 1
        const val VERSION_V2: Byte = 2
        const val PACKET_TYPE_AUDIO_FRAME: Byte = 0x05
        const val HEADER_SIZE_V1 = 30
        const val HEADER_SIZE_V2 = 31
        const val HEADER_SIZE = HEADER_SIZE_V2

        fun decode(bytes: ByteArray, length: Int): AudioFramePacket? {
            if (length < HEADER_SIZE_V1) return null

            val buffer = ByteBuffer.wrap(bytes, 0, length).order(ByteOrder.BIG_ENDIAN)
            val magic = buffer.short
            if (magic != MAGIC) return null

            val version = buffer.get()
            if (version != VERSION_V1 && version != VERSION_V2) return null

            val type = buffer.get()
            if (type != PACKET_TYPE_AUDIO_FRAME) return null

            val sessionBytes = ByteArray(6)
            buffer.get(sessionBytes)
            val sessionId = String(sessionBytes, Charsets.US_ASCII).trim()

            val sequenceNumber = buffer.long
            val presentationTimestampNanos = buffer.long
            val payloadLength = buffer.int

            val bufferMode = if (version >= VERSION_V2 && buffer.hasRemaining()) {
                buffer.get()
            } else {
                SyncBufferMode.ULTRA_STABLE_MUSIC.id
            }

            if (payloadLength < 0 || buffer.remaining() < payloadLength) return null

            val payload = ByteArray(payloadLength)
            buffer.get(payload)

            return AudioFramePacket(
                sessionId = sessionId,
                sequenceNumber = sequenceNumber,
                presentationTimestampNanos = presentationTimestampNanos,
                payload = payload,
                bufferMode = bufferMode
            )
        }
    }

    fun encode(): ByteArray {
        val totalSize = HEADER_SIZE_V2 + payload.size
        val buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.BIG_ENDIAN)

        buffer.putShort(MAGIC)
        buffer.put(VERSION_V2)
        buffer.put(PACKET_TYPE_AUDIO_FRAME)

        val sessionBytes = ByteArray(6) { 0x20 } // default space
        val rawSession = sessionId.toByteArray(Charsets.US_ASCII)
        System.arraycopy(rawSession, 0, sessionBytes, 0, minOf(rawSession.size, 6))
        buffer.put(sessionBytes)

        buffer.putLong(sequenceNumber)
        buffer.putLong(presentationTimestampNanos)
        buffer.putInt(payload.size)
        buffer.put(bufferMode)
        buffer.put(payload)

        return buffer.array()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioFramePacket
        return sequenceNumber == other.sequenceNumber &&
               presentationTimestampNanos == other.presentationTimestampNanos &&
               sessionId == other.sessionId &&
               bufferMode == other.bufferMode &&
               payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = sessionId.hashCode()
        result = 31 * result + sequenceNumber.hashCode()
        result = 31 * result + presentationTimestampNanos.hashCode()
        result = 31 * result + bufferMode.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

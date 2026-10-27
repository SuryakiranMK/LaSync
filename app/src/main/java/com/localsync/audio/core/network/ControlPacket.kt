package com.localsync.audio.core.network

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

data class ControlPacket(
    val type: Byte,
    val sessionId: String,
    val sequenceNumber: Long,
    val senderId: String,
    val t1: Long = 0L,
    val t2: Long = 0L,
    val t3: Long = 0L,
    val payload: ByteArray = ByteArray(0)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ControlPacket

        if (type != other.type) return false
        if (sessionId != other.sessionId) return false
        if (sequenceNumber != other.sequenceNumber) return false
        if (senderId != other.senderId) return false
        if (t1 != other.t1) return false
        if (t2 != other.t2) return false
        if (t3 != other.t3) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.toInt()
        result = 31 * result + sessionId.hashCode()
        result = 31 * result + sequenceNumber.hashCode()
        result = 31 * result + senderId.hashCode()
        result = 31 * result + t1.hashCode()
        result = 31 * result + t2.hashCode()
        result = 31 * result + t3.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }

    fun toByteArray(): ByteArray {
        val sessionBytes = sessionId.toByteArray(StandardCharsets.UTF_8).let {
            if (it.size > 8) it.copyOf(8) else it
        }
        val senderBytes = senderId.toByteArray(StandardCharsets.UTF_8).let {
            if (it.size > 16) it.copyOf(16) else it
        }

        val totalSize = 2 + 1 + 1 + 1 + sessionBytes.size + 1 + senderBytes.size + 8 + 8 + 8 + 8 + 2 + payload.size
        val buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.BIG_ENDIAN)

        buffer.putShort(NetworkConstants.MAGIC_CONTROL)
        buffer.put(NetworkConstants.PROTOCOL_VERSION)
        buffer.put(type)

        // Session ID: 1-byte len + bytes
        buffer.put(sessionBytes.size.toByte())
        buffer.put(sessionBytes)

        // Sender ID: 1-byte len + bytes
        buffer.put(senderBytes.size.toByte())
        buffer.put(senderBytes)

        buffer.putLong(sequenceNumber)
        buffer.putLong(t1)
        buffer.putLong(t2)
        buffer.putLong(t3)

        buffer.putShort(payload.size.toShort())
        if (payload.isNotEmpty()) {
            buffer.put(payload)
        }

        return buffer.array()
    }

    companion object {
        fun fromByteArray(data: ByteArray, length: Int): ControlPacket? {
            if (length < 32) return null // Minimum valid header size
            val buffer = ByteBuffer.wrap(data, 0, length).order(ByteOrder.BIG_ENDIAN)

            val magic = buffer.short
            if (magic != NetworkConstants.MAGIC_CONTROL) return null

            val version = buffer.get()
            if (version != NetworkConstants.PROTOCOL_VERSION) return null

            val type = buffer.get()

            val sessionLen = buffer.get().toInt() and 0xFF
            if (buffer.remaining() < sessionLen) return null
            val sessionBytes = ByteArray(sessionLen)
            buffer.get(sessionBytes)
            val sessionId = String(sessionBytes, StandardCharsets.UTF_8)

            val senderLen = buffer.get().toInt() and 0xFF
            if (buffer.remaining() < senderLen) return null
            val senderBytes = ByteArray(senderLen)
            buffer.get(senderBytes)
            val senderId = String(senderBytes, StandardCharsets.UTF_8)

            if (buffer.remaining() < 8 * 4 + 2) return null
            val sequenceNumber = buffer.long
            val t1 = buffer.long
            val t2 = buffer.long
            val t3 = buffer.long

            val payloadLen = buffer.short.toInt() and 0xFFFF
            val payload = if (payloadLen > 0 && buffer.remaining() >= payloadLen) {
                val bytes = ByteArray(payloadLen)
                buffer.get(bytes)
                bytes
            } else {
                ByteArray(0)
            }

            return ControlPacket(
                type = type,
                sessionId = sessionId,
                sequenceNumber = sequenceNumber,
                senderId = senderId,
                t1 = t1,
                t2 = t2,
                t3 = t3,
                payload = payload
            )
        }
    }
}

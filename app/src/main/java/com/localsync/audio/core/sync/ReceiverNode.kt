package com.localsync.audio.core.sync

data class ReceiverNode(
    val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int,
    val rttMs: Float = 0f,
    val jitterMs: Float = 0f,
    val offsetMs: Float = 0f,
    val packetLossPercent: Float = 0f,
    val lastSeenMonotonicNanos: Long = 0L,
    val isSynchronized: Boolean = false
)

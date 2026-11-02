package com.lasync.audio.core.sync

import com.lasync.audio.core.audio.AudioOutputHardware

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
    val isSynchronized: Boolean = false,
    val outputHardware: AudioOutputHardware = AudioOutputHardware.SPEAKER,
    val delayTrimMs: Int = 0,
    val volumePercent: Int = 100,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false
)

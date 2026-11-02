package com.lasync.audio.core.audio

/**
 * TruePhase Precision Buffer Modes:
 * 1. ULTRA_LOW_LATENCY: 50ms ultra-low buffer tailored for real-time party sync and zero echo
 *    across multiple devices in the same room.
 * 2. LOW_LATENCY_VIDEO: 100ms buffer tailored for lip-sync accuracy when watching videos, movies,
 *    or gaming.
 * 3. ULTRA_STABLE_MUSIC: 300ms deep RAM buffer cushion for streaming across challenging or
 *    congested Wi-Fi networks.
 */
enum class SyncBufferMode(
    val id: Byte,
    val title: String,
    val leadTimeMs: Long,
    val bufferFrames: Int,
    val description: String
) {
    ULTRA_LOW_LATENCY(
        id = 3,
        title = "Ultra-Low Party",
        leadTimeMs = 50L,
        bufferFrames = 5,  // 5 frames * 10ms = 50ms
        description = "Zero room echo • 50ms ultra-low latency party sync"
    ),
    LOW_LATENCY_VIDEO(
        id = 2,
        title = "Low-Latency Video",
        leadTimeMs = 100L,
        bufferFrames = 10, // 10 frames * 10ms = 100ms
        description = "Lip-sync priority for YouTube / Movies • 100ms buffer"
    ),
    ULTRA_STABLE_MUSIC(
        id = 1,
        title = "Ultra-Stable Music",
        leadTimeMs = 300L,
        bufferFrames = 30, // 30 frames * 10ms = 300ms
        description = "Immune to Wi-Fi drops • 300ms RAM cushion"
    );

    val leadTimeNs: Long
        get() = leadTimeMs * 1_000_000L

    companion object {
        fun fromId(id: Byte): SyncBufferMode {
            return entries.firstOrNull { it.id == id } ?: ULTRA_LOW_LATENCY
        }
    }
}



package com.lasync.audio.core.audio

/**
 * TruePhase Dual Buffer Modes:
 * 1. ULTRA_STABLE_MUSIC: 1000ms deep RAM buffer cushion. Completely immune to Wi-Fi drops,
 *    interference, and jitter spikes when streaming Spotify, YouTube Music, or songs.
 * 2. LOW_LATENCY_VIDEO: 150ms buffer tailored for lip-sync accuracy when watching videos,
 *    movies, or gaming.
 */
enum class SyncBufferMode(
    val id: Byte,
    val title: String,
    val leadTimeMs: Long,
    val bufferFrames: Int,
    val description: String
) {
    ULTRA_STABLE_MUSIC(
        id = 1,
        title = "Ultra-Stable Music",
        leadTimeMs = 1000L,
        bufferFrames = 100, // 100 frames * 10ms = 1000ms
        description = "Immune to Wi-Fi drops • 1.0s deep RAM cushion"
    ),
    LOW_LATENCY_VIDEO(
        id = 2,
        title = "Low-Latency Video",
        leadTimeMs = 150L,
        bufferFrames = 15,  // 15 frames * 10ms = 150ms
        description = "Lip-sync priority for YouTube / Movies • 150ms buffer"
    );

    val leadTimeNs: Long
        get() = leadTimeMs * 1_000_000L

    companion object {
        fun fromId(id: Byte): SyncBufferMode {
            return entries.firstOrNull { it.id == id } ?: ULTRA_STABLE_MUSIC
        }
    }
}


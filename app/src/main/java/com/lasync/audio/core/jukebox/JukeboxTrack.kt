package com.lasync.audio.core.jukebox

/**
 * JukeboxTrack:
 * Represents a local audio track fully decoded into linear 16-bit PCM memory buffer.
 * Provides microsecond seek indexing and playback frame alignment.
 */
data class JukeboxTrack(
    val title: String,
    val artist: String,
    val durationMs: Long,
    val sampleRate: Int = 48000,
    val channelCount: Int = 2,
    val pcmBytes: ByteArray
) {
    val totalFrames: Long = if (channelCount > 0) (pcmBytes.size / (channelCount * 2)).toLong() else 0L

    fun getOffsetBytesForPositionMs(positionMs: Long): Int {
        val clampedMs = positionMs.coerceIn(0L, durationMs)
        val bytesPerMs = (sampleRate * channelCount * 2L) / 1000.0
        val target = (clampedMs * bytesPerMs).toInt()
        val frameSize = channelCount * 2
        val aligned = (target / frameSize) * frameSize
        return aligned.coerceIn(0, pcmBytes.size)
    }

    fun getPositionMsForOffsetBytes(offsetBytes: Int): Long {
        val bytesPerMs = (sampleRate * channelCount * 2L) / 1000.0
        if (bytesPerMs <= 0.0) return 0L
        return (offsetBytes / bytesPerMs).toLong().coerceIn(0L, durationMs)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as JukeboxTrack
        return title == other.title &&
               artist == other.artist &&
               durationMs == other.durationMs &&
               sampleRate == other.sampleRate &&
               channelCount == other.channelCount &&
               pcmBytes.size == other.pcmBytes.size
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + durationMs.hashCode()
        result = 31 * result + sampleRate.hashCode()
        result = 31 * result + channelCount.hashCode()
        result = 31 * result + pcmBytes.size.hashCode()
        return result
    }
}


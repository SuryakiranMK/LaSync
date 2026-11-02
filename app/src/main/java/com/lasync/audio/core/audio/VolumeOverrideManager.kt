package com.lasync.audio.core.audio

import android.content.Context
import android.media.AudioManager
import android.util.Log

/**
 * VolumeOverrideManager:
 * Safely forces Android audio output (STREAM_MUSIC) to 100% maximum volume during
 * emergency Megaphone / Walkie-Talkie broadcasts, and cleanly restores the receiver's
 * original volume level once the broadcast ends.
 */
class VolumeOverrideManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "VolumeOverrideManager"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var savedOriginalVolume: Int? = null

    @Volatile
    var isOverridden: Boolean = false
        private set

    /**
     * Overrides STREAM_MUSIC to the device's maximum hardware volume.
     * Records original volume level so it can be restored precisely.
     */
    @Synchronized
    fun overrideToMaxVolume(): Boolean {
        val am = audioManager ?: return false
        try {
            if (!isOverridden) {
                val currentVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                savedOriginalVolume = currentVol
                isOverridden = true
            }

            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)
            Log.i(TAG, "Megaphone volume overridden to MAX ($maxVol), saved original=$savedOriginalVolume")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to override stream volume: ${e.message}", e)
            return false
        }
    }

    /**
     * Restores STREAM_MUSIC back to the user's volume level before the Megaphone broadcast.
     */
    @Synchronized
    fun restoreOriginalVolume(): Boolean {
        val am = audioManager ?: return false
        if (!isOverridden) return false

        try {
            val original = savedOriginalVolume
            if (original != null) {
                am.setStreamVolume(AudioManager.STREAM_MUSIC, original, 0)
                Log.i(TAG, "Megaphone ended: volume restored back to $original")
            }
            savedOriginalVolume = null
            isOverridden = false
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore original volume: ${e.message}", e)
            return false
        }
    }
}

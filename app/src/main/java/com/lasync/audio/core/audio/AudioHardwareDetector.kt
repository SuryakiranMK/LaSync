package com.lasync.audio.core.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

/**
 * AudioHardwareDetector:
 * Automatically detects whether audio output is routed to built-in phone speakers,
 * Bluetooth A2DP/LE headphones/speakers, or wired headsets.
 * Used for SoundBoard mixer telemetry and latency compensation.
 */
object AudioHardwareDetector {

    fun getActiveOutputHardware(context: Context?): AudioOutputHardware {
        if (context == null) return AudioOutputHardware.SPEAKER

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return AudioOutputHardware.SPEAKER

            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (device in devices) {
                when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> return AudioOutputHardware.BLUETOOTH

                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> return AudioOutputHardware.WIRED

                    AudioDeviceInfo.TYPE_USB_HEADSET,
                    AudioDeviceInfo.TYPE_USB_DEVICE -> return AudioOutputHardware.USB
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                        device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                        device.type == AudioDeviceInfo.TYPE_BLE_BROADCAST
                    ) {
                        return AudioOutputHardware.BLUETOOTH
                    }
                }
            }
        } catch (_: Exception) {}

        return AudioOutputHardware.SPEAKER
    }
}

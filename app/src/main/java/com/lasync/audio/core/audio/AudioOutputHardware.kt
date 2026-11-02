package com.lasync.audio.core.audio

enum class AudioOutputHardware(val id: Int, val label: String, val iconEmoji: String) {
    SPEAKER(0, "Phone Speaker", "🔊"),
    BLUETOOTH(1, "Bluetooth", "🎧"),
    WIRED(2, "Wired Audio", "🎧"),
    USB(3, "USB DAC", "🔌"),
    UNKNOWN(4, "Default Out", "🔊");

    companion object {
        fun fromId(id: Int): AudioOutputHardware {
            return entries.firstOrNull { it.id == id } ?: SPEAKER
        }
    }
}

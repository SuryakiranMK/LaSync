package com.lasync.audio.core.hotspot

data class HotspotInfo(
    val ssid: String,
    val passphrase: String,
    val hostIp: String = "192.168.43.1",
    val qrPayload: String = "WIFI:S:$ssid;T:WPA;P:$passphrase;;"
)

enum class HotspotState {
    IDLE,
    STARTING,
    ACTIVE,
    ERROR
}

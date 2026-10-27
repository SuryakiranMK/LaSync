package com.lasync.audio.core.network

object NetworkConstants {
    const val MAGIC_CONTROL: Short = 0x4C53 // ASCII "LS"
    const val PROTOCOL_VERSION: Byte = 1

    const val PORT_CONTROL = 8888
    const val PORT_AUDIO = 8889

    const val TYPE_DISCOVERY_BEACON: Byte = 1
    const val TYPE_JOIN_REQUEST: Byte = 2
    const val TYPE_JOIN_ACCEPT: Byte = 3
    const val TYPE_PING: Byte = 4
    const val TYPE_PONG: Byte = 5
    const val TYPE_HEARTBEAT: Byte = 6
    const val TYPE_LEAVE: Byte = 7

    const val BEACON_INTERVAL_MS = 1000L
    const val PING_INTERVAL_FAST_MS = 200L
    const val PING_INTERVAL_STEADY_MS = 500L
    const val RECEIVER_TIMEOUT_MS = 3500L

    const val MAX_DATAGRAM_SIZE = 1400
}

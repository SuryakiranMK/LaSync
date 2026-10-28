package com.lasync.audio.core.network

import android.os.Build
import android.os.SystemClock
import com.lasync.audio.core.sync.ClockSyncEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.UUID

enum class ReceiverState {
    IDLE,
    DISCOVERING,
    CONNECTING,
    CONNECTED
}

data class DiscoveredHost(
    val sessionId: String,
    val hostName: String,
    val ipAddress: String,
    val lastSeenMonotonicNanos: Long
)

data class ReceiverUiState(
    val state: ReceiverState = ReceiverState.IDLE,
    val discoveredHosts: List<DiscoveredHost> = emptyList(),
    val connectedHostName: String = "",
    val sessionId: String = "",
    val rttMs: Float = 0f,
    val jitterMs: Float = 0f,
    val offsetMs: Float = 0f,
    val packetLossPercent: Float = 0f,
    val pingsCount: Long = 0L,
    val isSynchronized: Boolean = false,
    val isAudioPlaying: Boolean = false,
    val audioFramesReceived: Long = 0L
)

class ReceiverNetworkEngine(
    private val scope: CoroutineScope
) {
    private var socket: DatagramSocket? = null
    private var receiveJob: Job? = null
    private var pingJob: Job? = null
    private var heartbeatJob: Job? = null
    private var pruneDiscoveryJob: Job? = null

    val clockSyncEngine = ClockSyncEngine(windowSize = 12)
    val audioEngine = com.lasync.audio.core.audio.ReceiverAudioEngine(scope, clockSyncEngine)
    private val discoveredHostsMap = mutableMapOf<String, DiscoveredHost>()

    private val clientId = UUID.randomUUID().toString().take(8).uppercase()
    private val clientDeviceName = Build.MODEL ?: "Receiver Phone"

    private var targetHostAddress: InetAddress? = null
    private var targetSessionId = ""
    private var pingSequence = 0L

    private val _uiState = MutableStateFlow(ReceiverUiState())
    val uiState: StateFlow<ReceiverUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            launch {
                audioEngine.isAudioPlaying.collect { isPlaying ->
                    _uiState.value = _uiState.value.copy(isAudioPlaying = isPlaying)
                }
            }
            launch {
                audioEngine.framesReceived.collect { frames ->
                    _uiState.value = _uiState.value.copy(audioFramesReceived = frames)
                }
            }
        }
    }

    fun startDiscovery() {
        if (socket != null && !socket!!.isClosed) return

        try {
            socket = DatagramSocket(null).apply {
                reuseAddress = true
                bind(java.net.InetSocketAddress(NetworkConstants.PORT_CONTROL))
                broadcast = true
                receiveBufferSize = 512 * 1024
                sendBufferSize = 512 * 1024
            }
        } catch (_: Exception) {
            // Port 8888 might be in use if running on same device during testing; fall back to ephemeral port
            try {
                socket = DatagramSocket().apply {
                    broadcast = true
                    receiveBufferSize = 512 * 1024
                    sendBufferSize = 512 * 1024
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return
            }
        }

        _uiState.value = ReceiverUiState(state = ReceiverState.DISCOVERING)
        startListening()
        startPruningDiscovery()
    }

    private fun startListening() {
        receiveJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(NetworkConstants.MAX_DATAGRAM_SIZE)
            val packet = DatagramPacket(buffer, buffer.size)

            while (isActive && socket != null && !socket!!.isClosed) {
                try {
                    socket?.receive(packet)
                    val t4 = SystemClock.elapsedRealtimeNanos()
                    val controlPacket = ControlPacket.fromByteArray(packet.data, packet.length)
                    if (controlPacket != null) {
                        handlePacket(controlPacket, packet.address, packet.port, t4)
                    }
                } catch (_: Exception) {
                    // Socket closed
                }
            }
        }
    }

    private fun handlePacket(
        packet: ControlPacket,
        senderAddress: InetAddress,
        senderPort: Int,
        t4Nanos: Long
    ) {
        when (packet.type) {
            NetworkConstants.TYPE_DISCOVERY_BEACON -> {
                val hostName = if (packet.payload.isNotEmpty()) {
                    String(packet.payload, StandardCharsets.UTF_8)
                } else {
                    "Host (${senderAddress.hostAddress})"
                }

                val discovered = DiscoveredHost(
                    sessionId = packet.sessionId,
                    hostName = hostName,
                    ipAddress = senderAddress.hostAddress ?: "Unknown",
                    lastSeenMonotonicNanos = t4Nanos
                )
                discoveredHostsMap[packet.sessionId] = discovered
                _uiState.value = _uiState.value.copy(
                    discoveredHosts = discoveredHostsMap.values.toList()
                )
            }

            NetworkConstants.TYPE_JOIN_ACCEPT -> {
                if (packet.sessionId == targetSessionId) {
                    targetHostAddress = senderAddress
                    val hostName = if (packet.payload.isNotEmpty()) {
                        String(packet.payload, StandardCharsets.UTF_8)
                    } else "Host"

                    _uiState.value = _uiState.value.copy(
                        state = ReceiverState.CONNECTED,
                        connectedHostName = hostName,
                        sessionId = packet.sessionId
                    )

                    audioEngine.startListening()
                    startPinging()
                    startHeartbeat()
                }
            }

            NetworkConstants.TYPE_PONG -> {
                if (_uiState.value.state == ReceiverState.CONNECTED) {
                    clockSyncEngine.processPong(
                        t1 = packet.t1,
                        t2 = packet.t2,
                        t3 = packet.t3,
                        t4 = t4Nanos,
                        seq = packet.sequenceNumber
                    )

                    val rtt = clockSyncEngine.getAverageRttMs()
                    val jitter = clockSyncEngine.getJitterMs()
                    val offset = (clockSyncEngine.getFilteredOffsetNanos() / 1_000_000.0).toFloat()
                    val loss = clockSyncEngine.getPacketLossPercent()

                    _uiState.value = _uiState.value.copy(
                        rttMs = rtt,
                        jitterMs = jitter,
                        offsetMs = offset,
                        packetLossPercent = loss,
                        pingsCount = pingSequence,
                        isSynchronized = rtt > 0f && rtt < 25f
                    )
                }
            }
        }
    }

    fun joinSession(discoveredHost: DiscoveredHost) {
        targetHostAddress = InetAddress.getByName(discoveredHost.ipAddress)
        targetSessionId = discoveredHost.sessionId

        _uiState.value = _uiState.value.copy(
            state = ReceiverState.CONNECTING,
            sessionId = targetSessionId,
            connectedHostName = discoveredHost.hostName
        )

        sendJoinRequest()
    }

    fun joinBySessionId(sessionId: String, hostIp: String) {
        targetHostAddress = InetAddress.getByName(hostIp)
        targetSessionId = sessionId.uppercase().trim()

        _uiState.value = _uiState.value.copy(
            state = ReceiverState.CONNECTING,
            sessionId = targetSessionId,
            connectedHostName = "Host ($hostIp)"
        )

        sendJoinRequest()
    }

    private fun sendJoinRequest() {
        scope.launch(Dispatchers.IO) {
            val hostAddr = targetHostAddress ?: return@launch
            val joinPacket = ControlPacket(
                type = NetworkConstants.TYPE_JOIN_REQUEST,
                sessionId = targetSessionId,
                sequenceNumber = 0L,
                senderId = clientId,
                payload = clientDeviceName.toByteArray(StandardCharsets.UTF_8)
            )

            // Send multiple join packets in quick succession to avoid initial UDP packet drop
            for (i in 0 until 3) {
                if (_uiState.value.state == ReceiverState.CONNECTED) break
                sendPacket(joinPacket, hostAddr, NetworkConstants.PORT_CONTROL)
                delay(300L)
            }
        }
    }

    private fun startPinging() {
        pingJob?.cancel()
        pingJob = scope.launch(Dispatchers.IO) {
            val hostAddr = targetHostAddress ?: return@launch

            while (isActive && _uiState.value.state == ReceiverState.CONNECTED) {
                pingSequence++
                val t1 = SystemClock.elapsedRealtimeNanos()
                clockSyncEngine.onPingSent(pingSequence)

                val pingPacket = ControlPacket(
                    type = NetworkConstants.TYPE_PING,
                    sessionId = targetSessionId,
                    sequenceNumber = pingSequence,
                    senderId = clientId,
                    t1 = t1
                )
                sendPacket(pingPacket, hostAddr, NetworkConstants.PORT_CONTROL)

                // Fast pinging initially, then relax to steady state
                val interval = if (pingSequence < 20) {
                    NetworkConstants.PING_INTERVAL_FAST_MS
                } else {
                    NetworkConstants.PING_INTERVAL_STEADY_MS
                }
                delay(interval)
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch(Dispatchers.IO) {
            val hostAddr = targetHostAddress ?: return@launch

            while (isActive && _uiState.value.state == ReceiverState.CONNECTED) {
                delay(1000L)

                // Package telemetry: [rtt, jitter, offset, loss]
                val bb = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN)
                bb.putFloat(clockSyncEngine.getAverageRttMs())
                bb.putFloat(clockSyncEngine.getJitterMs())
                bb.putFloat((clockSyncEngine.getFilteredOffsetNanos() / 1_000_000.0).toFloat())
                bb.putFloat(clockSyncEngine.getPacketLossPercent())

                val heartbeatPacket = ControlPacket(
                    type = NetworkConstants.TYPE_HEARTBEAT,
                    sessionId = targetSessionId,
                    sequenceNumber = pingSequence,
                    senderId = clientId,
                    payload = bb.array()
                )
                sendPacket(heartbeatPacket, hostAddr, NetworkConstants.PORT_CONTROL)
            }
        }
    }

    private fun sendPacket(packet: ControlPacket, targetAddress: InetAddress, targetPort: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                val bytes = packet.toByteArray()
                val datagram = DatagramPacket(bytes, bytes.size, targetAddress, targetPort)
                socket?.send(datagram)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startPruningDiscovery() {
        pruneDiscoveryJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(2000L)
                val now = SystemClock.elapsedRealtimeNanos()
                val timeout = 4000L * 1_000_000L
                val expired = discoveredHostsMap.filter { now - it.value.lastSeenMonotonicNanos > timeout }.keys
                if (expired.isNotEmpty()) {
                    expired.forEach { discoveredHostsMap.remove(it) }
                    _uiState.value = _uiState.value.copy(
                        discoveredHosts = discoveredHostsMap.values.toList()
                    )
                }
            }
        }
    }

    fun disconnect() {
        targetHostAddress?.let { hostAddr ->
            val leavePacket = ControlPacket(
                type = NetworkConstants.TYPE_LEAVE,
                sessionId = targetSessionId,
                sequenceNumber = 0L,
                senderId = clientId
            )
            sendPacket(leavePacket, hostAddr, NetworkConstants.PORT_CONTROL)
        }

        audioEngine.stopListening()
        pingJob?.cancel()
        heartbeatJob?.cancel()
        clockSyncEngine.reset()
        _uiState.value = _uiState.value.copy(
            state = ReceiverState.DISCOVERING,
            rttMs = 0f,
            jitterMs = 0f,
            offsetMs = 0f,
            isSynchronized = false,
            isAudioPlaying = false
        )
    }

    fun stop() {
        disconnect()
        receiveJob?.cancel()
        pruneDiscoveryJob?.cancel()
        socket?.close()
        socket = null
        discoveredHostsMap.clear()
        _uiState.value = ReceiverUiState(state = ReceiverState.IDLE)
    }
}

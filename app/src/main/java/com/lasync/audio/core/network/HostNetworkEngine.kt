package com.lasync.audio.core.network

import android.os.Build
import android.os.SystemClock
import com.lasync.audio.core.sync.ReceiverNode
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
import java.util.concurrent.ConcurrentHashMap

data class HostUiState(
    val sessionId: String = "",
    val localIp: String = "",
    val isRunning: Boolean = false,
    val receivers: List<ReceiverNode> = emptyList(),
    val totalPingsAnswered: Long = 0L,
    val isAudioStreaming: Boolean = false,
    val audioFramesSent: Long = 0L,
    val activeAudioSource: com.lasync.audio.core.audio.HostAudioSource = com.lasync.audio.core.audio.HostAudioSource.IDLE
)

class HostNetworkEngine(
    private val scope: CoroutineScope
) {
    private var socket: DatagramSocket? = null
    private var beaconJob: Job? = null
    private var receiveJob: Job? = null
    private var pruneJob: Job? = null
    private var audioObserverJob: Job? = null

    private val receiversMap = ConcurrentHashMap<String, ReceiverNode>()
    private var sessionId = ""
    private var hostDeviceName = Build.MODEL ?: "Host Phone"
    private var pingsCount = 0L

    val audioEngine = com.lasync.audio.core.audio.HostAudioEngine(
        scope = scope,
        getSessionId = { sessionId },
        getTargetIps = { receiversMap.values.map { it.ipAddress } }
    )

    private val _uiState = MutableStateFlow(HostUiState())
    val uiState: StateFlow<HostUiState> = _uiState.asStateFlow()

    init {
        audioObserverJob = scope.launch {
            launch {
                audioEngine.isStreaming.collect { isStreaming ->
                    _uiState.value = _uiState.value.copy(isAudioStreaming = isStreaming)
                }
            }
            launch {
                audioEngine.framesSent.collect { frames ->
                    _uiState.value = _uiState.value.copy(audioFramesSent = frames)
                }
            }
            launch {
                audioEngine.activeSource.collect { source ->
                    _uiState.value = _uiState.value.copy(activeAudioSource = source)
                }
            }
        }
    }

    fun startToneTest() {
        audioEngine.startTone()
    }

    fun stopToneTest() {
        audioEngine.stopTone()
    }

    fun startSystemAudio(mediaProjection: android.media.projection.MediaProjection) {
        audioEngine.startSystemAudio(mediaProjection)
    }

    fun stopSystemAudio() {
        audioEngine.stopSystemAudio()
    }

    fun stopAllAudio() {
        audioEngine.stopAll()
    }

    fun start(customSessionId: String? = null) {
        if (socket != null && !socket!!.isClosed) return

        sessionId = customSessionId ?: NetworkUtils.generateSessionId()
        val localIp = NetworkUtils.getLocalIpAddress() ?: "0.0.0.0"

        try {
            socket = DatagramSocket(NetworkConstants.PORT_CONTROL).apply {
                broadcast = true
                sendBufferSize = 512 * 1024
                receiveBufferSize = 512 * 1024
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        _uiState.value = HostUiState(
            sessionId = sessionId,
            localIp = localIp,
            isRunning = true,
            receivers = emptyList()
        )

        startReceiving()
        startBeacon()
        startPruning()
    }

    private fun startReceiving() {
        receiveJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(NetworkConstants.MAX_DATAGRAM_SIZE)
            val packet = DatagramPacket(buffer, buffer.size)

            while (isActive && socket != null && !socket!!.isClosed) {
                try {
                    socket?.receive(packet)
                    val t2 = SystemClock.elapsedRealtimeNanos()
                    val controlPacket = ControlPacket.fromByteArray(packet.data, packet.length)
                    if (controlPacket != null) {
                        handleIncomingPacket(controlPacket, packet.address, packet.port, t2)
                    }
                } catch (_: Exception) {
                    // Socket closed or timeout
                }
            }
        }
    }

    private fun handleIncomingPacket(
        packet: ControlPacket,
        senderAddress: InetAddress,
        senderPort: Int,
        t2Nanos: Long
    ) {
        when (packet.type) {
            NetworkConstants.TYPE_JOIN_REQUEST -> {
                val clientDeviceName = if (packet.payload.isNotEmpty()) {
                    String(packet.payload, StandardCharsets.UTF_8)
                } else {
                    "Receiver ${packet.senderId.take(4)}"
                }

                val node = ReceiverNode(
                    id = packet.senderId,
                    name = clientDeviceName,
                    ipAddress = senderAddress.hostAddress ?: "Unknown",
                    port = senderPort,
                    lastSeenMonotonicNanos = t2Nanos
                )
                receiversMap[packet.senderId] = node
                updateUiState()

                // Reply with JOIN_ACCEPT
                val acceptPacket = ControlPacket(
                    type = NetworkConstants.TYPE_JOIN_ACCEPT,
                    sessionId = sessionId,
                    sequenceNumber = 0L,
                    senderId = "HOST",
                    payload = hostDeviceName.toByteArray(StandardCharsets.UTF_8)
                )
                sendPacket(acceptPacket, senderAddress, senderPort)
            }

            NetworkConstants.TYPE_PING -> {
                pingsCount++
                val t3 = SystemClock.elapsedRealtimeNanos()
                val pongPacket = ControlPacket(
                    type = NetworkConstants.TYPE_PONG,
                    sessionId = sessionId,
                    sequenceNumber = packet.sequenceNumber,
                    senderId = "HOST",
                    t1 = packet.t1,
                    t2 = t2Nanos,
                    t3 = t3
                )
                sendPacket(pongPacket, senderAddress, senderPort)

                // Update last seen
                receiversMap[packet.senderId]?.let { existing ->
                    receiversMap[packet.senderId] = existing.copy(
                        lastSeenMonotonicNanos = t2Nanos
                    )
                }
            }

            NetworkConstants.TYPE_HEARTBEAT -> {
                // Parse telemetry metrics from payload
                // Format: Float(rttMs), Float(jitterMs), Float(offsetMs), Float(packetLoss)
                if (packet.payload.size >= 16) {
                    val bb = ByteBuffer.wrap(packet.payload).order(ByteOrder.BIG_ENDIAN)
                    val rtt = bb.float
                    val jitter = bb.float
                    val offset = bb.float
                    val loss = bb.float

                    receiversMap[packet.senderId]?.let { existing ->
                        receiversMap[packet.senderId] = existing.copy(
                            rttMs = rtt,
                            jitterMs = jitter,
                            offsetMs = offset,
                            packetLossPercent = loss,
                            lastSeenMonotonicNanos = t2Nanos,
                            isSynchronized = rtt > 0f && rtt < 25f
                        )
                        updateUiState()
                    }
                }
            }

            NetworkConstants.TYPE_LEAVE -> {
                receiversMap.remove(packet.senderId)
                updateUiState()
            }
        }
    }

    private fun sendPacket(controlPacket: ControlPacket, targetAddress: InetAddress, targetPort: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                val bytes = controlPacket.toByteArray()
                val datagram = DatagramPacket(bytes, bytes.size, targetAddress, targetPort)
                socket?.send(datagram)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startBeacon() {
        beaconJob = scope.launch(Dispatchers.IO) {
            val broadcastAddr = NetworkUtils.getBroadcastAddress()
            val beaconPayload = hostDeviceName.toByteArray(StandardCharsets.UTF_8)

            while (isActive) {
                val beacon = ControlPacket(
                    type = NetworkConstants.TYPE_DISCOVERY_BEACON,
                    sessionId = sessionId,
                    sequenceNumber = 0L,
                    senderId = "HOST",
                    payload = beaconPayload
                )
                sendPacket(beacon, broadcastAddr, NetworkConstants.PORT_CONTROL)
                delay(NetworkConstants.BEACON_INTERVAL_MS)
            }
        }
    }

    private fun startPruning() {
        pruneJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(1000L)
                val now = SystemClock.elapsedRealtimeNanos()
                val timeoutNanos = NetworkConstants.RECEIVER_TIMEOUT_MS * 1_000_000L
                var changed = false

                receiversMap.forEach { (id, node) ->
                    if (now - node.lastSeenMonotonicNanos > timeoutNanos) {
                        receiversMap.remove(id)
                        changed = true
                    }
                }

                if (changed) {
                    updateUiState()
                }
            }
        }
    }

    private fun updateUiState() {
        _uiState.value = _uiState.value.copy(
            receivers = receiversMap.values.toList().sortedBy { it.name },
            totalPingsAnswered = pingsCount
        )
    }

    fun stop() {
        audioEngine.stopAll()
        beaconJob?.cancel()
        receiveJob?.cancel()
        pruneJob?.cancel()
        socket?.close()
        socket = null
        receiversMap.clear()
        _uiState.value = HostUiState(isRunning = false)
    }
}

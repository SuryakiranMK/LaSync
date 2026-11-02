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
    val audioFramesReceived: Long = 0L,
    val bufferMode: com.lasync.audio.core.audio.SyncBufferMode = com.lasync.audio.core.audio.SyncBufferMode.ULTRA_LOW_LATENCY,
    val hardwareLatencyMs: Float = 0f,
    val phaseErrorMs: Float = 0f,
    val isPhaseLocked: Boolean = false,
    val isHardwareTimestampSupported: Boolean = false,
    val isMegaphoneActive: Boolean = false,
    val outputHardware: com.lasync.audio.core.audio.AudioOutputHardware = com.lasync.audio.core.audio.AudioOutputHardware.SPEAKER,
    val delayTrimMs: Int = 0,
    val remoteVolumePercent: Int = 100,
    val isMutedByHost: Boolean = false,
    val isJukeboxDownloading: Boolean = false,
    val jukeboxDownloadProgress: Float = 0f,
    val jukeboxTrackTitle: String = "",
    val isJukeboxReady: Boolean = false,
    val isJukeboxPlaying: Boolean = false,
    val jukeboxPositionMs: Long = 0L,
    val jukeboxDurationMs: Long = 0L,
    val jukeboxProgress: Float = 0f
)

class ReceiverNetworkEngine(
    private val scope: CoroutineScope,
    private val context: android.content.Context? = null
) {
    private var socket: DatagramSocket? = null
    private var receiveJob: Job? = null
    private var pingJob: Job? = null
    private var heartbeatJob: Job? = null
    private var pruneDiscoveryJob: Job? = null

    val clockSyncEngine = ClockSyncEngine(windowSize = 12)
    val audioEngine = com.lasync.audio.core.audio.ReceiverAudioEngine(scope, clockSyncEngine, context)
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
            launch {
                audioEngine.activeBufferMode.collect { mode ->
                    _uiState.value = _uiState.value.copy(bufferMode = mode)
                }
            }
            launch {
                audioEngine.hardwareLatencyMs.collect { lat ->
                    _uiState.value = _uiState.value.copy(hardwareLatencyMs = lat)
                }
            }
            launch {
                audioEngine.phaseErrorMs.collect { err ->
                    _uiState.value = _uiState.value.copy(phaseErrorMs = err)
                }
            }
            launch {
                audioEngine.isPhaseLocked.collect { locked ->
                    _uiState.value = _uiState.value.copy(isPhaseLocked = locked)
                }
            }
            launch {
                audioEngine.isHardwareTimestampSupported.collect { supported ->
                    _uiState.value = _uiState.value.copy(isHardwareTimestampSupported = supported)
                }
            }
            launch {
                audioEngine.isMegaphoneActive.collect { isMegaphone ->
                    _uiState.value = _uiState.value.copy(isMegaphoneActive = isMegaphone)
                }
            }
            launch {
                audioEngine.jukeboxPlaybackEngine.isPlayingState.collect { playing ->
                    _uiState.value = _uiState.value.copy(isJukeboxPlaying = playing)
                }
            }
            launch {
                audioEngine.jukeboxPlaybackEngine.playbackPositionMs.collect { pos ->
                    _uiState.value = _uiState.value.copy(jukeboxPositionMs = pos)
                }
            }
            launch {
                audioEngine.jukeboxPlaybackEngine.playbackProgress.collect { prog ->
                    _uiState.value = _uiState.value.copy(jukeboxProgress = prog)
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

            NetworkConstants.TYPE_MEGAPHONE_START -> {
                audioEngine.onMegaphoneStarted()
                _uiState.value = _uiState.value.copy(isMegaphoneActive = true)
            }

            NetworkConstants.TYPE_MEGAPHONE_STOP -> {
                audioEngine.onMegaphoneStopped()
                _uiState.value = _uiState.value.copy(isMegaphoneActive = false)
            }

            NetworkConstants.TYPE_REMOTE_CONFIG -> {
                if (packet.payload.size >= 9) {
                    val bb = ByteBuffer.wrap(packet.payload).order(ByteOrder.BIG_ENDIAN)
                    val trimMs = bb.int
                    val volPercent = bb.int
                    val isMuted = bb.get().toInt() == 1

                    audioEngine.setDelayTrimMs(trimMs)
                    audioEngine.setRemoteVolume(volPercent, isMuted)

                    _uiState.value = _uiState.value.copy(
                        delayTrimMs = trimMs,
                        remoteVolumePercent = volPercent,
                        isMutedByHost = isMuted
                    )
                }
            }

            NetworkConstants.TYPE_REMOTE_DISCONNECT -> {
                audioEngine.stopAudioStream()
                stop()
                _uiState.value = ReceiverUiState(state = ReceiverState.IDLE)
            }

            NetworkConstants.TYPE_AUDIO_STOP -> {
                audioEngine.stopAudioStream()
            }

            NetworkConstants.TYPE_JUKEBOX_PRELOAD_START -> {
                val hostIp = senderAddress.hostAddress ?: targetHostAddress?.hostAddress
                if (hostIp != null) {
                    _uiState.value = _uiState.value.copy(
                        isJukeboxDownloading = true,
                        jukeboxDownloadProgress = 0f,
                        isJukeboxReady = false
                    )
                    scope.launch(Dispatchers.IO) {
                        val track = com.lasync.audio.core.jukebox.JukeboxClientDownloader.downloadTrackFromHost(hostIp) { progress ->
                            _uiState.value = _uiState.value.copy(jukeboxDownloadProgress = progress)
                        }
                        if (track != null) {
                            audioEngine.setJukeboxTrack(track)
                            _uiState.value = _uiState.value.copy(
                                isJukeboxDownloading = false,
                                isJukeboxReady = true,
                                jukeboxTrackTitle = track.title,
                                jukeboxDurationMs = track.durationMs
                            )
                            // Notify Host that this receiver is loaded and ready
                            val readyPacket = ControlPacket(
                                type = NetworkConstants.TYPE_JUKEBOX_READY,
                                sessionId = targetSessionId,
                                sequenceNumber = 0L,
                                senderId = clientId
                            )
                            sendPacket(readyPacket, senderAddress, NetworkConstants.PORT_CONTROL)
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isJukeboxDownloading = false,
                                isJukeboxReady = false
                            )
                        }
                    }
                }
            }

            NetworkConstants.TYPE_JUKEBOX_PLAY -> {
                audioEngine.playJukebox(packet.t1, packet.t2)
            }

            NetworkConstants.TYPE_JUKEBOX_PAUSE -> {
                audioEngine.pauseJukebox()
            }

            NetworkConstants.TYPE_JUKEBOX_STOP -> {
                audioEngine.stopJukebox()
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

                // Package telemetry: [rtt, jitter, offset, loss, outputHardware, delayTrimMs]
                val currentHw = com.lasync.audio.core.audio.AudioHardwareDetector.getActiveOutputHardware(context)
                val currentTrim = audioEngine.delayTrimMs
                _uiState.value = _uiState.value.copy(
                    outputHardware = currentHw,
                    delayTrimMs = currentTrim,
                    remoteVolumePercent = audioEngine.remoteVolumePercent,
                    isMutedByHost = audioEngine.isMutedByHost
                )

                val bb = ByteBuffer.allocate(21).order(ByteOrder.BIG_ENDIAN)
                bb.putFloat(clockSyncEngine.getAverageRttMs())
                bb.putFloat(clockSyncEngine.getJitterMs())
                bb.putFloat((clockSyncEngine.getFilteredOffsetNanos() / 1_000_000.0).toFloat())
                bb.putFloat(clockSyncEngine.getPacketLossPercent())
                bb.put(currentHw.id.toByte())
                bb.putInt(currentTrim)

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
            isAudioPlaying = false,
            isJukeboxPlaying = false,
            isJukeboxReady = false,
            isJukeboxDownloading = false,
            jukeboxProgress = 0f,
            jukeboxPositionMs = 0L
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

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
    val activeAudioSource: com.lasync.audio.core.audio.HostAudioSource = com.lasync.audio.core.audio.HostAudioSource.IDLE,
    val bufferMode: com.lasync.audio.core.audio.SyncBufferMode = com.lasync.audio.core.audio.SyncBufferMode.ULTRA_LOW_LATENCY,
    val isMegaphoneActive: Boolean = false,
    val loadedJukeboxTrack: com.lasync.audio.core.jukebox.JukeboxTrack? = null,
    val isJukeboxDecoding: Boolean = false,
    val jukeboxDecodeProgress: Float = 0f,
    val isJukeboxPlaying: Boolean = false,
    val jukeboxPositionMs: Long = 0L,
    val jukeboxProgress: Float = 0f,
    val readyReceiversCount: Int = 0
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
            launch {
                audioEngine.bufferMode.collect { mode ->
                    _uiState.value = _uiState.value.copy(bufferMode = mode)
                }
            }
            launch {
                audioEngine.isMegaphoneActive.collect { isMegaphone ->
                    _uiState.value = _uiState.value.copy(isMegaphoneActive = isMegaphone)
                }
            }
            launch {
                audioEngine.loadedJukeboxTrack.collect { track ->
                    _uiState.value = _uiState.value.copy(loadedJukeboxTrack = track)
                }
            }
            launch {
                audioEngine.isJukeboxDecoding.collect { decoding ->
                    _uiState.value = _uiState.value.copy(isJukeboxDecoding = decoding)
                }
            }
            launch {
                audioEngine.jukeboxDecodeProgress.collect { prog ->
                    _uiState.value = _uiState.value.copy(jukeboxDecodeProgress = prog)
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

    fun startMegaphone(context: android.content.Context): Boolean {
        val started = audioEngine.startMegaphone(context)
        if (started) {
            broadcastControlPacket(NetworkConstants.TYPE_MEGAPHONE_START)
        }
        return started
    }

    fun stopMegaphone() {
        audioEngine.stopMegaphone()
        broadcastControlPacket(NetworkConstants.TYPE_MEGAPHONE_STOP)
    }

    private fun broadcastControlPacket(type: Byte) {
        val packet = ControlPacket(
            type = type,
            sessionId = sessionId,
            sequenceNumber = 0L,
            senderId = "HOST"
        )
        broadcastControlPacket(packet)
    }

    private fun broadcastControlPacket(packet: ControlPacket) {
        // Send to all registered receivers
        for (receiver in receiversMap.values) {
            try {
                val addr = InetAddress.getByName(receiver.ipAddress)
                sendPacket(packet, addr, receiver.port)
            } catch (_: Exception) {}
        }
        // Also broadcast to subnet
        try {
            val broadcastAddr = NetworkUtils.getBroadcastAddress()
            sendPacket(packet, broadcastAddr, NetworkConstants.PORT_CONTROL)
        } catch (_: Exception) {}
    }

    fun setBufferMode(mode: com.lasync.audio.core.audio.SyncBufferMode) {
        audioEngine.setBufferMode(mode)
    }

    fun startToneTest() {
        audioEngine.startTone()
    }

    fun stopToneTest() {
        audioEngine.stopTone()
        broadcastControlPacket(NetworkConstants.TYPE_AUDIO_STOP)
    }

    fun startSystemAudio(mediaProjection: android.media.projection.MediaProjection) {
        audioEngine.startSystemAudio(mediaProjection)
    }

    fun stopSystemAudio() {
        audioEngine.stopSystemAudio()
        broadcastControlPacket(NetworkConstants.TYPE_AUDIO_STOP)
    }

    fun stopAllAudio() {
        audioEngine.stopAll()
        broadcastControlPacket(NetworkConstants.TYPE_AUDIO_STOP)
    }

    private val readyReceiversSet = ConcurrentHashMap.newKeySet<String>()

    suspend fun loadJukeboxFile(context: android.content.Context, uri: android.net.Uri): com.lasync.audio.core.jukebox.JukeboxTrack? {
        val track = audioEngine.loadJukeboxTrack(context, uri)
        if (track != null) {
            readyReceiversSet.clear()
            _uiState.value = _uiState.value.copy(readyReceiversCount = 0)
            broadcastControlPacket(NetworkConstants.TYPE_JUKEBOX_PRELOAD_START)
        }
        return track
    }

    fun playJukebox(startOffsetMs: Long = 0L) {
        val now = SystemClock.elapsedRealtimeNanos()
        val scheduledStartNanos = now + 150_000_000L // 150ms lead time for scheduled phase-locked trigger
        audioEngine.playJukebox(scheduledStartNanos, startOffsetMs)

        val packet = ControlPacket(
            type = NetworkConstants.TYPE_JUKEBOX_PLAY,
            sessionId = sessionId,
            sequenceNumber = 0L,
            senderId = "HOST",
            t1 = scheduledStartNanos,
            t2 = startOffsetMs
        )
        broadcastControlPacket(packet)
    }

    fun pauseJukebox() {
        audioEngine.pauseJukebox()
        broadcastControlPacket(NetworkConstants.TYPE_JUKEBOX_PAUSE)
    }

    fun stopJukebox() {
        audioEngine.stopJukebox()
        broadcastControlPacket(NetworkConstants.TYPE_JUKEBOX_STOP)
    }

    fun seekJukebox(positionMs: Long) {
        playJukebox(positionMs)
    }

    // SoundBoard Mixer Console Actions
    fun setReceiverDelayTrim(receiverId: String, trimMs: Int) {
        val clamped = trimMs.coerceIn(-250, 350)
        receiversMap[receiverId]?.let { node ->
            receiversMap[receiverId] = node.copy(delayTrimMs = clamped)
            sendRemoteConfigToReceiver(receiverId)
            updateUiState()
        }
    }

    fun adjustReceiverDelayTrim(receiverId: String, deltaMs: Int) {
        receiversMap[receiverId]?.let { node ->
            setReceiverDelayTrim(receiverId, node.delayTrimMs + deltaMs)
        }
    }

    fun setReceiverVolume(receiverId: String, volumePercent: Int) {
        val clamped = volumePercent.coerceIn(0, 100)
        receiversMap[receiverId]?.let { node ->
            receiversMap[receiverId] = node.copy(volumePercent = clamped)
            sendRemoteConfigToReceiver(receiverId)
            updateUiState()
        }
    }

    fun adjustReceiverVolume(receiverId: String, deltaPercent: Int) {
        receiversMap[receiverId]?.let { node ->
            setReceiverVolume(receiverId, node.volumePercent + deltaPercent)
        }
    }

    fun toggleReceiverMute(receiverId: String) {
        receiversMap[receiverId]?.let { node ->
            receiversMap[receiverId] = node.copy(isMuted = !node.isMuted)
            sendRemoteConfigToReceiver(receiverId)
            updateUiState()
        }
    }

    fun toggleReceiverSolo(receiverId: String) {
        val targetNode = receiversMap[receiverId] ?: return
        val currentlySolo = targetNode.isSolo

        if (currentlySolo) {
            // Un-solo: clear solo and unmute all
            receiversMap.forEach { (id, node) ->
                receiversMap[id] = node.copy(isSolo = false, isMuted = false)
                sendRemoteConfigToReceiver(id)
            }
        } else {
            // Solo this receiver: mute all others
            receiversMap.forEach { (id, node) ->
                val isTarget = (id == receiverId)
                receiversMap[id] = node.copy(isSolo = isTarget, isMuted = !isTarget)
                sendRemoteConfigToReceiver(id)
            }
        }
        updateUiState()
    }

    fun disconnectReceiver(receiverId: String) {
        val node = receiversMap.remove(receiverId) ?: return
        updateUiState()
        val packet = ControlPacket(
            type = NetworkConstants.TYPE_REMOTE_DISCONNECT,
            sessionId = sessionId,
            sequenceNumber = 0L,
            senderId = "HOST"
        )
        try {
            val addr = InetAddress.getByName(node.ipAddress)
            sendPacket(packet, addr, node.port)
        } catch (_: Exception) {}
    }

    private fun sendRemoteConfigToReceiver(receiverId: String) {
        val node = receiversMap[receiverId] ?: return
        val bb = ByteBuffer.allocate(9).order(ByteOrder.BIG_ENDIAN)
        bb.putInt(node.delayTrimMs)
        bb.putInt(node.volumePercent)
        bb.put(if (node.isMuted) 1.toByte() else 0.toByte())
        val packet = ControlPacket(
            type = NetworkConstants.TYPE_REMOTE_CONFIG,
            sessionId = sessionId,
            sequenceNumber = 0L,
            senderId = "HOST",
            payload = bb.array()
        )
        try {
            val addr = InetAddress.getByName(node.ipAddress)
            sendPacket(packet, addr, node.port)
        } catch (_: Exception) {}
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
                // Format: Float(rttMs), Float(jitterMs), Float(offsetMs), Float(packetLoss), [Byte(outputHw), Int(activeTrim)]
                if (packet.payload.size >= 16) {
                    val bb = ByteBuffer.wrap(packet.payload).order(ByteOrder.BIG_ENDIAN)
                    val rtt = bb.float
                    val jitter = bb.float
                    val offset = bb.float
                    val loss = bb.float

                    val outputHw = if (bb.hasRemaining()) {
                        com.lasync.audio.core.audio.AudioOutputHardware.fromId(bb.get().toInt())
                    } else {
                        com.lasync.audio.core.audio.AudioOutputHardware.SPEAKER
                    }

                    receiversMap[packet.senderId]?.let { existing ->
                        receiversMap[packet.senderId] = existing.copy(
                            rttMs = rtt,
                            jitterMs = jitter,
                            offsetMs = offset,
                            packetLossPercent = loss,
                            outputHardware = outputHw,
                            lastSeenMonotonicNanos = t2Nanos,
                            isSynchronized = rtt > 0f && rtt < 25f
                        )
                        updateUiState()
                    }
                }
            }

            NetworkConstants.TYPE_LEAVE -> {
                receiversMap.remove(packet.senderId)
                readyReceiversSet.remove(packet.senderId)
                _uiState.value = _uiState.value.copy(readyReceiversCount = readyReceiversSet.size)
                updateUiState()
            }

            NetworkConstants.TYPE_JUKEBOX_READY -> {
                readyReceiversSet.add(packet.senderId)
                _uiState.value = _uiState.value.copy(readyReceiversCount = readyReceiversSet.size)
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
        broadcastControlPacket(NetworkConstants.TYPE_AUDIO_STOP)
        broadcastControlPacket(NetworkConstants.TYPE_REMOTE_DISCONNECT)
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

package com.lasync.audio.core.jukebox

import android.util.Log
import com.lasync.audio.core.network.NetworkConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket

/**
 * JukeboxHostServer:
 * High-speed local TCP server on Port 8890 that blasts pre-decoded 16-bit PCM tracks
 * directly into receiver devices' RAM buffers over high-throughput Wi-Fi LAN sockets.
 */
class JukeboxHostServer(
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "JukeboxHostServer"
    }

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    @Volatile
    private var currentTrack: JukeboxTrack? = null

    fun startServer(track: JukeboxTrack) {
        currentTrack = track
        if (serverJob != null) return

        serverJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(NetworkConstants.PORT_JUKEBOX).apply {
                    reuseAddress = true
                }
                Log.i(TAG, "Jukebox TCP Server listening on port ${NetworkConstants.PORT_JUKEBOX}")

                while (isActive && serverSocket != null && !serverSocket!!.isClosed) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        launch(Dispatchers.IO) {
                            handleClientTransfer(clientSocket)
                        }
                    } catch (_: Exception) {
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Jukebox TCP Server error: ${e.message}", e)
            } finally {
                stopServer()
            }
        }
    }

    private fun handleClientTransfer(socket: Socket) {
        val track = currentTrack ?: run {
            socket.close()
            return
        }

        try {
            socket.tcpNoDelay = true
            socket.sendBufferSize = 128 * 1024

            val rawOut = socket.getOutputStream()
            val bufferedOut = BufferedOutputStream(rawOut, 64 * 1024)
            val dataOut = DataOutputStream(bufferedOut)

            // Header
            dataOut.writeUTF(track.title)
            dataOut.writeUTF(track.artist)
            dataOut.writeLong(track.durationMs)
            dataOut.writeInt(track.sampleRate)
            dataOut.writeInt(track.channelCount)
            dataOut.writeInt(track.pcmBytes.size)
            dataOut.flush()

            // Stream PCM in 64KB blocks
            val pcm = track.pcmBytes
            val chunkSize = 64 * 1024
            var offset = 0
            while (offset < pcm.size) {
                val length = minOf(chunkSize, pcm.size - offset)
                bufferedOut.write(pcm, offset, length)
                offset += length
            }
            bufferedOut.flush()
            socket.shutdownOutput()

            Log.i(TAG, "Successfully transferred ${pcm.size} bytes to ${socket.inetAddress.hostAddress}")
        } catch (e: Exception) {
            Log.w(TAG, "Error transferring track to ${socket.inetAddress.hostAddress}: ${e.message}")
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    fun stopServer() {
        serverJob?.cancel()
        serverJob = null
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        currentTrack = null
    }
}


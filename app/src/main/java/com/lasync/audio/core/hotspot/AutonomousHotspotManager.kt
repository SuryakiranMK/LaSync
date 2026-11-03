package com.lasync.audio.core.hotspot

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.lasync.audio.core.network.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Autonomous Hotspot Manager:
 * Creates a zero-router local Wi-Fi Hotspot on the Host device using Android's
 * official WifiManager.startLocalOnlyHotspot API.
 * Allows multi-device audio streaming anywhere (camping, outdoor parties, road trips)
 * with zero dependency on home or venue Wi-Fi routers.
 */
class AutonomousHotspotManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "AutonomousHotspot"
    }

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private var hotspotReservation: WifiManager.LocalOnlyHotspotReservation? = null

    private val _hotspotState = MutableStateFlow(HotspotState.IDLE)
    val hotspotState: StateFlow<HotspotState> = _hotspotState.asStateFlow()

    private val _activeHotspotInfo = MutableStateFlow<HotspotInfo?>(null)
    val activeHotspotInfo: StateFlow<HotspotInfo?> = _activeHotspotInfo.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())

    @SuppressLint("MissingPermission")
    fun startAutonomousHotspot(
        onStarted: (HotspotInfo) -> Unit,
        onError: (String) -> Unit
    ) {
        if (_hotspotState.value == HotspotState.ACTIVE && _activeHotspotInfo.value != null) {
            onStarted(_activeHotspotInfo.value!!)
            return
        }

        val wm = wifiManager
        if (wm == null) {
            val err = "Wi-Fi hardware manager unavailable"
            _hotspotState.value = HotspotState.ERROR
            onError(err)
            return
        }

        _hotspotState.value = HotspotState.STARTING

        try {
            wm.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                    super.onStarted(reservation)
                    hotspotReservation = reservation

                    val ssid: String
                    val password: String

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val config = reservation.softApConfiguration
                        ssid = config.ssid ?: "LaSync-Party-Hub"
                        password = config.passphrase ?: ""
                    } else {
                        @Suppress("DEPRECATION")
                        val config = reservation.wifiConfiguration
                        ssid = config?.SSID?.removeSurrounding("\"") ?: "LaSync-Party-Hub"
                        password = config?.preSharedKey?.removeSurrounding("\"") ?: ""
                    }

                    // Host IP in Android Local-Only Hotspot is typically 192.168.43.1
                    val detectedIp = NetworkUtils.getLocalIpAddress()
                    val hostIp = if (detectedIp != null && !detectedIp.startsWith("127.")) {
                        detectedIp
                    } else {
                        "192.168.43.1"
                    }

                    val info = HotspotInfo(
                        ssid = ssid,
                        passphrase = password,
                        hostIp = hostIp
                    )

                    _activeHotspotInfo.value = info
                    _hotspotState.value = HotspotState.ACTIVE
                    Log.i(TAG, "Autonomous Hotspot created: SSID=$ssid, IP=$hostIp")
                    onStarted(info)
                }

                override fun onStopped() {
                    super.onStopped()
                    Log.i(TAG, "Autonomous Hotspot stopped")
                    hotspotReservation = null
                    _activeHotspotInfo.value = null
                    _hotspotState.value = HotspotState.IDLE
                }

                override fun onFailed(reason: Int) {
                    super.onFailed(reason)
                    val errorMsg = when (reason) {
                        ERROR_NO_CHANNEL -> "Hotspot failed: No Wi-Fi channel available"
                        ERROR_GENERIC -> "Hotspot failed: Android system rejected request"
                        ERROR_INCOMPATIBLE_MODE -> "Hotspot failed: Wi-Fi mode incompatible"
                        ERROR_TETHERING_DISALLOWED -> "Hotspot failed: Tethering restricted by system"
                        else -> "Hotspot startup failed (code $reason)"
                    }
                    Log.e(TAG, errorMsg)
                    hotspotReservation = null
                    _activeHotspotInfo.value = null
                    _hotspotState.value = HotspotState.ERROR
                    onError(errorMsg)
                }
            }, mainHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting LocalOnlyHotspot: ${e.message}", e)
            _hotspotState.value = HotspotState.ERROR
            onError("Hotspot error: ${e.message ?: "Permission required"}")
        }
    }

    fun stopAutonomousHotspot() {
        try {
            hotspotReservation?.close()
        } catch (_: Exception) {}
        hotspotReservation = null
        _activeHotspotInfo.value = null
        _hotspotState.value = HotspotState.IDLE
    }
}

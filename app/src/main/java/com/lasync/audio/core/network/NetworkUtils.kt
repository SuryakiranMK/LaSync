package com.lasync.audio.core.network

import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Random

object NetworkUtils {

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addresses = intf.inetAddresses
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val hostAddress = addr.hostAddress
                        // Prioritize standard local Wi-Fi subnets (192.168.x.x, 10.x.x.x, 172.16-31.x.x)
                        if (hostAddress != null && !hostAddress.startsWith("127.")) {
                            return hostAddress
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    fun getBroadcastAddress(): InetAddress {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return InetAddress.getByName("255.255.255.255")
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                for (intfAddr in intf.interfaceAddresses) {
                    val broadcast = intfAddr.broadcast
                    if (broadcast != null && intfAddr.address is Inet4Address) {
                        return broadcast
                    }
                }
            }
        } catch (_: Exception) {}
        return InetAddress.getByName("255.255.255.255")
    }

    fun generateSessionId(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val rnd = Random()
        val sb = StringBuilder(6)
        for (i in 0 until 6) {
            sb.append(chars[rnd.nextInt(chars.length)])
        }
        return sb.toString()
    }
}

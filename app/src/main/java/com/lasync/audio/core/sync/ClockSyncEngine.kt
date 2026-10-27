package com.lasync.audio.core.sync

import android.os.SystemClock
import kotlin.math.abs

data class SyncMeasurement(
    val rttNanos: Long,
    val offsetNanos: Long,
    val timestampNanos: Long
)

class ClockSyncEngine(
    private val windowSize: Int = 10
) {
    private val measurements = ArrayDeque<SyncMeasurement>()
    private var lastRttNanos: Long = 0L
    private var jitterNanos: Double = 0.0

    // Packet loss tracking
    private var totalSent: Long = 0L
    private var totalReceived: Long = 0L
    private var expectedSeq: Long = 1L
    private var lostPackets: Long = 0L

    @Synchronized
    fun onPingSent(seq: Long) {
        totalSent++
    }

    /**
     * Process NTP-lite 4-timestamp measurement:
     * T1: Client departure monotonic nanos
     * T2: Host arrival monotonic nanos
     * T3: Host departure monotonic nanos
     * T4: Client arrival monotonic nanos
     */
    @Synchronized
    fun processPong(t1: Long, t2: Long, t3: Long, t4: Long, seq: Long): SyncMeasurement? {
        totalReceived++
        if (seq > expectedSeq) {
            lostPackets += (seq - expectedSeq)
        }
        expectedSeq = seq + 1

        val roundTrip = (t4 - t1) - (t3 - t2)
        if (roundTrip < 0) {
            // Clock anomaly or packet reordering, discard
            return null
        }

        val offset = ((t2 - t1) + (t3 - t4)) / 2

        val measurement = SyncMeasurement(
            rttNanos = roundTrip,
            offsetNanos = offset,
            timestampNanos = t4
        )

        if (measurements.size >= windowSize) {
            measurements.removeFirst()
        }
        measurements.addLast(measurement)

        // RFC 3550 Jitter algorithm
        if (lastRttNanos > 0) {
            val d = abs(roundTrip - lastRttNanos)
            jitterNanos += (d - jitterNanos) / 16.0
        }
        lastRttNanos = roundTrip

        return measurement
    }

    /**
     * Returns median offset across the sliding window to reject Wi-Fi burst outliers.
     */
    @Synchronized
    fun getFilteredOffsetNanos(): Long {
        if (measurements.isEmpty()) return 0L
        val sortedOffsets = measurements.map { it.offsetNanos }.sorted()
        return sortedOffsets[sortedOffsets.size / 2]
    }

    @Synchronized
    fun getAverageRttMs(): Float {
        if (measurements.isEmpty()) return 0f
        val avgNanos = measurements.map { it.rttNanos }.average()
        return (avgNanos / 1_000_000.0).toFloat()
    }

    @Synchronized
    fun getJitterMs(): Float {
        return (jitterNanos / 1_000_000.0).toFloat()
    }

    @Synchronized
    fun getPacketLossPercent(): Float {
        if (totalSent == 0L) return 0f
        val loss = (totalSent - totalReceived).coerceAtLeast(0)
        return ((loss.toFloat() / totalSent.toFloat()) * 100f).coerceIn(0f, 100f)
    }

    /**
     * Convert current local monotonic time into estimated Host Master Timeline monotonic time.
     */
    fun toMasterTimeNanos(localNanos: Long = SystemClock.elapsedRealtimeNanos()): Long {
        return localNanos + getFilteredOffsetNanos()
    }

    @Synchronized
    fun reset() {
        measurements.clear()
        lastRttNanos = 0L
        jitterNanos = 0.0
        totalSent = 0L
        totalReceived = 0L
        expectedSeq = 1L
        lostPackets = 0L
    }
}

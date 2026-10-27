package com.lasync.audio

import com.lasync.audio.core.sync.ClockSyncEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockSyncEngineTest {

    @Test
    fun testRttAndClockOffsetCalculation() {
        val engine = ClockSyncEngine(windowSize = 8)

        // Scenario:
        // Host clock is ahead of Client clock by +20,000,000 nanos (+20 ms).
        // True one-way transmission latency = 2,000,000 nanos (2 ms each direction).
        // Round trip = 4,000,000 nanos (4 ms).
        val t1 = 100_000_000L                   // Client departure
        val t2 = t1 + 2_000_000L + 20_000_000L  // Host arrival
        val t3 = t2 + 100_000L                  // Host processing (0.1 ms)
        val t4 = t1 + 4_000_000L + 100_000L     // Client arrival

        val measurement = engine.processPong(t1, t2, t3, t4, seq = 1L)
        assertNotNull(measurement)

        // RTT = (T4 - T1) - (T3 - T2) = (4.1ms) - (0.1ms) = 4ms = 4,000,000 nanos
        assertEquals(4_000_000L, measurement!!.rttNanos)

        // Offset = ((T2 - T1) + (T3 - T4)) / 2 = (22ms + (122.1ms - 104.1ms)) / 2 = 20ms = 20,000,000 nanos
        assertEquals(20_000_000L, measurement.offsetNanos)

        // Filtered offset across median should be exactly 20ms
        assertEquals(20_000_000L, engine.getFilteredOffsetNanos())
        assertEquals(4.0f, engine.getAverageRttMs(), 0.01f)
    }

    @Test
    fun testMedianFilterRejectsSpikes() {
        val engine = ClockSyncEngine(windowSize = 5)

        // Feed 4 normal samples with 10ms offset
        for (i in 1..4) {
            val t1 = i * 100_000_000L
            val t2 = t1 + 1_000_000L + 10_000_000L
            val t3 = t2 + 50_000L
            val t4 = t1 + 2_000_000L + 50_000L
            engine.processPong(t1, t2, t3, t4, seq = i.toLong())
        }

        // Feed 1 massive network jitter spike (e.g. 150ms spike)
        val t1 = 500_000_000L
        val t2 = t1 + 150_000_000L + 80_000_000L
        val t3 = t2 + 50_000L
        val t4 = t1 + 300_000_000L + 50_000L
        engine.processPong(t1, t2, t3, t4, seq = 5L)

        // Median should still remain exactly 10,000,000L (rejecting the spike)
        assertEquals(10_000_000L, engine.getFilteredOffsetNanos())
    }

    @Test
    fun testPacketLossTracking() {
        val engine = ClockSyncEngine(windowSize = 10)

        // Simulating 10 pings sent
        for (i in 1..10) {
            engine.onPingSent(i.toLong())
        }

        // Receive only packets 1, 2, 4, 6 (4 received, 6 lost = 60% loss)
        listOf(1L, 2L, 4L, 6L).forEach { seq ->
            engine.processPong(100L, 102L, 103L, 105L, seq)
        }

        val loss = engine.getPacketLossPercent()
        assertEquals(60.0f, loss, 0.1f)
    }
}

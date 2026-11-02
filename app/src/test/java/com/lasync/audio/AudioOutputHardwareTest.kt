package com.lasync.audio

import com.lasync.audio.core.audio.AudioOutputHardware
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioOutputHardwareTest {

    @Test
    fun testAudioOutputHardwareEnumMapping() {
        assertEquals(AudioOutputHardware.SPEAKER, AudioOutputHardware.fromId(0))
        assertEquals(AudioOutputHardware.BLUETOOTH, AudioOutputHardware.fromId(1))
        assertEquals(AudioOutputHardware.WIRED, AudioOutputHardware.fromId(2))
        assertEquals(AudioOutputHardware.USB, AudioOutputHardware.fromId(3))
        assertEquals(AudioOutputHardware.UNKNOWN, AudioOutputHardware.fromId(4))
        // Fallback for invalid id
        assertEquals(AudioOutputHardware.SPEAKER, AudioOutputHardware.fromId(99))
    }
}

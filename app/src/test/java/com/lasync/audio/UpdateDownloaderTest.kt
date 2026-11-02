package com.lasync.audio

import com.lasync.audio.core.updater.UpdateDownloader
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateDownloaderTest {

    @Test
    fun testFormatBytes() {
        assertEquals("0 MB", UpdateDownloader.formatBytes(0L))
        assertEquals("0 MB", UpdateDownloader.formatBytes(-50L))
        assertEquals("1.0 MB", UpdateDownloader.formatBytes(1024 * 1024L))
        assertEquals("17.3 MB", UpdateDownloader.formatBytes((17.3 * 1024 * 1024).toLong()))
    }
}

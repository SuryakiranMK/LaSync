package com.lasync.audio

import com.lasync.audio.core.updater.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun testVersionComparison() {
        assertTrue(UpdateChecker.isNewerVersion("1.0.4", "1.0.3"))
        assertTrue(UpdateChecker.isNewerVersion("1.1.0", "1.0.9"))
        assertTrue(UpdateChecker.isNewerVersion("2.0.0", "1.9.9"))
        assertTrue(UpdateChecker.isNewerVersion("1.0.3.1", "1.0.3"))

        assertFalse(UpdateChecker.isNewerVersion("1.0.3", "1.0.3"))
        assertFalse(UpdateChecker.isNewerVersion("1.0.2", "1.0.3"))
        assertFalse(UpdateChecker.isNewerVersion("0.9.9", "1.0.0"))
    }

    @Test
    fun testCleanVersionString() {
        assertEquals("1.0.4", UpdateChecker.cleanVersionString("v1.0.4"))
        assertEquals("1.0.4", UpdateChecker.cleanVersionString("V1.0.4"))
        assertEquals("1.0.4", UpdateChecker.cleanVersionString("1.0.4"))
        assertEquals("1.0.4", UpdateChecker.cleanVersionString("v1.0.4-hotfix"))
    }

    @Test
    fun testParseGitHubReleaseJson() {
        val json = """
            {
              "tag_name": "v1.0.4",
              "name": "LaSync v1.0.4 - In-App Auto Updater",
              "body": "## What's New\n- Auto updater added",
              "html_url": "https://github.com/SuryakiranMK/LaSync/releases/tag/v1.0.4",
              "assets": [
                {
                  "name": "LaSync.apk",
                  "browser_download_url": "https://github.com/SuryakiranMK/LaSync/releases/download/v1.0.4/LaSync.apk"
                }
              ]
            }
        """.trimIndent()

        // When current is 1.0.3, it should detect update
        val update = UpdateChecker.parseAndCheckRelease(json, "1.0.3")
        assertNotNull(update)
        assertEquals("v1.0.4", update?.versionName)
        assertEquals("LaSync v1.0.4 - In-App Auto Updater", update?.title)
        assertEquals("https://github.com/SuryakiranMK/LaSync/releases/download/v1.0.4/LaSync.apk", update?.downloadUrl)

        // When current is already 1.0.4, it should return null
        val noUpdate = UpdateChecker.parseAndCheckRelease(json, "1.0.4")
        assertNull(noUpdate)
    }
}


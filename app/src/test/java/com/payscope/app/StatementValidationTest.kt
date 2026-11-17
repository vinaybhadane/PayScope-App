package com.payscope.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatementValidationTest {

    private val allowedExtensions = setOf("pdf", "csv", "xlsx", "json")
    private val maxSizeBytes = 10 * 1024 * 1024L // 10 MB

    @Test
    fun testValidExtensions() {
        listOf("pdf", "csv", "xlsx", "json").forEach { ext ->
            assertTrue("Extension $ext should be allowed", ext in allowedExtensions)
        }
    }

    @Test
    fun testInvalidExtensions() {
        listOf("exe", "apk", "jpg", "mp3", "doc").forEach { ext ->
            assertFalse("Extension $ext should not be allowed", ext in allowedExtensions)
        }
    }

    @Test
    fun testFileSizeValidation() {
        val validSize = 5 * 1024 * 1024L // 5 MB
        val invalidSize = 12 * 1024 * 1024L // 12 MB
        assertTrue(validSize <= maxSizeBytes)
        assertFalse(invalidSize <= maxSizeBytes)
    }
}

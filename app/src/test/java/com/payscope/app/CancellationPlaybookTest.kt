package com.payscope.app

import com.payscope.app.data.local.SyntheticDemoData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CancellationPlaybookTest {

    @Test
    fun testUpiAutoPayPlaybookStructure() {
        val playbook = SyntheticDemoData.getCancellationPlaybook("sub-1", "Disney+ Hotstar", "UPI AutoPay")

        assertNotNull(playbook)
        assertEquals("UPI AutoPay", playbook.paymentMechanism)
        assertTrue("UPI AutoPay playbook must have actionable steps", playbook.steps.isNotEmpty())
        assertTrue(
            "Steps must include mandate revocation",
            playbook.steps.any { it.contains("Mandate", ignoreCase = true) || it.contains("AutoPay", ignoreCase = true) }
        )
        assertNotNull("Warning note must be provided", playbook.warningNote)
        assertTrue(playbook.warningNote!!.isNotEmpty())
    }

    @Test
    fun testCardTokenPlaybookStructure() {
        val playbook = SyntheticDemoData.getCancellationPlaybook("sub-2", "Cult.fit", "Credit Card Recurring")

        assertNotNull(playbook)
        assertEquals("Credit Card Recurring Instruction", playbook.paymentMechanism)
        assertTrue(
            "Card playbook should instruct turning off standing instructions",
            playbook.steps.any { it.contains("Standing", ignoreCase = true) || it.contains("recurring", ignoreCase = true) || it.contains("Cards", ignoreCase = true) }
        )
    }

    @Test
    fun testNachEmandatePlaybookStructure() {
        val playbook = SyntheticDemoData.getCancellationPlaybook("sub-3", "PolicyBazaar Life", "NACH / e-Mandate")

        assertNotNull(playbook)
        assertEquals("NACH / e-Mandate", playbook.paymentMechanism)
        assertTrue(
            "NACH playbook must guide to Net Banking / Bank Portal",
            playbook.steps.any { it.contains("Net Banking", ignoreCase = true) || it.contains("Bank", ignoreCase = true) || it.contains("Mandate", ignoreCase = true) }
        )
    }

    @Test
    fun testPlayStorePlaybookStructure() {
        val playbook = SyntheticDemoData.getCancellationPlaybook("sub-4", "YouTube Premium", "Google Play Store")

        assertNotNull(playbook)
        assertEquals("Google Play Store Subscription", playbook.paymentMechanism)
        assertTrue(
            "Google Play playbook must refer to Play Store App",
            playbook.steps.any { it.contains("Play Store", ignoreCase = true) }
        )
        assertNotNull(playbook.destinationUrl)
        assertTrue(playbook.destinationUrl!!.contains("play.google.com"))
    }

    @Test
    fun testDirectWebPlaybookStructure() {
        val playbook = SyntheticDemoData.getCancellationPlaybook("sub-5", "ChatGPT Plus", "Direct Web")

        assertNotNull(playbook)
        assertEquals("Direct Merchant Website", playbook.paymentMechanism)
        assertFalse("Web portal steps must be populated", playbook.steps.isEmpty())
    }

    @Test
    fun testDefaultFallbackMechanism() {
        val playbook = SyntheticDemoData.getCancellationPlaybook("sub-unknown", "Unknown Service", "Other")

        assertNotNull(playbook)
        assertEquals("Direct Merchant Website", playbook.paymentMechanism)
        assertTrue(playbook.steps.isNotEmpty())
    }
}

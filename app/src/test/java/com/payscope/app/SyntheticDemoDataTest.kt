package com.payscope.app

import com.payscope.app.data.local.SyntheticDemoData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyntheticDemoDataTest {

    @Test
    fun testSummaryValues() {
        val summary = SyntheticDemoData.getSummary()
        assertEquals(12450.0, summary.monthlyRecurringSpend, 0.01)
        assertEquals(149400.0, summary.annualizedSpend, 0.01)
        assertEquals(14, summary.activeSubscriptionsCount)
        assertEquals(34200.0, summary.potentialSavingsAnnual, 0.01)
        assertEquals(2, summary.priceHikesCount)
        assertEquals("₹", summary.currencySymbol)
    }

    @Test
    fun testCommitmentsListNotEmpty() {
        val commitments = SyntheticDemoData.getCommitments()
        assertTrue("Commitments should not be empty in demo mode", commitments.isNotEmpty())
        assertTrue("Should have at least 5 curated demo commitments", commitments.size >= 5)

        val netflix = commitments.find { it.merchantName.contains("Netflix", ignoreCase = true) }
        assertNotNull("Netflix should be present in demo data", netflix)
        assertTrue("Netflix should have detected price hike", netflix!!.hasPriceHike)
        assertEquals(799.0, netflix.amount, 0.01)
        assertEquals(649.0, netflix.previousAmount ?: 0.0, 0.01)
    }

    @Test
    fun testSpendTrendMonths() {
        val trend = SyntheticDemoData.getSpendTrend()
        assertEquals(6, trend.size)
        val projected = trend.last()
        assertTrue("Last month should be projected", projected.isProjected)
        assertEquals("Nov (Est)", projected.label)
    }

    @Test
    fun testCategoryPercentagesSum() {
        val categories = SyntheticDemoData.getCategories()
        val totalPercentage = categories.sumOf { it.percentage }
        assertEquals(100, totalPercentage)
    }

    @Test
    fun testPriceHikesAlerts() {
        val hikes = SyntheticDemoData.getPriceHikes()
        assertEquals(2, hikes.size)
        assertTrue(hikes.all { it.percentageChange > 0 })
    }
}

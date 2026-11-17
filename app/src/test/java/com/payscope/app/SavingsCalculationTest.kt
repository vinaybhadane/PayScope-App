package com.payscope.app

import com.payscope.app.domain.model.CommitmentItem
import com.payscope.app.domain.model.SavedSavingsPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavingsCalculationTest {

    private fun createSampleCommitments(): List<CommitmentItem> {
        return listOf(
            CommitmentItem(
                id = "sub-1",
                name = "Hotstar Super",
                merchantName = "Disney+ Hotstar",
                amount = 299.0,
                cadence = "Monthly",
                normalizedMonthlyAmount = 299.0,
                nextBillingDate = "Nov 15, 2024",
                category = "Entertainment",
                status = "Confirmed",
                confidenceScore = 95,
                paymentRail = "UPI AutoPay",
                logoKey = "hotstar"
            ),
            CommitmentItem(
                id = "sub-2",
                name = "Cult.fit Elite",
                merchantName = "Cult.fit",
                amount = 1499.0,
                cadence = "Monthly",
                normalizedMonthlyAmount = 1499.0,
                nextBillingDate = "Nov 01, 2024",
                category = "Fitness",
                status = "Confirmed",
                confidenceScore = 92,
                paymentRail = "Credit Card Token",
                logoKey = "cultfit"
            ),
            CommitmentItem(
                id = "sub-3",
                name = "Amazon Prime Annual",
                merchantName = "Amazon Prime",
                amount = 1499.0,
                cadence = "Annual",
                normalizedMonthlyAmount = 124.92,
                nextBillingDate = "Dec 05, 2024",
                category = "Shopping",
                status = "Confirmed",
                confidenceScore = 98,
                paymentRail = "Credit Card",
                logoKey = "amazon"
            )
        )
    }

    @Test
    fun testAnnualizedSpendCalculation() {
        val commitments = createSampleCommitments()
        // sub-1: 299 * 12 = 3588
        // sub-2: 1499 * 12 = 17988
        // sub-3: 1499 * 1 = 1499
        // total = 3588 + 17988 + 1499 = 23075
        val totalAnnualized = commitments.sumOf { item ->
            when (item.cadence.uppercase()) {
                "MONTHLY" -> item.amount * 12
                "ANNUAL", "YEARLY" -> item.amount
                "QUARTERLY" -> item.amount * 4
                "WEEKLY" -> item.amount * 52
                else -> item.amount * 12
            }
        }
        assertEquals(23075.0, totalAnnualized, 0.01)
    }

    @Test
    fun testNonDoubleCountingMathWhenSelectingOpportunities() {
        val commitments = createSampleCommitments()
        val totalMonthlySpend = 299.0 + 1499.0 + 124.92 // 1922.92

        // User selects sub-1 and sub-2 to cancel
        val selectedIds = setOf("sub-1", "sub-2")
        val selectedItems = commitments.filter { it.id in selectedIds }

        // Compute monthly savings
        val monthlySavings = selectedItems.sumOf { item -> item.normalizedMonthlyAmount }
        assertEquals(299.0 + 1499.0, monthlySavings, 0.01)

        val newProjectedMonthlySpend = totalMonthlySpend - monthlySavings
        assertEquals(124.92, newProjectedMonthlySpend, 0.01)

        val percentageCut = (monthlySavings / totalMonthlySpend) * 100.0
        assertTrue("Cut percentage should be between 0 and 100", percentageCut in 0.0..100.0)
        assertEquals(93.5, percentageCut, 0.5)
    }

    @Test
    fun testPlanPersistenceModel() {
        val plan = SavedSavingsPlan(
            id = "plan-oct-2024",
            name = "Q4 Spend Optimization",
            selectedCommitmentIds = listOf("sub-1", "sub-2"),
            annualSavings = 21576.0,
            projectedAnnualCost = 1499.0,
            createdAt = "2024-10-25T14:30:00Z"
        )

        assertEquals("plan-oct-2024", plan.id)
        assertEquals(2, plan.selectedCommitmentIds.size)
        assertTrue(plan.selectedCommitmentIds.contains("sub-1"))
        assertTrue(plan.selectedCommitmentIds.contains("sub-2"))
        assertEquals(21576.0, plan.annualSavings, 0.01)
        assertEquals(1499.0, plan.projectedAnnualCost, 0.01)
        assertFalse(plan.name.isEmpty())
    }
}

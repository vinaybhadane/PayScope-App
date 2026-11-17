package com.payscope.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class RecurrenceClassificationTest {

    enum class Cadence {
        WEEKLY, MONTHLY, QUARTERLY, ANNUAL, IRREGULAR
    }

    data class RecurrenceResult(
        val cadence: Cadence,
        val meanIntervalDays: Double,
        val confidenceScore: Float,
        val intervalVarianceDays: Double
    )

    private fun classifyCadence(intervalsInDays: List<Int>): RecurrenceResult? {
        if (intervalsInDays.isEmpty()) return null

        val mean = intervalsInDays.average()
        val variance = intervalsInDays.map { abs(it - mean) }.average()

        val cadence = when {
            mean in 5.0..9.0 -> Cadence.WEEKLY
            mean in 26.0..33.0 -> Cadence.MONTHLY
            mean in 85.0..95.0 -> Cadence.QUARTERLY
            mean in 350.0..375.0 -> Cadence.ANNUAL
            else -> Cadence.IRREGULAR
        }

        // Higher variance = lower confidence
        val baseConfidence = when (cadence) {
            Cadence.MONTHLY -> 0.95f
            Cadence.ANNUAL -> 0.92f
            Cadence.QUARTERLY -> 0.88f
            Cadence.WEEKLY -> 0.85f
            Cadence.IRREGULAR -> 0.30f
        }
        val penalty = (variance * 0.05f).toFloat()
        val confidence = (baseConfidence - penalty).coerceIn(0.1f, 1.0f)

        return RecurrenceResult(
            cadence = cadence,
            meanIntervalDays = mean,
            confidenceScore = confidence,
            intervalVarianceDays = variance
        )
    }

    @Test
    fun testMonthlyIntervalClassification() {
        // Debits on 1st of each month (intervals 30, 31, 30, 31, 28)
        val monthlyIntervals = listOf(30, 31, 30, 31, 28)
        val result = classifyCadence(monthlyIntervals)

        assertNotNull(result)
        assertEquals(Cadence.MONTHLY, result!!.cadence)
        assertEquals(30.0, result.meanIntervalDays, 0.5)
        assertTrue("Confidence score should be above 0.85 for regular monthly debits", result.confidenceScore > 0.85f)
    }

    @Test
    fun testWeeklyIntervalClassification() {
        val weeklyIntervals = listOf(7, 7, 8, 7, 6, 7)
        val result = classifyCadence(weeklyIntervals)

        assertNotNull(result)
        assertEquals(Cadence.WEEKLY, result!!.cadence)
        assertEquals(7.0, result.meanIntervalDays, 0.5)
        assertTrue(result.confidenceScore > 0.80f)
    }

    @Test
    fun testAnnualIntervalClassification() {
        val annualIntervals = listOf(365, 366, 365)
        val result = classifyCadence(annualIntervals)

        assertNotNull(result)
        assertEquals(Cadence.ANNUAL, result!!.cadence)
        assertEquals(365.33, result.meanIntervalDays, 0.5)
        assertTrue(result.confidenceScore > 0.85f)
    }

    @Test
    fun testQuarterlyIntervalClassification() {
        val quarterlyIntervals = listOf(91, 92, 90)
        val result = classifyCadence(quarterlyIntervals)

        assertNotNull(result)
        assertEquals(Cadence.QUARTERLY, result!!.cadence)
        assertEquals(91.0, result.meanIntervalDays, 1.0)
    }

    @Test
    fun testIrregularTransactions() {
        val sporadicIntervals = listOf(12, 45, 120, 6)
        val result = classifyCadence(sporadicIntervals)

        assertNotNull(result)
        assertEquals(Cadence.IRREGULAR, result!!.cadence)
        assertTrue("Irregular cadence should have low confidence score", result.confidenceScore < 0.5f)
    }

    @Test
    fun testEmptyIntervalsReturnsNull() {
        val result = classifyCadence(emptyList())
        assertNull(result)
    }
}

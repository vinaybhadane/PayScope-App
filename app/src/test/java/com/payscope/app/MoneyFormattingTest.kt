package com.payscope.app

import com.payscope.app.core.util.formatMoney
import org.junit.Assert.*
import org.junit.Test

class MoneyFormattingTest {
    @Test fun preservesPaiseRatherThanTruncatingOrRoundingToRupees() {
        assertEquals("₹249.75", formatMoney(249.75))
        assertEquals("₹249.50", formatMoney(249.5))
    }

    @Test fun wholeAmountsStayCompact() {
        assertEquals("₹9,014", formatMoney(9014.0))
        assertEquals("₹0", formatMoney(0.0))
    }

    @Test fun foreignCurrencyIsNeverLabelledAsRupees() {
        assertEquals("USD 19.99", formatMoney(19.99, "USD"))
        assertEquals("EUR 12.50", formatMoney(12.5, "eur"))
    }

    @Test fun respectsCurrencyMinorUnits() {
        assertEquals("JPY 250", formatMoney(250.0, "JPY"))
        assertEquals("KWD 12.125", formatMoney(12.125, "KWD"))
    }
}

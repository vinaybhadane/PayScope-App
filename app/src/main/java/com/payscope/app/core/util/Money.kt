package com.payscope.app.core.util

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Keep the original currency and minor units visible in financial views. */
fun formatMoney(amount: Double, currency: String = "INR"): String {
    val code = currency.uppercase(Locale.ROOT)
    val unit = runCatching { Currency.getInstance(code) }.getOrNull()
    val decimals = unit?.defaultFractionDigits?.coerceAtLeast(0) ?: 2
    val number = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = if (amount % 1.0 == 0.0) 0 else decimals
        maximumFractionDigits = decimals
    }.format(amount)
    return if (code == "INR") "₹$number" else "$code $number"
}

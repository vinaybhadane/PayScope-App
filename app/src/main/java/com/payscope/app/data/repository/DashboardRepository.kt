package com.payscope.app.data.repository

import com.payscope.app.data.api.*
import com.payscope.app.data.local.DataStoreManager
import com.payscope.app.data.model.*
import com.payscope.app.domain.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor(
    private val apiService: PayScopeApiService,
    private val dataStoreManager: DataStoreManager
) {
    private val mutex = Mutex()
    private var snapshot: DashboardSummaryDto? = null
    private var snapshotToken: String? = null
    private var loadedAt = 0L
    var lastError: String? = null
        private set

    fun clearUserCommitments() { snapshot = null; loadedAt = 0L }
    suspend fun refresh() { loadedAt = 0L; data() }

    suspend fun refreshStrict(): DashboardSummaryDto = mutex.withLock {
        val token = dataStoreManager.authTokenFlow.first() ?: error("Please sign in again.")
        val result = apiService.getDashboardSummary("Bearer $token").requireBody()
        snapshotToken = token
        snapshot = result
        loadedAt = System.currentTimeMillis()
        lastError = null
        result
    }

    suspend fun getWorkspaceActivity(): WorkspaceActivity {
        val d = data()
        return WorkspaceActivity(d.importedTransactionsCount, d.importedStatementsCount,
            d.statementTotals.map { StatementCurrencyTotal(it.currency, it.debits, it.credits) },
            d.recentTransactions.map { ImportedTransaction(it.id, it.date, it.description, it.amount, it.currency, it.direction, it.status) },
            d.analysisStatus, d.analysisError)
    }

    suspend fun getStatementAnalytics(): StatementAnalyticsDto = data().statementAnalytics

    private suspend fun data(): DashboardSummaryDto = mutex.withLock {
        val token = dataStoreManager.authTokenFlow.first()
        if (token != snapshotToken) { snapshot = null; loadedAt = 0L; snapshotToken = token }
        val cacheDuration = if (lastError == null) 30_000 else 1_000
        if (System.currentTimeMillis() - loadedAt < cacheDuration) return@withLock snapshot ?: DashboardSummaryDto()
        try {
            check(!token.isNullOrBlank()) { "Please sign in to view your workspace." }
            val result = apiService.getDashboardSummary("Bearer $token").requireBody()
            snapshot = result
            loadedAt = System.currentTimeMillis()
            lastError = null
            result
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            lastError = e.localizedMessage ?: "Could not load your workspace. Check your connection and retry."
            loadedAt = System.currentTimeMillis()
            // A failed request is surfaced in the UI. Never manufacture account data.
            snapshot ?: DashboardSummaryDto()
        }
    }

    suspend fun getFinancialSummary(): FinancialSummary {
        val d = data()
        return FinancialSummary(d.monthlySpend ?: 0.0, d.annualizedSpend ?: 0.0,
            d.activeCount ?: 0, d.potentialSavings ?: 0.0, d.priceHikesCount ?: 0,
            totalHistoricalSpend12M = d.historicalAmounts.sum(), upcomingBillsCount = d.upcoming.size,
            upcomingBillsTotal = d.upcoming.sumOf { it.amount })
    }

    suspend fun getCommitments(): List<CommitmentItem> = data().commitments.orEmpty().map { d ->
        val status = when (d.status?.lowercase()) {
            "active", "user_confirmed" -> "Confirmed"
            "candidate" -> "Potential"
            "review_required", "needs_review" -> "Needs Review"
            else -> d.status ?: "Needs Review"
        }
        val cadence = when (d.cadence?.lowercase()) {
            "annual" -> "Yearly"
            "variable_monthly" -> "Monthly"
            else -> d.cadence.orEmpty().replaceFirstChar { it.titlecase() }
        }
        CommitmentItem(id = d.id, name = d.displayName ?: d.name ?: "Payment",
            merchantName = d.merchantName ?: d.displayName ?: "Merchant",
            amount = d.representativeAmount ?: 0.0, currency = d.currency, cadence = cadence,
            normalizedMonthlyAmount = d.normalizedMonthlyCost ?: 0.0,
            nextBillingDate = d.nextExpectedDate.orEmpty(), category = d.category ?: "Other", status = status,
            confidenceScore = d.confidenceScore?.toInt() ?: 0,
            logoKey = d.logoKey ?: (d.merchantName ?: d.displayName).orEmpty().lowercase(),
            paymentRail = d.paymentRail ?: "Unknown", hasPriceHike = d.hasPriceHike,
            previousAmount = d.previousAmount, priceHikePercentage = d.priceHikePercentage,
            reasonExplanation = d.reasonExplanation ?: "Review the supporting transactions.",
            annualizedAmount = d.annualizedAmount ?: 0.0)
    }

    suspend fun updateCommitment(id: String, name: String? = null, category: String? = null,
        cadence: String? = null, amount: Double? = null, status: String? = null): Result<Unit> = try {
        val token = dataStoreManager.authTokenFlow.first() ?: error("Please sign in again.")
        val backendCadence = when (cadence?.lowercase()) { "yearly", "annual" -> "annual"; else -> cadence?.lowercase() }
        val backendStatus = when (status) { "Confirmed" -> "user_confirmed"; "Dismissed" -> "marked_not_recurring"; else -> null }
        apiService.updateCommitment("Bearer $token", id, UpdateCommitmentDto(name, category, backendCadence, amount, backendStatus)).requireSuccess()
        clearUserCommitments()
        Result.success(Unit)
    } catch (e: Exception) { if (e is CancellationException) throw e; Result.failure(e) }

    suspend fun getSpendTrend(filter: DateRangeFilter = DateRangeFilter.LAST_6_MONTHS): List<SpendTrendPoint> {
        val d = data()
        val today = LocalDate.now()
        val start = when (filter) {
            DateRangeFilter.LAST_30_DAYS -> YearMonth.from(today.minusDays(30))
            DateRangeFilter.LAST_3_MONTHS -> YearMonth.from(today).minusMonths(2)
            DateRangeFilter.LAST_6_MONTHS -> YearMonth.from(today).minusMonths(5)
            DateRangeFilter.LAST_12_MONTHS -> YearMonth.from(today).minusMonths(11)
            DateRangeFilter.THIS_YEAR -> YearMonth.of(today.year, 1)
        }
        if (filter == DateRangeFilter.LAST_30_DAYS) {
            return d.dailySpend.entries.filter { runCatching {
                val day = LocalDate.parse(it.key)
                !day.isBefore(today.minusDays(29)) && !day.isAfter(today)
            }.getOrDefault(false) }.groupBy { YearMonth.from(LocalDate.parse(it.key)) }.toSortedMap().map { (month, values) ->
                SpendTrendPoint(month.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)), values.sumOf { it.value })
            }
        }
        val fmt = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)
        return d.historicalMonths.zip(d.historicalAmounts).filter { (month, _) ->
            runCatching { YearMonth.parse(month, fmt) >= start }.getOrDefault(false)
        }.map { SpendTrendPoint(it.first, it.second) }
    }

    suspend fun getCategories(): List<CategorySpend> {
        val categories = data().categories
        val total = categories.values.sum()
        val colors = listOf(0xFFFF5500L, 0xFF00E0E0L, 0xFF14B8A6L, 0xFFF59E0BL, 0xFF3B82F6L, 0xFFA855F7L)
        return categories.entries.sortedByDescending { it.value }.mapIndexed { i, e ->
            CategorySpend(e.key, e.value, if (total > 0) (e.value / total * 100).toInt() else 0, colors[i % colors.size])
        }
    }

    suspend fun getTopMerchants(): List<MerchantComparison> = data().statementAnalytics.topMerchants
        .take(5).mapIndexed { i, m -> MerchantComparison(m.name, m.totalSpend,
            m.monthlyAverage, m.name.lowercase(), "Observed payments", i + 1) }

    suspend fun getUpcoming(): List<UpcomingPayment> = data().upcoming.map {
        UpcomingPayment(it.id, it.merchantName, it.amount, it.dueDate, it.daysRemaining, it.cadence, it.logoKey)
    }

    suspend fun getPriceHikes(): List<PriceHikeAlert> = data().priceHikes.map {
        PriceHikeAlert(it.id, it.merchantName, it.oldAmount, it.newAmount, it.percentageChange,
            it.effectiveDate, it.logoKey, it.annualizedImpact)
    }

    suspend fun getHistoricalDebits(commitmentId: String): List<HistoricalDebit> {
        val token = dataStoreManager.authTokenFlow.first() ?: error("Please sign in again.")
        return apiService.getCommitmentDetail("Bearer $token", commitmentId).requireBody().transactions.map {
            HistoricalDebit(it.id, it.date.take(10), it.amount, it.descriptor)
        }
    }
}

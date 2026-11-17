package com.payscope.app.domain.model

data class UserSession(
    val userId: String,
    val email: String,
    val displayName: String,
    val workspaceId: String,
    val isGuest: Boolean,
    val token: String,
    val hasUploadedStatements: Boolean = false
)

data class FinancialSummary(
    val monthlyRecurringSpend: Double,
    val annualizedSpend: Double,
    val activeSubscriptionsCount: Int,
    val potentialSavingsAnnual: Double,
    val priceHikesCount: Int,
    val totalHistoricalSpend12M: Double = 0.0,
    val upcomingBillsCount: Int = 0,
    val upcomingBillsTotal: Double = 0.0,
    val currencySymbol: String = "₹"
)

data class CommitmentItem(
    val id: String,
    val name: String,
    val merchantName: String,
    val amount: Double,
    val currency: String = "INR",
    val cadence: String, // "Monthly", "Yearly", etc.
    val normalizedMonthlyAmount: Double,
    val nextBillingDate: String,
    val category: String,
    val status: String = "Confirmed", // "Confirmed", "Needs Review", "Potential", "Cancelled"
    val confidenceScore: Int = 95,
    val logoKey: String,
    val paymentRail: String = "UPI AutoPay",
    val hasPriceHike: Boolean = false,
    val previousAmount: Double? = null,
    val priceHikePercentage: Double? = null,
    val reasonExplanation: String = "Observed consistent recurring debit amounts across multiple billing intervals.",
    val annualizedAmount: Double = normalizedMonthlyAmount * 12
)

data class SpendTrendPoint(
    val label: String,
    val amount: Double,
    val isProjected: Boolean = false
)

data class CategorySpend(
    val category: String,
    val amount: Double,
    val percentage: Int,
    val colorHex: Long
)

data class UpcomingPayment(
    val id: String,
    val merchantName: String,
    val amount: Double,
    val dueDate: String,
    val daysRemaining: Int,
    val cadence: String,
    val logoKey: String
)

data class PriceHikeAlert(
    val id: String,
    val merchantName: String,
    val oldAmount: Double,
    val newAmount: Double,
    val percentageChange: Double,
    val effectiveDate: String,
    val logoKey: String,
    val annualizedImpact: Double = (newAmount - oldAmount) * 12
)

data class BankStatementItem(
    val id: String,
    val fileName: String,
    val fileType: String,
    val recordCount: Int,
    val uploadedAt: String
)

enum class UploadStage(val title: String, val description: String) {
    READING("Reading Statement", "Parsing digital document structures and tables..."),
    SAVING("Saving and analyzing", "Saving validated transactions and reviewing recurring payments with Gemini..."),
    EXTRACTING("Extracting Transactions", "Normalizing debit records, timestamps, and references..."),
    IDENTIFYING("Identifying Merchants", "Reviewing merchant descriptions from your records..."),
    DETECTING("Detecting Recurring Payments", "Gemini is reviewing recurrence and payment evidence..."),
    PREPARING("Preparing Dashboard", "Projecting metrics into your workspace..."),
    COMPLETED("Analysis Complete", "Statement processed successfully!")
}

data class TransactionPreviewItem(
    val date: String,
    val description: String,
    val amount: Double,
    val isDebit: Boolean,
    val normalizedMerchant: String,
    val currency: String = "INR"
)

data class StatementPreviewData(
    val fileName: String,
    val fileType: String,
    val totalRows: Int,
    val debitCount: Int,
    val totalDebits: Double,
    val creditCount: Int,
    val totalCredits: Double,
    val sampleTransactions: List<TransactionPreviewItem>,
    val currency: String = "INR"
)

data class ChatMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: String,
    val suggestions: List<String> = emptyList()
)

data class CancellationPlaybook(
    val commitmentId: String,
    val merchantName: String,
    val paymentMechanism: String,
    val steps: List<String>,
    val destinationUrl: String? = null,
    val supportMessage: String? = null,
    val warningNote: String? = null
)

data class SavedSavingsPlan(
    val id: String,
    val name: String,
    val selectedCommitmentIds: List<String>,
    val annualSavings: Double,
    val projectedAnnualCost: Double,
    val createdAt: String
)

data class HistoricalDebit(
    val id: String,
    val date: String,
    val amount: Double,
    val descriptor: String
)

enum class DateRangeFilter(val label: String) {
    LAST_30_DAYS("Last 30 Days"),
    LAST_3_MONTHS("Last 3 Months"),
    LAST_6_MONTHS("Last 6 Months"),
    LAST_12_MONTHS("Last 12 Months"),
    THIS_YEAR("This Year")
}

data class MerchantComparison(
    val merchantName: String,
    val annualSpend: Double,
    val monthlySpend: Double,
    val logoKey: String,
    val category: String,
    val rank: Int
)

data class GraphNodeItem(
    val id: String,
    val name: String,
    val label: String,
    val nodeType: String,
    val category: String?,
    val amount: Double?,
    val cadence: String?,
    val colorHex: String,
    var x: Float = 0f,
    var y: Float = 0f
)

data class GraphEdgeItem(
    val id: String,
    val source: String,
    val target: String,
    val relationshipType: String,
    val label: String
)

data class GraphExplorerData(
    val nodes: List<GraphNodeItem>,
    val edges: List<GraphEdgeItem>,
    val nodeCount: Int,
    val edgeCount: Int,
    val isConnected: Boolean = false,
    val databaseName: String = "",
    val source: String = "relational",
    val message: String? = null
)


data class ImportedTransaction(
    val id: String, val date: String, val description: String,
    val amount: Double, val currency: String, val direction: String, val status: String
)
data class StatementCurrencyTotal(val currency: String, val debits: Double, val credits: Double)
data class WorkspaceActivity(
    val transactionCount: Int = 0, val statementCount: Int = 0,
    val totals: List<StatementCurrencyTotal> = emptyList(),
    val transactions: List<ImportedTransaction> = emptyList(),
    val analysisStatus: String = "empty", val analysisError: String? = null
)

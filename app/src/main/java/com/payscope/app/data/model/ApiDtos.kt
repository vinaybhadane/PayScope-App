package com.payscope.app.data.model

import com.google.gson.annotations.SerializedName

data class GuestSessionResponse(
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("token") val token: String? = null,
    @SerializedName("token_type") val tokenType: String = "bearer",
    @SerializedName("workspace_id") val workspaceId: String? = null,
    @SerializedName("expires_in") val expiresIn: Long? = 86400
)

data class UserProfileDto(
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("id") val id: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("workspace_id") val workspaceId: String? = null,
    @SerializedName("is_guest") val isGuest: Boolean = false,
    @SerializedName("has_uploaded_statements") val hasUploadedStatements: Boolean = false
)

data class DashboardSummaryDto(
    @SerializedName("statement_analytics") val statementAnalytics: StatementAnalyticsDto = StatementAnalyticsDto(),
    @SerializedName("imported_transactions_count") val importedTransactionsCount: Int = 0,
    @SerializedName("imported_statements_count") val importedStatementsCount: Int = 0,
    @SerializedName("statement_totals") val statementTotals: List<StatementCurrencyTotalDto> = emptyList(),
    @SerializedName("recent_transactions") val recentTransactions: List<ImportedTransactionDto> = emptyList(),
    @SerializedName("analysis_status") val analysisStatus: String = "empty",
    @SerializedName("analysis_error") val analysisError: String? = null,
    @SerializedName("monthly_recurring_spend") val monthlySpend: Double? = 0.0,
    @SerializedName("annualized_spend") val annualizedSpend: Double? = 0.0,
    @SerializedName("active_subscriptions_count") val activeCount: Int? = 0,
    @SerializedName("potential_savings_annual") val potentialSavings: Double? = 0.0,
    @SerializedName("price_hikes_count") val priceHikesCount: Int? = 0,
    @SerializedName("currency") val currency: String? = "₹",
    @SerializedName("commitments") val commitments: List<CommitmentDto>? = null,
    @SerializedName("daily_spend") val dailySpend: Map<String, Double> = emptyMap(),
    @SerializedName("historical_months") val historicalMonths: List<String> = emptyList(),
    @SerializedName("historical_amounts") val historicalAmounts: List<Double> = emptyList(),
    @SerializedName("category_distribution") val categories: Map<String, Double> = emptyMap(),
    @SerializedName("upcoming") val upcoming: List<UpcomingDto> = emptyList(),
    @SerializedName("price_hikes") val priceHikes: List<PriceHikeDto> = emptyList(),
    @SerializedName("is_synthetic") val isSynthetic: Boolean = false
)

data class StatementAnalyticsDto(
    @SerializedName("total_debits") val totalDebits: Double = 0.0,
    @SerializedName("monthly_average") val monthlyAverage: Double = 0.0,
    @SerializedName("period_start") val periodStart: String? = null,
    @SerializedName("period_end") val periodEnd: String? = null,
    @SerializedName("top_merchants") val topMerchants: List<StatementMerchantDto> = emptyList(),
    @SerializedName("overview") val overview: String = "",
    @SerializedName("spending_analysis") val spendingAnalysis: String = "",
    @SerializedName("savings_analysis") val savingsAnalysis: String = ""
)

data class StatementMerchantDto(
    @SerializedName("name") val name: String,
    @SerializedName("total_spend") val totalSpend: Double,
    @SerializedName("monthly_average") val monthlyAverage: Double
)

data class CommitmentDto(
    @SerializedName("id") val id: String,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("merchant_name") val merchantName: String? = null,
    @SerializedName("representative_amount") val representativeAmount: Double? = 0.0,
    @SerializedName("amount") val amount: Double? = null,
    @SerializedName("annualised_amount") val annualizedAmount: Double? = null,
    @SerializedName("normalized_monthly_cost") val normalizedMonthlyCost: Double? = 0.0,
    @SerializedName("currency") val currency: String = "INR",
    @SerializedName("cadence") val cadence: String? = "Monthly",
    @SerializedName("category") val category: String? = "Software",
    @SerializedName("status") val status: String? = "Confirmed",
    @SerializedName("confidence_score") val confidenceScore: Double? = null,
    @SerializedName("next_expected_date") val nextExpectedDate: String? = null,
    @SerializedName("logo_key") val logoKey: String? = null,
    @SerializedName("payment_rail") val paymentRail: String? = "UPI AutoPay",
    @SerializedName("has_price_hike") val hasPriceHike: Boolean = false,
    @SerializedName("previous_amount") val previousAmount: Double? = null,
    @SerializedName("price_hike_percentage") val priceHikePercentage: Double? = null,
    @SerializedName("reason_explanation") val reasonExplanation: String? = null
)

data class StatementFileInfo(
    @SerializedName("filename") val filename: String? = null,
    @SerializedName("file_type") val fileType: String? = null,
    @SerializedName("size_bytes") val sizeBytes: Long? = 0
)

data class StatementPreviewResponseDto(
    @SerializedName("job_id") val jobId: String? = null,
    @SerializedName("statement_id") val statementId: String? = null,
    @SerializedName("file_info") val fileInfo: StatementFileInfo? = null,
    @SerializedName("filename", alternate = ["file_name"]) val fileName: String? = null,
    @SerializedName("file_type") val fileType: String? = null,
    @SerializedName("total_rows") val totalRows: Int = 0,
    @SerializedName("valid_count") val validCount: Int = 0,
    @SerializedName("detected_columns", alternate = ["headers"]) val detectedColumns: List<String> = emptyList(),
    @SerializedName("preview_rows", alternate = ["sample_rows"]) val previewRows: List<Map<String, Any>> = emptyList(),
    @SerializedName("invalid_count") val invalidCount: Int = 0,
    @SerializedName("currencies") val currencies: List<String> = emptyList(),
    @SerializedName("debit_count") val debitCount: Int = 0,
    @SerializedName("total_debits") val totalDebits: Double = 0.0,
    @SerializedName("credit_count") val creditCount: Int = 0,
    @SerializedName("total_credits") val totalCredits: Double = 0.0,
    @SerializedName("status") val status: String = "preview_ready"
)

data class StatementUploadResponseDto(
    @SerializedName("import_id") val importId: String? = null,
    @SerializedName("job_id") val jobId: String? = null,
    @SerializedName("filename", alternate = ["file_name"]) val fileName: String? = "Statement",
    @SerializedName("accepted_count", alternate = ["record_count", "total_rows"]) val recordCount: Int = 0,
    @SerializedName("status") val status: String = "completed",
    @SerializedName("message") val message: String? = "Statement imported successfully",
    @SerializedName("workspace_refresh") val workspaceRefresh: Map<String, Any>? = null,
    @SerializedName("error_summary") val errorSummary: String? = null
)

data class ImportJobDto(
    @SerializedName("job_id") val jobId: String,
    @SerializedName("status") val status: String,
    @SerializedName("result") val result: Map<String, Any?>? = null,
    @SerializedName("error") val error: String? = null
)

// Assistant Chat DTOs
data class ChatMessageDto(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class ChatRequestDto(
    @SerializedName("message") val message: String,
    @SerializedName("history") val history: List<ChatMessageDto> = emptyList()
)

data class CommitmentSnippetDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("cadence") val cadence: String,
    @SerializedName("category") val category: String
)

data class ChatResponseDto(
    @SerializedName("reply") val reply: String,
    @SerializedName("suggestions") val suggestions: List<String> = emptyList(),
    @SerializedName("related_commitments") val relatedCommitments: List<CommitmentSnippetDto> = emptyList()
)

// Savings Plans DTOs
data class SavePlanDto(
    @SerializedName("name") val name: String,
    @SerializedName("selected_commitment_ids") val selectedCommitmentIds: List<String>,
    @SerializedName("annual_savings") val annualSavings: Double,
    @SerializedName("projected_annual_cost") val projectedAnnualCost: Double,
    @SerializedName("include_candidates") val includeCandidates: Boolean = false
)

data class SavedPlanDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("selected_commitment_ids") val selectedCommitmentIds: List<String>,
    @SerializedName("annual_savings") val annualSavings: Double,
    @SerializedName("projected_annual_cost") val projectedAnnualCost: Double,
    @SerializedName("created_at") val createdAt: String
)

// Cancellation Playbook DTOs
data class CancellationPlaybookDto(
    @SerializedName("commitment_id") val commitmentId: String,
    @SerializedName("merchant_name") val merchantName: String,
    @SerializedName("payment_mechanism") val paymentMechanism: String,
    @SerializedName("steps") val steps: List<String>,
    @SerializedName("destination_url") val destinationUrl: String? = null,
    @SerializedName("support_message") val supportMessage: String? = null,
    @SerializedName("warning_note") val warningNote: String? = null
)

data class RecordCancellationDto(
    @SerializedName("commitment_id") val commitmentId: String,
    @SerializedName("cancellation_method") val cancellationMethod: String,
    @SerializedName("notes") val notes: String? = null
)

data class UpdateCommitmentDto(
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("cadence") val cadence: String? = null,
    @SerializedName("representative_amount") val representativeAmount: Double? = null,
    @SerializedName("status") val status: String? = null
)

// Neo4j Graph Relationship Intelligence DTOs
data class GraphNodeDataDto(
    @SerializedName("id") val id: String,
    @SerializedName("label") val label: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("node_type") val nodeType: String? = "Commitment",
    @SerializedName("category") val category: String? = null,
    @SerializedName("amount") val amount: Double? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("cadence") val cadence: String? = null
)

data class GraphNodeDto(
    @SerializedName("data") val data: GraphNodeDataDto
)

data class GraphEdgeDataDto(
    @SerializedName("id") val id: String,
    @SerializedName("source") val source: String,
    @SerializedName("target") val target: String,
    @SerializedName("relationship_type") val relationshipType: String? = "ASSOCIATED_WITH",
    @SerializedName("label") val label: String? = null
)

data class GraphEdgeDto(
    @SerializedName("data") val data: GraphEdgeDataDto
)

data class GraphElementsDto(
    @SerializedName("nodes") val nodes: List<GraphNodeDto> = emptyList(),
    @SerializedName("edges") val edges: List<GraphEdgeDto> = emptyList(),
    @SerializedName("source") val source: String? = "neo4j",
    @SerializedName("message") val message: String? = null
)

data class GraphHealthDto(
    @SerializedName("status") val status: String,
    @SerializedName("enabled") val enabled: Boolean,
    @SerializedName("connected") val connected: Boolean,
    @SerializedName("node_count") val nodeCount: Int = 0,
    @SerializedName("edge_count") val edgeCount: Int = 0,
    @SerializedName("message") val message: String
)

data class GraphSyncDto(
    @SerializedName("status") val status: String,
    @SerializedName("nodes_synced") val nodesSynced: Int = 0,
    @SerializedName("relationships_synced") val relationshipsSynced: Int = 0,
    @SerializedName("error") val error: String? = null
)


data class UpcomingDto(
    @SerializedName("id") val id: String,
    @SerializedName("merchant_name") val merchantName: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("due_date") val dueDate: String,
    @SerializedName("days_remaining") val daysRemaining: Int,
    @SerializedName("cadence") val cadence: String,
    @SerializedName("logo_key") val logoKey: String
)
data class PriceHikeDto(
    @SerializedName("id") val id: String,
    @SerializedName("merchant_name") val merchantName: String,
    @SerializedName("old_amount") val oldAmount: Double,
    @SerializedName("new_amount") val newAmount: Double,
    @SerializedName("percentage_change") val percentageChange: Double,
    @SerializedName("effective_date") val effectiveDate: String,
    @SerializedName("logo_key") val logoKey: String,
    @SerializedName("annualized_impact") val annualizedImpact: Double
)
data class CommitmentDetailDto(@SerializedName("supporting_transactions") val transactions: List<EvidenceDto>)
data class EvidenceDto(
    @SerializedName("transaction_id") val id: String,
    @SerializedName("transaction_date") val date: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("raw_descriptor") val descriptor: String
)
data class ImportHistoryDto(@SerializedName("items") val items: List<ImportDto>)
data class ImportDto(
    @SerializedName("id") val id: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("file_type") val fileType: String,
    @SerializedName("accepted_count") val acceptedCount: Int,
    @SerializedName("created_at") val createdAt: String
)
data class CancellationEligibilityDto(
    @SerializedName("commitment_id") val commitmentId: String,
    @SerializedName("commitment_name") val name: String,
    @SerializedName("detected_mechanism") val mechanism: String,
    @SerializedName("recommended_playbook") val playbook: PlaybookDto
)
data class PlaybookDto(
    @SerializedName("steps") val steps: List<PlaybookStepDto>,
    @SerializedName("official_source_url") val url: String?,
    @SerializedName("important_warnings") val warnings: List<String>
)
data class PlaybookStepDto(@SerializedName("instruction") val instruction: String)
data class CancellationCaseDto(@SerializedName("id") val id: String)


data class StatementCurrencyTotalDto(
    @SerializedName("currency") val currency: String,
    @SerializedName("debits") val debits: Double,
    @SerializedName("credits") val credits: Double
)
data class ImportedTransactionDto(
    @SerializedName("id") val id: String,
    @SerializedName("date") val date: String,
    @SerializedName("description") val description: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("currency") val currency: String,
    @SerializedName("direction") val direction: String,
    @SerializedName("status") val status: String
)
data class ImportStatusDto(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: String,
    @SerializedName("accepted_count") val acceptedCount: Int,
    @SerializedName("duplicate_count") val duplicateCount: Int,
    @SerializedName("error_summary") val error: String?
)

package com.payscope.app.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payscope.app.data.repository.DashboardRepository
import com.payscope.app.domain.model.CategorySpend
import com.payscope.app.domain.model.DateRangeFilter
import com.payscope.app.domain.model.FinancialSummary
import com.payscope.app.domain.model.MerchantComparison
import com.payscope.app.domain.model.SpendTrendPoint
import com.payscope.app.domain.model.UpcomingPayment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsUiState(
    val statement: com.payscope.app.data.model.StatementAnalyticsDto = com.payscope.app.data.model.StatementAnalyticsDto(),
    val potentialMonthlyEstimate: Double = 0.0,
    val potentialCount: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val selectedFilter: DateRangeFilter = DateRangeFilter.LAST_6_MONTHS,
    val summary: FinancialSummary = FinancialSummary(0.0, 0.0, 0, 0.0, 0),
    val spendTrend: List<SpendTrendPoint> = emptyList(),
    val categories: List<CategorySpend> = emptyList(),
    val topMerchants: List<MerchantComparison> = emptyList(),
    val upcoming: List<UpcomingPayment> = emptyList()
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadData(DateRangeFilter.LAST_6_MONTHS)
    }

    fun onFilterSelected(filter: DateRangeFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        loadData(filter)
    }

    fun reload() = loadData(_uiState.value.selectedFilter)

    private fun loadData(filter: DateRangeFilter) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val summary = dashboardRepository.getFinancialSummary()
            val trend = dashboardRepository.getSpendTrend(filter)
            val categories = dashboardRepository.getCategories()
            val topMerchants = dashboardRepository.getTopMerchants()
            val upcoming = dashboardRepository.getUpcoming()
            val statement = dashboardRepository.getStatementAnalytics()
            val potential = dashboardRepository.getCommitments().filter { it.currency == "INR" && it.status != "Confirmed" }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                summary = summary,
                statement = statement,
                potentialMonthlyEstimate = potential.sumOf { it.normalizedMonthlyAmount },
                potentialCount = potential.size,
                spendTrend = trend,
                categories = categories,
                topMerchants = topMerchants,
                upcoming = upcoming,
                errorMessage = dashboardRepository.lastError
            )
        }
    }
}

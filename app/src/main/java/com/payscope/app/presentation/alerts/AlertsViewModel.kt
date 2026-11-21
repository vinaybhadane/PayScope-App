package com.payscope.app.presentation.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payscope.app.data.repository.DashboardRepository
import com.payscope.app.data.repository.StatementRepository
import com.payscope.app.domain.model.PriceHikeAlert
import com.payscope.app.domain.model.UpcomingPayment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertsUiState(
    val priceHikes: List<PriceHikeAlert> = emptyList(),
    val upcomingRenewals: List<UpcomingPayment> = emptyList(),
    val analysisStatus: String = "empty",
    val analysisError: String? = null,
    val selectedFilter: String = "All", // "All", "Price Changes", "Renewals"
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val statementRepository: StatementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        loadAlerts()
    }

    fun loadAlerts(forceRefresh: Boolean = false) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                if (forceRefresh) {
                    dashboardRepository.refreshStrict()
                }
                val hikes = dashboardRepository.getPriceHikes()
                val upcoming = dashboardRepository.getUpcoming()
                val activity = dashboardRepository.getWorkspaceActivity()
                _uiState.value = _uiState.value.copy(
                    priceHikes = hikes,
                    upcomingRenewals = upcoming,
                    analysisStatus = activity.analysisStatus,
                    analysisError = activity.analysisError,
                    isLoading = false
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Could not load alerts."
                )
            }
        }
    }

    fun onFilterSelected(filter: String) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun retryAnalysis() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                statementRepository.retryAnalysis()
                _uiState.value = _uiState.value.copy(isLoading = false)
                loadAlerts(forceRefresh = true)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Could not trigger re-analysis."
                )
            }
        }
    }
}

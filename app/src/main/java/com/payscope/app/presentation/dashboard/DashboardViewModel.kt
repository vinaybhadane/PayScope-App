package com.payscope.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payscope.app.data.local.DataStoreManager
import com.payscope.app.data.repository.AuthRepository
import com.payscope.app.data.repository.DashboardRepository
import com.payscope.app.domain.model.CategorySpend
import com.payscope.app.domain.model.CommitmentItem
import com.payscope.app.domain.model.FinancialSummary
import com.payscope.app.domain.model.PriceHikeAlert
import com.payscope.app.domain.model.SpendTrendPoint
import com.payscope.app.domain.model.UpcomingPayment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DashboardEvent {
    data object NavigateToUpload : DashboardEvent
    data object NavigateToAuth : DashboardEvent
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isGuestDemo: Boolean = false,
    val userName: String = "Your workspace",
    val hasUploadedStatements: Boolean = false,
    val activity: com.payscope.app.domain.model.WorkspaceActivity = com.payscope.app.domain.model.WorkspaceActivity(),
    val summary: FinancialSummary = FinancialSummary(0.0, 0.0, 0, 0.0, 0),
    val commitments: List<CommitmentItem> = emptyList(),
    val spendTrend: List<SpendTrendPoint> = emptyList(),
    val categories: List<CategorySpend> = emptyList(),
    val upcoming: List<UpcomingPayment> = emptyList(),
    val priceHikes: List<PriceHikeAlert> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val authRepository: AuthRepository,
    private val dataStoreManager: DataStoreManager,
    private val statementRepository: com.payscope.app.data.repository.StatementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DashboardEvent>()
    val events: SharedFlow<DashboardEvent> = _events.asSharedFlow()


    fun loadDashboardData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val isGuest = dataStoreManager.isGuestFlow.first()
            val userName = dataStoreManager.userNameFlow.first()
            val hasUploaded = dataStoreManager.hasUploadedStatementsFlow.first()

            if (forceRefresh) dashboardRepository.refresh()
            val activity = dashboardRepository.getWorkspaceActivity()
            val summary = dashboardRepository.getFinancialSummary()
            val commitments = dashboardRepository.getCommitments()
            val trend = dashboardRepository.getSpendTrend()
            val categories = dashboardRepository.getCategories()
            val upcoming = dashboardRepository.getUpcoming()
            val priceHikes = dashboardRepository.getPriceHikes()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isGuestDemo = isGuest,
                userName = userName,
                hasUploadedStatements = activity.transactionCount > 0 || activity.statementCount > 0,
                activity = activity,
                summary = summary,
                commitments = commitments,
                spendTrend = trend,
                categories = categories,
                upcoming = upcoming,
                priceHikes = priceHikes,
                errorMessage = dashboardRepository.lastError
            )
        }
    }

    fun retryAnalysis() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try { statementRepository.retryAnalysis(); loadDashboardData() }
            catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.localizedMessage)
            }
        }
    }

    fun onUploadStatementClicked() {
        viewModelScope.launch {
            if (dataStoreManager.isGuestFlow.first()) {
                authRepository.signOut()
                _events.emit(DashboardEvent.NavigateToAuth)
            } else _events.emit(DashboardEvent.NavigateToUpload)
        }
    }

    fun onSignOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _events.emit(DashboardEvent.NavigateToAuth)
        }
    }
}

package com.payscope.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payscope.app.data.model.StatementAnalyticsDto
import com.payscope.app.data.repository.DashboardRepository
import com.payscope.app.data.repository.StatementRepository
import com.payscope.app.domain.model.BankStatementItem
import com.payscope.app.domain.model.WorkspaceActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val statements: List<BankStatementItem> = emptyList(),
    val activity: WorkspaceActivity? = null,
    val statementAnalytics: StatementAnalyticsDto? = null,
    val searchQuery: String = "",
    val expandedStatementId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val statementRepository: StatementRepository,
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                if (forceRefresh) {
                    dashboardRepository.refreshStrict()
                }
                val list = statementRepository.getImportHistory()
                val activity = dashboardRepository.getWorkspaceActivity()
                val analytics = dashboardRepository.getStatementAnalytics()
                _uiState.value = _uiState.value.copy(
                    statements = list,
                    activity = activity,
                    statementAnalytics = analytics,
                    isLoading = false
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Could not load statement history."
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onToggleExpandStatement(id: String) {
        val current = _uiState.value.expandedStatementId
        _uiState.value = _uiState.value.copy(
            expandedStatementId = if (current == id) null else id
        )
    }
}

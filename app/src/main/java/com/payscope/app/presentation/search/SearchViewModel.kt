package com.payscope.app.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payscope.app.data.repository.DashboardRepository
import com.payscope.app.domain.model.CommitmentItem
import com.payscope.app.domain.model.ImportedTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val selectedFilter: String = "All", // "All", "Subscriptions", "Transactions"
    val allCommitments: List<CommitmentItem> = emptyList(),
    val allTransactions: List<ImportedTransaction> = emptyList(),
    val filteredCommitments: List<CommitmentItem> = emptyList(),
    val filteredTransactions: List<ImportedTransaction> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val commitments = dashboardRepository.getCommitments()
                val activity = dashboardRepository.getWorkspaceActivity()
                val transactions = activity.transactions
                _uiState.value = _uiState.value.copy(
                    allCommitments = commitments,
                    allTransactions = transactions,
                    isLoading = false
                )
                applyFilter(_uiState.value.query, _uiState.value.selectedFilter)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        applyFilter(query, _uiState.value.selectedFilter)
    }

    fun onFilterSelected(filter: String) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        applyFilter(_uiState.value.query, filter)
    }

    private fun applyFilter(query: String, filter: String) {
        val q = query.trim().lowercase()
        val cList = if (filter == "Transactions") emptyList() else {
            if (q.isBlank()) _uiState.value.allCommitments else {
                _uiState.value.allCommitments.filter {
                    it.name.lowercase().contains(q) ||
                            it.merchantName.lowercase().contains(q) ||
                            it.category.lowercase().contains(q) ||
                            it.paymentRail.lowercase().contains(q)
                }
            }
        }

        val tList = if (filter == "Subscriptions") emptyList() else {
            if (q.isBlank()) _uiState.value.allTransactions.take(15) else {
                _uiState.value.allTransactions.filter {
                    it.description.lowercase().contains(q) ||
                            it.date.lowercase().contains(q) ||
                            it.amount.toString().contains(q)
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            filteredCommitments = cList,
            filteredTransactions = tList
        )
    }
}

package com.payscope.app.presentation.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payscope.app.data.repository.DashboardRepository
import com.payscope.app.domain.model.CommitmentItem
import com.payscope.app.domain.model.HistoricalDebit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StatusFilter(val label: String) {
    ALL("All"),
    CONFIRMED("Confirmed"),
    NEEDS_REVIEW("Needs Review"),
    POTENTIAL("Potential")
}

enum class SortOption(val label: String) {
    HIGHEST_COST("Highest Cost"),
    NEXT_RENEWAL("Next Renewal"),
    CONFIDENCE("Confidence")
}

data class SubscriptionsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val cancellationPlaybook: com.payscope.app.domain.model.CancellationPlaybook? = null,
    val commitments: List<CommitmentItem> = emptyList(),
    val filteredCommitments: List<CommitmentItem> = emptyList(),
    val searchQuery: String = "",
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val cadenceFilter: String = "All",
    val sortOption: SortOption = SortOption.HIGHEST_COST,
    val selectedCommitment: CommitmentItem? = null,
    val selectedHistoricalDebits: List<HistoricalDebit> = emptyList(),
    val isEditing: Boolean = false
)

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val cancellationRepository: com.payscope.app.data.repository.CancellationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionsUiState())
    val uiState: StateFlow<SubscriptionsUiState> = _uiState.asStateFlow()

    init {
        loadSubscriptions()
    }

    fun loadSubscriptions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val list = dashboardRepository.getCommitments()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                commitments = list,
                errorMessage = dashboardRepository.lastError
            )
            applyFilters()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun onStatusFilterSelected(filter: StatusFilter) {
        _uiState.value = _uiState.value.copy(statusFilter = filter)
        applyFilters()
    }

    fun onCadenceFilterSelected(cadence: String) {
        _uiState.value = _uiState.value.copy(cadenceFilter = cadence)
        applyFilters()
    }

    fun onSortOptionSelected(sort: SortOption) {
        _uiState.value = _uiState.value.copy(sortOption = sort)
        applyFilters()
    }

    fun onCommitmentSelected(commitment: CommitmentItem) {
        viewModelScope.launch {
            val debits = try { dashboardRepository.getHistoricalDebits(commitment.id) } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
                emptyList()
            }
            _uiState.value = _uiState.value.copy(
                selectedCommitment = commitment,
                selectedHistoricalDebits = debits
            )
        }
    }

    fun onDismissDetails() {
        _uiState.value = _uiState.value.copy(
            selectedCommitment = null,
            selectedHistoricalDebits = emptyList(),
            isEditing = false
        )
    }

    fun onConfirmCommitment(id: String) {
        viewModelScope.launch {
            dashboardRepository.updateCommitment(id = id, status = "Confirmed").onFailure { _uiState.value = _uiState.value.copy(errorMessage = it.localizedMessage); return@launch }
            loadSubscriptions()
        }
    }

    fun onDismissCommitment(id: String) {
        viewModelScope.launch {
            dashboardRepository.updateCommitment(id = id, status = "Dismissed").onFailure { _uiState.value = _uiState.value.copy(errorMessage = it.localizedMessage); return@launch }
            loadSubscriptions()
        }
    }

    fun onUpdateCommitment(
        id: String,
        name: String,
        category: String,
        cadence: String,
        amount: Double
    ) {
        viewModelScope.launch {
            dashboardRepository.updateCommitment(
                id = id,
                name = name,
                category = category,
                cadence = cadence,
                amount = amount
            ).onFailure { _uiState.value = _uiState.value.copy(errorMessage = it.localizedMessage); return@launch }
            onDismissDetails()
            loadSubscriptions()
        }
    }

    fun startCancellation(c: CommitmentItem) {
        viewModelScope.launch {
            try { _uiState.value = _uiState.value.copy(cancellationPlaybook = cancellationRepository.getCancellationPlaybook(c.id, c.merchantName, c.paymentRail)) }
            catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage) }
        }
    }
    fun dismissCancellation() { _uiState.value = _uiState.value.copy(cancellationPlaybook = null) }
    fun reportCancellation() {
        val p = _uiState.value.cancellationPlaybook ?: return
        viewModelScope.launch {
            cancellationRepository.recordCancellation(p.commitmentId, p.paymentMechanism).fold(
                onSuccess = { dismissCancellation(); loadSubscriptions() },
                onFailure = { _uiState.value = _uiState.value.copy(errorMessage = it.localizedMessage) })
        }
    }

    private fun applyFilters() {
        val q = _uiState.value.searchQuery.trim().lowercase()
        val sFilter = _uiState.value.statusFilter
        val cFilter = _uiState.value.cadenceFilter
        val sort = _uiState.value.sortOption

        var result = _uiState.value.commitments.filter { item ->
            val matchesQuery = q.isEmpty() || item.name.lowercase().contains(q) || item.merchantName.lowercase().contains(q)
            val matchesStatus = when (sFilter) {
                StatusFilter.ALL -> true
                StatusFilter.CONFIRMED -> item.status.equals("Confirmed", ignoreCase = true)
                StatusFilter.NEEDS_REVIEW -> item.status.equals("Needs Review", ignoreCase = true)
                StatusFilter.POTENTIAL -> item.status.equals("Potential", ignoreCase = true)
            }
            val matchesCadence = cFilter == "All" || item.cadence.equals(cFilter, ignoreCase = true)

            matchesQuery && matchesStatus && matchesCadence
        }

        result = when (sort) {
            SortOption.HIGHEST_COST -> result.sortedByDescending { it.normalizedMonthlyAmount }
            SortOption.NEXT_RENEWAL -> result.sortedBy { it.nextBillingDate }
            SortOption.CONFIDENCE -> result.sortedByDescending { it.confidenceScore }
        }

        _uiState.value = _uiState.value.copy(filteredCommitments = result)
    }
}

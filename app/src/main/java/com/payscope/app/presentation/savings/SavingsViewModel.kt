package com.payscope.app.presentation.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payscope.app.data.repository.DashboardRepository
import com.payscope.app.data.repository.SavingsCalculationResult
import com.payscope.app.data.repository.SavingsRepository
import com.payscope.app.domain.model.CommitmentItem
import com.payscope.app.domain.model.SavedSavingsPlan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavingsUiState(
    val includesEstimates: Boolean = false,
    val analysis: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val commitments: List<CommitmentItem> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val calculation: SavingsCalculationResult = SavingsCalculationResult(0.0, 0.0, 0.0, 0.0, 0),
    val savedPlans: List<SavedSavingsPlan> = emptyList(),
    val isSavingPlan: Boolean = false
)

@HiltViewModel
class SavingsViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val savingsRepository: SavingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavingsUiState())
    val uiState: StateFlow<SavingsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val commitments = dashboardRepository.getCommitments().filter {
                it.status in setOf("Confirmed", "Potential", "Needs Review") && it.currency == "INR" && it.annualizedAmount > 0
            }
            val analysis = dashboardRepository.getStatementAnalytics().savingsAnalysis
            val plans = try { savingsRepository.getSavedPlans() } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.localizedMessage)
                return@launch
            }

            val defaultSelected = _uiState.value.selectedIds.intersect(commitments.map { it.id }.toSet())

            val calc = savingsRepository.calculateSavings(commitments, defaultSelected)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                commitments = commitments,
                includesEstimates = commitments.any { it.status != "Confirmed" },
                analysis = analysis,
                selectedIds = defaultSelected,
                calculation = calc,
                savedPlans = plans,
                errorMessage = dashboardRepository.lastError
            )
        }
    }

    fun onToggleCommitment(id: String) {
        val current = _uiState.value.selectedIds.toMutableSet()
        if (id in current) {
            current.remove(id)
        } else {
            current.add(id)
        }
        val calc = savingsRepository.calculateSavings(_uiState.value.commitments, current)
        _uiState.value = _uiState.value.copy(
            selectedIds = current,
            calculation = calc
        )
    }

    fun onSavePlan(name: String) {
        val state = _uiState.value
        if (state.selectedIds.isEmpty() || name.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingPlan = true)
            savingsRepository.savePlan(
                name = name,
                selectedCommitmentIds = state.selectedIds.toList(),
                annualSavings = state.calculation.selectedAnnualSavings,
                projectedAnnualCost = state.calculation.projectedAnnualCost,
                includeCandidates = state.includesEstimates
            ).onFailure { _uiState.value = _uiState.value.copy(isSavingPlan = false, errorMessage = it.localizedMessage); return@launch }
            val updatedPlans = savingsRepository.getSavedPlans()
            _uiState.value = _uiState.value.copy(
                isSavingPlan = false,
                savedPlans = updatedPlans
            )
        }
    }

    fun onDeletePlan(planId: String) {
        viewModelScope.launch {
            savingsRepository.deletePlan(planId).onFailure { _uiState.value = _uiState.value.copy(errorMessage = it.localizedMessage); return@launch }
            val updatedPlans = savingsRepository.getSavedPlans()
            _uiState.value = _uiState.value.copy(savedPlans = updatedPlans)
        }
    }

    fun onLoadSavedPlan(plan: SavedSavingsPlan) {
        val newSelected = plan.selectedCommitmentIds.toSet()
        val calc = savingsRepository.calculateSavings(_uiState.value.commitments, newSelected)
        _uiState.value = _uiState.value.copy(
            selectedIds = newSelected,
            calculation = calc
        )
    }
}

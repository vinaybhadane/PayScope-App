package com.payscope.app.presentation.profile

import com.payscope.app.data.api.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payscope.app.data.local.DataStoreManager
import com.payscope.app.data.repository.AuthRepository
import com.payscope.app.data.repository.DashboardRepository
import com.payscope.app.data.repository.StatementRepository
import com.payscope.app.domain.model.BankStatementItem
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

sealed interface ProfileEvent {
    data object NavigateToAuth : ProfileEvent
    data class ShowMessage(val text: String) : ProfileEvent
}

data class ProfileUiState(
    val userName: String = "Your workspace",
    val email: String = "user@payscope.app",
    val workspaceId: String = "workspace-default",
    val isGuest: Boolean = false,
    val statementsCount: Int = 0,
    val activeSubscriptionsCount: Int = 0,
    val currency: String = "INR (₹)",
    val renewalAlertsEnabled: Boolean = false,
    val alertDaysBefore: Int = 3,
    val statementHistory: List<BankStatementItem> = emptyList(),
    val errorMessage: String? = null,
    val exportSummaryText: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dataStoreManager: DataStoreManager,
    private val statementRepository: StatementRepository,
    private val dashboardRepository: DashboardRepository,
    private val api: PayScopeApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProfileEvent>()
    val events: SharedFlow<ProfileEvent> = _events.asSharedFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            try {
            val isGuest = dataStoreManager.isGuestFlow.first()
            val email = dataStoreManager.userEmailFlow.first() ?: if (isGuest) "guest@payscope.demo" else "user@payscope.app"
            val name = dataStoreManager.userNameFlow.first()
            val summary = dashboardRepository.getFinancialSummary()
            val history = statementRepository.getImportHistory()
            val token = dataStoreManager.authTokenFlow.first() ?: error("Please sign in again.")
            val emailSettings = api.getEmailSettings("Bearer $token").requireBody()

            _uiState.value = _uiState.value.copy(
                userName = name,
                email = email,
                workspaceId = dataStoreManager.workspaceIdFlow.first().orEmpty(),
                statementHistory = history, statementsCount = history.size,
                renewalAlertsEnabled = emailSettings["reminders_enabled"] == true,
                isGuest = isGuest,
                errorMessage = dashboardRepository.lastError,
                activeSubscriptionsCount = summary.activeSubscriptionsCount
            )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage ?: "Could not load your profile.")
            }
        }
    }

    fun onToggleRenewalAlerts(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val token = dataStoreManager.authTokenFlow.first() ?: error("Please sign in again.")
                val current = api.getEmailSettings("Bearer $token").requireBody()
                api.updateEmailSettings("Bearer $token", mapOf("reports_enabled" to (current["reports_enabled"] == true),
                    "reminders_enabled" to enabled)).requireSuccess()
                _uiState.value = _uiState.value.copy(renewalAlertsEnabled = enabled)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _events.emit(ProfileEvent.ShowMessage(e.localizedMessage ?: "Could not update reminders."))
            }
        }
    }

    fun onExportData() {
        viewModelScope.launch {
            try {
            val summary = dashboardRepository.getFinancialSummary()
            check(dashboardRepository.lastError == null) { dashboardRepository.lastError.orEmpty() }
            val commitments = dashboardRepository.getCommitments()
            val text = buildString {
                appendLine("=== PayScope Financial Intelligence Audit Report ===")
                appendLine("Generated: ${java.time.LocalDate.now()}")
                appendLine("Workspace: ${_uiState.value.workspaceId}")
                appendLine("Monthly Recurring Spend: ₹${summary.monthlyRecurringSpend.toInt()}")
                appendLine("Annual Commitment: ₹${summary.annualizedSpend.toInt()}")
                appendLine("Potential Annual Savings: ₹${summary.potentialSavingsAnnual.toInt()}")
                appendLine()
                appendLine("--- Monitored Subscriptions ---")
                commitments.forEach { c ->
                    appendLine("• ${c.name} (${c.merchantName}): ₹${c.amount.toInt()} / ${c.cadence.lowercase()} via ${c.paymentRail} [${c.status}]")
                }
            }
            _uiState.value = _uiState.value.copy(exportSummaryText = text)
            _events.emit(ProfileEvent.ShowMessage("Financial audit export generated successfully."))
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _events.emit(ProfileEvent.ShowMessage(e.localizedMessage ?: "Could not generate export."))
            }
        }
    }

    fun onDismissExport() {
        _uiState.value = _uiState.value.copy(exportSummaryText = null)
    }

    fun onDeleteWorkspaceData() {
        viewModelScope.launch {
            statementRepository.deleteWorkspaceData().onFailure {
                _events.emit(ProfileEvent.ShowMessage(it.localizedMessage ?: "Could not clear workspace. Please retry."))
                return@launch
            }
            _events.emit(ProfileEvent.ShowMessage("Workspace data safely deleted."))
            authRepository.signOut()
            _events.emit(ProfileEvent.NavigateToAuth)
        }
    }

    fun onSignOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _events.emit(ProfileEvent.NavigateToAuth)
        }
    }
}

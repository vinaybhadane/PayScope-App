package com.payscope.app.presentation.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payscope.app.core.util.FinancialAuditPdfGenerator
import com.payscope.app.data.api.*
import com.payscope.app.data.local.DataStoreManager
import com.payscope.app.data.repository.AuthRepository
import com.payscope.app.data.repository.DashboardRepository
import com.payscope.app.data.repository.StatementRepository
import com.payscope.app.domain.model.BankStatementItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
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
    val themeMode: String = "dark",
    val statementHistory: List<BankStatementItem> = emptyList(),
    val errorMessage: String? = null,
    val exportPdfFile: File? = null,
    val isExporting: Boolean = false,
    val userPhotoUrl: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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
                val photoUrl = dataStoreManager.userPhotoUrlFlow.first()
                    ?: runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.photoUrl?.toString() }.getOrNull()
                val theme = dataStoreManager.themeModeFlow.first()
                val summary = dashboardRepository.getFinancialSummary()
                val history = statementRepository.getImportHistory()

                _uiState.value = _uiState.value.copy(
                    userName = name,
                    email = email,
                    workspaceId = dataStoreManager.workspaceIdFlow.first().orEmpty(),
                    statementHistory = history,
                    statementsCount = history.size,
                    themeMode = theme,
                    isGuest = isGuest,
                    userPhotoUrl = photoUrl,
                    errorMessage = dashboardRepository.lastError,
                    activeSubscriptionsCount = summary.activeSubscriptionsCount
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage ?: "Could not load your profile.")
            }
        }
    }

    fun onSetThemeMode(mode: String) {
        viewModelScope.launch {
            dataStoreManager.setThemeMode(mode)
            _uiState.value = _uiState.value.copy(themeMode = mode)
        }
    }

    fun onExportData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isExporting = true)
                val summary = dashboardRepository.getFinancialSummary()
                check(dashboardRepository.lastError == null) { dashboardRepository.lastError.orEmpty() }
                val commitments = dashboardRepository.getCommitments()
                val activity = dashboardRepository.getWorkspaceActivity()
                val priceHikes = dashboardRepository.getPriceHikes()

                val pdfFile = FinancialAuditPdfGenerator.generateAuditPdf(
                    context = context,
                    workspaceId = _uiState.value.workspaceId,
                    userName = _uiState.value.userName,
                    userEmail = _uiState.value.email,
                    summary = summary,
                    commitments = commitments,
                    activity = activity,
                    priceHikes = priceHikes
                )

                _uiState.value = _uiState.value.copy(exportPdfFile = pdfFile, isExporting = false)
                _events.emit(ProfileEvent.ShowMessage("Financial audit PDF report generated successfully."))
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.value = _uiState.value.copy(isExporting = false)
                _events.emit(ProfileEvent.ShowMessage(e.localizedMessage ?: "Could not generate PDF export."))
            }
        }
    }

    fun onDismissExport() {
        _uiState.value = _uiState.value.copy(exportPdfFile = null)
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

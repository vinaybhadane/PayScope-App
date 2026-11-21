package com.payscope.app.presentation.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payscope.app.data.local.DataStoreManager
import com.payscope.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface WelcomeEvent {
    data object NavigateToAuth : WelcomeEvent
    data object NavigateToDemoDashboard : WelcomeEvent
}

data class WelcomeUiState(
    val isStartingDemo: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<WelcomeEvent>()
    val events: SharedFlow<WelcomeEvent> = _events.asSharedFlow()

    fun toggleTheme(isCurrentlyDark: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setThemeMode(if (isCurrentlyDark) "light" else "dark")
        }
    }

    fun onGetStartedClicked() {
        viewModelScope.launch {
            _events.emit(WelcomeEvent.NavigateToAuth)
        }
    }

    fun onExploreDemoClicked() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isStartingDemo = true)
            val result = authRepository.createGuestSession()
            _uiState.value = _uiState.value.copy(isStartingDemo = false)
            result.fold(onSuccess = { _events.emit(WelcomeEvent.NavigateToDemoDashboard) },
                onFailure = { _uiState.value = _uiState.value.copy(errorMessage = it.localizedMessage) })
        }
    }
}

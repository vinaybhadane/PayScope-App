package com.payscope.app.presentation.upload

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payscope.app.data.repository.SelectedFile
import com.payscope.app.data.repository.StatementRepository
import com.payscope.app.domain.model.UploadStage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UploadUiState(
    val selectedFile: SelectedFile? = null,
    val isProcessing: Boolean = false,
    val currentStage: UploadStage? = null,
    val completed: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class UploadViewModel @Inject constructor(private val repository: StatementRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState = _uiState.asStateFlow()

    fun onFileSelected(uri: Uri) {
        if (_uiState.value.isProcessing) return
        repository.validateFile(uri).fold(
            onSuccess = { file ->
                _uiState.value = UploadUiState(selectedFile = file)
                start { repository.processStatement(file) }
            },
            onFailure = { _uiState.value = UploadUiState(errorMessage = it.localizedMessage) })
    }

    fun onRetryImport() {
        val file = _uiState.value.selectedFile ?: return
        start { repository.processStatement(file) }
    }

    fun onUseSampleStatement() { start { repository.processSampleStatement() } }

    private fun start(stages: () -> Flow<UploadStage>) {
        if (_uiState.value.isProcessing) return
        _uiState.value = _uiState.value.copy(isProcessing = true, completed = false,
            currentStage = UploadStage.READING, errorMessage = null)
        viewModelScope.launch {
            try {
                stages().collect { stage ->
                    _uiState.value = _uiState.value.copy(currentStage = stage, completed = stage == UploadStage.COMPLETED)
                }
                // Remain on the loading surface until the completed state navigates.
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(isProcessing = false,
                    errorMessage = e.localizedMessage ?: "Could not import this statement. Please retry.")
            }
        }
    }
}

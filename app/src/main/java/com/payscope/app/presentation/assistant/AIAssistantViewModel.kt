package com.payscope.app.presentation.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payscope.app.data.repository.AssistantRepository
import com.payscope.app.domain.model.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class AssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isTyping: Boolean = false,
    val suggestedPrompts: List<String> = listOf(
        "How much am I spending each month?",
        "Which subscriptions increased in price?",
        "Show me savings scenarios",
        "Which renewals are coming up?"
    )
)

@HiltViewModel
class AIAssistantViewModel @Inject constructor(
    private val assistantRepository: AssistantRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    init {
        // Welcome message
        val welcomeMsg = ChatMessage(
            id = "welcome",
            content = "Hi! Ask me about your spending, subscriptions, price changes, or ways to save. I will use the records in your workspace.",
            isUser = false,
            timestamp = timeFormat.format(Date()),
            suggestions = _uiState.value.suggestedPrompts
        )
        _uiState.value = _uiState.value.copy(messages = listOf(welcomeMsg))
    }

    fun onSendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _uiState.value.isTyping) return

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = trimmed,
            isUser = true,
            timestamp = timeFormat.format(Date())
        )

        val updatedList = _uiState.value.messages + userMsg
        _uiState.value = _uiState.value.copy(
            messages = updatedList,
            isTyping = true
        )

        viewModelScope.launch {
            val response = try {
                assistantRepository.sendMessage(trimmed, updatedList.dropLast(1).filter { it.id != "welcome" })
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                ChatMessage(UUID.randomUUID().toString(), e.localizedMessage ?: "Assistant unavailable. Please retry.", false, timeFormat.format(Date()))
            }
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + response,
                isTyping = false,
                suggestedPrompts = if (response.suggestions.isNotEmpty()) response.suggestions else _uiState.value.suggestedPrompts
            )
        }
    }
}

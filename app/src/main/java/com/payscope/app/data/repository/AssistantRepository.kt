package com.payscope.app.data.repository

import com.payscope.app.data.api.*
import com.payscope.app.data.local.DataStoreManager
import com.payscope.app.data.model.*
import com.payscope.app.domain.model.ChatMessage
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssistantRepository @Inject constructor(
    private val apiService: PayScopeApiService,
    private val dataStoreManager: DataStoreManager
) {
    suspend fun sendMessage(query: String, history: List<ChatMessage>): ChatMessage {
        val token = dataStoreManager.authTokenFlow.first() ?: error("Please sign in again.")
        val response = apiService.chatWithAssistant("Bearer $token", ChatRequestDto(query,
            history.takeLast(10).map { ChatMessageDto(if (it.isUser) "user" else "assistant", it.content) })).requireBody()
        return ChatMessage(UUID.randomUUID().toString(), response.reply, false,
            LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a")), response.suggestions)
    }
}

package com.payscope.app.data.repository

import com.payscope.app.data.api.*
import com.payscope.app.data.local.DataStoreManager
import com.payscope.app.domain.model.CancellationPlaybook
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CancellationRepository @Inject constructor(
    private val apiService: PayScopeApiService,
    private val dataStoreManager: DataStoreManager
) {
    private suspend fun auth() = "Bearer ${dataStoreManager.authTokenFlow.first() ?: error("Please sign in again.")}"
    suspend fun getCancellationPlaybook(commitmentId: String, merchantName: String, paymentRail: String): CancellationPlaybook {
        val e = apiService.getCancellationEligibility(auth(), commitmentId).requireBody()
        return CancellationPlaybook(commitmentId, e.name, e.mechanism,
            e.playbook.steps.map { it.instruction }, e.playbook.url,
            warningNote = e.playbook.warnings.joinToString("\n").ifBlank { null })
    }
    suspend fun recordCancellation(commitmentId: String, cancellationMethod: String, notes: String? = null): Result<Unit> = try {
        val token = auth()
        val case = apiService.createCancellationCase(token, mapOf("commitment_id" to commitmentId)).requireBody()
        apiService.reportCancelled(token, case.id, mapOf("user_notes" to (notes ?: "Reported from Android"))).requireSuccess()
        Result.success(Unit)
    } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; Result.failure(e) }
}

package com.payscope.app.data.repository

import com.payscope.app.data.api.*
import com.payscope.app.data.local.DataStoreManager
import com.payscope.app.data.model.SavePlanDto
import com.payscope.app.domain.model.CommitmentItem
import com.payscope.app.domain.model.SavedSavingsPlan
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class SavingsCalculationResult(
    val baselineAnnualCost: Double,
    val selectedAnnualSavings: Double,
    val projectedAnnualCost: Double,
    val percentageReduction: Double,
    val selectedCount: Int
)

@Singleton
class SavingsRepository @Inject constructor(
    private val apiService: PayScopeApiService,
    private val dataStoreManager: DataStoreManager
) {
    fun calculateSavings(
        allCommitments: List<CommitmentItem>,
        selectedIds: Set<String>
    ): SavingsCalculationResult {
        val baselineAnnual = allCommitments.sumOf {
            it.annualizedAmount
        }

        val selectedCommitments = allCommitments.filter { it.id in selectedIds }
        val annualSavings = selectedCommitments.sumOf {
            it.annualizedAmount
        }

        val projected = (baselineAnnual - annualSavings).coerceAtLeast(0.0)
        val percentage = if (baselineAnnual > 0) (annualSavings / baselineAnnual) * 100.0 else 0.0

        return SavingsCalculationResult(
            baselineAnnualCost = baselineAnnual,
            selectedAnnualSavings = annualSavings,
            projectedAnnualCost = projected,
            percentageReduction = percentage,
            selectedCount = selectedCommitments.size
        )
    }


    private suspend fun auth() = "Bearer ${dataStoreManager.authTokenFlow.first() ?: error("Please sign in again.")}"

    suspend fun savePlan(name: String, selectedCommitmentIds: List<String>, annualSavings: Double,
        projectedAnnualCost: Double, includeCandidates: Boolean = false): Result<SavedSavingsPlan> = try {
        val saved = apiService.saveSavingsPlan(auth(), SavePlanDto(name, selectedCommitmentIds,
            annualSavings, projectedAnnualCost, includeCandidates)).requireBody()
        Result.success(SavedSavingsPlan(saved.id, saved.name, saved.selectedCommitmentIds, saved.annualSavings,
            saved.projectedAnnualCost, saved.createdAt.take(10)))
    } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; Result.failure(e) }

    suspend fun getSavedPlans(): List<SavedSavingsPlan> = apiService.getSavedPlans(auth()).requireBody().map {
        SavedSavingsPlan(it.id, it.name, it.selectedCommitmentIds, it.annualSavings, it.projectedAnnualCost, it.createdAt.take(10))
    }

    suspend fun deletePlan(planId: String): Result<Unit> = try {
        apiService.deleteSavedPlan(auth(), planId).requireSuccess()
        Result.success(Unit)
    } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; Result.failure(e) }
}

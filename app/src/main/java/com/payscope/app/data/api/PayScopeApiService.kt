package com.payscope.app.data.api

import com.payscope.app.data.model.CancellationPlaybookDto
import com.payscope.app.data.model.ChatRequestDto
import com.payscope.app.data.model.ChatResponseDto
import com.payscope.app.data.model.CommitmentDto
import com.payscope.app.data.model.DashboardSummaryDto
import com.payscope.app.data.model.GuestSessionResponse
import com.payscope.app.data.model.RecordCancellationDto
import com.payscope.app.data.model.SavePlanDto
import com.payscope.app.data.model.SavedPlanDto
import com.payscope.app.data.model.StatementPreviewResponseDto
import com.payscope.app.data.model.StatementUploadResponseDto
import com.payscope.app.data.model.UpdateCommitmentDto
import com.payscope.app.data.model.UserProfileDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query

interface PayScopeApiService {

    @POST("api/v1/auth/guest")
    suspend fun createGuestSession(): Response<GuestSessionResponse>

    @GET("api/v1/auth/me")
    suspend fun getCurrentUser(
        @Header("Authorization") token: String
    ): Response<UserProfileDto>

    @GET("api/v1/mobile/dashboard")
    suspend fun getDashboardSummary(
        @Header("Authorization") token: String
    ): Response<DashboardSummaryDto>

    @GET("api/v1/commitments")
    suspend fun getCommitments(
        @Header("Authorization") token: String
    ): Response<List<CommitmentDto>>

    @PATCH("api/v1/commitments/{id}")
    suspend fun updateCommitment(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body update: UpdateCommitmentDto
    ): Response<CommitmentDto>

    @Multipart
    @POST("api/v1/imports/preview")
    suspend fun previewStatement(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part,
        @Part("synthetic_confirmed") syntheticConfirmed: RequestBody,
        @Part("sync") sync: RequestBody? = null
    ): Response<StatementPreviewResponseDto>

    @Multipart
    @POST("api/v1/imports")
    suspend fun uploadStatement(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part,
        @Part("synthetic_confirmed") syntheticConfirmed: RequestBody,
        @Part("sync") sync: RequestBody? = null
    ): Response<StatementUploadResponseDto>

    @GET("api/v1/imports/jobs/{jobId}")
    suspend fun getImportJob(
        @Header("Authorization") token: String,
        @Path("jobId") jobId: String
    ): Response<com.payscope.app.data.model.ImportJobDto>

    @POST("api/v1/assistant/chat")
    suspend fun chatWithAssistant(
        @Header("Authorization") token: String,
        @Body request: ChatRequestDto
    ): Response<ChatResponseDto>

    @GET("api/v1/cancellations/playbook/{id}")
    suspend fun getCancellationPlaybook(
        @Header("Authorization") token: String,
        @Path("id") commitmentId: String
    ): Response<CancellationPlaybookDto>

    @POST("api/v1/cancellations/record")
    suspend fun recordCancellation(
        @Header("Authorization") token: String,
        @Body request: RecordCancellationDto
    ): Response<Map<String, Any>>

    @POST("api/v1/savings/scenarios")
    suspend fun saveSavingsPlan(
        @Header("Authorization") token: String,
        @Body plan: SavePlanDto
    ): Response<SavedPlanDto>

    @GET("api/v1/savings/scenarios")
    suspend fun getSavedPlans(
        @Header("Authorization") token: String
    ): Response<List<SavedPlanDto>>

    @DELETE("api/v1/savings/scenarios/{id}")
    suspend fun deleteSavedPlan(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Map<String, Any>>

    @POST("api/v1/workspace/clear")
    suspend fun deleteWorkspaceData(
        @Header("Authorization") token: String,
        @Body confirmation: Map<String, String> = mapOf("confirmation" to "CLEAR")
    ): Response<Map<String, Any>>

    // Neo4j Graph Relationship Explorer Endpoints
    @GET("api/v1/graph/explorer")
    suspend fun getGraphExplorer(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 50
    ): Response<com.payscope.app.data.model.GraphElementsDto>

    @GET("api/v1/graph/health")
    suspend fun getGraphHealth(
        @Header("Authorization") token: String
    ): Response<com.payscope.app.data.model.GraphHealthDto>

    @POST("api/v1/graph/sync")
    suspend fun syncGraph(
        @Header("Authorization") token: String
    ): Response<com.payscope.app.data.model.GraphSyncDto>

    @POST("api/v1/demo/seed")
    suspend fun seedDemo(@Header("Authorization") token: String): Response<Map<String, Any>>

    @GET("api/v1/commitments/{id}")
    suspend fun getCommitmentDetail(@Header("Authorization") token: String, @Path("id") id: String): Response<com.payscope.app.data.model.CommitmentDetailDto>

    @GET("api/v1/imports")
    suspend fun getImports(@Header("Authorization") token: String): Response<com.payscope.app.data.model.ImportHistoryDto>

    @GET("api/v1/cancellations/eligibility/{id}")
    suspend fun getCancellationEligibility(@Header("Authorization") token: String, @Path("id") id: String): Response<com.payscope.app.data.model.CancellationEligibilityDto>

    @POST("api/v1/cancellations")
    suspend fun createCancellationCase(@Header("Authorization") token: String, @Body request: Map<String, String>): Response<com.payscope.app.data.model.CancellationCaseDto>

    @POST("api/v1/cancellations/{id}/report-cancelled")
    suspend fun reportCancelled(@Header("Authorization") token: String, @Path("id") id: String, @Body request: Map<String, String>): Response<Map<String, Any>>

    @POST("api/v1/workspace/review")
    suspend fun retryAnalysis(@Header("Authorization") token: String): Response<Map<String, Any>>

    @GET("api/v1/workspace/email-settings")
    suspend fun getEmailSettings(@Header("Authorization") token: String): Response<Map<String, Any>>

    @retrofit2.http.PUT("api/v1/workspace/email-settings")
    suspend fun updateEmailSettings(@Header("Authorization") token: String, @Body settings: Map<String, Boolean>): Response<Map<String, Any>>

    @GET("api/v1/imports/{id}/status")
    suspend fun getImportStatus(@Header("Authorization") token: String, @Path("id") id: String): Response<com.payscope.app.data.model.ImportStatusDto>
}

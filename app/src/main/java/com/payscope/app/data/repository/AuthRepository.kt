package com.payscope.app.data.repository

import com.payscope.app.data.api.PayScopeApiService
import com.payscope.app.data.api.requireBody
import com.payscope.app.data.api.requireSuccess
import kotlinx.coroutines.CancellationException
import com.google.firebase.auth.FirebaseAuth
import com.payscope.app.data.local.DataStoreManager
import com.payscope.app.domain.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: PayScopeApiService,
    private val dataStoreManager: DataStoreManager
) {

    val currentSessionFlow: Flow<UserSession?> = dataStoreManager.authTokenFlow.map { token ->
        if (token == null) null
        else {
            val isGuest = dataStoreManager.isGuestFlow.first()
            val email = dataStoreManager.userEmailFlow.first() ?: (if (isGuest) "guest@payscope.demo" else "user@payscope.app")
            val name = dataStoreManager.userNameFlow.first()
            val hasUploaded = dataStoreManager.hasUploadedStatementsFlow.first()
            val photoUrl = dataStoreManager.userPhotoUrlFlow.first()
                ?: runCatching { FirebaseAuth.getInstance().currentUser?.photoUrl?.toString() }.getOrNull()
            UserSession(
                userId = dataStoreManager.workspaceIdFlow.first().orEmpty(),
                email = email,
                displayName = name,
                workspaceId = dataStoreManager.workspaceIdFlow.first().orEmpty(),
                isGuest = isGuest,
                token = token,
                hasUploadedStatements = hasUploaded,
                photoUrl = photoUrl
            )
        }
    }

    suspend fun createGuestSession(): Result<UserSession> = try {
        val body = apiService.createGuestSession().requireBody()
        val token = body.accessToken ?: error("Could not create a sample session.")
        apiService.seedDemo("Bearer $token").requireSuccess()
        val profile = apiService.getCurrentUser("Bearer $token").requireBody()
        val workspace = profile.id ?: error("Could not load the sample workspace.")
        dataStoreManager.saveSession(token, workspace, "guest@payscope.demo", "Sample workspace", true, true)
        Result.success(UserSession(workspace, "guest@payscope.demo", "Sample workspace", workspace, true, token, true))
    } catch (e: Exception) { if (e is CancellationException) throw e; Result.failure(e) }

    suspend fun handleGoogleSignInSuccess(
        idToken: String,
        email: String,
        displayName: String,
        photoUrl: String? = null
    ): Result<UserSession> {
        return try {
            val profile = apiService.getCurrentUser("Bearer $idToken").requireBody()
            val workspaceId = profile.id ?: error("Could not load your workspace.")
            val hasUploaded = apiService.getImports("Bearer $idToken").requireBody().items.isNotEmpty()
            dataStoreManager.saveSession(idToken, workspaceId, email, displayName.ifBlank { "PayScope User" }, false, hasUploaded, photoUrl)
            Result.success(UserSession(workspaceId, email, displayName, workspaceId, false, idToken, hasUploaded, photoUrl))
        } catch (e: Exception) { if (e is CancellationException) throw e; Result.failure(e) }
    }

    suspend fun signOut() {
        runCatching { FirebaseAuth.getInstance().signOut() }
        dataStoreManager.clearSession()
    }
}

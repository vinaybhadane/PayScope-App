package com.payscope.app.presentation.auth

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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

sealed interface AuthEvent {
    data object NavigateToUpload : AuthEvent
    data object NavigateToDashboard : AuthEvent
    data class ShowError(val message: String) : AuthEvent
}

data class AuthUiState(
    val isLoadingGoogle: Boolean = false,
    val isLoadingGuest: Boolean = false,
    val errorMessage: String? = null,
    val firebaseConfigWarning: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>()
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    fun toggleTheme(isCurrentlyDark: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setThemeMode(if (isCurrentlyDark) "light" else "dark")
        }
    }

    companion object {
        const val DEBUG_SHA1 = "E0:09:D9:D7:1D:E8:68:76:33:C7:09:BE:FD:90:55:07:64:D0:20:FA"
    }

    /**
     * Prepares and triggers the official Google Play Services Account Chooser intent.
     * Calling signOut first guarantees that Android displays the account picker sheet
     * so the user can explicitly select which Google account to use.
     */
    fun startGoogleSignIn(context: Context, launchIntent: (Intent) -> Unit) {
        if (!com.payscope.app.BuildConfig.FIREBASE_CONFIGURED) {
            _uiState.value = _uiState.value.copy(errorMessage = "Google sign-in is not configured for this build. Use a build with Firebase Android configuration.")
            return
        }
        _uiState.value = _uiState.value.copy(
            isLoadingGoogle = true,
            errorMessage = null,
            firebaseConfigWarning = null
        )

        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(com.payscope.app.BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .requestEmail()
                .requestProfile()
                .build()

            val client = GoogleSignIn.getClient(context, gso)

            // Sign out of previous client cache so Google account picker always appears
            client.signOut().addOnCompleteListener {
                try {
                    launchIntent(client.signInIntent)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoadingGoogle = false,
                        errorMessage = "Could not launch Google Sign-In: ${e.localizedMessage}"
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoadingGoogle = false,
                errorMessage = "Google Sign-In initialization failed: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Handles the result returned from the Google Account Picker sheet.
     */
    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account: GoogleSignInAccount = task.getResult(ApiException::class.java)

                val idToken = account.idToken
                val email = account.email ?: "user@payscope.app"
                val displayName = account.displayName ?: email
                val googlePhotoUrl = account.photoUrl?.toString()

                check(!idToken.isNullOrBlank()) { "Google did not return an identity token. Please retry." }
                val firebaseAuth = FirebaseAuth.getInstance()
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val firebaseResult = kotlinx.coroutines.suspendCancellableCoroutine<com.google.firebase.auth.AuthResult> { continuation ->
                    firebaseAuth.signInWithCredential(credential)
                        .addOnSuccessListener { if (continuation.isActive) continuation.resumeWith(Result.success(it)) }
                        .addOnFailureListener { if (continuation.isActive) continuation.resumeWith(Result.failure(it)) }
                }
                val firebaseUser = firebaseResult.user ?: error("Firebase sign-in failed.")
                val verifiedToken = kotlinx.coroutines.suspendCancellableCoroutine<String> { continuation ->
                    firebaseUser.getIdToken(true)
                        .addOnSuccessListener { result ->
                            if (continuation.isActive) continuation.resumeWith(
                                result.token?.let { Result.success(it) } ?: Result.failure(IllegalStateException("No Firebase token returned.")))
                        }
                        .addOnFailureListener { if (continuation.isActive) continuation.resumeWith(Result.failure(it)) }
                }

                val sessionResult = authRepository.handleGoogleSignInSuccess(
                    idToken = verifiedToken,
                    email = email,
                    displayName = displayName,
                    photoUrl = googlePhotoUrl ?: firebaseUser.photoUrl?.toString()
                )

                _uiState.value = _uiState.value.copy(isLoadingGoogle = false)

                sessionResult.fold(
                    onSuccess = { session ->
                        if (session.hasUploadedStatements) {
                            _events.emit(AuthEvent.NavigateToDashboard)
                        } else {
                            _events.emit(AuthEvent.NavigateToUpload)
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            errorMessage = error.localizedMessage ?: "Failed to save user session."
                        )
                    }
                )

            } catch (e: ApiException) {
                _uiState.value = _uiState.value.copy(isLoadingGoogle = false)

                if (e.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                    // User dismissed the account picker sheet without picking an account
                    return@launch
                }

                val errorDetail = when (e.statusCode) {
                    10 -> "Google Sign-In error (Developer Error 10). Add SHA-1 fingerprint ($DEBUG_SHA1) to Firebase Console > Project Settings > Android App (com.payscope.app)."
                    12500 -> "Google Play Services authentication failed (12500). Please verify network and Google Play Services."
                    GoogleSignInStatusCodes.NETWORK_ERROR -> "Network error during Google Sign-In. Check your internet connection."
                    else -> "Google Sign-In failed (${e.statusCode}): ${e.localizedMessage ?: "Unknown error"}"
                }

                _uiState.value = _uiState.value.copy(errorMessage = errorDetail)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingGoogle = false,
                    errorMessage = "Authentication failed: ${e.localizedMessage}"
                )
            }
        }
    }

    fun onDemoDashboardClicked() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingGuest = true, errorMessage = null)
            val result = authRepository.createGuestSession()
            _uiState.value = _uiState.value.copy(isLoadingGuest = false)

            result.fold(
                onSuccess = { _events.emit(AuthEvent.NavigateToDashboard) },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.localizedMessage ?: "Failed to initialize demo session."
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

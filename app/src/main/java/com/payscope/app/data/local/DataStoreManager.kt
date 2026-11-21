package com.payscope.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "payscope_session")

@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        val KEY_WORKSPACE_ID = stringPreferencesKey("workspace_id")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_USER_PHOTO_URL = stringPreferencesKey("user_photo_url")
        val KEY_IS_GUEST = booleanPreferencesKey("is_guest")
        val KEY_HAS_UPLOADED_STATEMENTS = booleanPreferencesKey("has_uploaded_statements")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode") // "dark", "light", "system"
    }

    val workspaceIdFlow: Flow<String?> = context.dataStore.data.map { it[KEY_WORKSPACE_ID] }
    val authTokenFlow: Flow<String?> = context.dataStore.data.map { it[KEY_AUTH_TOKEN] }
    val isGuestFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_IS_GUEST] ?: false }
    val userEmailFlow: Flow<String?> = context.dataStore.data.map { it[KEY_USER_EMAIL] }
    val userNameFlow: Flow<String> = context.dataStore.data.map { it[KEY_USER_NAME] ?: "Your workspace" }
    val userPhotoUrlFlow: Flow<String?> = context.dataStore.data.map { it[KEY_USER_PHOTO_URL] }
    val hasUploadedStatementsFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_HAS_UPLOADED_STATEMENTS] ?: false }
    val themeModeFlow: Flow<String> = context.dataStore.data.map { it[KEY_THEME_MODE] ?: "dark" }

    suspend fun saveSession(
        token: String,
        workspaceId: String,
        email: String,
        name: String,
        isGuest: Boolean,
        hasUploaded: Boolean = false,
        photoUrl: String? = null
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTH_TOKEN] = token
            prefs[KEY_WORKSPACE_ID] = workspaceId
            prefs[KEY_USER_EMAIL] = email
            prefs[KEY_USER_NAME] = name
            prefs[KEY_IS_GUEST] = isGuest
            prefs[KEY_HAS_UPLOADED_STATEMENTS] = hasUploaded
            if (photoUrl != null) {
                prefs[KEY_USER_PHOTO_URL] = photoUrl
            } else if (isGuest) {
                prefs.remove(KEY_USER_PHOTO_URL)
            }
        }
    }

    suspend fun setUserPhotoUrl(url: String?) {
        context.dataStore.edit { prefs ->
            if (url != null) prefs[KEY_USER_PHOTO_URL] = url
            else prefs.remove(KEY_USER_PHOTO_URL)
        }
    }

    suspend fun setHasUploadedStatements(hasUploaded: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HAS_UPLOADED_STATEMENTS] = hasUploaded
        }
    }

    suspend fun setIsGuest(isGuest: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_GUEST] = isGuest
        }
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_NAME] = name
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}

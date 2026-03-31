package com.privacynudge.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

/**
 * Data class representing the user profile.
 */
data class UserProfile(
    val name: String = "",
    val dob: String = "",
    val phone: String = "",
    val gmail: String = "",
    val profilePicUri: String = ""
)

/**
 * Manages user authentication and registration data.
 */
class AuthRepository(private val context: Context) {

    companion object {
        private val IS_AUTHENTICATED = booleanPreferencesKey("is_authenticated")
        private val USER_NAME = stringPreferencesKey("user_name")
        private val USER_DOB = stringPreferencesKey("user_dob")
        private val USER_PHONE = stringPreferencesKey("user_phone")
        private val USER_GMAIL = stringPreferencesKey("user_gmail")
        private val USER_PASSWORD = stringPreferencesKey("user_password")
        private val USER_PROFILE_PIC_URI = stringPreferencesKey("user_profile_pic_uri")
    }

    /**
     * Flow of the authentication status.
     */
    val isAuthenticated: Flow<Boolean> = context.authDataStore.data.map { it[IS_AUTHENTICATED] ?: false }

    /**
     * Register a new user.
     */
    suspend fun registerUser(name: String, dob: String, phone: String, gmail: String, password: String): Boolean {
        // Simple duplicate check (in a real app, this would be server-side)
        val data = context.authDataStore.data.first()
        if (data[USER_PHONE] == phone || data[USER_GMAIL] == gmail) return false

        context.authDataStore.edit { prefs ->
            prefs[USER_NAME] = name
            prefs[USER_DOB] = dob
            prefs[USER_PHONE] = phone
            prefs[USER_GMAIL] = gmail
            prefs[USER_PASSWORD] = password // In production, use strong hashing
            prefs[IS_AUTHENTICATED] = true
        }
        return true
    }

    /**
     * Verify login credentials.
     */
    suspend fun login(identity: String, password: String): Boolean {
        val data = context.authDataStore.data.first()
        val storedPhone = data[USER_PHONE]
        val storedGmail = data[USER_GMAIL]
        val storedPassword = data[USER_PASSWORD]

        return if ((identity == storedPhone || identity == storedGmail) && password == storedPassword) {
            context.authDataStore.edit { it[IS_AUTHENTICATED] = true }
            true
        } else {
            false
        }
    }

    /**
     * Logout the current user.
     */
    suspend fun logout() {
        context.authDataStore.edit { it[IS_AUTHENTICATED] = false }
    }

    /**
     * Update editable profile fields (name, dob, phone, gmail, profilePicUri).
     */
    suspend fun updateProfile(name: String, dob: String, phone: String, gmail: String, profilePicUri: String) {
        context.authDataStore.edit { prefs ->
            prefs[USER_NAME] = name
            prefs[USER_DOB] = dob
            prefs[USER_PHONE] = phone
            prefs[USER_GMAIL] = gmail
            prefs[USER_PROFILE_PIC_URI] = profilePicUri
        }
    }
    
    /**
     * Update password.
     */
    suspend fun updatePassword(password: String) {
        context.authDataStore.edit { prefs ->
            prefs[USER_PASSWORD] = password
        }
    }
    

    /**
     * Get registered user name.
     */
    val userName: Flow<String?> = context.authDataStore.data.map { it[USER_NAME] }

    /**
     * Get full user profile details.
     */
    val userProfile: Flow<UserProfile> = context.authDataStore.data.map { prefs ->
        UserProfile(
            name = prefs[USER_NAME] ?: "",
            dob = prefs[USER_DOB] ?: "",
            phone = prefs[USER_PHONE] ?: "",
            gmail = prefs[USER_GMAIL] ?: "",
            profilePicUri = prefs[USER_PROFILE_PIC_URI] ?: ""
        )
    }
}

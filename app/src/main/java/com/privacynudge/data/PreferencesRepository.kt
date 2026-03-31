package com.privacynudge.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property for DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "privacy_nudge_prefs")

/**
 * Manages user preferences using DataStore.
 */
class PreferencesRepository(private val context: Context) {

    companion object {
        private val SELECTED_PACKAGE_KEY = stringPreferencesKey("selected_package")
        private val SELECTED_APP_NAME_KEY = stringPreferencesKey("selected_app_name")
        private val BLOCKED_PACKAGES_KEY = stringPreferencesKey("blocked_packages")
    }

    /**
     * Flow of the currently selected package name.
     */
    val selectedPackage: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_PACKAGE_KEY]
    }

    /**
     * Flow of the currently selected app name.
     */
    val selectedAppName: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_APP_NAME_KEY]
    }

    /**
     * Saves the selected app to monitor.
     */
    suspend fun setSelectedApp(packageName: String, appName: String) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_PACKAGE_KEY] = packageName
            prefs[SELECTED_APP_NAME_KEY] = appName
        }
    }

    /**
     * Clears the selected app.
     */
    suspend fun clearSelectedApp() {
        context.dataStore.edit { prefs ->
            prefs.remove(SELECTED_PACKAGE_KEY)
            prefs.remove(SELECTED_APP_NAME_KEY)
        }
    }

    /**
     * Flow of blocked package names (comma-separated string).
     */
    val blockedPackages: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[BLOCKED_PACKAGES_KEY]?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
    }

    /**
     * Toggles the blocked status for a package.
     */
    suspend fun togglePackageBlock(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[BLOCKED_PACKAGES_KEY]?.split(",")?.filter { it.isNotEmpty() }?.toMutableSet() ?: mutableSetOf()
            if (current.contains(packageName)) {
                current.remove(packageName)
            } else {
                current.add(packageName)
            }
            prefs[BLOCKED_PACKAGES_KEY] = current.joinToString(",")
        }
    }

    /**
     * Checks if a package is blocked.
     */
    suspend fun isPackageBlocked(packageName: String): Boolean {
        var isBlocked = false
        context.dataStore.edit { prefs ->
            val current = prefs[BLOCKED_PACKAGES_KEY]?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
            isBlocked = current.contains(packageName)
        }
        return isBlocked
    }
}

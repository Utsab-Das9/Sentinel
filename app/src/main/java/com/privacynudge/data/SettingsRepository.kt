package com.privacynudge.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_prefs")

/**
 * Repository for managing all Sentinel settings using DataStore.
 */
class SettingsRepository(private val context: Context) {

    companion object {
        // General
        private val LIVE_AWARENESS_ENABLED = booleanPreferencesKey("live_awareness_enabled")
        private val VPN_ENABLED = booleanPreferencesKey("vpn_enabled")
        private val NOTIFICATION_SOUND = booleanPreferencesKey("notification_sound")
        private val NOTIFICATION_VIBRATION = booleanPreferencesKey("notification_vibration")
        private val DARK_MODE = booleanPreferencesKey("dark_mode")
        private val AUTO_START = booleanPreferencesKey("auto_start")

        // Notifications
        private val NUDGES_ENABLED = booleanPreferencesKey("nudges_enabled")
        private val SILENT_MODE = booleanPreferencesKey("silent_mode")
        private val HIGH_RISK_ALERTS = booleanPreferencesKey("high_risk_alerts")
        private val ALERT_FREQUENCY = stringPreferencesKey("alert_frequency") // "low", "medium", "high"

        // Security
        private val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        private val APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
        private val APP_LOCK_TYPE = stringPreferencesKey("app_lock_type") // "pin" or "password"
        private val ENCRYPTED_STORAGE = booleanPreferencesKey("encrypted_storage")

        // Privacy
        private val MONITOR_CAMERA = booleanPreferencesKey("monitor_camera")
        private val MONITOR_LOCATION = booleanPreferencesKey("monitor_location")
        private val MONITOR_MICROPHONE = booleanPreferencesKey("monitor_microphone")
        private val MONITOR_CONTACTS = booleanPreferencesKey("monitor_contacts")
        private val MONITOR_STORAGE = booleanPreferencesKey("monitor_storage")

        // Data Management
        private val BLOCKED_PACKAGES = stringPreferencesKey("blocked_packages")
    }

    // ---- General ----

    val liveAwarenessEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[LIVE_AWARENESS_ENABLED] ?: false }
    suspend fun setLiveAwarenessEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[LIVE_AWARENESS_ENABLED] = enabled }
    }

    val vpnEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[VPN_ENABLED] ?: false }
    suspend fun setVpnEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[VPN_ENABLED] = enabled }
    }

    val notificationSound: Flow<Boolean> = context.settingsDataStore.data.map { it[NOTIFICATION_SOUND] ?: true }
    suspend fun setNotificationSound(enabled: Boolean) {
        context.settingsDataStore.edit { it[NOTIFICATION_SOUND] = enabled }
    }

    val notificationVibration: Flow<Boolean> = context.settingsDataStore.data.map { it[NOTIFICATION_VIBRATION] ?: true }
    suspend fun setNotificationVibration(enabled: Boolean) {
        context.settingsDataStore.edit { it[NOTIFICATION_VIBRATION] = enabled }
    }

    val darkMode: Flow<Boolean> = context.settingsDataStore.data.map { it[DARK_MODE] ?: true }
    suspend fun setDarkMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[DARK_MODE] = enabled }
    }

    val autoStart: Flow<Boolean> = context.settingsDataStore.data.map { it[AUTO_START] ?: false }
    suspend fun setAutoStart(enabled: Boolean) {
        context.settingsDataStore.edit { it[AUTO_START] = enabled }
    }

    // ---- Notifications ----

    val nudgesEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[NUDGES_ENABLED] ?: true }
    suspend fun setNudgesEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[NUDGES_ENABLED] = enabled }
    }

    val silentMode: Flow<Boolean> = context.settingsDataStore.data.map { it[SILENT_MODE] ?: false }
    suspend fun setSilentMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[SILENT_MODE] = enabled }
    }

    val highRiskAlerts: Flow<Boolean> = context.settingsDataStore.data.map { it[HIGH_RISK_ALERTS] ?: true }
    suspend fun setHighRiskAlerts(enabled: Boolean) {
        context.settingsDataStore.edit { it[HIGH_RISK_ALERTS] = enabled }
    }

    val alertFrequency: Flow<String> = context.settingsDataStore.data.map { it[ALERT_FREQUENCY] ?: "medium" }
    suspend fun setAlertFrequency(frequency: String) {
        context.settingsDataStore.edit { it[ALERT_FREQUENCY] = frequency }
    }

    // ---- Security ----

    val appLockEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[APP_LOCK_ENABLED] ?: false }
    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[APP_LOCK_ENABLED] = enabled }
    }

    val appLockPin: Flow<String> = context.settingsDataStore.data.map { it[APP_LOCK_PIN] ?: "" }
    suspend fun setAppLockPin(pin: String) {
        context.settingsDataStore.edit { it[APP_LOCK_PIN] = pin }
    }

    val appLockType: Flow<String> = context.settingsDataStore.data.map { it[APP_LOCK_TYPE] ?: "pin" }
    suspend fun setAppLockType(type: String) {
        context.settingsDataStore.edit { it[APP_LOCK_TYPE] = type }
    }

    val encryptedStorage: Flow<Boolean> = context.settingsDataStore.data.map { it[ENCRYPTED_STORAGE] ?: false }
    suspend fun setEncryptedStorage(enabled: Boolean) {
        context.settingsDataStore.edit { it[ENCRYPTED_STORAGE] = enabled }
    }

    // ---- Privacy Monitoring Categories ----

    val monitorCamera: Flow<Boolean> = context.settingsDataStore.data.map { it[MONITOR_CAMERA] ?: true }
    suspend fun setMonitorCamera(enabled: Boolean) {
        context.settingsDataStore.edit { it[MONITOR_CAMERA] = enabled }
    }

    val monitorLocation: Flow<Boolean> = context.settingsDataStore.data.map { it[MONITOR_LOCATION] ?: true }
    suspend fun setMonitorLocation(enabled: Boolean) {
        context.settingsDataStore.edit { it[MONITOR_LOCATION] = enabled }
    }

    val monitorMicrophone: Flow<Boolean> = context.settingsDataStore.data.map { it[MONITOR_MICROPHONE] ?: true }
    suspend fun setMonitorMicrophone(enabled: Boolean) {
        context.settingsDataStore.edit { it[MONITOR_MICROPHONE] = enabled }
    }

    val monitorContacts: Flow<Boolean> = context.settingsDataStore.data.map { it[MONITOR_CONTACTS] ?: true }
    suspend fun setMonitorContacts(enabled: Boolean) {
        context.settingsDataStore.edit { it[MONITOR_CONTACTS] = enabled }
    }

    val monitorStorage: Flow<Boolean> = context.settingsDataStore.data.map { it[MONITOR_STORAGE] ?: true }
    suspend fun setMonitorStorage(enabled: Boolean) {
        context.settingsDataStore.edit { it[MONITOR_STORAGE] = enabled }
    }

    // ---- Data Management ----

    suspend fun clearAllSettings() {
        context.settingsDataStore.edit { it.clear() }
    }

    // ---- Blocked Packages ----

    val blockedPackages: Flow<Set<String>> = context.settingsDataStore.data.map { prefs ->
        prefs[BLOCKED_PACKAGES]?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
    }

    suspend fun togglePackageBlock(packageName: String) {
        context.settingsDataStore.edit { prefs ->
            val current = prefs[BLOCKED_PACKAGES]?.split(",")?.filter { it.isNotEmpty() }?.toMutableSet() ?: mutableSetOf()
            if (current.contains(packageName)) {
                current.remove(packageName)
            } else {
                current.add(packageName)
            }
            prefs[BLOCKED_PACKAGES] = current.joinToString(",")
        }
    }
}

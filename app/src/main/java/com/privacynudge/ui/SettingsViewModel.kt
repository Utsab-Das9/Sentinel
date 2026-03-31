package com.privacynudge.ui

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.privacynudge.data.*
import com.privacynudge.vpn.PrivacyNudgeVpnService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * UI state for the Settings screen.
 */
data class SettingsUiState(
    // General
    val liveAwarenessEnabled: Boolean = false,
    val vpnEnabled: Boolean = false,
    val notificationSound: Boolean = true,
    val notificationVibration: Boolean = true,
    val darkMode: Boolean = true,
    val autoStart: Boolean = false,

    // Notifications
    val nudgesEnabled: Boolean = true,
    val silentMode: Boolean = false,
    val highRiskAlerts: Boolean = true,
    val alertFrequency: String = "medium",

    // Security
    val appLockEnabled: Boolean = false,
    val appLockPin: String = "",
    val appLockType: String = "pin", // "pin" or "password"
    val showAppLockSetupDialog: Boolean = false,
    val encryptedStorage: Boolean = false,

    // Privacy Monitoring Categories
    val monitorCamera: Boolean = true,
    val monitorLocation: Boolean = true,
    val monitorMicrophone: Boolean = true,
    val monitorContacts: Boolean = true,
    val monitorStorage: Boolean = true,

    // Profile
    val userProfile: UserProfile = UserProfile(),
    val editName: String = "",
    val editDob: String = "",
    val editPhone: String = "",
    val editGmail: String = "",
    val editProfilePicUri: String = "",
    val isEditingProfile: Boolean = false,
    val profileSaved: Boolean = false,
    val showPasswordChangeDialog: Boolean = false,
    val blockedPackages: Set<String> = emptySet(),
    val installedApps: List<InstalledApp> = emptyList(),
    val nudgeEvents: List<NudgeEvent> = emptyList()
)

/**
 * ViewModel managing the Settings section state and actions.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val authRepository = AuthRepository(application)
    private val appInsightsRepository = AppInsightsRepository(application)
    private val installedAppsRepository = InstalledAppsRepository(application)
    private val nudgeEventRepository = NudgeEventRepository.getInstance()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        collectSettings()
        fetchInstalledApps()
    }

    private fun fetchInstalledApps() {
        viewModelScope.launch {
            val apps = installedAppsRepository.getInstalledApps()
            _uiState.value = _uiState.value.copy(installedApps = apps)
        }
    }

    private fun collectSettings() {
        viewModelScope.launch {
            combine(
                settingsRepository.liveAwarenessEnabled,
                settingsRepository.vpnEnabled,
                settingsRepository.notificationSound,
                settingsRepository.notificationVibration,
                settingsRepository.darkMode
            ) { liveAwareness, vpn, sound, vibration, darkMode ->
                _uiState.value.copy(
                    liveAwarenessEnabled = liveAwareness,
                    vpnEnabled = vpn,
                    notificationSound = sound,
                    notificationVibration = vibration,
                    darkMode = darkMode
                )
            }.collect { _uiState.value = it }
        }

        viewModelScope.launch {
            combine(
                settingsRepository.autoStart,
                settingsRepository.nudgesEnabled,
                settingsRepository.silentMode,
                settingsRepository.highRiskAlerts,
                settingsRepository.alertFrequency
            ) { autoStart, nudges, silent, highRisk, alertFreq ->
                _uiState.value.copy(
                    autoStart = autoStart,
                    nudgesEnabled = nudges,
                    silentMode = silent,
                    highRiskAlerts = highRisk,
                    alertFrequency = alertFreq
                )
            }.collect { _uiState.value = it }
        }

        viewModelScope.launch {
            combine(
                settingsRepository.appLockEnabled,
                settingsRepository.appLockPin,
                settingsRepository.appLockType,
                settingsRepository.encryptedStorage,
                settingsRepository.monitorCamera
            ) { appLock, appLockPin, appLockType, encrypted, camera ->
                _uiState.value.copy(
                    appLockEnabled = appLock,
                    appLockPin = appLockPin,
                    appLockType = appLockType,
                    encryptedStorage = encrypted,
                    monitorCamera = camera
                )
            }.collect { _uiState.value = it }
        }

        viewModelScope.launch {
            combine(
                settingsRepository.monitorLocation,
                settingsRepository.monitorMicrophone,
                settingsRepository.monitorContacts,
                settingsRepository.monitorStorage
            ) { location, mic, contacts, storage ->
                _uiState.value.copy(
                    monitorLocation = location,
                    monitorMicrophone = mic,
                    monitorContacts = contacts,
                    monitorStorage = storage
                )
            }.collect { _uiState.value = it }
        }

        viewModelScope.launch {
            authRepository.userProfile.collect { profile ->
                _uiState.value = _uiState.value.copy(
                    userProfile = profile,
                    editName = if (!_uiState.value.isEditingProfile) profile.name else _uiState.value.editName,
                    editDob = if (!_uiState.value.isEditingProfile) profile.dob else _uiState.value.editDob,
                    editPhone = if (!_uiState.value.isEditingProfile) profile.phone else _uiState.value.editPhone,
                    editGmail = if (!_uiState.value.isEditingProfile) profile.gmail else _uiState.value.editGmail,
                    editProfilePicUri = if (!_uiState.value.isEditingProfile) profile.profilePicUri else _uiState.value.editProfilePicUri
                )
            }
        }

        viewModelScope.launch {
            nudgeEventRepository.events.collect { events ->
                _uiState.value = _uiState.value.copy(nudgeEvents = events)
            }
        }
    }

    // ---- General Toggles ----

    fun toggleLiveAwareness(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setLiveAwarenessEnabled(enabled)
            val intent = Intent(getApplication(), PermissionAwarenessService::class.java).apply {
                action = if (enabled) PermissionAwarenessService.ACTION_START else PermissionAwarenessService.ACTION_STOP
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && enabled) {
                getApplication<Application>().startForegroundService(intent)
            } else {
                getApplication<Application>().startService(intent)
            }
        }
    }

    fun toggleVpn(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setVpnEnabled(enabled)
            val intent = Intent(getApplication(), PrivacyNudgeVpnService::class.java).apply {
                action = if (enabled) PrivacyNudgeVpnService.ACTION_START else PrivacyNudgeVpnService.ACTION_STOP
            }
            getApplication<Application>().startService(intent)
        }
    }

    fun toggleNotificationSound(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setNotificationSound(enabled)
    }

    fun toggleNotificationVibration(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setNotificationVibration(enabled)
    }

    fun toggleDarkMode(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setDarkMode(enabled)
    }

    fun toggleAutoStart(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setAutoStart(enabled)
    }

    // ---- Notification Toggles ----

    fun toggleNudges(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setNudgesEnabled(enabled)
    }

    fun toggleSilentMode(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setSilentMode(enabled)
    }

    fun toggleHighRiskAlerts(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setHighRiskAlerts(enabled)
    }

    fun setAlertFrequency(frequency: String) = viewModelScope.launch {
        settingsRepository.setAlertFrequency(frequency)
    }

    // ---- Security Toggles ----

    fun toggleAppLock(enabled: Boolean) = viewModelScope.launch {
        if (enabled && _uiState.value.appLockPin.isEmpty()) {
            // Show setup dialog if no PIN/password is set
            _uiState.value = _uiState.value.copy(showAppLockSetupDialog = true)
        } else {
            settingsRepository.setAppLockEnabled(enabled)
        }
    }

    fun setShowAppLockSetupDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAppLockSetupDialog = show)
    }

    fun setupAppLock(pin: String, type: String) = viewModelScope.launch {
        settingsRepository.setAppLockPin(pin)
        settingsRepository.setAppLockType(type)
        settingsRepository.setAppLockEnabled(true)
        _uiState.value = _uiState.value.copy(showAppLockSetupDialog = false)
    }

    fun changeAppLockPin(newPin: String) = viewModelScope.launch {
        settingsRepository.setAppLockPin(newPin)
    }

    fun changeAppLockType(type: String) = viewModelScope.launch {
        settingsRepository.setAppLockType(type)
    }

    fun toggleEncryptedStorage(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setEncryptedStorage(enabled)
    }

    // ---- Privacy Monitoring ----

    fun toggleMonitorCamera(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setMonitorCamera(enabled)
    }

    fun toggleMonitorLocation(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setMonitorLocation(enabled)
    }

    fun toggleMonitorMicrophone(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setMonitorMicrophone(enabled)
    }

    fun toggleMonitorContacts(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setMonitorContacts(enabled)
    }

    fun toggleMonitorStorage(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setMonitorStorage(enabled)
    }

    // ---- Profile ----

    fun startEditingProfile() {
        _uiState.value = _uiState.value.copy(
            isEditingProfile = true,
            editName = _uiState.value.userProfile.name,
            editDob = _uiState.value.userProfile.dob,
            editPhone = _uiState.value.userProfile.phone,
            editGmail = _uiState.value.userProfile.gmail,
            editProfilePicUri = _uiState.value.userProfile.profilePicUri,
            profileSaved = false
        )
    }

    fun onEditNameChange(name: String) {
        _uiState.value = _uiState.value.copy(editName = name)
    }

    fun onEditDobChange(dob: String) {
        _uiState.value = _uiState.value.copy(editDob = dob)
    }

    fun onEditPhoneChange(phone: String) {
        _uiState.value = _uiState.value.copy(editPhone = phone)
    }

    fun onEditGmailChange(gmail: String) {
        _uiState.value = _uiState.value.copy(editGmail = gmail)
    }

    fun onEditProfilePicUriChange(uri: String) {
        _uiState.value = _uiState.value.copy(editProfilePicUri = uri)
    }

    fun saveProfile() = viewModelScope.launch {
        authRepository.updateProfile(
            name = _uiState.value.editName,
            dob = _uiState.value.editDob,
            phone = _uiState.value.editPhone,
            gmail = _uiState.value.editGmail,
            profilePicUri = _uiState.value.editProfilePicUri
        )
        _uiState.value = _uiState.value.copy(
            isEditingProfile = false,
            profileSaved = true
        )
    }

    fun cancelEditProfile() {
        _uiState.value = _uiState.value.copy(
            isEditingProfile = false,
            editName = _uiState.value.userProfile.name,
            editDob = _uiState.value.userProfile.dob,
            editPhone = _uiState.value.userProfile.phone,
            editGmail = _uiState.value.userProfile.gmail,
            editProfilePicUri = _uiState.value.userProfile.profilePicUri
        )
    }

    fun updatePassword(newPassword: String) = viewModelScope.launch {
        authRepository.updatePassword(newPassword)
        _uiState.value = _uiState.value.copy(showPasswordChangeDialog = false)
    }

    fun setShowPasswordChangeDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showPasswordChangeDialog = show)
    }

    // ---- Data Management ----

    fun clearCache() = viewModelScope.launch {
        val cacheDir = getApplication<Application>().cacheDir
        cacheDir.deleteRecursively()
    }

    /**
     * Resets privacy analysis data and logs.
     */
    fun clearLogs() {
        viewModelScope.launch {
            settingsRepository.clearAllSettings()
            appInsightsRepository.clearCache()
            nudgeEventRepository.clearEvents()
        }
    }

    // ---- Auth ----

    fun logout(onLoggedOut: () -> Unit) = viewModelScope.launch {
        authRepository.logout()
        onLoggedOut()
    }

    // ---- App Blocking ----

    fun togglePackageBlock(packageName: String) = viewModelScope.launch {
        settingsRepository.togglePackageBlock(packageName)
    }
}

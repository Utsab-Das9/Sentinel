package com.privacynudge.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.privacynudge.data.*
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * UI state for the home screen.
 */
data class HomeUiState(
    val vpnState: VpnState = VpnState.STOPPED,
    val installedApps: List<InstalledApp> = emptyList(),
    val selectedApp: InstalledApp? = null,
    val selectedAppPermissionStatus: LocationPermissionStatus = LocationPermissionStatus.UNKNOWN,
    val nudgeEvents: List<NudgeEvent> = emptyList(),
    val isLoadingApps: Boolean = true,
    val showAppDropdown: Boolean = false
)

/**
 * ViewModel for the home screen.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val installedAppsRepository = InstalledAppsRepository(application)
    private val permissionInspector = PermissionInspector(application)
    private val preferencesRepository = PreferencesRepository(application)
    private val nudgeEventRepository = NudgeEventRepository.getInstance()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadInstalledApps()
        observeSelectedApp()
        observeNudgeEvents()
    }

    private fun observeNudgeEvents() {
        viewModelScope.launch {
            nudgeEventRepository.events.collect { events ->
                _uiState.value = _uiState.value.copy(nudgeEvents = events)
            }
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoadingApps = true)
                val apps = installedAppsRepository.getInstalledApps()
                _uiState.value = _uiState.value.copy(
                    installedApps = apps,
                    isLoadingApps = false
                )

                // Restore previously selected app
                val savedPackage = preferencesRepository.selectedPackage.first()
                if (savedPackage != null) {
                    val savedApp = apps.find { it.packageName == savedPackage }
                    if (savedApp != null) {
                        selectApp(savedApp, persist = false)
                    }
                }
            } catch (e: Exception) {
                // Prevent crash from propagating to Activity on init
                Log.e("HomeViewModel", "Failed to load installed apps", e)
                _uiState.value = _uiState.value.copy(isLoadingApps = false)
            }
        }
    }

    private fun observeSelectedApp() {
        viewModelScope.launch {
            preferencesRepository.selectedPackage.collect { packageName ->
                if (packageName != null) {
                    val app = _uiState.value.installedApps.find { it.packageName == packageName }
                    if (app != null && _uiState.value.selectedApp?.packageName != packageName) {
                        val status = permissionInspector.checkLocationPermission(packageName)
                        _uiState.value = _uiState.value.copy(
                            selectedApp = app,
                            selectedAppPermissionStatus = status
                        )
                    }
                }
            }
        }
    }

    fun selectApp(app: InstalledApp, persist: Boolean = true) {
        viewModelScope.launch {
            val status = permissionInspector.checkLocationPermission(app.packageName)
            _uiState.value = _uiState.value.copy(
                selectedApp = app,
                selectedAppPermissionStatus = status,
                showAppDropdown = false
            )
            if (persist) {
                preferencesRepository.setSelectedApp(app.packageName, app.name)
            }
        }
    }

    fun toggleAppDropdown() {
        _uiState.value = _uiState.value.copy(
            showAppDropdown = !_uiState.value.showAppDropdown
        )
    }

    fun dismissAppDropdown() {
        _uiState.value = _uiState.value.copy(showAppDropdown = false)
    }

    fun toggleVpn() {
        val currentState = _uiState.value.vpnState
        when (currentState) {
            VpnState.STOPPED -> {
                // Will be handled by MainActivity to start VPN service
                _uiState.value = _uiState.value.copy(vpnState = VpnState.STARTING)
            }
            VpnState.RUNNING -> {
                _uiState.value = _uiState.value.copy(vpnState = VpnState.STOPPING)
            }
            else -> { /* Ignore during transitions */ }
        }
    }

    fun setVpnState(state: VpnState) {
        _uiState.value = _uiState.value.copy(vpnState = state)
    }

    fun addNudgeEvent(event: NudgeEvent) {
        nudgeEventRepository.addEvent(event)
    }

    fun refreshEvents() {
        // No-op as we use Flow observation
    }
}

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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * Tabs for permission categories in the detail sheet.
 */
enum class PermissionTab {
    DANGEROUS,
    NORMAL
}

/**
 * UI state for the User Apps screen.
 */
data class AllAppsUiState(
    val apps: List<InstalledApp> = emptyList(),
    val filteredApps: List<InstalledApp> = emptyList(),
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.NAME,
    val showOnlyDangerous: Boolean = false,
    val showOnlySafe: Boolean = false,
    val selectedApp: InstalledApp? = null,
    val selectedAppPermissions: List<DetailedPermission> = emptyList(),
    val showAppDetail: Boolean = false,
    val selectedAppTab: PermissionTab = PermissionTab.DANGEROUS,
    val isLoading: Boolean = true
)

/**
 * ViewModel for the User Apps screen.
 * Manages user app list, search, sort, filter, and permission inspection.
 */
class AllAppsViewModel(application: Application) : AndroidViewModel(application) {

    private val installedAppsRepository = InstalledAppsRepository(application)
    private val permissionInspector = PermissionInspector(application)
    private val packageChangeManager = PackageChangeManager(application)
    private var filterJob: kotlinx.coroutines.Job? = null

    private val _uiState = MutableStateFlow(AllAppsUiState())
    val uiState: StateFlow<AllAppsUiState> = _uiState.asStateFlow()

    init {
        loadApps()
        startPackageChangeListener()
    }

    /**
     * Load all installed apps.
     */
    fun loadApps() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                // Get blocked packages from settings
                val settingsRepo = com.privacynudge.data.SettingsRepository(getApplication())
                val blockedPackages = settingsRepo.blockedPackages.first()

                // Get all user apps
                val userApps = installedAppsRepository.getAllInstalledApps(includeSystem = false)

                // Get blocked apps that might be system apps
                val blockedApps = if (blockedPackages.isNotEmpty()) {
                    blockedPackages.mapNotNull { packageName ->
                        try {
                            // Check if this blocked app is already in userApps
                            if (userApps.any { it.packageName == packageName }) {
                                null // Already included
                            } else {
                                // This is a system app that's blocked, fetch its details
                                installedAppsRepository.getAppDetails(packageName)
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                } else {
                    emptyList()
                }

                // Combine user apps with blocked system apps
                val allApps = (userApps + blockedApps).distinctBy { it.packageName }

                _uiState.value = _uiState.value.copy(
                    apps = allApps,
                    isLoading = false
                )

                applyFilters()
            } catch (e: Exception) {
                // Prevent crash from propagating to Activity on init
                Log.e("AllAppsViewModel", "Failed to load apps", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    /**
     * Start listening for package changes.
     */
    private fun startPackageChangeListener() {
        packageChangeManager.startListening(
            onAdded = { packageName ->
                viewModelScope.launch {
                    val newApp = installedAppsRepository.getAppDetails(packageName)
                    if (newApp != null && !newApp.isSystemApp) {
                        val currentApps = _uiState.value.apps.toMutableList()
                        currentApps.add(0, newApp)
                        updateAppList(currentApps)
                    }
                }
            },
            onRemoved = { packageName ->
                viewModelScope.launch {
                    val currentApps = _uiState.value.apps.filter { it.packageName != packageName }
                    updateAppList(currentApps)
                }
            },
            onUpdated = { packageName ->
                viewModelScope.launch {
                    val updatedApp = installedAppsRepository.getAppDetails(packageName)
                    if (updatedApp != null && !updatedApp.isSystemApp) {
                        val currentApps = _uiState.value.apps.map { app ->
                            if (app.packageName == packageName) updatedApp else app
                        }
                        updateAppList(currentApps)
                    }
                }
            }
        )
    }

    private fun updateAppList(apps: List<InstalledApp>) {
        _uiState.value = _uiState.value.copy(
            apps = apps
        )
        applyFilters()
    }

    /**
     * Update search query.
     */
    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    /**
     * Update sort option.
     */
    fun setSortOption(option: SortOption) {
        _uiState.value = _uiState.value.copy(sortOption = option)
        applyFilters()
    }

    /**
     * Toggle dangerous permissions only filter.
     * Disables Safe filter when enabled for mutual exclusivity.
     */
    fun toggleDangerousOnly() {
        val newValue = !_uiState.value.showOnlyDangerous
        _uiState.value = _uiState.value.copy(
            showOnlyDangerous = newValue,
            showOnlySafe = if (newValue) false else _uiState.value.showOnlySafe
        )
        applyFilters()
    }

    /**
     * Toggle safe apps only filter (PRS < 0.5).
     * Disables Dangerous filter when enabled for mutual exclusivity.
     */
    fun toggleSafeOnly() {
        val newValue = !_uiState.value.showOnlySafe
        _uiState.value = _uiState.value.copy(
            showOnlySafe = newValue,
            showOnlyDangerous = if (newValue) false else _uiState.value.showOnlyDangerous
        )
        applyFilters()
    }

    /**
     * Determine if an app qualifies as "Safe" (PRS < 0.5).
     */
    fun isSafeApp(app: InstalledApp): Boolean {
        return app.prsRiskLevel == RiskLevel.LOW
    }




    /**
     * Apply all filters and sorting.
     */
    private fun applyFilters() {
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            // Debounce search to improve performance with large lists
            if (_uiState.value.searchQuery.isNotBlank()) {
                kotlinx.coroutines.delay(300)
            }
            
            val state = _uiState.value
            
            try {
                val filtered = withContext(Dispatchers.Default) {
                    var list = state.apps

                    // Apply search filter
                    if (state.searchQuery.isNotBlank()) {
                        val query = state.searchQuery.lowercase().trim()
                        list = list.filter { app ->
                            app.name.lowercase().contains(query) ||
                            app.packageName.lowercase().contains(query)
                        }
                    }

                    // Apply dangerous apps filter (High/Medium)
                    if (state.showOnlyDangerous) {
                        list = list.filter { it.prsRiskLevel != RiskLevel.LOW }
                    }

                    // Apply safe apps filter (Low)
                    if (state.showOnlySafe) {
                        list = list.filter { it.prsRiskLevel == RiskLevel.LOW }
                    }




                    // Apply stable sorting
                    when (state.sortOption) {
                        SortOption.NAME -> list.sortedWith(
                            compareBy<InstalledApp> { it.name.lowercase() }
                                .thenBy { it.packageName }
                        )
                        SortOption.INSTALL_DATE -> list.sortedWith(
                            compareByDescending<InstalledApp> { it.installTime }
                                .thenBy { it.name.lowercase() }
                        )
                        SortOption.UPDATE_DATE -> list.sortedWith(
                            compareByDescending<InstalledApp> { it.updateTime }
                                .thenBy { it.name.lowercase() }
                        )
                        SortOption.PRS_SCORE -> list.sortedWith(
                            compareByDescending<InstalledApp> { it.prsScore }
                                .thenBy { it.name.lowercase() }
                        )
                    }
                }

                _uiState.value = state.copy(filteredApps = filtered)
            } catch (e: Exception) {
                // Fallback to current apps if sorting fails
                _uiState.value = state.copy(filteredApps = state.apps)
            }
        }
    }

    /**
     * Select an app to view details.
     */
    fun selectApp(app: InstalledApp) {
        val permissions = permissionInspector.getAllPermissions(app.packageName)
        _uiState.value = _uiState.value.copy(
            selectedApp = app,
            selectedAppPermissions = permissions,
            showAppDetail = true
        )
    }

    /**
     * Set the selected permission tab.
     */
    fun setPermissionTab(tab: PermissionTab) {
        _uiState.value = _uiState.value.copy(selectedAppTab = tab)
    }

    /**
     * Close app detail view.
     */
    fun closeAppDetail() {
        _uiState.value = _uiState.value.copy(
            selectedApp = null,
            selectedAppPermissions = emptyList(),
            showAppDetail = false
        )
    }

    /**
     * Get sensitive permission count for an app.
     */

    /**
     * Get risk level for an app.
     */
    fun getRiskLevel(packageName: String): RiskLevel {
        val profile = permissionInspector.getFullPermissionProfile(packageName)
        return profile.riskLevel
    }

    /**
     * Get formatted install date.
     */
    fun getInstallDateString(app: InstalledApp): String {
        return installedAppsRepository.getInstallDateString(app.installTime)
    }

    /**
     * Get formatted update date.
     */
    fun getUpdateDateString(app: InstalledApp): String {
        return installedAppsRepository.getUpdateDateString(app.updateTime)
    }

    /**
     * Get permission icon summary for an app.
     */
    fun getPermissionIconSummary(packageName: String): String {
        return permissionInspector.getPermissionIconSummary(packageName)
    }

    /**
     * Refresh the app list.
     */
    fun refresh() {
        loadApps()
    }

    override fun onCleared() {
        super.onCleared()
        packageChangeManager.stopListening()
    }
}

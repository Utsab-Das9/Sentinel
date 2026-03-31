package com.privacynudge.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.privacynudge.data.*
import com.privacynudge.notification.NotificationHelper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * UI state for the dashboard screen.
 */
data class DashboardUiState(
    val highRiskApps: List<AppPermissionProfile> = emptyList(),
    val mediumRiskApps: List<AppPermissionProfile> = emptyList(),
    val lowRiskApps: List<AppPermissionProfile> = emptyList(),
    val selectedApp: AppPermissionProfile? = null,
    val isLoadingCategorized: Boolean = true,
    val showAppDetail: Boolean = false
)

/**
 * ViewModel for the dashboard screen.
 * Manages state for monitoring Facebook, LinkedIn, and WhatsApp.
 */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val permissionInspector = PermissionInspector(application)
    private val installedAppsRepository = InstalledAppsRepository(application)
    private val usageTracker = AppUsageTracker(application)
    private val settingsRepository = SettingsRepository(application)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    companion object {
        /**
         * Apps to exclude from dashboard risk sections.
         * These apps will be filtered out and replaced with other relevant apps.
         */
        private val EXCLUDED_APPS = listOf(
            "com.productwarehouse", // PW app
            "com.pw",
            "ai.x.grok",            // Grok app
            "com.x.grok",
            "com.grok"
        )
        
        /**
         * List of globally popular apps to prioritize in the dashboard.
         */
        private val POPULAR_APPS = listOf(
            "com.whatsapp",
            "com.facebook.katana",
            "com.instagram.android",
            "com.google.android.youtube",
            "com.android.vending",
            "com.twitter.android",
            "com.snapchat.android",
            "com.linkedin.android",
            "com.spotify.music",
            "com.netflix.mediaclient",
            "com.google.android.gm",
            "com.google.android.apps.messaging",
            "com.android.chrome",
            "com.google.android.apps.maps",
            "com.google.android.apps.photos",
            "com.amazon.mShop.android.shopping",
            "com.ebay.mobile",
            "com.microsoft.teams",
            "com.skype.raider",
            "com.zhiliaoapp.musically", // TikTok
            "com.viber.voip",
            "com.telegram.messenger",
            "com.truecaller",
            "com.ubercab",
            "com.grabtaxi.passenger",
            "com.zomato.android",
            "com.swiggy.android"
        )
    }

    private fun getPopularityScore(app: InstalledApp): Long {
        // Priority 1: Existence in POPULAR_APPS list (0 is most popular)
        val listIndex = POPULAR_APPS.indexOf(app.packageName)
        if (listIndex != -1) return listIndex.toLong()

        // Priority 2: Usage time (if available, reduce score to make it more popular)
        val usageTime = usageTracker.getForegroundTimeToday(app.packageName)
        if (usageTime > 0) {
            // Map usage time to a negative offset (max 1 hour = 3.6M ms offset)
            return POPULAR_APPS.size + 100_000L - (usageTime / 1000)
        }

        // Priority 3: Natural sort/Install time as fallback
        return POPULAR_APPS.size + 200_000L - (app.installTime / 86400000) // Rough day-based offset
    }

    init {
        viewModelScope.launch {
            settingsRepository.blockedPackages.collect {
                loadCategorizedApps()
            }
        }
    }

    /**
     * Load and categorize user apps by risk.
     */
    fun loadCategorizedApps() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoadingCategorized = true)
                
                val blockedPackages = settingsRepository.blockedPackages.first()
                val allApps = installedAppsRepository.getAllInstalledApps(includeSystem = true)
                
                // Filter and categorize apps
                val dashboardApps = allApps.filter { app ->
                    val isBlocked = blockedPackages.contains(app.packageName)
                    val isUserOrPriority = !app.isSystemApp || 
                                          InstalledAppsRepository.isPrioritySystemApp(app.packageName)
                    
                    val isExcluded = EXCLUDED_APPS.any { excluded -> 
                        app.packageName.contains(excluded, ignoreCase = true) ||
                        app.name.contains("PW", ignoreCase = true) ||
                        app.name.contains("Grok", ignoreCase = true)
                    }
                    
                    // Include if blocked OR (it's a user app and not excluded)
                    isBlocked || (isUserOrPriority && !isExcluded)
                }

                val sortedApps = dashboardApps.sortedBy { getPopularityScore(it) }
                
                val highRisk = sortedApps.filter { it.prsRiskLevel == RiskLevel.HIGH }
                    .mapNotNull { app -> 
                        try { permissionInspector.getFullPermissionProfile(app.packageName) } catch (e: Exception) { null }
                    }
                    .sortedBy { it.appName.lowercase() }
                    
                val mediumRisk = sortedApps.filter { it.prsRiskLevel == RiskLevel.MEDIUM }
                    .mapNotNull { app -> 
                        try { permissionInspector.getFullPermissionProfile(app.packageName) } catch (e: Exception) { null }
                    }
                    .sortedBy { it.appName.lowercase() }
                    
                val lowRisk = sortedApps.filter { it.prsRiskLevel == RiskLevel.LOW }
                    .mapNotNull { app -> 
                        try { permissionInspector.getFullPermissionProfile(app.packageName) } catch (e: Exception) { null }
                    }
                    .sortedBy { it.appName.lowercase() }
                
                _uiState.value = _uiState.value.copy(
                    highRiskApps = highRisk,
                    mediumRiskApps = mediumRisk,
                    lowRiskApps = lowRisk,
                    isLoadingCategorized = false
                )
            } catch (e: Exception) {
                // Prevent ViewModel crash from propagating to Activity
                Log.e("DashboardViewModel", "Failed to load categorized apps", e)
                _uiState.value = _uiState.value.copy(isLoadingCategorized = false)
            }
        }
    }

    /**
     * Select an app to view details.
     */
    fun selectApp(packageName: String) {
        val state = _uiState.value
        val app = state.highRiskApps.find { it.packageName == packageName }
            ?: state.mediumRiskApps.find { it.packageName == packageName }
            ?: state.lowRiskApps.find { it.packageName == packageName }
            
        _uiState.value = state.copy(
            selectedApp = app,
            showAppDetail = app != null
        )
    }

    /**
     * Close the app detail view.
     */
    fun closeAppDetail() {
        _uiState.value = _uiState.value.copy(
            selectedApp = null,
            showAppDetail = false
        )
    }

    /**
     * Refresh all data.
     */
    fun refresh() {
        loadCategorizedApps()
    }
}

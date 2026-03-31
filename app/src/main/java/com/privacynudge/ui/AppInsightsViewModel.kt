package com.privacynudge.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.privacynudge.data.AppInsightsRepository
import com.privacynudge.data.InstalledApp
import com.privacynudge.vpn.PrivacyNudgeVpnService
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.privacynudge.data.RiskLevel
import com.privacynudge.data.SortOption


data class AppInsightsSummaryStats(
    val totalApps: Int = 0,
    val hasHighRisk: Boolean = false,
    val highRiskCount: Int = 0,
    val blockedCount: Int = 0,
    val avgPrsScore: Float = 0f
)

/**
 * UI state for the App Insights screen.
 */
data class AppInsightsUiState(
    val apps: List<InstalledApp> = emptyList(),
    val isLoading: Boolean = false,
    val currentSort: SortOption = SortOption.NAME,
    val summaryStats: AppInsightsSummaryStats = AppInsightsSummaryStats(),
    val isVpnActive: Boolean = false,
    val showGraph: Boolean = false,
    val graphPermissions: List<com.privacynudge.data.DetailedPermission> = emptyList(),
    val graphApp: InstalledApp? = null
)

/**
 * ViewModel for managing the App Insights screen state and data.
 */
class AppInsightsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = AppInsightsRepository(application)
    private val preferencesRepository = com.privacynudge.data.PreferencesRepository(application)
    
    private val _uiState = MutableStateFlow(AppInsightsUiState())
    val uiState: StateFlow<AppInsightsUiState> = _uiState.asStateFlow()

    init {
        refresh()
        // Listen for VPN state changes
        PrivacyNudgeVpnService.onStateChanged = { active: Boolean ->
            _uiState.value = _uiState.value.copy(isVpnActive = active)
        }
    }

    /**
     * Refresh the list of apps with insights.
     */
    fun refresh() {
        viewModelScope.launch {
            try {
                // Only show loader if we have no data (stale-while-revalidate)
                if (_uiState.value.apps.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
                val appsWithInsights = repository.getAppsWithInsights()
                
                // Calculate Summary Stats
                val total = appsWithInsights.size
                val highRiskCount = appsWithInsights.count { it.prsRiskLevel == RiskLevel.HIGH }
                val blockedCount = appsWithInsights.count { it.insights?.isBlocked == true }
                val hasHighRisk = highRiskCount > 0
                val avgPrs = if (total > 0) appsWithInsights.map { it.prsScore }.average().toFloat() else 0f
                
                _uiState.value = _uiState.value.copy(
                    apps = appsWithInsights,
                    summaryStats = AppInsightsSummaryStats(
                        totalApps = total,
                        hasHighRisk = hasHighRisk,
                        highRiskCount = highRiskCount,
                        blockedCount = blockedCount,
                        avgPrsScore = avgPrs
                    ),
                    isLoading = false
                )
            } catch (e: Exception) {
                // Prevent crash from propagating to Activity on init
                Log.e("AppInsightsViewModel", "Failed to load app insights", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    /**
     * Toggles the blocked status for an app with a partial UI update.
     */
    fun toggleBlockApp(packageName: String) {
        viewModelScope.launch {
            preferencesRepository.togglePackageBlock(packageName)
            
            // 1. Update the app's insight specifically (force refresh bypasses cache)
            val updatedInsight = repository.getAppInsight(packageName, forceRefresh = true)
            
            // 2. Update only the targeted app in the UI state list
            val updatedApps = _uiState.value.apps.map { app ->
                if (app.packageName == packageName) {
                    app.copy(insights = updatedInsight)
                } else {
                    app
                }
            }
            
            _uiState.value = _uiState.value.copy(apps = updatedApps)
        }
    }


    /**
     * Update sort option.
     */
    fun onSortChanged(sort: SortOption) {
        _uiState.value = _uiState.value.copy(currentSort = sort)
    }


    /**
     * Shows the permission graph for a specific app.
     */
    fun showPermissionGraph(app: InstalledApp) {
        viewModelScope.launch {
            val permissions = com.privacynudge.data.PermissionInspector(getApplication())
                .getAllPermissions(app.packageName)
            _uiState.value = _uiState.value.copy(
                showGraph = true,
                graphPermissions = permissions,
                graphApp = app
            )
        }
    }

    /**
     * Closes the permission graph.
     */
    fun closePermissionGraph() {
        _uiState.value = _uiState.value.copy(
            showGraph = false,
            graphPermissions = emptyList(),
            graphApp = null
        )
    }

    /**
     * Get filtered and sorted list of apps.
     */
    fun getSortedApps(): List<InstalledApp> {
        return uiState.value.apps.sortedBy { it.name.lowercase() }
    }
}

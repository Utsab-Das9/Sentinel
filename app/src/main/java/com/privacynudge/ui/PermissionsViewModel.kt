package com.privacynudge.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.privacynudge.data.*
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Data representing a permission aggregated across multiple apps.
 */
data class AggregatedPermission(
    val permissionName: String,
    val friendlyName: String,
    val shortName: String,
    val icon: String,
    val category: PermissionCategory,
    val description: String,
    val apps: List<InstalledApp>
)

/**
 * UI state for the permissions screen.
 */
data class PermissionsUiState(
    val groupedPermissions: Map<PermissionCategory, List<AggregatedPermission>> = emptyMap(),
    val isLoading: Boolean = true,
    val selectedPermission: AggregatedPermission? = null
)

class PermissionsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = InstalledAppsRepository(application)
    private val permissionInspector = PermissionInspector(application)

    private val _uiState = MutableStateFlow(PermissionsUiState())
    val uiState: StateFlow<PermissionsUiState> = _uiState.asStateFlow()

    init {
        loadPermissions()
    }

    fun loadPermissions() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val aggregated = withContext(Dispatchers.IO) {
                    // EXCLUSIVELY fetch user-installed apps
                    val userApps = repository.getAppsByType(AppType.USER)
                    
                    val permissionMap = mutableMapOf<String, MutableList<InstalledApp>>()
                    val metadataMap = mutableMapOf<String, DetailedPermission>()

                    // 1. Dynamically discover permissions from user apps
                    for (app in userApps) {
                        val appPermissions = permissionInspector.getAllPermissions(app.packageName)
                        for (perm in appPermissions) {
                            permissionMap.getOrPut(perm.name) { mutableListOf() }.add(app)
                            if (!metadataMap.containsKey(perm.name)) {
                                metadataMap[perm.name] = perm
                            }
                        }
                    }

                    // 2. Aggregate and sort
                    permissionMap.map { (name, apps) ->
                        val metadata = metadataMap[name]!!
                        AggregatedPermission(
                            permissionName = name,
                            friendlyName = metadata.label,
                            shortName = metadata.shortName,
                            icon = DangerousPermissions.getIcon(name),
                            category = metadata.category,
                            description = metadata.description,
                            apps = apps.sortedBy { it.name.lowercase() }
                        )
                    }
                    .groupBy { it.category }
                    .mapValues { (_, perms) -> 
                        // Sort permissions alphabetically by friendly name within each category
                        perms.sortedWith(
                            compareBy<AggregatedPermission> { it.friendlyName.lowercase() }
                                .thenBy { it.permissionName }
                        )
                    }
                    // Sort categories alphabetically by display name (Dangerous, Normal, Signature, Special)
                    .toSortedMap(compareBy { it.displayName })
                }

                _uiState.value = _uiState.value.copy(
                    groupedPermissions = aggregated,
                    isLoading = false
                )
            } catch (e: Exception) {
                // Prevent ViewModel crash from propagating to Activity
                Log.e("PermissionsViewModel", "Failed to load permissions", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectPermission(permission: AggregatedPermission?) {
        _uiState.value = _uiState.value.copy(selectedPermission = permission)
    }
}

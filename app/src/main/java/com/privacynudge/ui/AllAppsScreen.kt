package com.privacynudge.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privacynudge.data.*
import com.privacynudge.ui.components.*

/**
 * Main screen for browsing all installed apps.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AllAppsScreen(
    allAppsViewModel: AllAppsViewModel = viewModel<AllAppsViewModel>(),
    modifier: Modifier = Modifier
) {
    val uiState by allAppsViewModel.uiState.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Applications",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Monitor all installed apps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = { allAppsViewModel.refresh() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Loading state
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading apps...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Search bar
                    AppSearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = { allAppsViewModel.setSearchQuery(it) }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    // ── Primary Action Filters: Balanced 50/50 Split ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DangerousOnlyToggle(
                            isEnabled = uiState.showOnlyDangerous,
                            onToggle = { allAppsViewModel.toggleDangerousOnly() },
                            modifier = Modifier.weight(1f)
                        )
                        ReliableOnlyToggle(
                            isEnabled = uiState.showOnlySafe,
                            onToggle = { allAppsViewModel.toggleSafeOnly() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // ── Results & Sorting Hierarchy ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Results count
                        Text(
                            text = "${uiState.filteredApps.size} apps found",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Sort dropdown
                        SortDropdown(
                            currentOption = uiState.sortOption,
                            onOptionSelected = { allAppsViewModel.setSortOption(it) }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // App list
                    if (uiState.filteredApps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No user apps match your filters",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(
                                items = uiState.filteredApps,
                                key = { it.packageName }
                            ) { app ->
                                AllAppsListItem(
                                    app = app,
                                    permissionIcons = allAppsViewModel.getPermissionIconSummary(app.packageName),
                                    updateDateString = allAppsViewModel.getUpdateDateString(app),
                                    installDateString = allAppsViewModel.getInstallDateString(app),
                                    isSafe = allAppsViewModel.isSafeApp(app),
                                    onClick = { allAppsViewModel.selectApp(app) }
                                )

                            }
                        }
                    }
                }
            }
        }
    }

    // App detail bottom sheet
    if (uiState.showAppDetail && uiState.selectedApp != null) {
        AllAppsDetailSheet(
            app = uiState.selectedApp!!,
            packageName = uiState.selectedApp!!.packageName,
            permissions = uiState.selectedAppPermissions,
            installDate = allAppsViewModel.getInstallDateString(uiState.selectedApp!!),
            updateDate = allAppsViewModel.getUpdateDateString(uiState.selectedApp!!),
            riskLevel = allAppsViewModel.getRiskLevel(uiState.selectedApp!!.packageName),
            onDismiss = { allAppsViewModel.closeAppDetail() }
        )
    }
}

/**
 * Bottom sheet showing detailed app information.
 *
 * Permissions are displayed as a unified, flat list — no Dangerous/Normal
 * tab split — keeping the UI minimal and approachable for all users.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllAppsDetailSheet(
    app: InstalledApp,
    packageName: String,
    permissions: List<DetailedPermission>,
    installDate: String,
    updateDate: String,
    riskLevel: RiskLevel,
    onDismiss: () -> Unit
) {
    // Holds the permission whose toggle was tapped; triggers the warning dialog.
    var pendingTogglePermission by remember { mutableStateOf<DetailedPermission?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Privacy Risk Score: ${"%.2f".format(app.prsScore)}/1.0",
                        style = MaterialTheme.typography.labelMedium,
                        color = when (app.prsRiskLevel) {
                            RiskLevel.HIGH -> Color(0xFFF44336)
                            RiskLevel.MEDIUM -> Color(0xFFFF9800)
                            RiskLevel.LOW -> Color(0xFF4CAF50)
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    val reliableCount = permissions.count { it.isGranted && it.category != PermissionCategory.DANGEROUS }
                    val unreliableCount = permissions.count { it.isGranted && it.category == PermissionCategory.DANGEROUS }

                    InfoRow(label = "Type", value = if (app.isSystemApp) "System App" else "User App")
                    InfoRow(label = "Installed", value = installDate)
                    InfoRow(label = "Updated", value = updateDate)
                    InfoRow(label = "Version", value = "${app.versionName} (${app.versionCode})")
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                    
                    InfoRow(
                        label = "Reliable Permissions", 
                        value = reliableCount.toString(),
                        valueColor = Color(0xFF4CAF50)
                    )
                    InfoRow(
                        label = "Unreliable Permissions", 
                        value = unreliableCount.toString(),
                        valueColor = Color(0xFFF44336)
                    )
                    
                    // Restore Exposure, Usage, and Traffic
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    InfoRow(label = "Exposure (SL)", value = String.format("%.2f", app.sensitivityScore))
                    InfoRow(label = "Usage (UI)", value = String.format("%.2f", app.usageIntensityScore))
                    InfoRow(label = "Traffic (NB)", value = String.format("%.2f", app.networkBehaviourScore))
                }

            }

            Spacer(modifier = Modifier.height(16.dp))


            Spacer(modifier = Modifier.height(16.dp))
            // Permissions are grouped into Unreliable and Reliable sections with clear headers.
            // Within each section, they are sorted: Prerequisite first, then Optional.
            
            val dangerousPermissions = permissions.filter { it.category == PermissionCategory.DANGEROUS }
            val normalPermissions = permissions.filter { it.category != PermissionCategory.DANGEROUS }

            // ── Unreliable Permissions Section ──
            PermissionSectionHeader(
                title = "Unreliable Permissions",
                color = Color(0xFFF44336) // Red
            )
            
            if (dangerousPermissions.isEmpty()) {
                Text(
                    text = "No unreliable permissions requested.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                val sortedDangerous = dangerousPermissions.sortedWith(
                    compareBy(
                        { it.usageRole != PermissionUsageRole.PREREQUISITE },
                        { it.label }
                    )
                )
                sortedDangerous.forEach { permission ->
                    PermissionToggleItem(
                        permission = permission,
                        onToggleRequested = { pendingTogglePermission = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Reliable Permissions Section ──
            PermissionSectionHeader(
                title = "Reliable Permissions",
                color = Color(0xFF4CAF50) // Green
            )
            
            if (normalPermissions.isEmpty()) {
                Text(
                    text = "No reliable permissions requested.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                val sortedNormal = normalPermissions.sortedWith(
                    compareBy(
                        { it.usageRole != PermissionUsageRole.PREREQUISITE },
                        { it.label }
                    )
                )
                sortedNormal.forEach { permission ->
                    PermissionToggleItem(
                        permission = permission,
                        onToggleRequested = { pendingTogglePermission = it }
                    )
                }
            }


        }
    }

    // Warning dialog: shown when the user taps a permission toggle.
    // Guides the user to the system App Info screen for the selected package.
    pendingTogglePermission?.let { perm ->
        PermissionSettingsWarningDialog(
            permission = perm,
            packageName = packageName,
            onDismiss = { pendingTogglePermission = null }
        )
    }
}

@Composable
private fun InfoRow(
    label: String, 
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

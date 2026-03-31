package com.privacynudge.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privacynudge.ui.components.AppInsightCard
import com.privacynudge.ui.components.AppInsightsSummary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

/**
 * Screen displaying comprehensive insights for all user-installed apps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInsightsScreen(
    viewModel: AppInsightsViewModel = viewModel(),
    onStartVpn: (String, String) -> Unit,
    onStopVpn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val sortedApps = viewModel.getSortedApps()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "App Insights",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Resource usage and risk analysis",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = { viewModel.refresh() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Dashboard
            item {
                AppInsightsSummary(uiState.summaryStats)
            }

            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (sortedApps.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No apps found",
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                items(sortedApps) { app ->
                    AppInsightCard(
                        app = app,
                        onBlockToggle = { pkg ->
                            viewModel.toggleBlockApp(pkg)
                            if (!uiState.isVpnActive) {
                                onStartVpn(app.packageName, app.name)
                            }
                        },
                        onViewGraph = { viewModel.showPermissionGraph(app) }
                    )
                }
            }
        }

        if (uiState.showGraph && uiState.graphApp != null) {
            com.privacynudge.ui.components.PermissionGraphDialog(
                app = uiState.graphApp!!,
                permissions = uiState.graphPermissions,
                onDismiss = { viewModel.closePermissionGraph() }
            )
        }
    }
}

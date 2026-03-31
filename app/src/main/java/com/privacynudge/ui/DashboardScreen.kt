package com.privacynudge.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.content.Intent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privacynudge.data.*
import com.privacynudge.ui.components.*

/**
 * Main dashboard screen showing Facebook, LinkedIn, and WhatsApp monitoring.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onStartVpn: (String?, String?) -> Unit = { _, _ -> },
    onStopVpn: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "App Privacy Risk Analysis",
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

            Spacer(modifier = Modifier.height(24.dp))

            // Live Awareness Toggle
            LiveAwarenessCard(
                context = LocalContext.current,
                onStartVpn = onStartVpn,
                onStopVpn = onStopVpn
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Risk Sections
            if (uiState.isLoadingCategorized) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                RiskCategorizedAppsSection(
                    title = "🔴 High Risk Apps (PRS > 0.7)",
                    apps = uiState.highRiskApps,
                    onAppClick = { viewModel.selectApp(it) },
                    description = "These apps request multiple high-risk permissions or exhibit behaviors that may threaten user privacy. Immediate review is advised."
                )
                
                RiskCategorizedAppsSection(
                    title = "🟡 Medium Risk Apps (0.35 < PRS ≤ 0.7)",
                    apps = uiState.mediumRiskApps,
                    onAppClick = { viewModel.selectApp(it) },
                    description = "They pose moderate privacy risks and may access sensitive data occasionally. Regular monitoring is recommended."
                )
                
                RiskCategorizedAppsSection(
                    title = "🟢 Low Risk Apps (PRS ≤ 0.35)",
                    apps = uiState.lowRiskApps,
                    onAppClick = { viewModel.selectApp(it) },
                    description = "They request minimal sensitive permissions and pose little to no privacy risk."
                )

                Spacer(modifier = Modifier.height(16.dp))

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // App detail bottom sheet
    if (uiState.showAppDetail && uiState.selectedApp != null) {
        AppDetailBottomSheet(
            profile = uiState.selectedApp!!,
            onDismiss = { viewModel.closeAppDetail() }
        )
    }
}

/**
 * Bottom sheet showing detailed app information.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailBottomSheet(
    profile: AppPermissionProfile,
    onDismiss: () -> Unit
) {
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
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.appName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    RiskBadge(level = profile.riskLevel)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // PRS Components Section
            Text(
                text = "Privacy Risk Indicators",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PRSIndicatorRow(
                        label = "Exposure",
                        value = profile.sensitivityScore,
                        description = "Data sensitivity risk",
                        color = Color(0xFFF44336)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                    PRSIndicatorRow(
                        label = "Usage",
                        value = profile.usageIntensityScore,
                        description = "App activity intensity",
                        color = Color(0xFFFF9800)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                    PRSIndicatorRow(
                        label = "Traffic",
                        value = profile.networkBehaviourScore,
                        description = "Network behavior risk",
                        color = Color(0xFF2196F3)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Granted permissions
            if (profile.grantedPermissions.isNotEmpty()) {
                Text(
                    text = "Granted Risky Permissions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                profile.grantedPermissions.forEach { permission ->
                    PermissionDetailRow(
                        permission = permission,
                        isGranted = true
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Target sensible permissions for denial view
            val sensitiveTypes = listOf(
                PermissionType.LOCATION, 
                PermissionType.CAMERA, 
                PermissionType.MICROPHONE,
                PermissionType.CONTACTS
            )

            val deniedPermissions = profile.deniedPermissions.filter { 
                it.type in sensitiveTypes
            }
            
            if (deniedPermissions.isNotEmpty()) {
                Text(
                    text = "Denied Sensitive Permissions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                deniedPermissions.forEach { permission ->
                    PermissionDetailRow(
                        permission = permission,
                        isGranted = false
                    )
                }
            }
        }
    }
}


@Composable
fun LiveAwarenessCard(
    context: android.content.Context,
    onStartVpn: (String?, String?) -> Unit,
    onStopVpn: () -> Unit
) {
    var isEnabled by remember { mutableStateOf(PermissionAwarenessService.isRunning) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Live Awareness",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Monitor active permission usage silently",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            ModernPillSwitch(
                checked = isEnabled,
                onCheckedChange = { checked ->
                    isEnabled = checked
                    val intent = Intent(context, PermissionAwarenessService::class.java).apply {
                        action = if (checked) PermissionAwarenessService.ACTION_START else PermissionAwarenessService.ACTION_STOP
                    }
                    if (checked) {
                        context.startForegroundService(intent)
                        onStartVpn(null, "Live Awareness")
                    } else {
                        context.startService(intent)
                        onStopVpn()
                    }
                }
            )
        }
    }
}

@Composable
private fun PermissionDetailRow(
    permission: PermissionState,
    isGranted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = permission.type.icon,
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = permission.type.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = permission.type.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Surface(
            shape = MaterialTheme.shapes.small,
            color = if (isGranted) 
                Color(0xFFF44336).copy(alpha = 0.1f)
            else 
                Color(0xFF4CAF50).copy(alpha = 0.1f)
        ) {
            Text(
                text = if (isGranted) "GRANTED" else "DENIED",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isGranted) Color(0xFFF44336) else Color(0xFF4CAF50),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun PRSIndicatorRow(
    label: String,
    value: Float,
    description: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = String.format("%.2f", value),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = color
            )
            LinearProgressIndicator(
                progress = { value },
                modifier = Modifier.width(60.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.1f)
            )
        }
    }
}

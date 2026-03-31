package com.privacynudge.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.core.graphics.drawable.toBitmap
import com.privacynudge.data.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.remember
import kotlin.math.*

/**
 * Summary card for App Insights.
 */
@Composable
fun AppInsightsSummary(
    stats: com.privacynudge.ui.AppInsightsSummaryStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Privacy Health",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = if (stats.hasHighRisk) "Action Required" else "System Secure",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = if (stats.hasHighRisk) Color(0xFFF44336) else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                // Circular Progress for Avg PRS
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { stats.avgPrsScore / 10f },
                        modifier = Modifier.size(64.dp),
                        strokeWidth = 6.dp,
                        color = if (stats.avgPrsScore > 5f) Color(0xFFF44336) else MaterialTheme.colorScheme.onPrimaryContainer,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                    )
                    Text(
                        text = String.format("%.1f", stats.avgPrsScore),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(24.dp))

            // Main Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryStatNode(
                    icon = Icons.Default.Analytics,
                    label = "Analyzed", 
                    value = stats.totalApps.toString()
                )
                SummaryStatNode(
                    icon = Icons.Default.BugReport,
                    label = "High Risk", 
                    value = stats.highRiskCount.toString(),
                    valueColor = if (stats.highRiskCount > 0) Color(0xFFF44336) else MaterialTheme.colorScheme.onPrimaryContainer
                )
                SummaryStatNode(
                    icon = Icons.Default.Block,
                    label = "Blocked", 
                    value = stats.blockedCount.toString(),
                    valueColor = if (stats.blockedCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer
                )
                SummaryStatNode(
                    icon = Icons.Default.Security,
                    label = "Coverage", 
                    value = "100%"
                )
            }
        }
    }
}

@Composable
private fun SummaryStatNode(
    icon: ImageVector,
    label: String, 
    value: String, 
    valueColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = valueColor.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value, 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.ExtraBold, 
            color = valueColor
        )
        Text(
            text = label, 
            style = MaterialTheme.typography.labelSmall, 
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            fontSize = 10.sp
        )
    }
}

/**
 * Card displaying insights for a single app.
 */
@Composable
fun AppInsightCard(
    app: InstalledApp,
    onBlockToggle: (String) -> Unit,
    onViewGraph: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.clickable { onViewGraph() }.padding(16.dp)) {
            // App Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                app.icon?.let { icon ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Image(
                            bitmap = icon.toBitmap().asImageBitmap(),
                            contentDescription = app.name,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))


            // Metrics Row in a subtle elevated surface
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InsightMetric(
                        icon = Icons.Default.SdStorage,
                        label = "Storage",
                        value = formatBytes(app.insights?.storageUsageBytes ?: 0L),
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(
                        modifier = Modifier.height(24.dp).width(1.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    InsightMetric(
                        icon = Icons.Default.DataUsage,
                        label = "Data",
                        value = formatBytes(app.insights?.dataUsageBytes ?: 0L),
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(
                        modifier = Modifier.height(24.dp).width(1.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    InsightMetric(
                        icon = Icons.Default.BatteryChargingFull,
                        label = "Battery",
                        value = "${String.format("%.2f", app.insights?.batteryUsagePercentage ?: 0f)}%",
                        valueColor = app.insights?.batteryImpact?.color ?: MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Risk Level and PRS Score Summary
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(app.prsRiskLevel.colorHex).copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = Color(app.prsRiskLevel.colorHex).copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Risk Level",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = when(app.prsRiskLevel) {
                                RiskLevel.HIGH -> "High"
                                RiskLevel.MEDIUM -> "Medium"
                                RiskLevel.LOW -> "Low"
                                else -> "Unknown"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(app.prsRiskLevel.colorHex)
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "PRS Score",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format("%.2f", app.prsScore),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(app.prsRiskLevel.colorHex)
                        )
                    }
                }
            }

            // Blocking Enforcement Section (only for High Risk)
            if (app.prsRiskLevel == RiskLevel.HIGH) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (app.insights?.isBlocked == true) Icons.Default.Shield else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = if (app.insights?.isBlocked == true) Color(0xFFF44336) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Internet Access",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (app.insights?.isBlocked == true) "Strictly Blocked" else "Monitored",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (app.insights?.isBlocked == true) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Switch(
                        checked = app.insights?.isBlocked == true,
                        onCheckedChange = { onBlockToggle(app.packageName) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFF44336),
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightMetric(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


@Composable
fun PermissionGraphDialog(
    app: InstalledApp,
    permissions: List<DetailedPermission>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "Permission Analysis",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(48.dp)) // Balance
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        PRSScoreCard(app, permissions)
                        Spacer(modifier = Modifier.height(16.dp))
                        CategoryDistributionSection(permissions)
                        Spacer(modifier = Modifier.height(16.dp))
                        PrivacyDoctorSummary(app, permissions)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryDistributionSection(permissions: List<DetailedPermission>) {
    val grantedPermissions = permissions.filter { it.isGranted }
    if (grantedPermissions.isEmpty()) return

    val categoryCounts = grantedPermissions
        .groupBy { getPieChartCategory(it.name) }
        .mapValues { it.value.size }

    val categories = listOf(
        "Camera", "Microphone / Audio", "Contacts / Social Data", 
        "Messages / SMS", "Location", "Storage / Memory", 
        "Phone / Call Information", "Network / Internet", 
        "Nearby Devices / Bluetooth", "Sensors / Activity", "Other"
    )

    val chartData = categories.mapNotNull { category ->
        val count = categoryCounts[category] ?: 0
        if (count > 0) category to count else null
    }.toMap()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Permission Category Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            if (chartData.isNotEmpty()) {
                PermissionPieChart(
                    data = chartData,
                    modifier = Modifier
                        .size(200.dp)
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(24.dp))

                PieChartLegend(chartData)
            }
        }
    }
}

@Composable
private fun PermissionPieChart(
    data: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val total = data.values.sum().toFloat()
    val colors = data.keys.map { getCategoryColor(it) }
    
    Canvas(modifier = modifier) {
        var startAngle = -90f
        data.values.forEachIndexed { index, count ->
            val sweepAngle = (count / total) * 360f
            drawArc(
                color = colors[index],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true
            )
            // Add subtle white separator
            drawArc(
                color = Color.White,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                style = Stroke(width = 2.dp.toPx())
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
private fun PieChartLegend(data: Map<String, Int>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        data.forEach { (category, count) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(getCategoryColor(category))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (count == 1) "1 permission" else "$count permissions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun getCategoryColor(category: String): Color {
    return when (category) {
        "Camera" -> Color(0xFF673AB7) // Deep Purple
        "Microphone / Audio" -> Color(0xFFE91E63) // Pink
        "Contacts / Social Data" -> Color(0xFF2196F3) // Blue
        "Messages / SMS" -> Color(0xFFFF9800) // Orange
        "Location" -> Color(0xFFF44336) // Red
        "Storage / Memory" -> Color(0xFF4CAF50) // Green
        "Phone / Call Information" -> Color(0xFF795548) // Brown
        "Network / Internet" -> Color(0xFF00BCD4) // Cyan
        "Nearby Devices / Bluetooth" -> Color(0xFF3F51B5) // Indigo
        "Sensors / Activity" -> Color(0xFFCDDC39) // Lime
        else -> Color(0xFF607D8B) // Blue Grey
    }
}

private fun getPieChartCategory(permissionName: String): String {
    return when {
        permissionName.contains("CAMERA") -> "Camera"
        permissionName.contains("RECORD_AUDIO") || permissionName.contains("MICROPHONE") -> "Microphone / Audio"
        permissionName.contains("CONTACT") || permissionName.contains("ACCOUNT") -> "Contacts / Social Data"
        permissionName.contains("SMS") || permissionName.contains("MMS") -> "Messages / SMS"
        permissionName.contains("LOCATION") -> "Location"
        permissionName.contains("STORAGE") || permissionName.contains("MEDIA") -> "Storage / Memory"
        permissionName.contains("PHONE") || permissionName.contains("CALL_LOG") || 
        permissionName.contains("PROCESS_OUTGOING") || permissionName.contains("CALL_PHONE") -> "Phone / Call Information"
        permissionName.contains("INTERNET") || permissionName.contains("NETWORK") -> "Network / Internet"
        permissionName.contains("BLUETOOTH") || permissionName.contains("NEARBY_WIFI_DEVICES") -> "Nearby Devices / Bluetooth"
        permissionName.contains("SENSOR") || permissionName.contains("ACTIVITY_RECOGNITION") -> "Sensors / Activity"
        else -> "Other"
    }
}

@Composable
private fun PRSScoreCard(app: InstalledApp, permissions: List<DetailedPermission>) {
    val dangerousCount = permissions.count { it.isDangerous && it.isGranted }
    val normalCount = permissions.count { !it.isDangerous && it.isGranted }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = String.format("%.2f", app.prsScore),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = Color(app.prsRiskLevel.colorHex)
                    )
                    Text(
                        text = "Privacy Risk Score",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Score Breakdown removed as per requirement
        }
    }
}





@Composable
private fun PrivacyDoctorSummary(app: InstalledApp, permissions: List<DetailedPermission>) {
    val riskyCategories = permissions.filter { it.isDangerous && it.isGranted }
        .groupBy { getCategory(it.name) }
        .keys
        .filterNotNull()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1).copy(alpha = 0.9f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = Color(0xFFFFA000))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (riskyCategories.isNotEmpty()) {
                    "Privacy Doctor Analysis: ${app.name} has significant access to your ${riskyCategories.joinToString(", ")}. Consider revoking unused permissions in settings."
                } else {
                    "Privacy Doctor Analysis: No major risks detected. This app follows good privacy practices."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5D4037)
            )
        }
    }
}

private fun getCategory(permissionName: String): String? {
    val category = DangerousPermissions.getFunctionalCategory(permissionName)
    return if (category == "Other") null else category
}


private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    
    return if (digitGroups == 0) {
        "${bytes.toInt()} B"
    } else {
        // Use up to 2 decimal places, but trim trailing zeros for a clean look
        val df = java.text.DecimalFormat("#.##")
        "${df.format(value)} ${units[digitGroups]}"
    }
}


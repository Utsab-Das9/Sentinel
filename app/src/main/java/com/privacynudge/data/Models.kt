package com.privacynudge.data

import android.graphics.drawable.Drawable

/**
 * Represents an installed app with full metadata.
 */
data class InstalledApp(
    val name: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable?,
    val isSystemApp: Boolean = false,
    val installTime: Long = 0L,
    val updateTime: Long = 0L,
    val versionName: String = "",
    val versionCode: Long = 0L,
    val prsScore: Float = 0f,
    val prsRiskLevel: RiskLevel = RiskLevel.LOW,
    val sensitivityScore: Float = 0f,
    val usageIntensityScore: Float = 0f,
    val networkBehaviourScore: Float = 0f,
    val insights: AppInsight? = null
)

/**
 * Detailed insights for an app.
 */
data class AppInsight(
    val storageUsageBytes: Long = 0L,
    val dataUsageBytes: Long = 0L,
    val batteryImpact: BatteryImpact = BatteryImpact.LOW,
    val batteryUsagePercentage: Float = 0f,
    val foregroundTimeMs: Long = 0L,
    val isBlocked: Boolean = false
)

/**
 * Battery impact levels.
 */
enum class BatteryImpact(val displayName: String, val color: androidx.compose.ui.graphics.Color) {
    LOW("Low", androidx.compose.ui.graphics.Color(0xFF4CAF50)),
    MEDIUM("Medium", androidx.compose.ui.graphics.Color(0xFFFFB300)),
    HIGH("High", androidx.compose.ui.graphics.Color(0xFFF44336))
}

/**
 * App type filter for browsing.
 */
enum class AppType(val displayName: String) {
    ALL("All"),
    USER("User"),
    SYSTEM("System")
}

/**
 * Sort options for app list.
 */
enum class SortOption(val displayName: String) {
    NAME("Name"),
    INSTALL_DATE("Install Date"),
    UPDATE_DATE("Update Date"),
    PRS_SCORE("PRS Score")
}

/**
 * Location permission status for a given app.
 */
enum class LocationPermissionStatus {
    GRANTED,
    NOT_GRANTED,
    UNKNOWN
}

/**
 * Represents a privacy nudge event logged by the app.
 */
data class NudgeEvent(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val appName: String,
    val packageName: String,
    val domain: String,
    val permissionType: String,
    val reason: String
)

/**
 * VPN monitoring state.
 */
enum class VpnState {
    STOPPED,
    STARTING,
    RUNNING,
    STOPPING
}

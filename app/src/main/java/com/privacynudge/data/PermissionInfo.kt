package com.privacynudge.data

import android.graphics.drawable.Drawable

/**
 * Types of permissions we track for privacy awareness.
 */
enum class PermissionType(
    val displayName: String,
    val icon: String, // Emoji for simple display
    val description: String
) {
    LOCATION("Location", "📍", "Access to your GPS location"),
    CAMERA("Camera", "📷", "Access to take photos and videos"),
    MICROPHONE("Microphone", "🎤", "Access to record audio"),
    CONTACTS("Contacts", "👥", "Access to your contact list"),
    STORAGE("Storage", "📁", "Access to files and media"),
    PHONE("Phone", "📞", "Access to phone state and calls"),
    SMS("SMS", "💬", "Access to text messages"),
    CALENDAR("Calendar", "📅", "Access to calendar events")
}

/**
 * Risk level assessment for an app based on its permission profile.
 */
enum class RiskLevel(
    val displayName: String,
    val description: String,
    val colorHex: Long
) {
    LOW(
        "Low Risk",
        "Minimal sensitive permissions granted",
        0xFF4CAF50 // Green
    ),
    MEDIUM(
        "Medium Risk",
        "Some sensitive permissions granted",
        0xFFFF9800 // Orange
    ),
    HIGH(
        "High Risk",
        "Multiple sensitive permissions granted",
        0xFFF44336 // Red
    )
}

/**
 * State of a single permission for an app.
 */
data class PermissionState(
    val type: PermissionType,
    val isGranted: Boolean,
    val isDangerous: Boolean = true, // Most permissions we track are dangerous
    val lastAccessTime: Long? = null, // From AppOpsManager if available
    val permissionName: String = "" // Android manifest permission name
)

/**
 * Complete permission profile for a target app.
 */
data class AppPermissionProfile(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isInstalled: Boolean,
    val permissions: List<PermissionState>,
    val riskLevel: RiskLevel,
    val prsScore: Float = 0f,
    val sensitivityScore: Float = 0f,
    val usageIntensityScore: Float = 0f,
    val networkBehaviourScore: Float = 0f,
    val lastUsedTime: Long? = null,
    val foregroundTimeToday: Long = 0, // In milliseconds
    val networkDomainsContacted: List<String> = emptyList()
) {
    /**
     * Get only granted permissions
     */
    val grantedPermissions: List<PermissionState>
        get() = permissions.filter { it.isGranted }

    /**
     * Get only denied/not granted permissions
     */
    val deniedPermissions: List<PermissionState>
        get() = permissions.filter { !it.isGranted }

    /**
     * Count of dangerous permissions granted
     */
    val dangerousPermissionCount: Int
        get() = permissions.count { it.isGranted && it.isDangerous }
}

/**
 * Represents a permission access event (when we can detect it)
 */
data class PermissionAccessEvent(
    val packageName: String,
    val permissionType: PermissionType,
    val timestamp: Long,
    val wasGranted: Boolean
)

/**
 * Summary of permission usage for display
 */
data class PermissionUsageSummary(
    val totalAppsMonitored: Int,
    val highRiskCount: Int,
    val mediumRiskCount: Int,
    val lowRiskCount: Int,
    val mostAccessedPermission: PermissionType?,
    val lastNudgeTime: Long?
)

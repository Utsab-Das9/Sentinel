package com.privacynudge.data

import android.content.Context
import com.privacynudge.notification.NotificationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Central engine for generating privacy nudges based on multiple signals:
 * - Permission states
 * - DNS/network activity
 * - App usage patterns
 * - Risk assessments
 */
class NudgeEngine(
    private val context: Context,
    private val permissionInspector: PermissionInspector,
    private val appUsageTracker: AppUsageTracker,
    private val notificationHelper: NotificationHelper,
    private val settingsRepository: SettingsRepository,
    private val nudgeEventRepository: NudgeEventRepository = NudgeEventRepository.getInstance()
) {

    /**
     * Evaluate DNS activity and generate a nudge if concerning.
     * Called when VPN service detects a DNS query to a sensitive domain.
     */
    fun evaluateDnsActivity(packageName: String, domain: String): NudgeEvent? = runBlocking {
        // 0. Check if nudges are enabled globally
        if (!settingsRepository.nudgesEnabled.first()) return@runBlocking null

        // Check if this is a target app
        val targetApp = TargetApps.findByPackageName(packageName) ?: return@runBlocking null
        
        // Match domain to sensitive patterns
        val domainPattern = SensitiveDomains.matchDomain(domain) ?: return@runBlocking null

        // 1. Filter based on specific monitored categories
        val category = domainPattern.category
        val isMonitored = when {
            category.contains("Location") -> settingsRepository.monitorLocation.first()
            category.contains("Camera") -> settingsRepository.monitorCamera.first()
            category.contains("Microphone") -> settingsRepository.monitorMicrophone.first()
            category.contains("Contact") -> settingsRepository.monitorContacts.first()
            category.contains("Storage") -> settingsRepository.monitorStorage.first()
            else -> true
        }
        
        if (!isMonitored) return@runBlocking null

        // Get the app's permission profile
        val profile = permissionInspector.getFullPermissionProfile(packageName)
        
        // Generate contextual message based on permissions and domain
        val (nudgeReason, shouldNotify) = generateDnsNudgeReason(
            profile = profile,
            domain = domain,
            category = category
        )
        
        if (!shouldNotify) return@runBlocking null

        val event = NudgeEvent(
            appName = profile.appName,
            packageName = packageName,
            domain = domain,
            permissionType = domainPattern.category,
            reason = nudgeReason
        )

        // Log the event for history
        nudgeEventRepository.addEvent(event)

        // Send notification
        notificationHelper.sendDataSharingNudge(
            appName = profile.appName,
            domain = domain,
            category = domainPattern.category,
            permissions = profile.grantedPermissions.map { it.type.displayName }
        )
        
        event
    }

    /**
     * Generate a contextual reason for a DNS-based nudge.
     * 
     * This is where privacy decisions are made:
     * - Location permission + Analytics/Advertising domain = High risk (Location leak).
     * - Contacts permission + Social/Auth domain = Potential sync event.
     * - Multiple permissions + Analytics = Mass data profiling.
     */
    private fun generateDnsNudgeReason(
        profile: AppPermissionProfile,
        domain: String,
        category: String
    ): Pair<String, Boolean> {
        val grantedPermissions = profile.grantedPermissions
        
        // Check for concerning combinations
        val hasLocation = grantedPermissions.any { it.type == PermissionType.LOCATION }
        val hasContacts = grantedPermissions.any { it.type == PermissionType.CONTACTS }
        val hasCamera = grantedPermissions.any { it.type == PermissionType.CAMERA }
        val hasMicrophone = grantedPermissions.any { it.type == PermissionType.MICROPHONE }

        return when {
            // Location + Analytics/Tracking - High concern
            hasLocation && category in listOf("Analytics", "Advertising", "Location") -> {
                Pair(
                    "FYI: ${profile.appName} contacted $domain (${category}) " +
                    "and has Location permission. Your location data may be shared.",
                    true
                )
            }
            
            // Contacts + Social/Auth - Medium concern
            hasContacts && category in listOf("Social", "Auth", "Analytics") -> {
                Pair(
                    "${profile.appName} contacted $domain while having Contacts access. " +
                    "Contact information may be synced.",
                    true
                )
            }
            
            // Any tracking domain with multiple permissions
            category == "Analytics" && grantedPermissions.size >= 3 -> {
                val permList = grantedPermissions.take(3).joinToString(", ") { it.type.displayName }
                Pair(
                    "${profile.appName} sent data to analytics ($domain) with access to: $permList",
                    true
                )
            }
            
            // Advertising with any permissions
            category == "Advertising" && grantedPermissions.isNotEmpty() -> {
                Pair(
                    "${profile.appName} contacted ad network ($domain). " +
                    "App has ${grantedPermissions.size} sensitive permissions.",
                    true
                )
            }
            
            // Default for any sensitive domain
            else -> {
                Pair(
                    "${profile.appName} contacted $domain (${category})",
                    grantedPermissions.isNotEmpty() // Only notify if app has permissions
                )
            }
        }
    }

    /**
     * Evaluate an app's permission profile and generate educational nudge.
     * Called periodically or when user views the dashboard.
     */
    fun evaluatePermissionRisk(profile: AppPermissionProfile): NudgeEvent? {
        if (!profile.isInstalled) return null
        if (profile.riskLevel == RiskLevel.LOW) return null

        val reason = when (profile.riskLevel) {
            RiskLevel.HIGH -> {
                val permissions = profile.grantedPermissions.take(4)
                    .joinToString(", ") { it.type.displayName }
                "⚠️ ${profile.appName} has high-risk permissions: $permissions"
            }
            RiskLevel.MEDIUM -> {
                val permissions = profile.grantedPermissions.take(3)
                    .joinToString(", ") { it.type.displayName }
                "${profile.appName} has noteworthy permissions: $permissions"
            }
            RiskLevel.LOW -> return null
        }

        return NudgeEvent(
            appName = profile.appName,
            packageName = profile.packageName,
            domain = "",
            permissionType = "Permissions",
            reason = reason
        )
    }

    /**
     * Generate an educational nudge about an app's current state.
     * Returns a user-friendly message for display in the UI.
     */
    fun generateEducationalNudge(packageName: String): String {
        val profile = permissionInspector.getFullPermissionProfile(packageName)
        
        if (!profile.isInstalled) {
            return "${TargetApps.findByPackageName(packageName)?.displayName ?: "App"} is not installed."
        }

        val grantedCount = profile.grantedPermissions.size
        val lastUsed = appUsageTracker.getLastUsedTimeString(packageName)
        val foregroundTime = appUsageTracker.getForegroundTimeString(packageName)

        val permissionSummary = when {
            grantedCount == 0 -> "No sensitive permissions"
            grantedCount <= 2 -> profile.grantedPermissions.joinToString(" & ") { it.type.icon }
            else -> "${profile.grantedPermissions.take(3).joinToString(" ") { it.type.icon }} +${grantedCount - 3}"
        }

        val riskEmoji = when (profile.riskLevel) {
            RiskLevel.HIGH -> "🔴"
            RiskLevel.MEDIUM -> "🟡"
            RiskLevel.LOW -> "🟢"
        }

        return buildString {
            append("$riskEmoji ${profile.appName}\n")
            append("Permissions: $permissionSummary\n")
            if (appUsageTracker.hasUsageStatsPermission()) {
                append("Last used: $lastUsed\n")
                append("Usage: $foregroundTime")
            }
        }
    }

    /**
     * Get a quick risk summary for all target apps.
     */
    fun getTargetAppsRiskSummary(): Map<String, RiskLevel> {
        return TargetApps.ALL.associate { targetApp ->
            val profile = permissionInspector.getFullPermissionProfile(targetApp.packageName)
            targetApp.packageName to profile.riskLevel
        }
    }

    /**
     * Check if any target app has concerning permission combinations.
     */
    fun hasConcerningApps(): Boolean {
        return TargetApps.ALL.any { targetApp ->
            val profile = permissionInspector.getFullPermissionProfile(targetApp.packageName)
            profile.isInstalled && profile.riskLevel == RiskLevel.HIGH
        }
    }

    /**
     * Generate a summary notification about overall privacy status.
     */
    fun sendPrivacySummaryNotification() {
        val profiles = TargetApps.ALL.map { 
            permissionInspector.getFullPermissionProfile(it.packageName) 
        }.filter { it.isInstalled }

        if (profiles.isEmpty()) return

        val highRiskCount = profiles.count { it.riskLevel == RiskLevel.HIGH }
        val totalPermissions = profiles.sumOf { it.grantedPermissions.size }

        val message = when {
            highRiskCount > 0 -> 
                "$highRiskCount app(s) have high-risk permissions. Tap to review."
            totalPermissions > 5 -> 
                "Your social apps have $totalPermissions sensitive permissions combined."
            else -> 
                "Your social apps have minimal permissions. Good privacy hygiene!"
        }

        notificationHelper.sendEducationalNudge(
            title = "Privacy Status Update",
            message = message
        )
    }
}

package com.privacynudge.data

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import java.util.Calendar

/**
 * Tracks app usage statistics using Android's UsageStatsManager API.
 * 
 * Requires the PACKAGE_USAGE_STATS permission, which must be granted
 * by the user through Settings > Apps > Special access > Usage access.
 */
class AppUsageTracker(private val context: Context) {

    private val usageStatsManager: UsageStatsManager? = 
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    /**
     * Check if the app has permission to access usage stats.
     * This permission must be granted manually by the user in Settings.
     */
    fun hasUsageStatsPermission(): Boolean {
        if (usageStatsManager == null) return false

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        return try {
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )
            // If we get any stats back (even empty), we have permission
            stats != null
        } catch (e: SecurityException) {
            false
        }
    }

    /**
     * Get the Intent to open the Usage Access settings screen.
     */
    fun getUsageStatsSettingsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Get the last time an app was used (brought to foreground).
     * Returns null if permission not granted or data not available.
     */
    fun getLastUsedTime(packageName: String): Long? {
        if (!hasUsageStatsPermission()) return null

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7) // Look back 7 days
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        return try {
            val stats = usageStatsManager?.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )
            
            stats?.find { it.packageName == packageName }?.lastTimeUsed
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get the total foreground time for an app today (in milliseconds).
     */
    fun getForegroundTimeToday(packageName: String): Long {
        if (!hasUsageStatsPermission()) return 0L

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        val now = System.currentTimeMillis()

        return try {
            val stats = usageStatsManager?.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startOfDay,
                now
            )
            
            stats?.find { it.packageName == packageName }?.totalTimeInForeground ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Get usage statistics for all target apps.
     */
    fun getTargetAppsUsage(): Map<String, UsageStats?> {
        if (!hasUsageStatsPermission()) {
            return TargetApps.ALL.associate { it.packageName to null }
        }

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        return try {
            val stats = usageStatsManager?.queryUsageStats(
                UsageStatsManager.INTERVAL_WEEKLY,
                startTime,
                endTime
            )

            TargetApps.ALL.associate { targetApp ->
                targetApp.packageName to stats?.find { it.packageName == targetApp.packageName }
            }
        } catch (e: Exception) {
            TargetApps.ALL.associate { it.packageName to null }
        }
    }

    /**
     * Get a human-readable string for last used time.
     */
    fun getLastUsedTimeString(packageName: String): String {
        val lastUsed = getLastUsedTime(packageName) ?: return "Unknown"
        
        if (lastUsed == 0L) return "Never"

        val now = System.currentTimeMillis()
        val diff = now - lastUsed

        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000} min ago"
            diff < 86400_000 -> "${diff / 3600_000} hours ago"
            diff < 604800_000 -> "${diff / 86400_000} days ago"
            else -> "Over a week ago"
        }
    }

    /**
     * Get a human-readable string for foreground time.
     */
    fun getForegroundTimeString(packageName: String): String {
        val timeMs = getForegroundTimeToday(packageName)
        
        if (timeMs == 0L) return "Not used today"

        val minutes = timeMs / 60_000
        val hours = minutes / 60
        val remainingMinutes = minutes % 60

        return when {
            hours > 0 -> "${hours}h ${remainingMinutes}m today"
            minutes > 0 -> "${minutes}m today"
            else -> "< 1m today"
        }
    }

    /**
     * Check if an app was used recently (within the last hour).
     */
    fun wasUsedRecently(packageName: String): Boolean {
        val lastUsed = getLastUsedTime(packageName) ?: return false
        val oneHourAgo = System.currentTimeMillis() - 3600_000
        return lastUsed > oneHourAgo
    }

    /**
     * Get the package name of the app currently in the foreground.
     * Uses UsageStatsManager.queryEvents to find the most recent MOVE_TO_FOREGROUND event.
     */
    fun getForegroundPackage(): String? {
        if (!hasUsageStatsPermission()) return null

        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10_000 // Look back 10 seconds

        val usageEvents = usageStatsManager?.queryEvents(startTime, endTime) ?: return null
        val event = android.app.usage.UsageEvents.Event()
        
        var lastForegroundPackage: String? = null
        
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastForegroundPackage = event.packageName
            }
        }
        
        return lastForegroundPackage
    }

    /**
     * Get the most recently used target app.
     */
    fun getMostRecentlyUsedTargetApp(): TargetApps.TargetApp? {
        val usageMap = getTargetAppsUsage()
        
        return TargetApps.ALL
            .filter { usageMap[it.packageName]?.lastTimeUsed ?: 0L > 0L }
            .maxByOrNull { usageMap[it.packageName]?.lastTimeUsed ?: 0L }
    }
}

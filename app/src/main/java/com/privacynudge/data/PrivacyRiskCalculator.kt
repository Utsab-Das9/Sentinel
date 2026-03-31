package com.privacynudge.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Calculates Privacy Risk Score (PRS) using the formula:
 * PRS = 0.45(SL) + 0.35(UI) + 0.20(NB)
 */
class PrivacyRiskCalculator(private val context: Context) {

    private val packageManager = context.packageManager
    private val permissionInspector by lazy { PermissionInspector(context) }

    /**
     * Breakdown of weighted PRS components.
     * Guaranteed bounds: Sensitivity <= 0.45, Usage <= 0.35, Network <= 0.20.
     */
    data class PRSBreakdown(
        val weightedSensitivity: Float,
        val weightedUsage: Float,
        val weightedNetwork: Float,
        val totalPRS: Float
    )

    /**
     * Calculate weighted PRS breakdown for an app.
     */
    fun calculatePRSBreakdown(
        packageInfo: android.content.pm.PackageInfo,
        permissions: List<DetailedPermission>
    ): PRSBreakdown {
        val sl = calculateSL(permissions)
        val ui = calculateUI(packageInfo)
        val nb = calculateNB(packageInfo)

        // Weights: SL: 0.45, UI: 0.35, NB: 0.20
        val wSL = (0.45f * sl).coerceIn(0f, 0.45f)
        val wUI = (0.35f * ui).coerceIn(0f, 0.35f)
        val wNB = (0.20f * nb).coerceIn(0f, 0.20f)
        
        val total = (wSL + wUI + wNB).coerceIn(0f, 1f)

        return PRSBreakdown(
            weightedSensitivity = wSL,
            weightedUsage = wUI,
            weightedNetwork = wNB,
            totalPRS = total
        )
    }

    /**
     * Calculate PRS for an app using pre-fetched data.
     */
    fun calculatePRS(
        packageInfo: android.content.pm.PackageInfo,
        permissions: List<DetailedPermission>
    ): Float {
        return calculatePRSBreakdown(packageInfo, permissions).totalPRS
    }

    /**
     * Legacy/Convenience method to calculate PRS for an app.
     * Note: This is expensive as it fetches data from PackageManager.
     */
    fun calculatePRS(packageName: String): Float {
        val packageInfo = try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        } catch (e: Exception) {
            return 0f
        }
        val permissions = permissionInspector.getAllPermissions(packageName)
        return calculatePRS(packageInfo, permissions)
    }

    /**
     * Sensitivity Level (SL) - 0.45 weight
     */
    private fun calculateSL(permissions: List<DetailedPermission>): Float {
        val dangerousCount = permissions.count { it.isDangerous && it.isGranted }
        
        if (dangerousCount == 0) return 0f

        // Highly sensitive permissions check
        val highlySensitive = listOf(
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.READ_CONTACTS",
            "android.permission.READ_SMS",
            "android.permission.READ_CALL_LOG"
        )

        val sensitiveGranted = permissions.count { 
            it.name in highlySensitive && it.isGranted 
        }

        val baseScore = (dangerousCount.toFloat() / 15f).coerceAtMost(1f)
        val sensitivityBonus = (sensitiveGranted.toFloat() / 5f).coerceAtMost(1f)

        return (baseScore * 0.4f + sensitivityBonus * 0.6f).coerceIn(0f, 1f)
    }

    /**
     * Usage Intensity (UI) - 0.35 weight
     */
    private fun calculateUI(packageInfo: android.content.pm.PackageInfo): Float {
        val now = System.currentTimeMillis()
        val updateAgeDays = (now - packageInfo.lastUpdateTime) / (1000 * 60 * 60 * 24)
        
        return when {
            updateAgeDays <= 30 -> 1.0f
            updateAgeDays >= 365 -> 0.1f
            else -> 1.0f - (updateAgeDays.toFloat() / 365f)
        }.coerceIn(0f, 1f)
    }

    /**
     * Network Behaviour (NB) - 0.20 weight
     */
    private fun calculateNB(packageInfo: android.content.pm.PackageInfo): Float {
        val permissions = packageInfo.requestedPermissions ?: emptyArray()

        val hasInternet = permissions.contains("android.permission.INTERNET")
        val hasNetworkState = permissions.contains("android.permission.ACCESS_NETWORK_STATE")
        val hasBackgroundNetwork = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissions.contains("android.permission.FOREGROUND_SERVICE")
        } else {
            false
        }

        if (!hasInternet) return 0f

        var score = 0.5f // Base score for having internet
        if (hasNetworkState) score += 0.2f
        if (hasBackgroundNetwork) score += 0.3f

        return score.coerceIn(0f, 1f)
    }

    /**
     * Map PRS score to RiskLevel.
     */
    fun getRiskLevel(score: Float, packageName: String? = null, appName: String? = null): RiskLevel {
        // Special override for Gemini as per requirement
        val isGemini = packageName == "com.google.android.apps.bard" || 
                      (appName != null && appName.contains("Gemini", ignoreCase = true))
        
        if (isGemini) {
            return RiskLevel.LOW
        }

        return when {
            score >= 0.70f -> RiskLevel.HIGH
            score >= 0.35f -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }
}

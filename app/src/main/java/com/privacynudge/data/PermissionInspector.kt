package com.privacynudge.data

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

/**
 * Extended permission inspector that checks all relevant permissions
 * for target apps and calculates risk levels.
 */
class PermissionInspector(private val context: Context) {

    private val packageManager = context.packageManager
    private val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
    private val prsCalculator by lazy { PrivacyRiskCalculator(context) }

    /**
     * Map of PermissionType to actual Android manifest permissions
     */
    private val permissionMapping = mapOf(
        PermissionType.LOCATION to listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ),
        PermissionType.CAMERA to listOf(
            Manifest.permission.CAMERA
        ),
        PermissionType.MICROPHONE to listOf(
            Manifest.permission.RECORD_AUDIO
        ),
        PermissionType.CONTACTS to listOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
        ),
        PermissionType.STORAGE to listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_MEDIA_VIDEO",
            "android.permission.READ_MEDIA_AUDIO"
        ),
        PermissionType.PHONE to listOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE
        ),
        PermissionType.SMS to listOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS
        ),
        PermissionType.CALENDAR to listOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
    )

    /**
     * Get the full permission profile for a package.
     */
    fun getFullPermissionProfile(packageName: String): AppPermissionProfile {
        val isInstalled = isAppInstalled(packageName)
        
        if (!isInstalled) {
            val targetApp = TargetApps.findByPackageName(packageName)
            return AppPermissionProfile(
                packageName = packageName,
                appName = targetApp?.displayName ?: packageName,
                icon = null,
                isInstalled = false,
                permissions = emptyList(),
                riskLevel = RiskLevel.LOW,
                prsScore = 0f
            )
        }

        val appInfo = try {
            packageManager.getApplicationInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }

        val appName = appInfo?.let { 
            packageManager.getApplicationLabel(it).toString() 
        } ?: TargetApps.findByPackageName(packageName)?.displayName ?: packageName

        val icon = try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }

        val permissions = checkAllPermissions(packageName)
        val pInfo = try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        } catch (e: Exception) {
            null
        }

        val breakdown = if (pInfo != null) {
            val detailedPermissions = getAllPermissions(packageName)
            prsCalculator.calculatePRSBreakdown(pInfo, detailedPermissions)
        } else {
            PrivacyRiskCalculator.PRSBreakdown(0f, 0f, 0f, 0f)
        }

        return AppPermissionProfile(
            packageName = packageName,
            appName = appName,
            icon = icon,
            isInstalled = true,
            permissions = permissions,
            riskLevel = prsCalculator.getRiskLevel(breakdown.totalPRS, packageName, appName),
            prsScore = breakdown.totalPRS,
            sensitivityScore = breakdown.weightedSensitivity,
            usageIntensityScore = breakdown.weightedUsage,
            networkBehaviourScore = breakdown.weightedNetwork
        )
    }

    /**
     * Check all permission types for a package.
     */
    fun checkAllPermissions(packageName: String): List<PermissionState> {
        val packageInfo = try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        } catch (e: PackageManager.NameNotFoundException) {
            return emptyList()
        }

        val requestedPermissions = packageInfo.requestedPermissions ?: return emptyList()
        val requestedPermissionsFlags = packageInfo.requestedPermissionsFlags ?: return emptyList()

        return PermissionType.entries.map { permissionType ->
            val androidPermissions = permissionMapping[permissionType] ?: emptyList()
            val isGranted = checkPermissionGranted(
                androidPermissions,
                requestedPermissions,
                requestedPermissionsFlags
            )
            val lastAccessTime = getLastAccessTime(packageName, permissionType)
            
            PermissionState(
                type = permissionType,
                isGranted = isGranted,
                isDangerous = true,
                lastAccessTime = lastAccessTime,
                permissionName = androidPermissions.firstOrNull() ?: ""
            )
        }
    }

    /**
     * Check if any of the given permissions are granted.
     */
    private fun checkPermissionGranted(
        androidPermissions: List<String>,
        requestedPermissions: Array<String>,
        requestedPermissionsFlags: IntArray
    ): Boolean {
        for (permission in androidPermissions) {
            val index = requestedPermissions.indexOf(permission)
            if (index >= 0 && index < requestedPermissionsFlags.size) {
                val isGranted = (requestedPermissionsFlags[index] and 
                    PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                if (isGranted) return true
            }
        }
        return false
    }

    /**
     * Get the last access time for a permission type using AppOpsManager.
     * Only available on Android 10+ and may not be accurate for all permission types.
     */
    fun getLastAccessTime(packageName: String, permissionType: PermissionType): Long? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null
        }

        val opName = when (permissionType) {
            PermissionType.LOCATION -> AppOpsManager.OPSTR_FINE_LOCATION
            PermissionType.CAMERA -> AppOpsManager.OPSTR_CAMERA
            PermissionType.MICROPHONE -> AppOpsManager.OPSTR_RECORD_AUDIO
            PermissionType.CONTACTS -> AppOpsManager.OPSTR_READ_CONTACTS
            PermissionType.PHONE -> AppOpsManager.OPSTR_READ_PHONE_STATE
            else -> return null
        }

        return try {
            // Note: This requires PACKAGE_USAGE_STATS permission
            // and may return 0 if not available
            val uid = packageManager.getApplicationInfo(packageName, 0).uid
            // AppOpsManager doesn't expose direct last access time easily
            // This is a simplified approach
            null // Actual implementation would need more complex logic
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calculate the risk level based on granted dangerous permissions.
     */
    fun calculateRiskLevel(permissions: List<PermissionState>): RiskLevel {
        // Fallback for simple count-based assessment if direct packageName-based PRS is not feasible
        val dangerousGrantedCount = permissions.count { it.isGranted && it.isDangerous }
        return when {
            dangerousGrantedCount >= 4 -> RiskLevel.HIGH
            dangerousGrantedCount >= 2 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }

    /**
     * Check if an app is installed on the device.
     */
    fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Legacy method for backward compatibility - checks location permission.
     */
    fun checkLocationPermission(packageName: String): LocationPermissionStatus {
        return try {
            val packageInfo = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_PERMISSIONS
            )

            val requestedPermissions = packageInfo.requestedPermissions 
                ?: return LocationPermissionStatus.NOT_GRANTED
            val requestedPermissionsFlags = packageInfo.requestedPermissionsFlags 
                ?: return LocationPermissionStatus.NOT_GRANTED

            val hasFineLocation = requestedPermissions.contains(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            val hasCoarseLocation = requestedPermissions.contains(
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

            if (!hasFineLocation && !hasCoarseLocation) {
                return LocationPermissionStatus.NOT_GRANTED
            }

            for (i in requestedPermissions.indices) {
                if (i >= requestedPermissionsFlags.size) break
                val permission = requestedPermissions[i]
                val isGranted = (requestedPermissionsFlags[i] and 
                    PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0

                if (isGranted && (permission == Manifest.permission.ACCESS_FINE_LOCATION ||
                            permission == Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    return LocationPermissionStatus.GRANTED
                }
            }

            LocationPermissionStatus.NOT_GRANTED
        } catch (e: PackageManager.NameNotFoundException) {
            LocationPermissionStatus.UNKNOWN
        } catch (e: Exception) {
            LocationPermissionStatus.UNKNOWN
        }
    }

    /**
     * Gets a human-readable description of permissions for an app.
     */
    fun getPermissionSummary(packageName: String): String {
        val profile = getFullPermissionProfile(packageName)
        val grantedCount = profile.grantedPermissions.size
        
        return when {
            !profile.isInstalled -> "App not installed"
            grantedCount == 0 -> "No sensitive permissions"
            else -> {
                val permissionNames = profile.grantedPermissions
                    .take(3)
                    .joinToString(", ") { it.type.displayName }
                "$grantedCount permissions: $permissionNames"
            }
        }
    }

    /**
     * Get ALL permissions requested by an app with detailed information.
     * This provides more granular information than checkAllPermissions.
     */
    fun getAllPermissions(packageName: String): List<DetailedPermission> {
        val packageInfo = try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        } catch (e: PackageManager.NameNotFoundException) {
            return emptyList()
        }

        val requestedPermissions = packageInfo.requestedPermissions ?: return emptyList()
        val requestedPermissionsFlags = packageInfo.requestedPermissionsFlags ?: IntArray(0)

        return requestedPermissions.mapIndexed { index, permissionName ->
            val isGranted = if (index < requestedPermissionsFlags.size) {
                (requestedPermissionsFlags[index] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
            } else {
                false
            }
            
            val category = categorizePermission(permissionName)
            val shortName = permissionName.substringAfterLast('.')
            val friendlyName = DangerousPermissions.getFriendlyName(permissionName)

            // Classify necessity: Prerequisite (core feature) vs Optional (extra)
            val role = PermissionClassifier.classify(permissionName, category)
            val reason = PermissionClassifier.getReason(permissionName, role)
            
            DetailedPermission(
                name = permissionName,
                shortName = shortName,
                label = friendlyName,
                category = category,
                isGranted = isGranted,
                description = getPermissionDescription(permissionName),
                usageRole = role,
                usageReason = reason
            )

        }.sortedWith(
            compareBy<DetailedPermission> { it.category != PermissionCategory.DANGEROUS }
                .thenBy { !it.isGranted }
                .thenBy { it.shortName }
        )
    }

    /**
     * Get only dangerous/sensitive permissions for an app.
     */
    fun getDangerousPermissions(packageName: String): List<DetailedPermission> {
        return getAllPermissions(packageName).filter { it.isDangerous }
    }

    /**
     * Get count of sensitive permissions granted to an app.
     */
    fun getSensitivePermissionCount(packageName: String): Int {
        return getAllPermissions(packageName).count { 
            it.isDangerous && it.isGranted 
        }
    }

    /**
     * Categorize a permission based on its protection level.
     */
    fun categorizePermission(permissionName: String): PermissionCategory {
        // Check if it's a known dangerous permission
        if (DangerousPermissions.isSensitive(permissionName)) {
            return PermissionCategory.DANGEROUS
        }
        
        // Try to get permission info from system
        return try {
            val permissionInfo = packageManager.getPermissionInfo(permissionName, 0)
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                    when (permissionInfo.protection) {
                        android.content.pm.PermissionInfo.PROTECTION_DANGEROUS -> 
                            PermissionCategory.DANGEROUS
                        android.content.pm.PermissionInfo.PROTECTION_SIGNATURE -> 
                            PermissionCategory.SIGNATURE
                        else -> PermissionCategory.NORMAL
                    }
                }
                else -> {
                    @Suppress("DEPRECATION")
                    when (permissionInfo.protectionLevel and android.content.pm.PermissionInfo.PROTECTION_MASK_BASE) {
                        android.content.pm.PermissionInfo.PROTECTION_DANGEROUS -> 
                            PermissionCategory.DANGEROUS
                        android.content.pm.PermissionInfo.PROTECTION_SIGNATURE -> 
                            PermissionCategory.SIGNATURE
                        else -> PermissionCategory.NORMAL
                    }
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            // Unknown permission, default to normal
            PermissionCategory.NORMAL
        }
    }

    /**
     * Get a user-friendly description for a permission.
     */
    private fun getPermissionDescription(permissionName: String): String {
        return try {
            val permissionInfo = packageManager.getPermissionInfo(permissionName, 0)
            permissionInfo.loadDescription(packageManager)?.toString() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Get permission summary with icons for display.
     */
    fun getPermissionIconSummary(packageName: String, maxIcons: Int = 4): String {
        val dangerous = getDangerousPermissions(packageName)
            .filter { it.isGranted }
            .take(maxIcons)
        
        if (dangerous.isEmpty()) return ""
        
        val icons = dangerous.map { DangerousPermissions.getIcon(it.name) }
        val remaining = getDangerousPermissions(packageName).count { it.isGranted } - maxIcons
        
        return if (remaining > 0) {
            "${icons.joinToString("")} +$remaining"
        } else {
            icons.joinToString("")
        }
    }
}


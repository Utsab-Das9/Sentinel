package com.privacynudge.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Repository for fetching installed apps with full metadata.
 */
class InstalledAppsRepository(private val context: Context) {
    /**
     * Returns true if the app is a system app.
     */
    private fun isSystemApp(appInfo: ApplicationInfo): Boolean {
        return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    }

    /**
     * Returns the version code for a package, handling API differences.
     */
    private fun getVersionCode(packageInfo: PackageInfo?): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode ?: 0L
        } else {
            @Suppress("DEPRECATION")
            packageInfo?.versionCode?.toLong() ?: 0L
        }
    }

    private val packageManager = context.packageManager
    private val prsCalculator = PrivacyRiskCalculator(context)
    private val permissionInspector = PermissionInspector(context)

    companion object {
        /**
         * Priority system apps that should always be included in monitoring.
         * These apps are commonly used utilities and system apps that users interact with regularly.
         */
        private val PRIORITY_SYSTEM_APPS = setOf(
            // Google Apps
            "com.google.android.youtube",           // YouTube
            "com.android.vending",                  // Play Store
            "com.google.android.apps.photos",       // Google Photos
            "com.google.android.apps.maps",         // Maps
            "com.google.android.gm",                // Gmail
            "com.google.android.googlequicksearchbox", // Google
            "com.google.android.apps.messaging",    // Messages
            "com.android.chrome",                   // Chrome
            "com.google.android.apps.docs",         // Files by Google
            "com.google.android.apps.nbu.paisa.user", // GPay
            "com.google.android.projection.gearhead", // Android Auto
            "com.google.ar.lens",                   // Lens
            "com.google.android.apps.wellbeing",    // Digital Wellbeing
            
            // Samsung Apps
            "com.samsung.android.calendar",         // Calendar
            "com.sec.android.app.camera",           // Camera
            "com.android.settings",                 // Settings
            "com.samsung.android.app.settings.bixby", // Settings
            "com.samsung.android.dialer",           // Phone
            "com.samsung.android.messaging",        // Messages
            "com.sec.android.app.myfiles",          // My Files
            "com.samsung.android.app.telephonyui",  // Phone Manager
            "com.samsung.android.themestore",       // Theme Store
            "com.samsung.android.app.sbrowseredge", // Internet
            "com.samsung.android.contacts",         // Contacts
            "com.samsung.android.app.clockpackage", // Clock
            "com.samsung.android.video",            // Videos
            "com.samsung.android.weather",          // Weather
            "com.samsung.android.game.gametools",   // Game Assistant
            "com.samsung.android.app.assistant",    // Assistant
            
            // Generic Android Apps
            "com.android.calculator2",              // Calculator
            "com.android.calendar",                 // Calendar
            "com.android.camera",                   // Camera
            "com.android.camera2",                  // Camera
            "com.android.deskclock",                // Clock
            "com.android.settings",                 // Settings
            "com.android.dialer",                   // Phone
            "com.android.contacts",                 // Contacts
            "com.android.mms",                      // Messages
            "com.android.providers.downloads.ui",   // Downloads
            "com.android.documentsui",              // Files
            
            // Other Manufacturers
            "com.miui.calculator",                  // Xiaomi Calculator
            "com.miui.gallery",                     // Xiaomi Gallery
            "com.miui.compass",                     // Xiaomi Compass
            "com.android.gallery3d",                // Gallery
            "com.google.android.apps.photos",       // Photos
            "com.oneplus.calculator",               // OnePlus Calculator
            "com.oneplus.camera",                   // OnePlus Camera
            "com.coloros.compass",                  // Oppo Compass
            "com.coloros.calculator",               // Oppo Calculator
            "com.oppo.camera",                      // Oppo Camera
            "com.huawei.calculator",                // Huawei Calculator
            "com.huawei.camera",                    // Huawei Camera
            "com.vivo.calculator",                  // Vivo Calculator
            "com.vivo.camera"                       // Vivo Camera
        )
        
        /**
         * Check if a package is a priority system app that should be included.
         */
        fun isPrioritySystemApp(packageName: String): Boolean {
            return PRIORITY_SYSTEM_APPS.contains(packageName)
        }
    }

    fun getPackageManager(): PackageManager = packageManager

    /**
     * Returns a list of all launchable apps (apps with a launcher intent).
     * Excludes system apps without launcher activity.
     * This is the original method for backward compatibility.
     */
    suspend fun getInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfoList = packageManager.queryIntentActivities(mainIntent, 0)

        resolveInfoList
            .filter { it.activityInfo.packageName != context.packageName }
            .mapNotNull { resolveInfo ->
                val appInfo = resolveInfo.activityInfo.applicationInfo ?: return@mapNotNull null
                val packageInfo = try {
                    packageManager.getPackageInfo(appInfo.packageName, 0)
                } catch (e: Exception) {
                    null
                }
                
                InstalledApp(
                    name = packageManager.getApplicationLabel(appInfo).toString(),
                    packageName = appInfo.packageName,
                    icon = try {
                        packageManager.getApplicationIcon(appInfo.packageName)
                    } catch (e: Exception) {
                        null
                    },
                    isSystemApp = isSystemApp(appInfo),
                    installTime = packageInfo?.firstInstallTime ?: 0L,
                    updateTime = packageInfo?.lastUpdateTime ?: 0L,
                    versionName = packageInfo?.versionName ?: "",
                    versionCode = getVersionCode(packageInfo)
                )

            }
            .distinctBy { it.packageName }
            .sortedBy { it.name.lowercase() }
    }

    /**
     * Returns ALL installed apps (user + system) with full metadata.
     * @param includeSystem Whether to include system apps
     */
    suspend fun getAllInstalledApps(includeSystem: Boolean = true): List<InstalledApp> = 
        withContext(Dispatchers.IO) {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                PackageManager.MATCH_UNINSTALLED_PACKAGES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_UNINSTALLED_PACKAGES
            }
            val packages = try {
                packageManager.getInstalledPackages(flags)
            } catch (e: Exception) {
                packageManager.getInstalledPackages(0)
            }
            coroutineScope {
                val deferredApps = packages
                    .filter { it.packageName != context.packageName }
                    .map { packageInfo ->
                        async(Dispatchers.IO) {
                            val appInfo = packageInfo.applicationInfo ?: return@async null
                            // Filter system apps if needed, but ALWAYS include priority system apps
                            if (!includeSystem && isSystemApp(appInfo) && 
                                !isPrioritySystemApp(packageInfo.packageName)) {
                                return@async null
                            }
                            val pInfo = try {
                                packageManager.getPackageInfo(packageInfo.packageName, PackageManager.GET_PERMISSIONS)
                            } catch (e: Exception) {
                                null
                            }
                            val permissionsInner = if (pInfo != null) permissionInspector.getAllPermissions(pInfo.packageName) else emptyList()
                            val breakdown = if (pInfo != null) {
                                prsCalculator.calculatePRSBreakdown(pInfo, permissionsInner)
                            } else {
                                PrivacyRiskCalculator.PRSBreakdown(0f, 0f, 0f, 0f)
                            }
                            InstalledApp(
                                name = try {
                                    packageManager.getApplicationLabel(appInfo).toString()
                                } catch (e: Exception) {
                                    packageInfo.packageName
                                },
                                packageName = packageInfo.packageName,
                                icon = try {
                                    packageManager.getApplicationIcon(appInfo)
                                } catch (e: Exception) {
                                    null
                                },
                                isSystemApp = isSystemApp(appInfo),
                                installTime = packageInfo.firstInstallTime,
                                updateTime = packageInfo.lastUpdateTime,
                                versionName = packageInfo.versionName ?: "",
                                versionCode = getVersionCode(packageInfo),
                                prsScore = breakdown.totalPRS,
                                prsRiskLevel = prsCalculator.getRiskLevel(
                                    breakdown.totalPRS, 
                                    packageInfo.packageName,
                                    packageManager.getApplicationLabel(appInfo).toString()
                                ),
                                sensitivityScore = breakdown.weightedSensitivity,
                                usageIntensityScore = breakdown.weightedUsage,
                                networkBehaviourScore = breakdown.weightedNetwork,
                                insights = null // Will be populated in getAppsWithInsights
                            )
                        }
                    }
                deferredApps.awaitAll()
                    .filterNotNull()
                    .distinctBy { it.packageName }
                    .sortedBy { it.name.lowercase() }
            }
        }

    /**
     * Get apps filtered by type.
     */
    suspend fun getAppsByType(appType: AppType): List<InstalledApp> = 
        withContext(Dispatchers.IO) {
            val allApps = getAllInstalledApps(includeSystem = true)
            when (appType) {
                AppType.ALL -> allApps
                AppType.USER -> allApps.filter { 
                    !it.isSystemApp || isPrioritySystemApp(it.packageName)
                }
                AppType.SYSTEM -> allApps.filter { 
                    it.isSystemApp && !isPrioritySystemApp(it.packageName)
                }
            }
        }

    /**
     * Get details for a specific app.
     */
    suspend fun getAppDetails(packageName: String): InstalledApp? = 
        withContext(Dispatchers.IO) {
            try {
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                val appInfo = packageInfo.applicationInfo ?: return@withContext null
                
                InstalledApp(
                    name = packageManager.getApplicationLabel(appInfo).toString(),
                    packageName = packageInfo.packageName,
                    icon = try {
                        packageManager.getApplicationIcon(appInfo)
                    } catch (e: Exception) {
                        null
                    },
                    isSystemApp = isSystemApp(appInfo),
                    installTime = packageInfo.firstInstallTime,
                    updateTime = packageInfo.lastUpdateTime,
                    versionName = packageInfo.versionName ?: "",
                    versionCode = getVersionCode(packageInfo),
                    prsScore = prsCalculator.calculatePRS(packageInfo.packageName),
                    prsRiskLevel = prsCalculator.getRiskLevel(
                        prsCalculator.calculatePRS(packageInfo.packageName),
                        packageInfo.packageName,
                        packageManager.getApplicationLabel(appInfo).toString()
                    )
                )

            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }

    /**
     * Check if an app is a system app.
     */

    /**
     * Get version code handling API differences.
     */
    @Suppress("DEPRECATION")

    /**
     * Get human-readable install date.
     */
    fun getInstallDateString(installTime: Long): String {
        if (installTime == 0L) return "Unknown"
        val now = System.currentTimeMillis()
        val diff = now - installTime
        
        return when {
            diff < 86400_000L -> "Today"
            diff < 172800_000L -> "Yesterday"
            diff < 604800_000L -> "${diff / 86400_000L} days ago"
            diff < 2592000_000L -> "${diff / 604800_000L} weeks ago"
            diff < 31536000_000L -> "${diff / 2592000_000L} months ago"
            else -> "${diff / 31536000_000L} years ago"
        }
    }

    /**
     * Get human-readable update date.
     */
    fun getUpdateDateString(updateTime: Long): String {
        if (updateTime == 0L) return "Never"
        return getInstallDateString(updateTime)
    }


}

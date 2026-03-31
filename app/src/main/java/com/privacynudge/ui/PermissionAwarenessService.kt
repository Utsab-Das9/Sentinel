package com.privacynudge.ui

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.content.pm.PackageManager
import com.privacynudge.data.AppUsageTracker
import com.privacynudge.data.PermissionInspector
import com.privacynudge.data.PermissionUsageDetector
import com.privacynudge.notification.NotificationHelper
import com.privacynudge.data.SettingsRepository
import com.privacynudge.data.DangerousPermissions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * Foreground service that monitors the active app's permission usage.
 */
class PermissionAwarenessService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var appUsageTracker: AppUsageTracker
    private lateinit var permissionUsageDetector: PermissionUsageDetector
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var permissionInspector: PermissionInspector
    private lateinit var packageManagerHelper: PackageManager
    private lateinit var settingsRepository: SettingsRepository

    private var monitorJob: Job? = null
    private val lastNotifiedMap = mutableMapOf<String, Long>()
    private val THROTTLE_MS = 120_000L // 2 minutes

    companion object {
        const val ACTION_START = "com.privacynudge.AWARENESS_START"
        const val ACTION_STOP = "com.privacynudge.AWARENESS_STOP"
        
        var isRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appUsageTracker = AppUsageTracker(this)
        permissionUsageDetector = PermissionUsageDetector(this)
        notificationHelper = NotificationHelper(this)
        permissionInspector = PermissionInspector(this)
        packageManagerHelper = packageManager
        settingsRepository = SettingsRepository(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startMonitoring()
            ACTION_STOP -> stopMonitoring()
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        if (isRunning) return
        isRunning = true

        // Ensure VPN is active for continuous monitoring
        val vpnIntent = Intent(this, com.privacynudge.vpn.PrivacyNudgeVpnService::class.java).apply {
            action = com.privacynudge.vpn.PrivacyNudgeVpnService.ACTION_START
        }
        startService(vpnIntent)

        val initialNotification = notificationHelper.createAwarenessNotification("Sentinel Live Awareness", emptyList())
        startForeground(NotificationHelper.AWARENESS_NOTIFICATION_ID, initialNotification)

        monitorJob = serviceScope.launch {
            while (isActive) {
                val foregroundPackage = appUsageTracker.getForegroundPackage()
                if (foregroundPackage != null && foregroundPackage != packageName) {
                    try {
                        val appInfo = packageManagerHelper.getApplicationInfo(foregroundPackage, 0)
                        val appLabel = packageManagerHelper.getApplicationLabel(appInfo).toString()
                        
                        // 1. Get ALL granted dangerous permissions (for the persistent notification)
                        // Use checkAllPermissions which returns PermissionState objects
                        val permissionStates = permissionInspector.checkAllPermissions(foregroundPackage)
                        val grantedPermissionStates = permissionStates.filter { it.isGranted }
                        
                        // Debug logging
                        android.util.Log.d("PermissionAwareness", "=== App: $appLabel ===")
                        android.util.Log.d("PermissionAwareness", "Total permission types checked: ${permissionStates.size}")
                        android.util.Log.d("PermissionAwareness", "Granted permission types: ${grantedPermissionStates.size}")
                        grantedPermissionStates.forEach { perm ->
                            android.util.Log.d("PermissionAwareness", "  ✓ ${perm.type.displayName} (${perm.type.icon})")
                        }
                        
                        // Convert to labels for notification
                        val grantedLabels = grantedPermissionStates.map { it.type.displayName }
                        
                        // 2. Get CURRENTLY ACTIVE permissions (for the FYI notification)
                        val activeUsage = permissionUsageDetector.getActivePermissions(foregroundPackage, appInfo.uid)
                        
                        // Update persistent notification with granted permissions
                        val persistentNotification = notificationHelper.createAwarenessNotification(
                            appLabel, 
                            grantedLabels
                        )
                        val notificationManager = getSystemService(android.app.NotificationManager::class.java)
                        notificationManager.notify(NotificationHelper.AWARENESS_NOTIFICATION_ID, persistentNotification)

                        // 3. Handle FYI Notifications with Throttling
                        if (activeUsage.isNotEmpty()) {
                            val now = System.currentTimeMillis()
                            val newlyActiveLabels = mutableListOf<String>()

                            activeUsage.forEach { usage ->
                                val throttleKey = "${foregroundPackage}_${usage.label}"
                                val lastTime = lastNotifiedMap[throttleKey] ?: 0L
                                
                                if (now - lastTime > THROTTLE_MS) {
                                    newlyActiveLabels.add(usage.label)
                                    lastNotifiedMap[throttleKey] = now
                                }
                            }

                            if (newlyActiveLabels.isNotEmpty()) {
                                // 4. Check if nudges/notifications are enabled in settings
                                val nudgesEnabled = settingsRepository.nudgesEnabled.first()
                                if (nudgesEnabled) {
                                    // 5. Filter based on specific monitored categories
                                    val monitoredCategories = newlyActiveLabels.filter { label ->
                                        val permission = activeUsage.find { it.label == label }?.manifestPermission ?: ""
                                        val category = DangerousPermissions.getFunctionalCategory(permission)
                                        when {
                                            category.contains("Location") -> settingsRepository.monitorLocation.first()
                                            category.contains("Camera") -> settingsRepository.monitorCamera.first()
                                            category.contains("Microphone") -> settingsRepository.monitorMicrophone.first()
                                            category.contains("Contact") -> settingsRepository.monitorContacts.first()
                                            category.contains("Storage") -> settingsRepository.monitorStorage.first()
                                            else -> true // default to notify for other categories
                                        }
                                    }

                                    if (monitoredCategories.isNotEmpty()) {
                                        notificationHelper.sendLiveAwarenessNotification(appLabel, monitoredCategories)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Package might have been uninstalled or other issue
                    }
                }
                delay(2000) // Poll every 2 seconds for responsiveness
            }
        }
    }

    private fun stopMonitoring() {
        isRunning = false
        monitorJob?.cancel()
        
        // Stop VPN service as per requirement
        val vpnIntent = Intent(this, com.privacynudge.vpn.PrivacyNudgeVpnService::class.java).apply {
            action = com.privacynudge.vpn.PrivacyNudgeVpnService.ACTION_STOP
        }
        startService(vpnIntent)
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        isRunning = false
    }
}

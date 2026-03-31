package com.privacynudge.data

import android.app.AppOpsManager
import android.content.Context
import android.os.Build

/**
 * Detects active or recent permission usage using AppOpsManager.
 * This provides a way to see which apps are actually "using" permissions
 * like Location, Camera, and Microphone in the background or foreground.
 */
class PermissionUsageDetector(private val context: Context) {

    private val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

    data class ActivePermission(
        val name: String,
        val label: String,
        val icon: String,
        val manifestPermission: String
    )

    /**
     * Checks which major sensitive permissions are currently or recently accessed by a package.
     */
    fun getActivePermissions(packageName: String, uid: Int): List<ActivePermission> {
        val active = mutableListOf<ActivePermission>()

        // Maps AppOps to user-friendly labels/icons and manifest permissions
        val opConfig = listOf(
            // Triple(Op, Label, ManifestPermission)
            // Note: Icon is derived from DangerousPermissions.getIcon later
            Triple(AppOpsManager.OPSTR_FINE_LOCATION, "Location", android.Manifest.permission.ACCESS_FINE_LOCATION),
            Triple(AppOpsManager.OPSTR_CAMERA, "Camera", android.Manifest.permission.CAMERA),
            Triple(AppOpsManager.OPSTR_RECORD_AUDIO, "Microphone", android.Manifest.permission.RECORD_AUDIO),
            Triple(AppOpsManager.OPSTR_READ_EXTERNAL_STORAGE, "Storage", android.Manifest.permission.READ_EXTERNAL_STORAGE),
            Triple(AppOpsManager.OPSTR_WRITE_EXTERNAL_STORAGE, "Storage", android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
            Triple(AppOpsManager.OPSTR_READ_CONTACTS, "Contacts", android.Manifest.permission.READ_CONTACTS),
            Triple(AppOpsManager.OPSTR_READ_SMS, "SMS", android.Manifest.permission.READ_SMS),
            Triple(AppOpsManager.OPSTR_READ_CALL_LOG, "Call Log", android.Manifest.permission.READ_CALL_LOG),
            Triple(AppOpsManager.OPSTR_READ_CALENDAR, "Calendar", android.Manifest.permission.READ_CALENDAR),
            // Track Usage Stats and VPN as proxies for "Network/Data" awareness
            Triple(AppOpsManager.OPSTR_GET_USAGE_STATS, "Network Data", android.Manifest.permission.PACKAGE_USAGE_STATS),
            Triple("android:activate_vpn", "Network VPN", "android.permission.BIND_VPN_SERVICE")
        )

        for ((op, label, manifest) in opConfig) {
            if (isOpActive(op, packageName, uid)) {
                active.add(ActivePermission(
                    name = op,
                    label = label,
                    icon = DangerousPermissions.getIcon(manifest),
                    manifestPermission = manifest
                ))
            }
        }

        return active
    }

    private fun isOpActive(op: String, packageName: String, uid: Int): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ (API 30+) provides a direct way to check if an op is active (e.g., mic/camera indicator)
                appOpsManager.isOpActive(op, uid, packageName)
            } else {
                // Fallback for API 26-29: Check if the operation is currently allowed
                val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appOpsManager.unsafeCheckOpNoThrow(op, uid, packageName)
                } else {
                    @Suppress("DEPRECATION")
                    appOpsManager.checkOpNoThrow(op, uid, packageName)
                }
                
                // For older versions, we approximate "active" by combining "allowed" with a recent usage proxy.
                // However, since we poll every 3 seconds while in the foreground, 
                // and the user wants "reliable detection", for older APIs we will report if it's allowed.
                // On API 30+, it will be pixel-perfect based on system state.
                status == AppOpsManager.MODE_ALLOWED
            }
        } catch (e: Exception) {
            false
        }
    }
}

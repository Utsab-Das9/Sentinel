package com.privacynudge.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.privacynudge.MainActivity
import com.privacynudge.data.RiskLevel

/**
 * Helper class for managing notifications.
 * Enhanced with new notification types for privacy nudges.
 */
class NotificationHelper(private val context: Context) {

    companion object {
        const val VPN_SERVICE_CHANNEL_ID = "vpn_service_channel"
        const val NUDGE_CHANNEL_ID = "privacy_nudge_channel"
        const val EDUCATIONAL_CHANNEL_ID = "educational_channel"
        const val PERMISSION_AWARENESS_CHANNEL_ID = "permission_awareness_channel"

        const val VPN_NOTIFICATION_ID = 1
        const val AWARENESS_NOTIFICATION_ID = 2
        private var nudgeNotificationCounter = 100
        private var educationalNotificationCounter = 200

        const val ACTION_STOP_VPN = "com.privacynudge.STOP_VPN"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        // VPN Service Channel (low importance - always visible when VPN running)
        val vpnChannel = NotificationChannel(
            VPN_SERVICE_CHANNEL_ID,
            "VPN Monitoring",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when Sentinel is monitoring network traffic"
            setShowBadge(false)
        }

        // Nudge Channel (default importance for alerts)
        val nudgeChannel = NotificationChannel(
            NUDGE_CHANNEL_ID,
            "Privacy Nudges",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts when monitored apps contact sensitive domains"
        }

        // Educational Channel (low importance for tips)
        val educationalChannel = NotificationChannel(
            EDUCATIONAL_CHANNEL_ID,
            "Privacy Tips",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Educational information about app privacy"
        }

        // Awareness Channel (low importance - silent, no sound/vibe)
        val awarenessChannel = NotificationChannel(
            PERMISSION_AWARENESS_CHANNEL_ID,
            "Permission Awareness",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows real-time permission access by foreground apps"
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(vpnChannel)
        notificationManager.createNotificationChannel(nudgeChannel)
        notificationManager.createNotificationChannel(educationalChannel)
        notificationManager.createNotificationChannel(awarenessChannel)
    }

    /**
     * Creates the foreground service notification for VPN.
     */
    fun createVpnNotification(appName: String): Notification {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(ACTION_STOP_VPN).apply {
            setPackage(context.packageName)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, VPN_SERVICE_CHANNEL_ID)
            .setContentTitle("Sentinel Active")
            .setContentText("Monitoring $appName")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(mainPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    /**
     * Sends a privacy nudge notification (legacy method for backward compatibility).
     */
    fun sendNudgeNotification(
        appName: String,
        domain: String,
        permissionType: String
    ) {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NUDGE_CHANNEL_ID)
            .setContentTitle("Sentinel")
            .setContentText("FYI: $appName just contacted $domain and has $permissionType permission.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("FYI: $appName just contacted $domain and has $permissionType permission.\n\n" +
                        "This doesn't mean data was shared, but the app may be accessing related services."))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(nudgeNotificationCounter++, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted on Android 13+
        }
    }

    /**
     * Sends a data sharing nudge with detailed information.
     */
    fun sendDataSharingNudge(
        appName: String,
        domain: String,
        category: String,
        permissions: List<String>
    ) {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            3,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val permissionText = if (permissions.isNotEmpty()) {
            "App has access to: ${permissions.take(3).joinToString(", ")}"
        } else {
            ""
        }

        val shortText = "FYI: $appName just accessed $domain"
        val longText = buildString {
            append("FYI: Your $appName app just contacted $domain ($category)")
            if (permissions.isNotEmpty()) {
                append("\n\n")
                append(permissionText)
            }
            append("\n\n")
            append("This is informational - we can only see domain names, not the actual data being sent.")
        }

        val notification = NotificationCompat.Builder(context, NUDGE_CHANNEL_ID)
            .setContentTitle("🔔 Privacy Alert")
            .setContentText(shortText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(longText))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(nudgeNotificationCounter++, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted
        }
    }

    /**
     * Sends a permission risk warning notification.
     */
    fun sendPermissionRiskNudge(
        appName: String,
        riskLevel: RiskLevel,
        permissions: List<String>
    ) {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            4,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (title, icon) = when (riskLevel) {
            RiskLevel.HIGH -> "⚠️ High Risk App" to android.R.drawable.ic_dialog_alert
            RiskLevel.MEDIUM -> "📋 Permission Notice" to android.R.drawable.ic_dialog_info
            RiskLevel.LOW -> "ℹ️ App Info" to android.R.drawable.ic_dialog_info
        }

        val permissionList = permissions.take(4).joinToString(", ")
        val text = "$appName has ${riskLevel.displayName.lowercase()} with: $permissionList"

        val notification = NotificationCompat.Builder(context, NUDGE_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(icon)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(
                if (riskLevel == RiskLevel.HIGH) 
                    NotificationCompat.PRIORITY_HIGH 
                else 
                    NotificationCompat.PRIORITY_DEFAULT
            )
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(nudgeNotificationCounter++, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted
        }
    }

    /**
     * Sends an educational nudge notification.
     */
    fun sendEducationalNudge(title: String, message: String) {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            5,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, EDUCATIONAL_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(educationalNotificationCounter++, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted
        }
    }

    /**
     * Creates or updates the silent awareness notification (Persistent).
     */
    fun createAwarenessNotification(appName: String, permissions: List<String>): Notification {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            10,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Sentinel: Live Awareness"
        val contentText = "Monitoring $appName"
        val subtitle = if (permissions.isEmpty()) {
            "No high-risk permissions detected"
        } else {
            "Active: ${permissions.joinToString(", ")}"
        }

        return NotificationCompat.Builder(context, PERMISSION_AWARENESS_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSubText(subtitle)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(mainPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .build()
    }

    /**
     * Sends a one-time silent Live Awareness "FYI" notification.
     */
    fun sendLiveAwarenessNotification(appName: String, permissionLabels: List<String>) {
        if (permissionLabels.isEmpty()) return

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            11,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val permissionText = permissionLabels.joinToString(" and ")
        val text = "FYI: Your $appName app just accessed your $permissionText and shared data with a third party."

        val notification = NotificationCompat.Builder(context, PERMISSION_AWARENESS_CHANNEL_ID)
            .setContentTitle("🛡️ Privacy Awareness")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setSilent(true)
            .build()

        try {
            // Use unique ID or the awareness ID? 
            // The user wants "silent (non-intrusive) notification whenever any app accesses...".
            // Since it's a one-time FYI, we should use a counter or a specific ID range to avoid overwriting the persistent one.
            val notificationId = 1000 + (System.currentTimeMillis() % 1000).toInt()
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted
        }
    }

    /**
     * Cancels the VPN service notification.
     */
    fun cancelVpnNotification() {
        NotificationManagerCompat.from(context).cancel(VPN_NOTIFICATION_ID)
    }
}

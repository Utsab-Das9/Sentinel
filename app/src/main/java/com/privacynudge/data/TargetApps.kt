package com.privacynudge.data

/**
 * Defines the target apps that Privacy Nudge monitors.
 * These are the three social/messaging apps we focus on for privacy awareness.
 */
object TargetApps {

    /**
     * Represents a target app to monitor.
     */
    data class TargetApp(
        val packageName: String,
        val displayName: String,
        val brandColor: Long // ARGB color for UI theming
    )

    /**
     * Facebook - Meta's social network app
     */
    val FACEBOOK = TargetApp(
        packageName = "com.facebook.katana",
        displayName = "Facebook",
        brandColor = 0xFF1877F2 // Facebook blue
    )

    /**
     * LinkedIn - Professional networking app
     */
    val LINKEDIN = TargetApp(
        packageName = "com.linkedin.android",
        displayName = "LinkedIn",
        brandColor = 0xFF0A66C2 // LinkedIn blue
    )

    /**
     * WhatsApp - Meta's messaging app
     */
    val WHATSAPP = TargetApp(
        packageName = "com.whatsapp",
        displayName = "WhatsApp",
        brandColor = 0xFF25D366 // WhatsApp green
    )

    /**
     * YouTube - Google's video sharing platform
     */
    val YOUTUBE = TargetApp(
        packageName = "com.google.android.youtube",
        displayName = "YouTube",
        brandColor = 0xFFFF0000 // YouTube red
    )

    /**
     * All target apps as a list for iteration
     */
    val ALL = listOf(FACEBOOK, LINKEDIN, WHATSAPP, YOUTUBE)

    /**
     * Quick lookup by package name
     */
    fun findByPackageName(packageName: String): TargetApp? {
        return ALL.find { it.packageName == packageName }
    }

    /**
     * Check if a package name is one of our target apps
     */
    fun isTargetApp(packageName: String): Boolean {
        return ALL.any { it.packageName == packageName }
    }
}

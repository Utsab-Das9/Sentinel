package com.privacynudge.data

/**
 * Categories for Android permissions.
 */
enum class PermissionCategory(val displayName: String, val colorHex: Long) {
    DANGEROUS("Dangerous", 0xFFF44336),    // Red - requires runtime permission
    NORMAL("Normal", 0xFF4CAF50),          // Green - granted automatically
    SIGNATURE("Signature", 0xFF9C27B0),    // Purple - only for same-signed apps
    SPECIAL("Special", 0xFFFF9800)         // Orange - special access needed
}

/**
 * Detailed permission information for an app.
 */
data class DetailedPermission(
    val name: String,                      // Full name: android.permission.CAMERA
    val shortName: String,                 // Short: CAMERA
    val label: String,                     // User-friendly: Camera
    val category: PermissionCategory,
    val isGranted: Boolean,
    val description: String = "",
    // ── Necessity classification (added for Prerequisite / Optional UI) ──────
    val usageRole: PermissionUsageRole = PermissionUsageRole.OPTIONAL,
    val usageReason: String = ""           // One-line explanation shown in UI
) {
    val isDangerous: Boolean
        get() = category == PermissionCategory.DANGEROUS
}

/**
 * Known dangerous permissions that require special attention.
 */
object DangerousPermissions {
    
    /**
     * Map of permission names to their short display names.
     */
    val sensitivePermissions = mapOf(
        // Location
        "android.permission.ACCESS_FINE_LOCATION" to "Fine Location",
        "android.permission.ACCESS_COARSE_LOCATION" to "Coarse Location",
        "android.permission.ACCESS_BACKGROUND_LOCATION" to "Background Location",
        
        // Camera
        "android.permission.CAMERA" to "Camera",
        
        // Microphone
        "android.permission.RECORD_AUDIO" to "Microphone",
        
        // Contacts
        "android.permission.READ_CONTACTS" to "Read Contacts",
        "android.permission.WRITE_CONTACTS" to "Write Contacts",
        "android.permission.GET_ACCOUNTS" to "Get Accounts",
        
        // Phone
        "android.permission.READ_PHONE_STATE" to "Phone State",
        "android.permission.READ_PHONE_NUMBERS" to "Phone Numbers",
        "android.permission.CALL_PHONE" to "Make Calls",
        "android.permission.READ_CALL_LOG" to "Call Log",
        "android.permission.WRITE_CALL_LOG" to "Write Call Log",
        "android.permission.PROCESS_OUTGOING_CALLS" to "Outgoing Calls",
        
        // SMS
        "android.permission.SEND_SMS" to "Send SMS",
        "android.permission.RECEIVE_SMS" to "Receive SMS",
        "android.permission.READ_SMS" to "Read SMS",
        "android.permission.RECEIVE_MMS" to "Receive MMS",
        
        // Storage
        "android.permission.READ_EXTERNAL_STORAGE" to "Read Storage",
        "android.permission.WRITE_EXTERNAL_STORAGE" to "Write Storage",
        "android.permission.READ_MEDIA_IMAGES" to "Read Images",
        "android.permission.READ_MEDIA_VIDEO" to "Read Videos",
        "android.permission.READ_MEDIA_AUDIO" to "Read Audio",
        
        // Calendar
        "android.permission.READ_CALENDAR" to "Read Calendar",
        "android.permission.WRITE_CALENDAR" to "Write Calendar",
        
        // Body Sensors
        "android.permission.BODY_SENSORS" to "Body Sensors",
        "android.permission.BODY_SENSORS_BACKGROUND" to "Background Sensors",
        
        // Activity Recognition
        "android.permission.ACTIVITY_RECOGNITION" to "Activity Recognition",
        
        // Bluetooth
        "android.permission.BLUETOOTH_CONNECT" to "Bluetooth Connect",
        "android.permission.BLUETOOTH_SCAN" to "Bluetooth Scan",
        
        // Nearby Devices
        "android.permission.NEARBY_WIFI_DEVICES" to "Nearby WiFi"
    )
    
    /**
     * Detailed icons for common permission types.
     */
    val categoryEmojis = mapOf(
        "Location" to "📍",
        "Camera" to "📷",
        "Microphone" to "🎤",
        "Contacts / Social Data" to "👥",
        "Phone / Call Information" to "📞",
        "Messages / SMS" to "💬",
        "Storage / Media" to "📁",
        "Calendar" to "📅",
        "Sensors / Activity" to "💓",
        "Network / Internet" to "🌐",
        "Nearby Devices / Bluetooth" to "📡",
        "System" to "⚙️",
        "Other" to "🔐"
    )
    
    /**
     * Get functional category for any Android permission.
     */
    fun getFunctionalCategory(permissionName: String): String {
        return when {
            permissionName.contains("LOCATION") -> "Location"
            permissionName.contains("CAMERA") -> "Camera"
            permissionName.contains("RECORD_AUDIO") || permissionName.contains("MICROPHONE") -> "Microphone"
            permissionName.contains("CONTACT") || permissionName.contains("ACCOUNT") -> "Contacts / Social Data"
            permissionName.contains("PHONE") || permissionName.contains("CALL_LOG") || 
            permissionName.contains("PROCESS_OUTGOING") || permissionName.contains("CALL_PHONE") -> "Phone / Call Information"
            permissionName.contains("SMS") || permissionName.contains("MMS") -> "Messages / SMS"
            permissionName.contains("STORAGE") || permissionName.contains("MEDIA") -> "Storage / Media"
            permissionName.contains("CALENDAR") -> "Calendar"
            permissionName.contains("SENSOR") || permissionName.contains("ACTIVITY_RECOGNITION") -> "Sensors / Activity"
            permissionName.contains("BLUETOOTH") || permissionName.contains("NEARBY_WIFI") -> "Nearby Devices / Bluetooth"
            permissionName.contains("INTERNET") || permissionName.contains("NETWORK") || 
            permissionName.contains("WIFI") -> "Network / Internet"
            permissionName.contains("SYSTEM") || permissionName.contains("SERVICE") || 
            permissionName.contains("BOOT") || permissionName.contains("WINDOW") -> "System"
            else -> "Other"
        }
    }

    /**
     * Get emoji for a category.
     */
    fun getCategoryEmoji(category: String): String {
        return categoryEmojis[category] ?: "🔐"
    }

    /**
     * Get icon for a permission based on its functional category.
     */
    fun getIcon(permissionName: String): String {
        val category = getFunctionalCategory(permissionName)
        return getCategoryEmoji(category)
    }
    
    /**
     * Check if a permission is considered sensitive/dangerous.
     */
    fun isSensitive(permissionName: String): Boolean {
        return sensitivePermissions.containsKey(permissionName)
    }
    
    /**
     * Get a user-friendly name for a permission.
     */
    fun getFriendlyName(permissionName: String): String {
        return sensitivePermissions[permissionName] 
            ?: permissionName.substringAfterLast('.').replace('_', ' ')
                .lowercase()
                .replaceFirstChar { it.uppercase() }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Permission Necessity Classification
// ════════════════════════════════════════════════════════════════════════════

/**
 * Indicates whether a permission is essential for core app functionality
 * or an optional add-on that may increase privacy risk.
 *
 * Classification is purely heuristic / rule-based and happens fully on-device.
 */
enum class PermissionUsageRole(
    val displayName: String,
    val colorHex: Long
) {
    /**
     * The app's advertised core feature cannot work without this permission.
     * Example: a camera app requesting CAMERA.
     */
    PREREQUISITE("Prerequisite", 0xFF1565C0),  // Deep blue

    /**
     * The permission is not required for the core feature but adds capabilities
     * that may create additional privacy risk for the user.
     * Example: a calculator requesting CONTACTS.
     */
    OPTIONAL("Optional", 0xFFF57F17)            // Amber / dark-yellow
}

/**
 * Rule-based classifier that assigns a [PermissionUsageRole] to a permission.
 *
 * Strategy:
 *  1. Normal / Signature permissions → PREREQUISITE (auto-granted, infra-level).
 *  2. Dangerous permissions on the [prerequisitePermissions] allowlist → PREREQUISITE.
 *  3. Everything else → OPTIONAL.
 *
 * The allowlist covers permissions that have a single, unambiguous, universally
 * accepted use-case (e.g., READ_PHONE_STATE for call/dialler functionality).
 */
object PermissionClassifier {

    /**
     * Dangerous permissions commonly considered core infrastructure.
     * These are granted Prerequisite status regardless of the app's category
     * because their use-case is narrow and well-documented.
     */
    private val prerequisitePermissions: Set<String> = setOf(
        // Telephony / dialler apps must identify phone state
        "android.permission.READ_PHONE_STATE",
        // Storage: required for most multimedia and productivity apps
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.READ_MEDIA_IMAGES",
        "android.permission.READ_MEDIA_VIDEO",
        "android.permission.READ_MEDIA_AUDIO",
        // Camera: apps whose primary purpose is photography / video
        "android.permission.CAMERA",
        // Microphone: apps whose primary purpose is audio recording / VOIP
        "android.permission.RECORD_AUDIO",
        // Location: navigation and map apps
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        // Contacts: dialler, messaging, social apps
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        // Bluetooth: device connectivity apps (earbuds, IoT, etc.)
        "android.permission.BLUETOOTH_CONNECT",
        "android.permission.BLUETOOTH_SCAN"
    )

    /**
     * Classify a permission as Prerequisite or Optional.
     *
     * @param permissionName  Full Android permission name.
     * @param category        The [PermissionCategory] already computed for this permission.
     */
    fun classify(permissionName: String, category: PermissionCategory): PermissionUsageRole {
        // Normal and Signature permissions are auto-granted by the OS and
        // represent infrastructure-level access — always Prerequisite.
        if (category == PermissionCategory.NORMAL || category == PermissionCategory.SIGNATURE) {
            return PermissionUsageRole.PREREQUISITE
        }
        // Dangerous permissions on the explicit allowlist are Prerequisite.
        if (permissionName in prerequisitePermissions) {
            return PermissionUsageRole.PREREQUISITE
        }
        // Background location, SMS, call log, sensors, activity recognition, etc.
        // are considered Optional because they go beyond standard feature needs.
        return PermissionUsageRole.OPTIONAL
    }

    /**
     * Return a brief, user-friendly explanation suitable for display in the UI.
     *
     * @param permissionName  Full Android permission name.
     * @param role            The [PermissionUsageRole] already classified.
     */
    fun getReason(permissionName: String, role: PermissionUsageRole): String {
        if (role == PermissionUsageRole.PREREQUISITE) {
            return when {
                permissionName.contains("CAMERA")         -> "Core feature: needed to take photos or record video."
                permissionName.contains("RECORD_AUDIO")   -> "Core feature: needed for voice calls or audio recording."
                permissionName.contains("LOCATION")       -> "Core feature: needed for navigation or location-based services."
                permissionName.contains("CONTACTS")       -> "Core feature: needed to read or sync your contacts."
                permissionName.contains("STORAGE")
                || permissionName.contains("MEDIA")       -> "Core feature: needed to read or save files on this device."
                permissionName.contains("BLUETOOTH")      -> "Core feature: needed to connect to Bluetooth devices."
                permissionName.contains("PHONE")          -> "Core feature: needed to manage calls and device identity."
                permissionName.contains("SMS")            -> "Core feature: needed for messaging and verification."
                permissionName.contains("INTERNET")       -> "Core feature: needed for online services and sync."
                permissionName.contains("NETWORK")        -> "Core feature: needed for connectivity monitoring."
                else                                       -> "Required for the app's primary functionality."
            }
        }
        // Optional reasons
        return when {
            permissionName.contains("BACKGROUND_LOCATION") ->
                "Optional: tracks your location even when the app is closed. High privacy risk."
            permissionName.contains("SMS")
            || permissionName.contains("RECEIVE_SMS")
            || permissionName.contains("SEND_SMS")          ->
                "Optional: access to your text messages. Not needed for most apps."
            permissionName.contains("READ_CALL_LOG")
            || permissionName.contains("WRITE_CALL_LOG")   ->
                "Optional: access to your call history. Rarely required."
            permissionName.contains("CALENDAR")            ->
                "Optional: access to your calendar events."
            permissionName.contains("SENSOR")              ->
                "Optional: access to body sensor data (heart rate, etc.)."
            permissionName.contains("ACTIVITY_RECOGNITION") ->
                "Optional: monitors your physical activity (walking, running, etc.)."
            permissionName.contains("PROCESS_OUTGOING")    ->
                "Optional: can intercept and redirect outgoing calls."
            permissionName.contains("GET_ACCOUNTS")
            || permissionName.contains("ACCOUNTS")         ->
                "Optional: lists accounts stored on this device."
            permissionName.contains("CALL_PHONE")          ->
                "Optional: can initiate phone calls directly without confirmation."
            permissionName.contains("WIFI") || permissionName.contains("NETWORK") ->
                "Optional: details about your network connection could be used for tracking."
            permissionName.contains("WINDOW") || permissionName.contains("SYSTEM") ->
                "Optional: advanced system-level access often used for persistent functionality."
            else                                            ->
                "Optional: not clearly required for core functionality. Review with caution."
        }
    }
}

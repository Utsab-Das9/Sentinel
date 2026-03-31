package com.privacynudge.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.privacynudge.data.*
import com.privacynudge.notification.NotificationHelper
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * VPN Service for monitoring DNS queries from a selected app.
 * Enhanced to use NudgeEngine for intelligent privacy alerts.
 */
class PrivacyNudgeVpnService : VpnService() {

    companion object {
        private const val TAG = "PrivacyNudgeVPN"

        const val ACTION_START = "com.privacynudge.vpn.START"
        const val ACTION_STOP = "com.privacynudge.vpn.STOP"

        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_APP_NAME = "app_name"

        private const val VPN_ADDRESS = "10.0.0.2"
        private const val VPN_ROUTE = "0.0.0.0"
        private const val VPN_DNS = "1.1.1.1"
        private const val VPN_MTU = 1500

        // Callback to notify the UI of state changes
        var onStateChanged: ((Boolean) -> Unit)? = null
        var onDomainDetected: ((NudgeEvent) -> Unit)? = null
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var notificationHelper: NotificationHelper? = null
    private var permissionInspector: PermissionInspector? = null
    private var appUsageTracker: AppUsageTracker? = null
    private var nudgeEngine: NudgeEngine? = null

    private var monitoredPackage: String? = null
    private var monitoredAppName: String? = null
    private var blockedPackages: Set<String> = emptySet()

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var settingsRepository: SettingsRepository
    private var packetReaderJob: Job? = null

    // Track domains we've already alerted on to avoid spam
    private val alertedDomains = mutableSetOf<String>()

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == NotificationHelper.ACTION_STOP_VPN) {
                stopVpn()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        permissionInspector = PermissionInspector(this)
        appUsageTracker = AppUsageTracker(this)
        settingsRepository = SettingsRepository(this)
        nudgeEngine = NudgeEngine(
            context = this,
            permissionInspector = permissionInspector!!,
            appUsageTracker = appUsageTracker!!,
            notificationHelper = notificationHelper!!,
            settingsRepository = settingsRepository
        )

        // Listen for blocked packages changes to restart VPN if needed
        serviceScope.launch {
            settingsRepository.blockedPackages.collect { packages ->
                if (blockedPackages != packages && vpnInterface != null) {
                    Log.d(TAG, "Blocked packages changed, re-establishing VPN interface")
                    blockedPackages = packages
                    reestablishVpnInterface()
                } else {
                    blockedPackages = packages
                }
            }
        }

        // Register receiver for stop action
        val filter = IntentFilter(NotificationHelper.ACTION_STOP_VPN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stopReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stopReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
                val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "Unknown App"

                startVpn(packageName, appName ?: "Live Awareness")
            }
            ACTION_STOP -> {
                stopVpn()
            }
        }
        return START_STICKY
    }

    private fun startVpn(packageName: String?, appName: String?) {
        if (vpnInterface != null) {
            Log.d(TAG, "VPN already running")
            return
        }

        monitoredPackage = packageName
        monitoredAppName = appName
        alertedDomains.clear() // Reset alerted domains for new session

        try {
            // Create VPN interface
            val builder = Builder()
                .setSession("Sentinel")
                .addAddress(VPN_ADDRESS, 32)
                .addDnsServer(VPN_DNS)
                .addRoute(VPN_ROUTE, 0)
                .setMtu(VPN_MTU)
                .setBlocking(true)

            // Add monitored app if provided
            monitoredPackage?.let { 
                builder.addAllowedApplication(it)
            }

            // Add all blocked apps
            blockedPackages.forEach { pkg ->
                if (pkg != monitoredPackage) {
                    try {
                        builder.addAllowedApplication(pkg)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to add blocked app: $pkg", e)
                    }
                }
            }

            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN interface")
                onStateChanged?.invoke(false)
                return
            }

            Log.d(TAG, "VPN established. Monitoring: ${monitoredPackage ?: "None (Enforcement Only)"}, Blocking: ${blockedPackages.size} apps")

            // Start foreground service
            val notification = notificationHelper?.createVpnNotification(monitoredAppName ?: "Privacy Enforcement")
            if (notification != null) {
                startForeground(NotificationHelper.VPN_NOTIFICATION_ID, notification)
            }

            // Start packet reading
            startPacketReader()

            onStateChanged?.invoke(true)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN", e)
            onStateChanged?.invoke(false)
            stopSelf()
        }
    }

    private fun startPacketReader() {
        val vpnFd = vpnInterface ?: return

        packetReaderJob = serviceScope.launch {
            val inputStream = FileInputStream(vpnFd.fileDescriptor)
            val outputStream = FileOutputStream(vpnFd.fileDescriptor)
            val buffer = ByteBuffer.allocate(VPN_MTU)

            try {
                while (isActive) {
                    buffer.clear()
                    val length = inputStream.read(buffer.array())

                    if (length > 0) {
                        buffer.limit(length)
                        handlePacket(buffer.array().copyOf(length), outputStream)
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.e(TAG, "Packet reader error", e)
                }
            } finally {
                inputStream.close()
                outputStream.close()
            }
        }
    }

    private suspend fun handlePacket(packet: ByteArray, outputStream: FileOutputStream) {
        // Find packet source (UID) if possible to decide whether to drop or forward
        // For POC: In this implementation, we mostly use VPN for BLOCKING.
        // If an app is in the VPN and there's a default route, and we don't forward, it's blocked.
        
        // We need to identify if the packet is from the 'monitoredPackage' or a 'blockedPackage'
        // For now, since we don't have a NAT engine, we will ONLY process DNS for the monitored app
        // and let other packets drop (effectively blocking).
        
        // Check if this is a DNS query
        if (DnsPacketParser.isDnsQueryPacket(packet)) {
            val dnsPayload = DnsPacketParser.extractDnsPayload(packet)
            if (dnsPayload != null) {
                val domain = DnsPacketParser.extractQueryDomain(dnsPayload)
                if (domain != null && domain.isNotEmpty()) {
                    processDnsQuery(domain)
                }
            }
        }

        // To support "uninterrupted" monitoring, we would need to forward packets.
        // Without a NAT engine, everything in allowedApplications is currently blocked.
        // The requirement "strictly block internet access for all high-risk apps" is satisfied
        // by adding them to addAllowedApplication and NOT forwarding.
    }

    private suspend fun processDnsQuery(domain: String) {
        Log.d(TAG, "DNS query detected: $domain")

        val packageName = monitoredPackage ?: return
        val appName = monitoredAppName ?: "Unknown App"

        // Skip if we've already alerted on this domain in this session
        if (domain in alertedDomains) {
            return
        }

        // Use NudgeEngine for intelligent evaluation
        val event = nudgeEngine?.evaluateDnsActivity(packageName, domain)
        
        if (event != null) {
            alertedDomains.add(domain)
            Log.d(TAG, "Nudge generated for domain: $domain")

            // Notify UI
            withContext(Dispatchers.Main) {
                onDomainDetected?.invoke(event)
            }
        } else {
            // Fallback to legacy behavior for non-target apps
            legacyDnsCheck(packageName, appName, domain)
        }
    }

    /**
     * Legacy DNS checking for apps that aren't target apps (Facebook, LinkedIn, WhatsApp)
     */
    private suspend fun legacyDnsCheck(packageName: String, appName: String, domain: String) {
        val domainPattern = SensitiveDomains.matchDomain(domain) ?: return
        
        Log.d(TAG, "Sensitive domain detected: $domain (${domainPattern.category})")

        val locationStatus = permissionInspector?.checkLocationPermission(packageName)

        if (locationStatus == LocationPermissionStatus.GRANTED) {
            val event = NudgeEvent(
                appName = appName,
                packageName = packageName,
                domain = domain,
                permissionType = "Location",
                reason = "${domainPattern.description} contacted while app has Location permission"
            )

            alertedDomains.add(domain)

            // Notify UI
            withContext(Dispatchers.Main) {
                onDomainDetected?.invoke(event)
            }

            // Send notification
            notificationHelper?.sendNudgeNotification(
                appName = appName,
                domain = domain,
                permissionType = "Location"
            )
        }
    }

    private fun stopVpn() {
        Log.d(TAG, "Stopping VPN")

        packetReaderJob?.cancel()
        packetReaderJob = null

        vpnInterface?.close()
        vpnInterface = null

        monitoredPackage = null
        monitoredAppName = null
        alertedDomains.clear()

        onStateChanged?.invoke(false)

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun reestablishVpnInterface() {
        val packageName = monitoredPackage ?: return
        val appName = monitoredAppName ?: "Unknown App"
        
        // Cancel current reader
        packetReaderJob?.cancel()
        packetReaderJob = null
        
        // Establish new interface
        if (monitoredPackage == null && blockedPackages.isEmpty()) {
            Log.d(TAG, "No apps to monitor or block. Stopping VPN.")
            stopVpn()
            return
        }

        try {
            val builder = Builder()
                .setSession("Sentinel")
                .addAddress(VPN_ADDRESS, 32)
                .addDnsServer(VPN_DNS)
                .addRoute(VPN_ROUTE, 0)
                .setMtu(VPN_MTU)
                .setBlocking(true)

            // Add monitored app if provided
            monitoredPackage?.let { builder.addAllowedApplication(it) }

            blockedPackages.forEach { pkg ->
                if (pkg != monitoredPackage) {
                    try {
                        builder.addAllowedApplication(pkg)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to add blocked app: $pkg", e)
                    }
                }
            }

            val newInterface = builder.establish()
            if (newInterface != null) {
                vpnInterface?.close()
                vpnInterface = newInterface
                startPacketReader()
                Log.d(TAG, "VPN interface re-established")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to re-establish VPN", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(stopReceiver)
        } catch (e: Exception) {
            // Receiver may not be registered
        }
        serviceScope.cancel()
        stopVpn()
    }

    override fun onRevoke() {
        // Called when the user revokes VPN permission
        stopVpn()
        super.onRevoke()
    }
}

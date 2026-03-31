package com.privacynudge.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log

/**
 * BroadcastReceiver for detecting app install, uninstall, and update events.
 */
class PackageChangeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PackageChangeReceiver"

        /**
         * Create an IntentFilter for package change events.
         */
        fun createIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addDataScheme("package")
            }
        }
    }

    /**
     * Callback for when a new package is installed.
     */
    var onPackageAdded: ((String) -> Unit)? = null

    /**
     * Callback for when a package is uninstalled.
     */
    var onPackageRemoved: ((String) -> Unit)? = null

    /**
     * Callback for when a package is updated.
     */
    var onPackageReplaced: ((String) -> Unit)? = null

    /**
     * Callback for when a package is changed.
     */
    var onPackageChanged: ((String) -> Unit)? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return

        val packageName = intent.data?.schemeSpecificPart ?: return
        val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)

        Log.d(TAG, "Package event: ${intent.action} for $packageName (replacing=$isReplacing)")

        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                if (isReplacing) {
                    // This is an update, not a new install
                    onPackageReplaced?.invoke(packageName)
                } else {
                    onPackageAdded?.invoke(packageName)
                }
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                if (!isReplacing) {
                    // Only trigger if not being replaced (uninstall, not update)
                    onPackageRemoved?.invoke(packageName)
                }
            }
            Intent.ACTION_PACKAGE_REPLACED -> {
                onPackageReplaced?.invoke(packageName)
            }
            Intent.ACTION_PACKAGE_CHANGED -> {
                onPackageChanged?.invoke(packageName)
            }
        }
    }
}

/**
 * Helper class to manage package change listener lifecycle.
 */
class PackageChangeManager(private val context: Context) {

    private var receiver: PackageChangeReceiver? = null

    /**
     * Start listening for package changes.
     */
    fun startListening(
        onAdded: ((String) -> Unit)? = null,
        onRemoved: ((String) -> Unit)? = null,
        onUpdated: ((String) -> Unit)? = null
    ) {
        stopListening()

        receiver = PackageChangeReceiver().apply {
            onPackageAdded = onAdded
            onPackageRemoved = onRemoved
            onPackageReplaced = onUpdated
        }

        val filter = PackageChangeReceiver.createIntentFilter()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
    }

    /**
     * Stop listening for package changes.
     */
    fun stopListening() {
        receiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                // Receiver not registered
            }
        }
        receiver = null
    }
}

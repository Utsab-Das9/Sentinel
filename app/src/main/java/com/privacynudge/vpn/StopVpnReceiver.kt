package com.privacynudge.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receiver to handle stop VPN action from notification.
 */
class StopVpnReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        val stopIntent = Intent(context, PrivacyNudgeVpnService::class.java).apply {
            action = PrivacyNudgeVpnService.ACTION_STOP
        }
        context.startService(stopIntent)
    }
}

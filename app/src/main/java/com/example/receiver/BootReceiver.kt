package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.service.AmmuBackgroundVoiceService
import com.example.service.SecurityShieldManager

/**
 * BootReceiver
 * Automatically restarts Ammu Background Voice Assistant on phone startup
 * so Commander can say "Hey Ammu" without opening the app, just like Google Assistant.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent?.action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            if (SecurityShieldManager.isBootAutostartEnabled(context)) {
                try {
                    AmmuBackgroundVoiceService.startService(context)
                } catch (_: Exception) {}
            }
        }
    }
}

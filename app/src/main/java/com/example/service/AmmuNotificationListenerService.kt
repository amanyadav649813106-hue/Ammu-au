package com.example.service

import android.app.Notification
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AmmuNotificationListenerService
 * Listens to incoming chat notifications from Instagram, WhatsApp, Telegram, Messages.
 * Announces: "कमांडर, आपके दोस्त [आकाश] ने [इंस्टाग्राम/व्हाट्सएप] पर मैसेज भेजा है: '[हाय]'"
 * And supports touchless instant auto-reply via RemoteInput when Commander says:
 * "उसको मैसेज भेजो [hmm]"
 */
class AmmuNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "AmmuNotifService"

        private var instance: AmmuNotificationListenerService? = null

        private val _lastNotification = MutableStateFlow("कोई नया मैसेज नहीं आया है कमांडर!")
        val lastNotification: StateFlow<String> = _lastNotification.asStateFlow()

        private val _lastSender = MutableStateFlow("")
        val lastSender: StateFlow<String> = _lastSender.asStateFlow()

        private val _lastApp = MutableStateFlow("")
        val lastApp: StateFlow<String> = _lastApp.asStateFlow()

        private var cachedReplyAction: Notification.Action? = null
        private var lastAnnouncedTime = 0L
        private var lastAnnouncedMsg = ""

        fun isPermissionGranted(context: Context): Boolean {
            val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
            return enabledListeners.contains(context.packageName)
        }

        fun openNotificationAccessSettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open notification listener settings", e)
            }
        }

        fun replyToLastNotification(context: Context, replyText: String): Pair<Boolean, String> {
            val action = cachedReplyAction
            val sender = _lastSender.value.ifBlank { "दोस्त" }
            val app = _lastApp.value.ifBlank { "मैसेजिंग ऐप" }

            if (action == null) {
                return Pair(false, "कमांडर, रिप्लाई करने के लिए कोई सक्रिय नोटिफिकेशन नहीं मिली है।")
            }

            try {
                val remoteInputs = action.remoteInputs
                if (remoteInputs.isNullOrEmpty()) {
                    return Pair(false, "कमांडर, $app के इस मैसेज में डायरेक्ट रिप्लाई सपोर्ट नहीं है।")
                }

                val intent = Intent()
                val bundle = Bundle()
                for (remoteInput in remoteInputs) {
                    bundle.putCharSequence(remoteInput.resultKey, replyText)
                }
                RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)

                action.actionIntent.send(context, 0, intent)
                Log.i(TAG, "Sent reply '$replyText' to $sender on $app")
                return Pair(true, "जी कमांडर, मैंने आपके दोस्त $sender को $app पर '$replyText' का रिप्लाई भेज दिया है!")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send auto-reply", e)
                return Pair(false, "कमांडर, रिप्लाई भेजने में तकनीकी समस्या आई: ${e.message}")
            }
        }

        fun getLatestNotificationSummary(): String {
            val msg = _lastNotification.value
            val sender = _lastSender.value
            val app = _lastApp.value
            return if (sender.isNotBlank()) {
                "कमांडर, आखिरी मैसेज $sender का $app पर आया था: \"$msg\""
            } else {
                "कमांडर, अभी तक कोई नया नोटिफिकेशन मैसेज नहीं आया है।"
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Log.i(TAG, "Ammu Notification Listener Connected successfully")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        if (pkg == packageName) return // Ignore own notifications

        // Supported communication & social apps
        val appFriendlyName = when {
            pkg.contains("instagram") -> "इंस्टाग्राम"
            pkg.contains("whatsapp") -> "व्हाट्सएप"
            pkg.contains("telegram") -> "टेलीग्राम"
            pkg.contains("messaging") || pkg.contains("mms") -> "मैसेज"
            pkg.contains("facebook.orca") -> "मैसेंजर"
            pkg.contains("twitter") || pkg.contains("x.android") -> "X (ट्विटर)"
            else -> return // Only announce personal social & chat notifications
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getString(Notification.EXTRA_TITLE)
            ?: extras.getString(Notification.EXTRA_CONVERSATION_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: "कोई"

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: ""

        if (text.isBlank() || title.contains("WhatsApp", ignoreCase = true) && text.contains("messages", ignoreCase = true)) {
            return
        }

        // Deduplicate messages within 4 seconds
        val now = System.currentTimeMillis()
        if (title == _lastSender.value && text == lastAnnouncedMsg && (now - lastAnnouncedTime) < 4000) {
            return
        }
        lastAnnouncedTime = now
        lastAnnouncedMsg = text

        // Save reply action if available
        val actions = notification.actions
        var foundReplyAction: Notification.Action? = null
        if (actions != null) {
            for (action in actions) {
                if (!action.remoteInputs.isNullOrEmpty()) {
                    foundReplyAction = action
                    break
                }
            }
        }
        if (foundReplyAction != null) {
            cachedReplyAction = foundReplyAction
        }

        _lastSender.value = title
        _lastApp.value = appFriendlyName
        _lastNotification.value = text

        // Announce aloud warmly:
        // "कमांडर आपके दोस्त [आकाश] ने [इंस्टाग्राम] पर मैसेज भेजा है: '[हाय]'"
        val announcement = "कमांडर! आपके दोस्त $title ने $appFriendlyName पर मैसेज भेजा है: \"$text\"।"
        DeviceActionManager.speakDirectly(applicationContext, announcement)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}
}

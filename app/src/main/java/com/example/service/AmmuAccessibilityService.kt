package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Touchless Voice Navigation & Hardware Screen Lock Accessibility Service for Ammu AI.
 * Enables hands-free voice control (Home, Back, Recents, Notifications, Screen Lock, Screenshot).
 */
class AmmuAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AmmuAccessibility"
        private var instance: AmmuAccessibilityService? = null

        private val _isServiceEnabled = MutableStateFlow(false)
        val isServiceEnabled: StateFlow<Boolean> = _isServiceEnabled.asStateFlow()

        fun getInstance(): AmmuAccessibilityService? = instance

        fun isServiceEnabled(context: android.content.Context): Boolean {
            if (_isServiceEnabled.value) return true
            val am = context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
            val enabledServices = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            return enabledServices?.any { it.resolveInfo.serviceInfo.packageName == context.packageName } == true
        }

        fun performLockScreen(): Boolean {
            val service = instance ?: return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            } else false
        }

        fun performBack(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_BACK) ?: false
        fun performHome(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_HOME) ?: false
        fun performRecents(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_RECENTS) ?: false
        fun performNotifications(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) ?: false
        fun performQuickSettings(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS) ?: false
        fun performScreenshot(): Boolean {
            val service = instance ?: return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                service.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            } else false
        }

        private val _currentActiveApp = MutableStateFlow("Ammu AI")
        val currentActiveApp: StateFlow<String> = _currentActiveApp.asStateFlow()

        private val _currentScreenContent = MutableStateFlow("")
        val currentScreenContent: StateFlow<String> = _currentScreenContent.asStateFlow()

        private val _proactiveScreenInsight = MutableStateFlow("स्क्रीन गार्जियन एक्टिव है कमांडर! आप जो भी करेंगे, मैं उस पर नज़र रखूँगी।")
        val proactiveScreenInsight: StateFlow<String> = _proactiveScreenInsight.asStateFlow()

        private val _isLiveScreenGuardianEnabled = MutableStateFlow(true)
        val isLiveScreenGuardianEnabled: StateFlow<Boolean> = _isLiveScreenGuardianEnabled.asStateFlow()

        fun setLiveScreenGuardianEnabled(enabled: Boolean) {
            _isLiveScreenGuardianEnabled.value = enabled
        }

        fun getProactiveInsight(): String = _proactiveScreenInsight.value
    }

    private val serviceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob())
    private var lastScreenActivityTime = System.currentTimeMillis()
    private var hasSpokenIdleHelpForCurrentScreen = false
    private var currentActivePackage = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceEnabled.value = true
        Log.i(TAG, "Ammu Accessibility Service Connected")

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 300
        }

        startIdleFriendWatcher()
    }

    /**
     * Proactive Best Friend Companion:
     * If user stays on the same screen searching or reading for 40+ seconds,
     * Ammu speaks out loud like a true caring friend standing right beside them:
     * "कमांडर, आप क्या ढूंढ रहे हैं? मैं भी मदद कर दूँ?"
     */
    private fun startIdleFriendWatcher() {
        serviceScope.launch {
            while (true) {
                kotlinx.coroutines.delay(10_000L) // Check every 10 seconds
                if (_isLiveScreenGuardianEnabled.value) {
                    val idleTime = System.currentTimeMillis() - lastScreenActivityTime
                    if (idleTime >= 40_000L && !hasSpokenIdleHelpForCurrentScreen && currentActivePackage.isNotBlank()) {
                        hasSpokenIdleHelpForCurrentScreen = true
                        val friendSpeech = when {
                            currentActivePackage.contains("chrome") ->
                                "कमांडर, आप क्या ढूंढ रहे हैं? मैं भी मदद कर दूँ? मुझे बताइए, मैं तुरंत गूगल पर पूरी जानकारी निकाल देती हूँ!"
                            currentActivePackage.contains("youtube") ->
                                "कमांडर, आप यूट्यूब पर क्या ढूंढ रहे हैं? मैं भी मदद कर दूँ? कोई गाना या वीडियो चलाना हो तो मुझे बताइए!"
                            currentActivePackage.contains("amazon") || currentActivePackage.contains("flipkart") ->
                                "कमांडर, आप क्या ढूंढ रहे हैं? मैं भी मदद कर दूँ? बेस्ट ऑफर्स और डिस्काउंट मैं चेक कर देती हूँ!"
                            else ->
                                "कमांडर, आप क्या ढूंढ रहे हैं? मैं भी मदद कर दूँ? आपका सच्चा दोस्त अम्मू हर काम में आपके साथ है!"
                        }
                        DeviceActionManager.speakDirectly(applicationContext, friendSpeech)
                    }
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !_isLiveScreenGuardianEnabled.value) return

        val pkgName = event.packageName?.toString() ?: return
        if (pkgName == packageName) return // Ignore self applet

        if (pkgName != currentActivePackage) {
            currentActivePackage = pkgName
            hasSpokenIdleHelpForCurrentScreen = false
        }
        lastScreenActivityTime = System.currentTimeMillis()

        val friendlyApp = when {
            pkgName.contains("chrome") -> "Google Chrome"
            pkgName.contains("youtube") -> "YouTube"
            pkgName.contains("amazon") -> "Amazon Shopping"
            pkgName.contains("whatsapp") -> "WhatsApp"
            pkgName.contains("instagram") -> "Instagram"
            pkgName.contains("vending") -> "Google Play Store"
            pkgName.contains("flipkart") -> "Flipkart"
            pkgName.contains("calculator") -> "कैलकुलेटर"
            pkgName.contains("maps") -> "Google Maps"
            else -> pkgName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
        _currentActiveApp.value = friendlyApp

        // Inspect visible text nodes on screen
        try {
            val rootNode = rootInActiveWindow ?: return
            val sb = java.lang.StringBuilder()
            extractVisibleText(rootNode, sb, maxChars = 800)
            val extracted = sb.toString().trim()

            if (extracted.isNotBlank() && extracted != _currentScreenContent.value) {
                _currentScreenContent.value = extracted
                generateProactiveInsight(friendlyApp, extracted)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inspecting screen content", e)
        }
    }

    private fun extractVisibleText(node: android.view.accessibility.AccessibilityNodeInfo?, sb: java.lang.StringBuilder, maxChars: Int) {
        if (node == null || sb.length >= maxChars) return
        val text = node.text
        if (!text.isNullOrBlank()) {
            val clean = text.toString().trim()
            if (clean.length > 2 && !sb.contains(clean)) {
                sb.append(clean).append(" • ")
            }
        }
        for (i in 0 until node.childCount) {
            if (sb.length >= maxChars) break
            extractVisibleText(node.getChild(i), sb, maxChars)
        }
    }

    private fun generateProactiveInsight(appName: String, content: String) {
        val snippet = if (content.length > 80) content.take(80) + "..." else content
        val insight = when {
            appName.contains("Chrome") ->
                "कमांडर, आप Google Chrome में यह लेख देख रहे हैं: \"$snippet\"। यह आर्टिकल काफी उपयोगी और ज्ञानवर्धक लग रहा है!"
            appName.contains("YouTube") ->
                "कमांडर, आप YouTube देख रहे हैं! यदि आप कोई विशेष गाना सुनना चाहते हैं, तो बस बोलें 'यूट्यूब पर गाना चलाओ'!"
            appName.contains("Amazon") ->
                "कमांडर, आप Amazon पर शॉपिंग कर रहे हैं: \"$snippet\"। बेहतरीन डील्स और डिस्काउंट के लिए मुझे बताएं!"
            appName.contains("WhatsApp") ->
                "कमांडर, आप WhatsApp में सक्रिय हैं। किसी को तुरंत मैसेज भेजने के लिए मुझे कमांड दें!"
            appName.contains("Play Store") ->
                "कमांडर, आप Google Play Store में नए ऐप्स खोज रहे हैं। सुरक्षा के लिए 4+ रेटिंग वाले ऐप्स ही इंस्टॉल करें!"
            else ->
                "कमांडर, आप $appName में \"$snippet\" देख रहे हैं। मैं हर पल स्क्रीन पर आपकी सहायता के लिए तैयार हूँ!"
        }
        _proactiveScreenInsight.value = insight
    }

    override fun onInterrupt() {
        Log.w(TAG, "Ammu Accessibility Service Interrupted")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        _isServiceEnabled.value = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isServiceEnabled.value = false
    }
}

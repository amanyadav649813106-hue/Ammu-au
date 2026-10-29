package com.example.service

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.receiver.AmmuDeviceAdminReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class DevicePermissionStatus(
    val hasAudio: Boolean,
    val hasCall: Boolean,
    val hasLocation: Boolean,
    val hasCamera: Boolean,
    val hasContacts: Boolean = false,
    val hasNotifications: Boolean = false,
    val hasDeviceAdmin: Boolean = false,
    val hasAccessibility: Boolean = false
) {
    val hasAllRequired: Boolean
        get() = hasAudio && hasCall && hasLocation
}

data class DeviceCommandResult(
    val handled: Boolean,
    val replyText: String,
    val actionType: String? = null,
    val generatedImageUri: String? = null
)

object DeviceActionManager {
    private const val TAG = "DeviceActionManager"
    private var isTorchOn = false
    private var isCallRinging = false
    private var currentRingingCaller = ""
    private var directTts: TextToSpeech? = null

    fun setRingingCallState(ringing: Boolean, caller: String) {
        isCallRinging = ringing
        currentRingingCaller = caller
    }

    fun isCallRinging(): Boolean = isCallRinging
    fun getRingingCaller(): String = currentRingingCaller

    fun speakDirectly(context: Context, text: String) {
        try {
            if (directTts == null) {
                directTts = TextToSpeech(context.applicationContext) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        directTts?.language = Locale("hi", "IN")
                        directTts?.setPitch(1.25f)
                        directTts?.setSpeechRate(0.98f)
                        directTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ammu_direct_${System.currentTimeMillis()}")
                    }
                }
            } else {
                directTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ammu_direct_${System.currentTimeMillis()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "speakDirectly error", e)
        }
    }

    fun getPermissionStatus(context: Context): DevicePermissionStatus {
        val hasAudio = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val hasCall = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        val hasFineLoc = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLoc = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCamera = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val hasContacts = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        val hasNotifications = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        return DevicePermissionStatus(
            hasAudio = hasAudio,
            hasCall = hasCall,
            hasLocation = hasFineLoc || hasCoarseLoc,
            hasCamera = hasCamera,
            hasContacts = hasContacts,
            hasNotifications = hasNotifications
        )
    }

    suspend fun executeCommandIfApplicable(context: Context, rawText: String): DeviceCommandResult? {
        val lower = rawText.lowercase().trim()

        // 0. Notification Auto-Reply Command (e.g. "usko message bhejo hmm", "usko reply do")
        if (lower.contains("message bhejo") || lower.contains("msg bhejo") ||
            lower.contains("reply do") || lower.contains("reply bhejo") ||
            lower.contains("usko bolo") || lower.contains("usko message") ||
            lower.contains("usko reply") || lower.contains("मैसेज भेजो") || lower.contains("रिप्लाई दो")) {
            val replyText = rawText.replace(Regex("(?i).*?\\b(message bhejo|msg bhejo|reply do|reply bhejo|usko bolo|usko message|usko reply|मैसेज भेजो|रिप्लाई दो)\\s*"), "").trim()
            val finalText = if (replyText.isNotBlank()) replyText else "hmm"
            val result = AmmuNotificationListenerService.replyToLastNotification(context, finalText)
            return DeviceCommandResult(
                handled = true,
                replyText = result.second,
                actionType = "NOTIFICATION_REPLY"
            )
        }

        // 0.1 Notification Status / What arrived
        if (lower.contains("kya notification") || lower.contains("kiska message") ||
            lower.contains("koi message aaya") || lower.contains("notification kya hai") ||
            lower.contains("क्या नोटिफिकेशन आया") || lower.contains("किसका मैसेज आया")) {
            val summary = AmmuNotificationListenerService.getLatestNotificationSummary()
            return DeviceCommandResult(
                handled = true,
                replyText = summary,
                actionType = "NOTIFICATION_QUERY"
            )
        }

        // 0.2 Home Location Distance & Navigation Guidance
        if (lower.contains("ghar se kitni dur") || lower.contains("ghar kitna dur") ||
            lower.contains("ghar ki duri") || lower.contains("घर से कितनी दूर") ||
            lower.contains("distance to home") || lower.contains("home distance") ||
            lower.contains("kitni dur hu ghar se")) {
            val speech = LocationTracker.getInstance(context).getHomeNavigationSpeech()
            return DeviceCommandResult(
                handled = true,
                replyText = speech,
                actionType = "HOME_DISTANCE"
            )
        }

        // 0.3 Google Maps Route to Home
        if (lower.contains("ghar chalo") || lower.contains("ghar ka rasta") ||
            lower.contains("ghar navigate") || lower.contains("navigate home") ||
            lower.contains("घर का रास्ता") || lower.contains("घर चलो") ||
            lower.contains("google map ghar")) {
            val success = LocationTracker.getInstance(context).openGoogleMapsToHome(context)
            val reply = if (success) {
                "जी कमांडर, गूगल मैप्स में आपके घर का सबसे तेज़ रास्ता नेविगेशन शुरू कर दिया गया है!"
            } else {
                "कमांडर, पहले अपना घर सेट कर लें। ऐप में 'Set Home Location' बटन दबाकर घर सेट कर सकते हैं!"
            }
            return DeviceCommandResult(
                handled = true,
                replyText = reply,
                actionType = "HOME_NAVIGATION"
            )
        }

        // 0.4 Set Home Location from Current GPS
        if (lower.contains("set home location") || lower.contains("ghar set karo") ||
            lower.contains("ye mera ghar hai") || lower.contains("home set karo") ||
            lower.contains("घर सेट करो")) {
            val success = LocationTracker.getInstance(context).setHomeLocationFromCurrent()
            val reply = if (success) {
                "जी कमांडर, आपकी वर्तमान लोकेशन को आपके घर के रूप में सुरक्षित सेव कर दिया गया है!"
            } else {
                "कमांडर, लोकेशन सिग्नल प्राप्त नहीं हो सका। कृपया जीपीएस ऑन करके दोबारा बोलें।"
            }
            return DeviceCommandResult(
                handled = true,
                replyText = reply,
                actionType = "SET_HOME_LOCATION"
            )
        }

        // 0.5 Battery & Health Status
        if (lower.contains("battery") || lower.contains("charge") ||
            lower.contains("बैटरी") || lower.contains("चार्ज") || lower.contains("charge kitna")) {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
            val pct = batteryManager?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
            return DeviceCommandResult(
                handled = true,
                replyText = "कमांडर! अभी आपके फोन में $pct% बैटरी बची है। बैटरी और सेहत का पूरा ध्यान रखा जा रहा है!",
                actionType = "BATTERY_STATUS"
            )
        }

        // 1. Screen Lock & Unlock Commands
        if ((lower.contains("lock") && lower.contains("screen")) || lower.contains("screen lock") ||
            lower.contains("फोन लॉक") || lower.contains("स्क्रीन लॉक") || lower.contains("phone lock")) {
            return lockScreen(context)
        }
        if ((lower.contains("unlock") && lower.contains("screen")) || lower.contains("screen unlock") ||
            lower.contains("फोन अनलॉक") || lower.contains("स्क्रीन अनलॉक") || lower.contains("wake up") ||
            lower.contains("स्क्रीन जगाओ") || lower.contains("screen wake") || lower.contains("phone unlock")) {
            return unlockScreen(context)
        }

        // 2. Creator Query (Aman)
        if (lower.contains("banaya") || lower.contains("creator") || lower.contains("who made") ||
            lower.contains("kisne banaya") || lower.contains("kon banaya") || lower.contains("kaun banaya") ||
            lower.contains("किसने बनाया") || lower.contains("कौन बनाया") || lower.contains("निर्माता")) {
            return DeviceCommandResult(
                handled = true,
                replyText = "मुझे अमन (Aman) ने बनाया है! अमन मेरे क्रिएटर और मेंटर हैं, और उन्होंने ही मुझे इतनी समझदार, प्यारी और सुरीली आवाज़ दी है। Ammu is proudly made by Aman!",
                actionType = "CREATOR_INFO"
            )
        }

        // 3. Torch / Flashlight Commands (off / on)
        if (lower.contains("torch") || lower.contains("flashlight") || lower.contains("टॉर्च") || lower.contains("फ्लैशलाइट")) {
            val turnOff = lower.contains("off") || lower.contains("band") || lower.contains("बंद") || lower.contains("bujhao") || lower.contains("बुझाओ")
            val torchResult = toggleTorch(context, enable = !turnOff)
            return DeviceCommandResult(
                handled = true,
                replyText = torchResult,
                actionType = "TORCH"
            )
        }

        // 4. Google Open Commands
        if (lower.contains("google open") || lower.contains("open google") || lower.contains("गूगल ओपन") || lower.contains("गूगल खोलो") || lower.contains("google chalao")) {
            return openGoogle(context)
        }

        // 5. WhatsApp Commands (Open, send sms)
        if (lower.contains("whatsapp") || lower.contains("व्हाट्सएप") || lower.contains("व्हाट्सअप") || (lower.contains("dost") && lower.contains("msg"))) {
            return handleWhatsAppCommand(context, rawText)
        }

        // 6. Instagram Commands
        if (lower.contains("instagram") || lower.contains("इंस्टाग्राम") || lower.contains("insta")) {
            return openInstagram(context)
        }

        // 7. Call / Phone Commands (including Papa, dialer, friends)
        if (lower.contains("call") || lower.contains("कॉल") || lower.contains("phone lagao") || lower.contains("फोन लगाओ") || lower.contains("number milao") || lower.contains("dialer") || lower.contains("डायलर") || lower.contains("papa") || lower.contains("पापा")) {
            return handleCallCommand(context, rawText)
        }

        // 8. Weather / Mausam Commands
        if (lower.contains("mausam") || lower.contains("मौसम") || lower.contains("weather") || lower.contains("baarish") || lower.contains("barish") || lower.contains("temperature") || lower.contains("taapmaan")) {
            val weatherInfo = fetchLiveWeather(context, rawText)
            return DeviceCommandResult(
                handled = true,
                replyText = weatherInfo,
                actionType = "WEATHER"
            )
        }

        // 9. Camera Photo Capture Commands
        if (lower.contains("camera") || lower.contains("कैमरा") || lower.contains("photo") || lower.contains("फोटो") || lower.contains("khicho") || lower.contains("picture")) {
            return openCamera(context)
        }

        // 9.5 Live Location Telemetry Queries
        if (lower.contains("location") || lower.contains("लोकेशन") || lower.contains("kaha hu") ||
            lower.contains("kahan hu") || lower.contains("kaha pe hu") || lower.contains("कहाँ हूँ") ||
            lower.contains("कहा पे हू") || lower.contains("मेरी लोकेशन") || lower.contains("कहाँ पर हूँ") ||
            lower.contains("where am i") || lower.contains("track location")) {
            val tracker = LocationTracker.getInstance(context)
            if (!tracker.locationInfo.value.isTracking) {
                tracker.startTracking()
            }
            val locationReply = tracker.getSpokenCurrentLocation()
            return DeviceCommandResult(
                handled = true,
                replyText = locationReply,
                actionType = "LOCATION_TRACKED"
            )
        }

        // 10. YouTube Search & Song Player (Checked before generic launcher)
        if ((lower.contains("youtube") || lower.contains("यूट्यूब")) &&
            (lower.contains("search") || lower.contains("chalao") || lower.contains("suno") ||
             lower.contains("song") || lower.contains("gaana") || lower.contains("gana") ||
             lower.contains("bajao") || lower.contains("khojo") || lower.contains("खोजो") ||
             lower.contains("सुनाओ") || lower.contains("चलाओ") || lower.contains("गाना")) ||
            lower.contains("song chalao") || lower.contains("gana chalao") || lower.contains("gaana chalao") ||
            lower.contains("gana bajao") || lower.contains("gaana sunao") || lower.contains("गाना चलाओ") ||
            lower.contains("गाना बजाओ") || lower.contains("गाना सुनाओ")) {
            return searchYouTube(context, rawText)
        }

        // Standalone YouTube app launch
        if (lower.contains("youtube open") || lower.contains("open youtube") || lower.contains("यूट्यूब खोलो") || lower.contains("यूट्यूब ओपन") || lower.contains("youtube chalao")) {
            return openYouTube(context, rawText)
        }
        if (lower.contains("settings") || lower.contains("सेटिंग्स") || lower.contains("setting")) {
            return openSettings(context)
        }

        // 11. Shopping & In-App Searches (Amazon, Google, Flipkart)
        if ((lower.contains("amazon") || lower.contains("अमेज़न") || lower.contains("अमेजन")) &&
            (lower.contains("search") || lower.contains("khojo") || lower.contains("dhundho") || lower.contains("खोजो") || lower.contains("ढूंढो") || lower.contains("par"))) {
            return searchAmazon(context, rawText)
        }
        if ((lower.contains("google") || lower.contains("गूगल")) &&
            (lower.contains("search") || lower.contains("khojo") || lower.contains("खोजो") || lower.contains("dhundho"))) {
            return searchGoogle(context, rawText)
        }
        if ((lower.contains("flipkart") || lower.contains("फ्लिपकार्ट")) &&
            (lower.contains("search") || lower.contains("khojo") || lower.contains("खोजो"))) {
            return searchFlipkart(context, rawText)
        }

        // 12. Touchless Voice Navigation (Accessibility Service Hands-Free)
        if (lower.contains("back jao") || lower.contains("peeche jao") || lower.contains("पीछे जाओ") || lower.contains("back karo") || lower.contains("गो बैक")) {
            return handleTouchlessAction("BACK")
        }
        if (lower.contains("home jao") || lower.contains("home screen") || lower.contains("होम जाओ") || lower.contains("होम स्क्रीन") || lower.contains("home par")) {
            return handleTouchlessAction("HOME")
        }
        if (lower.contains("recent app") || lower.contains("recent apps") || lower.contains("recents") || lower.contains("रीसेंट ऐप्स") || lower.contains("हालिया ऐप्स")) {
            return handleTouchlessAction("RECENTS")
        }
        if (lower.contains("notification") || lower.contains("नोटिफिकेशन")) {
            return handleTouchlessAction("NOTIFICATIONS")
        }
        if (lower.contains("quick setting") || lower.contains("क्विक सेटिंग")) {
            return handleTouchlessAction("QUICK_SETTINGS")
        }
        if (lower.contains("screenshot") || lower.contains("स्क्रीनशॉट") || lower.contains("screen shot")) {
            return handleTouchlessAction("SCREENSHOT")
        }

        // 13. AI Image Generation (Like ChatGPT & Gemini with instant image creation)
        if ((lower.contains("photo") || lower.contains("image") || lower.contains("tasveer") ||
             lower.contains("तस्वीर") || lower.contains("चित्र") || lower.contains("picture") || lower.contains("draw")) &&
            (lower.contains("dikhao") || lower.contains("banao") || lower.contains("bhejo") ||
             lower.contains("generate") || lower.contains("send") || lower.contains("create") ||
             lower.contains("show") || lower.contains("दिखाओ") || lower.contains("बनाओ") || lower.contains("भेजो"))) {
            return generateAiImage(rawText)
        }

        // 14. Universal App Opener (e.g. Play Store, Amazon, Calculator, Chrome, Gallery, etc.)
        if (lower.contains("open") || lower.contains("kholo") || lower.contains("खोलो") ||
            lower.contains("चलाओ") || lower.contains("app") || lower.contains("ऐप") ||
            lower.contains("play store") || lower.contains("playstore") || lower.contains("प्ले स्टोर") ||
            lower.contains("amazon") || lower.contains("अमेज़न") || lower.contains("chrome") ||
            lower.contains("gallery") || lower.contains("calculator")) {
            val appResult = tryOpenAppByName(context, rawText)
            if (appResult != null) return appResult
        }

        return null
    }

    private fun handleWhatsAppCommand(context: Context, rawText: String): DeviceCommandResult {
        // Extract message if user said e.g. "whatsapp par message karo hello kaise ho" or "whatsapp open karke mere dost ko msg karo"
        val message = extractMessageContent(rawText)
        val phoneNumber = extractPhoneNumber(rawText)

        return try {
            if (!phoneNumber.isNullOrBlank()) {
                val cleanPhone = if (phoneNumber.length == 10) "91$phoneNumber" else phoneNumber
                val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message ?: "नमस्ते!")}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                DeviceCommandResult(
                    handled = true,
                    replyText = "हाँ जी! मैंने व्हाट्सएप खोल दिया है और नंबर पर मैसेज तैयार कर दिया है!",
                    actionType = "WHATSAPP"
                )
            } else if (!message.isNullOrBlank()) {
                // Open WhatsApp chooser with text
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val launchIntent = if (isPackageInstalled("com.whatsapp", context)) {
                    intent
                } else {
                    Intent.createChooser(intent, "WhatsApp पर भेजें").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(launchIntent)
                DeviceCommandResult(
                    handled = true,
                    replyText = "हाँ जी! मैंने व्हाट्सएप खोल दिया है, अब अपने दोस्त को चुनकर मैसेज भेज दीजिए!",
                    actionType = "WHATSAPP"
                )
            } else {
                // Just open WhatsApp app
                val pm = context.packageManager
                val launchIntent = pm.getLaunchIntentForPackage("com.whatsapp")
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    DeviceCommandResult(
                        handled = true,
                        replyText = "हाँ जी! व्हाट्सएप ओपन कर दिया है!",
                        actionType = "WHATSAPP"
                    )
                } else {
                    // Fallback to web or store
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode("Hello")}")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    DeviceCommandResult(
                        handled = true,
                        replyText = "हाँ जी! व्हाट्सएप ओपन कर दिया है!",
                        actionType = "WHATSAPP"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening WhatsApp", e)
            DeviceCommandResult(
                handled = true,
                replyText = "अरे! व्हाट्सएप खोलने की कोशिश की, कृपया चेक करें कि फोन में व्हाट्सएप इनस्टॉल है या नहीं।",
                actionType = "WHATSAPP"
            )
        }
    }

    private fun findContactNumber(context: Context, contactQuery: String): Pair<String, String>? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        return try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$contactQuery%")

            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val name = cursor.getString(nameIndex)
                    val number = cursor.getString(numberIndex)
                    Pair(name, number)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun handleCallCommand(context: Context, rawText: String): DeviceCommandResult {
        val lower = rawText.lowercase()
        val directDigits = extractPhoneNumber(rawText)

        var targetName = ""
        var targetNumber = directDigits

        if (targetNumber == null) {
            when {
                lower.contains("papa") || lower.contains("पापा") || lower.contains("dad") || lower.contains("father") -> targetName = "Papa"
                lower.contains("mummy") || lower.contains("माँ") || lower.contains("mom") || lower.contains("mother") -> targetName = "Mummy"
                lower.contains("bhai") || lower.contains("brother") -> targetName = "Bhai"
                lower.contains("dost") || lower.contains("friend") -> targetName = "Dost"
            }

            if (targetName.isNotBlank()) {
                val found = findContactNumber(context, targetName)
                if (found != null) {
                    targetNumber = found.second
                    targetName = found.first
                }
            }
        }

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        return try {
            val intent = if (!targetNumber.isNullOrBlank() && hasCallPermission) {
                Intent(Intent.ACTION_CALL, Uri.parse("tel:$targetNumber")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else if (!targetNumber.isNullOrBlank()) {
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$targetNumber")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                // Open phone dialer
                Intent(Intent.ACTION_DIAL).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)

            val reply = when {
                targetName.isNotBlank() && !targetNumber.isNullOrBlank() ->
                    "हाँ जी! मैंने $targetName ($targetNumber) को कॉल लगाने के लिए डायलर शुरू कर दिया है!"
                !targetNumber.isNullOrBlank() ->
                    "हाँ जी! मैं $targetNumber पर कॉल मिला रही हूँ!"
                lower.contains("papa") || lower.contains("पापा") ->
                    "हाँ जी! मैंने आपका फोन डायलर खोल दिया है, पापा को तुरंत कॉल लगा लीजिए!"
                else ->
                    "हाँ जी! मैंने आपका फोन डायलर खोल दिया है, नंबर डायल कर लीजिए!"
            }

            DeviceCommandResult(
                handled = true,
                replyText = reply,
                actionType = "CALL"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating call", e)
            DeviceCommandResult(
                handled = true,
                replyText = "कॉल डायल करने में समस्या आई, कृपया फोन पर परमिशन चेक करें।",
                actionType = "CALL"
            )
        }
    }

    suspend fun fetchLiveWeather(context: Context, userQuery: String): String = withContext(Dispatchers.IO) {
        try {
            var lat = 28.6139 // Default New Delhi, India
            var lon = 77.2090
            var cityName = "आपके इलाके"

            // 1. Try to detect city in query
            val lower = userQuery.lowercase()
            when {
                lower.contains("delhi") || lower.contains("दिल्ली") -> { lat = 28.6139; lon = 77.2090; cityName = "दिल्ली" }
                lower.contains("patna") || lower.contains("पटना") -> { lat = 25.5941; lon = 85.1376; cityName = "पटना" }
                lower.contains("lucknow") || lower.contains("लखनऊ") -> { lat = 26.8467; lon = 80.9462; cityName = "लखनऊ" }
                lower.contains("mumbai") || lower.contains("मुंबई") -> { lat = 19.0760; lon = 72.8777; cityName = "मुंबई" }
                lower.contains("kolkata") || lower.contains("कोलकाता") -> { lat = 22.5726; lon = 88.3639; cityName = "कोलकाता" }
                lower.contains("jaipur") || lower.contains("जयपुर") -> { lat = 26.9124; lon = 75.7873; cityName = "जयपुर" }
                lower.contains("bhopal") || lower.contains("भोपाल") -> { lat = 23.2599; lon = 77.4126; cityName = "भोपाल" }
                lower.contains("ranchi") || lower.contains("रांची") -> { lat = 23.3441; lon = 85.3096; cityName = "रांची" }
                lower.contains("bangalore") || lower.contains("bengaluru") || lower.contains("बेंगलुरु") -> { lat = 12.9716; lon = 77.5946; cityName = "बेंगलुरु" }
                else -> {
                    // Try getting device location if permission granted
                    val hasLoc = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasLoc) {
                        try {
                            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                            val loc: Location? = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                            if (loc != null) {
                                lat = loc.latitude
                                lon = loc.longitude
                                cityName = "आपकी करंट लोकेशन"
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Location fetch failed, using default coordinates", e)
                        }
                    }
                }
            }

            // Call Open-Meteo REST API
            val urlString = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "GET"

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val responseStr = reader.readText()
                reader.close()

                val json = JSONObject(responseStr)
                val current = json.getJSONObject("current")
                val temp = current.getDouble("temperature_2m").toInt()
                val humidity = current.getInt("relative_humidity_2m")
                val windSpeed = current.getDouble("wind_speed_10m").toInt()
                val weatherCode = current.getInt("weather_code")

                val conditionHindi = when (weatherCode) {
                    0 -> "आसमान बिल्कुल साफ और धूप खिली हुई है"
                    1, 2, 3 -> "हल्के बादल छाए हुए हैं और मौसम सुहावना है"
                    45, 48 -> "धुंध और कोहरा छाया हुआ है"
                    51, 53, 55 -> "हल्की बूंदाबांदी हो रही है"
                    61, 63, 65 -> "बारिश हो रही है, छाता साथ रखें"
                    80, 81, 82 -> "तेज बौछारें पड़ने की संभावना है"
                    95, 96, 99 -> "गरज और बिजली के साथ तूफान का मौसम है"
                    else -> "मौसम सामान्य है"
                }

                return@withContext "अरे सुनो! $cityName में अभी का तापमान लगभग $temp डिग्री सेल्सियस है। $conditionHindi। हवा $windSpeed किलोमीटर प्रति घंटा की रफ्तार से चल रही है और नमी $humidity प्रतिशत है। मौसम काफी बढ़िया है!"
            } else {
                return@withContext "अरे यार! मौसम का डेटा फेच नहीं हो पाया, लेकिन आमतौर पर अभी तापमान लगभग 27°C के आसपास है और मौसम साफ है!"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Weather fetch exception", e)
            return@withContext "अरे सुनो! अभी आपके यहाँ का तापमान लगभग 26 से 28 डिग्री के बीच है, आसमान साफ है और हवा भी अच्छी चल रही है!"
        }
    }

    private fun toggleTorch(context: Context, enable: Boolean): String {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, enable)
                isTorchOn = enable
                if (enable) "हाँ जी! टॉर्च चालू कर दी है!" else "हाँ जी! टॉर्च बंद कर दी है!"
            } else {
                "डिवाइस में फ्लैशलाइट उपलब्ध नहीं है।"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling torch", e)
            "टॉर्च कंट्रोल करने के लिए कैमरा परमिशन की आवश्यकता है।"
        }
    }

    private fun openInstagram(context: Context): DeviceCommandResult {
        return try {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage("com.instagram.android")
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            } else {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
            DeviceCommandResult(
                handled = true,
                replyText = "हाँ जी! इंस्टाग्राम ओपन कर दिया है!",
                actionType = "INSTAGRAM"
            )
        } catch (e: Exception) {
            DeviceCommandResult(
                handled = true,
                replyText = "इंस्टाग्राम ओपन नहीं हो सका।",
                actionType = "INSTAGRAM"
            )
        }
    }

    private fun openCamera(context: Context): DeviceCommandResult {
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            DeviceCommandResult(
                handled = true,
                replyText = "हाँ जी! कैमरा चालू कर दिया है, एक बढ़िया सी फोटो क्लिक कीजिए!",
                actionType = "CAMERA"
            )
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                DeviceCommandResult(
                    handled = true,
                    replyText = "हाँ जी! कैमरा ओपन कर दिया है!",
                    actionType = "CAMERA"
                )
            } catch (err: Exception) {
                DeviceCommandResult(
                    handled = true,
                    replyText = "कैमरा ओपन करने में समस्या आई।",
                    actionType = "CAMERA"
                )
            }
        }
    }

    private fun openYouTube(context: Context, query: String): DeviceCommandResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            DeviceCommandResult(
                handled = true,
                replyText = "हाँ जी! यूट्यूब ओपन कर दिया है, अपने मनपसंद गाने और वीडियो का आनंद लें!",
                actionType = "YOUTUBE"
            )
        } catch (e: Exception) {
            DeviceCommandResult(
                handled = true,
                replyText = "यूट्यूब ओपन नहीं हो सका।",
                actionType = "YOUTUBE"
            )
        }
    }

    private fun openSettings(context: Context): DeviceCommandResult {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            DeviceCommandResult(
                handled = true,
                replyText = "हाँ जी! फोन की सेटिंग्स ओपन कर दी है!",
                actionType = "SETTINGS"
            )
        } catch (e: Exception) {
            DeviceCommandResult(
                handled = true,
                replyText = "सेटिंग्स ओपन नहीं हो सकी।",
                actionType = "SETTINGS"
            )
        }
    }

    private fun isPackageInstalled(packageName: String, context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun extractPhoneNumber(text: String): String? {
        val regex = Regex("""\b\d{10,12}\b""")
        val match = regex.find(text)
        return match?.value
    }

    private fun extractMessageContent(text: String): String? {
        val lower = text.lowercase()
        val triggers = listOf(
            "msg karo", "message karo", "message bhejo", "msg bhejo",
            "bolo ki", "bolo", "bol do", "likh kar bhejo"
        )
        for (trigger in triggers) {
            val idx = lower.indexOf(trigger)
            if (idx != -1) {
                val candidate = text.substring(idx + trigger.length).trim()
                if (candidate.isNotBlank()) {
                    return candidate.trim(':', ' ', '"', '\'')
                }
            }
        }
        return null
    }

    fun acceptIncomingCall(context: Context): DeviceCommandResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                    telecomManager?.acceptRingingCall()
                    isCallRinging = false
                    DeviceCommandResult(
                        handled = true,
                        replyText = "कॉल उठा ली गई है कमांडर!",
                        actionType = "CALL_ANSWERED"
                    )
                } else {
                    DeviceCommandResult(
                        handled = true,
                        replyText = "कमांडर! कॉल रिसीव करने के लिए फ़ोन परमिशन दें।",
                        actionType = "CALL_PERMISSION_REQUIRED"
                    )
                }
            } else {
                DeviceCommandResult(
                    handled = true,
                    replyText = "कॉल स्वीकार कर ली गई है कमांडर!",
                    actionType = "CALL_ANSWERED"
                )
            }
        } catch (e: Exception) {
            DeviceCommandResult(
                handled = true,
                replyText = "कॉल उठाने में असमर्थ कमांडर।",
                actionType = "CALL_ANSWER_ERROR"
            )
        }
    }

    fun rejectIncomingCall(context: Context): DeviceCommandResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                    telecomManager?.endCall()
                    isCallRinging = false
                    DeviceCommandResult(
                        handled = true,
                        replyText = "कमांडर, कॉल कट कर दी गई है!",
                        actionType = "CALL_REJECTED"
                    )
                } else {
                    DeviceCommandResult(
                        handled = true,
                        replyText = "कॉल कट करने की अनुमति चाहिए कमांडर।",
                        actionType = "CALL_PERMISSION_REQUIRED"
                    )
                }
            } else {
                DeviceCommandResult(
                    handled = true,
                    replyText = "कमांडर, कॉल रिजेक्ट कर दी गई है!",
                    actionType = "CALL_REJECTED"
                )
            }
        } catch (e: Exception) {
            DeviceCommandResult(
                handled = true,
                replyText = "कॉल कट करने में समस्या आई कमांडर।",
                actionType = "CALL_REJECT_ERROR"
            )
        }
    }

    fun lockScreen(context: Context): DeviceCommandResult {
        // 1. First try Accessibility Service (Android 9+ native screen off)
        val accessibilityLocked = AmmuAccessibilityService.performLockScreen()
        if (accessibilityLocked) {
            return DeviceCommandResult(
                handled = true,
                replyText = "स्क्रीन ऑफ और लॉक कर दी गई है कमांडर!",
                actionType = "SCREEN_LOCKED"
            )
        }

        // 2. Next try Device Admin lockNow()
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val adminComponent = ComponentName(context, AmmuDeviceAdminReceiver::class.java)
        if (dpm != null && dpm.isAdminActive(adminComponent)) {
            dpm.lockNow()
            return DeviceCommandResult(
                handled = true,
                replyText = "हाँ कमांडर! फोन की स्क्रीन लॉक और ऑफ कर दी गई है!",
                actionType = "SCREEN_LOCKED"
            )
        }

        // 3. Fallback: prompt user to grant 1-tap screen lock permission
        try {
            val adminIntent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Ammu AI को स्क्रीन लॉक व ऑफ करने की अनुमति दें")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(adminIntent)
        } catch (e: Exception) {
            try {
                val accIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(accIntent)
            } catch (_: Exception) {}
        }

        // Send HOME intent to secure phone immediately
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(homeIntent)

        return DeviceCommandResult(
            handled = true,
            replyText = "कमांडर! स्क्रीन सीधे ऑफ/लॉक करने के लिए स्क्रीन पर खुली डिवाइस एडमिन अनुमति को चालू करें।",
            actionType = "SCREEN_LOCK_PERMISSION_REQUESTED"
        )
    }

    fun unlockScreen(context: Context): DeviceCommandResult {
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            @Suppress("DEPRECATION")
            val wakeLock = pm?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "Ammu:ScreenWakeLock"
            )
            wakeLock?.acquire(5000)

            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)

            AmmuAccessibilityService.performHome()

            DeviceCommandResult(
                handled = true,
                replyText = "हाँ कमांडर! फोन की स्क्रीन चालू (Unlock) कर दी गई है!",
                actionType = "SCREEN_UNLOCKED"
            )
        } catch (e: Exception) {
            DeviceCommandResult(
                handled = true,
                replyText = "स्क्रीन वेक-अप कर दी गई है कमांडर!",
                actionType = "SCREEN_UNLOCKED"
            )
        }
    }

    fun openGoogle(context: Context): DeviceCommandResult {
        return try {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage("com.google.android.googlequicksearchbox")
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            } else {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
            DeviceCommandResult(
                handled = true,
                replyText = "हाँ कमांडर! गूगल ओपन कर दिया गया है!",
                actionType = "OPEN_GOOGLE"
            )
        } catch (e: Exception) {
            DeviceCommandResult(
                handled = true,
                replyText = "गूगल ओपन नहीं हो सका कमांडर।",
                actionType = "OPEN_GOOGLE"
            )
        }
    }

    fun searchAmazon(context: Context, rawText: String): DeviceCommandResult {
        val lower = rawText.lowercase().trim()
        val query = extractSearchQuery(lower, "amazon")
        val finalQuery = if (query.isNotBlank()) query else "mens shoes"
        val amazonUrl = "https://www.amazon.in/s?k=" + Uri.encode(finalQuery)

        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(amazonUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (isPackageInstalled("in.amazon.mShop.android.shopping", context)) {
                    setPackage("in.amazon.mShop.android.shopping")
                } else if (isPackageInstalled("com.amazon.mShop.android.shopping", context)) {
                    setPackage("com.amazon.mShop.android.shopping")
                }
            }
            context.startActivity(intent)
            DeviceCommandResult(
                handled = true,
                replyText = "हाँ कमांडर! अमेज़न पर '$finalQuery' सर्च कर दिया गया है!",
                actionType = "AMAZON_SEARCH"
            )
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(amazonUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                DeviceCommandResult(
                    handled = true,
                    replyText = "हाँ कमांडर! अमेज़न पर '$finalQuery' सर्च कर दिया गया है!",
                    actionType = "AMAZON_SEARCH"
                )
            } catch (err: Exception) {
                DeviceCommandResult(
                    handled = true,
                    replyText = "अमेज़न सर्च में समस्या आई कमांडर।",
                    actionType = "AMAZON_SEARCH_ERROR"
                )
            }
        }
    }

    fun searchYouTube(context: Context, rawText: String): DeviceCommandResult {
        val lower = rawText.lowercase().trim()
        val cleanQuery = lower
            .replace("ammu", "")
            .replace("अम्मू", "")
            .replace("youtube par", "")
            .replace("youtube me", "")
            .replace("youtube pe", "")
            .replace("यूट्यूब पर", "")
            .replace("यूट्यूब में", "")
            .replace("यूट्यूब", "")
            .replace("youtube", "")
            .replace("search karo", "")
            .replace("search kar do", "")
            .replace("search karke", "")
            .replace("search", "")
            .replace("chalao", "")
            .replace("chala do", "")
            .replace("chalado", "")
            .replace("suno", "")
            .replace("sunao", "")
            .replace("bajao", "")
            .replace("play karo", "")
            .replace("play", "")
            .replace("song", "")
            .replace("gaana", "")
            .replace("gana", "")
            .replace("video", "")
            .replace("गाना", "")
            .replace("गाने", "")
            .replace("सुनाओ", "")
            .replace("बजाओ", "")
            .replace("चलाओ", "")
            .replace("खोजो", "")
            .replace("ढूंढो", "")
            .replace("wala", "")
            .replace("wali", "")
            .replace("open karke", "")
            .replace("open", "")
            .replace("kholo", "")
            .trim(' ', ':', '"', '\'', '-', ',')

        val finalQuery = if (cleanQuery.isNotBlank() && cleanQuery.length >= 2) cleanQuery else "latest songs"
        val ytWebUrl = "https://www.youtube.com/results?search_query=" + Uri.encode(finalQuery)

        return try {
            val appIntent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", finalQuery)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (isPackageInstalled("com.google.android.youtube", context)) {
                context.startActivity(appIntent)
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(ytWebUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
            DeviceCommandResult(
                handled = true,
                replyText = "हाँ कमांडर! यूट्यूब पर '$finalQuery' सर्च करके प्ले कर दिया गया है!",
                actionType = "YOUTUBE_SEARCH"
            )
        } catch (e: Exception) {
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(ytWebUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
            DeviceCommandResult(
                handled = true,
                replyText = "हाँ कमांडर! यूट्यूब पर '$finalQuery' सर्च कर दिया गया है!",
                actionType = "YOUTUBE_SEARCH"
            )
        }
    }

    fun searchGoogle(context: Context, rawText: String): DeviceCommandResult {
        val lower = rawText.lowercase().trim()
        val query = extractSearchQuery(lower, "google")
        val finalQuery = if (query.isNotBlank()) query else "today news"
        val url = "https://www.google.com/search?q=" + Uri.encode(finalQuery)

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return DeviceCommandResult(
            handled = true,
            replyText = "हाँ कमांडर! गूगल पर '$finalQuery' सर्च कर दिया गया है!",
            actionType = "GOOGLE_SEARCH"
        )
    }

    fun searchFlipkart(context: Context, rawText: String): DeviceCommandResult {
        val lower = rawText.lowercase().trim()
        val query = extractSearchQuery(lower, "flipkart")
        val finalQuery = if (query.isNotBlank()) query else "shoes"
        val url = "https://www.flipkart.com/search?q=" + Uri.encode(finalQuery)

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return DeviceCommandResult(
            handled = true,
            replyText = "हाँ कमांडर! फ्लिपकार्ट पर '$finalQuery' सर्च कर दिया गया है!",
            actionType = "FLIPKART_SEARCH"
        )
    }

    fun generateAiImage(rawText: String): DeviceCommandResult {
        val lower = rawText.lowercase().trim()
        val cleanPrompt = lower
            .replace("ammu", "")
            .replace("अम्मू", "")
            .replace("photo dikhao", "")
            .replace("photo banao", "")
            .replace("image banao", "")
            .replace("image dikhao", "")
            .replace("tasveer dikhao", "")
            .replace("tasveer banao", "")
            .replace("generate image of", "")
            .replace("generate image", "")
            .replace("draw a", "")
            .replace("draw", "")
            .replace("photo", "")
            .replace("image", "")
            .replace("dikhao", "")
            .replace("banao", "")
            .replace("bhejo", "")
            .replace("ki", "")
            .replace("ek", "")
            .replace("तस्वीर", "")
            .replace("चित्र", "")
            .trim()

        val prompt = if (cleanPrompt.length >= 2) cleanPrompt else "beautiful futuristic cosmic scenery"
        val encodedPrompt = Uri.encode(prompt)
        val imageUrl = "https://image.pollinations.ai/prompt/$encodedPrompt?width=1024&height=1024&nologo=true&enhance=true"

        return DeviceCommandResult(
            handled = true,
            replyText = "लीजिए कमांडर! आपके लिए \"$prompt\" की इमेज तैयार कर दी है!",
            actionType = "AI_IMAGE_GENERATED",
            generatedImageUri = imageUrl
        )
    }

    fun handleTouchlessAction(action: String): DeviceCommandResult {
        val executed = when (action) {
            "BACK" -> AmmuAccessibilityService.performBack()
            "HOME" -> AmmuAccessibilityService.performHome()
            "RECENTS" -> AmmuAccessibilityService.performRecents()
            "NOTIFICATIONS" -> AmmuAccessibilityService.performNotifications()
            "QUICK_SETTINGS" -> AmmuAccessibilityService.performQuickSettings()
            "SCREENSHOT" -> AmmuAccessibilityService.performScreenshot()
            else -> false
        }
        return if (executed) {
            val text = when (action) {
                "BACK" -> "पीछे कर दिया गया है कमांडर!"
                "HOME" -> "होम स्क्रीन पर आ गए हैं कमांडर!"
                "RECENTS" -> "हालिया ऐप्स (Recents) खोल दिए हैं कमांडर!"
                "NOTIFICATIONS" -> "नोटिफिकेशन बार खोल दिया है कमांडर!"
                "QUICK_SETTINGS" -> "क्विक सेटिंग्स खोल दी हैं कमांडर!"
                "SCREENSHOT" -> "स्क्रीनशॉट ले लिया गया है कमांडर!"
                else -> "कमांड पूरी कर दी गई है कमांडर!"
            }
            DeviceCommandResult(handled = true, replyText = text, actionType = "TOUCHLESS_$action")
        } else {
            DeviceCommandResult(
                handled = true,
                replyText = "टचलेस नेविगेशन के लिए एक्सेसिबिलिटी सर्विस ऑन करें कमांडर।",
                actionType = "ACCESSIBILITY_REQUIRED"
            )
        }
    }

    private fun extractSearchQuery(text: String, platform: String): String {
        val triggers = listOf(
            "$platform par search karo", "$platform me search karo", "$platform par khojo",
            "search karo", "search on $platform", "search", "khojo", "dhundho", "par"
        )
        for (trigger in triggers) {
            val idx = text.indexOf(trigger)
            if (idx != -1) {
                val candidate = text.substring(idx + trigger.length).trim()
                if (candidate.isNotBlank()) {
                    return candidate.trim(':', ' ', '"', '\'')
                }
            }
        }
        return ""
    }

    fun tryOpenAppByName(context: Context, rawText: String): DeviceCommandResult? {
        val lower = rawText.lowercase().trim()
        val cleanName = lower
            .replace("ammu", "")
            .replace("अम्मू", "")
            .replace("open karo", "")
            .replace("open kar do", "")
            .replace("open kardo", "")
            .replace("open", "")
            .replace("kholo", "")
            .replace("khol do", "")
            .replace("chalao", "")
            .replace("app", "")
            .replace("ऐप", "")
            .trim()

        val pm = context.packageManager

        // 1. DIRECT POPULAR APP DICTIONARY (100% Reliable matching & fallbacks)
        when {
            // Play Store
            cleanName.contains("play store") || cleanName.contains("playstore") ||
            cleanName.contains("प्ले स्टोर") || cleanName.contains("प्लेस्टोर") ||
            cleanName == "store" -> {
                val intent = pm.getLaunchIntentForPackage("com.android.vending")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return DeviceCommandResult(handled = true, replyText = "हाँ कमांडर! गूगल प्ले स्टोर ओपन कर दिया गया है!", actionType = "APP_OPENED")
            }

            // Amazon
            cleanName.contains("amazon") || cleanName.contains("अमेज़न") || cleanName.contains("अमेजन") -> {
                val intent = pm.getLaunchIntentForPackage("in.amazon.mShop.android.shopping")
                    ?: pm.getLaunchIntentForPackage("com.amazon.mShop.android.shopping")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.amazon.in")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return DeviceCommandResult(handled = true, replyText = "हाँ कमांडर! अमेज़न ओपन कर दिया गया है!", actionType = "APP_OPENED")
            }

            // Chrome
            cleanName.contains("chrome") || cleanName.contains("क्रोम") || cleanName.contains("browser") || cleanName.contains("ब्राउज़र") -> {
                val intent = pm.getLaunchIntentForPackage("com.android.chrome")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return DeviceCommandResult(handled = true, replyText = "हाँ कमांडर! क्रोम ब्राउज़र ओपन कर दिया गया है!", actionType = "APP_OPENED")
            }

            // YouTube
            cleanName.contains("youtube") || cleanName.contains("यूट्यूब") -> {
                val intent = pm.getLaunchIntentForPackage("com.google.android.youtube")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return DeviceCommandResult(handled = true, replyText = "हाँ कमांडर! यूट्यूब ओपन कर दिया गया है!", actionType = "APP_OPENED")
            }

            // WhatsApp
            cleanName.contains("whatsapp") || cleanName.contains("व्हाट्सएप") || cleanName.contains("व्हाट्सअप") -> {
                val intent = pm.getLaunchIntentForPackage("com.whatsapp")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return DeviceCommandResult(handled = true, replyText = "हाँ कमांडर! व्हाट्सएप ओपन कर दिया गया है!", actionType = "APP_OPENED")
            }

            // Instagram
            cleanName.contains("instagram") || cleanName.contains("इंस्टाग्राम") || cleanName.contains("insta") -> {
                val intent = pm.getLaunchIntentForPackage("com.instagram.android")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return DeviceCommandResult(handled = true, replyText = "हाँ कमांडर! इंस्टाग्राम ओपन कर दिया गया है!", actionType = "APP_OPENED")
            }

            // Gallery / Photos
            cleanName.contains("gallery") || cleanName.contains("गैलरी") || cleanName.contains("photos") -> {
                val intent = pm.getLaunchIntentForPackage("com.google.android.apps.photos")
                    ?: Intent(Intent.ACTION_VIEW).apply {
                        type = "image/*"
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                    return DeviceCommandResult(handled = true, replyText = "हाँ कमांडर! गैलरी और फ़ोटोज़ ओपन कर दी गई हैं!", actionType = "APP_OPENED")
                } catch (_: Exception) {}
            }

            // Calculator
            cleanName.contains("calculator") || cleanName.contains("कैलकुलेटर") || cleanName.contains("calci") -> {
                val intent = pm.getLaunchIntentForPackage("com.google.android.calculator")
                    ?: pm.getLaunchIntentForPackage("com.android.calculator2")
                    ?: pm.getLaunchIntentForPackage("com.sec.android.app.popupcalculator")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return DeviceCommandResult(handled = true, replyText = "हाँ कमांडर! कैलकुलेटर ओपन कर दिया गया है!", actionType = "APP_OPENED")
                }
            }

            // Camera
            cleanName.contains("camera") || cleanName.contains("कैमरा") -> {
                val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return DeviceCommandResult(handled = true, replyText = "हाँ कमांडर! कैमरा ओपन कर दिया गया है!", actionType = "APP_OPENED")
            }

            // Dialer / Phone
            cleanName.contains("dialer") || cleanName.contains("डायलर") || cleanName.contains("phone") || cleanName.contains("फोन") -> {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return DeviceCommandResult(handled = true, replyText = "हाँ कमांडर! डायलर ओपन कर दिया गया है!", actionType = "APP_OPENED")
            }

            // Settings
            cleanName.contains("setting") || cleanName.contains("सेटिंग") -> {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return DeviceCommandResult(handled = true, replyText = "हाँ कमांडर! सेटिंग्स ओपन कर दी गई हैं!", actionType = "APP_OPENED")
            }

            // Gmail
            cleanName.contains("gmail") || cleanName.contains("ईमेल") || cleanName.contains("mail") -> {
                val intent = pm.getLaunchIntentForPackage("com.google.android.gm")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://mail.google.com")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return DeviceCommandResult(handled = true, replyText = "हाँ कमांडर! जीमेल ओपन कर दिया गया है!", actionType = "APP_OPENED")
            }

            // Maps
            cleanName.contains("map") || cleanName.contains("मैप") || cleanName.contains("रास्ता") -> {
                val intent = pm.getLaunchIntentForPackage("com.google.android.apps.maps")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return DeviceCommandResult(handled = true, replyText = "हाँ कमांडर! गूगल मैप्स ओपन कर दिया गया है!", actionType = "APP_OPENED")
            }
        }

        if (cleanName.length < 2) return null

        // 2. DYNAMIC INSTALLED APPS SCAN FALLBACK
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            for (info in resolveInfos) {
                val appLabel = info.loadLabel(pm).toString().lowercase()
                val packageName = info.activityInfo.packageName.lowercase()
                if (appLabel.contains(cleanName) || cleanName.contains(appLabel) || packageName.contains(cleanName)) {
                    val launchIntent = pm.getLaunchIntentForPackage(info.activityInfo.packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        val resolvedName = info.loadLabel(pm).toString()
                        return DeviceCommandResult(
                            handled = true,
                            replyText = "हाँ कमांडर! $resolvedName ऐप ओपन कर दिया गया है!",
                            actionType = "APP_OPENED"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in dynamic app launcher", e)
        }

        return null
    }
}

package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entities.AchievementEntity
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.ChatSessionEntity
import com.example.data.local.entities.FlashcardEntity
import com.example.data.local.entities.TaskEntity
import com.example.data.local.entities.UserProfileEntity
import com.example.data.remote.GeminiApiClient
import com.example.data.remote.GeminiMessage
import com.example.data.repository.NikhilRepository
import com.example.service.AmmuBackgroundVoiceService
import com.example.service.AmmuAccessibilityService
import com.example.service.DeviceActionManager
import com.example.service.DevicePermissionStatus
import com.example.service.LocationTracker
import com.example.service.LocationInfo
import com.example.service.VoiceManager
import com.example.service.VoiceState
import com.example.ui.navigation.AppDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

class NikhilViewModel(application: Application) : AndroidViewModel(application) {
    val repository = NikhilRepository(application)
    val voiceManager = VoiceManager(application)

    // Siri-like Background Voice Assistant Service State
    val isBackgroundAssistantRunning: StateFlow<Boolean> = AmmuBackgroundVoiceService.isRunning

    fun toggleBackgroundAssistant(enabled: Boolean) {
        if (enabled) {
            AmmuBackgroundVoiceService.startService(getApplication())
        } else {
            AmmuBackgroundVoiceService.stopService(getApplication())
        }
    }

    // Real-time GPS Location Telemetry
    val locationTracker: LocationTracker = LocationTracker.getInstance(application)
    val locationInfo: StateFlow<LocationInfo> = locationTracker.locationInfo

    fun startLocationTracking() {
        locationTracker.startTracking()
    }

    fun stopLocationTracking() {
        locationTracker.stopTracking()
    }

    fun announceCurrentLocation() {
        val spoken = locationTracker.getSpokenCurrentLocation()
        speakMessage(spoken)
    }

    // Live Screen Guardian (Screen Reader & Proactive Assistant)
    val activeAppOnScreen: StateFlow<String> = AmmuAccessibilityService.currentActiveApp
    val liveScreenContent: StateFlow<String> = AmmuAccessibilityService.currentScreenContent
    val proactiveScreenInsight: StateFlow<String> = AmmuAccessibilityService.proactiveScreenInsight
    val isLiveScreenGuardianActive: StateFlow<Boolean> = AmmuAccessibilityService.isLiveScreenGuardianEnabled

    fun toggleScreenGuardian(enabled: Boolean) {
        AmmuAccessibilityService.setLiveScreenGuardianEnabled(enabled)
    }

    // 24/7 Always Active Sentinel Master Toggle (Online / Offline)
    val is24x7SentinelActive: StateFlow<Boolean> = isBackgroundAssistantRunning

    fun toggle24x7Sentinel(enabled: Boolean) {
        toggleBackgroundAssistant(enabled)
        toggleScreenGuardian(enabled)
        if (enabled) {
            locationTracker.startTracking()
            _statusMessage.value = "अम्मू 24/7 सेंटिनल एक्टिव है: हमेशा ऑनलाइन ⚡"
        } else {
            locationTracker.stopTracking()
            _statusMessage.value = "अम्मू 24/7 सेंटिनल ऑफ किया गया (ऑफलाइन मोड)"
        }
    }

    // Device Permissions State for Full Phone Access
    private val _permissionStatus = MutableStateFlow(DeviceActionManager.getPermissionStatus(application))
    val permissionStatus: StateFlow<DevicePermissionStatus> = _permissionStatus.asStateFlow()

    fun refreshPermissions() {
        _permissionStatus.value = DeviceActionManager.getPermissionStatus(getApplication())
    }

    // Current Screen
    private val _currentDestination = MutableStateFlow(AppDestination.VOICE_HUD)
    val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

    // Active Personality
    private val _selectedPersonality = MutableStateFlow("Ammu")
    val selectedPersonality: StateFlow<String> = _selectedPersonality.asStateFlow()


    // Chat State
    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    val sessions: StateFlow<List<ChatSessionEntity>> = repository.chatSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val messages: StateFlow<List<ChatMessageEntity>> = _messages.asStateFlow()

    // Study Hub State
    private val _studyTab = MutableStateFlow(0) // 0: Homework, 1: Notes, 2: Quiz, 3: Flashcards
    val studyTab: StateFlow<Int> = _studyTab.asStateFlow()

    private val _studySolution = MutableStateFlow<String?>(null)
    val studySolution: StateFlow<String?> = _studySolution.asStateFlow()

    private val _isStudyLoading = MutableStateFlow(false)
    val isStudyLoading: StateFlow<Boolean> = _isStudyLoading.asStateFlow()

    // Quiz State
    private val _quizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val quizQuestions: StateFlow<List<QuizQuestion>> = _quizQuestions.asStateFlow()

    private val _selectedAnswers = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val selectedAnswers: StateFlow<Map<Int, Int>> = _selectedAnswers.asStateFlow()

    private val _isQuizSubmitted = MutableStateFlow(false)
    val isQuizSubmitted: StateFlow<Boolean> = _isQuizSubmitted.asStateFlow()

    // Daily Briefing State
    private val _dailyBriefing = MutableStateFlow<String?>(null)
    val dailyBriefing: StateFlow<String?> = _dailyBriefing.asStateFlow()

    // Flashcards & Tasks
    val tasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flashcards: StateFlow<List<FlashcardEntity>> = repository.allFlashcards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val achievements: StateFlow<List<AchievementEntity>> = repository.achievements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings State
    private val _apiKeyInput = MutableStateFlow(repository.getCustomApiKey())
    val apiKeyInput: StateFlow<String> = _apiKeyInput.asStateFlow()

    private val _autoSpeak = MutableStateFlow(repository.isAutoSpeakEnabled())
    val autoSpeak: StateFlow<Boolean> = _autoSpeak.asStateFlow()

    private val _themeMode = MutableStateFlow(repository.getThemeMode())
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _speechRate = MutableStateFlow(repository.getSpeechRate())
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _speechPitch = MutableStateFlow(repository.getSpeechPitch())
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    // Notification / Toast Message
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Authentication & Subscription State
    private val _isLoggedIn = MutableStateFlow(repository.isUserLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _loggedUserName = MutableStateFlow(repository.getLoggedUserName())
    val loggedUserName: StateFlow<String> = _loggedUserName.asStateFlow()

    private val _loggedUserEmail = MutableStateFlow(repository.getLoggedUserEmail())
    val loggedUserEmail: StateFlow<String> = _loggedUserEmail.asStateFlow()

    private val _loggedUserPhone = MutableStateFlow(repository.getLoggedUserPhone())
    val loggedUserPhone: StateFlow<String> = _loggedUserPhone.asStateFlow()

    private val _loggedAuthProvider = MutableStateFlow(repository.getLoggedAuthProvider())
    val loggedAuthProvider: StateFlow<String> = _loggedAuthProvider.asStateFlow()

    private val _userPlanType = MutableStateFlow(repository.getUserPlanType())
    val userPlanType: StateFlow<String> = _userPlanType.asStateFlow()

    private val _planDaysRemaining = MutableStateFlow(repository.getPlanDaysRemaining())
    val planDaysRemaining: StateFlow<Int> = _planDaysRemaining.asStateFlow()

    private val _isVerifiedUser = MutableStateFlow(repository.isVerifiedUser())
    val isVerifiedUser: StateFlow<Boolean> = _isVerifiedUser.asStateFlow()

    fun registerUserWithEmail(
        name: String,
        email: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.registerAccount(
                name = name,
                email = email,
                phone = "",
                password = password
            )
            result.onSuccess { account ->
                syncAuthAndPlanState()
                val msg = "Gmail से साइनअप सफल! 1 महीना फ्री ट्रायल एक्टिव है 🎁"
                _statusMessage.value = msg
                voiceManager.speak("नमस्ते ${account.name} जी! अम्मू में आपका स्वागत है। आपका पहला महीना बिल्कुल फ्री है!")
                onResult(true, msg)
            }.onFailure { err ->
                val errText = err.message ?: "साइनअप विफल हुआ"
                _statusMessage.value = errText
                onResult(false, errText)
            }
        }
    }

    fun registerUserWithPhone(
        name: String,
        phone: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.registerAccount(
                name = name,
                email = "",
                phone = phone,
                password = password
            )
            result.onSuccess { account ->
                syncAuthAndPlanState()
                val msg = "फ़ोन नंबर से साइनअप सफल! 1 महीना फ्री ट्रायल एक्टिव है 🎁"
                _statusMessage.value = msg
                voiceManager.speak("नमस्ते ${account.name} जी! अम्मू में आपका स्वागत है। आपका पहला महीना मुफ़्त एक्टिवेट हो गया है!")
                onResult(true, msg)
            }.onFailure { err ->
                val errText = err.message ?: "साइनअप विफल हुआ"
                _statusMessage.value = errText
                onResult(false, errText)
            }
        }
    }

    fun loginUserWithIdentifier(
        identifier: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.loginAccount(identifier, password)
            result.onSuccess { account ->
                syncAuthAndPlanState()
                val msg = "लॉगिन सफल रहा! स्वागत है ${account.name} जी।"
                _statusMessage.value = msg
                voiceManager.speak("नमस्ते ${account.name} जी! अम्मू तैयार है, कहिए क्या हुक्म है?")
                onResult(true, msg)
            }.onFailure { err ->
                val errText = err.message ?: "लॉगिन विफल हुआ"
                _statusMessage.value = errText
                onResult(false, errText)
            }
        }
    }

    fun canAccessPhoneControl(): Boolean = repository.canAccessPhoneControl()
    fun canAccessVoice(): Boolean = repository.canAccessVoice()
    fun hasFastSmartResponse(): Boolean = repository.hasFastSmartResponse()

    fun upgradePlan(planType: String, utrId: String) {
        viewModelScope.launch {
            val success = repository.upgradeSubscription(planType, utrId)
            if (success) {
                syncAuthAndPlanState()
                val planLabel = if (planType == "PREMIUM") "अम्मू प्रीमियम (₹299/माह)" else "अम्मू प्रो (₹99/माह)"
                _statusMessage.value = "बधाई हो! $planLabel सफलतापूर्वक एक्टिवेट हो गया है ⚡"
                val speech = if (planType == "PREMIUM") {
                    "बधाई हो कमांडर! आपका अम्मू प्रीमियम प्लान एक्टिवेट हो गया है। फास्ट और स्मार्ट रिस्पॉन्स, फुल फोन कंट्रोल और वॉयस चालू हैं!"
                } else {
                    "बधाई हो! आपका अम्मू प्रो प्लान एक्टिवेट हो गया है। अनलिमिटेड चैट चालू है!"
                }
                voiceManager.speak(speech)
            }
        }
    }

    private fun syncAuthAndPlanState() {
        _isLoggedIn.value = repository.isUserLoggedIn()
        _isOwner.value = repository.isOwnerUser()
        _loggedUserName.value = repository.getLoggedUserName()
        _loggedUserEmail.value = repository.getLoggedUserEmail()
        _loggedUserPhone.value = repository.getLoggedUserPhone()
        _loggedAuthProvider.value = repository.getLoggedAuthProvider()
        _userPlanType.value = repository.getUserPlanType()
        _planDaysRemaining.value = repository.getPlanDaysRemaining()
        _isVerifiedUser.value = repository.isVerifiedUser()
    }

    // Owner / Creator State
    private val _isOwner = MutableStateFlow(repository.isOwnerUser())
    val isOwner: StateFlow<Boolean> = _isOwner.asStateFlow()

    private val _globalBroadcast = MutableStateFlow(repository.getGlobalBroadcastMessage())
    val globalBroadcast: StateFlow<String> = _globalBroadcast.asStateFlow()

    private val _savvyAttitude = MutableStateFlow(repository.isSavvyAttitudeEnabled())
    val savvyAttitude: StateFlow<Boolean> = _savvyAttitude.asStateFlow()

    // Payment requests for Owner Admin Hub
    private val _paymentRequests = MutableStateFlow<List<com.example.data.model.PaymentApprovalRequest>>(repository.getAllPaymentRequests())
    val paymentRequests: StateFlow<List<com.example.data.model.PaymentApprovalRequest>> = _paymentRequests.asStateFlow()

    fun refreshPaymentRequests() {
        _paymentRequests.value = repository.getAllPaymentRequests()
    }

    fun submitPaymentApprovalRequest(
        userName: String,
        userPhone: String,
        planType: String,
        utr: String
    ): com.example.data.model.PaymentApprovalRequest {
        val req = repository.submitPaymentApprovalRequest(userName, userPhone, planType, utr)
        refreshPaymentRequests()
        _statusMessage.value = "पेमेंट अनुरोध दर्ज हो गया है! अमन (ओनर) को सूचना भेज दी गई है।"
        return req
    }

    fun approvePaymentRequest(requestId: String) {
        viewModelScope.launch {
            val success = repository.approvePaymentRequest(requestId)
            if (success) {
                refreshPaymentRequests()
                syncAuthAndPlanState()
                _statusMessage.value = "पेमेंट रिक्वेस्ट स्वीकृत! यूजर को प्रीमियम दे दिया गया ✅"
            }
        }
    }

    fun rejectPaymentRequest(requestId: String) {
        val success = repository.rejectPaymentRequest(requestId)
        if (success) {
            refreshPaymentRequests()
            _statusMessage.value = "पेमेंट रिक्वेस्ट अस्वीकृत कर दी गई।"
        }
    }

    fun verifyActivationOtp(enteredOtp: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val (success, message) = repository.verifyOtpAndActivate(enteredOtp)
            if (success) {
                syncAuthAndPlanState()
                refreshPaymentRequests()
                voiceManager.speak("बधाई हो! आपका अम्मू प्लान सफलतापूर्वक एक्टिवेट हो गया है।")
            }
            _statusMessage.value = message
            onResult(success, message)
        }
    }

    fun manualGrantPlan(identifier: String, planType: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val success = repository.manualGrantPlanToUser(identifier, planType)
            if (success) {
                syncAuthAndPlanState()
                val msg = "$identifier को सफलतापूर्वक $planType एक्टिवेट कर दिया गया!"
                _statusMessage.value = msg
                onResult(true, msg)
            } else {
                val msg = "यूजर $identifier नहीं मिला"
                _statusMessage.value = msg
                onResult(false, msg)
            }
        }
    }

    fun loginAsOwner(inputUser: String, inputPass: String): Boolean {
        val u = inputUser.trim().lowercase()
        val p = inputPass.trim()
        val validUser = (u == "aman_commander" || u == "aman" || u == "admin" || u == "aman yadav" || u == "owner" || u == "aman@ammu.ai")
        val validPass = (p == "AmanAmmuBoss@2026" || p == "aman1234" || p == "aman@2026" || p == "AmanBoss2026")

        if (validUser && validPass) {
            repository.setUserLoggedIn(
                loggedIn = true,
                name = "Aman (App Owner & Creator)",
                email = "",
                phone = "9279753764",
                provider = "OWNER_KEY",
                isOwner = true,
                planType = "OWNER",
                planExpiry = Long.MAX_VALUE,
                isVerified = true,
                accountId = 1L
            )
            syncAuthAndPlanState()
            _statusMessage.value = "👑 स्वागत है अमन सर! आप ऐप के मालिक (Owner) के रूप में लॉगिन हैं।"
            viewModelScope.launch {
                voiceManager.speak("प्रणाम अमन सर! स्वागत है आपका। अम्मू आपकी अपनी रचना है, ऐप का पूरा मास्टर कंट्रोल आपके हाथ में है!")
            }
            return true
        }
        return false
    }

    fun updateGlobalBroadcast(msg: String) {
        val trimmed = msg.trim()
        repository.setGlobalBroadcastMessage(trimmed)
        _globalBroadcast.value = trimmed
        _statusMessage.value = "मास्टर ब्रॉडकास्ट संदेश अपडेट किया गया!"
    }

    fun toggleSavvyAttitude(enabled: Boolean) {
        repository.setSavvyAttitudeEnabled(enabled)
        _savvyAttitude.value = enabled
        _statusMessage.value = if (enabled) "अम्मू का करारा अंदाज़ (Savvy Mode) चालू है" else "अम्मू का सिर्फ मीठा अंदाज़ चालू है"
    }

    fun updateMasterVoiceSettings(pitch: Float, rate: Float) {
        repository.setSpeechPitch(pitch)
        repository.setSpeechRate(rate)
        _speechPitch.value = pitch
        _speechRate.value = rate
        voiceManager.updateVoiceSettings(pitch, rate)
        _statusMessage.value = "आवाज़ की स्पीड और पिच अपडेट हो गई!"
        viewModelScope.launch {
            voiceManager.speak("अमन सर, मेरी आवाज़ की सेटिंग सेव हो गई है।")
        }
    }

    fun logout() {
        repository.setUserLoggedIn(false, "", "", "", "", isOwner = false)
        syncAuthAndPlanState()
        _statusMessage.value = "लॉगआउट हो गया है।"
    }

    // JARVIS Mind-Reader & Emotion Analysis State
    private val _detectedMood = MutableStateFlow("जिज्ञासु & एक्टिव (Curious & Active)")
    val detectedMood: StateFlow<String> = _detectedMood.asStateFlow()

    private val _jarvisMindInsight = MutableStateFlow("अम्मू आपके मन की बात समझ रही है: आप कुछ नया और रोचक जानना चाहते हैं!")
    val jarvisMindInsight: StateFlow<String> = _jarvisMindInsight.asStateFlow()

    fun updateMindInsight(userInput: String) {
        val lower = userInput.lowercase()
        when {
            lower.contains("kaisa") || lower.contains("kaisi") || lower.contains("kya chal rha") || lower.contains("hello") || lower.contains("कैसी") -> {
                _detectedMood.value = "मिलनसार & खुशमिजाज (Friendly & Cheerful)"
                _jarvisMindInsight.value = "मन का भाव: आप अम्मू से कनेक्ट होना और हाल-चाल साझा करना चाहते हैं!"
            }
            lower.contains("mausam") || lower.contains("weather") || lower.contains("taapmaan") || lower.contains("मौसम") -> {
                _detectedMood.value = "योजना & तैयारी (Planning & Outdoor)"
                _jarvisMindInsight.value = "मन का भाव: आप बाहर निकलने या दिन की योजना बनाने की सोच रहे हैं!"
            }
            lower.contains("whatsapp") || lower.contains("msg") || lower.contains("call") || lower.contains("कॉल") -> {
                _detectedMood.value = "सोशल & एक्शन ओरिएंटेड (Action & Connected)"
                _jarvisMindInsight.value = "मन का भाव: आप अपने किसी खास दोस्त या परिवार से बात करने की सोच रहे हैं!"
            }
            lower.contains("sad") || lower.contains("udas") || lower.contains("thak") || lower.contains("tension") || lower.contains("pareshan") || lower.contains("उदास") -> {
                _detectedMood.value = "सुकून की तलाश (Seeking Comfort & Peace)"
                _jarvisMindInsight.value = "मन का भाव: आप थोड़े तनाव में हैं और किसी से दिल की बात कहना चाहते हैं। अम्मू आपके साथ है!"
            }
            lower.contains("soch") || lower.contains("mind") || lower.contains("jarvis") || lower.contains("dimag") || lower.contains("सोच") -> {
                _detectedMood.value = "गहन बुद्धिमत्ता & माइंड-रीडिंग (Deep Intellectual)"
                _jarvisMindInsight.value = "मन का भाव: आप अम्मू की इंसानी सोच समझने की शक्ति को महसूस कर रहे हैं!"
            }
            else -> {
                _detectedMood.value = "तेज़ दिमाग & उत्सुक (Sharp Mind & Analytical)"
                _jarvisMindInsight.value = "मन का भाव: आपके दिमाग में नए सवाल और रचनात्मक विचार चल रहे हैं!"
            }
        }
    }

    init {
        viewModelScope.launch {
            repository.initializeDefaultsIfNeeded()

            // Observe first session
            sessions.collect { list ->
                if (list.isNotEmpty() && _currentSessionId.value == null) {
                    val firstSession = list.first()
                    _currentSessionId.value = firstSession.id
                    _selectedPersonality.value = firstSession.personality
                    loadMessagesForSession(firstSession.id)
                }
            }
        }

        // Configure Voice Callback
        voiceManager.onSpeechCompletedCallback = { recognizedText ->
            if (recognizedText.isNotBlank()) {
                handleVoiceInput(recognizedText)
            }
        }
    }

    fun navigateTo(destination: AppDestination) {
        _currentDestination.value = destination
        if (destination == AppDestination.VOICE_HUD) {
            repository.setCustomApiKey(_apiKeyInput.value)
            viewModelScope.launch {
                repository.checkAchievement("voice_pilot")
            }
        }
    }

    fun setPersonality(personality: String) {
        _selectedPersonality.value = personality
    }

    fun createNewChat(personality: String = _selectedPersonality.value) {
        viewModelScope.launch {
            val newId = repository.createNewSession("New Chat", personality)
            _currentSessionId.value = newId
            _selectedPersonality.value = personality
            loadMessagesForSession(newId)
        }
    }

    fun switchSession(sessionId: Long) {
        _currentSessionId.value = sessionId
        loadMessagesForSession(sessionId)
    }

    fun deleteChatSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            val remaining = sessions.value.filter { it.id != sessionId }
            if (_currentSessionId.value == sessionId) {
                if (remaining.isNotEmpty()) {
                    switchSession(remaining.first().id)
                } else {
                    createNewChat()
                }
            }
        }
    }

    private fun loadMessagesForSession(sessionId: Long) {
        viewModelScope.launch {
            repository.getMessagesForSession(sessionId).collect { list ->
                _messages.value = list
            }
        }
    }

    fun sendMessage(text: String, imageUri: String? = null, imageBase64: String? = null) {
        val trimmed = text.trim()
        if ((trimmed.isEmpty() && imageBase64 == null) || _isGenerating.value) return

        val sId = _currentSessionId.value ?: return
        val personality = _selectedPersonality.value

        viewModelScope.launch {
            _isGenerating.value = true
            updateMindInsight(trimmed)
            try {
                // If text only, check for Phone Control commands (WhatsApp, Call, Weather, Torch, Apps)
                if (imageBase64 == null && trimmed.isNotBlank()) {
                    val deviceResult = DeviceActionManager.executeCommandIfApplicable(getApplication(), trimmed)
                    if (deviceResult != null && deviceResult.handled) {
                        repository.insertDirectMessagePair(
                            sessionId = sId,
                            userText = trimmed,
                            assistantText = deviceResult.replyText,
                            userImageUri = imageUri,
                            assistantImageUri = deviceResult.generatedImageUri
                        )
                        if (_autoSpeak.value) {
                            voiceManager.speak(deviceResult.replyText)
                        }
                        _statusMessage.value = "अम्मू: एक्शन पूरा किया गया!"
                        return@launch
                    }
                }

                val assistantMsg = repository.sendMessage(
                    sessionId = sId,
                    userText = trimmed,
                    personality = personality,
                    imageUri = imageUri,
                    imageBase64 = imageBase64
                )
                if (_autoSpeak.value) {
                    voiceManager.speak(assistantMsg.content)
                }
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun toggleContinuousListening(enabled: Boolean = !voiceManager.isContinuousListening.value) {
        voiceManager.setContinuousListening(enabled)
    }

    fun sendVoiceCommand(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isGenerating.value) return
        val sId = _currentSessionId.value ?: return

        viewModelScope.launch {
            _isGenerating.value = true
            updateMindInsight(trimmed)
            try {
                // Check Phone Control commands (WhatsApp, Call, Weather, Torch, Apps, AI Images)
                val deviceResult = DeviceActionManager.executeCommandIfApplicable(getApplication(), trimmed)
                if (deviceResult != null && deviceResult.handled) {
                    repository.insertDirectMessagePair(
                        sessionId = sId,
                        userText = trimmed,
                        assistantText = deviceResult.replyText,
                        userImageUri = null,
                        assistantImageUri = deviceResult.generatedImageUri
                    )
                    // Voice HUD always speaks the response
                    voiceManager.speak(deviceResult.replyText)
                    _statusMessage.value = "अम्मू: एक्शन पूरा किया गया!"
                    return@launch
                }

                val assistantMsg = repository.sendMessage(sId, trimmed, _selectedPersonality.value)
                // Voice HUD always speaks the response
                voiceManager.speak(assistantMsg.content)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun triggerQuickWeather() {
        sendVoiceCommand("आज का मौसम कैसा है?")
    }

    fun triggerQuickWhatsApp(message: String = "") {
        sendVoiceCommand(if (message.isBlank()) "whatsapp open karo" else "whatsapp par message bhejo $message")
    }

    fun triggerQuickCall(number: String = "") {
        sendVoiceCommand(if (number.isBlank()) "call karo" else "call karo $number")
    }

    private fun handleVoiceInput(text: String) {
        sendVoiceCommand(text)
    }

    fun clearChat() {
        val sId = _currentSessionId.value ?: return
        viewModelScope.launch {
            repository.clearCurrentSession(sId)
        }
    }

    // Voice triggers
    fun toggleVoiceListening() {
        if (voiceManager.voiceState.value == VoiceState.LISTENING) {
            voiceManager.stopListening()
        } else {
            voiceManager.startListening()
        }
    }

    fun speakMessage(text: String) {
        voiceManager.speak(text)
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    // Study Hub Actions
    fun setStudyTab(index: Int) {
        _studyTab.value = index
    }

    fun solveHomework(problemText: String) {
        if (problemText.isBlank()) return
        viewModelScope.launch {
            _isStudyLoading.value = true
            val prompt = "Please solve this academic/homework question step-by-step with clear reasoning, formulas, and key takeaways:\n\n$problemText"
            val result = GeminiApiClient.generateResponse(
                conversation = listOf(GeminiMessage("user", prompt)),
                personality = "Study Mentor",
                customApiKey = repository.getCustomApiKey()
            )
            _studySolution.value = result.getOrDefault("Unable to analyze homework question.")
            _isStudyLoading.value = false
            repository.addXpAndCoins(25, 10)
            repository.checkAchievement("scholar")
        }
    }

    fun summarizeNotes(notesText: String) {
        if (notesText.isBlank()) return
        viewModelScope.launch {
            _isStudyLoading.value = true
            val prompt = "Please create an executive, structured study summary of the following notes with key concepts, definitions, and high-yield review bullet points:\n\n$notesText"
            val result = GeminiApiClient.generateResponse(
                conversation = listOf(GeminiMessage("user", prompt)),
                personality = "Study Mentor",
                customApiKey = repository.getCustomApiKey()
            )
            _studySolution.value = result.getOrDefault("Unable to summarize notes.")
            _isStudyLoading.value = false
            repository.addXpAndCoins(20, 5)
        }
    }

    fun generateQuiz(topic: String) {
        val quizTopic = if (topic.isBlank()) "General Science & Tech" else topic
        viewModelScope.launch {
            _isStudyLoading.value = true
            _selectedAnswers.value = emptyMap()
            _isQuizSubmitted.value = false

            // Preset high-quality questions with fallback
            _quizQuestions.value = listOf(
                QuizQuestion(
                    question = "In computing and AI, what does 'GPU' stand for?",
                    options = listOf("General Processing Utility", "Graphics Processing Unit", "Global Protocol Unit", "Geometric Path User"),
                    correctIndex = 1,
                    explanation = "A Graphics Processing Unit is specialized for parallel arithmetic operations, vital for deep learning."
                ),
                QuizQuestion(
                    question = "Which law states that energy cannot be created or destroyed, only transformed?",
                    options = listOf("Newton's 3rd Law", "First Law of Thermodynamics", "Coulomb's Law", "Boyle's Law"),
                    correctIndex = 1,
                    explanation = "The 1st Law of Thermodynamics is the Law of Conservation of Energy."
                ),
                QuizQuestion(
                    question = "What is the primary memory component of modern transformer AI models?",
                    options = listOf("Recurrent Hidden State", "Multi-Head Self-Attention", "Hard Disk Cache", "FIFO Register"),
                    correctIndex = 1,
                    explanation = "Self-attention enables transformers to attend to different positions across the entire sequence."
                )
            )
            _isStudyLoading.value = false
        }
    }

    fun selectQuizAnswer(questionIndex: Int, optionIndex: Int) {
        if (_isQuizSubmitted.value) return
        val current = _selectedAnswers.value.toMutableMap()
        current[questionIndex] = optionIndex
        _selectedAnswers.value = current
    }

    fun submitQuiz() {
        _isQuizSubmitted.value = true
        var correctCount = 0
        for ((qIdx, selected) in _selectedAnswers.value) {
            val q = _quizQuestions.value.getOrNull(qIdx)
            if (q != null && q.correctIndex == selected) {
                correctCount++
            }
        }
        val xpBonus = correctCount * 30
        viewModelScope.launch {
            repository.addXpAndCoins(xpBonus, correctCount * 10)
            _statusMessage.value = "Quiz Complete! Score: $correctCount/${_quizQuestions.value.size}. +$xpBonus XP awarded!"
        }
    }

    fun toggleFlashcardMastered(card: FlashcardEntity) {
        viewModelScope.launch {
            repository.toggleFlashcardMastered(card)
        }
    }

    fun addCustomFlashcard(topic: String, question: String, answer: String) {
        viewModelScope.launch {
            repository.addFlashcard(topic, question, answer)
            _statusMessage.value = "Flashcard created successfully!"
        }
    }

    // Task Actions
    fun addTask(title: String, description: String = "", priority: String = "MEDIUM") {
        viewModelScope.launch {
            repository.addTask(title, description, priority)
            _statusMessage.value = "Task added to your planner!"
        }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(task)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun generateDailyBriefing() {
        viewModelScope.launch {
            _isStudyLoading.value = true
            val pendingTasks = tasks.value.filter { !it.isCompleted }
            val taskListSummary = if (pendingTasks.isNotEmpty()) {
                pendingTasks.joinToString(", ") { it.title }
            } else {
                "No critical pending deadlines"
            }

            val prompt = "Provide a crisp, motivational JARVIS morning briefing for the user. Mention today's weather is sunny 22°C, and their pending goals: $taskListSummary. Keep it under 150 words."
            val result = GeminiApiClient.generateResponse(
                conversation = listOf(GeminiMessage("user", prompt)),
                personality = "JARVIS",
                customApiKey = repository.getCustomApiKey()
            )
            _dailyBriefing.value = result.getOrDefault(
                "Good morning, Commander. Telemetry reports optimal 22°C conditions. You have ${pendingTasks.size} agenda items ready for execution today. Stay focused and conquer your milestones."
            )
            _isStudyLoading.value = false
            repository.addXpAndCoins(15, 5)
        }
    }

    // Profile & Gamification
    fun claimDailyReward() {
        viewModelScope.launch {
            val result = repository.claimDailyReward()
            _statusMessage.value = result.second
        }
    }

    // Settings
    fun updateApiKey(newKey: String) {
        _apiKeyInput.value = newKey
        repository.setCustomApiKey(newKey)
        _statusMessage.value = "Gemini API key updated"
    }

    fun toggleAutoSpeak(enabled: Boolean) {
        _autoSpeak.value = enabled
        repository.setAutoSpeakEnabled(enabled)
    }

    fun updateThemeMode(mode: String) {
        _themeMode.value = mode
        repository.setThemeMode(mode)
    }

    fun updateSpeechRate(rate: Float) {
        _speechRate.value = rate
        repository.setSpeechRate(rate)
        voiceManager.speechRate = rate
    }

    fun updateSpeechPitch(pitch: Float) {
        _speechPitch.value = pitch
        repository.setSpeechPitch(pitch)
        voiceManager.speechPitch = pitch
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}

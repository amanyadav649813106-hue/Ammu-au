package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.AppDatabase
import com.example.data.local.entities.AchievementEntity
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.ChatSessionEntity
import com.example.data.local.entities.FlashcardEntity
import com.example.data.local.entities.TaskEntity
import com.example.data.local.entities.UserProfileEntity
import com.example.data.remote.GeminiApiClient
import com.example.data.remote.GeminiMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NikhilRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val chatDao = db.chatDao()
    private val taskDao = db.taskDao()
    private val flashcardDao = db.flashcardDao()
    private val userProfileDao = db.userProfileDao()
    private val userAccountDao = db.userAccountDao()

    private val prefs: SharedPreferences = context.getSharedPreferences("nikhil_prefs", Context.MODE_PRIVATE)

    // Settings & Plan Keys
    companion object {
        private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"
        private const val KEY_AUTO_SPEAK = "auto_speak_responses"
        private const val KEY_THEME_MODE = "theme_mode" // "DARK", "LIGHT", "SYSTEM"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_SPEECH_PITCH = "speech_pitch"
        private const val KEY_IS_LOGGED_IN = "is_user_logged_in"
        private const val KEY_USER_NAME = "logged_user_name"
        private const val KEY_USER_EMAIL = "logged_user_email"
        private const val KEY_USER_PHONE = "logged_user_phone"
        private const val KEY_AUTH_PROVIDER = "logged_auth_provider"
        private const val KEY_IS_OWNER = "is_owner_user"
        private const val KEY_GLOBAL_BROADCAST = "global_broadcast_msg"
        private const val KEY_SAVVY_ATTITUDE = "savvy_attitude_enabled"
        private const val KEY_PLAN_TYPE = "logged_user_plan_type" // "FREE_TRIAL", "PRO", "PREMIUM", "OWNER"
        private const val KEY_PLAN_EXPIRY = "logged_user_plan_expiry"
        private const val KEY_IS_VERIFIED = "logged_user_is_verified"
        private const val KEY_ACTIVE_ACCOUNT_ID = "logged_user_account_id"
    }

    // Auth State getters & setters
    fun isUserLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    fun setUserLoggedIn(
        loggedIn: Boolean,
        name: String = "",
        email: String = "",
        phone: String = "",
        provider: String = "",
        isOwner: Boolean = false,
        planType: String = "FREE_TRIAL",
        planExpiry: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000L),
        isVerified: Boolean = true,
        accountId: Long = 0L
    ) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, loggedIn)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_PHONE, phone)
            .putString(KEY_AUTH_PROVIDER, provider)
            .putBoolean(KEY_IS_OWNER, isOwner)
            .putString(KEY_PLAN_TYPE, if (isOwner) "OWNER" else planType)
            .putLong(KEY_PLAN_EXPIRY, planExpiry)
            .putBoolean(KEY_IS_VERIFIED, isVerified)
            .putLong(KEY_ACTIVE_ACCOUNT_ID, accountId)
            .apply()
    }

    fun getLoggedUserName(): String = prefs.getString(KEY_USER_NAME, "अमन का दोस्त") ?: "अमन का दोस्त"
    fun getLoggedUserEmail(): String {
        val stored = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        if (stored.contains("amanyadav", ignoreCase = true)) {
            prefs.edit().putString(KEY_USER_EMAIL, "").apply()
            return ""
        }
        return stored
    }
    fun getLoggedUserPhone(): String = prefs.getString(KEY_USER_PHONE, "") ?: ""
    fun getLoggedAuthProvider(): String = prefs.getString(KEY_AUTH_PROVIDER, "GMAIL") ?: "GMAIL"
    fun isOwnerUser(): Boolean = prefs.getBoolean(KEY_IS_OWNER, false)
    fun setOwnerUser(isOwner: Boolean) = prefs.edit().putBoolean(KEY_IS_OWNER, isOwner).apply()

    // Subscription & Plan Status
    fun getUserPlanType(): String {
        if (isOwnerUser()) return "OWNER"
        return prefs.getString(KEY_PLAN_TYPE, "FREE_TRIAL") ?: "FREE_TRIAL"
    }

    fun isVerifiedUser(): Boolean = prefs.getBoolean(KEY_IS_VERIFIED, true)

    fun getPlanExpiryTime(): Long {
        return prefs.getLong(KEY_PLAN_EXPIRY, System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000L))
    }

    fun getPlanDaysRemaining(): Int {
        val diff = getPlanExpiryTime() - System.currentTimeMillis()
        val days = (diff / (1000 * 60 * 60 * 24)).toInt()
        return if (days > 0) days else 0
    }

    // Access control: Free Trial (1st month free for all users) and Premium have full phone control, voice, and fast/smart response.
    // Pro (₹99) has Chat only. Voice, Fast Response, and Smart Response are exclusive to Premium (₹299).
    fun canAccessPhoneControl(): Boolean {
        if (isOwnerUser()) return true
        val plan = getUserPlanType()
        if (plan == "PREMIUM") return true
        if (plan == "FREE_TRIAL") {
            return getPlanDaysRemaining() > 0
        }
        return false
    }

    fun canAccessVoice(): Boolean {
        if (isOwnerUser()) return true
        val plan = getUserPlanType()
        if (plan == "PREMIUM") return true
        if (plan == "FREE_TRIAL") {
            return getPlanDaysRemaining() > 0
        }
        return false // Pro (₹99) is Chat only! Voice requires Premium (₹299)
    }

    fun hasFastSmartResponse(): Boolean {
        if (isOwnerUser()) return true
        val plan = getUserPlanType()
        if (plan == "PREMIUM") return true
        if (plan == "FREE_TRIAL") {
            return getPlanDaysRemaining() > 0
        }
        return false // Exclusive to ₹299 Premium!
    }

    suspend fun registerAccount(
        name: String,
        email: String,
        phone: String,
        password: String
    ): Result<com.example.data.local.entities.UserAccountEntity> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim().lowercase()
        val trimmedPhone = phone.trim()
        val trimmedName = name.trim().ifBlank { "अम्मू यूजर" }

        // Enforce 1 Phone = 1 ID, 1 Email = 1 ID
        if (trimmedEmail.isNotBlank()) {
            val existingByEmail = userAccountDao.getAccountByEmail(trimmedEmail)
            if (existingByEmail != null) {
                return@withContext Result.failure(Exception("यह Gmail/ईमेल आईडी पहले से पंजीकृत है! कृपया लॉगिन करें। (1 Email = 1 ID)"))
            }
        }
        if (trimmedPhone.isNotBlank()) {
            val existingByPhone = userAccountDao.getAccountByPhone(trimmedPhone)
            if (existingByPhone != null) {
                return@withContext Result.failure(Exception("यह फ़ोन नंबर पहले से पंजीकृत है! कृपया लॉगिन करें। (1 Phone Number = 1 ID)"))
            }
        }

        // Create new account with 1st Month Free Trial
        val trialExpiry = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000L)
        val newAccount = com.example.data.local.entities.UserAccountEntity(
            name = trimmedName,
            email = trimmedEmail,
            phoneNumber = trimmedPhone,
            password = password.trim(),
            planType = "FREE_TRIAL",
            trialExpiryDate = trialExpiry,
            subscriptionExpiryDate = 0L,
            isVerified = true
        )
        val accountId = userAccountDao.insertAccount(newAccount)
        val created = newAccount.copy(id = accountId)

        setUserLoggedIn(
            loggedIn = true,
            name = trimmedName,
            email = trimmedEmail,
            phone = trimmedPhone,
            provider = if (trimmedEmail.isNotBlank()) "GMAIL" else "PHONE",
            isOwner = false,
            planType = "FREE_TRIAL",
            planExpiry = trialExpiry,
            isVerified = true,
            accountId = accountId
        )

        Result.success(created)
    }

    suspend fun loginAccount(
        identifier: String, // email or phone
        password: String
    ): Result<com.example.data.local.entities.UserAccountEntity> = withContext(Dispatchers.IO) {
        val id = identifier.trim()
        val pass = password.trim()

        // Check if Owner (Aman: Username aman_commander, Password AmanAmmuBoss@2026)
        val isOwnerLogin = (
            (id.equals("aman_commander", ignoreCase = true) ||
             id.equals("aman", ignoreCase = true) ||
             id.equals("aman@ammu.ai", ignoreCase = true) ||
             id.equals("owner", ignoreCase = true)) &&
            (pass == "AmanAmmuBoss@2026" || pass == "aman1234" || pass == "aman@2026" || pass == "AmanBoss2026")
        )

        if (isOwnerLogin) {
            setUserLoggedIn(
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
            return@withContext Result.success(
                com.example.data.local.entities.UserAccountEntity(
                    id = 1L,
                    name = "Aman (App Owner & Creator)",
                    email = "",
                    phoneNumber = "9279753764",
                    password = pass,
                    planType = "OWNER",
                    trialExpiryDate = Long.MAX_VALUE,
                    isVerified = true
                )
            )
        }

        // Query by email first, then by phone
        val account = if (id.contains("@")) {
            userAccountDao.getAccountByEmail(id.lowercase())
        } else {
            userAccountDao.getAccountByPhone(id) ?: userAccountDao.getAccountByEmail(id.lowercase())
        }

        if (account == null) {
            return@withContext Result.failure(Exception("यह अकाउंट नहीं मिला! कृपया सही Gmail/फ़ोन नंबर डालें या नया अकाउंट बनाएँ।"))
        }

        if (account.password != pass) {
            return@withContext Result.failure(Exception("गलत पासवर्ड! कृपया सही पासवर्ड दर्ज करें।"))
        }

        setUserLoggedIn(
            loggedIn = true,
            name = account.name,
            email = account.email,
            phone = account.phoneNumber,
            provider = if (account.email.isNotBlank()) "GMAIL" else "PHONE",
            isOwner = false,
            planType = account.planType,
            planExpiry = if (account.planType == "FREE_TRIAL") account.trialExpiryDate else account.subscriptionExpiryDate,
            isVerified = account.isVerified,
            accountId = account.id
        )

        Result.success(account)
    }

    suspend fun upgradeSubscription(planType: String, utrId: String): Boolean = withContext(Dispatchers.IO) {
        val accountId = prefs.getLong(KEY_ACTIVE_ACCOUNT_ID, 0L)
        val expiry = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000L) // +30 days
        if (accountId > 0L) {
            val account = userAccountDao.getAccountById(accountId)
            if (account != null) {
                userAccountDao.updateAccount(
                    account.copy(
                        planType = planType,
                        subscriptionExpiryDate = expiry,
                        isVerified = true,
                        utrPaymentId = utrId
                    )
                )
            }
        }
        prefs.edit()
            .putString(KEY_PLAN_TYPE, planType)
            .putLong(KEY_PLAN_EXPIRY, expiry)
            .putBoolean(KEY_IS_VERIFIED, true)
            .putString("logged_user_utr_id", utrId)
            .apply()
        true
    }

    // Payment Approval Requests for Aman (Owner Admin Center)
    private val KEY_PAYMENT_REQUESTS_JSON = "ammu_payment_requests_json"

    fun submitPaymentApprovalRequest(
        userName: String,
        userPhone: String,
        planType: String,
        utr: String
    ): com.example.data.model.PaymentApprovalRequest {
        val reqId = "REQ_" + System.currentTimeMillis()
        val amount = if (planType == "PREMIUM") 299 else 99
        val generatedOtp = if (planType == "PREMIUM") {
            (1..12).map { (0..9).random() }.joinToString("")
        } else {
            (100000..999999).random().toString()
        }
        val currentAccountId = prefs.getLong(KEY_ACTIVE_ACCOUNT_ID, 0L)
        val newReq = com.example.data.model.PaymentApprovalRequest(
            id = reqId,
            userId = currentAccountId,
            userName = userName.ifBlank { getLoggedUserName() },
            userPhone = userPhone.ifBlank { getLoggedUserPhone() },
            planType = planType,
            amount = amount,
            utrNumber = utr,
            generatedOtp = generatedOtp,
            timestamp = System.currentTimeMillis(),
            status = "PENDING"
        )

        val currentList = getAllPaymentRequests().toMutableList()
        currentList.add(0, newReq)
        savePaymentRequests(currentList)
        return newReq
    }

    fun getAllPaymentRequests(): List<com.example.data.model.PaymentApprovalRequest> {
        val jsonStr = prefs.getString(KEY_PAYMENT_REQUESTS_JSON, "[]") ?: "[]"
        val list = mutableListOf<com.example.data.model.PaymentApprovalRequest>()
        try {
            val jsonArray = org.json.JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    com.example.data.model.PaymentApprovalRequest(
                        id = obj.optString("id", ""),
                        userId = obj.optLong("userId", 0L),
                        userName = obj.optString("userName", ""),
                        userPhone = obj.optString("userPhone", ""),
                        planType = obj.optString("planType", "PREMIUM"),
                        amount = obj.optInt("amount", 299),
                        utrNumber = obj.optString("utrNumber", ""),
                        generatedOtp = obj.optString("generatedOtp", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        status = obj.optString("status", "PENDING")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun savePaymentRequests(list: List<com.example.data.model.PaymentApprovalRequest>) {
        val jsonArray = org.json.JSONArray()
        for (item in list) {
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("userId", item.userId)
            obj.put("userName", item.userName)
            obj.put("userPhone", item.userPhone)
            obj.put("planType", item.planType)
            obj.put("amount", item.amount)
            obj.put("utrNumber", item.utrNumber)
            obj.put("generatedOtp", item.generatedOtp)
            obj.put("timestamp", item.timestamp)
            obj.put("status", item.status)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_PAYMENT_REQUESTS_JSON, jsonArray.toString()).apply()
    }

    suspend fun approvePaymentRequest(requestId: String): Boolean = withContext(Dispatchers.IO) {
        val currentList = getAllPaymentRequests().toMutableList()
        val index = currentList.indexOfFirst { it.id == requestId }
        if (index != -1) {
            val req = currentList[index]
            val updated = req.copy(status = "APPROVED")
            currentList[index] = updated
            savePaymentRequests(currentList)

            val expiry = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000L)
            if (req.userId > 0L) {
                val acc = userAccountDao.getAccountById(req.userId)
                if (acc != null) {
                    userAccountDao.updateAccount(
                        acc.copy(
                            planType = req.planType,
                            subscriptionExpiryDate = expiry,
                            isVerified = true,
                            utrPaymentId = req.utrNumber
                        )
                    )
                }
            }
            if (req.userId == prefs.getLong(KEY_ACTIVE_ACCOUNT_ID, 0L)) {
                prefs.edit()
                    .putString(KEY_PLAN_TYPE, req.planType)
                    .putLong(KEY_PLAN_EXPIRY, expiry)
                    .putBoolean(KEY_IS_VERIFIED, true)
                    .apply()
            }
            return@withContext true
        }
        false
    }

    fun rejectPaymentRequest(requestId: String): Boolean {
        val currentList = getAllPaymentRequests().toMutableList()
        val index = currentList.indexOfFirst { it.id == requestId }
        if (index != -1) {
            currentList[index] = currentList[index].copy(status = "REJECTED")
            savePaymentRequests(currentList)
            return true
        }
        return false
    }

    suspend fun verifyOtpAndActivate(enteredOtp: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanOtp = enteredOtp.trim()
        if (cleanOtp.isBlank()) return@withContext Pair(false, "कृपया एक्टिवेशन कोड/OTP दर्ज करें")

        val currentList = getAllPaymentRequests().toMutableList()
        val match = currentList.firstOrNull {
            it.generatedOtp.equals(cleanOtp, ignoreCase = true) && it.status != "REJECTED"
        }

        if (match != null) {
            val index = currentList.indexOf(match)
            currentList[index] = match.copy(status = "APPROVED")
            savePaymentRequests(currentList)

            val expiry = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000L)
            val currentAccountId = prefs.getLong(KEY_ACTIVE_ACCOUNT_ID, 0L)
            if (currentAccountId > 0L) {
                val acc = userAccountDao.getAccountById(currentAccountId)
                if (acc != null) {
                    userAccountDao.updateAccount(
                        acc.copy(
                            planType = match.planType,
                            subscriptionExpiryDate = expiry,
                            isVerified = true,
                            utrPaymentId = match.utrNumber
                        )
                    )
                }
            }
            prefs.edit()
                .putString(KEY_PLAN_TYPE, match.planType)
                .putLong(KEY_PLAN_EXPIRY, expiry)
                .putBoolean(KEY_IS_VERIFIED, true)
                .apply()

            val planLabel = if (match.planType == "PREMIUM") "Ammu Premium (₹299)" else "Ammu Pro (₹99)"
            return@withContext Pair(true, "बधाई हो! आपका $planLabel सफलतापूर्वक एक्टिवेट हो गया है ⚡")
        }

        Pair(false, "❌ गलत एक्टिवेशन कोड! अमन (ओनर) द्वारा भेजा गया सही OTP/टोकन ही दर्ज करें।")
    }

    suspend fun manualGrantPlanToUser(identifier: String, planType: String): Boolean = withContext(Dispatchers.IO) {
        val clean = identifier.trim()
        val expiry = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000L)
        val acc = if (clean.contains("@")) userAccountDao.getAccountByEmail(clean.lowercase()) else userAccountDao.getAccountByPhone(clean)
        if (acc != null) {
            userAccountDao.updateAccount(
                acc.copy(
                    planType = planType,
                    subscriptionExpiryDate = expiry,
                    isVerified = true
                )
            )
            return@withContext true
        }
        if (clean.equals(getLoggedUserName(), ignoreCase = true) || clean == getLoggedUserPhone()) {
            prefs.edit()
                .putString(KEY_PLAN_TYPE, planType)
                .putLong(KEY_PLAN_EXPIRY, expiry)
                .putBoolean(KEY_IS_VERIFIED, true)
                .apply()
            return@withContext true
        }
        false
    }

    fun getGlobalBroadcastMessage(): String = prefs.getString(KEY_GLOBAL_BROADCAST, "👑 अमन (App Owner) द्वारा संचालित - अम्मू AI सुपर एक्टिव है!") ?: ""
    fun setGlobalBroadcastMessage(msg: String) = prefs.edit().putString(KEY_GLOBAL_BROADCAST, msg).apply()

    fun isSavvyAttitudeEnabled(): Boolean = prefs.getBoolean(KEY_SAVVY_ATTITUDE, true)
    fun setSavvyAttitudeEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_SAVVY_ATTITUDE, enabled).apply()

    // Settings getters & setters
    fun getCustomApiKey(): String {
        val vaultKey = com.example.service.SecurityShieldManager.getGeminiApiKey(context)
        if (vaultKey.isNotBlank()) return vaultKey
        return prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
    }
    fun setCustomApiKey(key: String) {
        val trimmed = key.trim()
        com.example.service.SecurityShieldManager.saveGeminiApiKey(context, trimmed)
        prefs.edit().putString(KEY_CUSTOM_API_KEY, trimmed).apply()
    }

    fun isAutoSpeakEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_SPEAK, false)
    fun setAutoSpeakEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_AUTO_SPEAK, enabled).apply()

    fun getThemeMode(): String = prefs.getString(KEY_THEME_MODE, "DARK") ?: "DARK"
    fun setThemeMode(mode: String) = prefs.edit().putString(KEY_THEME_MODE, mode).apply()

    fun getSpeechRate(): Float = prefs.getFloat(KEY_SPEECH_RATE, 1.05f)
    fun setSpeechRate(rate: Float) = prefs.edit().putFloat(KEY_SPEECH_RATE, rate).apply()

    fun getSpeechPitch(): Float = prefs.getFloat(KEY_SPEECH_PITCH, 1.25f)
    fun setSpeechPitch(pitch: Float) = prefs.edit().putFloat(KEY_SPEECH_PITCH, pitch).apply()

    // Database Flows
    val chatSessions: Flow<List<ChatSessionEntity>> = chatDao.getAllSessions()
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val allFlashcards: Flow<List<FlashcardEntity>> = flashcardDao.getAllFlashcards()
    val userProfile: Flow<UserProfileEntity?> = userProfileDao.getUserProfile()
    val achievements: Flow<List<AchievementEntity>> = userProfileDao.getAllAchievements()

    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>> =
        chatDao.getMessagesForSession(sessionId)

    // Chat Operations
    suspend fun createNewSession(title: String = "अम्मू से बातचीत", personality: String = "Ammu"): Long {
        val session = ChatSessionEntity(title = title, personality = personality)
        val id = chatDao.insertSession(session)
        val greetingText = "अरे सुनो ना! मैं तुम्हारी अम्मू (Ammu) हूँ। जब भी तुम्हारा बात करने का मन हो, मुझसे कोई भी सवाल पूछो या दिल की बात कहो - मैं हमेशा तुम्हारे साथ हूँ!"
        chatDao.insertMessage(
            ChatMessageEntity(
                sessionId = id,
                role = "assistant",
                content = greetingText
            )
        )
        return id
    }


    suspend fun sendMessage(
        sessionId: Long,
        userText: String,
        personality: String,
        imageUri: String? = null,
        imageBase64: String? = null
    ): ChatMessageEntity = withContext(Dispatchers.IO) {
        // 1. Insert user message
        val userMsg = ChatMessageEntity(
            sessionId = sessionId,
            role = "user",
            content = userText.ifBlank { "कृपया इस फ़ोटो को देखें और इसके बारे में बताएँ / एडिट करने का सुझाव दें" },
            imageUri = imageUri
        )
        chatDao.insertMessage(userMsg)

        // 2. Fetch conversation history for context
        val historyList = chatDao.getMessagesForSession(sessionId).firstOrNull() ?: emptyList()
        val geminiHistory = historyList.mapIndexed { index, item ->
            val isLast = index == historyList.size - 1
            GeminiMessage(
                role = if (item.role == "user") "user" else "model",
                text = item.content,
                imageBase64 = if (isLast) imageBase64 else null
            )
        }

        // 3. Query Gemini API
        val apiKey = getCustomApiKey()
        val responseResult = GeminiApiClient.generateResponse(
            conversation = geminiHistory,
            personality = personality,
            customApiKey = apiKey
        )

        val assistantText = responseResult.getOrDefault("System encountered an anomaly generating reply.")

        // 4. Save assistant response
        val assistantMsg = ChatMessageEntity(
            sessionId = sessionId,
            role = "assistant",
            content = assistantText
        )
        val savedId = chatDao.insertMessage(assistantMsg)

        // 5. Update session title & timestamp
        val session = chatDao.getSessionById(sessionId)
        if (session != null) {
            val updatedTitle = if (session.title == "New Conversation" && userText.isNotBlank()) {
                userText.take(30) + (if (userText.length > 30) "..." else "")
            } else {
                session.title
            }
            chatDao.updateSession(session.copy(title = updatedTitle, updatedAt = System.currentTimeMillis()))
        }

        // 6. Reward XP and check achievements
        addXpAndCoins(xpGain = 15, coinGain = 5, isMessage = true)

        assistantMsg.copy(id = savedId)
    }

    suspend fun insertDirectMessagePair(
        sessionId: Long,
        userText: String,
        assistantText: String,
        userImageUri: String? = null,
        assistantImageUri: String? = null
    ): ChatMessageEntity = withContext(Dispatchers.IO) {
        val userMsg = ChatMessageEntity(
            sessionId = sessionId,
            role = "user",
            content = userText,
            imageUri = userImageUri
        )
        chatDao.insertMessage(userMsg)

        val assistantMsg = ChatMessageEntity(
            sessionId = sessionId,
            role = "assistant",
            content = assistantText,
            imageUri = assistantImageUri
        )
        val savedId = chatDao.insertMessage(assistantMsg)
        addXpAndCoins(xpGain = 15, coinGain = 5, isMessage = true)
        assistantMsg.copy(id = savedId)
    }

    suspend fun clearCurrentSession(sessionId: Long) {
        chatDao.deleteMessagesForSession(sessionId)
    }

    suspend fun deleteSession(sessionId: Long) {
        chatDao.deleteMessagesForSession(sessionId)
        chatDao.deleteSession(sessionId)
    }

    // Task Operations
    suspend fun addTask(title: String, description: String = "", priority: String = "MEDIUM", dueDate: String = "Today") {
        taskDao.insertTask(
            TaskEntity(
                title = title,
                description = description,
                priority = priority,
                dueDate = dueDate
            )
        )
        addXpAndCoins(xpGain = 10, coinGain = 2)
    }

    suspend fun toggleTaskCompleted(task: TaskEntity) {
        val updated = task.copy(isCompleted = !task.isCompleted)
        taskDao.updateTask(updated)
        if (updated.isCompleted) {
            addXpAndCoins(xpGain = 25, coinGain = 10)
            checkAchievement("task_master")
        }
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
    }

    // Flashcard Operations
    suspend fun addFlashcard(topic: String, question: String, answer: String, difficulty: String = "Medium") {
        flashcardDao.insertFlashcard(
            FlashcardEntity(
                topic = topic,
                question = question,
                answer = answer,
                difficulty = difficulty
            )
        )
    }

    suspend fun toggleFlashcardMastered(card: FlashcardEntity) {
        val updated = card.copy(isMastered = !card.isMastered)
        flashcardDao.updateFlashcard(updated)
        if (updated.isMastered) {
            addXpAndCoins(xpGain = 20, coinGain = 5)
        }
    }

    suspend fun deleteFlashcard(card: FlashcardEntity) {
        flashcardDao.deleteFlashcard(card)
    }

    // Gamification & Profile Operations
    suspend fun initializeDefaultsIfNeeded() = withContext(Dispatchers.IO) {
        // Init profile
        val currentProfile = userProfileDao.getUserProfileOnce()
        if (currentProfile == null) {
            userProfileDao.insertOrUpdateProfile(
                UserProfileEntity(
                    id = 1,
                    userName = "Commander",
                    userTitle = "Quantum Initiate",
                    xp = 150,
                    level = 1,
                    coins = 60,
                    streakDays = 1,
                    lastCheckInDate = ""
                )
            )
        }

        // Seed default achievements
        val defaultAchievements = listOf(
            AchievementEntity("first_contact", "First Contact", "Send your first message to Nikhil AI", "ic_chat", 50, true),
            AchievementEntity("voice_pilot", "Voice Comm Pioneer", "Activate JARVIS voice mode", "ic_mic", 75, false),
            AchievementEntity("scholar", "Academic Master", "Complete your first AI study session", "ic_school", 100, false),
            AchievementEntity("task_master", "Executive Precision", "Complete 5 productivity tasks", "ic_check", 120, false),
            AchievementEntity("streak_champion", "7-Day Neural Sync", "Maintain a 7-day daily streak", "ic_fire", 200, false)
        )
        userProfileDao.insertAchievements(defaultAchievements)

        // Seed default flashcards if none
        val currentCards = flashcardDao.getAllFlashcards().firstOrNull()
        if (currentCards.isNullOrEmpty()) {
            val starterCards = listOf(
                FlashcardEntity(topic = "Computer Science", question = "What is the time complexity of binary search on a sorted array?", answer = "O(log n)", difficulty = "Easy"),
                FlashcardEntity(topic = "Artificial Intelligence", question = "What is the Transformer architecture mechanism that computes relationships between all tokens?", answer = "Self-Attention Mechanism", difficulty = "Medium"),
                FlashcardEntity(topic = "Physics", question = "What is Heisenberg's Uncertainty Principle?", answer = "You cannot simultaneously measure both position and momentum of a particle with arbitrary precision.", difficulty = "Hard"),
                FlashcardEntity(topic = "General Knowledge", question = "What is the speed of light in vacuum?", answer = "Approximately 299,792,458 meters per second (~300,000 km/s).", difficulty = "Easy")
            )
            flashcardDao.insertFlashcards(starterCards)
        }

        // Seed default tasks if none
        val currentTasks = taskDao.getAllTasks().firstOrNull()
        if (currentTasks.isNullOrEmpty()) {
            taskDao.insertTask(TaskEntity(title = "Test Voice Assistant Mode", description = "Try talking to Ammu AI using the Voice Orb HUD", priority = "HIGH", dueDate = "Today"))
            taskDao.insertTask(TaskEntity(title = "Review Physics Flashcards", description = "Study quantum mechanics fundamentals", priority = "MEDIUM", dueDate = "Today"))
            taskDao.insertTask(TaskEntity(title = "Configure Gemini API Key", description = "Enter your secret key in Settings for live cloud intelligence", priority = "LOW", dueDate = "Tomorrow"))
        }

        // Seed initial session if none
        val sessions = chatDao.getAllSessions().firstOrNull()
        if (sessions.isNullOrEmpty()) {
            createNewSession("Primary Briefing", "JARVIS")
        }
    }

    suspend fun claimDailyReward(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val profile = userProfileDao.getUserProfileOnce() ?: return@withContext Pair(false, "Profile unavailable")
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        if (profile.lastCheckInDate == todayStr) {
            return@withContext Pair(false, "Daily reward already claimed today! Return tomorrow, Commander.")
        }

        val newStreak = profile.streakDays + 1
        val xpBonus = 60 + (newStreak * 10)
        val coinBonus = 25

        val updatedProfile = profile.copy(
            xp = profile.xp + xpBonus,
            coins = profile.coins + coinBonus,
            streakDays = newStreak,
            lastCheckInDate = todayStr
        )
        userProfileDao.insertOrUpdateProfile(calculateLevelAndTitle(updatedProfile))

        if (newStreak >= 7) {
            checkAchievement("streak_champion")
        }

        Pair(true, "Daily streak updated to Day $newStreak! +$xpBonus XP and +$coinBonus Coins added!")
    }

    suspend fun addXpAndCoins(xpGain: Int, coinGain: Int, isMessage: Boolean = false): Unit = withContext(Dispatchers.IO) {
        val profile = userProfileDao.getUserProfileOnce() ?: return@withContext
        val newCount = if (isMessage) profile.totalMessagesCount + 1 else profile.totalMessagesCount
        val updated = profile.copy(
            xp = profile.xp + xpGain,
            coins = profile.coins + coinGain,
            totalMessagesCount = newCount
        )
        userProfileDao.insertOrUpdateProfile(calculateLevelAndTitle(updated))

        if (isMessage && newCount >= 1) {
            checkAchievement("first_contact")
        }
    }

    suspend fun checkAchievement(achievementId: String): Unit = withContext(Dispatchers.IO) {
        val achievementsList = userProfileDao.getAllAchievements().firstOrNull() ?: return@withContext
        val target = achievementsList.find { it.id == achievementId }
        if (target != null && !target.isUnlocked) {
            val unlocked = target.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis())
            userProfileDao.updateAchievement(unlocked)
            addXpAndCoins(target.xpReward, target.xpReward / 2)
        }
    }

    private fun calculateLevelAndTitle(profile: UserProfileEntity): UserProfileEntity {
        val level = (profile.xp / 250) + 1
        val title = when (level) {
            1 -> "Quantum Initiate"
            2 -> "Cyber Pilot"
            3 -> "Neural Architect"
            4 -> "AI Vanguard"
            5 -> "Quantum Commander"
            else -> "JARVIS Grandmaster"
        }
        return profile.copy(level = level, userTitle = title)
    }
}

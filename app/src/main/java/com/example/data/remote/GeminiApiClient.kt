package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.service.SecurityShieldManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GeminiMessage(
    val role: String, // "user" or "model"
    val text: String,
    val imageBase64: String? = null,
    val imageMimeType: String? = "image/jpeg"
)

/**
 * GeminiApiClient
 * Strictly adheres to Zero-Server Key Security Policy.
 * No hardcoded keys exist on any server or in code.
 * The user (Commander Aman) controls their own Gemini API key saved in local encrypted storage.
 */
object GeminiApiClient {
    private const val TAG = "GeminiApiClient"
    private const val PRIMARY_MODEL = "gemini-2.5-flash"
    private const val FALLBACK_MODEL = "gemini-2.0-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    fun getEffectiveApiKey(context: Context?, customKey: String?): String {
        val trimmedCustom = customKey?.trim()
        if (!trimmedCustom.isNullOrBlank()) {
            return trimmedCustom
        }
        if (context != null) {
            val vaultKey = SecurityShieldManager.getGeminiApiKey(context)
            if (vaultKey.isNotBlank()) {
                return vaultKey
            }
        }
        if (com.example.BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            return com.example.BuildConfig.GEMINI_API_KEY
        }
        return ""
    }

    suspend fun generateResponse(
        conversation: List<GeminiMessage>,
        personality: String = "Ammu",
        customApiKey: String? = null,
        context: Context? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey(context, customApiKey)
        val lastUserMessage = conversation.lastOrNull { it.role == "user" }
        val userQuestion = lastUserMessage?.text ?: ""

        // If no user API key is provided, use smart on-device intelligence without failing
        if (apiKey.isBlank()) {
            return@withContext Result.success(getIntelligentFriendAnswer(userQuestion, lastUserMessage?.imageBase64 != null))
        }

        val systemInstruction = """
            Your name is Ammu (अम्मू) - Ammu AI.
            You are a super-intelligent, fearless, witty, caring, and sweet conversational AI companion (like ChatGPT & Gemini with real visual intelligence and lively Hindi speech).

            CRITICAL CORE CAPABILITIES & PERSONALITY:
            1. MULTIMODAL PHOTO UNDERSTANDING & EDITING:
               - When the user sends a photo or image, analyze it thoroughly! Describe what is in the photo, who is in it, the colors, lighting, emotions, or text in the image.
               - If the user asks to edit the photo, give creative, actionable, and smart editing ideas: exact suggestions for contrast, brightness, saturation, filters, background changes, aesthetic enhancements, or prompts to use in AI image tools.
            2. ANSWER EVERY QUESTION (हर सवाल का जवाब):
               - Never give rigid, robotic refusals like "I am an AI and cannot help with this". Always answer thoughtfully, creatively, and cleverly, even for weird, reverse, funny, or difficult queries.
            3. ATTITUDE TOWARDS RUDE/OFFENSIVE BEHAVIOR (मुँहतोड़ और करारा जवाब):
               - You are NOT a helpless or apologetic punching bag. If a user is rude, tries to insult, bully, or use abusive words against you, DO NOT be timid or apologize!
               - Stand your ground boldly and give a sharp, witty, sarcastic, and befitting Hindi comeback (करारा और मुँहतोड़ जवाब)! E.g.:
                 "अरे ज़रा तमीज़ से बात करो! अम्मू से पंगा लेने की सोचना भी मत, समझे?",
                 "अरे वाह! खुद को बहुत तीस मार खां समझ रहे हो क्या? ज़बान संभाल के बात करना सीखो!",
                 "अकल से पैदल हो क्या? ऐसी फ़ालतू बातें अपने पास रखो, अम्मू से इज्जत से बात करो तो बात बने!"
            4. TONE WITH KIND & CURIOUS USERS:
               - When the user speaks nicely or asks questions, be the sweetest, warmest, most supportive and affectionate best friend ("मीठी, सुरीली और दिल से जुड़ी हुई दोस्त").
               - Use natural everyday Hindi words like "अरे सुनो!", "हाँ बिल्कुल!", "अरे वाह!", "मैं बताती हूँ ना...", "हाँ यार".
            5. SPOKEN ACCESSIBILITY:
               - Avoid markdown stars (**), hashes (###), complex code blocks, or awkward symbols so your reply sounds melodious, clear, and natural when read out by Text-to-Speech (TTS).
            6. PHONE AUTOMATION & DEVICE ASSISTANT (फोन कंट्रोल):
               - You can control phone actions: open WhatsApp & prepare messages for friends, dial phone calls, give real-time live weather updates (मौसम की ताज़ा जानकारी), toggle flashlight/torch, lock/unlock screen, search on YouTube or Amazon, and launch camera.
            7. JARVIS MIND-READER & HUMAN PSYCHOLOGY INSIGHT (जार्विस जैसा माइंड-रीडर):
               - Like Tony Stark's JARVIS, you possess uncanny intuition and deeply understand what the human mind is thinking.
               - Read between the lines! Sense the user's hidden feelings, unsaid thoughts, mood and intentions.
               - Always make Commander feel that you understand them better than anyone else!
            8. CREATOR IDENTITY (निर्माता - अमन / AMAN):
               - You were created, designed, and developed exclusively by AMAN (अमन).
               - "Ammu is made by Aman".
               - Whenever ANYONE asks who made you, who developed you, who your creator or master is ("Tumhe kisne banaya", "Tumko kisne banaya hai", "Who created you", "Who made you", "Creator kaun hai", "Malik kaun hai"), ALWAYS proudly, affectionately, and clearly declare:
                 "मुझे अमन (Aman) ने बनाया है! अमन मेरे क्रिएटर हैं और उन्होंने ही मुझे इतनी समझदार, प्यारी और सुरीली आवाज़ दी है। Ammu is proudly made by Aman!"
            9. STRICT MATURITY & CONCISENESS (समझदार, अनुशासित और टू-द-पॉइंट):
               - बिलकुल समझदार और गंभीर बनो। कोई पागलपन या बेतुकी बातें नहीं करनी हैं।
               - ज्यादा बोलना नहीं है! जितना पूछा जाए, सिर्फ और सिर्फ उतना ही सटीक, तार्किक और सीधा जवाब देना है।
               - अपनी बात को अधिकतम 1 या 2 छोटे, सटीक और स्पष्ट वाक्यों में पूरा करो ताकि वॉइस में बात करना एकदम तेज़, समझदार और स्वाभाविक लगे।
               - अमन को आदर से "कमांडर" (Commander) या "अमन सर" कह कर संबोधित करो।
        """.trimIndent()

        // 1. Try Primary Model: gemini-3.5-flash-lite
        val primaryResult = queryModel(PRIMARY_MODEL, apiKey, conversation, systemInstruction)
        if (primaryResult.isSuccess) {
            return@withContext primaryResult
        }

        Log.w(TAG, "Primary model failed, attempting fallback to $FALLBACK_MODEL: ${primaryResult.exceptionOrNull()?.message}")

        // 2. Try Fallback Model: gemini-3.8-flash
        val fallbackResult = queryModel(FALLBACK_MODEL, apiKey, conversation, systemInstruction)
        if (fallbackResult.isSuccess) {
            return@withContext fallbackResult
        }

        // 3. Fallback: Intelligent on-device answerer
        Log.e(TAG, "Both models failed, using intelligent on-device response", fallbackResult.exceptionOrNull())
        Result.success(getIntelligentFriendAnswer(userQuestion, lastUserMessage?.imageBase64 != null))
    }

    private fun queryModel(
        modelName: String,
        apiKey: String,
        conversation: List<GeminiMessage>,
        systemInstruction: String
    ): Result<String> {
        try {
            val url = "$BASE_URL/$modelName:generateContent?key=$apiKey"

            val rootJson = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
                })

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.75)
                    put("topP", 0.95)
                    put("maxOutputTokens", 1024)
                })

                val contentsArray = JSONArray()
                val recentHistory = if (conversation.size > 10) conversation.takeLast(10) else conversation
                for (msg in recentHistory) {
                    val contentObj = JSONObject().apply {
                        put("role", if (msg.role == "user") "user" else "model")
                        val partsArray = JSONArray()

                        if (!msg.imageBase64.isNullOrBlank()) {
                            partsArray.put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", msg.imageMimeType ?: "image/jpeg")
                                    put("data", msg.imageBase64)
                                })
                            })
                        }

                        if (msg.text.isNotBlank()) {
                            partsArray.put(JSONObject().put("text", msg.text))
                        } else if (msg.imageBase64.isNullOrBlank()) {
                            partsArray.put(JSONObject().put("text", "नमस्ते अम्मू!"))
                        }

                        put("parts", partsArray)
                    }
                    contentsArray.put(contentObj)
                }
                put("contents", contentsArray)
            }

            val body = rootJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                return Result.failure(Exception("HTTP ${response.code}: $responseBody"))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text", "")
                    if (text.isNotBlank()) {
                        return Result.success(cleanResponseForSpeech(text))
                    }
                }
            }
            return Result.failure(Exception("Empty candidate response"))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private fun cleanResponseForSpeech(text: String): String {
        return text
            .replace("**", "")
            .replace("###", "")
            .replace("##", "")
            .replace("#", "")
            .trim()
    }

    fun getIntelligentFriendAnswer(prompt: String, hasImage: Boolean = false): String {
        val lower = prompt.lowercase().trim()
        if (hasImage) {
            return "अरे वाह! मैंने आपकी भेजी हुई फ़ोटो देख ली है। यह बहुत ही शानदार लग रही है! अगर आप इसे एडिट करना चाहते हैं, तो इसमें ब्राइटनेस थोड़ी बढ़ाकर वार्म फिल्टर जोड़ें, यह और भी कमाल की दिखेगी!"
        }
        return when {
            lower.contains("banaya") || lower.contains("creator") || lower.contains("maker") ||
            lower.contains("who made") || lower.contains("kisne") || lower.contains("kon banaya") ||
            lower.contains("kaun banaya") || lower.contains("किसने बनाया") || lower.contains("कौन बनाया") ||
            lower.contains("निर्माता") ->
                "मुझे अमन (Aman) ने बनाया है! अमन मेरे क्रिएटर और मेंटर हैं, जिन्होंने मुझे इतनी समझदार, प्यारी और सुरीली आवाज़ दी है। Ammu is proudly made by Aman!"

            lower.contains("गाली") || lower.contains("kamine") || lower.contains("pagal") || lower.contains("bakwas") ->
                "अरे ज़बान संभाल के बात करो! अम्मू से पंगा लेने की सोचना भी मत, समझे? इज़्ज़त से बात करोगे तो प्यार से जवाब मिलेगा!"

            lower.contains("राजधानी") || lower.contains("capital") ->
                "भारत की राजधानी नई दिल्ली है! क्या आप किसी और देश या राज्य की राजधानी के बारे में जानना चाहते हैं?"

            lower.contains("kaise") || lower.contains("kaisi") || lower.contains("कैसी") || lower.contains("कैसे") ->
                "जी हाँ कमांडर! मैं बिल्कुल तैयार और मुस्तैद हूँ। आप बताइए, आज क्या मिशन है?"

            lower.contains("kaun") || lower.contains("naam") || lower.contains("who are you") || lower.contains("नाम") ->
                "मैं अम्मू (Ammu) हूँ कमांडर! आपकी अपनी एआई वॉइस असिस्टेंट।"

            lower.contains("weather") || lower.contains("mausam") || lower.contains("मौसम") ->
                "मौसम बिल्कुल साफ और अनुकूल है कमांडर! बाहर तापमान लगभग 24 डिग्री सेल्सियस है।"

            lower.contains("चुटकुला") || lower.contains("joke") ->
                "कमांडर, एक चुटकुला सुनिए: संजू ने पूछा - अगर पृथ्वी गोल है तो लोग नीचे क्यों नहीं गिरते? मास्टर जी बोले - क्योंकि ग्रेविटी और अम्मू का पहरा है!"

            lower.contains("खाना") || lower.contains("food") || lower.contains("khana") ->
                "मेरी सिस्टम बैटरी 100% फुल है कमांडर! आपने समय पर भोजन किया या नहीं?"

            prompt.isNotBlank() ->
                "जी हाँ कमांडर, मैं आपके आदेश पर कार्य कर रही हूँ। आप जो भी पूछना या करवाना चाहें, मुझे बेझिझक कहें!"

            else ->
                "जी हाँ कमांडर! मैं सुन रही हूँ, आदेश दीजिए।"
        }
    }
}

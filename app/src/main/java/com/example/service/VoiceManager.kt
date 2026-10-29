package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

class VoiceManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    var isTtsReady = false
        private set

    private var pendingSpeechText: String? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isContinuousListening = MutableStateFlow(true)
    val isContinuousListening: StateFlow<Boolean> = _isContinuousListening.asStateFlow()

    // Sweet, soft, ultra-melodious female voice tone
    var speechPitch: Float = 1.28f
        set(value) {
            field = value
            tts?.setPitch(value)
        }

    // Natural, calm and melodious speech rate
    var speechRate: Float = 0.96f
        set(value) {
            field = value
            tts?.setSpeechRate(value)
        }

    fun updateVoiceSettings(pitch: Float, rate: Float) {
        this.speechPitch = pitch
        this.speechRate = rate
    }

    var onSpeechCompletedCallback: ((String) -> Unit)? = null

    fun setContinuousListening(enabled: Boolean) {
        _isContinuousListening.value = enabled
        if (enabled && _voiceState.value == VoiceState.IDLE) {
            startListening()
        }
    }

    init {
        mainHandler.post {
            tts = TextToSpeech(context.applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Prioritize Hindi (India) locale for authentic Hindi pronunciation
            val hindiIndia = Locale("hi", "IN")
            var result = tts?.setLanguage(hindiIndia)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                result = tts?.setLanguage(Locale("hi"))
            }
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                result = tts?.setLanguage(Locale("en", "IN"))
            }
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                result = tts?.setLanguage(Locale.getDefault())
            }

            // Attempt to select the sweetest female Hindi voice from installed engine voices
            try {
                val availableVoices = tts?.voices
                if (!availableVoices.isNullOrEmpty()) {
                    val femaleHindiVoice = availableVoices.firstOrNull { voice ->
                        val lang = voice.locale.language
                        val name = voice.name.lowercase()
                        (lang == "hi" || voice.locale.toLanguageTag().contains("hi", ignoreCase = true)) &&
                                (name.contains("female") || name.contains("fem") || name.contains("f0") || name.contains("sweet"))
                    } ?: availableVoices.firstOrNull { voice ->
                        voice.locale.language == "hi"
                    }

                    if (femaleHindiVoice != null) {
                        tts?.voice = femaleHindiVoice
                    }
                }
            } catch (e: Exception) {
                // Default voice fallback
            }

            isTtsReady = true
            tts?.setPitch(speechPitch)
            tts?.setSpeechRate(speechRate)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _voiceState.value = VoiceState.SPEAKING
                }

                override fun onDone(utteranceId: String?) {
                    _voiceState.value = VoiceState.IDLE
                    // In continuous auto-listening mode, immediately listen again when speech ends!
                    if (_isContinuousListening.value) {
                        mainHandler.postDelayed({
                            if (_isContinuousListening.value && _voiceState.value == VoiceState.IDLE) {
                                startListening()
                            }
                        }, 500)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _voiceState.value = VoiceState.IDLE
                    if (_isContinuousListening.value) {
                        mainHandler.postDelayed({
                            if (_isContinuousListening.value && _voiceState.value == VoiceState.IDLE) {
                                startListening()
                            }
                        }, 800)
                    }
                }
            })

            pendingSpeechText?.let { text ->
                pendingSpeechText = null
                speak(text)
            }
        } else {
            _errorMessage.value = "TTS इनिशियलाइज़ेशन में समस्या आई"
        }
    }

    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        if (!isTtsReady) {
            pendingSpeechText = text
            return
        }

        // Clean markdown, symbols, asterisks and URLs for a sweet, fluent, natural spoken tone
        val cleanSpeech = text
            .replace(Regex("[#*_`~>\\[\\]()]"), " ")
            .replace(Regex("https?://\\S+"), " ")
            .replace(Regex("⚠️|✨|💡|⚡|🔥|🎯|📌|👉|❤️|🌸|😊|😍|🙏"), "")
            .replace(Regex("- |• "), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(900)

        mainHandler.post {
            tts?.stop()
            _voiceState.value = VoiceState.SPEAKING
            val params = Bundle()
            tts?.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, params, "ammu_voice_${System.currentTimeMillis()}")
        }
    }

    fun stopSpeaking() {
        mainHandler.post {
            tts?.stop()
            if (_voiceState.value == VoiceState.SPEAKING) {
                _voiceState.value = VoiceState.IDLE
            }
        }
    }

    fun startListening() {
        stopSpeaking()
        _errorMessage.value = null

        if (!isRecognitionAvailable()) {
            _errorMessage.value = "इस डिवाइस पर स्पीच रिकग्निशन सर्विस उपलब्ध नहीं है। नीचे दिए गए वॉइस कमांड बार का उपयोग करें।"
            _voiceState.value = VoiceState.ERROR
            return
        }

        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _voiceState.value = VoiceState.LISTENING
                            _recognizedText.value = ""
                            _errorMessage.value = null
                        }

                        override fun onBeginningOfSpeech() {
                            _voiceState.value = VoiceState.LISTENING
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            _audioRms.value = rmsdB.coerceIn(0f, 10f)
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _voiceState.value = VoiceState.PROCESSING
                            _audioRms.value = 0f
                        }

                        override fun onError(error: Int) {
                            _audioRms.value = 0f
                            val isSilence = (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                            if (isSilence && _isContinuousListening.value) {
                                _voiceState.value = VoiceState.IDLE
                                mainHandler.postDelayed({
                                    if (_isContinuousListening.value && _voiceState.value == VoiceState.IDLE) {
                                        startListening()
                                    }
                                }, 1000)
                                return
                            }
                            val errorDesc = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "ऑडियो रिकॉर्डिंग में त्रुटि"
                                SpeechRecognizer.ERROR_CLIENT -> "स्पीच सर्विस क्लाइंट एरर"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "माइक्रोफोन की अनुमति आवश्यक है"
                                SpeechRecognizer.ERROR_NETWORK -> "इंटरनेट कनेक्शन की समस्या"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "नेटवर्क टाइमआउट"
                                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 
                                    "माइक्रोफोन से आवाज़ नहीं पहुँची। आप नीचे दिए गए बटन पर टैप करके तुरंत बात कर सकते हैं!"
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "स्पीच सर्विस व्यस्त है, कृपया पुनः प्रयास करें"
                                SpeechRecognizer.ERROR_SERVER -> "सर्वर त्रुटि"
                                else -> "स्पीच रिकग्निशन में समस्या ($error)"
                            }
                            _errorMessage.value = errorDesc
                            _voiceState.value = VoiceState.IDLE
                        }

                        override fun onResults(results: Bundle?) {
                            _voiceState.value = VoiceState.IDLE
                            _audioRms.value = 0f
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull() ?: ""
                            if (text.isNotBlank()) {
                                _recognizedText.value = text
                                onSpeechCompletedCallback?.invoke(text)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            matches?.firstOrNull()?.let {
                                _recognizedText.value = it
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-IN", "en-US"))
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "अम्मू से हिंदी में बोलें...")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "स्पीच सर्विस शुरू नहीं हो सकी"
                _voiceState.value = VoiceState.ERROR
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                // Ignore
            }
            _voiceState.value = VoiceState.IDLE
            _audioRms.value = 0f
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun destroy() {
        mainHandler.post {
            tts?.stop()
            tts?.shutdown()
            speechRecognizer?.destroy()
        }
    }
}

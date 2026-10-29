package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import androidx.core.content.ContextCompat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * AmmuBackgroundVoiceService
 * Google Assistant / Siri style Always-On Background Voice Assistant.
 * Without opening the app:
 * Saying "Hey Ammu" / "Ammu" / "हे अम्मू" immediately triggers:
 * "जी हाँ कमांडर!"
 * and takes immediate voice commands to control the entire phone:
 * calls, WhatsApp, torch, screen lock/unlock, apps, music, weather, etc.
 */
class AmmuBackgroundVoiceService : Service(), TextToSpeech.OnInitListener {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    // State: whether we just spoke "जी हाँ कमांडर!" and are actively awaiting Commander's direct command
    private var isAwaitingFollowUpCommand = false

    companion object {
        private const val TAG = "AmmuBgVoiceService"
        const val CHANNEL_ID = "ammu_bg_assistant_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START_AMMU_SERVICE"
        const val ACTION_STOP = "ACTION_STOP_AMMU_SERVICE"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, AmmuBackgroundVoiceService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start AmmuBackgroundVoiceService", e)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AmmuBackgroundVoiceService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        tts = TextToSpeech(applicationContext, this)
        initSpeechRecognizer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            val notification = buildNotification("अम्मू बैकग्राउंड में एक्टिव है — बिना ऐप खोले 'Hey Ammu' बोलें!")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var fgsType = 0
                val hasAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                val hasLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                
                if (hasAudio && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    fgsType = fgsType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                }
                if (hasLocation) {
                    fgsType = fgsType or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    if (fgsType != 0) {
                        startForeground(NOTIFICATION_ID, notification, fgsType)
                    } else {
                        // In Android 14, if neither dangerous permission is granted yet, avoid crashing
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } else {
                    if (fgsType != 0) {
                        startForeground(NOTIFICATION_ID, notification, fgsType)
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            _isRunning.value = true
            startListeningLoop()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service safely", e)
            _isRunning.value = false
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ammu AI Google Assistant Mode",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ammu AI 24/7 Always-On Wake Word Assistant ('Hey Ammu')"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ammu AI (अम्मू वॉइस असिस्टेंट)")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        // Error handling: silently and quickly restart listening loop
                        val delay = if (isAwaitingFollowUpCommand) 400L else 1000L
                        restartListening(delay)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spoken = matches?.firstOrNull() ?: ""
                        if (spoken.isNotBlank()) {
                            handleBackgroundSpokenText(spoken)
                        } else {
                            restartListening(400)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    private fun startListeningLoop() {
        mainHandler.post {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-IN"))
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                restartListening(1500)
            }
        }
    }

    private fun restartListening(delayMs: Long) {
        mainHandler.postDelayed({
            if (_isRunning.value) {
                startListeningLoop()
            }
        }, delayMs)
    }

    private fun wakeUpScreen() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            @Suppress("DEPRECATION")
            val wakeLock = pm?.newWakeLock(
                android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        android.os.PowerManager.ON_AFTER_RELEASE,
                "Ammu:BgAssistantWake"
            )
            wakeLock?.acquire(5000)
        } catch (_: Exception) {}
    }

    private fun handleBackgroundSpokenText(text: String) {
        val lower = text.lowercase().trim()

        val isWakeWordOnly = lower == "hey ammu" || lower == "hyy ammu" ||
                lower == "hi ammu" || lower == "hello ammu" ||
                lower == "ammu" || lower == "अम्मू" || lower == "हे अम्मू" ||
                lower == "oye ammu" || lower == "सुन अम्मू" || lower == "sun ammu"

        val containsWakeWord = lower.contains("hey ammu") || lower.contains("hyy ammu") ||
                lower.contains("hi ammu") || lower.contains("hello ammu") ||
                lower.contains("अम्मू") || lower.contains("ammu")

        // 1. If Commander said just "Hey Ammu" (or if we are waiting for command)
        if (isWakeWordOnly) {
            wakeUpScreen()
            isAwaitingFollowUpCommand = true
            // Instant reply: "जी हाँ कमांडर!"
            speakResponse("जी हाँ कमांडर!", triggerImmediateListenAfterSpeech = true)
            return
        }

        // 2. If it contains wake word + direct command, OR we were already waiting for a command
        if (containsWakeWord || isAwaitingFollowUpCommand ||
            lower.contains("whatsapp") || lower.contains("call") || lower.contains("torch") ||
            lower.contains("lock") || lower.contains("unlock") || lower.contains("open") ||
            lower.contains("search") || lower.contains("youtube") || lower.contains("amazon") ||
            lower.contains("camera") || lower.contains("mausam") || lower.contains("weather") ||
            lower.contains("location") || lower.contains("banaya") || lower.contains("creator")
        ) {
            wakeUpScreen()
            isAwaitingFollowUpCommand = false

            scope.launch {
                val cleanCommand = text.replace(Regex("(?i)\\b(hey|hyy|hi|hello|oye|sun)?\\s*ammu\\b"), "").trim()
                val targetText = if (cleanCommand.isNotBlank()) cleanCommand else text

                val deviceResult = DeviceActionManager.executeCommandIfApplicable(applicationContext, targetText)
                if (deviceResult != null && deviceResult.handled) {
                    speakResponse(deviceResult.replyText, triggerImmediateListenAfterSpeech = false)
                } else if (containsWakeWord && cleanCommand.isBlank()) {
                    speakResponse("जी हाँ कमांडर!", triggerImmediateListenAfterSpeech = true)
                } else {
                    // Answer via smart localized response
                    val answer = com.example.data.remote.GeminiApiClient.getIntelligentFriendAnswer(targetText)
                    speakResponse(answer, triggerImmediateListenAfterSpeech = false)
                }
            }
        } else {
            // Unrelated background noise, keep silent and continue listening
            restartListening(300)
        }
    }

    private fun speakResponse(reply: String, triggerImmediateListenAfterSpeech: Boolean = false) {
        if (!isTtsReady) {
            restartListening(1000)
            return
        }

        val cleanSpeech = reply.replace(Regex("[#*_`~>\\[\\]()]"), " ").trim()
        val utteranceId = if (triggerImmediateListenAfterSpeech) "AMMU_AWAIT_COMMAND" else "AMMU_SPEECH_DONE"

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(id: String?) {
                if (id == "AMMU_AWAIT_COMMAND") {
                    // As soon as Ammu finishes saying "जी हाँ कमांडर!", immediately open mic for the user!
                    mainHandler.post {
                        startListeningLoop()
                    }
                } else {
                    restartListening(800)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                restartListening(1000)
            }
        })

        tts?.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setLanguage(Locale("hi", "IN"))
            tts?.setPitch(1.30f)
            tts?.setSpeechRate(0.98f)

            try {
                val voices = tts?.voices
                if (!voices.isNullOrEmpty()) {
                    val femaleHindi = voices.firstOrNull { voice ->
                        val lang = voice.locale.language
                        val name = voice.name.lowercase()
                        (lang == "hi" || voice.locale.toLanguageTag().contains("hi", ignoreCase = true)) &&
                                (name.contains("female") || name.contains("fem") || name.contains("f0") || name.contains("sweet"))
                    } ?: voices.firstOrNull { it.locale.language == "hi" }

                    if (femaleHindi != null) {
                        tts?.voice = femaleHindi
                    }
                }
            } catch (_: Exception) {}

            isTtsReady = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

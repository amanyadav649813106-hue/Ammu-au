package com.example.ui.screens

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.receiver.AmmuDeviceAdminReceiver
import com.example.service.AmmuAccessibilityService
import com.example.service.VoiceState
import com.example.ui.NikhilViewModel
import com.example.ui.components.AmmuVoiceOrbVisualizer
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.CyberCard
import com.example.ui.components.HudStatusBadge
import com.example.ui.theme.CyberBorderDark
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberNavyDark
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose

@Composable
fun VoiceHudScreen(
    viewModel: NikhilViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val voiceState by viewModel.voiceManager.voiceState.collectAsState()
    val recognizedText by viewModel.voiceManager.recognizedText.collectAsState()
    val audioRms by viewModel.voiceManager.audioRms.collectAsState()
    val errorMessage by viewModel.voiceManager.errorMessage.collectAsState()
    val isContinuousListening by viewModel.voiceManager.isContinuousListening.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val currentUserName by viewModel.loggedUserName.collectAsState()
    val currentPlanType by viewModel.userPlanType.collectAsState()
    val isUserVerified by viewModel.isVerifiedUser.collectAsState()
    val planDaysLeft by viewModel.planDaysRemaining.collectAsState()

    var manualVoiceInput by remember { mutableStateOf("") }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showVoiceLockedDialog by remember { mutableStateOf(false) }
    val isBgAssistantRunning by viewModel.isBackgroundAssistantRunning.collectAsState()
    val scrollState = rememberScrollState()

    val lastAssistantMessage = messages.lastOrNull { it.role == "assistant" }?.content ?: ""
    val lastAssistantImage = messages.lastOrNull { it.role == "assistant" && !it.imageUri.isNullOrBlank() }?.imageUri

    // Android System Intent Speech Recognizer Launcher with Hindi support
    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = spokenMatches?.firstOrNull() ?: ""
            if (text.isNotBlank()) {
                viewModel.sendVoiceCommand(text)
            }
        }
    }

    fun triggerSpeechInput() {
        if (!viewModel.canAccessVoice()) {
            showVoiceLockedDialog = true
            return
        }
        if (viewModel.voiceManager.isRecognitionAvailable()) {
            viewModel.toggleVoiceListening()
        } else {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "अम्मू से बोलिए (उदा: हेलो अम्मू, कैसी हो तुम?)...")
                }
                systemSpeechLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "वॉइस सर्च उपलब्ध नहीं है", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val friendlyHindiConversations = listOf(
        "हेलो अम्मू! कैसी हो तुम?",
        "तुमको किसने बनाया है?",
        "एक प्यारा सा गाना सुनाओ",
        "आज का मौसम कैसा है?",
        "एक मज़ेदार चुटकुला सुनाओ",
        "मेरी लोकेशन बताओ",
        "मुझे तुम्हारी आवाज़ बहुत पसंद है"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top HUD Bar with User Plan, Blue Verified Tick & Creator Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "AMMU // वॉयस ऑर्ब",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                                color = CyberCyan
                            )
                        )
                        HudStatusBadge(
                            text = when (voiceState) {
                                VoiceState.SPEAKING -> "बोल रही है 🌸"
                                VoiceState.LISTENING -> "सुन रही है 🎙️"
                                VoiceState.PROCESSING -> "सोच रही है ⚡"
                                else -> "तैयार"
                            },
                            statusColor = when (voiceState) {
                                VoiceState.SPEAKING -> CyberCyan
                                VoiceState.LISTENING -> NeonEmerald
                                VoiceState.PROCESSING -> NeonAmber
                                else -> NeonEmerald
                            }
                        )
                    }

                    // User Badge & Blue Verified Tick Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = currentUserName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )

                        // Blue Verified Tick
                        if (isUserVerified) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Member",
                                tint = Color(0xFF1D9BF0),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Plan Pill
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (currentPlanType) {
                                "OWNER", "PREMIUM" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                "PRO" -> CyberCyan.copy(alpha = 0.2f)
                                else -> NeonEmerald.copy(alpha = 0.2f)
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                0.8.dp,
                                when (currentPlanType) {
                                    "OWNER", "PREMIUM" -> Color(0xFFF59E0B)
                                    "PRO" -> CyberCyan
                                    else -> NeonEmerald
                                }
                            )
                        ) {
                            Text(
                                text = when (currentPlanType) {
                                    "OWNER" -> "👑 OWNER"
                                    "PREMIUM" -> "👑 PREMIUM"
                                    "PRO" -> "⚡ PRO"
                                    else -> "🌟 30D TRIAL"
                                },
                                color = when (currentPlanType) {
                                    "OWNER", "PREMIUM" -> Color(0xFFFBBF24)
                                    "PRO" -> CyberCyan
                                    else -> NeonEmerald
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Action Buttons: Plans Shortcut + Account & Profile Button
                val isOwner by viewModel.isOwner.collectAsState()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isOwner) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFFBBF24)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.navigateTo(com.example.ui.navigation.AppDestination.OWNER_PANEL)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("👑", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("ओनर हब", color = Color(0xFFFBBF24), fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        }
                    }

                    // Plans Button Shortcut
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.navigateTo(com.example.ui.navigation.AppDestination.PLANS)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "Plans",
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "प्लान्स",
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = { showAccountDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyberBorderDark.copy(alpha = 0.5f))
                            .testTag("account_profile_hud_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "User Account",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Permanent Creator Attribution Banner (Ammu is made by Aman)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        viewModel.speakMessage("अम्मू को अमन ने बड़े प्यार से बनाया है। Ammu is proudly made by Aman!")
                    },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E1B4B).copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFA78BFA))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("❤️", fontSize = 16.sp)
                        Column {
                            Text(
                                text = "Ammu is made by Aman",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFE0E7FF),
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = "निर्माता: अमन (Aman) • आपकी अपनी पर्सनल सुपर AI",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFC7D2FE),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                    Text("🌸", fontSize = 16.sp)
                }
            }

            // Siri-like Background Voice Assistant ('Hey Ammu' Wake-Word)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = if (isBgAssistantRunning) Color(0xFF8B5CF6).copy(alpha = 0.15f) else CyberSurfaceDark,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isBgAssistantRunning) Color(0xFFA78BFA) else CyberBorderDark
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Siri जैसा बैकग्राउंड असिस्टेंट ('Hey Ammu')",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBgAssistantRunning) Color(0xFFA78BFA) else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            if (isBgAssistantRunning) {
                                Text(
                                    text = "🎙️ LIVE",
                                    color = Color(0xFFA78BFA),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Text(
                            text = if (isBgAssistantRunning) "स्क्रीन लॉक होने पर भी 'Hey Ammu' बोलते ही जागेगी और जवाब देगी!" else "ऐप के बाहर भी 'Hey Ammu' सुनने के लिए चालू करें",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                    Switch(
                        checked = isBgAssistantRunning,
                        onCheckedChange = { viewModel.toggleBackgroundAssistant(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = Color(0xFFA78BFA),
                            uncheckedThumbColor = CyberBorderDark,
                            uncheckedTrackColor = CyberNavyDark
                        )
                    )
                }
            }

            // Error banner if mic was silent
            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = NeonAmber.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = NeonAmber, fontSize = 11.sp),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.voiceManager.clearError() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = NeonAmber, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // CENTERPIECE: Holographic Ammu Voice Orb / Sound Reactive Visualizer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AmmuVoiceOrbVisualizer(
                    size = 220.dp,
                    voiceState = if (isGenerating) VoiceState.PROCESSING else voiceState,
                    audioRms = audioRms,
                    onClick = {
                        if (voiceState == VoiceState.SPEAKING) {
                            viewModel.stopSpeaking()
                        } else {
                            triggerSpeechInput()
                        }
                    },
                    modifier = Modifier.testTag("ammu_voice_orb")
                )

                Text(
                    text = when {
                        voiceState == VoiceState.SPEAKING -> "अम्मू बोल रही है (सुरीली आवाज़)..."
                        voiceState == VoiceState.LISTENING -> "अम्मू सुन रही है... बोलिए कमांडर!"
                        isGenerating || voiceState == VoiceState.PROCESSING -> "सोच रही हूँ कमांडर..."
                        else -> "अम्मू वॉयस ऑर्ब • टैप करके बात करें"
                    },
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = when {
                            voiceState == VoiceState.SPEAKING -> CyberCyan
                            voiceState == VoiceState.LISTENING -> NeonEmerald
                            isGenerating || voiceState == VoiceState.PROCESSING -> NeonAmber
                            else -> CyberCyan.copy(alpha = 0.9f)
                        }
                    )
                )

                // High-Visibility Ammu Plans & QR Scanner Quick Banner
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            viewModel.navigateTo(com.example.ui.navigation.AppDestination.PLANS)
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1E1B4B).copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFF59E0B))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("💎", fontSize = 22.sp)
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "अम्मू प्लान्स (₹99 Pro • ₹299 Premium)",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFFBBF24),
                                            fontSize = 12.sp
                                        )
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = NeonEmerald.copy(alpha = 0.25f)
                                    ) {
                                        Text(
                                            text = "पहला महीना मुफ़्त 🎁",
                                            color = NeonEmerald,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "FamPay QR कोड से पेमेंट करें • 100% सुरक्षित • ब्लू वेरिफाइड टिक पाएं",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFC7D2FE),
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF59E0B)
                        ) {
                            Text(
                                text = "QR देखें ➔",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Audio Waveform
                AudioWaveformVisualizer(
                    isSpeaking = voiceState == VoiceState.SPEAKING || voiceState == VoiceState.LISTENING,
                    barCount = 22,
                    color = if (voiceState == VoiceState.LISTENING) NeonEmerald else CyberCyan
                )

                // Quick Voice Button (Stop / Test voice)
                if (voiceState == VoiceState.SPEAKING) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { viewModel.stopSpeaking() },
                        shape = RoundedCornerShape(20.dp),
                        color = NeonRose.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonRose)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = NeonRose, modifier = Modifier.size(16.dp))
                            Text("बोलना रोकें (Stop)", color = NeonRose, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                viewModel.speakMessage("अरे हेलो! मैं तुम्हारी प्यारी अम्मू हूँ। मैं बहुत खुश और बिल्कुल ठीक हूँ! तुम बताओ, आज क्या चल रहा है?")
                            },
                        shape = RoundedCornerShape(20.dp),
                        color = CyberCardDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorderDark)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                            Text("अम्मू की मीठी आवाज़ टेस्ट करें 🌸", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Transcript Card (You Spoke)
            if (recognizedText.isNotBlank() || voiceState == VoiceState.LISTENING) {
                CyberCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = NeonEmerald.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "आपने कहा (YOU SPOKE):",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NeonEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (recognizedText.isNotBlank()) recognizedText else "अम्मू आपकी आवाज़ सुन रही है...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // AI Response Preview with Replay button & Image Support
            if (lastAssistantMessage.isNotBlank()) {
                CyberCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = CyberCyan.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "अम्मू का जवाब (AMMU SPOKE):",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CyberCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                            IconButton(
                                onClick = { viewModel.speakMessage(lastAssistantMessage) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Speak Response",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = lastAssistantMessage,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 22.sp
                            )
                        )
                        if (!lastAssistantImage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            AsyncImage(
                                model = lastAssistantImage,
                                contentDescription = "AI Generated Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            // Quick Conversation Starters Chips
            Text(
                text = "अम्मू से बातचीत (टैप करें और बोलकर सुनें)",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(friendlyHindiConversations) { cmd ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (!viewModel.canAccessVoice()) {
                                    showVoiceLockedDialog = true
                                } else {
                                    viewModel.sendVoiceCommand(cmd)
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = CyberSurfaceDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorderDark)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                            Text(text = cmd, style = MaterialTheme.typography.bodySmall.copy(color = CyberCyan, fontSize = 12.sp))
                        }
                    }
                }
            }

            // Direct Voice Input Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = CyberSurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorderDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = manualVoiceInput,
                        onValueChange = { manualVoiceInput = it },
                        placeholder = {
                            Text(
                                text = "यहाँ लिखें या बोलें...",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("voice_command_input"),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberBorderDark,
                            focusedContainerColor = CyberCardDark,
                            unfocusedContainerColor = CyberCardDark
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            autoCorrect = true,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (manualVoiceInput.isNotBlank() && !isGenerating) {
                                    viewModel.sendVoiceCommand(manualVoiceInput)
                                    manualVoiceInput = ""
                                    focusManager.clearFocus()
                                }
                            }
                        ),
                        maxLines = 2
                    )

                    // Mic Trigger
                    IconButton(
                        onClick = { triggerSpeechInput() },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (voiceState == VoiceState.LISTENING) NeonEmerald.copy(alpha = 0.3f) else CyberCyan.copy(alpha = 0.15f)
                            )
                            .border(
                                1.dp,
                                if (voiceState == VoiceState.LISTENING) NeonEmerald else CyberCyan.copy(alpha = 0.4f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Mic",
                            tint = if (voiceState == VoiceState.LISTENING) NeonEmerald else CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Send Button
                    IconButton(
                        onClick = {
                            if (manualVoiceInput.isNotBlank() && !isGenerating) {
                                viewModel.sendVoiceCommand(manualVoiceInput)
                                manualVoiceInput = ""
                                focusManager.clearFocus()
                            }
                        },
                        enabled = manualVoiceInput.isNotBlank() && !isGenerating,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (manualVoiceInput.isNotBlank()) CyberCyan else CyberBorderDark.copy(alpha = 0.4f)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Voice Command",
                            tint = if (manualVoiceInput.isNotBlank()) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Account Profile & Logout Dialog
        val loggedUserName by viewModel.loggedUserName.collectAsState()
        val loggedUserEmail by viewModel.loggedUserEmail.collectAsState()
        val loggedAuthProvider by viewModel.loggedAuthProvider.collectAsState()

        if (showAccountDialog) {
            AlertDialog(
                onDismissRequest = { showAccountDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(24.dp))
                        Text(text = "आपका अम्मू अकाउंट", color = CyberCyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = CyberNavyDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorderDark)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "नाम: $loggedUserName",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    if (isUserVerified) {
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "Verified Member",
                                            tint = Color(0xFF1D9BF0),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                val userPhone by viewModel.loggedUserPhone.collectAsState()
                                if (userPhone.isNotBlank()) {
                                    Text(
                                        text = "फ़ोन: $userPhone",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "एक्टिव प्लान:",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = when (currentPlanType) {
                                            "OWNER", "PREMIUM" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                            "PRO" -> CyberCyan.copy(alpha = 0.2f)
                                            else -> NeonEmerald.copy(alpha = 0.2f)
                                        }
                                    ) {
                                        Text(
                                            text = when (currentPlanType) {
                                                "OWNER" -> "👑 OWNER"
                                                "PREMIUM" -> "👑 PREMIUM (₹299/माह)"
                                                "PRO" -> "⚡ PRO (₹99/माह)"
                                                else -> "🌟 30-DAY FREE TRIAL"
                                            },
                                            color = when (currentPlanType) {
                                                "OWNER", "PREMIUM" -> Color(0xFFFBBF24)
                                                "PRO" -> CyberCyan
                                                else -> NeonEmerald
                                            },
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "बचा हुआ समय: $planDaysLeft दिन",
                                    color = NeonEmerald,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // View Plans Button
                        Button(
                            onClick = {
                                showAccountDialog = false
                                viewModel.navigateTo(com.example.ui.navigation.AppDestination.PLANS)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("अम्मू प्लान्स & अपग्रेड हब ➔", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // Permanent Creator attribution notice
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E1B4B).copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA78BFA))
                        ) {
                            Text(
                                text = "❤️ Ammu is made with love by Aman",
                                color = Color(0xFFE0E7FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showAccountDialog = false
                            viewModel.logout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRose, contentColor = Color.White)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("लॉगआउट (Logout)", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAccountDialog = false }) {
                        Text("बंद करें", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }

        // Voice Locked Dialog for users on ₹99 Pro Plan (Chat Only)
        if (showVoiceLockedDialog) {
            AlertDialog(
                onDismissRequest = { showVoiceLockedDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFFBBF24))
                        Text(
                            text = "अम्मू वॉयस ₹299 प्रीमियम में है 👑",
                            color = Color(0xFFFBBF24),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "प्रिय यूजर, ₹99 Ammu Pro प्लान में केवल 'AI चैट साथी' (Chat Only) शामिल है।",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "अम्मू की असली सुरीली वॉयस, अल्ट्रा-फास्ट रिस्पॉन्स (⚡), सुपर-स्मार्ट AI इंटेलिजेंस (🧠) और फुल फोन कंट्रोल फीचर्स पाने के लिए ₹299 Ammu Premium प्लान में अपग्रेड करें।",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showVoiceLockedDialog = false
                            viewModel.navigateTo(com.example.ui.navigation.AppDestination.PLANS)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B),
                            contentColor = Color.Black
                        )
                    ) {
                        Text("₹299 Premium में अपग्रेड करें ➔", fontWeight = FontWeight.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showVoiceLockedDialog = false }) {
                        Text("समझ गया", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}

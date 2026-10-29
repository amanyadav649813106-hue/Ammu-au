package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entities.ChatMessageEntity
import com.example.ui.NikhilViewModel
import com.example.ui.components.HudStatusBadge
import com.example.ui.components.MarkdownFormattedText
import com.example.ui.components.MessageActionRow
import com.example.ui.theme.CyberBorderDark
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberNavyDark
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable
fun ChatScreen(
    viewModel: NikhilViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    var inputText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBase64 by remember { mutableStateOf<String?>(null) }
    var isProcessingImage by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            isProcessingImage = true
            coroutineScope.launch {
                val b64 = withContext(Dispatchers.IO) {
                    encodeImageUriToBase64(context, uri)
                }
                selectedImageBase64 = b64
                isProcessingImage = false
            }
        }
    }

    var showServerSettingsDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showChatHistoryDialog by remember { mutableStateOf(false) }
    val allSessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val customApiKey by viewModel.apiKeyInput.collectAsState()
    var tempKeyInput by remember(customApiKey) { mutableStateOf(customApiKey) }

    val knowledgePrompts = listOf(
        "तुमको किसने बनाया है?",
        "व्हाट्सएप ओपन करके मेरे दोस्त को मैसेज करो",
        "आज का मौसम कैसा है?",
        "कॉल डायल करो",
        "टॉर्च जलाओ",
        "अरे अम्मू, कैसी हो तुम?",
        "एक मज़ेदार और नया चुटकुला सुनाओ",
        "भारत की राजधानी क्या है?"
    )

    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "AMMU // अम्मू",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color = CyberCyan
                        )
                    )
                    HudStatusBadge(text = "सर्वर कनेक्टेड ⚡", statusColor = NeonEmerald)
                }
                Text(
                    text = "आपकी अपनी प्यारी और सुपर-इंटेलिजेंट AI दोस्त",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Server Config / Key Icon
                IconButton(
                    onClick = { showServerSettingsDialog = true },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CyberBorderDark.copy(alpha = 0.5f))
                        .testTag("server_key_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Server Settings",
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // New Chat Button
                IconButton(
                    onClick = { viewModel.createNewChat("Ammu") },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CyberBorderDark.copy(alpha = 0.5f))
                        .testTag("new_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Chat",
                        tint = NeonEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Chat History Button (Saved chats)
                IconButton(
                    onClick = { showChatHistoryDialog = true },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CyberBorderDark.copy(alpha = 0.5f))
                        .testTag("chat_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Chat History",
                        tint = NeonAmber,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Clear Chat Button
                IconButton(
                    onClick = { viewModel.clearChat() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CyberBorderDark.copy(alpha = 0.5f))
                        .testTag("clear_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Chat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Account / Logout Button
                IconButton(
                    onClick = { showAccountDialog = true },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CyberBorderDark.copy(alpha = 0.5f))
                        .testTag("account_profile_button")
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

        // Permanent Creator Attribution Banner (कभी ना हटने वाला मैसेज: Ammu is made by Aman)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    viewModel.speakMessage("अम्मू को अमन ने बड़े प्यार से बनाया है। Ammu is made by Aman!")
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
                            text = "अमन द्वारा निर्मित • सदैव समर्पित AI दोस्त",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFA5B4FC),
                                fontSize = 10.sp
                            )
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF6366F1).copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA78BFA).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Creator: Aman",
                        color = Color(0xFFC7D2FE),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // ChatGPT-Style AI Plugins & Special Skills Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    PluginChip("🌐 वेब रिसर्च (Live Search)", CyberCyan) {
                        inputText = "आज की ताज़ा मुख्य खबरें और मौसम का अपडेट बताओ"
                    }
                }
                item {
                    PluginChip("🎨 DALL-E AI फोटो", Color(0xFFF472B6)) {
                        inputText = "एक सुंदर और भविष्यवादी साइबरपंक शहर की फोटो बनाओ"
                    }
                }
                item {
                    PluginChip("💻 सुपर कोडर (Kotlin/Python)", NeonEmerald) {
                        inputText = "एक एंड्रॉइड ऐप के लिए बैकग्राउंड सर्विस का पूरा कोड लिखो"
                    }
                }
                item {
                    PluginChip("⚡ स्टॉनिक फोन कमांडर", NeonAmber) {
                        inputText = "youtube par song chalao kesariya"
                    }
                }
                item {
                    PluginChip("🧠 डीप माइंड रीज़निंग", Color(0xFFA78BFA)) {
                        inputText = "इंसानी दिमाग और सुपर आर्टिफिशियल इंटेलिजेंस में क्या अंतर है?"
                    }
                }
            }
        }

        // Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            state = listState,
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Permanent Pinned Notice in Chat
            item(key = "permanent_creator_chat_card") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = CyberCardDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA78BFA).copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("⭐", fontSize = 20.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ammu is made by Aman",
                                color = Color(0xFFE0E7FF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "मुझे अमन (Aman) ने बनाया है। जब भी आप पूछेंगे, मैं गर्व से बताऊँगी कि मेरे निर्माता अमन हैं!",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { message ->
                AmmuChatMessageItem(
                    message = message,
                    onSpeak = { viewModel.speakMessage(message.content) }
                )
            }

            if (isGenerating) {
                item {
                    AmmuThinkingIndicator()
                }
            }

            if (messages.size <= 1) {
                item {
                    AmmuQuickPrompts(
                        prompts = knowledgePrompts,
                        onSelectPrompt = { prompt ->
                            viewModel.sendMessage(prompt)
                        }
                    )
                }
            }
        }

        // Bottom Input Area (Equipped with full IME keyboard support & Send Action)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorderDark.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Attached Photo Preview Bar
                AnimatedVisibility(visible = selectedImageUri != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = CyberCardDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "Selected Photo",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "फ़ोटो जुड़ी हुई है 📸",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = NeonAmber,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        if (isProcessingImage) {
                                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = NeonAmber, strokeWidth = 1.5.dp)
                                        }
                                    }
                                    Text(
                                        text = "अम्मू को भेजें और एडिटिंग सुझाव या जानकारी पाएँ",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    selectedImageUri = null
                                    selectedImageBase64 = null
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Photo",
                                    tint = NeonAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Photo Attach Button (Zero-permission Android Photo Picker)
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (selectedImageUri != null) NeonAmber.copy(alpha = 0.25f) else CyberBorderDark.copy(alpha = 0.4f)
                            )
                            .border(
                                1.dp,
                                if (selectedImageUri != null) NeonAmber else CyberBorderDark,
                                CircleShape
                            )
                            .testTag("attach_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Attach Photo",
                            tint = if (selectedImageUri != null) NeonAmber else CyberCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = if (selectedImageUri != null) "फ़ोटो के बारे में पूछें (उदा: इसे एडिट कैसे करें?)..." else "अम्मू से कुछ भी पूछें या बात करें...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberBorderDark,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            autoCorrect = true,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                val canSend = (inputText.isNotBlank() || selectedImageBase64 != null) && !isGenerating && !isProcessingImage
                                if (canSend) {
                                    val textToSend = inputText.ifBlank { "कृपया इस फोटो को देखें और इसके बारे में बताएं या एडिट करने का सुझाव दें" }
                                    viewModel.sendMessage(
                                        text = textToSend,
                                        imageUri = selectedImageUri?.toString(),
                                        imageBase64 = selectedImageBase64
                                    )
                                    inputText = ""
                                    selectedImageUri = null
                                    selectedImageBase64 = null
                                    focusManager.clearFocus()
                                }
                            }
                        ),
                        maxLines = 4
                    )

                    // Voice Mic Button
                    IconButton(
                        onClick = { viewModel.toggleVoiceListening() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.15f))
                            .border(1.dp, CyberCyan.copy(alpha = 0.4f), CircleShape)
                            .testTag("voice_input_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Send Button
                    val canSend = (inputText.isNotBlank() || selectedImageBase64 != null) && !isGenerating && !isProcessingImage
                    IconButton(
                        onClick = {
                            if (canSend) {
                                val textToSend = inputText.ifBlank { "कृपया इस फोटो को देखें और इसके बारे में बताएं या एडिट करने का सुझाव दें" }
                                viewModel.sendMessage(
                                    text = textToSend,
                                    imageUri = selectedImageUri?.toString(),
                                    imageBase64 = selectedImageBase64
                                )
                                inputText = ""
                                selectedImageUri = null
                                selectedImageBase64 = null
                                focusManager.clearFocus()
                            }
                        },
                        enabled = canSend,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSend) CyberCyan else CyberBorderDark.copy(alpha = 0.4f)
                            )
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = if (canSend) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // Server Settings Dialog (For viewing connection or setting custom key)
    if (showServerSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showServerSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = CyberCyan)
                    Text(text = "AI सर्वर व API सेटिंग्स", color = CyberCyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "अम्मू Google Gemini 3.5 Flash Lite सर्वर से सीधे कनेक्टेड है और हर सवाल का लाइव जवाब दे सकती है।",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NeonEmerald.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald)
                    ) {
                        Text(
                            text = "🟢 स्टेटस: लाइव सर्वर एक्टिव (Active)",
                            color = NeonEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Text(
                        text = "यदि आप अपनी व्यक्तिगत Gemini API Key दर्ज करना चाहते हैं (वैकल्पिक):",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    OutlinedTextField(
                        value = tempKeyInput,
                        onValueChange = { tempKeyInput = it },
                        placeholder = { Text("AIzaSy... (खाली छोड़ सकते हैं)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateApiKey(tempKeyInput)
                        showServerSettingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
                ) {
                    Text("सेव करें", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerSettingsDialog = false }) {
                    Text("बंद करें", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
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
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "नाम: $loggedUserName",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            Text(
                                text = "लॉगिन माध्यम: $loggedAuthProvider",
                                color = NeonEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
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
                    Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
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

    // Chat History Dialog (Browse past saved conversations)
    if (showChatHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showChatHistoryDialog = false },
            containerColor = CyberSurfaceDark,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.History, contentDescription = null, tint = NeonAmber)
                    Text(text = "चैट हिस्ट्री (Saved Conversations)", color = NeonAmber, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (allSessions.isEmpty()) {
                        Text("कोई पुरानी चैट नहीं मिली।", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    } else {
                        allSessions.forEach { session ->
                            val isCurrent = session.id == currentSessionId
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.switchSession(session.id)
                                        showChatHistoryDialog = false
                                    },
                                color = if (isCurrent) CyberCyan.copy(alpha = 0.15f) else CyberNavyDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isCurrent) CyberCyan else CyberBorderDark),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = session.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isCurrent) CyberCyan else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "पर्सनैलिटी: ${session.personality} • आईडी: #${session.id}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteChatSession(session.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Delete", tint = NeonRose.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createNewChat("Ammu")
                        showChatHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color.Black)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("नई चैट (+ New)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChatHistoryDialog = false }) {
                    Text("बंद करें", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
fun AmmuChatMessageItem(
    message: ChatMessageEntity,
    onSpeak: () -> Unit
) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            // Ammu AI Avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CyberNavyDark)
                    .border(1.5.dp, CyberCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Ammu",
                    tint = NeonRose,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 310.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) CyberCyan.copy(alpha = 0.2f) else CyberCardDark,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isUser) CyberCyan.copy(alpha = 0.5f) else CyberBorderDark
                ),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (!message.imageUri.isNullOrBlank()) {
                        AsyncImage(
                            model = message.imageUri,
                            contentDescription = "Attached photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    MarkdownFormattedText(
                        text = message.content,
                        textColor = if (isUser) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
                    )

                    if (!isUser) {
                        Spacer(modifier = Modifier.height(6.dp))
                        MessageActionRow(
                            messageText = message.content,
                            onSpeak = onSpeak
                        )
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NeonAmber.copy(alpha = 0.2f))
                    .border(1.dp, NeonAmber, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User",
                    tint = NeonAmber,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AmmuThinkingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(CyberNavyDark)
                .border(1.dp, CyberCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = CyberCyan,
                strokeWidth = 2.dp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CyberCardDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorderDark)
        ) {
            Text(
                text = "अम्मू सोच रही है...",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = CyberCyan,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
fun AmmuQuickPrompts(
    prompts: List<String>,
    onSelectPrompt: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "अम्मू से कुछ भी पूछें (ChatGPT की तरह तुरंत जवाब पाएँ):",
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        )
        for (prompt in prompts) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectPrompt(prompt) },
                shape = RoundedCornerShape(10.dp),
                color = CyberSurfaceDark.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorderDark.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }
    }
}

private fun encodeImageUriToBase64(context: Context, uri: Uri, maxDimension: Int = 1024): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        if (originalBitmap == null) return null

        val width = originalBitmap.width
        val height = originalBitmap.height
        val scale = if (width > maxDimension || height > maxDimension) {
            val maxSide = maxOf(width, height)
            maxDimension.toFloat() / maxSide
        } else {
            1f
        }

        val scaledBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(originalBitmap, (width * scale).toInt(), (height * scale).toInt(), true)
        } else {
            originalBitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        Base64.encodeToString(byteArray, Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }
}

@Composable
fun PluginChip(label: String, tint: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = tint.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}


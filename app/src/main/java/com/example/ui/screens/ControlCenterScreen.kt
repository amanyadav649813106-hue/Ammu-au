package com.example.ui.screens

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.receiver.AmmuDeviceAdminReceiver
import com.example.service.AmmuAccessibilityService
import com.example.ui.NikhilViewModel
import com.example.ui.components.CyberCard
import com.example.ui.theme.CyberBorderDark
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberNavyDark
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose

@Composable
fun ControlCenterScreen(
    viewModel: NikhilViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val dpm = remember { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager }
    val adminComponent = remember { ComponentName(context, AmmuDeviceAdminReceiver::class.java) }
    val isDeviceAdminActive = dpm?.isAdminActive(adminComponent) == true
    val isAccessibilityActive = AmmuAccessibilityService.isServiceEnabled(context)

    var songSearchText by remember { mutableStateOf("") }
    var shoppingSearchText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "कंट्रोल डेक // CONTROL DECK",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = CyberCyan
                    )
                )
                Text(
                    text = "हार्डवेयर लॉक, टचलेस नेविगेशन व सिस्टम ऑटोमेशन",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = NeonEmerald.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald)
            ) {
                Text(
                    text = "⚡ READY",
                    color = NeonEmerald,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        var showSecurityVaultDialog by remember { mutableStateOf(false) }
        val isBgRunning by viewModel.isBackgroundAssistantRunning.collectAsState()

        if (showSecurityVaultDialog) {
            com.example.ui.components.OwnerControlDialog(
                viewModel = viewModel,
                onDismiss = { showSecurityVaultDialog = false }
            )
        }

        // Section 0: Military-Grade Security Shield & Zero-Server Vault
        CyberCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = NeonEmerald.copy(alpha = 0.8f)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(20.dp))
                        Column {
                            Text(
                                text = "सुरक्षा शील्ड: 100% अभेद्य",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NeonEmerald)
                            )
                            Text(
                                text = "शून्य सर्वर कीज़ • डिवाइस वॉल्ट • एंटी-हैक",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NeonEmerald.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald)
                    ) {
                        Text(
                            text = "SECURE",
                            color = NeonEmerald,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "सर्वर से सभी API कीज़ हटा दी गई हैं। बिना ऐप खोले 'Hey Ammu' बोलते ही अम्मू जवाब देगी: 'जी हाँ कमांडर!'",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showSecurityVaultDialog = true },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color.Black)
                    ) {
                        Text("🔑 API Key वॉल्ट & शील्ड", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.toggleBackgroundAssistant(!isBgRunning) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBgRunning) CyberCyan else CyberSurfaceDark,
                            contentColor = if (isBgRunning) Color.Black else CyberCyan
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan)
                    ) {
                        Text(if (isBgRunning) "बैकग्राउंड: ON ⚡" else "बैकग्राउंड: OFF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 1: Hardware Screen Lock & Unlock Controls
        CyberCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = CyberCyan.copy(alpha = 0.6f)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                    Text(
                        text = "हार्डवेयर स्क्रीन लॉक & अनलॉक",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CyberCyan)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Lock Screen Button
                    Button(
                        onClick = { viewModel.sendVoiceCommand("ammu lock the screen") },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("lock_screen_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151), contentColor = Color(0xFFE5E7EB))
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("स्क्रीन लॉक (Off)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Unlock Screen Button
                    Button(
                        onClick = { viewModel.sendVoiceCommand("ammu unlock the screen") },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("unlock_screen_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color.Black)
                    ) {
                        Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("अनलॉक (Wake Up)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // 1-Tap Permissions status & activation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "अम्मू को हार्डवेयर स्क्रीन लॉक करने की अनुमति दें")
                                }
                                try { context.startActivity(intent) } catch (e: Exception) { Toast.makeText(context, "सेटिंग्स नहीं खुल सकी", Toast.LENGTH_SHORT).show() }
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDeviceAdminActive) NeonEmerald.copy(alpha = 0.12f) else CyberNavyDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDeviceAdminActive) NeonEmerald else CyberBorderDark)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(if (isDeviceAdminActive) "🔒" else "⚠️", fontSize = 14.sp)
                            Column {
                                Text(
                                    text = if (isDeviceAdminActive) "डिवाइस एडमिन: एक्टिव" else "एडमिन परमिशन दें",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDeviceAdminActive) NeonEmerald else NeonAmber
                                )
                                Text("स्क्रीन ऑफ के लिए", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                try { context.startActivity(intent) } catch (e: Exception) { Toast.makeText(context, "एक्सेसिबिलिटी सेटिंग्स नहीं खुली", Toast.LENGTH_SHORT).show() }
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isAccessibilityActive) CyberCyan.copy(alpha = 0.12f) else CyberNavyDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isAccessibilityActive) CyberCyan else CyberBorderDark)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(if (isAccessibilityActive) "⚡" else "⚠️", fontSize = 14.sp)
                            Column {
                                Text(
                                    text = if (isAccessibilityActive) "एक्सेसिबिलिटी: एक्टिव" else "एक्सेसिबिलिटी दें",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAccessibilityActive) CyberCyan else NeonAmber
                                )
                                Text("टचलेस कंट्रोल के लिए", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Touchless Voice Navigation (बिना हाथ लगाए फोन कंट्रोल)
        CyberCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = Color(0xFF8B5CF6).copy(alpha = 0.5f)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🖐️", fontSize = 18.sp)
                    Text(
                        text = "टचलेस नेविगेशन (Hands-Free Voice Controls)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFA78BFA))
                    )
                }
                Text(
                    text = "बिना स्क्रीन को छुए सिर्फ आवाज़ से फोन चलाएं या नीचे एक टैप करें:",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TouchlessActionTile("◀ Back", "back jao", CyberBorderDark, MaterialTheme.colorScheme.onSurface) { viewModel.sendVoiceCommand("back jao") }
                    TouchlessActionTile("⌂ Home", "home jao", NeonEmerald.copy(alpha = 0.4f), NeonEmerald) { viewModel.sendVoiceCommand("home jao") }
                    TouchlessActionTile("▢ Recents", "recent apps", CyberCyan.copy(alpha = 0.4f), CyberCyan) { viewModel.sendVoiceCommand("recent apps") }
                    TouchlessActionTile("🔔 Notifs", "notifications kholo", NeonAmber.copy(alpha = 0.4f), NeonAmber) { viewModel.sendVoiceCommand("notifications kholo") }
                    TouchlessActionTile("📸 Shot", "screenshot lo", Color(0xFFA78BFA).copy(alpha = 0.4f), Color(0xFFA78BFA)) { viewModel.sendVoiceCommand("screenshot lo") }
                }
            }
        }

        // Section 3: YouTube In-App Song Player & Auto-Play
        CyberCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = Color(0xFFEF4444).copy(alpha = 0.5f)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("▶️", fontSize = 18.sp)
                    Text(
                        text = "यूट्यूब सॉन्ग प्लेयर (In-App Song Search & Play)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFF87171))
                    )
                }
                Text(
                    text = "कोई भी गाना या वीडियो बोलें या लिखें, अम्मू सीधे यूट्यूब पर सर्च करके प्ले करेगी:",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = songSearchText,
                        onValueChange = { songSearchText = it },
                        placeholder = { Text("गाने का नाम (उदा: Kesariya, Arijit Singh)...", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF87171),
                            unfocusedBorderColor = CyberBorderDark,
                            focusedContainerColor = CyberCardDark,
                            unfocusedContainerColor = CyberCardDark
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (songSearchText.isNotBlank()) {
                                    viewModel.sendVoiceCommand("youtube par song chalao $songSearchText")
                                    songSearchText = ""
                                }
                            }
                        ),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (songSearchText.isNotBlank()) {
                                viewModel.sendVoiceCommand("youtube par song chalao $songSearchText")
                                songSearchText = ""
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play Song", tint = Color.White)
                    }
                }

                // Quick Song Chips
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuickChip("🎵 Kesariya") { viewModel.sendVoiceCommand("youtube par Kesariya song chalao") }
                    QuickChip("🔥 Trending") { viewModel.sendVoiceCommand("youtube par trending bollywood songs chalao") }
                    QuickChip("🎧 Lofi Chill") { viewModel.sendVoiceCommand("youtube par hindi lofi songs chalao") }
                }
            }
        }

        // Section 4: Universal App Launchers & Shopping
        CyberCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = NeonAmber.copy(alpha = 0.5f)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "📦 ऐप्स & शॉपिंग सर्च (Amazon, Play Store, Chrome)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NeonAmber)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppActionTile("▶️ प्ले स्टोर", Color(0xFF60A5FA)) { viewModel.sendVoiceCommand("play store open karo") }
                    AppActionTile("📦 अमेज़न", Color(0xFFFBBF24)) { viewModel.sendVoiceCommand("amazon open karo") }
                    AppActionTile("🌐 गूगल", Color(0xFF93C5FD)) { viewModel.sendVoiceCommand("google open karo") }
                    AppActionTile("📸 कैमरा", Color(0xFFA78BFA)) { viewModel.sendVoiceCommand("camera open karke photo lo") }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppActionTile("💬 व्हाट्सएप", NeonEmerald) { viewModel.sendVoiceCommand("whatsapp open karo") }
                    AppActionTile("📞 कॉल पापा", CyberCyan) { viewModel.sendVoiceCommand("mere papa ko call karo") }
                    AppActionTile("🔦 टॉर्च ON", NeonRose) { viewModel.sendVoiceCommand("torch on karo") }
                    AppActionTile("🔦 टॉर्च OFF", Color(0xFF9CA3AF)) { viewModel.sendVoiceCommand("torch off karo") }
                }

                // Direct Amazon In-App Shopping Search Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = shoppingSearchText,
                        onValueChange = { shoppingSearchText = it },
                        placeholder = { Text("अमेज़न पर सर्च करें (उदा: mens shoes, smartwatch)...", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonAmber,
                            unfocusedBorderColor = CyberBorderDark,
                            focusedContainerColor = CyberCardDark,
                            unfocusedContainerColor = CyberCardDark
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (shoppingSearchText.isNotBlank()) {
                                    viewModel.sendVoiceCommand("amazon par search karo $shoppingSearchText")
                                    shoppingSearchText = ""
                                }
                            }
                        ),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (shoppingSearchText.isNotBlank()) {
                                viewModel.sendVoiceCommand("amazon par search karo $shoppingSearchText")
                                shoppingSearchText = ""
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(NeonAmber)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Search Amazon", tint = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun TouchlessActionTile(
    label: String,
    sublabel: String,
    borderColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = CyberCardDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
            Text(text = sublabel, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AppActionTile(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun QuickChip(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = CyberNavyDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorderDark),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp,
            color = CyberCyan
        )
    }
}

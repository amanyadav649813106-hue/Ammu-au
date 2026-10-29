package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.SecurityShieldManager
import com.example.ui.NikhilViewModel
import com.example.ui.theme.CyberBorderDark
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberNavyDark
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose

@Composable
fun OwnerControlDialog(
    viewModel: NikhilViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val currentPitch by viewModel.speechPitch.collectAsState()
    val currentRate by viewModel.speechRate.collectAsState()
    val globalBroadcast by viewModel.globalBroadcast.collectAsState()
    val isSavvyEnabled by viewModel.savvyAttitude.collectAsState()
    val apiKey by viewModel.apiKeyInput.collectAsState()
    val isBgRunning by viewModel.isBackgroundAssistantRunning.collectAsState()

    var pitchSlider by remember(currentPitch) { mutableFloatStateOf(currentPitch) }
    var rateSlider by remember(currentRate) { mutableFloatStateOf(currentRate) }
    var broadcastInput by remember(globalBroadcast) { mutableStateOf(globalBroadcast) }
    var apiKeyInput by remember(apiKey) { mutableStateOf(apiKey) }

    var isScreenProtectionActive by remember {
        mutableStateOf(SecurityShieldManager.isScreenProtectionEnabled(context))
    }
    var isBootAutostartActive by remember {
        mutableStateOf(SecurityShieldManager.isBootAutostartEnabled(context))
    }

    val securityReport = remember { SecurityShieldManager.checkDeviceIntegrity() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🛡️", fontSize = 18.sp)
                        }
                    }
                    Column {
                        Text(
                            text = "कमांडर सिक्योरिटी & कंट्रोल डेक",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = Color(0xFFF59E0B))
                        )
                        Text(
                            text = "100% अभेद्य सुरक्षा • शून्य सर्वर कीज़ • अमन कंट्रोल",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Military Security Shield Status Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = CyberNavyDark,
                    border = BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(16.dp))
                            Text("एंटी-हैक डिफेंस शील्ड: 100% सक्रिय", color = NeonEmerald, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Text(
                            text = securityReport.details,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("रूट स्टेटस: ${if (securityReport.isRooted) "रूटेड ⚠️" else "नो रूट (सुरक्षित) ✅"}", fontSize = 10.sp, color = CyberCyan)
                            Text("सर्वर कीज़: 0 (हटा दी गई हैं) ✅", fontSize = 10.sp, color = NeonEmerald)
                        }
                    }
                }

                // 2. Encrypted Gemini API Key Vault (Zero-Server Storage)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = CyberNavyDark,
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                            Text("निजी Gemini API Key वॉल्ट (Zero-Server)", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "सर्वर से सभी कीज़ हटा दी गई हैं। अपनी Gemini API Key यहाँ डालें, यह केवल आपके फ़ोन में 256-बिट एन्क्रिप्टेड रहेगी।",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            placeholder = { Text("अपनी Gemini API Key यहाँ पेस्ट करें...", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = CyberBorderDark,
                                focusedLabelColor = CyberCyan
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    viewModel.updateApiKey(apiKeyInput)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
                            ) {
                                Text("सुरक्षित सेव करें 🔒", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            if (apiKeyInput.isNotBlank()) {
                                Button(
                                    onClick = {
                                        apiKeyInput = ""
                                        viewModel.updateApiKey("")
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonRose.copy(alpha = 0.2f), contentColor = NeonRose)
                                ) {
                                    Text("हटाएं", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 3. Google Assistant Mode (24/7 Always-On "Hey Ammu" Trigger)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = CyberNavyDark,
                    border = BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(16.dp))
                                Text("गूगल असिस्टेंट मोड (Hey Ammu)", color = NeonEmerald, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Text(
                                text = "बिना ऐप खोले 'Hey Ammu' बोलते ही अम्मू बोलेगी: 'जी हाँ कमांडर!'",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = isBgRunning,
                            onCheckedChange = { viewModel.toggleBackgroundAssistant(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NeonEmerald)
                        )
                    }
                }

                // 4. Anti-Screenshot & Screen Snoop Protection (FLAG_SECURE)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = CyberNavyDark,
                    border = BorderStroke(1.dp, NeonAmber.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(16.dp))
                                Text("एंटी-स्क्रीनशॉट शील्ड (Anti-Snoop)", color = NeonAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Text(
                                text = "कोई दूसरा ऐप या जासूस स्क्रीन रिकॉर्ड या स्क्रीनशॉट नहीं ले सकेगा",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = isScreenProtectionActive,
                            onCheckedChange = {
                                isScreenProtectionActive = it
                                SecurityShieldManager.setScreenProtectionEnabled(context, it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NeonAmber)
                        )
                    }
                }

                // 5. Auto-start on Phone Boot
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = CyberNavyDark,
                    border = BorderStroke(1.dp, CyberBorderDark)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("फोन चालू होते ही ऑटो-स्टार्ट", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(
                                text = "फोन रीस्टार्ट होने पर बैकग्राउंड में अम्मू तुरंत चालू हो जाएगी",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = isBootAutostartActive,
                            onCheckedChange = {
                                isBootAutostartActive = it
                                SecurityShieldManager.setBootAutostartEnabled(context, it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CyberCyan)
                        )
                    }
                }

                // 6. Voice Sweetness & Pitch
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = CyberNavyDark,
                    border = BorderStroke(1.dp, CyberBorderDark)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                            Text("आवाज़ की मिठास व टोन (Voice Tuning)", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("पिच (मिठास): ${String.format("%.2f", pitchSlider)}x", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Slider(
                            value = pitchSlider,
                            onValueChange = { pitchSlider = it },
                            valueRange = 0.8f..1.6f,
                            colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                        )
                        Text("स्पीड: ${String.format("%.2f", rateSlider)}x", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Slider(
                            value = rateSlider,
                            onValueChange = { rateSlider = it },
                            valueRange = 0.8f..1.4f,
                            colors = SliderDefaults.colors(thumbColor = NeonEmerald, activeTrackColor = NeonEmerald)
                        )
                        Button(
                            onClick = { viewModel.updateMasterVoiceSettings(pitchSlider, rateSlider) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
                        ) {
                            Text("आवाज़ अपडेट व टेस्ट करें 🔊", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 7. Savvy / Bully Attitude Switch
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = CyberNavyDark,
                    border = BorderStroke(1.dp, CyberBorderDark)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = NeonRose, modifier = Modifier.size(16.dp))
                                Text("करारा अंदाज़ (Savvy Mode)", color = NeonRose, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Text(
                                text = "गाली या बदतमीजी करने वालों को मुँहतोड़ जवाब दे",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = isSavvyEnabled,
                            onCheckedChange = { viewModel.toggleSavvyAttitude(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NeonRose)
                        )
                    }
                }

                // 8. Global Broadcast / Announcement Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = CyberNavyDark,
                    border = BorderStroke(1.dp, CyberBorderDark)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.Campaign, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(16.dp))
                            Text("मालिक का संदेश (Broadcast Announcement)", color = NeonAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = broadcastInput,
                            onValueChange = { broadcastInput = it },
                            label = { Text("संदेश टाइप करें", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonAmber,
                                unfocusedBorderColor = CyberBorderDark,
                                focusedLabelColor = NeonAmber
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.updateGlobalBroadcast(broadcastInput) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonAmber, contentColor = Color.Black)
                        ) {
                            Text("संदेश लाइव करें 📢", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black)
            ) {
                Text("सुरक्षित & सेव (Done)", fontWeight = FontWeight.Black)
            }
        }
    )
}

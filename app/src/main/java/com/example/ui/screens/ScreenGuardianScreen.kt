package com.example.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.util.Locale

@Composable
fun ScreenGuardianScreen(
    viewModel: NikhilViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val is24x7Active by viewModel.is24x7SentinelActive.collectAsState()
    val isScreenGuardianActive by viewModel.isLiveScreenGuardianActive.collectAsState()
    val activeApp by viewModel.activeAppOnScreen.collectAsState()
    val screenContent by viewModel.liveScreenContent.collectAsState()
    val proactiveAdvice by viewModel.proactiveScreenInsight.collectAsState()

    val locationInfo by viewModel.locationInfo.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "स्क्रीन AI गार्जियन & GPS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = Color(0xFFA78BFA)
                    )
                )
                Text(
                    text = "स्क्रीन शेयर एनालिसिस व रियल-टाइम लोकेशन ट्रैकिंग",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (is24x7Active) NeonEmerald.copy(alpha = 0.2f) else CyberNavyDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (is24x7Active) NeonEmerald else CyberBorderDark)
            ) {
                Text(
                    text = if (is24x7Active) "🟢 ऑनलाइन" else "⚪ ऑफलाइन",
                    color = if (is24x7Active) NeonEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Section 1: 24/7 Background Sentinel Master Toggle (Online / Offline)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = if (is24x7Active) Color(0xFF1E1B4B) else CyberSurfaceDark,
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (is24x7Active) Color(0xFFA78BFA) else CyberBorderDark
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🛡️", fontSize = 18.sp)
                        Text(
                            text = "24/7 अम्मू सेंटिनल (Always Online)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (is24x7Active) Color(0xFFA78BFA) else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (is24x7Active)
                            "अम्मू बैकग्राउंड में हमेशा सक्रिय है — 'Hey Ammu' सुनना, स्क्रीन देखना और लोकेशन ट्रैक करना चालू है!"
                        else
                            "बैकग्राउंड सेवा बंद है (ऑफलाइन मोड) — ऑन करने के लिए स्विच दबाएं",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
                Switch(
                    checked = is24x7Active,
                    onCheckedChange = { viewModel.toggle24x7Sentinel(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = Color(0xFFA78BFA),
                        uncheckedThumbColor = CyberBorderDark,
                        uncheckedTrackColor = CyberNavyDark
                    )
                )
            }
        }

        // Section 2: Live Screen AI Reader & Proactive Insights
        CyberCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = CyberCyan.copy(alpha = 0.6f)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.RemoveRedEye, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                        Text(
                            text = "लाइव स्क्रीन गार्जियन (Screen Vision)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CyberCyan)
                        )
                    }
                    Switch(
                        checked = isScreenGuardianActive,
                        onCheckedChange = { viewModel.toggleScreenGuardian(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = CyberCyan,
                            uncheckedThumbColor = CyberBorderDark,
                            uncheckedTrackColor = CyberNavyDark
                        )
                    )
                }

                Text(
                    text = "अम्मू बिना आपके पूछे स्क्रीन पढ़ती है और उपयोगी सलाह देती है (उदा: Chrome में अच्छा लेख, Amazon पर डील्स)",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                )

                // Current Active App Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CyberNavyDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📱", fontSize = 16.sp)
                        Column {
                            Text("वर्तमान सक्रिय ऐप (Active Window):", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = activeApp, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                        }
                    }
                }

                // Proactive Insight Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E1B4B).copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA78BFA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🧠", fontSize = 16.sp)
                            Text("अम्मू की लाइव स्क्रीन सलाह (Proactive Insight):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE0E7FF))
                        }
                        Text(
                            text = proactiveAdvice,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFE0E7FF),
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        )
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.speakMessage(proactiveAdvice) },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFA78BFA).copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA78BFA))
                            ) {
                                Text(
                                    text = "🔊 बोलकर सुनाओ",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE0E7FF)
                                )
                            }
                        }
                    }
                }

                if (screenContent.isNotBlank()) {
                    Text(
                        text = "स्क्रीन पर देखा गया टेक्स्ट: ${screenContent.take(160)}...",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }

        // Section 3: Real-Time GPS Location Telemetry
        CyberCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = NeonEmerald.copy(alpha = 0.6f)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(20.dp))
                        Text(
                            text = "रियल-टाइम GPS लोकेशन ट्रैकर",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NeonEmerald)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (locationInfo.isTracking) NeonEmerald.copy(alpha = 0.2f) else CyberNavyDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (locationInfo.isTracking) NeonEmerald else CyberBorderDark)
                    ) {
                        Text(
                            text = if (locationInfo.isTracking) "🛰️ GPS एक्टिव" else "🛰️ GPS बंद",
                            color = if (locationInfo.isTracking) NeonEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Live Address Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CyberNavyDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("आपकी वर्तमान जगह (Current Location):", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = locationInfo.fullAddress,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp
                            )
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "शहर: ${locationInfo.city}",
                                fontSize = 11.sp,
                                color = NeonEmerald,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (locationInfo.latitude != 0.0) {
                                Text(
                                    text = "GPS: ${String.format(Locale.US, "%.4f", locationInfo.latitude)}, ${String.format(Locale.US, "%.4f", locationInfo.longitude)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Action Buttons: Announce Location & Refresh
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.announceCurrentLocation() },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color.Black)
                    ) {
                        Icon(imageVector = Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("मेरी लोकेशन बताओ", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (!locationInfo.isTracking) {
                                viewModel.startLocationTracking()
                            } else {
                                viewModel.stopLocationTracking()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (locationInfo.isTracking) Color(0xFF374151) else CyberCyan,
                            contentColor = if (locationInfo.isTracking) Color.White else Color.Black
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (locationInfo.isTracking) "GPS रोकें" else "GPS शुरू करें", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

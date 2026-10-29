package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentApprovalRequest
import com.example.ui.NikhilViewModel
import com.example.ui.theme.CyberBorderDark
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberNavyDark
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OwnerAdminScreen(
    viewModel: NikhilViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    val paymentRequests by viewModel.paymentRequests.collectAsState()
    val isOwner by viewModel.isOwner.collectAsState()
    val globalBroadcast by viewModel.globalBroadcast.collectAsState()

    var manualUserInput by remember { mutableStateOf("") }
    var manualPlanType by remember { mutableStateOf("PREMIUM") }
    var broadcastInput by remember { mutableStateOf(globalBroadcast) }

    val pendingRequests = paymentRequests.filter { it.status == "PENDING" }
    val approvedRequests = paymentRequests.filter { it.status == "APPROVED" }
    val totalRevenue = approvedRequests.sumOf { it.amount }

    fun sendOtpViaWhatsApp(req: PaymentApprovalRequest) {
        val phoneWithCode = if (req.userPhone.startsWith("+")) req.userPhone else "+91${req.userPhone.replace("-", "").replace(" ", "")}"
        val message = "नमस्ते ${req.userName} जी! अमन (Ammu AI Owner) की तरफ से। आपके ₹${req.amount} के भुगतान का एक्टिवेशन टोकन है:\n\n*${req.generatedOtp}*\n\nकृपया इसे अपने Ammu ऐप में डालकर तुरंत अपना ${if (req.planType == "PREMIUM") "₹299 प्रीमियम (वॉयस & फास्ट रिस्पॉन्स)" else "₹99 प्रो"} प्लान चालू करें। धन्यवाद!"
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phoneWithCode&text=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback to regular SMS
            val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${req.userPhone}")).apply {
                putExtra("sms_body", message)
            }
            try {
                context.startActivity(smsIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "मैसेज ऐप नहीं खुला। OTP कॉपी कर लिया गया है।", Toast.LENGTH_SHORT).show()
                clipboardManager.setText(AnnotatedString(req.generatedOtp))
            }
        }
    }

    fun sendOtpViaSms(req: PaymentApprovalRequest) {
        val message = "Ammu AI OTP: आपके ₹${req.amount} के भुगतान का एक्टिवेशन कोड है: ${req.generatedOtp}। ऐप में डालकर तुरंत एक्टिवेट करें। - अमन (ओनर)"
        val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${req.userPhone}")).apply {
            putExtra("sms_body", message)
        }
        try {
            context.startActivity(smsIntent)
        } catch (_: Exception) {
            clipboardManager.setText(AnnotatedString(req.generatedOtp))
            Toast.makeText(context, "OTP कॉपी कर लिया गया है!", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        CyberSurfaceDark,
                        CyberNavyDark,
                        Color(0xFF040711)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1B4B).copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFBBF24))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF59E0B).copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("👑", fontSize = 22.sp)
                            }
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "अमन कंट्रोल सेंटर",
                                    color = Color(0xFFFBBF24),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified Owner",
                                    tint = Color(0xFF1D9BF0),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "यूजरनेम: aman_commander • ओनर मास्टर कंट्रोल",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.refreshPaymentRequests() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Real-Time Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pending Count Card
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = CyberCardDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("पेंडिंग पेमेंट्स", color = NeonAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${pendingRequests.size} नए",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text("स्वीकृति का इंतज़ार", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    }
                }

                // Active Subscribers Card
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = CyberCardDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("एप्रूव्ड पेमेंट्स", color = NeonEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${approvedRequests.size} यूजर",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text("एक्टिव ग्राहक", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    }
                }

                // Total Revenue Card
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = CyberCardDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("कुल कमाई", color = Color(0xFFFBBF24), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "₹$totalRevenue",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text("बैंक/UPI पेमेंट्स", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    }
                }
            }

            // SECTION 1: PENDING PAYMENT REQUESTS (Approve / Send OTP / Reject)
            Text(
                text = "पेंडिंग पेमेंट रिक्वेस्ट्स (${pendingRequests.size})",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )

            if (pendingRequests.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = CyberCardDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorderDark)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎉", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "कोई नई पेंडिंग पेमेंट नहीं है!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "जब भी कोई यूजर QR कोड से पेमेंट करके रिक्वेस्ट भेजेगा, वह यहाँ दिखेगा।",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                pendingRequests.forEach { req ->
                    val dateFormatted = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(req.timestamp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberNavyDark),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, if (req.planType == "PREMIUM") Color(0xFFFBBF24) else CyberCyan)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = req.userName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "फ़ोन: ${req.userPhone} • $dateFormatted",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (req.planType == "PREMIUM") Color(0xFFF59E0B) else CyberCyan
                                ) {
                                    Text(
                                        text = if (req.planType == "PREMIUM") "₹299 Premium" else "₹99 Pro",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // UTR and OTP Details Box
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0F172A),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorderDark)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("UTR/Ref No:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                        Text(req.utrNumber, color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    HorizontalDivider(color = CyberBorderDark)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "एक्टिवेशन OTP/टोकन (${if (req.planType == "PREMIUM") 12 else 6} अंक):",
                                                color = Color(0xFFFBBF24),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = req.generatedOtp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(req.generatedOtp))
                                                Toast.makeText(context, "OTP कॉपी कर लिया गया!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy OTP", tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { sendOtpViaWhatsApp(req) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366), contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Text("WhatsApp OTP 💬", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }

                                Button(
                                    onClick = { sendOtpViaSms(req) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6), contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Text("SMS OTP 📩", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.approvePaymentRequest(req.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1.5f).height(38.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("स्वीकार करें & एक्टिवेट करें ✅", fontWeight = FontWeight.Black, fontSize = 10.sp)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.rejectPaymentRequest(req.id) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Text("अस्वीकार ❌", color = NeonRose, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 2: MANUAL USER PLAN GRANT (Give plan directly)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorderDark)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                        Text(
                            text = "मैन्युअल यूजर अपग्रेड (सीधे प्लान एक्टिवेट करें)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    OutlinedTextField(
                        value = manualUserInput,
                        onValueChange = { manualUserInput = it },
                        placeholder = { Text("यूजर का फ़ोन नंबर या नाम दर्ज करें...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberBorderDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { manualPlanType = "PRO" },
                            shape = RoundedCornerShape(8.dp),
                            color = if (manualPlanType == "PRO") CyberCyan.copy(alpha = 0.2f) else CyberNavyDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (manualPlanType == "PRO") CyberCyan else CyberBorderDark)
                        ) {
                            Text(
                                text = "₹99 Ammu Pro",
                                color = if (manualPlanType == "PRO") CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { manualPlanType = "PREMIUM" },
                            shape = RoundedCornerShape(8.dp),
                            color = if (manualPlanType == "PREMIUM") Color(0xFFF59E0B).copy(alpha = 0.2f) else CyberNavyDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (manualPlanType == "PREMIUM") Color(0xFFFBBF24) else CyberBorderDark)
                        ) {
                            Text(
                                text = "₹299 Ammu Premium 👑",
                                color = if (manualPlanType == "PREMIUM") Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val target = manualUserInput.trim()
                            if (target.isBlank()) {
                                Toast.makeText(context, "कृपया फ़ोन नंबर या नाम दर्ज करें", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.manualGrantPlan(target, manualPlanType) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                if (success) manualUserInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (manualPlanType == "PREMIUM") Color(0xFFF59E0B) else CyberCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text("यूजर को प्लान तुरंत प्रदान करें ⚡", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }

            // SECTION 3: APP GLOBAL NOTICE & CONTROLS
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorderDark)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                        Text(
                            text = "ग्लोबल ब्रॉडकास्ट संदेश (सभी यूजर्स को दिखेगा)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    OutlinedTextField(
                        value = broadcastInput,
                        onValueChange = { broadcastInput = it },
                        placeholder = { Text("उदा: अमन द्वारा स्पेशल ऑफर! आज ही 299 वाला प्लान लें...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFBBF24),
                            unfocusedBorderColor = CyberBorderDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            viewModel.updateGlobalBroadcast(broadcastInput)
                            Toast.makeText(context, "ब्रॉडकास्ट मैसेज अपडेट हो गया!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ब्रॉडकास्ट संदेश सेव करें 📢", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

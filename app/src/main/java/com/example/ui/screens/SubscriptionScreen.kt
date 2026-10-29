package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.NikhilViewModel
import com.example.ui.theme.CyberBorderDark
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberNavyDark
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose

@Composable
fun SubscriptionScreen(
    viewModel: NikhilViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    val userName by viewModel.loggedUserName.collectAsState()
    val userEmail by viewModel.loggedUserEmail.collectAsState()
    val userPhone by viewModel.loggedUserPhone.collectAsState()
    val planType by viewModel.userPlanType.collectAsState()
    val daysRemaining by viewModel.planDaysRemaining.collectAsState()
    val isVerified by viewModel.isVerifiedUser.collectAsState()
    val isOwner by viewModel.isOwner.collectAsState()

    var showPaymentScannerDialog by remember { mutableStateOf(false) }
    var selectedPlanToBuy by remember { mutableStateOf("PREMIUM") } // "PRO" or "PREMIUM"
    var requesterNameInput by remember { mutableStateOf("") }
    var requesterPhoneInput by remember { mutableStateOf("") }
    var utrInput by remember { mutableStateOf("") }
    var isRequestSubmitted by remember { mutableStateOf(false) }
    var otpVerificationInput by remember { mutableStateOf("") }
    var otpVerificationError by remember { mutableStateOf<String?>(null) }
    var isSuccessDialogShown by remember { mutableStateOf(false) }

    fun openPaymentDialog(plan: String) {
        selectedPlanToBuy = plan
        requesterNameInput = userName
        requesterPhoneInput = userPhone
        utrInput = ""
        isRequestSubmitted = false
        otpVerificationInput = ""
        otpVerificationError = null
        showPaymentScannerDialog = true
    }

    val upiId = "9279753764@fam"
    val merchantName = "AMMU AI BY AMAN"

    fun openUpiIntent(amount: String, planLabel: String) {
        val uri = Uri.parse(
            "upi://pay?pa=$upiId&pn=${Uri.encode(merchantName)}&am=$amount&cu=INR&tn=${Uri.encode("Ammu $planLabel Subscription")}"
        )
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(Intent.createChooser(intent, "UPI ऐप चुनें (Paytm / GPay / PhonePe)"))
        } catch (_: Exception) {
            Toast.makeText(context, "कोई UPI ऐप नहीं मिला। कृपया QR कोड स्कैन करें या UPI ID कॉपी करें।", Toast.LENGTH_LONG).show()
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
                        Color(0xFF060914)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Exclusive Owner Control Banner (Visible only to Aman)
            if (isOwner) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.navigateTo(com.example.ui.navigation.AppDestination.OWNER_PANEL) },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFFBBF24))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("👑", fontSize = 20.sp)
                            Column {
                                Text("कमांडर अमन कंट्रोल सेंटर (Admin Hub)", color = Color(0xFFFBBF24), fontWeight = FontWeight.Black, fontSize = 13.sp)
                                Text("पेंडिंग पेमेंट्स अप्रूव करें और कस्टमर्स को OTP भेजें", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            }
                        }
                        Text("खोलें ➔", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            // Header
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
                            text = "अम्मू प्लान्स & अर्निंग हब",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = CyberCyan
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "ऑफिशियल सब्सक्रिप्शन • कमाई और प्रीमियम फीचर्स",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }

                // Blue Verified Tick Emblem
                if (isVerified) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1D9BF0).copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF1D9BF0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Member",
                                tint = Color(0xFF1D9BF0),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "वेरिफाइड",
                                color = Color(0xFF1D9BF0),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Creator Attribution
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E1B4B).copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA78BFA))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("❤️", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ammu is made by Aman",
                        color = Color(0xFFE0E7FF),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF6366F1).copy(alpha = 0.35f)
                    ) {
                        Text(
                            text = "अमन द्वारा निर्मित",
                            color = Color(0xFFA5B4FC),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Current Active Plan Status Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = CyberCardDark,
                border = androidx.compose.foundation.BorderStroke(
                    1.4.dp,
                    when (planType) {
                        "PREMIUM", "OWNER" -> Color(0xFFF59E0B)
                        "PRO" -> CyberCyan
                        else -> NeonEmerald
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "वर्तमान एक्टिव प्लान",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = when (planType) {
                                        "OWNER" -> "👑 APP OWNER (अमन सर)"
                                        "PREMIUM" -> "👑 AMMU PREMIUM (₹299/माह)"
                                        "PRO" -> "⚡ AMMU PRO (₹99/माह)"
                                        else -> "🌟 1-MONTH FREE TRIAL (मुफ़्त)"
                                    },
                                    color = when (planType) {
                                        "OWNER", "PREMIUM" -> Color(0xFFFBBF24)
                                        "PRO" -> CyberCyan
                                        else -> NeonEmerald
                                    },
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                )
                                if (isVerified) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified Member",
                                        tint = Color(0xFF1D9BF0),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Countdown Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (planType == "OWNER") Color(0xFFF59E0B).copy(alpha = 0.2f) else NeonEmerald.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (planType == "OWNER") Color(0xFFF59E0B) else NeonEmerald)
                        ) {
                            Text(
                                text = if (planType == "OWNER") "लाइफटाइम एक्टिव ♾️" else "$daysRemaining दिन शेष",
                                color = if (planType == "OWNER") Color(0xFFFBBF24) else NeonEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = CyberBorderDark)

                    // User Identity details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "मेंबर: $userName",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (userPhone.isNotBlank()) {
                                Text(
                                    text = "फ़ोन: $userPhone",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Blue badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CyberNavyDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = if (viewModel.canAccessPhoneControl()) "फोन कंट्रोल: चालू ✅" else "फोन कंट्रोल: लॉक 🔒",
                                color = if (viewModel.canAccessPhoneControl()) NeonEmerald else NeonAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Promotional Free Trial Highlight
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0F291E),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, NeonEmerald)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎁", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "पहला महीना सबके लिए बिल्कुल मुफ़्त!",
                            color = NeonEmerald,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "नए यूजर्स को पहले 30 दिनों तक फुल फोन कंट्रोल और सभी फीचर्स बिना किसी शुल्क के मिलते हैं। बाद में कमाई के लिए नीचे दिए प्लान्स ले सकते हैं।",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Text(
                text = "उपलब्ध प्लान्स (AVAILABLE TIERS)",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // PLAN 1: AMMU PRO (₹99 / Month)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardDark),
                border = androidx.compose.foundation.BorderStroke(
                    1.4.dp,
                    if (planType == "PRO") CyberCyan else CyberBorderDark
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CyberCyan.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "सिर्फ AI चैट साथी (No Voice)",
                                    color = CyberCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ammu Pro",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "₹99",
                                color = CyberCyan,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp
                            )
                            Text(
                                text = "/ महीना (Monthly)",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }

                    HorizontalDivider(color = CyberBorderDark)

                    // Features List (Ammu Pro: Chat Only)
                    PlanFeatureItem(text = "24/7 सुपर स्मार्ट AI चैट (अनलिमिटेड टेक्स्ट बातचीत)", isIncluded = true)
                    PlanFeatureItem(text = "फोटो देखकर समझ और फोटो एडिटिंग सलाह", isIncluded = true)
                    PlanFeatureItem(text = "प्रोफाइल पर 'AMMU PRO' बैज & ब्लू वेरिफाइड टिक", isIncluded = true)
                    PlanFeatureItem(text = "अम्मू वॉयस असिस्टेंट (केवल ₹299 प्रीमियम में)", isIncluded = false)
                    PlanFeatureItem(text = "फास्ट रिस्पॉन्स & स्मार्ट रिस्पॉन्स (केवल ₹299 प्रीमियम में)", isIncluded = false)
                    PlanFeatureItem(text = "फुल फोन कंट्रोल (WhatsApp रिप्लाई, ऐप कंट्रोल)", isIncluded = false)

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            openPaymentDialog("PRO")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("buy_ammu_pro_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = if (planType == "PRO") "वर्तमान एक्टिव प्लान ⚡" else "Ammu Pro चुनें (₹99 / माह)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // PLAN 2: AMMU PREMIUM (₹299 / Month) - RECOMMENDED
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF171306)),
                border = androidx.compose.foundation.BorderStroke(1.8.dp, Color(0xFFF59E0B))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.25f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "सुझावित • वॉयस + फास्ट & स्मार्ट",
                                        color = Color(0xFFFBBF24),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ammu Premium 👑",
                                color = Color(0xFFFDE68A),
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "₹299",
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp
                            )
                            Text(
                                text = "/ महीना (Monthly)",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF59E0B).copy(alpha = 0.3f))

                    // Features List (Ammu Premium: Full Voice + Fast & Smart Response + Full Control)
                    PlanFeatureItem(text = "अम्मू वॉयस असिस्टेंट (सुरीली आवाज़ + वॉयस ऑर्ब)", isIncluded = true)
                    PlanFeatureItem(text = "फास्ट रिस्पॉन्स (अल्ट्रा-फास्ट रिस्पॉन्स ⚡)", isIncluded = true)
                    PlanFeatureItem(text = "स्मार्ट रिस्पॉन्स (सुपर-स्मार्ट AI इंटेलिजेंस 🧠)", isIncluded = true)
                    PlanFeatureItem(text = "बिना छुए फुल फोन कंट्रोल (टचलेस व्हाट्सएप/SMS रिप्लाई)", isIncluded = true)
                    PlanFeatureItem(text = "24/7 बैकग्राउंड सेंटिनल (स्क्रीन बंद पर 'हे अम्मू' एक्टिव)", isIncluded = true)
                    PlanFeatureItem(text = "स्क्रीन गार्जियन (स्क्रीन पर 40 सेकंड रहने पर दोस्त की तरह मदद)", isIncluded = true)
                    PlanFeatureItem(text = "10% बैटरी पर खुद स्क्रीन लॉक और सोने की चेतावनी", isIncluded = true)
                    PlanFeatureItem(text = "लाइव GPS लोकेशन और घर की दूरी/गूगल मैप्स नेविगेशन", isIncluded = true)
                    PlanFeatureItem(text = "गोल्डन 'AMMU PREMIUM' बैज & ब्लू वेरिफाइड टिक (✓)", isIncluded = true)

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            openPaymentDialog("PREMIUM")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("buy_ammu_premium_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B),
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = if (planType == "PREMIUM") "वर्तमान एक्टिव प्लान 👑" else "Ammu Premium लें (₹299 / माह) 🚀",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Direct Payment Scanner & UPI Hub Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CyberNavyDark,
                border = androidx.compose.foundation.BorderStroke(1.2.dp, CyberCyan.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "पेमेंट स्कैनर (UPI QR & Direct Pay)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NeonEmerald.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "100% सेफ UPI",
                                color = NeonEmerald,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "GPay, PhonePe, Paytm, CRED या किसी भी UPI ऐप से सीधे स्कैन करके या UPI ऐप खोलकर भुगतान करें।",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )

                    // Embedded QR Code preview directly on page
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        color = Color(0xFF0A0F1D),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberCyan)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = com.example.R.drawable.ammu_payment_qr),
                                contentDescription = "FamPay UPI QR Code Scanner (9279753764@fam)",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("ऑफिशियल UPI ID:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                    Text(upiId, color = CyberCyan, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                }
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(upiId))
                                        Toast.makeText(context, "UPI ID कॉपी हो गई: $upiId", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyberCyan, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { openPaymentDialog("PRO") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text("Pro लें (₹99)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { openPaymentDialog("PREMIUM") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text("Premium लें (₹299)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Interactive Payment Scanner & Owner Approval Flow Dialog
        if (showPaymentScannerDialog) {
            val amount = if (selectedPlanToBuy == "PREMIUM") "299" else "99"
            val planTitle = if (selectedPlanToBuy == "PREMIUM") "Ammu Premium (₹299)" else "Ammu Pro (₹99)"
            val requiredDigits = if (selectedPlanToBuy == "PREMIUM") 12 else 6

            AlertDialog(
                onDismissRequest = { showPaymentScannerDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$planTitle पेमेंट",
                                color = if (selectedPlanToBuy == "PREMIUM") Color(0xFFFBBF24) else CyberCyan,
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "राशि: ₹$amount • अमन (ओनर) द्वारा वेरीफाई",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = { showPaymentScannerDialog = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // FamPay QR Code Screenshot
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp)),
                            color = Color(0xFF0F172A),
                            border = androidx.compose.foundation.BorderStroke(2.dp, if (selectedPlanToBuy == "PREMIUM") Color(0xFFFBBF24) else CyberCyan)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    painter = painterResource(id = com.example.R.drawable.ammu_payment_qr),
                                    contentDescription = "FamPay UPI QR Code Screenshot (9279753764@fam)",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                        }

                        Text(
                            text = "चरण 1: स्कैन करें और ₹$amount का भुगतान करें",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )

                        // UPI ID Copy Row
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)),
                            color = CyberNavyDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorderDark)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("UPI ID:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                    Text(upiId, color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(upiId))
                                        Toast.makeText(context, "UPI ID कॉपी हो गई!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyberCyan, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // One-tap UPI App Launcher Button
                        Button(
                            onClick = {
                                openUpiIntent(amount, planTitle)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            )
                        ) {
                            Text("किसी भी UPI ऐप से पे करें (GPay / PhonePe) ➔", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        HorizontalDivider(color = CyberBorderDark)

                        if (!isRequestSubmitted) {
                            // Step 2: Submit Details to Owner
                            Text(
                                text = "चरण 2: पेमेंट के बाद अमन (ओनर) को विवरण भेजें",
                                color = if (selectedPlanToBuy == "PREMIUM") Color(0xFFFBBF24) else CyberCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = requesterNameInput,
                                onValueChange = { requesterNameInput = it },
                                label = { Text("आपका नाम (Name)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = requesterPhoneInput,
                                onValueChange = { requesterPhoneInput = it },
                                label = { Text("मोबाइल नंबर (जहाँ अमन सर OTP भेजेंगे)") },
                                placeholder = { Text("उदा: 9876543210") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = utrInput,
                                onValueChange = { utrInput = it },
                                label = { Text("12-अंकों का UPI UTR / Ref No.") },
                                placeholder = { Text("उदा: 429810238472") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (otpVerificationError != null) {
                                Text(
                                    text = otpVerificationError ?: "",
                                    color = Color(0xFFEF4444),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    val name = requesterNameInput.trim()
                                    val phone = requesterPhoneInput.trim()
                                    val utr = utrInput.trim()

                                    if (phone.length < 10) {
                                        otpVerificationError = "कृपया 10 अंकों का वैध मोबाइल नंबर दर्ज करें"
                                        return@Button
                                    }
                                    if (utr.length < 6) {
                                        otpVerificationError = "कृपया 12 अंकों का वैध UTR नंबर दर्ज करें"
                                        return@Button
                                    }

                                    otpVerificationError = null
                                    viewModel.submitPaymentApprovalRequest(name, phone, selectedPlanToBuy, utr)
                                    isRequestSubmitted = true
                                    Toast.makeText(context, "पेमेंट अनुरोध अमन (ओनर) को भेज दिया गया! 📤", Toast.LENGTH_LONG).show()
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedPlanToBuy == "PREMIUM") Color(0xFFF59E0B) else CyberCyan,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text("पेमेंट कर दी है - अमन (ओनर) को रिक्वेस्ट भेजें 📤", fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        } else {
                            // Request Submitted: Waiting for Owner's OTP / Direct Approval
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp)),
                                color = Color(0xFF0F231D),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("✅", fontSize = 16.sp)
                                        Text(
                                            text = "अनुरोध सफलतापूर्वक दर्ज हो गया!",
                                            color = NeonEmerald,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(
                                        text = "जैसे ही अमन सर के बैंक खाते में ₹$amount का भुगतान प्राप्त होगा, वह आपके नंबर ($requesterPhoneInput) पर WhatsApp/SMS से $requiredDigits-अंकों का एक्टिवेशन कोड भेजेंगे या सीधे अप्रूव कर देंगे।",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            // Enter Owner Dispatched OTP
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "अमन सर द्वारा भेजा गया $requiredDigits-अंकों का कोड दर्ज करें:",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = otpVerificationInput,
                                    onValueChange = {
                                        val filtered = it.filter { ch -> ch.isDigit() }
                                        if (filtered.length <= requiredDigits) {
                                            otpVerificationInput = filtered
                                            otpVerificationError = null
                                        }
                                    },
                                    placeholder = { Text("अमन सर से प्राप्त कोड दर्ज करें...") },
                                    singleLine = true,
                                    isError = otpVerificationError != null,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = if (selectedPlanToBuy == "PREMIUM") Color(0xFFFBBF24) else CyberCyan,
                                        unfocusedBorderColor = CyberBorderDark,
                                        errorBorderColor = Color(0xFFEF4444)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (otpVerificationError != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = otpVerificationError ?: "",
                                        color = Color(0xFFEF4444),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val entered = otpVerificationInput.trim()
                                    if (entered.isBlank()) {
                                        otpVerificationError = "कृपया अमन सर द्वारा भेजा गया कोड दर्ज करें"
                                        return@Button
                                    }
                                    viewModel.verifyActivationOtp(entered) { success, msg ->
                                        if (success) {
                                            showPaymentScannerDialog = false
                                            isSuccessDialogShown = true
                                        } else {
                                            otpVerificationError = msg
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedPlanToBuy == "PREMIUM") Color(0xFFF59E0B) else CyberCyan,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text("एक्टिवेशन कोड वेरीफाई करें & शुरू करें ⚡", fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }

                            // Check status button in case Owner approved directly from his portal
                            OutlinedButton(
                                onClick = {
                                    if (viewModel.userPlanType.value == selectedPlanToBuy) {
                                        showPaymentScannerDialog = false
                                        isSuccessDialogShown = true
                                    } else {
                                        Toast.makeText(context, "अभी अमन सर का अप्रूवल पेंडिंग है। कृपया SMS/WhatsApp कोड का इंतज़ार करें।", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(38.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("अमन सर ने अप्रूव किया? स्टेटस चेक करें 🔄", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showPaymentScannerDialog = false }) {
                        Text("बंद करें", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }

        // Celebration & Plan Activated Dialog
        if (isSuccessDialogShown) {
            AlertDialog(
                onDismissRequest = { isSuccessDialogShown = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🎉", fontSize = 24.sp)
                        Text("प्लान एक्टिवेट हो गया!", color = NeonEmerald, fontWeight = FontWeight.Black)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "बधाई हो $userName जी!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "आपका $selectedPlanToBuy सब्सक्रिप्शन 30 दिनों के लिए सफलतापूर्वक एक्टिवेट हो गया है। आपकी प्रोफाइल में ब्लू वेरिफाइड टिक और सब्सक्राइबर बैज जोड़ दिया गया है!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { isSuccessDialogShown = false },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color.Black)
                    ) {
                        Text("शानदार! अम्मू शुरू करें", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
fun PlanFeatureItem(text: String, isIncluded: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isIncluded) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = NeonEmerald,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = if (isIncluded) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = if (isIncluded) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
fun ProceduralUpiQrPattern(amount: String, upiId: String) {
    Canvas(modifier = Modifier.size(160.dp)) {
        val blockSize = size.width / 18f
        val darkColor = Color(0xFF0F172A)

        // Draw Corner Finder Patterns (standard QR markers)
        fun drawFinder(startX: Float, startY: Float) {
            drawRect(
                color = darkColor,
                topLeft = Offset(startX, startY),
                size = Size(blockSize * 5, blockSize * 5)
            )
            drawRect(
                color = Color.White,
                topLeft = Offset(startX + blockSize, startY + blockSize),
                size = Size(blockSize * 3, blockSize * 3)
            )
            drawRect(
                color = darkColor,
                topLeft = Offset(startX + blockSize * 2, startY + blockSize * 2),
                size = Size(blockSize, blockSize)
            )
        }

        drawFinder(0f, 0f)
        drawFinder(size.width - blockSize * 5, 0f)
        drawFinder(0f, size.height - blockSize * 5)

        // Procedural QR Data dots
        val seed = (amount.hashCode() xor upiId.hashCode())
        for (row in 0..17) {
            for (col in 0..17) {
                // Avoid finders
                val inTopLeft = row < 5 && col < 5
                val inTopRight = row < 5 && col > 12
                val inBottomLeft = row > 12 && col < 5
                val inCenter = row in 7..10 && col in 7..10

                if (!inTopLeft && !inTopRight && !inBottomLeft && !inCenter) {
                    val pseudoRandom = ((seed * (row * 31 + col * 17) + 1013904223) ushr 16) and 1
                    if (pseudoRandom == 1) {
                        drawRect(
                            color = darkColor,
                            topLeft = Offset(col * blockSize, row * blockSize),
                            size = Size(blockSize, blockSize)
                        )
                    }
                }
            }
        }
    }
}

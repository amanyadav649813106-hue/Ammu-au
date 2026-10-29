package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.VoiceState
import com.example.ui.NikhilViewModel
import com.example.ui.components.AmmuVoiceOrbVisualizer
import com.example.ui.theme.CyberBorderDark
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberNavyDark
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose

@Composable
fun AuthScreen(
    viewModel: NikhilViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Mode: 0 = Sign In, 1 = Sign Up
    var authModeIndex by remember { mutableStateOf(0) }
    val isSignUp = authModeIndex == 1

    // Channel: 0 = Gmail / Email, 1 = Phone Number
    var channelIndex by remember { mutableStateOf(0) }
    val isEmailChannel = channelIndex == 0

    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showSecretOwnerDialog by remember { mutableStateOf(false) }
    var secretOwnerUserInput by remember { mutableStateOf("aman_commander") }
    var secretOwnerPassInput by remember { mutableStateOf("") }

    fun handleSubmit() {
        focusManager.clearFocus()
        val password = passwordInput.trim()
        val name = nameInput.trim()

        if (password.length < 4) {
            Toast.makeText(context, "पासवर्ड कम से कम 4 अक्षरों का होना चाहिए", Toast.LENGTH_SHORT).show()
            return
        }

        if (isSignUp && name.isBlank()) {
            Toast.makeText(context, "कृपया अपना पूरा नाम दर्ज करें", Toast.LENGTH_SHORT).show()
            return
        }

        isSubmitting = true

        if (isEmailChannel) {
            val email = emailInput.trim().lowercase()
            val isOwnerUsername = email == "aman_commander" || email == "aman" || email == "aman@ammu.ai" || email == "owner"
            if (!isSignUp && isOwnerUsername) {
                viewModel.loginUserWithIdentifier(
                    identifier = email,
                    password = password
                ) { success, message ->
                    isSubmitting = false
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
                return
            }

            if (email.isBlank() || !email.contains("@")) {
                Toast.makeText(context, "कृपया वैध Gmail या ईमेल पता दर्ज करें (उदा: user@gmail.com)", Toast.LENGTH_SHORT).show()
                isSubmitting = false
                return
            }

            if (isSignUp) {
                viewModel.registerUserWithEmail(
                    name = name,
                    email = email,
                    password = password
                ) { success, message ->
                    isSubmitting = false
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            } else {
                viewModel.loginUserWithIdentifier(
                    identifier = email,
                    password = password
                ) { success, message ->
                    isSubmitting = false
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            val phone = phoneInput.trim()
            if (phone.length < 10) {
                Toast.makeText(context, "कृपया 10 अंकों का वैध मोबाइल नंबर दर्ज करें", Toast.LENGTH_SHORT).show()
                isSubmitting = false
                return
            }

            if (isSignUp) {
                viewModel.registerUserWithPhone(
                    name = name,
                    phone = phone,
                    password = password
                ) { success, message ->
                    isSubmitting = false
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            } else {
                viewModel.loginUserWithIdentifier(
                    identifier = phone,
                    password = password
                ) { success, message ->
                    isSubmitting = false
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        CyberSurfaceDark,
                        CyberNavyDark,
                        Color(0xFF070B19)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Ammu Voice Orb Visualizer Hero
            AmmuVoiceOrbVisualizer(
                size = 85.dp,
                voiceState = VoiceState.IDLE
            )

            Spacer(modifier = Modifier.height(10.dp))

            // App Title & Tagline
            Text(
                text = "AMMU // अम्मू",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = CyberCyan
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "सुपर-इंटेलिजेंट AI वॉयस साथी • 24/7 फुल फोन कंट्रोल",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Creator Attribution (Clickable for Owner Secret Access)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showSecretOwnerDialog = true },
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E1B4B).copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA78BFA))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp),
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

            Spacer(modifier = Modifier.height(12.dp))

            // 1st Month Free Trial Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = NeonEmerald.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, NeonEmerald)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null,
                        tint = NeonEmerald,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "पहला महीना 100% फ्री ट्रायल 🎁",
                            color = NeonEmerald,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "हर नए यूजर के लिए 30 दिन तक सब कुछ अनलॉक!",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Auth Container
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = CyberCardDark,
                border = androidx.compose.foundation.BorderStroke(1.2.dp, CyberCyan.copy(alpha = 0.45f)),
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Sign In / Sign Up Tabs
                    TabRow(
                        selectedTabIndex = authModeIndex,
                        containerColor = CyberNavyDark,
                        contentColor = CyberCyan,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[authModeIndex]),
                                color = CyberCyan,
                                height = 3.dp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Tab(
                            selected = !isSignUp,
                            onClick = { authModeIndex = 0 },
                            text = {
                                Text(
                                    text = "लॉगिन (Log In)",
                                    fontWeight = if (!isSignUp) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (!isSignUp) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                        Tab(
                            selected = isSignUp,
                            onClick = { authModeIndex = 1 },
                            text = {
                                Text(
                                    text = "नया अकाउंट (Sign Up)",
                                    fontWeight = if (isSignUp) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isSignUp) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Channel Switcher: Gmail vs Phone Number
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberNavyDark)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { channelIndex = 0 },
                            color = if (isEmailChannel) CyberCyan else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = if (isEmailChannel) Color.Black else CyberCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Gmail / ईमेल",
                                    color = if (isEmailChannel) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { channelIndex = 1 },
                            color = if (!isEmailChannel) CyberCyan else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = if (!isEmailChannel) Color.Black else CyberCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "फ़ोन नंबर",
                                    color = if (!isEmailChannel) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 1 ID Rule Indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = NeonAmber,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEmailChannel) "1 Gmail = केवल 1 ID मान्य" else "1 फ़ोन नंबर = केवल 1 ID मान्य",
                            color = NeonAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Name Input (Only on Sign Up)
                    AnimatedVisibility(
                        visible = isSignUp,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column {
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text("आपका नाम (Full Name)") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = CyberCyan)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = CyberBorderDark,
                                    focusedLabelColor = CyberCyan,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("name_input_field")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    // Email or Phone Input
                    if (isEmailChannel) {
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Gmail या ईमेल पता (user@gmail.com)") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = CyberCyan)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = CyberBorderDark,
                                focusedLabelColor = CyberCyan,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_input_field")
                        )
                    } else {
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { if (it.length <= 10 && it.all { char -> char.isDigit() }) phoneInput = it },
                            label = { Text("10 अंकों का मोबाइल नंबर") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = CyberCyan)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = CyberBorderDark,
                                focusedLabelColor = CyberCyan,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("phone_input_field")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Password Input (User Creates/Enters Password)
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text(if (isSignUp) "अपना पासवर्ड बनाएँ (Create Password)" else "पासवर्ड (Password)") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = CyberCyan)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Password Visibility",
                                    tint = CyberCyan
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { handleSubmit() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberBorderDark,
                            focusedLabelColor = CyberCyan,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input_field")
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Submit Button
                    Button(
                        onClick = { handleSubmit() },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("auth_submit_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = Color.Black
                        )
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isSignUp) "अकाउंट बनाएँ और 1 महीना फ्री शुरू करें 🚀" else "अम्मू में लॉगिन करें ⚡",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "सुरक्षित एन्क्रिप्शन • आपका डेटा केवल आपके डिवाइस पर सुरक्षित है",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Secret Owner (Aman) Login Dialog
        if (showSecretOwnerDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showSecretOwnerDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "👑 कमांडर अमन - ओनर पोर्टल",
                            color = Color(0xFFFBBF24),
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "यह सिर्फ ऐप मालिक (Aman) के लिए सुरक्षित कंट्रोल सेंटर है:",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        OutlinedTextField(
                            value = secretOwnerUserInput,
                            onValueChange = { secretOwnerUserInput = it },
                            label = { Text("यूजरनेम (Username)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFBBF24),
                                focusedLabelColor = Color(0xFFFBBF24)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = secretOwnerPassInput,
                            onValueChange = { secretOwnerPassInput = it },
                            label = { Text("पासवर्ड (Password)") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFBBF24),
                                focusedLabelColor = Color(0xFFFBBF24)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val success = viewModel.loginAsOwner(secretOwnerUserInput, secretOwnerPassInput)
                            if (success) {
                                showSecretOwnerDialog = false
                                Toast.makeText(context, "स्वागत है कमांडर अमन सर! 👑", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "गलत क्रेडेंशियल्स! अनधिकृत एक्सेस अस्वीकृत।", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B),
                            contentColor = Color.Black
                        )
                    ) {
                        Text("मास्टर लॉगिन ⚡", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showSecretOwnerDialog = false }) {
                        Text("रद्द करें", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}

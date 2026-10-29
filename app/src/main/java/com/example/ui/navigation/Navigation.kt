package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(
    val title: String,
    val icon: ImageVector
) {
    VOICE_HUD("अम्मू वॉयस", Icons.Default.GraphicEq),
    CHAT("अम्मू चैट", Icons.Default.ChatBubble),
    CONTROLS("कंट्रोल डेक", Icons.Default.PhoneAndroid),
    SCREEN_GUARDIAN("स्क्रीन & GPS", Icons.Default.RemoveRedEye),
    PLANS("अम्मू प्लान्स", Icons.Default.WorkspacePremium),
    OWNER_PANEL("ओनर हब 👑", Icons.Default.Security)
}



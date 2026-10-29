package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = CyberCyanDim,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = CyberBorderDark,
    onSecondaryContainer = Color(0xFFD6F5FF),
    tertiary = NeonEmerald,
    onTertiary = Color(0xFF003822),
    background = CyberNavyDark,
    onBackground = TextPrimaryDark,
    surface = CyberSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CyberCardDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = CyberBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = CyberBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCBE6FF),
    onPrimaryContainer = Color(0xFF001E30),
    secondary = CyberCyanDim,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDF3FF),
    onSecondaryContainer = Color(0xFF001F29),
    tertiary = NeonEmerald,
    onTertiary = Color.White,
    background = CyberLightBg,
    onBackground = TextPrimaryLight,
    surface = CyberLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = CyberLightCard,
    onSurfaceVariant = TextSecondaryLight,
    outline = CyberLightBorder
)

@Composable
fun NikhilAITheme(
    darkTheme: Boolean = true, // Default to futuristic cyber dark mode
    dynamicColor: Boolean = false, // Keep cyber colors cohesive
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

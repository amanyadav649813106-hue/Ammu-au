package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.NikhilViewModel
import com.example.ui.navigation.AppDestination
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ControlCenterScreen
import com.example.ui.screens.OwnerAdminScreen
import com.example.ui.screens.ScreenGuardianScreen
import com.example.ui.screens.SubscriptionScreen
import com.example.ui.screens.VoiceHudScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.NikhilAITheme

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.view.WindowManager

class MainActivity : ComponentActivity() {
    private val viewModel: NikhilViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isLoggedIn by viewModel.isLoggedIn.collectAsState()
            val isDark = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            NikhilAITheme(darkTheme = isDark) {
                if (!isLoggedIn) {
                    AuthScreen(viewModel = viewModel)
                } else {
                    MainAppScaffold(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppScaffold(viewModel: NikhilViewModel) {
    val currentDestination by viewModel.currentDestination.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Multiple Permissions Launcher for Full Phone Access (Audio, Call, Location, Camera)
    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        viewModel.refreshPermissions()
        val audioGranted = results[Manifest.permission.RECORD_AUDIO] == true
        if (!audioGranted) {
            Toast.makeText(context, "आवाज़ से बात करने के लिए माइक्रोफोन की अनुमति दें", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            multiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val isOwner by viewModel.isOwner.collectAsState()
            val visibleDestinations = if (isOwner) {
                listOf(
                    AppDestination.VOICE_HUD,
                    AppDestination.CHAT,
                    AppDestination.CONTROLS,
                    AppDestination.PLANS,
                    AppDestination.OWNER_PANEL
                )
            } else {
                listOf(
                    AppDestination.VOICE_HUD,
                    AppDestination.CHAT,
                    AppDestination.CONTROLS,
                    AppDestination.SCREEN_GUARDIAN,
                    AppDestination.PLANS
                )
            }

            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar"),
                containerColor = CyberSurfaceDark,
                contentColor = CyberCyan,
                tonalElevation = 8.dp
            ) {
                visibleDestinations.forEach { destination ->
                    val isSelected = currentDestination == destination
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.navigateTo(destination) },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.title,
                                tint = if (isSelected) {
                                    if (destination == AppDestination.OWNER_PANEL) Color(0xFFFBBF24) else CyberCyan
                                } else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                text = destination.title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                maxLines = 1,
                                color = if (isSelected) {
                                    if (destination == AppDestination.OWNER_PANEL) Color(0xFFFBBF24) else CyberCyan
                                } else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = if (destination == AppDestination.OWNER_PANEL) Color(0xFFF59E0B).copy(alpha = 0.25f) else CyberCyan.copy(alpha = 0.2f),
                            selectedIconColor = if (destination == AppDestination.OWNER_PANEL) Color(0xFFFBBF24) else CyberCyan,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentDestination) {
                AppDestination.VOICE_HUD -> VoiceHudScreen(viewModel = viewModel)
                AppDestination.CHAT -> ChatScreen(viewModel = viewModel)
                AppDestination.CONTROLS -> ControlCenterScreen(viewModel = viewModel)
                AppDestination.SCREEN_GUARDIAN -> ScreenGuardianScreen(viewModel = viewModel)
                AppDestination.PLANS -> SubscriptionScreen(viewModel = viewModel)
                AppDestination.OWNER_PANEL -> OwnerAdminScreen(viewModel = viewModel)
            }
        }
    }
}

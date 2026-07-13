package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.QuranViewModel
import com.example.ui.screens.*
import com.example.ui.theme.Localization
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: QuranViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = isDarkMode) {
                // Custom Stack-based Navigation Engine (Fast, Bulletproof, Zero compilation errors)
                var currentScreen by remember { mutableStateOf("Home") }
                val backStack = remember { mutableStateListOf<String>() }

                val navigateTo = { screen: String ->
                    backStack.add(currentScreen)
                    currentScreen = screen
                }

                val goBack = {
                    if (backStack.isNotEmpty()) {
                        currentScreen = backStack.removeAt(backStack.size - 1)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // Display bottom nav only for main hubs (Home, Quran, Prayer, Assistant)
                        val mainHubs = listOf("Home", "Quran", "Prayer", "Assistant")
                        if (currentScreen in mainHubs) {
                            NavigationBar(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                                    .background(MaterialTheme.colorScheme.surface),
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen == "Home",
                                    onClick = {
                                        backStack.clear()
                                        currentScreen = "Home"
                                    },
                                    icon = { Icon(Icons.Default.Home, contentDescription = Localization.translate("home", appLanguage)) },
                                    label = { Text(Localization.translate("home", appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFD4AF37))
                                )
                                NavigationBarItem(
                                    selected = currentScreen == "Quran",
                                    onClick = {
                                        backStack.clear()
                                        currentScreen = "Quran"
                                    },
                                    icon = { Icon(Icons.Default.Book, contentDescription = Localization.translate("quran", appLanguage)) },
                                    label = { Text(Localization.translate("quran", appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFD4AF37))
                                )
                                NavigationBarItem(
                                    selected = currentScreen == "Prayer",
                                    onClick = {
                                        backStack.clear()
                                        currentScreen = "Prayer"
                                    },
                                    icon = { Icon(Icons.Default.Schedule, contentDescription = Localization.translate("prayer_times", appLanguage)) },
                                    label = { Text(Localization.translate("prayer_times", appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFD4AF37))
                                )
                                NavigationBarItem(
                                    selected = currentScreen == "Assistant",
                                    onClick = {
                                        backStack.clear()
                                        currentScreen = "Assistant"
                                    },
                                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = Localization.translate("ai_scholar", appLanguage)) },
                                    label = { Text(Localization.translate("ai_scholar", appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFD4AF37))
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "ScreenTransition"
                        ) { screen ->
                            when (screen) {
                                "Home" -> HomeScreen(viewModel, onNavigateToFeature = navigateTo)
                                "Quran" -> QuranScreen(viewModel, onBack = { currentScreen = "Home" })
                                "Prayer" -> PrayerScreen(viewModel, onBack = { currentScreen = "Home" })
                                "Assistant" -> AssistantScreen(viewModel, onBack = { currentScreen = "Home" })
                                "Qibla" -> QiblaScreen(viewModel, onBack = goBack)
                                "Azkar" -> AzkarScreen(viewModel, onBack = goBack)
                                "Khatmah" -> KhatmahScreen(viewModel, onBack = goBack)
                                "Quiz" -> MemorizationQuizScreen(viewModel, onBack = goBack)
                                "Duas" -> DuasScreen(viewModel, onBack = goBack)
                                "Audio" -> AudioQuranScreen(viewModel, onBack = goBack)
                                "Tasbeeh" -> TasbeehScreen(viewModel, onBack = goBack)
                                "Settings" -> SettingsScreen(viewModel, onBack = goBack)
                                "Profile" -> ProfileScreen(viewModel, onBack = goBack)
                                else -> HomeScreen(viewModel, onNavigateToFeature = navigateTo)
                            }
                        }
                    }
                }
            }
        }
    }
}

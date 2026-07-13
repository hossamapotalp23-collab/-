package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.prayer.PrayerCalculator
import com.example.data.quran.QuranDataset
import com.example.ui.QuranViewModel
import com.example.ui.theme.Localization
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: QuranViewModel,
    onNavigateToFeature: (String) -> Unit
) {
    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()
    val activeKhatmah by viewModel.activeKhatmah.collectAsStateWithLifecycle()
    val prayerTimes by viewModel.prayerTimes.collectAsStateWithLifecycle()
    val nextPrayerName by viewModel.nextPrayerName.collectAsStateWithLifecycle()
    val nextPrayerCountdown by viewModel.nextPrayerCountdown.collectAsStateWithLifecycle()
    val city by viewModel.currentLocationName.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val hijriDateString by viewModel.hijriDateString.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var userName by remember { mutableStateOf("Hossam") }
    var isEditingName by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Decorative colors
    val emeraldPrimary = MaterialTheme.colorScheme.primary
    val goldSecondary = Color(0xFFD4AF37)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 80.dp) // Avoid overlap with bottom nav
    ) {
        // 1. Welcome Header (Islamic Arch Gradient Shape)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            emeraldPrimary,
                            emeraldPrimary.copy(alpha = 0.8f)
                        )
                    ),
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
                .padding(top = 16.dp, bottom = 32.dp, start = 24.dp, end = 24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = Localization.translate("welcome", appLanguage),
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 14.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val user = currentUser
                            if (user != null) {
                                Text(
                                    text = "${user.displayName ?: user.email.substringBefore("@")} ✨",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                if (isEditingName) {
                                    OutlinedTextField(
                                        value = userName,
                                        onValueChange = { userName = it },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedTextColor = Color.White,
                                            focusedTextColor = Color.White,
                                            focusedBorderColor = goldSecondary,
                                            unfocusedBorderColor = Color.White
                                        ),
                                        modifier = Modifier.width(150.dp),
                                        singleLine = true
                                    )
                                    IconButton(onClick = { isEditingName = false }) {
                                        Icon(Icons.Default.Check, contentDescription = Localization.translate("save", appLanguage), tint = goldSecondary)
                                    }
                                } else {
                                    Text(
                                        text = "$userName ✨",
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(onClick = { isEditingName = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = Localization.translate("edit_name", appLanguage), tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Pulse Ticking Live Clock
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = currentTime,
                                    color = goldSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = Localization.translate("utc_live", appLanguage),
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Circular Profile Avatar button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .border(1.5.dp, goldSecondary, CircleShape)
                                .clickable { onNavigateToFeature("Profile") }
                                .testTag("profile_avatar_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            val user = currentUser
                            if (user?.photoUrl != null) {
                                AsyncImage(
                                    model = user.photoUrl,
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = if (user != null) Icons.Default.Person else Icons.Default.AccountCircle,
                                    contentDescription = "Profile",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // Modern Date & Sun-tracking Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = hijriDateString,
                                color = goldSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
                            Text(
                                text = sdf.format(Date()),
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                        }

                        // Next Prayer Banner
                        if (prayerTimes != null) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${Localization.translate("next_prayer", appLanguage)}: $nextPrayerName",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = nextPrayerCountdown,
                                    color = goldSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Daily Verse Card (Glow card overlapping)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-16).dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Verse", tint = goldSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Localization.translate("daily_verse", appLanguage), fontWeight = FontWeight.Bold, color = emeraldPrimary)
                    }
                    Text("Juz 1", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = goldSecondary)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = QuranDataset.dailyVerse.textArabic,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right,
                    lineHeight = 28.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = QuranDataset.dailyVerse.textTranslation,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { onNavigateToFeature("Quran") }) {
                        Text(Localization.translate("read_tafsir", appLanguage), color = goldSecondary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = { viewModel.toggleAudioPlayback(QuranDataset.dailyVerse) },
                        modifier = Modifier.background(emeraldPrimary, CircleShape)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play Verse", tint = Color.White)
                    }
                }
            }
        }

        // 3. Quick Access Grid
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                Localization.translate("quick_services", appLanguage),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = emeraldPrimary
            )

            val features = listOf(
                FeatureItem("quran", "Quran", Icons.Default.Book, "Quran"),
                FeatureItem("prayer_times", "Prayer Times", Icons.Default.Schedule, "Prayer"),
                FeatureItem("qibla", "Qibla", Icons.Default.Navigation, "Qibla"),
                FeatureItem("azkar", "Azkar", Icons.Default.MenuBook, "Azkar"),
                FeatureItem("audio_quran", "Audio Quran", Icons.Default.Audiotrack, "Audio"),
                FeatureItem("khatmah", "Khatmah", Icons.Default.TrackChanges, "Khatmah"),
                FeatureItem("quiz", "Quiz", Icons.Default.School, "Quiz"),
                FeatureItem("duas", "Duas", Icons.Default.Favorite, "Duas"),
                FeatureItem("tasbeeh", "Tasbeeh", Icons.Default.Lens, "Tasbeeh"),
                FeatureItem("settings", "Settings", Icons.Default.Settings, "Settings")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Render custom grid-breaking visual rows
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    features.take(5).forEach { feat ->
                        FeatureCard(feat, appLanguage, onNavigateToFeature)
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    features.drop(5).forEach { feat ->
                        FeatureCard(feat, appLanguage, onNavigateToFeature)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. Continue reading and listening row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Continue Quran Reading Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToFeature("Quran") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Bookmark, contentDescription = "Reading progress", tint = goldSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(Localization.translate("continue_reading", appLanguage), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Surah Al-Fatihah, Ayah 1", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Continue Quran Listening Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToFeature("Audio") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Audiotrack, contentDescription = "Audio Recitations", tint = emeraldPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(Localization.translate("quran_listening", appLanguage), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Mishary Alafasy", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5. Daily Hadith Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MenuBook, contentDescription = "Hadith", tint = goldSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Localization.translate("daily_hadith", appLanguage), fontWeight = FontWeight.Bold, color = emeraldPrimary, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"${QuranDataset.dailyHadith}\"",
                    fontSize = 13.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

data class FeatureItem(
    val key: String,
    val name: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun FeatureCard(item: FeatureItem, appLanguage: String, onNavigate: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate(item.route) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.name,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = Localization.translate(item.key, appLanguage),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

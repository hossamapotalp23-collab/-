package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
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

    // Premium Color Accents from 2030 design spec
    val emeraldPrimary = Color(0xFF0F8F6B)
    val darkGreenSecondary = Color(0xFF063B35)
    val goldAccent = Color(0xFFD4AF37)
    val deepBlackBg = Color(0xFF040A07)

    // Soft Green & Obsidian Radial Gradient Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0D2D22), // Soft bright green center glow
                        Color(0xFF040B08), // Rich obsidian intermediate
                        Color(0xFF020403)  // Deep cosmic black edges
                    ),
                    radius = 1800f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 88.dp) // Generous padding to avoid bottom nav overlay
        ) {
            // 1. TOP HEADER: Futuristic Dashboard Greeting Panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, start = 16.dp, end = 16.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = Localization.translate("welcome", appLanguage).uppercase(),
                            color = emeraldPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val user = currentUser
                            if (user != null) {
                                Text(
                                    text = "${user.displayName ?: user.email.substringBefore("@")}",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            } else {
                                if (isEditingName) {
                                    OutlinedTextField(
                                        value = userName,
                                        onValueChange = { userName = it },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedTextColor = Color.White,
                                            focusedTextColor = Color.White,
                                            focusedBorderColor = goldAccent,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier.width(160.dp),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    IconButton(
                                        onClick = { isEditingName = false },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = Localization.translate("save", appLanguage), tint = goldAccent)
                                    }
                                } else {
                                    Text(
                                        text = userName,
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    IconButton(
                                        onClick = { isEditingName = true },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = Localization.translate("edit_name", appLanguage),
                                            tint = Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = " ✨",
                                fontSize = 20.sp
                            )
                        }
                    }

                    // Pulse Ticking Live Clock & Avatar Group
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Futuristic Digital Clock
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = currentTime,
                                    color = goldAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = Localization.translate("utc_live", appLanguage).uppercase(),
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        // Circular Profile Avatar with Glowing Gold Border
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(BorderStroke(1.5.dp, goldAccent), CircleShape)
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
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. MODERN HIJRI & ADHAN COUNTDOWN GLASS CARD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Glassmorphic backdrop card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(24.dp))
                        .border(
                            BorderStroke(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.Transparent))),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = emeraldPrimary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = city.uppercase(),
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = hijriDateString,
                                color = goldAccent,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp
                            )
                            val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
                            Text(
                                text = sdf.format(Date()),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }

                        // Countdown indicator
                        if (prayerTimes != null) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(emeraldPrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                        .border(BorderStroke(1.dp, emeraldPrimary.copy(alpha = 0.4f)), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${Localization.translate("next_prayer", appLanguage)}: $nextPrayerName",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = nextPrayerCountdown,
                                    color = goldAccent,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    // Horizontal Sliding Elegant Prayer Times
                    if (prayerTimes != null) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = if (appLanguage == "Arabic" || appLanguage == "العربية") "أوقات الصلاة اليوم" else "TODAY'S PRAYER TIMES",
                            color = Color.White.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        val prayerNames = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
                        val translatedNames = mapOf(
                            "Fajr" to (if (appLanguage == "Arabic" || appLanguage == "العربية") "الفجر" else "Fajr"),
                            "Dhuhr" to (if (appLanguage == "Arabic" || appLanguage == "العربية") "الظهر" else "Dhuhr"),
                            "Asr" to (if (appLanguage == "Arabic" || appLanguage == "العربية") "العصر" else "Asr"),
                            "Maghrib" to (if (appLanguage == "Arabic" || appLanguage == "العربية") "المغرب" else "Maghrib"),
                            "Isha" to (if (appLanguage == "Arabic" || appLanguage == "العربية") "العشاء" else "Isha")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            prayerNames.forEach { pName ->
                                val timeStr = prayerTimes?.let {
                                    when (pName) {
                                        "Fajr" -> it.fajr
                                        "Dhuhr" -> it.dhuhr
                                        "Asr" -> it.asr
                                        "Maghrib" -> it.maghrib
                                        "Isha" -> it.isha
                                        else -> ""
                                    }
                                } ?: ""
                                
                                val isActive = (nextPrayerName == pName)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isActive) emeraldPrimary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.02f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            BorderStroke(
                                                width = 1.dp,
                                                color = if (isActive) emeraldPrimary else Color.White.copy(alpha = 0.06f)
                                            ),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = translatedNames[pName] ?: pName,
                                            color = if (isActive) goldAccent else Color.White.copy(alpha = 0.6f),
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = timeStr,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. DAILY VERSE CARD WITH EXTRA POLISH
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(24.dp))
                        .border(
                            BorderStroke(1.dp, Brush.horizontalGradient(listOf(emeraldPrimary.copy(alpha = 0.25f), Color.Transparent))),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Daily Verse", tint = goldAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Localization.translate("daily_verse", appLanguage),
                                fontWeight = FontWeight.Bold,
                                color = emeraldPrimary,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(goldAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "JUZ 1",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = goldAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = QuranDataset.dailyVerse.textArabic,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        lineHeight = 32.sp,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = QuranDataset.dailyVerse.textTranslation,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { onNavigateToFeature("Quran") }) {
                            Text(
                                text = Localization.translate("read_tafsir", appLanguage),
                                color = goldAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(
                            onClick = { viewModel.toggleAudioPlayback(QuranDataset.dailyVerse) },
                            modifier = Modifier
                                .background(emeraldPrimary, CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Verse",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 4. QUICK SERVICES GRID (FUTURISTIC FLOATING SYSTEM CARDS)
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = Localization.translate("quick_services", appLanguage).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = emeraldPrimary,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )

                val features = listOf(
                    FeatureItem("quran", "Quran", Icons.Default.Book, "Quran"),
                    FeatureItem("prayer_times", "Prayer Times", Icons.Default.Schedule, "Prayer"),
                    FeatureItem("qibla", "Qibla", Icons.Default.Navigation, "Qibla"),
                    FeatureItem("azkar", "Azkar", Icons.Default.MenuBook, "Azkar"),
                    FeatureItem("audio_quran", "Audio Quran", Icons.Default.Audiotrack, "Audio"),
                    FeatureItem("live_radio", "Live Radio", Icons.Default.Radio, "Radio"),
                    FeatureItem("khatmah", "Khatmah", Icons.Default.TrackChanges, "Khatmah"),
                    FeatureItem("quiz", "Quiz", Icons.Default.School, "Quiz"),
                    FeatureItem("duas", "Duas", Icons.Default.Favorite, "Duas"),
                    FeatureItem("tasbeeh", "Tasbeeh", Icons.Default.Lens, "Tasbeeh"),
                    FeatureItem("downloads", "Downloads", Icons.Default.Download, "Downloads"),
                    FeatureItem("settings", "Settings", Icons.Default.Settings, "Settings")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val halfSize = (features.size + 1) / 2
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        features.take(halfSize).forEach { feat ->
                            FeatureCard(feat, appLanguage, onNavigateToFeature)
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        features.drop(halfSize).forEach { feat ->
                            FeatureCard(feat, appLanguage, onNavigateToFeature)
                        }
                    }
                }
            }

            // 5. CONTINUE READING & LISTENING PANELS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Continue Reading
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToFeature("Quran") },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Bookmark, contentDescription = "Reading progress", tint = goldAccent, modifier = Modifier.size(20.dp))
                        Column {
                            Text(
                                text = Localization.translate("continue_reading", appLanguage),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Surah Al-Fatihah, Ayah 1",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                // Continue Listening
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToFeature("Audio") },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Audiotrack, contentDescription = "Audio Recitations", tint = emeraldPrimary, modifier = Modifier.size(20.dp))
                        Column {
                            Text(
                                text = Localization.translate("quran_listening", appLanguage),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Mishary Alafasy",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 6. DAILY HADITH INTERACTIVE WINDOW
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(20.dp))
                        .border(
                            BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MenuBook, contentDescription = "Hadith", tint = goldAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Localization.translate("daily_hadith", appLanguage).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = emeraldPrimary,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "\"${QuranDataset.dailyHadith}\"",
                        fontSize = 13.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        lineHeight = 19.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Smooth custom hover/press spring animation scale
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ) { onNavigate(item.route) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F8F6B).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.name,
                    tint = Color(0xFF0F8F6B),
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = Localization.translate(item.key, appLanguage),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.White
            )
        }
    }
}

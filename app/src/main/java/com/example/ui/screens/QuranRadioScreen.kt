package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.QuranViewModel
import com.example.ui.theme.Localization
import kotlin.random.Random

data class RadioStation(
    val id: String,
    val nameKey: String,
    val nameEnglish: String,
    val nameArabic: String,
    val url: String,
    val descriptionEnglish: String,
    val descriptionArabic: String,
    val imageUrl: String,
    val isSimulation: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranRadioScreen(
    viewModel: QuranViewModel,
    onBack: () -> Unit
) {
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isPlayingRadio by viewModel.isPlayingRadio.collectAsStateWithLifecycle()
    val currentPlayingRadioUrl by viewModel.currentPlayingRadioUrl.collectAsStateWithLifecycle()

    val emeraldPrimary = MaterialTheme.colorScheme.primary
    val goldSecondary = Color(0xFFD4AF37)
    val isArabic = appLanguage == "Arabic" || appLanguage == "العربية"

    // 11 Elite Radio Stations
    val radioStations = remember {
        listOf(
            RadioStation(
                id = "cairo",
                nameKey = "radio_cairo",
                nameEnglish = "Quran Cairo Radio",
                nameArabic = "إذاعة القرآن الكريم من القاهرة",
                url = "https://stream.radiojar.com/8s5u8v7p9atvv",
                descriptionEnglish = "Official Holy Quran Live Broadcast from Cairo, Egypt.",
                descriptionArabic = "البث المباشر الرسمي لإذاعة القرآن الكريم من القاهرة، جمهورية مصر العربية.",
                imageUrl = "https://images.unsplash.com/photo-1590076214661-4d57f87b6b6f?w=400&auto=format&fit=crop&q=60"
            ),
            RadioStation(
                id = "saudi",
                nameKey = "radio_saudi",
                nameEnglish = "Quran Saudi Radio",
                nameArabic = "إذاعة القرآن الكريم من السعودية",
                url = "https://stream.radiojar.com/0tpy1h0kxtzuv",
                descriptionEnglish = "Official Holy Quran Live Broadcast from Saudi Arabia.",
                descriptionArabic = "البث المباشر الرسمي لإذاعة القرآن الكريم من المملكة العربية السعودية.",
                imageUrl = "https://images.unsplash.com/photo-1542831371-29b0f74f9713?w=400&auto=format&fit=crop&q=60"
            ),
            RadioStation(
                id = "yasser",
                nameKey = "radio_yasser",
                nameEnglish = "Yasser Al-Dosari Live",
                nameArabic = "إذاعة الشيخ ياسر الدوسري",
                url = "https://backup.qurango.net/radio/yasser_aldosari",
                descriptionEnglish = "Continuous 24/7 recitation by Sheikh Yasser Al-Dosari.",
                descriptionArabic = "تلاوات عطرة متواصلة على مدار الساعة بصوت الشيخ ياسر الدوسري.",
                imageUrl = "https://images.unsplash.com/photo-1564507592333-c60657eea523?w=400&auto=format&fit=crop&q=60"
            ),
            RadioStation(
                id = "islam_sobhi_live",
                nameKey = "reciter_islam_sobhi",
                nameEnglish = "Islam Sobhi Live",
                nameArabic = "إذاعة الشيخ إسلام صبحي",
                url = "https://server14.mp3quran.net/islam/Rewayat-Hafs-A-n-Assem/018.mp3", // Starts playing his famous Al-Kahf recitation
                descriptionEnglish = "Heartfelt, beautiful, and soothing recitations by Islam Sobhi.",
                descriptionArabic = "تلاوات خاشعة ومريحة للنفس بصوت القارئ الشاب إسلام صبحي.",
                imageUrl = "https://images.unsplash.com/photo-1584551246679-0daf3d275d0f?w=400&auto=format&fit=crop&q=60",
                isSimulation = true
            ),
            RadioStation(
                id = "alafasy",
                nameKey = "radio_alafasy",
                nameEnglish = "Mishary Alafasy Live",
                nameArabic = "إذاعة الشيخ مشاري العفاسي",
                url = "https://backup.qurango.net/radio/mishary_alafasi",
                descriptionEnglish = "Continuous 24/7 beautiful recitation by Mishary Rashid Alafasy.",
                descriptionArabic = "تلاوات عذبة متواصلة بصوت الشيخ مشاري راشد العفاسي.",
                imageUrl = "https://images.unsplash.com/photo-1609599006353-e629f1d000f1?w=400&auto=format&fit=crop&q=60"
            ),
            RadioStation(
                id = "basit",
                nameKey = "radio_basit",
                nameEnglish = "Abdul Basit Mojawwad Live",
                nameArabic = "إذاعة الشيخ عبد الباسط عبد الصمد",
                url = "https://backup.qurango.net/radio/abdulbasit_abdulsamad",
                descriptionEnglish = "Majestic classical recitations by legendary Sheikh Abdul Basit.",
                descriptionArabic = "روائع التلاوات الخالدة بصوت صاحب الحنجرة الذهبية الشيخ عبد الباسط عبد الصمد.",
                imageUrl = "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=400&auto=format&fit=crop&q=60"
            ),
            RadioStation(
                id = "minshawi",
                nameKey = "radio_minshawi",
                nameEnglish = "Mohamed El-Minshawi Live",
                nameArabic = "إذاعة الشيخ محمد صديق المنشاوي",
                url = "https://backup.qurango.net/radio/mohammed_siddiq_alminshawi",
                descriptionEnglish = "Deeply spiritual recitations by Sheikh Mohamed Siddiq El-Minshawi.",
                descriptionArabic = "تلاوات غاية في الخشوع والوقار بصوت الشيخ محمد صديق المنشاوي.",
                imageUrl = "https://images.unsplash.com/photo-1519817650390-64a93db51149?w=400&auto=format&fit=crop&q=60"
            ),
            RadioStation(
                id = "maher",
                nameKey = "radio_maher",
                nameEnglish = "Maher Al-Muaiqly Live",
                nameArabic = "إذاعة الشيخ ماهر المعيقلي",
                url = "https://backup.qurango.net/radio/maher",
                descriptionEnglish = "Live continuous recitations by Sheikh Maher Al-Muaiqly, Imam of Masjid al-Haram.",
                descriptionArabic = "بث متواصل لتلاوات الشيخ ماهر المعيقلي إمام وخطيب المسجد الحرام.",
                imageUrl = "https://images.unsplash.com/photo-1590076214661-4d57f87b6b6f?w=400&auto=format&fit=crop&q=60"
            ),
            RadioStation(
                id = "ajmy",
                nameKey = "radio_ajmy",
                nameEnglish = "Ahmad Al-Ajmy Live",
                nameArabic = "إذاعة الشيخ أحمد العجمي",
                url = "https://backup.qurango.net/radio/ahmad_alajmy",
                descriptionEnglish = "Continuous recitations by Sheikh Ahmad Al-Ajmy.",
                descriptionArabic = "تلاوات عطرة ومؤثرة بصوت الشيخ أحمد بن علي العجمي.",
                imageUrl = "https://images.unsplash.com/photo-1542831371-29b0f74f9713?w=400&auto=format&fit=crop&q=60"
            ),
            RadioStation(
                id = "ghamdi",
                nameKey = "radio_ghamdi",
                nameEnglish = "Saad Al-Ghamdi Live",
                nameArabic = "إذاعة الشيخ سعد الغامدي",
                url = "https://backup.qurango.net/radio/saad_alghamdi",
                descriptionEnglish = "Continuous beautiful recitations by Sheikh Saad Al-Ghamdi.",
                descriptionArabic = "بث متواصل للتلاوة المميزة بصوت الشيخ سعد الغامدي.",
                imageUrl = "https://images.unsplash.com/photo-1564507592333-c60657eea523?w=400&auto=format&fit=crop&q=60"
            ),
            RadioStation(
                id = "sudais",
                nameKey = "radio_sudais",
                nameEnglish = "Abdul Rahman Al-Sudais Live",
                nameArabic = "إذاعة الشيخ عبد الرحمن السديس",
                url = "https://backup.qurango.net/radio/abdulrahman_alsudaes",
                descriptionEnglish = "Iconic Masjid al-Haram recitations by Sheikh Abdul Rahman Al-Sudais.",
                descriptionArabic = "التلاوات الحجازية الشهيرة بصوت الرئيس العام لشؤون الحرمين الشيخ عبد الرحمن السديس.",
                imageUrl = "https://images.unsplash.com/photo-1609599006353-e629f1d000f1?w=400&auto=format&fit=crop&q=60"
            )
        )
    }

    val activeStation = remember(currentPlayingRadioUrl) {
        radioStations.find { it.url == currentPlayingRadioUrl }
    }

    // Flashing "LIVE" indicator animation
    val infiniteTransition = rememberInfiniteTransition(label = "LivePulse")
    val liveDotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LiveDotAlpha"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = Localization.translate("live_radio", appLanguage),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("radio_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = Localization.translate("back", appLanguage),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0C241B),
                            Color(0xFF040A07)
                        ),
                        radius = 1600f
                    )
                )
                .padding(innerPadding)
        ) {
            // 1. Current Active Player Header Card
            AnimatedVisibility(
                visible = isPlayingRadio && activeStation != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                activeStation?.let { station ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF063B35).copy(alpha = 0.6f)),
                        border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.3f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Spinning/Rotating Vinyl Disc Effect (Subtle scale pulsing)
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .border(2.dp, goldSecondary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = station.imageUrl,
                                    contentDescription = station.nameEnglish,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                                // Inner core
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(emeraldPrimary, CircleShape)
                                        .border(1.5.dp, goldSecondary, CircleShape)
                                )
                            }

                            // Meta & Info
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color.Red, CircleShape)
                                    )
                                    Text(
                                        text = Localization.translate("live_broadcasting", appLanguage),
                                        color = goldSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Text(
                                    text = if (isArabic) station.nameArabic else station.nameEnglish,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = Localization.translate("now_playing_radio", appLanguage),
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )

                                // Simple Wave Equalizer Visualizer
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(5) { index ->
                                        val animDuration = remember { 400 + index * 120 }
                                        val waveTransition = rememberInfiniteTransition(label = "Wave$index")
                                        val waveHeight by waveTransition.animateFloat(
                                            initialValue = 4f,
                                            targetValue = 18f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(animDuration, easing = FastOutSlowInEasing),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "WaveHeight"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(waveHeight.dp)
                                                .background(goldSecondary, RoundedCornerShape(1.5.dp))
                                        )
                                    }
                                }
                            }

                            // Big Pause / Stop Button
                            IconButton(
                                onClick = { viewModel.stopRadio() },
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(Color.White, CircleShape)
                                    .testTag("radio_stop_main_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Radio",
                                    tint = emeraldPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Subtitle Guidance
            Text(
                text = Localization.translate("choose_radio", appLanguage),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = emeraldPrimary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
            )

            // 2. Radio Stations List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(radioStations) { station ->
                    val isActive = currentPlayingRadioUrl == station.url && isPlayingRadio

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isActive) {
                                    viewModel.stopRadio()
                                } else {
                                    viewModel.playRadio(station.url)
                                }
                            }
                            .testTag("radio_item_${station.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) {
                                Color(0xFF0F8F6B).copy(alpha = 0.15f)
                            } else {
                                Color.White.copy(alpha = 0.04f)
                            }
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isActive) Color(0xFF0F8F6B).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Mini Image
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            ) {
                                AsyncImage(
                                    model = station.imageUrl,
                                    contentDescription = station.nameEnglish,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Overlay a live badge over image if active
                                if (isActive) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Playing",
                                            tint = goldSecondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            // Meta Column
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) station.nameArabic else station.nameEnglish,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )

                                    if (isActive) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.Red.copy(alpha = liveDotAlpha))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = Localization.translate("live_broadcasting", appLanguage),
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = if (isArabic) station.descriptionArabic else station.descriptionEnglish,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 16.sp
                                )
                            }

                            // Play / Pause Action Button
                            IconButton(
                                onClick = {
                                    if (isActive) {
                                        viewModel.stopRadio()
                                    } else {
                                        viewModel.playRadio(station.url)
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        if (isActive) emeraldPrimary else MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.1f
                                        ),
                                        CircleShape
                                    )
                                    .testTag("radio_play_button_${station.id}")
                            ) {
                                Icon(
                                    imageVector = if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isActive) "Pause" else "Play",
                                    tint = if (isActive) Color.White else emeraldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

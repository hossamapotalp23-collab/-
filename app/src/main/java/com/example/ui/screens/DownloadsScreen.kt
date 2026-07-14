package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.prayer.PrayerCalculator
import com.example.data.quran.QuranDataset
import com.example.data.quran.Reciter
import com.example.ui.QuranViewModel
import com.example.ui.theme.Localization
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: QuranViewModel,
    onBack: () -> Unit
) {
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val downloadStates by viewModel.downloadStates.collectAsStateWithLifecycle()
    val isPlayingAudio by viewModel.isPlayingAudio.collectAsStateWithLifecycle()
    val isPlayingAdhan by viewModel.isPlayingAdhan.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 for Quran, 1 for Adhan

    // Theme Colors
    val emeraldPrimary = Color(0xFF0F8F6B)
    val goldAccent = Color(0xFFD4AF37)

    // Compute downloaded Quran items
    val downloadedQuranItems = remember(downloadStates) {
        val list = mutableListOf<QuranDownloadItem>()
        QuranDataset.reciters.forEach { reciter ->
            QuranDataset.allSurahHeaders.forEach { surah ->
                val key = "${reciter.id}_${surah.id}"
                if (downloadStates[key] == QuranViewModel.DownloadState.Completed) {
                    list.add(QuranDownloadItem(reciter, surah))
                }
            }
        }
        list
    }

    // Compute downloaded Adhan items
    val downloadedAdhanItems = remember(downloadStates) {
        val list = mutableListOf<PrayerCalculator.AdhanMuezzin>()
        PrayerCalculator.adhanMuezzins.forEach { muezzin ->
            val key = "adhan_${muezzin.id}"
            if (downloadStates[key] == QuranViewModel.DownloadState.Completed) {
                list.add(muezzin)
            }
        }
        list
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0D2D22),
                        Color(0xFF040B08),
                        Color(0xFF020403)
                    ),
                    radius = 1800f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top App Bar
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = Localization.translate("all_downloads", appLanguage),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("downloads_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            // Tabs / Segmented Control
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.Transparent,
                contentColor = emeraldPrimary,
                indicator = { tabPositions ->
                    if (activeTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = goldAccent
                        )
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Text(
                            text = Localization.translate("quran_downloads", appLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    },
                    selectedContentColor = goldAccent,
                    unselectedContentColor = Color.LightGray
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Text(
                            text = Localization.translate("adhan_downloads", appLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    },
                    selectedContentColor = goldAccent,
                    unselectedContentColor = Color.LightGray
                )
            }

            // Download List View
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (activeTab == 0) {
                    // Quran Audio Downloads
                    if (downloadedQuranItems.isEmpty()) {
                        EmptyStateView(
                            message = Localization.translate("no_downloads", appLanguage)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(downloadedQuranItems) { item ->
                                QuranDownloadCard(
                                    item = item,
                                    appLanguage = appLanguage,
                                    isPlaying = isPlayingAudio && viewModel.selectedSurahId.value == item.surah.id && viewModel.selectedReciter.value.id == item.reciter.id,
                                    onPlayToggle = {
                                        if (viewModel.selectedReciter.value.id != item.reciter.id) {
                                            viewModel.selectReciter(item.reciter)
                                        }
                                        viewModel.loadSurahContent(item.surah.id)
                                        viewModel.toggleAudioPlayback()
                                    },
                                    onDelete = {
                                        viewModel.deleteDownloadedSurah(item.reciter, item.surah.id)
                                    },
                                    goldAccent = goldAccent
                                )
                            }
                        }
                    }
                } else {
                    // Adhan Downloads
                    if (downloadedAdhanItems.isEmpty()) {
                        EmptyStateView(
                            message = Localization.translate("no_downloads", appLanguage)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(downloadedAdhanItems) { muezzin ->
                                AdhanDownloadCard(
                                    muezzin = muezzin,
                                    appLanguage = appLanguage,
                                    isPlaying = isPlayingAdhan && viewModel.selectedAdhanMuezzin.value.id == muezzin.id,
                                    onPlayToggle = {
                                        if (isPlayingAdhan && viewModel.selectedAdhanMuezzin.value.id == muezzin.id) {
                                            viewModel.stopAdhan()
                                        } else {
                                            viewModel.playAdhan(muezzin)
                                        }
                                    },
                                    onDelete = {
                                        viewModel.deleteDownloadedAdhan(muezzin)
                                    },
                                    goldAccent = goldAccent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class QuranDownloadItem(
    val reciter: Reciter,
    val surah: com.example.data.quran.SurahHeader
)

@Composable
fun QuranDownloadCard(
    item: QuranDownloadItem,
    appLanguage: String,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    onDelete: () -> Unit,
    goldAccent: Color
) {
    val surahName = if (appLanguage == "Arabic" || appLanguage == "العربية") item.surah.arabicName else item.surah.name
    val reciterName = if (appLanguage == "Arabic" || appLanguage == "العربية") {
        when (item.reciter.id) {
            "mishary" -> "مشاري العفاسي"
            "abdul_basit" -> "عبد الباسط عبد الصمد"
            "minshawi" -> "محمد صديق المنشاوي"
            "shuraim" -> "سعود الشريم"
            "sudais" -> "عبد الرحمن السديس"
            "yasser_al_dosari" -> "ياسر الدوسري"
            "islam_sobhi" -> "إسلام صبحي"
            else -> item.reciter.name
        }
    } else {
        item.reciter.name
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F1A15)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E352B), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = surahName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = reciterName,
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Play Button
                IconButton(
                    onClick = onPlayToggle,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isPlaying) goldAccent else Color(0xFF1B3B2B)
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Play/Stop",
                        tint = if (isPlaying) Color.Black else goldAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0x33FF5252)
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AdhanDownloadCard(
    muezzin: PrayerCalculator.AdhanMuezzin,
    appLanguage: String,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    onDelete: () -> Unit,
    goldAccent: Color
) {
    val muezzinName = if (appLanguage == "Arabic" || appLanguage == "العربية") muezzin.nameAr else muezzin.nameEn

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F1A15)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E352B), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = muezzinName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "أذان بصيغة MP3 محمل أوفلاين",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Play Button
                IconButton(
                    onClick = onPlayToggle,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isPlaying) goldAccent else Color(0xFF1B3B2B)
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Play/Stop",
                        tint = if (isPlaying) Color.Black else goldAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0x33FF5252)
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = "No downloads",
            tint = Color(0xFF1B3B2B),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = Color.LightGray,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

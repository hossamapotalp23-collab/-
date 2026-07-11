package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.quran.QuranDataset
import com.example.data.quran.Reciter
import com.example.ui.QuranViewModel
import com.example.ui.theme.Localization

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioQuranScreen(
    viewModel: QuranViewModel,
    onBack: () -> Unit
) {
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val selectedReciter by viewModel.selectedReciter.collectAsStateWithLifecycle()
    val isPlayingAudio by viewModel.isPlayingAudio.collectAsStateWithLifecycle()
    val selectedSurahId by viewModel.selectedSurahId.collectAsStateWithLifecycle()
    val audioPlaybackSpeed by viewModel.audioPlaybackSpeed.collectAsStateWithLifecycle()
    val sleepTimerSeconds by viewModel.sleepTimerSeconds.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val goldSecondary = Color(0xFFD4AF37)
    val emeraldPrimary = MaterialTheme.colorScheme.primary

    // Selected Surah Header
    val selectedSurahHeader = remember(selectedSurahId) {
        QuranDataset.allSurahHeaders.firstOrNull { it.id == selectedSurahId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Localization.translate("audio_quran", appLanguage), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Localization.translate("back", appLanguage), tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Reciter Audio Controller Panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reciter Photo
                        AsyncImage(
                            model = selectedReciter.imageUrl,
                            contentDescription = selectedReciter.name,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.5.dp, goldSecondary, RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )

                        // Currently Playing Info
                        Column(modifier = Modifier.weight(1.0f)) {
                            val localizedReciterName = Localization.translate("reciter_${selectedReciter.id}", appLanguage)
                            Text(
                                text = localizedReciterName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = selectedSurahHeader?.let {
                                    val surahLabel = if (appLanguage == "Arabic" || appLanguage == "العربية") "سورة" else "Surah"
                                    "$surahLabel ${it.name} (${it.arabicName})"
                                } ?: "",
                                fontSize = 13.sp,
                                color = goldSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Player Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Prev Surah
                        IconButton(onClick = {
                            if (selectedSurahId > 1) {
                                viewModel.loadSurahContent(selectedSurahId - 1)
                                if (isPlayingAudio) {
                                    // re-trigger with new surah
                                    viewModel.toggleAudioPlayback()
                                    viewModel.toggleAudioPlayback()
                                }
                            }
                        }) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Surah", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                        }

                        // Play / Pause FAB style
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable { viewModel.toggleAudioPlayback() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Next Surah
                        IconButton(onClick = {
                            if (selectedSurahId < 114) {
                                viewModel.loadSurahContent(selectedSurahId + 1)
                                if (isPlayingAudio) {
                                    viewModel.toggleAudioPlayback()
                                    viewModel.toggleAudioPlayback()
                                }
                            }
                        }) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next Surah", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // 2. Horizontal Reciter Selection List
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = Localization.translate("select_reciter", appLanguage),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(QuranDataset.reciters) { reciter ->
                        val isSelected = selectedReciter.id == reciter.id
                        val localizedReciterName = Localization.translate("reciter_${reciter.id}", appLanguage)

                        Card(
                            modifier = Modifier
                                .width(120.dp)
                                .clickable { viewModel.selectReciter(reciter) }
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) goldSecondary else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AsyncImage(
                                    model = reciter.imageUrl,
                                    contentDescription = reciter.name,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = localizedReciterName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Vertical Surah Selection List
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (appLanguage == "Arabic" || appLanguage == "العربية") "اختر السورة" else "Select Surah",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(QuranDataset.allSurahHeaders) { surah ->
                        val isCurrentSurah = selectedSurahId == surah.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.loadSurahContent(surah.id)
                                    viewModel.toggleAudioPlayback()
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentSurah) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Surah index badge
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                if (isCurrentSurah) goldSecondary else MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${surah.id}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = surah.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isCurrentSurah) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${surah.translation} • ${surah.versesCount} verses",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = surah.arabicName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (isCurrentSurah) goldSecondary else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

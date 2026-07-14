package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.quran.Ayah
import com.example.data.quran.QuranDataset
import com.example.ui.QuranViewModel
import com.example.ui.theme.Localization

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranScreen(
    viewModel: QuranViewModel,
    onBack: () -> Unit
) {
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    var activeTab by remember { mutableStateOf("Surahs") }
    val searchQueries by viewModel.quranSearchQuery.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val selectedSurah by viewModel.selectedSurah.collectAsStateWithLifecycle()
    val isPlayingAudio by viewModel.isPlayingAudio.collectAsStateWithLifecycle()
    val currentPlayingAyah by viewModel.currentPlayingAyah.collectAsStateWithLifecycle()

    var isReaderActive by remember { mutableStateOf(false) }

    val context = LocalContext.current

    if (isReaderActive && selectedSurah != null) {
        // Quran Reader View (Verses reading)
        SurahReaderView(
            viewModel = viewModel,
            onBack = { isReaderActive = false }
        )
    } else {
        // List Selection View
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(Localization.translate("holy_quran", appLanguage), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
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
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0C241B),
                                Color(0xFF040A07)
                            ),
                            radius = 1600f
                        )
                    )
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQueries,
                    onValueChange = { viewModel.searchQuran(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(Localization.translate("search_surah", appLanguage)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary) },
                    shape = RoundedCornerShape(12.dp)
                )

                // Sub-Tabs Selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Surahs", "Bookmarks").forEach { tab ->
                        val translatedTab = if (tab == "Surahs") {
                            Localization.translate("surahs", appLanguage)
                        } else {
                            Localization.translate("bookmarks", appLanguage)
                        }
                        Button(
                            onClick = { activeTab = tab },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeTab == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = if (activeTab == tab) Color.White else MaterialTheme.colorScheme.onSurface
                             ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(translatedTab, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Render matching tab
                when (activeTab) {
                    "Surahs" -> {
                        val filteredSurahs = QuranDataset.allSurahHeaders.filter {
                            it.name.contains(searchQueries, ignoreCase = true) ||
                                    it.translation.contains(searchQueries, ignoreCase = true) ||
                                    it.arabicName.contains(searchQueries, ignoreCase = true)
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredSurahs) { surah ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.loadSurahContent(surah.id)
                                            isReaderActive = true
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            // Circular Index Badge
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${surah.id}",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                            }

                                            Column {
                                                Text(
                                                    text = surah.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    text = "${surah.translation} • ${surah.versesCount} verses",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = surah.arabicName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.AccessTime, contentDescription = "Duration", modifier = Modifier.size(10.dp), tint = Color.Gray)
                                                Text(
                                                    text = "${surah.durationMinutes}m",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = surah.type,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFD4AF37)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "Bookmarks" -> {
                        if (bookmarks.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.BookmarkBorder, contentDescription = "No bookmarks", modifier = Modifier.size(48.dp), tint = Color.Gray)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(Localization.translate("no_bookmarks", appLanguage), color = Color.Gray)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(bookmarks) { b ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.loadSurahContent(b.surahNumber)
                                                isReaderActive = true
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val surahLabel = if (appLanguage == "Arabic" || appLanguage == "العربية") "سورة" else "Surah"
                                                val ayahLabel = if (appLanguage == "Arabic" || appLanguage == "العربية") "آية" else "Ayah"
                                                Text(
                                                    "$surahLabel ${b.surahName} [$ayahLabel ${b.ayahNumber}]",
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontSize = 14.sp
                                                )
                                                IconButton(onClick = { viewModel.toggleBookmark(b.surahNumber, b.ayahNumber) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Remove bookmark", tint = Color.Red, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = b.arabicText,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(b.translationText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahReaderView(
    viewModel: QuranViewModel,
    onBack: () -> Unit
) {
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val selectedSurah by viewModel.selectedSurah.collectAsStateWithLifecycle()
    val quranFontSize by viewModel.quranFontSize.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val isPlayingAudio by viewModel.isPlayingAudio.collectAsStateWithLifecycle()
    val currentPlayingAyah by viewModel.currentPlayingAyah.collectAsStateWithLifecycle()
    val selectedReciter by viewModel.selectedReciter.collectAsStateWithLifecycle()
    val isLoadingRealSurah by viewModel.isLoadingRealSurah.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showReciterDialog by remember { mutableStateOf(false) }

    if (showReciterDialog) {
        AlertDialog(
            onDismissRequest = { showReciterDialog = false },
            title = { Text(Localization.translate("select_reciter", appLanguage), fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                ) {
                    items(QuranDataset.reciters) { reciter ->
                        val isSelected = selectedReciter.id == reciter.id
                        val localizedReciterName = Localization.translate("reciter_${reciter.id}", appLanguage)
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectReciter(reciter)
                                    showReciterDialog = false
                                }
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = reciter.name, tint = MaterialTheme.colorScheme.primary)
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = localizedReciterName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = reciter.description,
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    maxLines = 1
                                )
                            }
                            
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color(0xFFD4AF37))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReciterDialog = false }) {
                    Text(if (appLanguage == "Arabic" || appLanguage == "العربية") "إغلاق" else "Close")
                }
            }
        )
    }

    if (selectedSurah != null) {
        val surah = selectedSurah!!

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(surah.header.name, fontWeight = FontWeight.Bold)
                            Text(surah.header.translation, fontSize = 11.sp, color = Color(0xFFD4AF37))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Localization.translate("back", appLanguage), tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showReciterDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = Localization.translate("change_reciter", appLanguage),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { viewModel.toggleAudioPlayback() }) {
                            Icon(
                                imageVector = if (isPlayingAudio) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = Localization.translate("play_audio", appLanguage),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0D251C),
                                Color(0xFF040A07)
                            ),
                            radius = 1600f
                        )
                    )
            ) {
                if (isLoadingRealSurah) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        color = Color(0xFFD4AF37),
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Surah Header details (Regal Bismillah frame)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF063B35).copy(alpha = 0.6f)),
                            border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    surah.header.arabicName,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD4AF37)
                                )
                                Text(
                                    "${surah.header.type} • ${surah.header.versesCount} Verses",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 13.sp
                                )
                                Divider(color = Color(0xFFD4AF37), thickness = 1.dp, modifier = Modifier.width(120.dp))
                                Text(
                                    "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Verses List
                    items(surah.verses) { ayah ->
                        val isBookmarked = bookmarks.any { it.surahNumber == surah.header.id && it.ayahNumber == ayah.number }
                        val isCurrentlyPlaying = currentPlayingAyah?.number == ayah.number && isPlayingAudio

                        // Verse Card with word-by-word expander
                        var showWordMeanings by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentlyPlaying) {
                                    Color(0xFF0F8F6B).copy(alpha = 0.15f)
                                } else {
                                    Color.White.copy(alpha = 0.04f)
                                }
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isCurrentlyPlaying) Color(0xFF0F8F6B).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .clickable { showWordMeanings = !showWordMeanings }
                            ) {
                                // Verse Meta row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color(0xFFD4AF37), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${ayah.number}",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Verse Actions
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = { viewModel.toggleBookmark(surah.header.id, ayah.number) }) {
                                            Icon(
                                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                contentDescription = "Bookmark",
                                                tint = Color(0xFFD4AF37)
                                            )
                                        }
                                        IconButton(onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Quran Verse", "${ayah.textArabic}\n\n${ayah.textTranslation}")
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, Localization.translate("copied", appLanguage), Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = {
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, "${ayah.textArabic}\n\n${ayah.textTranslation} (Surah ${surah.header.name}:${ayah.number})")
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Share Verse"))
                                        }) {
                                            Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Arabic Text
                                Text(
                                    text = ayah.textArabic,
                                    fontSize = quranFontSize.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Right,
                                    lineHeight = (quranFontSize * 1.5).sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // English Translation
                                Text(
                                    text = ayah.textTranslation,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Word-by-word expandable drawer
                                if (showWordMeanings && ayah.wordMeanings.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(Localization.translate("word_meanings", appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD4AF37))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ayah.wordMeanings.forEach { word ->
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.padding(2.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(word.word, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text(word.meaning, fontSize = 10.sp, color = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Interactive Tafsir toggle summary
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Book, contentDescription = "Tafsir", tint = Color(0xFFD4AF37), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(Localization.translate("tafsir_title", appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(ayah.tafsir, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                // Audio Floating Reciter Tray (glassmorphism look)
                if (isPlayingAudio) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${Localization.translate("reciting", appLanguage)}: ${selectedReciter.name}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                if (currentPlayingAyah != null) {
                                    val ayahLabel = if (appLanguage == "Arabic" || appLanguage == "العربية") "آية" else "Ayah"
                                    Text("$ayahLabel ${currentPlayingAyah!!.number}", fontSize = 11.sp, color = Color(0xFFD4AF37))
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.toggleAudioPlayback() }) {
                                    Icon(Icons.Default.Pause, contentDescription = "Pause", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onBack() }) {
                                    Icon(Icons.Default.Close, contentDescription = Localization.translate("stop", appLanguage), tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

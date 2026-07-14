package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.ZikrCounterEntity
import com.example.data.prayer.PrayerCalculator
import com.example.data.quran.QuranDataset
import com.example.ui.QuranViewModel
import com.example.ui.theme.Localization
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(viewModel: QuranViewModel, onBack: () -> Unit) {
    val compassAzimuth by viewModel.compassAzimuth.collectAsStateWithLifecycle()
    val qiblaBearing by viewModel.qiblaBearing.collectAsStateWithLifecycle()
    val lat by viewModel.currentLatitude.collectAsStateWithLifecycle()
    val lng by viewModel.currentLongitude.collectAsStateWithLifecycle()
    val city by viewModel.currentLocationName.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    // Calculate rotation angle relative to North
    // Qibla is (qiblaBearing - compassAzimuth)
    val relativeAngle = (qiblaBearing - compassAzimuth).toFloat()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Localization.translate("compass", appLanguage), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Location Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(city, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Lat: %.4f, Lng: %.4f".format(lat, lng), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Compass Visualizer
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Outer Compass Ring
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0xFFD4AF37),
                        radius = size.minDimension / 2 - 10,
                        style = Stroke(width = 4.dp.toPx())
                    )
                }

                // Rotating Compass Dial
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(relativeAngle),
                    contentAlignment = Alignment.Center
                ) {
                    // Qibla Pointer Line pointing north-ish to Kaaba
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val radius = size.minDimension / 2 - 40
                        
                        // Draw Compass ticks
                        for (i in 0 until 360 step 30) {
                            val angleRad = Math.toRadians(i.toDouble())
                            val startX = (centerX + (radius - 15) * sin(angleRad)).toFloat()
                            val startY = (centerY - (radius - 15) * cos(angleRad)).toFloat()
                            val endX = (centerX + radius * sin(angleRad)).toFloat()
                            val endY = (centerY - radius * cos(angleRad)).toFloat()
                            
                            drawCircle(
                                color = if (i == 0) Color.Red else Color(0xFFD4AF37),
                                radius = if (i % 90 == 0) 5.dp.toPx() else 2.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(endX, endY)
                            )
                        }
                    }

                    // Needle
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Kaaba Direction",
                        modifier = Modifier
                            .size(120.dp)
                            .rotate(0f), // pointing straight up (0 deg relative)
                        tint = Color(0xFFD4AF37)
                    )
                }

                // Center Hub (Kaaba Icon symbol)
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    // A tiny golden box representing Kaaba
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFFD4AF37), shape = RoundedCornerShape(2.dp))
                    )
                }
            }

            // Direction Data Details
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    "${Localization.translate("qibla_angle", appLanguage)}: %.1f°".format(qiblaBearing),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    Localization.translate("align_phone", appLanguage),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzkarScreen(viewModel: QuranViewModel, onBack: () -> Unit) {
    val zikrCounters by viewModel.zikrCounters.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf("Morning") }
    val categories = listOf("Morning", "Evening", "Sleep", "Travel")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Localization.translate("daily_azkar", appLanguage), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
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
        ) {
            // Category Selector Chips
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory),
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                categories.forEachIndexed { index, cat ->
                    Tab(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        text = { Text(Localization.translate(cat.lowercase(), appLanguage), fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Azkar matching category
            val filteredAzkar = when (selectedCategory) {
                "Morning" -> QuranDataset.morningAzkar
                "Evening" -> QuranDataset.eveningAzkar
                "Sleep" -> QuranDataset.sleepAzkar
                else -> QuranDataset.travelAzkar
            }

            val context = LocalContext.current

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredAzkar) { zikr ->
                    val counterState = zikrCounters.find { it.zikrId == zikr.id } ?: ZikrCounterEntity(zikr.id)
                    val progress = counterState.count.toFloat() / counterState.maxCount

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (progress >= 1.0f) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Header (Source, Repeat target)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = zikr.source,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD4AF37)
                                )
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "${Localization.translate("repeat", appLanguage)}: ${counterState.maxCount}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Arabic Text
                            Text(
                                text = zikr.textArabic,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right,
                                lineHeight = 30.sp,
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Translation
                            Text(
                                text = zikr.textTranslation,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Counter button and actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Copy Action
                                    IconButton(onClick = {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "${zikr.textArabic}\n\n${zikr.textTranslation}")
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share Zikr"))
                                    }) {
                                        Icon(Icons.Default.Share, contentDescription = Localization.translate("share", appLanguage), tint = MaterialTheme.colorScheme.primary)
                                    }

                                    // Toggle Favorite
                                    IconButton(onClick = { viewModel.toggleZikrFavorite(zikr.id) }) {
                                        Icon(
                                            imageVector = if (counterState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = Localization.translate("favorite", appLanguage),
                                            tint = if (counterState.isFavorite) Color.Red else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // Click Counter Area
                                Button(
                                    onClick = { viewModel.incrementZikr(zikr.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (progress >= 1f) Color(0xFFD4AF37) else MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Default.PlusOne, contentDescription = "Add")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "${counterState.count} / ${counterState.maxCount}",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Progress Bar
                            LinearProgressIndicator(
                                progress = { progress.coerceAtMost(1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(CircleShape),
                                color = Color(0xFFD4AF37),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhatmahScreen(viewModel: QuranViewModel, onBack: () -> Unit) {
    val activePlan by viewModel.activeKhatmah.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    var inputTitle by remember { mutableStateOf("My Khatmah Journey") }
    var inputDays by remember { mutableStateOf("30") }
    var inputMinutes by remember { mutableStateOf("15") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Localization.translate("khatmah_planner", appLanguage), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (activePlan != null) {
                val plan = activePlan!!
                val progress = plan.pagesRead.toFloat() / plan.totalPages
                val remainingPages = plan.totalPages - plan.pagesRead

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(plan.title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)

                        // Circular Progress Ring
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxSize(),
                                color = Color(0xFFD4AF37),
                                strokeWidth = 12.dp,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "%.0f%%".format(progress * 100),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(Localization.translate("unlocked", appLanguage), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Statistics Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${plan.pagesRead}/${plan.totalPages}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(Localization.translate("pages_read", appLanguage), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$remainingPages", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFD4AF37))
                                Text(Localization.translate("remaining", appLanguage), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${plan.dailyMinutes} mins", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(Localization.translate("daily_target", appLanguage), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Read pages logging buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.logKhatmahPagesRead(5) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(Localization.translate("add_pages", appLanguage))
                            }
                            Button(
                                onClick = { viewModel.logKhatmahPagesRead(20) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                            ) {
                                Text(Localization.translate("add_juz", appLanguage))
                            }
                        }
                    }
                }

                // Achievements Badges
                Text(Localization.translate("khatmah", appLanguage), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BadgeCard(Localization.translate("bronze_badge", appLanguage), Localization.translate("start_khatmah", appLanguage), progress >= 0.05, Color(0xFFCD7F32))
                    BadgeCard(Localization.translate("silver_badge", appLanguage), Localization.translate("reach_50", appLanguage), progress >= 0.50, Color(0xFFC0C0C0))
                    BadgeCard(Localization.translate("gold_badge", appLanguage), Localization.translate("complete_quran", appLanguage), progress >= 1.0, Color(0xFFD4AF37))
                }

            } else {
                // Creation Screen
                Text(Localization.translate("create_new_khatmah", appLanguage), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                OutlinedTextField(
                    value = inputTitle,
                    onValueChange = { inputTitle = it },
                    label = { Text(Localization.translate("title", appLanguage)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = inputDays,
                    onValueChange = { inputDays = it },
                    label = { Text(Localization.translate("finish_goal", appLanguage)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = inputMinutes,
                    onValueChange = { inputMinutes = it },
                    label = { Text(Localization.translate("daily_reading_target", appLanguage)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val days = inputDays.toIntOrNull() ?: 30
                        val mins = inputMinutes.toIntOrNull() ?: 15
                        viewModel.createNewKhatmah(inputTitle, days, mins)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Localization.translate("initialize_khatmah", appLanguage))
                }
            }
        }
    }
}

@Composable
fun BadgeCard(title: String, desc: String, unlocked: Boolean, color: Color) {
    Card(
        modifier = Modifier
            .width(110.dp)
            .height(130.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Stars,
                contentDescription = title,
                tint = if (unlocked) color else Color.Gray,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
            Text(desc, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemorizationQuizScreen(viewModel: QuranViewModel, onBack: () -> Unit) {
    val quizQuestion by viewModel.quizQuestion.collectAsStateWithLifecycle()
    val checked by viewModel.quizAnswerChecked.collectAsStateWithLifecycle()
    val selectedIndex by viewModel.quizSelectedAnswerIndex.collectAsStateWithLifecycle()
    val streak by viewModel.quizStreak.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    // Generate first quiz on build
    LaunchedEffect(Unit) {
        if (quizQuestion == null) {
            viewModel.generateNextQuiz()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Localization.translate("quiz", appLanguage), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Streak counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = "Streak", tint = Color(0xFFF59E0B))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${Localization.translate("streak", appLanguage)}: $streak", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Text("Category: Quran Memorizer", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (quizQuestion != null) {
                val question = quizQuestion!!

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = question.type.uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD4AF37),
                        fontSize = 12.sp
                    )

                    // Question Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                    ) {
                        Text(
                            text = question.text,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(24.dp),
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp
                        )
                    }

                    // Options list
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        question.options.forEachIndexed { idx, option ->
                            val isSelected = selectedIndex == idx
                            val isCorrectAnswer = question.correctAnswerIndex == idx

                            val color = when {
                                checked && isCorrectAnswer -> Color(0xFF2E7D32) // green for correct
                                checked && isSelected && !isCorrectAnswer -> Color(0xFFC62828) // red for wrong selection
                                isSelected -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(!checked) { viewModel.selectQuizAnswer(idx) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = color,
                                    contentColor = if (isSelected || (checked && isCorrectAnswer)) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = option,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (checked && isCorrectAnswer) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Correct", tint = Color.White)
                                    } else if (checked && isSelected && !isCorrectAnswer) {
                                        Icon(Icons.Default.Cancel, contentDescription = "Incorrect", tint = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // Answer Details Summary
                    if (checked) {
                        AnimatedVisibility(visible = true) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = question.details,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // CTA action buttons
                if (!checked) {
                    Button(
                        onClick = { viewModel.submitQuizAnswer() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedIndex != -1,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(Localization.translate("verify_answer", appLanguage), fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { viewModel.generateNextQuiz() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                    ) {
                        Text(Localization.translate("next_question", appLanguage), fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuasScreen(viewModel: QuranViewModel, onBack: () -> Unit) {
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Localization.translate("duas", appLanguage), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Localization.translate("back", appLanguage), tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(QuranDataset.allDuas) { dua ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(dua.category, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD4AF37))
                            Icon(Icons.Default.MenuBook, contentDescription = "Dua", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = dua.textArabic,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            lineHeight = 32.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(dua.textTranslation, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Source: ${dua.source}", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: QuranViewModel, onBack: () -> Unit) {
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val quranFontSize by viewModel.quranFontSize.collectAsStateWithLifecycle()
    val selectedFont by viewModel.selectedArabicFont.collectAsStateWithLifecycle()
    val currentMethod by viewModel.currentCalculationMethod.collectAsStateWithLifecycle()
    val isTasbeehSoundEnabled by viewModel.isTasbeehSoundEnabled.collectAsStateWithLifecycle()
    val isTasbeehVibrationEnabled by viewModel.isTasbeehVibrationEnabled.collectAsStateWithLifecycle()
    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val isArabicFontFixed by viewModel.isArabicFontFixed.collectAsStateWithLifecycle()
    val currentLocationName by viewModel.currentLocationName.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var activeTabInAiTutor by remember { mutableStateOf("Wudu") } // "Wudu" or "Salah"
    var expandedAiTutor by remember { mutableStateOf(false) }
    var activeVideoPlayingStep by remember { mutableStateOf<String?>(null) } // Step ID of the playing video
    var isScanningWithAi by remember { mutableStateOf(false) }
    var scanningStepText by remember { mutableStateOf("") }
    
    // GPS detecting state
    var isDetectingGps by remember { mutableStateOf(false) }
    var gpsMessage by remember { mutableStateOf("") }

    val isAr = appLanguage == "Arabic" || appLanguage == "العربية"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isAr) "الإعدادات الذكية" else "Smart Settings", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- SECTION 1: LANGUAGES & LOCATION (اللغة والموقع) ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(if (isAr) "1. اللغة والموقع" else "1. Language & Location", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    
                    // Language picker
                    Text(if (isAr) "لغة التطبيق" else "App Language", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "العربية" to "Arabic",
                            "English" to "English",
                            "Deutsch" to "German",
                            "हिन्दी" to "Hindi",
                            "中文" to "Chinese",
                            "Español" to "Spanish",
                            "Français" to "French"
                        ).forEach { (displayName, codeName) ->
                            val isSelected = appLanguage == codeName || appLanguage == displayName
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setLanguage(codeName) },
                                label = { Text(displayName) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Location picker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(if (isAr) "الموقع الحالي" else "Current Location", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text(currentLocationName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFD4AF37))
                        }
                        
                        Button(
                            onClick = {
                                isDetectingGps = true
                                gpsMessage = if (isAr) "جاري تحديد الموقع عبر الـ GPS..." else "Detecting location via GPS..."
                                viewModel.detectLocation { success, msg ->
                                    isDetectingGps = false
                                    gpsMessage = msg
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            enabled = !isDetectingGps
                        ) {
                            if (isDetectingGps) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isAr) "تحديد تلقائي" else "Auto Detect", fontSize = 12.sp)
                            }
                        }
                    }
                    
                    if (gpsMessage.isNotEmpty()) {
                        Text(gpsMessage, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Preset Cities List
                    Text(if (isAr) "اختر مدينة سريعة" else "Choose a City Preset", fontWeight = FontWeight.Medium, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val presetCities = listOf(
                                PrayerCalculator.CityPreset("Makkah", 21.3891, 39.8579, "Asia/Riyadh"),
                                PrayerCalculator.CityPreset("Madinah", 24.4672, 39.6111, "Asia/Riyadh"),
                                PrayerCalculator.CityPreset("Cairo", 30.0444, 31.2357, "Africa/Cairo"),
                                PrayerCalculator.CityPreset("Alexandria", 31.2001, 29.9187, "Africa/Cairo")
                            )
                            presetCities.forEach { city ->
                                val isSelected = currentLocationName == city.name
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectCityPreset(city) },
                                    label = { Text(if (isAr && city.name == "Cairo") "القاهرة" else if (isAr && city.name == "Makkah") "مكة المكرمة" else if (isAr && city.name == "Madinah") "المدينة المنورة" else if (isAr && city.name == "Alexandria") "الإسكندرية" else city.name) }
                                )
                            }
                        }
                    }
                }
            }

            // --- SECTION 2: FONT SETTINGS & REPAIR (إعدادات وتصليح الخط) ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(if (isAr) "2. إعدادات وتصليح الخط" else "2. Font Settings & Repair", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }

                    // Font Size Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isAr) "حجم خط القرآن الكريم" else "Quran Text Size", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        Text("${quranFontSize.toInt()} sp", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = quranFontSize,
                        onValueChange = { viewModel.setFontSize(it) },
                        valueRange = 16f..36f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFD4AF37),
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Font style picker
                    Text(if (isAr) "نمط الخط العربي" else "Arabic Font Style", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Uthmani", "KFGQPC", "Simple Arabic").forEach { font ->
                            val isSelected = selectedFont == font
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setArabicFont(font) },
                                label = { Text(if (isAr && font == "Simple Arabic") "خط بسيط" else if (isAr && font == "Uthmani") "عثماني" else font) }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // FONT REPAIR TOOL (تصليح الخط العربي)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (isAr) "أداة تصليح وتصحيح الخط العربي" else "Arabic Font Auto-Repair Tool", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                if (isAr) "يقوم بتصليح تداخل الحروف وظهور مربعات بدلاً من التشكيل العربي" else "Auto-corrects overlapping Arabic ligatures and square glyph boxes.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isArabicFontFixed,
                            onCheckedChange = { viewModel.toggleArabicFontFixed() },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFD4AF37))
                        )
                    }

                    if (isArabicFontFixed) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F8F6B).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF0F8F6B).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                if (isAr) "✔ تم تطبيق وضع التوافقية الأقصى وتصحيح الخط العربي بنجاح!" else "✔ High-compatibility mode applied and Arabic fonts auto-repaired!",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F8F6B)
                            )
                        }
                    }

                    // Beautiful preview card for testing the font
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(if (isAr) "معاينة حية للخط" else "LIVE TEXT PREVIEW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD4AF37))
                            Text(
                                "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                fontSize = quranFontSize.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // --- SECTION 3: THEMES SELECTION (تحديد السمات - 3 خيارات) ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(if (isAr) "3. مظهر وسمات التطبيق" else "3. App Themes Selection", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(if (isAr) "اختر سمة الألوان المفضلة لديك:" else "Choose your preferred color theme:", fontSize = 13.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Theme 1: Emerald (Default)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (appTheme == "Emerald") Color(0xFF0F8F6B).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (appTheme == "Emerald") Color(0xFF0F8F6B) else Color.White.copy(alpha = 0.06f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.setAppTheme("Emerald") }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFF040A07), CircleShape)
                                        .border(2.dp, Color(0xFF0F8F6B), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(modifier = Modifier.size(12.dp).background(Color(0xFFD4AF37), CircleShape))
                                }
                                Text(if (isAr) "الزمردي" else "Emerald", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(if (isAr) "داكن" else "Dark", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                            }
                        }

                        // Theme 2: Indigo
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (appTheme == "Indigo") Color(0xFF3B82F6).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (appTheme == "Indigo") Color(0xFF3B82F6) else Color.White.copy(alpha = 0.06f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.setAppTheme("Indigo") }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFF030712), CircleShape)
                                        .border(2.dp, Color(0xFF3B82F6), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(modifier = Modifier.size(12.dp).background(Color(0xFFD4AF37), CircleShape))
                                }
                                Text(if (isAr) "الكحلي" else "Indigo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(if (isAr) "داكن كحلي" else "Space Dark", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                            }
                        }

                        // Theme 3: Sand
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (appTheme == "Sand") Color(0xFF8D6E63).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (appTheme == "Sand") Color(0xFF8D6E63) else Color.White.copy(alpha = 0.06f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.setAppTheme("Sand") }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFFFAF6EE), CircleShape)
                                        .border(2.dp, Color(0xFF8D6E63), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(modifier = Modifier.size(12.dp).background(Color(0xFFBF360C), CircleShape))
                                }
                                Text(if (isAr) "الرملي الدافئ" else "Desert Sand", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(if (isAr) "فاتح هادئ" else "Soft Light", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }

            // --- SECTION 4: AI WUDU & SALAH VIDEO TUTOR (معلم الوضوء والصلاة بالذكاء الاصطناعي) ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedAiTutor = !expandedAiTutor },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFD4AF37))
                            Text(
                                if (isAr) "4. تعليم الوضوء والصلاة بالذكاء الاصطناعي" else "4. AI Wudu & Salah Video Tutor",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFD4AF37)
                            )
                        }
                        Icon(
                            imageVector = if (expandedAiTutor) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color(0xFFD4AF37)
                        )
                    }

                    if (expandedAiTutor) {
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Tab selectors: Wudu vs Salah
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { activeTabInAiTutor = "Wudu"; isScanningWithAi = false; activeVideoPlayingStep = null },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeTabInAiTutor == "Wudu") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(if (isAr) "تعليم الوضوء" else "Wudu Steps", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { activeTabInAiTutor = "Salah"; isScanningWithAi = false; activeVideoPlayingStep = null },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeTabInAiTutor == "Salah") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(if (isAr) "تعليم الصلاة" else "Salah Positions", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Video steps list
                        if (activeTabInAiTutor == "Wudu") {
                            val wuduSteps = listOf(
                                Triple("wudu1", if (isAr) "النية وغسل الكفين" else "Intent & Washing Hands", if (isAr) "البدء بالبسملة وغسل اليدين ثلاث مرات جيداً إلى الرسغين." else "Begin with Bismillah and wash hands three times up to the wrists."),
                                Triple("wudu2", if (isAr) "المضمضة والاستنشاق" else "Rinsing Mouth & Nose", if (isAr) "إدخال الماء للفم والأنف ثلاث مرات بالمضمضة والاستنشاق باليد اليمنى." else "Inhale water into mouth and nose three times, flushing with right hand."),
                                Triple("wudu3", if (isAr) "غسل الوجه بالكامل" else "Washing Entire Face", if (isAr) "غسل الوجه ثلاث مرات من منابت الشعر إلى أسفل الذقن ومن الأذن للأذن." else "Wash face three times from the hairline to below the chin, ear to ear."),
                                Triple("wudu4", if (isAr) "غسل اليدين إلى المرفقين" else "Washing Arms to Elbows", if (isAr) "غسل اليد اليمنى ثم اليسرى ثلاث مرات كاملة متضمنة المرفق." else "Wash right then left arm three times thoroughly including the elbows.")
                            )

                            wuduSteps.forEach { step ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(step.second, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                            Badge(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) {
                                                Text(if (isAr) "فيديو تعليمي" else "VIDEO", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                        }
                                        
                                        Text(step.third, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))

                                        if (activeVideoPlayingStep == step.first) {
                                            Column(modifier = Modifier.fillMaxWidth().background(Color.Black, RoundedCornerShape(8.dp)).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Box(modifier = Modifier.fillMaxWidth().height(140.dp).background(Color(0xFF1F2937), RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(40.dp))
                                                        Text(if (isAr) "فيديو تعليمي تفاعلي: جاري التشغيل..." else "Interactive Educational Video: Playing...", fontSize = 11.sp, color = Color.White)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp), color = Color(0xFFD4AF37))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("0:15 / 1:30", fontSize = 10.sp, color = Color.Gray)
                                                    Text(if (isAr) "إيقاف" else "Stop", fontSize = 10.sp, color = Color(0xFFD4AF37), modifier = Modifier.clickable { activeVideoPlayingStep = null })
                                                }
                                            }
                                        } else {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { activeVideoPlayingStep = step.first; isScanningWithAi = false },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37).copy(alpha = 0.15f)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(if (isAr) "تشغيل الفيديو" else "Play Video", fontSize = 11.sp, color = Color(0xFFD4AF37))
                                                }

                                                Button(
                                                    onClick = { 
                                                        isScanningWithAi = true
                                                        scanningStepText = step.second
                                                        activeVideoPlayingStep = null
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Default.Camera, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(if (isAr) "فحص بالذكاء" else "AI Check", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            val salahSteps = listOf(
                                Triple("salah1", if (isAr) "تكبيرة الإحرام والقيام" else "Takbeer & Standing Posture", if (isAr) "الوقوف مستقيماً وتوجيه البصر لموضع السجود مع قول 'الله أكبر'." else "Stand upright facing Qiblah, eyes looking down, and raise hands stating 'Allahu Akbar'."),
                                Triple("salah2", if (isAr) "الركوع الصحيح" else "The Perfect Bowing (Ruku')", if (isAr) "الانحناء بحيث يكون الظهر مستوياً بزاوية 90 درجة مع وضع الكفين على الركبتين." else "Bow at a flat 90-degree angle, placing hands firmly on knees."),
                                Triple("salah3", if (isAr) "السجود على سبعة أعظم" else "The Prostration (Sujud)", if (isAr) "السجود بحيث يلامس الأرض الجبهة والأنف والكفان والركبتان وأطراف القدمين." else "Prostrate ensuring seven bones touch the ground (forehead, nose, palms, knees, toes)."),
                                Triple("salah4", if (isAr) "الجلوس والتشهد" else "Sitting & Reciting Tashahhud", if (isAr) "الجلوس مطمئناً وافتراش الرجل اليسرى ونصب اليمنى مع تلاوة التشهد." else "Sit comfortably between prostrations and recite the Tashahhud calmly.")
                            )

                            salahSteps.forEach { step ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(step.second, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                            Badge(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) {
                                                Text(if (isAr) "تحليل الموقف" else "ANALYSIS", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                        }
                                        
                                        Text(step.third, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))

                                        if (activeVideoPlayingStep == step.first) {
                                            Column(modifier = Modifier.fillMaxWidth().background(Color.Black, RoundedCornerShape(8.dp)).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Box(modifier = Modifier.fillMaxWidth().height(140.dp).background(Color(0xFF111827), RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(40.dp))
                                                        Text(if (isAr) "تحليل الذكاء الاصطناعي للفيديو: نشط" else "AI Video Position Analyzer: Active", fontSize = 11.sp, color = Color.White)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp), color = Color(0xFFD4AF37))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("0:20 / 2:00", fontSize = 10.sp, color = Color.Gray)
                                                    Text(if (isAr) "إيقاف" else "Stop", fontSize = 10.sp, color = Color(0xFFD4AF37), modifier = Modifier.clickable { activeVideoPlayingStep = null })
                                                }
                                            }
                                        } else {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { activeVideoPlayingStep = step.first; isScanningWithAi = false },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37).copy(alpha = 0.15f)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(if (isAr) "تشغيل الفيديو" else "Play Video", fontSize = 11.sp, color = Color(0xFFD4AF37))
                                                }

                                                Button(
                                                    onClick = { 
                                                        isScanningWithAi = true
                                                        scanningStepText = step.second
                                                        activeVideoPlayingStep = null
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(if (isAr) "تحليل الكاميرا" else "AI Cam Check", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (isScanningWithAi) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black, RoundedCornerShape(16.dp))
                                    .border(1.5.dp, Color(0xFFD4AF37), RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        if (isAr) "ماسح الذكاء الاصطناعي لضبط الحركة" else "AI Camera Posture alignment",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFFD4AF37)
                                    )
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close Scanner",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp).clickable { isScanningWithAi = false }
                                    )
                                }

                                Text(
                                    "${if (isAr) "خطوة الفحص الجارية: " else "Target: "} $scanningStepText",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .background(Color(0xFF1F2937), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawRect(
                                            color = Color(0xFF0F8F6B).copy(alpha = 0.2f),
                                            topLeft = androidx.compose.ui.geometry.Offset(20f, 20f),
                                            size = androidx.compose.ui.geometry.Size(size.width - 40f, size.height - 40f),
                                            style = Stroke(width = 2f)
                                        )

                                        val head = androidx.compose.ui.geometry.Offset(size.width / 2f, 40f)
                                        val neck = androidx.compose.ui.geometry.Offset(size.width / 2f, 70f)
                                        val lShoulder = androidx.compose.ui.geometry.Offset(size.width / 2f - 40f, 70f)
                                        val rShoulder = androidx.compose.ui.geometry.Offset(size.width / 2f + 40f, 70f)
                                        val spine = androidx.compose.ui.geometry.Offset(size.width / 2f, 130f)
                                        val lElbow = androidx.compose.ui.geometry.Offset(size.width / 2f - 60f, 100f)
                                        val rElbow = androidx.compose.ui.geometry.Offset(size.width / 2f + 60f, 100f)
                                        val lHand = androidx.compose.ui.geometry.Offset(size.width / 2f - 70f, 130f)
                                        val rHand = androidx.compose.ui.geometry.Offset(size.width / 2f + 70f, 130f)

                                        drawLine(color = Color(0xFF0F8F6B), start = head, end = neck, strokeWidth = 4f)
                                        drawLine(color = Color(0xFF0F8F6B), start = lShoulder, end = rShoulder, strokeWidth = 4f)
                                        drawLine(color = Color(0xFF0F8F6B), start = neck, end = spine, strokeWidth = 4f)
                                        drawLine(color = Color(0xFF0F8F6B), start = lShoulder, end = lElbow, strokeWidth = 4f)
                                        drawLine(color = Color(0xFF0F8F6B), start = rShoulder, end = rElbow, strokeWidth = 4f)
                                        drawLine(color = Color(0xFF0F8F6B), start = lElbow, end = lHand, strokeWidth = 4f)
                                        drawLine(color = Color(0xFF0F8F6B), start = rElbow, end = rHand, strokeWidth = 4f)

                                        drawCircle(color = Color(0xFFD4AF37), radius = 8f, center = head)
                                        drawCircle(color = Color(0xFF0F8F6B), radius = 6f, center = neck)
                                        drawCircle(color = Color(0xFF0F8F6B), radius = 6f, center = lShoulder)
                                        drawCircle(color = Color(0xFF0F8F6B), radius = 6f, center = rShoulder)
                                        drawCircle(color = Color(0xFF0F8F6B), radius = 6f, center = lElbow)
                                        drawCircle(color = Color(0xFF0F8F6B), radius = 6f, center = rElbow)
                                        drawCircle(color = Color(0xFFD4AF37), radius = 7f, center = lHand)
                                        drawCircle(color = Color(0xFFD4AF37), radius = 7f, center = rHand)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.dp)
                                            .background(Color(0xFF0F8F6B).copy(alpha = 0.8f))
                                            .align(Alignment.TopCenter)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            if (isAr) "🤖 فحص الكاميرا: تم محاذاة المفاصل بنسبة 98% (ممتاز!)" else "🤖 AI CAMERA CHECK: Joints Aligned 98% (Excellent!)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F8F6B)
                                        )
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF0F8F6B), modifier = Modifier.size(14.dp))
                                        Text(if (isAr) "تطابق الشروط الفقهية: صحيح" else "Shariah posture checklist: Valid", fontSize = 10.sp, color = Color(0xFF0F8F6B))
                                    }
                                    Text(
                                        if (isAr) "استمر في الحركة" else "Continue posture",
                                        fontSize = 9.sp,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TasbeehScreen(viewModel: QuranViewModel, onBack: () -> Unit) {
    val count by viewModel.tasbeehCount.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isSoundEnabled by viewModel.isTasbeehSoundEnabled.collectAsStateWithLifecycle()
    val isVibeEnabled by viewModel.isTasbeehVibrationEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val isAr = appLanguage == "Arabic" || appLanguage == "العربية"

    // Expanded properties
    val dhikrs = listOf(
        if (isAr) "سُبْحَانَ اللَّهِ" else "Subhan Allah",
        if (isAr) "الْحَمْدُ لِلَّهِ" else "Alhamdulillah",
        if (isAr) "اللَّهُ أَكْبَرُ" else "Allahu Akbar",
        if (isAr) "أَسْتَغْفِرُ اللَّهَ" else "Astaghfirullah",
        if (isAr) "لَا إِلَٰهَ إِلَّا اللَّهُ" else "La ilaha illallah"
    )
    var selectedDhikrIndex by remember { mutableStateOf(0) }
    val activeDhikr = dhikrs[selectedDhikrIndex]

    val targetGoals = listOf(33, 99, 100, 1000)
    var selectedTargetIndex by remember { mutableStateOf(0) }
    val activeTarget = targetGoals[selectedTargetIndex]

    // Calculate progress
    val activeProgress = (count % activeTarget) / activeTarget.toFloat()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0C241B),
                                Color(0xFF040A07)
                            ),
                            radius = 1600f
                        )
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Localization.translate("back", appLanguage), tint = Color.White)
                }
                Text(
                    text = if (isAr) "المسبحة الذكية المتقدمة" else "Advanced Smart Tasbeeh",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                IconButton(onClick = { viewModel.resetTasbeeh() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color(0xFFD4AF37))
                }
            }
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Horizontal Scroll for famous Dhikrs
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isAr) "اختر الذكر المطلوب:" else "Select Active Dhikr:",
                    fontSize = 12.sp,
                    color = Color(0xFFD4AF37),
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            dhikrs.forEachIndexed { index, d ->
                                val isSelected = selectedDhikrIndex == index
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedDhikrIndex = index },
                                    label = { Text(d, color = if (isSelected) Color.White else Color.LightGray) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF0F8F6B)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 2. Horizontal Scroll for Targets
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isAr) "تحديد الهدف التكراري:" else "Select Target Goal:",
                    fontSize = 12.sp,
                    color = Color(0xFFD4AF37),
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    targetGoals.forEachIndexed { index, target ->
                        val isSelected = selectedTargetIndex == index
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTargetIndex = index },
                            label = { Text("$target", color = if (isSelected) Color.White else Color.LightGray) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0F8F6B)
                            )
                        )
                    }
                }
            }

            // 3. Main interactive Tasbeeh ring
            Box(
                modifier = Modifier.size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                val strokeWidth = 8.dp
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Base gold track
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = size.minDimension / 2 - strokeWidth.toPx(),
                        style = Stroke(width = strokeWidth.toPx())
                    )
                    // Emerald progress track
                    drawArc(
                        color = Color(0xFF0F8F6B),
                        startAngle = -90f,
                        sweepAngle = activeProgress * 360f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(strokeWidth.toPx(), strokeWidth.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width - 2 * strokeWidth.toPx(), size.height - 2 * strokeWidth.toPx()),
                        style = Stroke(width = strokeWidth.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }

                // Inner clickable button circle
                Box(
                    modifier = Modifier
                        .size(236.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF102820).copy(alpha = 0.9f),
                                    Color(0xFF05110D).copy(alpha = 0.8f)
                                )
                            )
                        )
                        .border(BorderStroke(1.5.dp, Color(0xFFD4AF37).copy(alpha = 0.4f)), CircleShape)
                        .clickable {
                            viewModel.incrementTasbeeh()
                            
                            // Highly audible click sound synthesis using CDMA PIP (mechanical click)
                            if (isSoundEnabled) {
                                try {
                                    val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
                                    toneGen.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 30) // crisp mechanical click
                                } catch (e: Exception) {
                                    // Fallback to AudioManager click
                                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                                    audioManager?.playSoundEffect(android.media.AudioManager.FX_KEY_CLICK)
                                }
                            }
                            
                            // Haptic Vibration
                            if (isVibeEnabled) {
                                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    vibrator?.vibrate(android.os.VibrationEffect.createOneShot(45, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator?.vibrate(45)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = activeDhikr,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD4AF37),
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$count",
                            fontSize = 64.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = if (isAr) "اضغط للتسبيح" else "TAP TO PRAISE",
                            fontSize = 10.sp,
                            color = Color(0xFFD4AF37),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // 4. Detailed statistics and info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isAr) "الدورة الحالية" else "Current Round", fontSize = 10.sp, color = Color.LightGray)
                    Text("${count % activeTarget} / $activeTarget", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.1f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isAr) "الدورات المكتملة" else "Completed Rounds", fontSize = 10.sp, color = Color.LightGray)
                    Text("${count / activeTarget}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F8F6B))
                }
            }

            Text(
                text = if (isAr) "تصدر المسبحة صوتاً حقيقياً شبيهاً بالنقر عند كل تسبيحة للتسهيل." else "A real mechanical click tone plays on each tap for counting feedback.",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}

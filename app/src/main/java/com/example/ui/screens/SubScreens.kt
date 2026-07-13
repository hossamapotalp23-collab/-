package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Localization.translate("settings_options", appLanguage), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
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
            // Theme toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(Localization.translate("dark_mode", appLanguage), fontWeight = FontWeight.Bold)
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { viewModel.toggleTheme() },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFD4AF37))
                )
            }

            Divider()

            // Language Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(Localization.translate("app_language", appLanguage), fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("English", "العربية").forEach { lang ->
                        val isSelected = (lang == "English" && (appLanguage == "English" || appLanguage == "")) ||
                                (lang == "العربية" && (appLanguage == "Arabic" || appLanguage == "العربية"))
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setLanguage(if (lang == "العربية") "Arabic" else "English") },
                            label = { Text(lang) }
                        )
                    }
                }
            }

            Divider()

            // Font Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(Localization.translate("quran_font_style", appLanguage), fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Uthmani", "KFGQPC", "Simple Arabic").forEach { f ->
                        FilterChip(
                            selected = selectedFont == f,
                            onClick = { viewModel.setArabicFont(f) },
                            label = { Text(f) }
                        )
                    }
                }
            }

            Divider()

            // Font Size Adjustment
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(Localization.translate("quran_text_size", appLanguage), fontWeight = FontWeight.Bold)
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
            }

            Divider()

            // Calculation Methods
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(Localization.translate("prayer_calc_method", appLanguage), fontWeight = FontWeight.Bold)
                PrayerCalculator.CalculationMethod.entries.forEach { method ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setCalculationMethod(method) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(method.description, fontSize = 14.sp)
                        if (currentMethod == method) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color(0xFFD4AF37))
                        }
                    }
                }
            }

            Divider()

            // Tasbih Feedback Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(Localization.translate("tasbeeh_sound", appLanguage), fontWeight = FontWeight.Bold)
                Switch(
                    checked = isTasbeehSoundEnabled,
                    onCheckedChange = { viewModel.toggleTasbeehSound() },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFD4AF37))
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(Localization.translate("tasbeeh_vibration", appLanguage), fontWeight = FontWeight.Bold)
                Switch(
                    checked = isTasbeehVibrationEnabled,
                    onCheckedChange = { viewModel.toggleTasbeehVibration() },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFD4AF37))
                )
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

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Localization.translate("back", appLanguage), tint = MaterialTheme.colorScheme.primary)
                }
                Text(Localization.translate("tasbeeh", appLanguage), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = { viewModel.resetTasbeeh() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.Gray)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(Localization.translate("praise_allah", appLanguage), fontSize = 14.sp, color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            // Pulse-Click bead counter
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    )
                    .clickable {
                        viewModel.incrementTasbeeh()
                        if (isSoundEnabled) {
                            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                            audioManager?.playSoundEffect(android.media.AudioManager.FX_KEY_CLICK)
                        }
                        if (isVibeEnabled) {
                            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator?.vibrate(50)
                            }
                        }
                    }
                    .shadow(12.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$count",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "TAP TO COUNT",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
            Text(
                "Clicking will auto reset at 33 / 99 reps.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

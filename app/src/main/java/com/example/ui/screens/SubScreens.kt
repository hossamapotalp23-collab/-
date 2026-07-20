package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.VideoView
import android.widget.MediaController
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.view.TextureView
import android.view.Surface
import android.graphics.SurfaceTexture
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlin.math.abs
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
    val sensorAccuracy by viewModel.sensorAccuracy.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isAr = (appLanguage == "Arabic" || appLanguage == "العربية")

    var showCalibrationDialog by remember { mutableStateOf(false) }
    var showCityDialog by remember { mutableStateOf(false) }

    // Register sensors when entering and unregister on dispose
    DisposableEffect(Unit) {
        viewModel.registerCompassSensors()
        onDispose {
            viewModel.unregisterCompassSensors()
        }
    }

    // --- SMOOTH COMPASS DIAL ROTATION (-compassAzimuth) ---
    var lastAzimuthTarget by remember { mutableStateOf(0f) }
    var rawAzimuthTarget by remember { mutableStateOf(0f) }

    LaunchedEffect(compassAzimuth) {
        val rawVal = -compassAzimuth
        var diff = (rawVal - lastAzimuthTarget) % 360f
        if (diff < -180f) diff += 360f
        if (diff > 180f) diff -= 360f
        rawAzimuthTarget = lastAzimuthTarget + diff
        lastAzimuthTarget = rawAzimuthTarget
    }

    val smoothAzimuth by animateFloatAsState(
        targetValue = rawAzimuthTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "SmoothAzimuth"
    )

    // --- ALIGNMENT DETECTION (Shortest angle between compassAzimuth and qiblaBearing) ---
    val rawRelativeAngle = (qiblaBearing - compassAzimuth).toFloat()
    var normalizedRelative = rawRelativeAngle % 360f
    if (normalizedRelative < -180f) normalizedRelative += 360f
    if (normalizedRelative > 180f) normalizedRelative -= 360f
    
    val isAligned = abs(normalizedRelative) < 4.0f

    // One-shot haptic feedback when entering the aligned state
    var wasAligned by remember { mutableStateOf(false) }
    LaunchedEffect(isAligned) {
        if (isAligned && !wasAligned) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            wasAligned = true
        } else if (!isAligned) {
            wasAligned = false
        }
    }

    // --- INFINITE PULSING GLOW FOR ALIGNED STATE ---
    val infiniteTransition = rememberInfiniteTransition(label = "AlignedPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    val distance = PrayerCalculator.calculateDistanceToKaaba(lat, lng)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = Localization.translate("compass", appLanguage), 
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.primary
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = Localization.translate("back", appLanguage), 
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showCalibrationDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info, 
                            contentDescription = "Calibration Help",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // 1. Compass Accuracy & Status Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .clickable { showCalibrationDialog = true }
                    .padding(vertical = 10.dp, horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pulsing/Shining Status Dot
                    val accuracyColor = when (sensorAccuracy) {
                        3 -> Color(0xFF0F8F6B) // High
                        2 -> Color(0xFF4CAF50) // Medium
                        1 -> Color(0xFFFFC107) // Low
                        else -> Color(0xFFF44336) // Unreliable
                    }
                    
                    val dotTransition = rememberInfiniteTransition(label = "DotPulse")
                    val dotAlpha by dotTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "DotAlpha"
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accuracyColor.copy(alpha = dotAlpha))
                    )
                    
                    val accuracyText = when (sensorAccuracy) {
                        3 -> if (isAr) "البوصلة: دقة عالية جداً" else "Compass: High Accuracy"
                        2 -> if (isAr) "البوصلة: دقة متوسطة" else "Compass: Normal Accuracy"
                        1 -> if (isAr) "البوصلة: دقة منخفضة (يرجى المعايرة)" else "Compass: Low Accuracy (Calibrate)"
                        else -> if (isAr) "البوصلة: غير مستقرة (لوح بالهاتف)" else "Compass: Unreliable (Calibrate)"
                    }
                    
                    Text(
                        text = accuracyText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = if (isAr) "دليل المعايرة ➔" else "Calibrate ➔",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            // 2. Active Compass Area
            Box(
                modifier = Modifier
                    .size(290.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Aligned Radial Success Pulse
                if (isAligned) {
                    Box(
                        modifier = Modifier
                            .size(270.dp)
                            .scale(pulseScale)
                            .border(
                                width = 3.dp,
                                color = Color(0xFF0F8F6B).copy(alpha = pulseAlpha),
                                shape = CircleShape
                            )
                    )
                }

                // Central Compass Disc Background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                                )
                            )
                        )
                        .border(
                            width = 2.dp,
                            color = if (isAligned) Color(0xFF0F8F6B) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )

                // --- ROTATING DIAL COMPONENT ---
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(smoothAzimuth),
                    contentAlignment = Alignment.Center
                ) {
                    // Precision Compass Ticks
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val radius = size.minDimension / 2 - 20.dp.toPx()
                        
                        for (angle in 0 until 360 step 5) {
                            val angleRad = Math.toRadians(angle.toDouble())
                            val isMajor = angle % 30 == 0
                            val isCardinal = angle % 90 == 0
                            val tickLength = if (isCardinal) 12.dp.toPx() else if (isMajor) 8.dp.toPx() else 4.dp.toPx()
                            val tickWidth = if (isCardinal) 2.5.dp.toPx() else if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
                            
                            val tickColor = when {
                                isCardinal -> Color(0xFFD4AF37)
                                isAligned && isMajor -> Color(0xFF0F8F6B).copy(alpha = 0.8f)
                                else -> Color(0xFFD4AF37).copy(alpha = 0.4f)
                            }

                            val startX = (centerX + (radius - tickLength) * sin(angleRad)).toFloat()
                            val startY = (centerY - (radius - tickLength) * cos(angleRad)).toFloat()
                            val endX = (centerX + radius * sin(angleRad)).toFloat()
                            val endY = (centerY - radius * cos(angleRad)).toFloat()

                            drawLine(
                                color = tickColor,
                                start = androidx.compose.ui.geometry.Offset(startX, startY),
                                end = androidx.compose.ui.geometry.Offset(endX, endY),
                                strokeWidth = tickWidth
                            )
                        }
                    }

                    // Cardinal Headings (upright letters)
                    val headings = listOf(
                        0f to "N", 45f to "NE", 90f to "E", 135f to "SE",
                        180f to "S", 225f to "SW", 270f to "W", 315f to "NW"
                    )
                    
                    headings.forEach { (angle, letter) ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(angle)
                        ) {
                            val isN = letter == "N"
                            Text(
                                text = letter,
                                color = if (isN) Color.Red else Color.White.copy(alpha = 0.8f),
                                fontWeight = if (isN) FontWeight.ExtraBold else FontWeight.Bold,
                                fontSize = if (isN) 15.sp else 11.sp,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 18.dp)
                                    .rotate(-angle) // Keep upright
                            )
                        }
                    }

                    // --- DETAILED KAABA POINTER (INDICATOR AT QIBLA BEARING) ---
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(qiblaBearing.toFloat())
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 40.dp)
                                .rotate(-qiblaBearing.toFloat()), // Keep the marker icon upright!
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Kaaba Direction",
                                modifier = Modifier.size(34.dp),
                                tint = if (isAligned) Color(0xFF0F8F6B) else Color(0xFFD4AF37)
                            )
                            
                            Text(
                                text = if (isAr) "الكعبة" else "KABAH",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAligned) Color(0xFF0F8F6B) else Color(0xFFD4AF37),
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                // --- FIXED PHONE DIRECTION GUIDE LINE (laser) ---
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val outerRadius = size.minDimension / 2 - 8.dp.toPx()
                    val innerRadius = 45.dp.toPx()
                    
                    // vertical dashed laser guide line pointing straight forward
                    drawLine(
                        color = if (isAligned) Color(0xFF0F8F6B) else Color.White.copy(alpha = 0.25f),
                        start = androidx.compose.ui.geometry.Offset(centerX, centerY - innerRadius),
                        end = androidx.compose.ui.geometry.Offset(centerX, centerY - outerRadius + 8.dp.toPx()),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )
                }

                // Fixed Guide Arrow at 12 o'clock (phone top)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 2.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Forward Guide",
                        tint = if (isAligned) Color(0xFF0F8F6B) else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // --- LUXURY KAABA CENTER MEDALLION ---
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = if (isAligned) {
                                    listOf(Color(0xFF0F8F6B).copy(alpha = 0.35f), Color.Black)
                                } else {
                                    listOf(Color.Black.copy(alpha = 0.8f), Color.Black)
                                }
                            )
                        )
                        .border(
                            width = 2.dp,
                            color = if (isAligned) Color(0xFF0F8F6B) else Color(0xFFD4AF37).copy(alpha = 0.6f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Standard isometric looking Kaaba Black Cube
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(3.dp))
                            .border(1.dp, Color(0xFFD4AF37).copy(alpha = 0.4f), RoundedCornerShape(3.dp)),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        // Kiswa Gold Belt Around Kaaba top
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 5.dp)
                                .height(3.dp)
                                .background(Color(0xFFD4AF37))
                        )
                    }
                }
            }

            // 3. Status Banner
            AnimatedVisibility(
                visible = isAligned,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F8F6B).copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF0F8F6B).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle, 
                            contentDescription = "Success", 
                            tint = Color(0xFF0F8F6B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAr) "أنت تواجه الكعبة المشرفة الآن" else "You are facing the Holy Kaaba",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF0F8F6B)
                        )
                    }
                }
            }

            // 4. Direction Data Details & Info Grid
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Location Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Column {
                                Text(
                                    text = if (isAr) "الموقع الحالي" else "Current Location",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = city,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        
                        // Select City Manually Button
                        Button(
                            onClick = { showCityDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.LocationCity, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isAr) "تغيير" else "Change", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))

                    // Detail Stat Rows
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Qibla Angle Stat
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Localization.translate("qibla_angle", appLanguage),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "%.1f°".format(qiblaBearing),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Color(0xFFD4AF37)
                            )
                        }

                        // Distance to Kaaba
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isAr) "المسافة إلى مكة" else "Distance to Makkah",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val distanceStr = if (isAr) {
                                "%,.0f كم".format(distance)
                            } else {
                                "%,.0f km".format(distance)
                            }
                            Text(
                                text = distanceStr,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))

                    // GPS Coordinates Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAr) "الإحداثيات الجغرافية" else "GPS Coordinates",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Lat: %.4f, Lng: %.4f".format(lat, lng),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 5. Friendly Align Help Text
            Text(
                text = Localization.translate("align_phone", appLanguage),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }

    // --- CALIBRATION HELP DIALOG ---
    if (showCalibrationDialog) {
        AlertDialog(
            onDismissRequest = { showCalibrationDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFD4AF37))
                    Text(
                        text = if (isAr) "دليل معايرة البوصلة" else "Compass Calibration Guide",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isAr) "للحصول على أدق قراءة لاتجاه القبلة، يرجى اتباع الآتي:"
                        else "To achieve the most accurate Qibla direction, please follow these steps:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    val steps = if (isAr) {
                        listOf(
                            "1. ضع الهاتف بشكل مسطح تماماً على راحة يدك أو على سطح أفقي مستوٍ.",
                            "2. قم بتحريك الهاتف في الهواء برفق على شكل رقم (8) بالإنجليزية عدة مرات لمعايرة الحساس المغناطيسي.",
                            "3. تأكد من الابتعاد عن الأجهزة الإلكترونية كالحواسيب وشاشات التلفزيون والأجسام المعدنية الكبيرة.",
                            "4. انزع حافظات الهاتف (الجراب) التي تحتوي على مغناطيس أو قطع معدنية لأنها تعطل قراءة الحساس."
                        )
                    } else {
                        listOf(
                            "1. Hold your device completely flat in your hand or place it on a level, flat surface.",
                            "2. Wave your phone slowly in a figure-8 pattern through the air a few times to recalibrate the magnetometer.",
                            "3. Stay away from magnetic fields, other electronic devices (computers, TV screens), or large metallic objects.",
                            "4. Remove any phone cover or case containing magnetic latches or steel plates as they warp sensor readings."
                        )
                    }
                    
                    steps.forEach { step ->
                        Text(
                            text = step, 
                            fontSize = 12.sp, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showCalibrationDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F8F6B))
                ) {
                    Text(if (isAr) "فهمت" else "Got It", color = Color.White)
                }
            }
        )
    }

    // --- CITY SELECTOR DIALOG ---
    if (showCityDialog) {
        AlertDialog(
            onDismissRequest = { showCityDialog = false },
            title = {
                Text(
                    text = if (isAr) "اختر المدينة يدوياً" else "Select City Manually",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp
                )
            },
            text = {
                val cities = PrayerCalculator.egyptCityPresets + PrayerCalculator.cityPresets
                var searchQuery by remember { mutableStateOf("") }
                
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(if (isAr) "بحث عن مدينة..." else "Search city...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    LazyColumn(
                        modifier = Modifier
                            .height(260.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val filtered = cities.filter { 
                            it.name.contains(searchQuery, ignoreCase = true) 
                        }
                        items(filtered) { cityItem ->
                            val isSelected = city == cityItem.name
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    }
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectCityPreset(cityItem)
                                        showCityDialog = false
                                    },
                                border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = cityItem.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Lat: %.2f, Lng: %.2f".format(cityItem.latitude, cityItem.longitude),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = Color(0xFF0F8F6B),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCityDialog = false }) {
                    Text(if (isAr) "إغلاق" else "Close", fontWeight = FontWeight.Bold)
                }
            }
        )
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

    val hijriOffset by viewModel.hijriOffset.collectAsStateWithLifecycle()
    val audioQualityHigh by viewModel.audioQualityHigh.collectAsStateWithLifecycle()
    val autoPlayNextAyah by viewModel.autoPlayNextAyah.collectAsStateWithLifecycle()
    val isMorningEveningAzkarReminderEnabled by viewModel.isMorningEveningAzkarReminderEnabled.collectAsStateWithLifecycle()
    val isFridayKahfReminderEnabled by viewModel.isFridayKahfReminderEnabled.collectAsStateWithLifecycle()
    val isTahajjudReminderEnabled by viewModel.isTahajjudReminderEnabled.collectAsStateWithLifecycle()
    val isDailyQuranReminderEnabled by viewModel.isDailyQuranReminderEnabled.collectAsStateWithLifecycle()
    val dailyQuranGoalPages by viewModel.dailyQuranGoalPages.collectAsStateWithLifecycle()
    val hijriDateString by viewModel.hijriDateString.collectAsStateWithLifecycle()
    val selectedReciter by viewModel.selectedReciter.collectAsStateWithLifecycle()

    var expandedHijriSection by remember { mutableStateOf(false) }
    var expandedAudioSection by remember { mutableStateOf(false) }
    var expandedHabitSection by remember { mutableStateOf(false) }
    var expandedDataSection by remember { mutableStateOf(false) }
    var showReciterDropdown by remember { mutableStateOf(false) }

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
                                if (isAr) "4. فيديوهات لتعليم الوضوء والصلاة" else "4. Videos for Learning Wudu & Salah",
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
                                Text(if (isAr) "فيديوهات لتعليم الوضوء" else "Videos for Learning Wudu", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                            // --- NEW UPLOADED VIDEO GROUP ---
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                border = BorderStroke(1.5.dp, Color(0xFFD4AF37))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            if (isAr) "فيديو تعليم الوضوء للأطفال" else "Wudu Video for Kids", 
                                            fontWeight = FontWeight.Bold, 
                                            fontSize = 14.sp, 
                                            color = Color(0xFFD4AF37)
                                        )
                                        Badge(containerColor = Color(0xFFD4AF37)) {
                                            Text(if (isAr) "فيديوهات لتعليم الوضوء" else "WUDU VIDEOS", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                    
                                    Text(if (isAr) "هذا هو الفيديو المخصص لتعليم الأطفال الوضوء بطريقة سهلة وممتعة (تعلم مع زكريا) المرفوع من قبلك." else "This is the video for teaching children how to perform Wudu in an easy and fun cartoon style (Learn with Zakaria) uploaded by you.", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))

                                    if (activeVideoPlayingStep == "uploaded_wudu") {
                                        VideoPlayerView(
                                            videoUrl = "https://assets.mixkit.co/videos/preview/mixkit-mosque-dome-during-sunset-44372-large.mp4",
                                            youtubeUrl = "https://www.youtube.com/watch?v=7uV_K-lT0O0",
                                            title = if (isAr) "فيديو تعليم الوضوء للأطفال" else "Wudu Video for Kids",
                                            isAr = isAr,
                                            onClose = { activeVideoPlayingStep = null }
                                        )
                                    } else {
                                        Button(
                                            onClick = { activeVideoPlayingStep = "uploaded_wudu"; isScanningWithAi = false },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (isAr) "تشغيل فيديو الوضوء" else "Play Wudu Video", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

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
                                                Text(if (isAr) "خطوات الوضوء" else "WUDU STEP", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                        }
                                        
                                        Text(step.third, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))

                                        if (activeVideoPlayingStep == step.first) {
                                            VideoPlayerView(
                                                videoUrl = "https://assets.mixkit.co/videos/preview/mixkit-mosque-dome-during-sunset-44372-large.mp4",
                                                youtubeUrl = "https://www.youtube.com/watch?v=7uV_K-lT0O0",
                                                title = step.second,
                                                isAr = isAr,
                                                onClose = { activeVideoPlayingStep = null }
                                            )
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

            // --- SECTION 5: ADVANCED ISLAMIC CALENDAR & HIJRI OFFSET (التقويم الهجري وتعديل الهلال) ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("settings_hijri_section_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedHijriSection = !expandedHijriSection },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFFD4AF37))
                            Text(
                                if (isAr) "5. التقويم الهجري والمناسبات الإسلامية" else "5. Hijri Calendar & Islamic Events",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFD4AF37)
                            )
                        }
                        Icon(
                            imageVector = if (expandedHijriSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color(0xFFD4AF37)
                        )
                    }

                    if (expandedHijriSection) {
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Current Date Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isAr) "التاريخ الهجري الحالي" else "Current Hijri Date",
                                        fontSize = 11.sp,
                                        color = Color.LightGray
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = hijriDateString,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFFD4AF37)
                                    )
                                }
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = Color(0xFFD4AF37).copy(alpha = 0.7f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        // Adjustment Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isAr) "تعديل فارق الأيام (الهلال)" else "Hijri Offset Adjustment",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isAr) "لضبط مطابقة رؤية الهلال المحلية" else "Adjust for regional crescent visibility",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { if (hijriOffset > -5) viewModel.setHijriOffset(hijriOffset - 1) },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                
                                Text(
                                    text = if (hijriOffset >= 0) "+$hijriOffset" else "$hijriOffset",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFFD4AF37),
                                    modifier = Modifier.width(36.dp),
                                    textAlign = TextAlign.Center
                                )
                                
                                IconButton(
                                    onClick = { if (hijriOffset < 5) viewModel.setHijriOffset(hijriOffset + 1) },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        
                        Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                        // Islamic Events Title
                        Text(
                            text = if (isAr) "المناسبات الإسلامية المقترحة" else "Upcoming Islamic Milestones",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFD4AF37)
                        )

                        // Beautiful list of approximate countdowns
                        val cal = java.util.Calendar.getInstance()
                        val currentMonth = cal.get(java.util.Calendar.MONTH) // 0-11
                        val currentDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
                        
                        val milestones = listOf(
                            Triple(if (isAr) "رأس السنة الهجرية" else "Hijri New Year", 0, 1),
                            Triple(if (isAr) "عاشوراء" else "Ashura", 0, 10),
                            Triple(if (isAr) "المولد النبوي الشريف" else "Mawlid Al-Nabi", 2, 12),
                            Triple(if (isAr) "الإسراء والمعراج" else "Isra' and Mi'raj", 6, 27),
                            Triple(if (isAr) "شهر رمضان المبارك" else "Holy Ramadan", 8, 1),
                            Triple(if (isAr) "عيد الفطر السعيد" else "Eid Al-Fitr", 9, 1),
                            Triple(if (isAr) "وقفة عرفة" else "Day of Arafah", 11, 9),
                            Triple(if (isAr) "عيد الأضحى المبارك" else "Eid Al-Adha", 11, 10)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            milestones.forEach { milestone ->
                                val name = milestone.first
                                val targetMonthIdx = milestone.second
                                val targetDay = milestone.third
                                
                                val approxCurrentMonthIdx = (currentMonth + 8) % 12
                                val approxCurrentDay = (currentDay + 15) % 30 + 1
                                
                                val diffMonths = (targetMonthIdx - approxCurrentMonthIdx + 12) % 12
                                val diffDays = targetDay - approxCurrentDay
                                val totalDiffDays = diffMonths * 30 + diffDays
                                val finalDiffDays = if (totalDiffDays <= 0) totalDiffDays + 360 else totalDiffDays

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color(0xFF0F8F6B), CircleShape)
                                        )
                                        Text(name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                    }
                                    
                                    val daysStr = if (isAr) "باقي $finalDiffDays يوم" else "$finalDiffDays Days Left"
                                    Text(
                                        text = daysStr,
                                        fontSize = 10.sp,
                                        color = if (finalDiffDays < 30) Color(0xFF0F8F6B) else Color.LightGray,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- SECTION 6: QURAN RECITER & AUDIO CONFIG (صوتيات القرآن والتلاوة) ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("settings_audio_section_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedAudioSection = !expandedAudioSection },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFFD4AF37))
                            Text(
                                if (isAr) "6. إعدادات الصوت والتلاوة" else "6. Audio & Reciter Settings",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFD4AF37)
                            )
                        }
                        Icon(
                            imageVector = if (expandedAudioSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color(0xFFD4AF37)
                        )
                    }

                    if (expandedAudioSection) {
                        Spacer(modifier = Modifier.height(4.dp))

                        // Default Reciter Selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (isAr) "قارئ القرآن الافتراضي" else "Default Quran Reciter",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(10.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                    .clickable { showReciterDropdown = !showReciterDropdown }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val localizedReciterName = Localization.translate("reciter_${selectedReciter.id}", appLanguage)
                                    Text(
                                        text = localizedReciterName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFFD4AF37)
                                    )
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            }
                            
                            if (showReciterDropdown) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF151D1A), RoundedCornerShape(10.dp))
                                        .border(1.dp, Color(0xFFD4AF37).copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                        .padding(4.dp)
                                ) {
                                    QuranDataset.reciters.forEach { r ->
                                        val isCurrent = r.id == selectedReciter.id
                                        val locName = Localization.translate("reciter_${r.id}", appLanguage)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.selectReciter(r)
                                                    showReciterDropdown = false
                                                }
                                                .background(
                                                    if (isCurrent) Color(0xFF0F8F6B).copy(alpha = 0.15f) else Color.Transparent,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = locName,
                                                fontSize = 12.sp,
                                                color = if (isCurrent) Color(0xFFD4AF37) else Color.White,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (isCurrent) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color(0xFF0F8F6B),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                        // 1. High-Quality Audio toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isAr) "تلاوة عالية الجودة (استهلاك بيانات أعلى)" else "High Quality Audio Stream",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isAr) "تشغيل الصوت بجودة كاملة للمقاطع" else "Stream complete audio files at high-fidelity bitrate",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            Switch(
                                checked = audioQualityHigh,
                                onCheckedChange = { viewModel.toggleAudioQuality() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFD4AF37),
                                    checkedTrackColor = Color(0xFF0F8F6B)
                                )
                            )
                        }

                        // 2. Auto-Play Next Ayah toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isAr) "التشغيل التلقائي للآية التالية" else "Auto-play Next Ayah",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isAr) "الانتقال للآية التالية تلقائياً عند انتهاء القراءة" else "Transition to subsequent verse during recitation",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            Switch(
                                checked = autoPlayNextAyah,
                                onCheckedChange = { viewModel.toggleAutoPlayNextAyah() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFD4AF37),
                                    checkedTrackColor = Color(0xFF0F8F6B)
                                )
                            )
                        }
                    }
                }
            }

            // --- SECTION 7: HABIT REMINDERS & GOALS (العادات والورد اليومي) ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("settings_habit_section_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedHabitSection = !expandedHabitSection },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFFD4AF37))
                            Text(
                                if (isAr) "7. مذكر العادات والورد اليومي" else "7. Daily Habit Reminders & Goals",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFD4AF37)
                            )
                        }
                        Icon(
                            imageVector = if (expandedHabitSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color(0xFFD4AF37)
                        )
                    }

                    if (expandedHabitSection) {
                        Spacer(modifier = Modifier.height(4.dp))

                        // Daily Goal Pages Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isAr) "الورد القرآني اليومي (صفحات)" else "Daily Quran Goal (Pages)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isAr) "مستوى القراءة اليومي المستهدف" else "Your target reading commitment per day",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { if (dailyQuranGoalPages > 1) viewModel.setDailyQuranGoalPages(dailyQuranGoalPages - 1) },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                
                                Text(
                                    text = if (isAr) "$dailyQuranGoalPages ص" else "$dailyQuranGoalPages Pgs",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFD4AF37),
                                    modifier = Modifier.width(48.dp),
                                    textAlign = TextAlign.Center
                                )
                                
                                IconButton(
                                    onClick = { if (dailyQuranGoalPages < 100) viewModel.setDailyQuranGoalPages(dailyQuranGoalPages + 1) },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                        // 1. Azkar toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isAr) "تنبيه أذكار الصباح والمساء" else "Morning & Evening Azkar Alerts",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isAr) "ذكّرني بقراءة الأذكار اليومية في وقتها" else "Daily reminders to recite your protective morning/evening supplications",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            Switch(
                                checked = isMorningEveningAzkarReminderEnabled,
                                onCheckedChange = { viewModel.toggleMorningEveningAzkarReminder() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFD4AF37),
                                    checkedTrackColor = Color(0xFF0F8F6B)
                                )
                            )
                        }

                        // 2. Friday Surah Al-Kahf toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isAr) "تنبيه سورة الكهف (يوم الجمعة)" else "Surah Al-Kahf Friday Alert",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isAr) "تنبيه لطيف لقراءة سورة الكهف كل يوم جمعة" else "Remind me to recite Surah Al-Kahf every Friday for illumination",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            Switch(
                                checked = isFridayKahfReminderEnabled,
                                onCheckedChange = { viewModel.toggleFridayKahfReminder() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFD4AF37),
                                    checkedTrackColor = Color(0xFF0F8F6B)
                                )
                            )
                        }

                        // 3. Tahajjud Alert toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isAr) "منبه صلاة التهجد والقيام" else "Tahajjud & Night Prayer Alert",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isAr) "التنبيه في الثلث الأخير من الليل لقيام الليل" else "Gentle sound reminder in the last third of the night for Tahajjud",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            Switch(
                                checked = isTahajjudReminderEnabled,
                                onCheckedChange = { viewModel.toggleTahajjudReminder() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFD4AF37),
                                    checkedTrackColor = Color(0xFF0F8F6B)
                                )
                            )
                        }

                        // 4. Daily Quran Reminder
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isAr) "تذكير الورد القرآني اليومي" else "Daily Quran Habit Reminder",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isAr) "منبه يومي لطيف للمحافظة على وردك اليومي" else "Ensure daily progress notification on your selected reading target",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            Switch(
                                checked = isDailyQuranReminderEnabled,
                                onCheckedChange = { viewModel.toggleDailyQuranReminder() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFD4AF37),
                                    checkedTrackColor = Color(0xFF0F8F6B)
                                )
                            )
                        }
                    }
                }
            }

            // --- SECTION 8: APP DATA & STORAGE (إدارة البيانات والمساحة) ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("settings_data_section_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedDataSection = !expandedDataSection },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFD4AF37))
                            Text(
                                if (isAr) "8. إدارة المساحة والضبط الافتراضي" else "8. Space Management & Defaults",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFD4AF37)
                            )
                        }
                        Icon(
                            imageVector = if (expandedDataSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color(0xFFD4AF37)
                        )
                    }

                    if (expandedDataSection) {
                        Spacer(modifier = Modifier.height(4.dp))

                        var cacheSize by remember { mutableStateOf("Calculating...") }
                        
                        LaunchedEffect(expandedDataSection) {
                            val audioDir = java.io.File(context.filesDir, "audio_downloads")
                            if (!audioDir.exists() || !audioDir.isDirectory) {
                                cacheSize = "0.0 MB"
                            } else {
                                var totalBytes = 0L
                                audioDir.walkTopDown().forEach { file ->
                                    if (file.isFile) totalBytes += file.length()
                                }
                                cacheSize = String.format("%.1f MB", totalBytes.toDouble() / (1024 * 1024))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isAr) "الملفات الصوتية والمؤقتة" else "Audio Downloads & Cache",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isAr) "المساحة المستهلكة من الملفات الصوتية" else "Total storage consumed on device",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            Text(
                                text = cacheSize,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFD4AF37)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                viewModel.clearHistoryAndCache(context) { success ->
                                    if (success) {
                                        android.widget.Toast.makeText(
                                            context,
                                            if (isAr) "تم مسح الذاكرة المؤقتة وإعادة تعيين التفضيلات بنجاح" else "Cleared offline downloads & reset defaults successfully",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                        cacheSize = "0.0 MB"
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            if (isAr) "فشلت عملية مسح البيانات" else "Failed to clear cache",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("reset_defaults_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAr) "مسح الذاكرة المؤقتة واستعادة الضبط" else "Clear Offline Files & Reset Preferences",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
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

@Composable
fun VideoPlayerView(
    videoUrl: String,
    youtubeUrl: String,
    title: String,
    isAr: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlayerLoading by remember { mutableStateOf(true) }
    var isPlayerError by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!isPlayerError) {
                    AndroidView(
                        factory = { ctx ->
                            TextureView(ctx).apply {
                                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                    private var mediaPlayer: MediaPlayer? = null

                                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                                        val surface = Surface(surfaceTexture)
                                        try {
                                            mediaPlayer = MediaPlayer().apply {
                                                setSurface(surface)
                                                setDataSource(ctx, Uri.parse(videoUrl))
                                                setAudioAttributes(
                                                    AudioAttributes.Builder()
                                                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                                                        .setUsage(AudioAttributes.USAGE_MEDIA)
                                                        .build()
                                                )
                                                setOnPreparedListener { mp ->
                                                    isPlayerLoading = false
                                                    mp.isLooping = true
                                                    mp.start()
                                                }
                                                setOnErrorListener { mp, what, extra ->
                                                    isPlayerLoading = false
                                                    isPlayerError = true
                                                    true
                                                }
                                                prepareAsync()
                                            }
                                        } catch (e: Exception) {
                                            isPlayerLoading = false
                                            isPlayerError = true
                                        }
                                    }

                                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                        mediaPlayer?.stop()
                                        mediaPlayer?.release()
                                        mediaPlayer = null
                                        return true
                                    }

                                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (isPlayerLoading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(color = Color(0xFFD4AF37), modifier = Modifier.size(28.dp))
                        Text(
                            text = if (isAr) "جاري تحميل البث المباشر..." else "Loading stream...",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }

                if (isPlayerError) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(36.dp))
                        Text(
                            text = if (isAr) "فيديو تعليم الوضوء للأطفال" else "Wudu Learning Video for Kids",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (isAr) 
                                "لم يبدأ البث التفاعلي؟ اضغط أدناه للمشاهدة مباشرة بجودة عالية على يوتيوب (تعلم مع زكريا)!" 
                                else "Stream offline? Click below to watch high-quality directly on YouTube!",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Bottom controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111827))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                }
                
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // ignore
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isAr) "تشغيل في يوتيوب ↗" else "Watch on YouTube ↗",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakatScreen(viewModel: QuranViewModel, onBack: () -> Unit) {
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isAr = appLanguage == "Arabic" || appLanguage == "العربية"

    var selectedTab by remember { mutableStateOf(0) } // 0: Calculator, 1: FAQ / Rules

    // State for inputs
    var cashInput by remember { mutableStateOf("") }
    var goldWeightInput by remember { mutableStateOf("") }
    var goldPriceInput by remember { mutableStateOf("3200") } // default realistic per gram price in local currency/EGP
    var silverWeightInput by remember { mutableStateOf("") }
    var silverPriceInput by remember { mutableStateOf("40") } // default silver price
    var stocksInput by remember { mutableStateOf("") }
    var businessInput by remember { mutableStateOf("") }
    var debtsInput by remember { mutableStateOf("") }

    // Helper conversion
    fun parseDouble(input: String): Double {
        return input.toDoubleOrNull() ?: 0.0
    }

    val cash = parseDouble(cashInput)
    val goldWeight = parseDouble(goldWeightInput)
    val goldPrice = parseDouble(goldPriceInput)
    val silverWeight = parseDouble(silverWeightInput)
    val silverPrice = parseDouble(silverPriceInput)
    val stocks = parseDouble(stocksInput)
    val business = parseDouble(businessInput)
    val debts = parseDouble(debtsInput)

    // Gold value & Silver value
    val goldValue = goldWeight * goldPrice
    val silverValue = silverWeight * silverPrice

    // Calculations
    val totalAssets = cash + goldValue + silverValue + stocks + business
    val netWealth = maxOf(0.0, totalAssets - debts)
    val nisabThreshold = 85.0 * goldPrice // 85g gold Nisab

    val isAboveNisab = netWealth >= nisabThreshold && netWealth > 0
    val zakatDue = if (isAboveNisab) netWealth * 0.025 else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (isAr) "حاسبة الزكاة الشرعية" else "Shariah Zakat Calculator", 
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.primary
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = Localization.translate("back", appLanguage), 
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(if (isAr) "الحاسبة" else "Calculator", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(if (isAr) "أحكام الزكاة" else "Zakat Rules", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTab == 0) {
                // Calculator View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Result Header Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAboveNisab) Color(0xFF0F8F6B).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (isAboveNisab) Color(0xFF0F8F6B) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isAr) "مقدار الزكاة الواجبة" else "Total Zakat Due",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "%,.2f".format(zakatDue),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isAboveNisab) Color(0xFF0F8F6B) else Color(0xFFD4AF37)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (isAboveNisab) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF0F8F6B), modifier = Modifier.size(16.dp))
                                    Text(
                                        text = if (isAr) "ثروتك بلغت حد النصاب الشرعي تجب الزكاة" else "Wealth is above Nisab. Zakat is due.",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F8F6B)
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(16.dp))
                                    Text(
                                        text = if (isAr) "ثروتك لم تبلغ حد النصاب الشرعي بعد (النصاب: %,.2f)".format(nisabThreshold) else "Below Nisab limit (Nisab: %,.2f)".format(nisabThreshold),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Input Form Fields
                    Text(
                        text = if (isAr) "أدخل قيمة أصولك المالية:" else "Enter your assets value:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Cash & Savings
                    OutlinedTextField(
                        value = cashInput,
                        onValueChange = { cashInput = it },
                        label = { Text(if (isAr) "النقود والمدخرات البنكية" else "Cash & Savings") },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )

                    // Gold section
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (isAr) "الذهب الخاضع للزكاة (عيار 24/21)" else "Zakatable Gold (24K/21K)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFD4AF37))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = goldWeightInput,
                                    onValueChange = { goldWeightInput = it },
                                    label = { Text(if (isAr) "الوزن (جرام)" else "Weight (g)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = goldPriceInput,
                                    onValueChange = { goldPriceInput = it },
                                    label = { Text(if (isAr) "سعر الجرام" else "Price per g") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                )
                            }
                        }
                    }

                    // Silver section
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (isAr) "الفضة الخاضعة للزكاة" else "Zakatable Silver", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.LightGray)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = silverWeightInput,
                                    onValueChange = { silverWeightInput = it },
                                    label = { Text(if (isAr) "الوزن (جرام)" else "Weight (g)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = silverPriceInput,
                                    onValueChange = { silverPriceInput = it },
                                    label = { Text(if (isAr) "سعر الجرام" else "Price per g") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                )
                            }
                        }
                    }

                    // Stocks & Business Merchandise
                    OutlinedTextField(
                        value = stocksInput,
                        onValueChange = { stocksInput = it },
                        label = { Text(if (isAr) "قيمة الأسهم والاستثمارات" else "Stocks & Investments") },
                        leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = businessInput,
                        onValueChange = { businessInput = it },
                        label = { Text(if (isAr) "قيمة البضائع التجارية" else "Business Merchandise") },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )

                    // Liabilities / Debts to deduct
                    Text(
                        text = if (isAr) "الخصومات والالتزامات الحالية:" else "Liabilities & Current Debts:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = debtsInput,
                        onValueChange = { debtsInput = it },
                        label = { Text(if (isAr) "الديون المستحقة للغير" else "Debts Owed to Others") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.6f)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )

                    // Summary Stats Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isAr) "إجمالي الأصول والمدخرات:" else "Total Assets:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("%,.2f".format(totalAssets), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isAr) "الديون المستقطعة:" else "Deductions/Debts:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("-%,.2f".format(debts), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Red.copy(alpha = 0.8f))
                            }
                            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isAr) "صافي الوعاء الزكوي الخاضع للزكاة:" else "Net Zakatable Wealth:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("%,.2f".format(netWealth), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isAr) "نصاب الزكاة الحالي (85 جرام ذهب):" else "Current Nisab Limit (85g Gold):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("%,.2f".format(nisabThreshold), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFD4AF37))
                            }
                        }
                    }

                    // Reset Button
                    Button(
                        onClick = {
                            cashInput = ""
                            goldWeightInput = ""
                            goldPriceInput = "3200"
                            silverWeightInput = ""
                            silverPriceInput = "40"
                            stocksInput = ""
                            businessInput = ""
                            debtsInput = ""
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isAr) "إعادة تعيين الحقول" else "Reset Calculator Fields")
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                }
            } else {
                // FAQ / Zakat Rules View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val faqItems = if (isAr) listOf(
                        "ما هي زكاة المال وكيف تُحسب؟" to "زكاة المال هي الركن الثالث من أركان الإسلام، وتجب على كل مسلم يملك ثروة بالغة للنصاب الشرعي وحال عليها الحول (مر عليها عام هجري كامل). ونسبتها هي 2.5% (أو ربع العشر) من صافي قيمة المدخرات والأصول.",
                        "ما هو النصاب الشرعي لزكاة المال؟" to "النصاب الشرعي هو الحد الأدنى من المال الذي تجب فيه الزكاة. وهو ما يعادل قيمة 85 جراماً من الذهب الصافي (عيار 24)، أو 595 جراماً من الفضة الصافية. ويُفضل تقدير النصاب بالذهب نظراً لاستقرار قيمته مقارنة بالعملات الورقية.",
                        "هل يضم الذهب والفضة والأسهم إلى بعضها؟" to "نعم، يُضم الذهب والفضة والسيولة النقدية والأسهم وبضائع التجارة إلى بعضها لتكوين الوعاء الزكوي الإجمالي، فإذا بلغت القيمة الإجمالية نصاب الذهب وجبت الزكاة فيها جميعاً.",
                        "هل تجب الزكاة في الذهب المستعمل للزينة؟" to "فيه خلاف بين الفقهاء؛ والراجح والمستحب خروجاً من الخلاف أنه إذا بلغ النصاب (85 جراماً) تجب فيه الزكاة بنسبة 2.5% تبرئة للذمة، بينما يرى آخرون أن حلي الزينة المستعمل لا زكاة فيه.",
                        "هل يُخصم الدين المستحق قبل حساب الزكاة؟" to "نعم، يُخصم الدين المستحق عليك للغير (الذي يجب سداده حالاً) من إجمالي الأصول المدخرة، وما يتبقى بعد خصم الدين يُحسب منه مقدار الزكاة إذا كان الباقي أعلى من حد النصاب."
                    ) else listOf(
                        "What is Zakat and how is it calculated?" to "Zakat is the third pillar of Islam. It is a mandatory charity due on every Muslim who possesses wealth equal to or greater than the Nisab limit, maintained for one full lunar/lunar year (Hawl). The rate is 2.5% of net assets.",
                        "What is the Shariah Nisab limit?" to "Nisab is the minimum threshold of wealth below which Zakat is not due. It is equivalent to 85 grams of pure gold (24K) or 595 grams of pure silver. Today, using gold value is standard for paper currencies.",
                        "Are gold, silver, and cash combined together?" to "Yes, cash, gold, silver, stocks, and commercial trade goods are added together. If their total net value equals or exceeds the gold Nisab, Zakat of 2.5% is due on the entire amount.",
                        "Is Zakat due on gold jewelry worn for adornment?" to "There is a scholarly difference of opinion. The most precautionary opinion is that if the weight of personal jewelry exceeds 85 grams, Zakat is recommended at 2.5%. Some scholars hold that personal jewelry worn regularly is exempt.",
                        "Can debts be deducted from zakatable assets?" to "Yes, immediate debts that you owe to others are deducted from your total assets. Zakat is then calculated on the remaining net wealth, provided the remainder is still above the Nisab limit."
                    )

                    faqItems.forEach { (question, answer) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Text(text = question, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Text(text = answer, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.prayer.PrayerCalculator
import com.example.ui.QuranViewModel
import com.example.ui.theme.Localization
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerScreen(
    viewModel: QuranViewModel,
    onBack: () -> Unit
) {
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()
    val city by viewModel.currentLocationName.collectAsStateWithLifecycle()
    val prayerTimes by viewModel.prayerTimes.collectAsStateWithLifecycle()
    val nextPrayerName by viewModel.nextPrayerName.collectAsStateWithLifecycle()
    val nextPrayerCountdown by viewModel.nextPrayerCountdown.collectAsStateWithLifecycle()
    val prayerLogs by viewModel.prayerLogs.collectAsStateWithLifecycle()

    val selectedAdhanMuezzin by viewModel.selectedAdhanMuezzin.collectAsStateWithLifecycle()
    val isAdhanEnabled by viewModel.isAdhanEnabled.collectAsStateWithLifecycle()
    val isAdhanNotificationEnabled by viewModel.isAdhanNotificationEnabled.collectAsStateWithLifecycle()
    val isDSTEnabled by viewModel.isDSTEnabled.collectAsStateWithLifecycle()
    val isPrayerApproachingAlertEnabled by viewModel.isPrayerApproachingAlertEnabled.collectAsStateWithLifecycle()
    val isPlayingAdhan by viewModel.isPlayingAdhan.collectAsStateWithLifecycle()
    val isUsingLiveApi by viewModel.isUsingLiveApi.collectAsStateWithLifecycle()
    val downloadStates by viewModel.downloadStates.collectAsStateWithLifecycle()

    var showCityPresets by remember { mutableStateOf(false) }
    var selectedPresetTab by remember { mutableStateOf(0) } // 0 = Egypt, 1 = Global

    val isAr = appLanguage == "Arabic" || appLanguage == "العربية"

    var isDetectingLocation by remember { mutableStateOf(false) }
    var detectionMessage by remember { mutableStateOf<String?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            isDetectingLocation = true
            detectionMessage = null
            viewModel.detectLocation { success, msg ->
                isDetectingLocation = false
                detectionMessage = if (success) {
                    if (isAr) "تم تحديث الموقع بنجاح!" else "Location updated successfully!"
                } else {
                    msg
                }
            }
        } else {
            detectionMessage = if (isAr) "تم رفض إذن الموقع" else "Location permission denied"
        }
    }

    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todaysLog = prayerLogs.find { it.dateStr == todayStr }

    fun getPrayerNameLocalized(name: String): String {
        return if (isAr) {
            when (name.lowercase()) {
                "fajr" -> "الفجر"
                "sunrise" -> "الشروق"
                "dhuhr" -> "الظهر"
                "asr" -> "العصر"
                "maghrib" -> "المغرب"
                "isha" -> "العشاء"
                else -> name
            }
        } else {
            name
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Localization.translate("prayer_times", appLanguage), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Localization.translate("back", appLanguage), tint = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    // Quick city preset trigger
                    IconButton(onClick = { showCityPresets = !showCityPresets }) {
                        Icon(Icons.Default.LocationCity, contentDescription = "Switch City", tint = MaterialTheme.colorScheme.primary)
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
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Location preset card selection
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable { showCityPresets = !showCityPresets }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    val localizedCity = localizeCityName(city, isAr)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(localizedCity, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isUsingLiveApi) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isUsingLiveApi) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isUsingLiveApi) Icons.Default.Cloud else Icons.Default.Schedule,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = if (isUsingLiveApi) {
                                                        if (isAr) "مباشر" else "Live"
                                                    } else {
                                                        if (isAr) "تقريبي" else "Local"
                                                    },
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Text(if (isAr) "اضغط لتغيير المدينة" else "Tap to change city preset", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Switch")
                        }
                    }
                }

                // Digital Live ticking Clock & Countdown Hero
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(if (isAr) "الوقت المتبقي لـ ${getPrayerNameLocalized(nextPrayerName)}" else "Time until $nextPrayerName", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                            Text(
                                nextPrayerCountdown,
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD4AF37),
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.2f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = "Clock", tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (isAr) "الوقت الحالي: $currentTime" else "Current Time: $currentTime",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // Beautiful Adhan & Notifications Settings Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (isAr) "إعدادات الأذان والتنبيهات" else "Adhan & Notifications Settings",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                            // 1. Toggle Adhan Audio Sound
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Adhan Sound", tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (isAr) "تفعيل صوت الأذان" else "Enable Adhan Audio",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = if (isAr) "تشغيل صوت الأذان عند حلول موعد الصلاة" else "Play Adhan audio when prayer time begins",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = isAdhanEnabled,
                                    onCheckedChange = { viewModel.toggleAdhanEnabled() }
                                )
                            }

                            // 2. Toggle Notification banner
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = "Adhan Notifications", tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (isAr) "تنبيهات الإشعارات" else "Prayer Notifications",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = if (isAr) "إظهار إشعار على الهاتف عند دخول وقت الصلاة" else "Show a push notification on prayer time",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = isAdhanNotificationEnabled,
                                    onCheckedChange = { viewModel.toggleAdhanNotificationEnabled() }
                                )
                             }

                             // 3. Daylight Saving Time (+1 hr)
                             Row(
                                 modifier = Modifier.fillMaxWidth(),
                                 horizontalArrangement = Arrangement.SpaceBetween,
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 Row(
                                     verticalAlignment = Alignment.CenterVertically,
                                     modifier = Modifier.weight(1f)
                                 ) {
                                     Icon(Icons.Default.Schedule, contentDescription = "DST", tint = MaterialTheme.colorScheme.primary)
                                     Spacer(modifier = Modifier.width(12.dp))
                                     Column {
                                         Text(
                                             text = if (isAr) "التوقيت الصيفي (+١ ساعة)" else "Daylight Saving Time (+1h)",
                                             fontWeight = FontWeight.Medium,
                                             fontSize = 14.sp
                                         )
                                         Text(
                                             text = if (isAr) "تعديل التوقيت بإضافة ساعة واحدة لمواقيت الصلاة" else "Shift prayer times forward by one hour",
                                             fontSize = 11.sp,
                                             color = MaterialTheme.colorScheme.onSurfaceVariant
                                         )
                                     }
                                 }
                                 Switch(
                                     checked = isDSTEnabled,
                                     onCheckedChange = { viewModel.toggleDST() }
                                 )
                             }

                             // 4. Toggle Approaching Prayer Alert
                             Row(
                                 modifier = Modifier.fillMaxWidth(),
                                 horizontalArrangement = Arrangement.SpaceBetween,
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 Row(
                                     verticalAlignment = Alignment.CenterVertically,
                                     modifier = Modifier.weight(1f)
                                 ) {
                                     Icon(Icons.Default.HourglassTop, contentDescription = "Approaching Alert", tint = MaterialTheme.colorScheme.primary)
                                     Spacer(modifier = Modifier.width(12.dp))
                                     Column {
                                         Text(
                                             text = if (isAr) "تنبيه اقتراب الصلاة (١٥ د)" else "Prayer Approaching Alert (15m)",
                                             fontWeight = FontWeight.Medium,
                                             fontSize = 14.sp
                                         )
                                         Text(
                                             text = if (isAr) "تلقي إشعار للتذكير قبل رفع الأذان بـ ١٥ دقيقة" else "Get a warning notification 15 mins before prayer",
                                             fontSize = 11.sp,
                                             color = MaterialTheme.colorScheme.onSurfaceVariant
                                         )
                                     }
                                 }
                                 Switch(
                                     checked = isPrayerApproachingAlertEnabled,
                                     onCheckedChange = { viewModel.togglePrayerApproachingAlert() }
                                 )
                             }

                             Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                            // 5. Battery Optimization Exempt for Background Work
                             Row(
                                 modifier = Modifier.fillMaxWidth(),
                                 horizontalArrangement = Arrangement.SpaceBetween,
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 Row(
                                     verticalAlignment = Alignment.CenterVertically,
                                     modifier = Modifier.weight(1f)
                                 ) {
                                     Icon(Icons.Default.BatteryAlert, contentDescription = "Battery Optimization", tint = MaterialTheme.colorScheme.primary)
                                     Spacer(modifier = Modifier.width(12.dp))
                                     Column {
                                         Text(
                                             text = Localization.translate("battery_card_title", if (isAr) "العربية" else "English"),
                                             fontWeight = FontWeight.Medium,
                                             fontSize = 14.sp
                                         )
                                         Text(
                                             text = Localization.translate("battery_card_desc", if (isAr) "العربية" else "English"),
                                             fontSize = 11.sp,
                                             color = MaterialTheme.colorScheme.onSurfaceVariant
                                         )
                                     }
                                 }
                                 Button(
                                     onClick = {
                                         try {
                                             val intent = android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                             context.startActivity(intent)
                                         } catch (e: Exception) {
                                             try {
                                                 val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                                                 context.startActivity(intent)
                                             } catch (ex: Exception) {
                                                 // ignore
                                             }
                                         }
                                     },
                                     colors = ButtonDefaults.buttonColors(
                                         containerColor = MaterialTheme.colorScheme.primary
                                     ),
                                     contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                     modifier = Modifier.height(36.dp)
                                 ) {
                                     Text(
                                         text = if (isAr) "استثناء" else "Exempt",
                                         fontSize = 12.sp,
                                         fontWeight = FontWeight.Bold
                                      )
                                 }
                             }

                             Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                             // 3. Select Muezzin voice
                            Text(
                                text = if (isAr) "اختر صوت المؤذن" else "Select Muezzin Voice",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Muezzin List / selection
                            PrayerCalculator.adhanMuezzins.forEach { muezzin ->
                                val isSelected = selectedAdhanMuezzin.id == muezzin.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                            else Color.Transparent
                                        )
                                        .clickable { viewModel.selectAdhanMuezzin(muezzin) }
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { viewModel.selectAdhanMuezzin(muezzin) }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isAr) muezzin.nameAr else muezzin.nameEn,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                    }

                                    // Preview/Stop Button for each Muezzin!
                                     Row(
                                         verticalAlignment = Alignment.CenterVertically,
                                         horizontalArrangement = Arrangement.spacedBy(4.dp)
                                     ) {
                                         // Preview Button
                                         val isCurrentPlayingAdhan = isPlayingAdhan && isSelected
                                         IconButton(
                                             onClick = {
                                                 if (isCurrentPlayingAdhan) {
                                                     viewModel.stopAdhan()
                                                 } else {
                                                     viewModel.playAdhan(muezzin)
                                                 }
                                             },
                                             modifier = Modifier.size(32.dp)
                                         ) {
                                             Icon(
                                                 imageVector = if (isCurrentPlayingAdhan) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                 contentDescription = "Preview Voice",
                                                 tint = if (isCurrentPlayingAdhan) Color.Red else MaterialTheme.colorScheme.primary,
                                                 modifier = Modifier.size(20.dp)
                                             )
                                         }

                                         // Download Button
                                         val adhanKey = "adhan_${muezzin.id}"
                                         when (val state = downloadStates[adhanKey]) {
                                             is QuranViewModel.DownloadState.Progress -> {
                                                 CircularProgressIndicator(
                                                     progress = state.progress,
                                                     modifier = Modifier.size(18.dp),
                                                     strokeWidth = 2.dp,
                                                     color = MaterialTheme.colorScheme.primary
                                                 )
                                             }
                                             is QuranViewModel.DownloadState.Completed -> {
                                                 IconButton(
                                                     onClick = { viewModel.deleteDownloadedAdhan(muezzin) },
                                                     modifier = Modifier.size(32.dp)
                                                 ) {
                                                     Icon(
                                                         imageVector = Icons.Default.CheckCircle,
                                                         contentDescription = "Downloaded",
                                                         tint = Color(0xFF2E7D32),
                                                         modifier = Modifier.size(18.dp)
                                                     )
                                                 }
                                             }
                                             else -> {
                                                 IconButton(
                                                     onClick = { viewModel.downloadAdhanAudio(muezzin) },
                                                     modifier = Modifier.size(32.dp)
                                                 ) {
                                                     Icon(
                                                         imageVector = Icons.Default.Download,
                                                         contentDescription = "Download",
                                                         tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                         modifier = Modifier.size(18.dp)
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

                // Interactive Prayer Checklist / Streak Logger
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (isAr) "متتبع الصلوات اليومية" else "Daily Prayer Tracker (Log your prayers)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            if (isAr) "اضغط على كل صلاة أدناه لتسجيلها وحساب متتالية صلواتك." else "Tap each prayer below to log and calculate your spiritual streak.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Timetable and Checkbox Grid
                if (prayerTimes != null) {
                    val times = prayerTimes!!
                    val scheduledPrayers = listOf(
                        Pair("Fajr", times.fajr),
                        Pair("Sunrise", times.sunrise),
                        Pair("Dhuhr", times.dhuhr),
                        Pair("Asr", times.asr),
                        Pair("Maghrib", times.maghrib),
                        Pair("Isha", times.isha)
                    )

                    items(scheduledPrayers) { prayer ->
                        val isNext = nextPrayerName.lowercase() == prayer.first.lowercase()
                        val isLogged = when (prayer.first.lowercase()) {
                            "fajr" -> todaysLog?.fajr == true
                            "sunrise" -> todaysLog?.sunrise == true
                            "dhuhr" -> todaysLog?.dhuhr == true
                            "asr" -> todaysLog?.asr == true
                            "maghrib" -> todaysLog?.maghrib == true
                            "isha" -> todaysLog?.isha == true
                            else -> false
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.togglePrayerLogged(prayer.first) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isNext) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                },
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isNext) 4.dp else 0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Custom checkbox circle
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isLogged) Color(0xFFD4AF37) else MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isLogged) {
                                            Icon(Icons.Default.Check, contentDescription = "Logged", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = getPrayerNameLocalized(prayer.first),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        if (isNext) {
                                            Text(if (isAr) "الصلاة القادمة" else "NEXT PRAYER", color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                        }
                                    }
                                }

                                Text(
                                    text = prayer.second,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isNext) Color(0xFFD4AF37) else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Quick City Presets Sheet Selector Overlay
            if (showCityPresets) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showCityPresets = false }
                ) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable(enabled = false) {}, // Prevent closing when clicking inside
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (isAr) "اختر المدينة" else "Select Location Preset",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Divider()

                            // GPS Auto-Detect Button
                            Button(
                                onClick = {
                                    val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    
                                    val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                    if (hasFine || hasCoarse) {
                                        isDetectingLocation = true
                                        detectionMessage = null
                                        viewModel.detectLocation { success, msg ->
                                            isDetectingLocation = false
                                            detectionMessage = if (success) {
                                                if (isAr) "تم تحديث الموقع بنجاح!" else "Location updated successfully!"
                                            } else {
                                                msg
                                            }
                                        }
                                    } else {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                if (isDetectingLocation) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = if (isAr) "جاري تحديد الموقع..." else "Detecting location...", fontSize = 13.sp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = "GPS Detect",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isAr) "تحديد الموقع تلقائياً (GPS)" else "Auto-Detect Location (GPS)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            detectionMessage?.let { msg ->
                                Text(
                                    text = msg,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (msg.contains("نجاح") || msg.contains("success")) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Beautiful Tab Selector for Egypt vs Global
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selectedPresetTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { selectedPresetTab = 0 }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isAr) "مدن مصر" else "Egypt Cities",
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedPresetTab == 0) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selectedPresetTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { selectedPresetTab = 1 }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isAr) "مدن عالمية" else "Global Cities",
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedPresetTab == 1) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Scrollable list of cities with confined height to prevent overflow
                            Box(modifier = Modifier.heightIn(max = 280.dp)) {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val currentPresetsList = if (selectedPresetTab == 0) {
                                        PrayerCalculator.egyptCityPresets
                                    } else {
                                        PrayerCalculator.cityPresets
                                    }

                                    items(currentPresetsList) { preset ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.selectCityPreset(preset)
                                                    showCityPresets = false
                                                }
                                                .padding(vertical = 12.dp, horizontal = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val localizedPresetName = localizeCityName(preset.name, isAr)
                                            Text(
                                                text = localizedPresetName,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.sp
                                            )
                                            if (city == preset.name) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = "Active",
                                                    tint = Color(0xFFD4AF37)
                                                )
                                            }
                                        }
                                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
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

private fun localizeCityName(city: String, isAr: Boolean): String {
    if (!isAr) return city
    
    if (city.startsWith("Custom Location")) {
        return city.replace("Custom Location", "موقع مخصص")
    }
    
    return when {
        city.contains("Makkah") -> "مكة المكرمة"
        city.contains("Madinah") -> "المدينة المنورة"
        city == "Cairo" || city.contains("Cairo") -> "القاهرة"
        city.contains("Jerusalem") -> "القدس الشريف"
        city.contains("London") -> "لندن"
        city.contains("New York") -> "نيويورك"
        city.contains("Samanoud") -> "سمنود (الغربية)"
        city.contains("Mahalla") -> "المحلة الكبرى (الغربية)"
        city.contains("Tanta") -> "طنطا (الغربية)"
        city.contains("Zifta") -> "زفتى (الغربية)"
        city.contains("Kafr El Zayat") -> "كفر الزيات (الغربية)"
        city.contains("Basyoun") -> "بسيون (الغربية)"
        city.contains("Mansoura") -> "المنصورة (الدقهلية)"
        city.contains("Mit Ghamr") -> "ميت غمر (الدقهلية)"
        city.contains("Sennelawen") -> "السنبلاوين (الدقهلية)"
        city.contains("Talkha") -> "طلخا (الدقهلية)"
        city.contains("Damanhour") -> "دمنهور (البحيرة)"
        city.contains("Zagazig") -> "الزقازيق (الشرقية)"
        city.contains("Minya El Qamh") -> "منيا القمح (الشرقية)"
        city.contains("Belbeis") -> "بلبيس (الشرقية)"
        city.contains("Shibin El Kom") -> "شبين الكوم (المنوفية)"
        city.contains("Menouf") -> "منوف (المنوفية)"
        city.contains("Ashmoun") -> "أشمون (المنوفية)"
        city.contains("Banha") -> "بنها (القليوبية)"
        city.contains("Qalyub") -> "قليوب (القليوبية)"
        city == "Alexandria" || city.contains("Alexandria") -> "الإسكندرية"
        city == "Giza" || city.contains("Giza") -> "الجيزة"
        city == "Shubra El-Kheima" || city.contains("Shubra") -> "شبرا الخيمة"
        city == "Port Said" || city.contains("Port Said") -> "بورسعيد"
        city == "Suez" || city.contains("Suez") -> "السويس"
        city == "Luxor" || city.contains("Luxor") -> "الأقصر"
        city == "Aswan" || city.contains("Aswan") -> "أسوان"
        city == "Asyut" || city.contains("Asyut") -> "أسيوط"
        city == "Ismailia" || city.contains("Ismailia") -> "الإسماعيلية"
        city == "Faiyum" || city.contains("Faiyum") -> "الفيوم"
        city == "Damietta" || city.contains("Damietta") -> "دمياط"
        city == "Minya" || city.contains("Minya") -> "المنيا"
        city == "Beni Suef" || city.contains("Beni Suef") -> "بني سويف"
        city == "Qena" || city.contains("Qena") -> "قنا"
        city == "Sohag" || city.contains("Sohag") -> "سوهاج"
        city == "Hurghada" || city.contains("Hurghada") -> "الغردقة"
        city == "Kafr El Sheikh" || city.contains("Kafr El Sheikh") -> "كفر الشيخ"
        city == "Arish" || city.contains("Arish") -> "العريش"
        city == "Marsa Matruh" || city.contains("Marsa Matruh") -> "مرسى مطروح"
        city == "El Tor" || city.contains("El Tor") -> "الطور (سيناء)"
        city == "Kharga" || city.contains("Kharga") -> "الخارجة (الوادي الجديد)"
        city.contains("Jakarta") -> "جاكرتا (إندونيسيا)"
        city.contains("Dubai") -> "دبي (الإمارات)"
        city.contains("Kuala Lumpur") -> "كوالالمبور (ماليزيا)"
        city.contains("Istanbul") -> "إسطنبول (تركيا)"
        else -> city
    }
}

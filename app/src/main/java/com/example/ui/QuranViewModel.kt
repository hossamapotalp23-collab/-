package com.example.ui

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiService
import com.example.data.database.*
import com.example.data.prayer.PrayerCalculator
import com.example.data.quran.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class QuranViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val db = AppDatabase.getDatabase(application)
    private val bookmarkDao = db.bookmarkDao()
    private val khatmahDao = db.khatmahDao()
    private val zikrDao = db.zikrDao()
    private val memorizationDao = db.memorizationDao()
    private val prayerDao = db.prayerDao()

    // --- Compass / Qibla Variables ---
    private var sensorManager: SensorManager? = null
    private var rotationSensor: Sensor? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private var gravity = FloatArray(3)
    private var geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    // --- State flows for reactive updates ---
    val bookmarks: StateFlow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeKhatmah: StateFlow<KhatmahPlanEntity?> = khatmahDao.getActivePlan()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val zikrCounters: StateFlow<List<ZikrCounterEntity>> = zikrDao.getAllCounters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quizScores: StateFlow<List<MemorizationScoreEntity>> = memorizationDao.getAllScores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val prayerLogs: StateFlow<List<PrayerLogEntity>> = prayerDao.getRecentLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Live System States ---
    private val prefs = application.getSharedPreferences("noor_quran_prefs", Context.MODE_PRIVATE)

    private val _currentTime = MutableStateFlow("")
    val currentTime = _currentTime.asStateFlow()

    private val _currentLocationName = MutableStateFlow(prefs.getString("selected_city_name", "Cairo") ?: "Cairo")
    val currentLocationName = _currentLocationName.asStateFlow()

    private val _currentLatitude = MutableStateFlow(prefs.getFloat("selected_city_lat", 30.0444f).toDouble())
    val currentLatitude = _currentLatitude.asStateFlow()

    private val _currentLongitude = MutableStateFlow(prefs.getFloat("selected_city_lng", 31.2357f).toDouble())
    val currentLongitude = _currentLongitude.asStateFlow()

    private val _currentCalculationMethod = MutableStateFlow(PrayerCalculator.CalculationMethod.EGYPT)
    val currentCalculationMethod = _currentCalculationMethod.asStateFlow()

    private val _prayerTimes = MutableStateFlow<PrayerCalculator.PrayerTimes?>(null)
    val prayerTimes = _prayerTimes.asStateFlow()

    private val _nextPrayerName = MutableStateFlow("")
    val nextPrayerName = _nextPrayerName.asStateFlow()

    private val _nextPrayerCountdown = MutableStateFlow("")
    val nextPrayerCountdown = _nextPrayerCountdown.asStateFlow()

    // --- Adhan & Notification States ---
    private val _selectedAdhanMuezzin = MutableStateFlow(
        PrayerCalculator.adhanMuezzins.firstOrNull { it.id == prefs.getString("selected_adhan_muezzin_id", "makkah") }
            ?: PrayerCalculator.adhanMuezzins[0]
    )
    val selectedAdhanMuezzin = _selectedAdhanMuezzin.asStateFlow()

    private val _isAdhanEnabled = MutableStateFlow(prefs.getBoolean("is_adhan_enabled", true))
    val isAdhanEnabled = _isAdhanEnabled.asStateFlow()

    private val _isAdhanNotificationEnabled = MutableStateFlow(prefs.getBoolean("is_adhan_notification_enabled", true))
    val isAdhanNotificationEnabled = _isAdhanNotificationEnabled.asStateFlow()

    private val _isPlayingAdhan = MutableStateFlow(false)
    val isPlayingAdhan = _isPlayingAdhan.asStateFlow()

    private var adhanMediaPlayer: android.media.MediaPlayer? = null

    // --- UI State Variables ---
    // Quran UI
    private val _selectedSurahId = MutableStateFlow(1)
    val selectedSurahId = _selectedSurahId.asStateFlow()

    private val _selectedSurah = MutableStateFlow<Surah?>(null)
    val selectedSurah = _selectedSurah.asStateFlow()

    private val _quranSearchQuery = MutableStateFlow("")
    val quranSearchQuery = _quranSearchQuery.asStateFlow()

    private val _selectedArabicFont = MutableStateFlow("Uthmani")
    val selectedArabicFont = _selectedArabicFont.asStateFlow()

    private val _quranFontSize = MutableStateFlow(24f)
    val quranFontSize = _quranFontSize.asStateFlow()

    // Audio Reciter State
    private val _selectedReciter = MutableStateFlow(QuranDataset.reciters[0])
    val selectedReciter = _selectedReciter.asStateFlow()

    private val _isPlayingAudio = MutableStateFlow(false)
    val isPlayingAudio = _isPlayingAudio.asStateFlow()

    private val _currentPlayingAyah = MutableStateFlow<Ayah?>(null)
    val currentPlayingAyah = _currentPlayingAyah.asStateFlow()

    private val _audioPlaybackSpeed = MutableStateFlow(1.0f)
    val audioPlaybackSpeed = _audioPlaybackSpeed.asStateFlow()

    private val _sleepTimerSeconds = MutableStateFlow(0) // 0 = off
    val sleepTimerSeconds = _sleepTimerSeconds.asStateFlow()

    // Compass Orientation
    private val _compassAzimuth = MutableStateFlow(0f)
    val compassAzimuth = _compassAzimuth.asStateFlow()

    private val _qiblaBearing = MutableStateFlow(0.0)
    val qiblaBearing = _qiblaBearing.asStateFlow()

    // Memorization Test
    private val _quizQuestion = MutableStateFlow<QuizQuestion?>(null)
    val quizQuestion = _quizQuestion.asStateFlow()

    private val _quizAnswerChecked = MutableStateFlow(false)
    val quizAnswerChecked = _quizAnswerChecked.asStateFlow()

    private val _quizSelectedAnswerIndex = MutableStateFlow(-1)
    val quizSelectedAnswerIndex = _quizSelectedAnswerIndex.asStateFlow()

    private val _quizStreak = MutableStateFlow(0)
    val quizStreak = _quizStreak.asStateFlow()

    // Tasbeeh
    private val _tasbeehCount = MutableStateFlow(0)
    val tasbeehCount = _tasbeehCount.asStateFlow()

    // AI Chat
    private val _aiChatMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("Peace be upon you! I am Noor Al-Quran AI, your dedicated Islamic scholar. How can I assist you in exploring the Quran or your faith today?", false)
    ))
    val aiChatMessages = _aiChatMessages.asStateFlow()

    private val _aiThinking = MutableStateFlow(false)
    val aiThinking = _aiThinking.asStateFlow()

    // Settings
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode = _isDarkMode.asStateFlow()

    private val _appLanguage = MutableStateFlow("English")
    val appLanguage = _appLanguage.asStateFlow()

    data class QuizQuestion(
        val type: String,
        val text: String,
        val options: List<String>,
        val correctAnswerIndex: Int,
        val details: String
    )

    data class ChatMessage(
        val text: String,
        val isUser: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    )

    init {
        // Load default Surah
        loadSurahContent(1)

        // Initialize Sensor Manager for Qibla Compass
        val sensorContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            application.applicationContext.createAttributionContext("qibla")
        } else {
            application.applicationContext
        }
        sensorManager = sensorContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        if (sensorManager != null) {
            rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            if (rotationSensor == null) {
                accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            }
        }

        // Register Sensor Listeners
        registerCompassSensors()

        // Start Clock & Timer Loop
        viewModelScope.launch {
            while (true) {
                val cal = Calendar.getInstance()
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                _currentTime.value = sdf.format(cal.time)

                // Update Prayer Times calculations dynamically
                recalculatePrayerTimes(cal)

                // Check and trigger Adhan at prayer time
                checkAndTriggerAdhan(cal)

                // Sleep Timer decrement
                val currentSleep = _sleepTimerSeconds.value
                if (currentSleep > 0) {
                    _sleepTimerSeconds.value = currentSleep - 1
                    if (_sleepTimerSeconds.value == 0) {
                        _isPlayingAudio.value = false
                    }
                }

                delay(1000)
            }
        }

        // Insert default Khatmah plan if empty
        viewModelScope.launch {
            activeKhatmah.collect { plan ->
                if (plan == null) {
                    val cal = Calendar.getInstance()
                    val start = cal.timeInMillis
                    cal.add(Calendar.DAY_OF_YEAR, 30)
                    khatmahDao.insertPlan(
                        KhatmahPlanEntity(
                            title = "Ramadan Khatmah Plan",
                            startDate = start,
                            endDate = cal.timeInMillis,
                            targetDays = 30,
                            pagesRead = 120,
                            totalPages = 604,
                            dailyMinutes = 20
                        )
                    )
                }
            }
        }

        // Prepare Azkar counters
        viewModelScope.launch {
            zikrCounters.collect { counters ->
                if (counters.isEmpty()) {
                    QuranDataset.allAzkar.forEach { zikr ->
                        zikrDao.upsertCounter(
                            ZikrCounterEntity(
                                zikrId = zikr.id,
                                count = 0,
                                maxCount = zikr.repeatCount,
                                isFavorite = false
                            )
                        )
                    }
                }
            }
        }
    }

    // --- Compass Sensors ---
    fun registerCompassSensors() {
        if (rotationSensor != null) {
            sensorManager?.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            sensorManager?.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun unregisterCompassSensors() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            _compassAzimuth.value = Math.toDegrees(orientation[0].toDouble()).toFloat()
        } else {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, gravity, 0, event.values.size)
                hasGravity = true
            } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, geomagnetic, 0, event.values.size)
                hasGeomagnetic = true
            }

            if (hasGravity && hasGeomagnetic) {
                val r = FloatArray(9)
                val i = FloatArray(9)
                if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(r, orientation)
                    _compassAzimuth.value = Math.toDegrees(orientation[0].toDouble()).toFloat()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }

    override fun onCleared() {
        super.onCleared()
        unregisterCompassSensors()
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("QuranViewModel", "Error releasing MediaPlayer: ${e.message}")
        }
    }

    // --- Core State Modifiers ---
    fun loadSurahContent(surahId: Int) {
        viewModelScope.launch {
            _selectedSurahId.value = surahId
            _selectedSurah.value = QuranDataset.getSurahContent(surahId)
        }
    }

    fun searchQuran(query: String) {
        _quranSearchQuery.value = query
    }

    fun setArabicFont(font: String) {
        _selectedArabicFont.value = font
    }

    fun setFontSize(size: Float) {
        _quranFontSize.value = size
    }

    // Bookmarks
    fun toggleBookmark(surahId: Int, ayahId: Int) {
        viewModelScope.launch {
            val isBookmarked = bookmarks.value.any { it.surahNumber == surahId && it.ayahNumber == ayahId }
            if (isBookmarked) {
                bookmarkDao.deleteBookmark(surahId, ayahId)
            } else {
                val surahHeader = QuranDataset.allSurahHeaders.firstOrNull { it.id == surahId }
                val surahContent = QuranDataset.getSurahContent(surahId)
                val ayah = surahContent.verses.firstOrNull { it.number == ayahId }
                if (ayah != null && surahHeader != null) {
                    bookmarkDao.insertBookmark(
                        BookmarkEntity(
                            surahNumber = surahId,
                            ayahNumber = ayahId,
                            surahName = surahHeader.name,
                            arabicText = ayah.textArabic,
                            translationText = ayah.textTranslation
                        )
                    )
                }
            }
        }
    }

    // Audio Playback
    private var mediaPlayer: android.media.MediaPlayer? = null
    private var currentLoadedUrl: String? = null

    fun selectReciter(reciter: Reciter) {
        _selectedReciter.value = reciter
        // If playing, restart with new reciter
        if (_isPlayingAudio.value) {
            _isPlayingAudio.value = false
            toggleAudioPlayback()
        }
    }

    fun toggleAudioPlayback(ayah: Ayah? = null) {
        val surahId = _selectedSurahId.value
        val formattedSurahId = String.format("%03d", surahId)
        val reciter = _selectedReciter.value
        val audioUrl = "${reciter.audioBaseUrl}$formattedSurahId.mp3"

        if (ayah != null) {
            _currentPlayingAyah.value = ayah
        } else if (_currentPlayingAyah.value == null && _selectedSurah.value != null) {
            _currentPlayingAyah.value = _selectedSurah.value?.verses?.firstOrNull()
        }

        if (_isPlayingAudio.value) {
            _isPlayingAudio.value = false
            try {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                }
            } catch (e: Exception) {
                Log.e("QuranViewModel", "Error pausing MediaPlayer: ${e.message}")
            }
        } else {
            _isPlayingAudio.value = true
            if (currentLoadedUrl == audioUrl && mediaPlayer != null) {
                try {
                    mediaPlayer?.start()
                } catch (e: Exception) {
                    Log.e("QuranViewModel", "Error starting existing MediaPlayer, re-preparing: ${e.message}")
                    playAudioStream(audioUrl)
                }
            } else {
                playAudioStream(audioUrl)
            }
        }
    }

    private fun playAudioStream(url: String) {
        currentLoadedUrl = url
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val baseContext = getApplication<Application>().applicationContext
                val context = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    baseContext.createAttributionContext("audio_playback")
                } else {
                    baseContext
                }

                if (mediaPlayer == null) {
                    mediaPlayer = android.media.MediaPlayer().apply {
                        setAudioStreamType(android.media.AudioManager.STREAM_MUSIC)
                    }
                } else {
                    mediaPlayer?.reset()
                }

                mediaPlayer?.apply {
                    setDataSource(context, android.net.Uri.parse(url))
                    prepareAsync()
                    setOnPreparedListener { mp ->
                        mp.start()
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            try {
                                val params = mp.playbackParams
                                params.speed = _audioPlaybackSpeed.value
                                mp.playbackParams = params
                            } catch (e: Exception) {
                                Log.e("QuranViewModel", "Error setting speed: ${e.message}")
                            }
                        }
                    }
                    setOnCompletionListener {
                        _isPlayingAudio.value = false
                        _currentPlayingAyah.value = null
                    }
                    setOnErrorListener { _, _, _ ->
                        _isPlayingAudio.value = false
                        _currentPlayingAyah.value = null
                        true
                    }
                }
            } catch (e: Exception) {
                Log.e("QuranViewModel", "Error in playAudioStream: ${e.message}")
                _isPlayingAudio.value = false
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _audioPlaybackSpeed.value = speed
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        val params = mp.playbackParams
                        params.speed = speed
                        mp.playbackParams = params
                    }
                }
            } catch (e: Exception) {
                Log.e("QuranViewModel", "Error changing playback speed: ${e.message}")
            }
        }
    }

    fun setSleepTimer(minutes: Int) {
        _sleepTimerSeconds.value = minutes * 60
    }

    // Prayer & City Presets
    fun selectCityPreset(city: PrayerCalculator.CityPreset) {
        _currentLocationName.value = city.name
        _currentLatitude.value = city.latitude
        _currentLongitude.value = city.longitude
        prefs.edit()
            .putString("selected_city_name", city.name)
            .putFloat("selected_city_lat", city.latitude.toFloat())
            .putFloat("selected_city_lng", city.longitude.toFloat())
            .apply()
        recalculatePrayerTimes(Calendar.getInstance())
    }

    fun detectLocation(onResult: (Boolean, String) -> Unit) {
        val baseContext = getApplication<Application>().applicationContext
        val context = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            baseContext.createAttributionContext("prayer_times")
        } else {
            baseContext
        }
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        if (locationManager == null) {
            onResult(false, "Location Manager not available")
            return
        }

        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            onResult(false, "Permission Denied")
            return
        }

        try {
            val providers = locationManager.getProviders(true)
            var bestLocation: android.location.Location? = null

            for (provider in providers) {
                val loc = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                    bestLocation = loc
                }
            }

            if (bestLocation != null) {
                applyLocation(bestLocation.latitude, bestLocation.longitude)
                onResult(true, "Successfully updated")
            } else {
                val provider = if (locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                    android.location.LocationManager.NETWORK_PROVIDER
                } else if (locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                    android.location.LocationManager.GPS_PROVIDER
                } else {
                    null
                }

                if (provider != null) {
                    locationManager.requestSingleUpdate(provider, object : android.location.LocationListener {
                        override fun onLocationChanged(location: android.location.Location) {
                            applyLocation(location.latitude, location.longitude)
                            onResult(true, "Successfully updated")
                        }
                        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }, android.os.Looper.getMainLooper())
                } else {
                    onResult(false, "No active location providers found. Please enable GPS/Location in device settings.")
                }
            }
        } catch (e: SecurityException) {
            onResult(false, "Permission error: ${e.message}")
        } catch (e: Exception) {
            onResult(false, "Error: ${e.message}")
        }
    }

    private fun applyLocation(lat: Double, lng: Double) {
        val allPresets = PrayerCalculator.egyptCityPresets + PrayerCalculator.cityPresets
        var nearestPreset = allPresets[0]
        var minDistance = Double.MAX_VALUE

        for (preset in allPresets) {
            val dist = calculateDistance(lat, lng, preset.latitude, preset.longitude)
            if (dist < minDistance) {
                minDistance = dist
                nearestPreset = preset
            }
        }

        // If closer than ~20 km, we name it the nearest preset city, otherwise we use custom lat/lng coordinates
        val name = if (minDistance < 20.0) {
            nearestPreset.name
        } else {
            String.format(Locale.US, "Custom Location (%.4f, %.4f)", lat, lng)
        }

        _currentLocationName.value = name
        _currentLatitude.value = lat
        _currentLongitude.value = lng

        prefs.edit()
            .putString("selected_city_name", name)
            .putFloat("selected_city_lat", lat.toFloat())
            .putFloat("selected_city_lng", lng.toFloat())
            .apply()

        recalculatePrayerTimes(Calendar.getInstance())
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return 6371 * c
    }

    fun setCalculationMethod(method: PrayerCalculator.CalculationMethod) {
        _currentCalculationMethod.value = method
        recalculatePrayerTimes(Calendar.getInstance())
    }

    private fun recalculatePrayerTimes(cal: Calendar) {
        val lat = _currentLatitude.value
        val lng = _currentLongitude.value
        val method = _currentCalculationMethod.value

        // Calculate Qibla bearing
        _qiblaBearing.value = PrayerCalculator.calculateQiblaDirection(lat, lng)

        // Set timezone
        val preset = (PrayerCalculator.cityPresets + PrayerCalculator.egyptCityPresets).firstOrNull { it.latitude == lat && it.longitude == lng }
        if (preset != null) {
            cal.timeZone = TimeZone.getTimeZone(preset.timezoneId)
        }

        val times = PrayerCalculator.calculatePrayerTimes(lat, lng, cal, method)
        _prayerTimes.value = times

        // Highlight next prayer and calculate countdown
        val nowH = cal.get(Calendar.HOUR_OF_DAY)
        val nowM = cal.get(Calendar.MINUTE)
        val nowSeconds = nowH * 3600 + nowM * 60 + cal.get(Calendar.SECOND)

        fun parseTimeToSeconds(timeStr: String): Int {
            val parts = timeStr.split(":")
            if (parts.size == 2) {
                return parts[0].toInt() * 3600 + parts[1].toInt() * 60
            }
            return 0
        }

        val fajrS = parseTimeToSeconds(times.fajr)
        val sunriseS = parseTimeToSeconds(times.sunrise)
        val dhuhrS = parseTimeToSeconds(times.dhuhr)
        val asrS = parseTimeToSeconds(times.asr)
        val maghribS = parseTimeToSeconds(times.maghrib)
        val ishaS = parseTimeToSeconds(times.isha)

        val next = when {
            nowSeconds < fajrS -> Pair("Fajr", fajrS)
            nowSeconds < sunriseS -> Pair("Sunrise", sunriseS)
            nowSeconds < dhuhrS -> Pair("Dhuhr", dhuhrS)
            nowSeconds < asrS -> Pair("Asr", asrS)
            nowSeconds < maghribS -> Pair("Maghrib", maghribS)
            nowSeconds < ishaS -> Pair("Isha", ishaS)
            else -> Pair("Fajr", fajrS + 24 * 3600) // Next day's Fajr
        }

        _nextPrayerName.value = next.first
        val diffS = next.second - nowSeconds
        val h = diffS / 3600
        val m = (diffS % 3600) / 60
        val s = diffS % 60
        _nextPrayerCountdown.value = String.format("%02d:%02d:%02d", h, m, s)
    }

    // Log Daily Prayer
    fun togglePrayerLogged(prayerName: String) {
        viewModelScope.launch {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val existing = prayerLogs.value.firstOrNull { it.dateStr == todayStr } ?: PrayerLogEntity(todayStr)

            val updated = when (prayerName.lowercase()) {
                "fajr" -> existing.copy(fajr = !existing.fajr)
                "sunrise" -> existing.copy(sunrise = !existing.sunrise)
                "dhuhr" -> existing.copy(dhuhr = !existing.dhuhr)
                "asr" -> existing.copy(asr = !existing.asr)
                "maghrib" -> existing.copy(maghrib = !existing.maghrib)
                "isha" -> existing.copy(isha = !existing.isha)
                else -> existing
            }

            prayerDao.upsertLog(updated)
        }
    }

    // Azkar Counters
    fun incrementZikr(id: String) {
        viewModelScope.launch {
            val counter = zikrCounters.value.firstOrNull { it.zikrId == id } ?: ZikrCounterEntity(id)
            if (counter.count < counter.maxCount) {
                zikrDao.updateCount(id, counter.count + 1)
            } else {
                // Reset on complete
                zikrDao.updateCount(id, 0)
            }
        }
    }

    fun resetZikr(id: String) {
        viewModelScope.launch {
            zikrDao.updateCount(id, 0)
        }
    }

    fun toggleZikrFavorite(id: String) {
        viewModelScope.launch {
            val counter = zikrCounters.value.firstOrNull { it.zikrId == id } ?: ZikrCounterEntity(id)
            zikrDao.toggleFavorite(id, !counter.isFavorite)
        }
    }

    // Tasbeeh
    fun incrementTasbeeh() {
        _tasbeehCount.value = (_tasbeehCount.value + 1) % 100
    }

    fun resetTasbeeh() {
        _tasbeehCount.value = 0
    }

    // Khatmah Planner
    fun logKhatmahPagesRead(pages: Int) {
        viewModelScope.launch {
            val current = activeKhatmah.value ?: return@launch
            val newRead = (current.pagesRead + pages).coerceAtMost(current.totalPages)
            val isCompleted = newRead >= current.totalPages
            khatmahDao.updatePlan(current.copy(pagesRead = newRead, isCompleted = isCompleted))
        }
    }

    fun createNewKhatmah(title: String, days: Int, targetMinutes: Int) {
        viewModelScope.launch {
            khatmahDao.deleteAllPlans()
            val start = System.currentTimeMillis()
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, days)
            khatmahDao.insertPlan(
                KhatmahPlanEntity(
                    title = title,
                    startDate = start,
                    endDate = cal.timeInMillis,
                    targetDays = days,
                    pagesRead = 0,
                    totalPages = 604,
                    dailyMinutes = targetMinutes
                )
            )
        }
    }

    // Memorization Test
    fun generateNextQuiz() {
        // Generate a random quiz question from special Surahs
        val quizTypes = listOf("Complete the Verse", "Guess the Surah", "Guess the Ayah Number")
        val randomType = quizTypes.random()
        val surahIds = listOf(1, 108, 112, 113, 114)
        val selectedSurahId = surahIds.random()
        val surah = QuranDataset.getSurahContent(selectedSurahId)
        val verses = surah.verses

        if (verses.isEmpty()) return

        val targetVerse = verses.random()

        val question: QuizQuestion = when (randomType) {
            "Complete the Verse" -> {
                val fullText = targetVerse.textArabic
                val words = fullText.split(" ")
                if (words.size > 2) {
                    val splitIndex = words.size / 2
                    val promptText = words.subList(0, splitIndex).joinToString(" ") + " ..."
                    val correctAnswer = words.subList(splitIndex, words.size).joinToString(" ")
                    
                    val options = mutableListOf(correctAnswer)
                    while (options.size < 4) {
                        val otherVerse = verses.random()
                        val otherWords = otherVerse.textArabic.split(" ")
                        if (otherVerse.number != targetVerse.number && otherWords.size > 2) {
                            val chunk = otherWords.subList(otherWords.size / 2, otherWords.size).joinToString(" ")
                            if (chunk !in options) options.add(chunk)
                        } else {
                            options.add("وَاللَّهُ عَلِيمٌ بِالذَّاتِ الصُّدُورِ")
                        }
                    }
                    options.shuffle()

                    QuizQuestion(
                        type = "Complete the Verse",
                        text = "Complete this verse from Surah ${surah.header.name}:\n\n\"$promptText\"",
                        options = options,
                        correctAnswerIndex = options.indexOf(correctAnswer),
                        details = "Full Ayah: \"$fullText\""
                    )
                } else {
                    // Fallback to choose next verse
                    generateChooseNextVerse(surah, targetVerse)
                }
            }
            "Guess the Surah" -> {
                val options = listOf("Al-Fatihah", "Al-Ikhlas", "Al-Falaq", "An-Nas").shuffled()
                val correctIndex = options.indexOf(surah.header.name).coerceAtLeast(0)
                val finalOptions = if (surah.header.name !in options) {
                    val m = options.toMutableList()
                    m[0] = surah.header.name
                    m.shuffle()
                    m
                } else options

                QuizQuestion(
                    type = "Guess the Surah",
                    text = "Which Surah contains this verse?\n\n\"${targetVerse.textArabic}\"",
                    options = finalOptions,
                    correctAnswerIndex = finalOptions.indexOf(surah.header.name),
                    details = "This is Verse ${targetVerse.number} of Surah ${surah.header.name} (${surah.header.translation})."
                )
            }
            else -> {
                generateChooseNextVerse(surah, targetVerse)
            }
        }

        _quizQuestion.value = question
        _quizAnswerChecked.value = false
        _quizSelectedAnswerIndex.value = -1
    }

    private fun generateChooseNextVerse(surah: Surah, targetVerse: Ayah): QuizQuestion {
        val nextVerse = surah.verses.firstOrNull { it.number == targetVerse.number + 1 }
        val correctAnswer = nextVerse?.textArabic ?: "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
        
        val options = mutableListOf(correctAnswer)
        while (options.size < 4) {
            val randomV = surah.verses.random()
            if (randomV.number != targetVerse.number + 1 && randomV.textArabic !in options) {
                options.add(randomV.textArabic)
            } else {
                options.add("إِنَّ اللَّهَ مَعَ الصَّابِرِينَ")
            }
        }
        options.shuffle()

        return QuizQuestion(
            type = "What is the Next Verse?",
            text = "What is the next verse after:\n\n\"${targetVerse.textArabic}\"\n(Surah ${surah.header.name})",
            options = options,
            correctAnswerIndex = options.indexOf(correctAnswer),
            details = "Next Verse is indeed: \"$correctAnswer\""
        )
    }

    fun selectQuizAnswer(index: Int) {
        if (_quizAnswerChecked.value) return
        _quizSelectedAnswerIndex.value = index
    }

    fun submitQuizAnswer() {
        val question = _quizQuestion.value ?: return
        val selected = _quizSelectedAnswerIndex.value
        if (selected == -1) return

        _quizAnswerChecked.value = true
        val isCorrect = (selected == question.correctAnswerIndex)

        if (isCorrect) {
            _quizStreak.value += 1
        } else {
            _quizStreak.value = 0
        }

        viewModelScope.launch {
            memorizationDao.insertScore(
                MemorizationScoreEntity(
                    quizType = question.type,
                    score = if (isCorrect) 1 else 0,
                    total = 1,
                    accuracy = if (isCorrect) 100.0 else 0.0
                )
            )
        }
    }

    // AI chat assistant
    fun sendAiMessage(promptText: String) {
        if (promptText.isBlank()) return
        val currentList = _aiChatMessages.value.toMutableList()
        currentList.add(ChatMessage(promptText, true))
        _aiChatMessages.value = currentList
        _aiThinking.value = true

        viewModelScope.launch {
            val response = GeminiService.askAssistant(promptText)
            val updatedList = _aiChatMessages.value.toMutableList()
            updatedList.add(ChatMessage(response, false))
            _aiChatMessages.value = updatedList
            _aiThinking.value = false
        }
    }

    fun clearAiChat() {
        _aiChatMessages.value = listOf(
            ChatMessage("Peace be upon you! I am Noor Al-Quran AI, your dedicated Islamic scholar. How can I assist you in exploring the Quran or your faith today?", false)
        )
    }

    // Settings modifiers
    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setLanguage(lang: String) {
        _appLanguage.value = lang
    }

    // --- Adhan & Notification Playback ---
    fun playAdhan(muezzin: PrayerCalculator.AdhanMuezzin) {
        stopAdhan()
        _isPlayingAdhan.value = true
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val baseContext = getApplication<Application>().applicationContext
                val context = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    baseContext.createAttributionContext("prayer_times")
                } else {
                    baseContext
                }

                adhanMediaPlayer = android.media.MediaPlayer().apply {
                    setDataSource(context, android.net.Uri.parse(muezzin.audioUrl))
                    setOnPreparedListener { mp ->
                        mp.start()
                    }
                    setOnCompletionListener {
                        _isPlayingAdhan.value = false
                        it.release()
                        adhanMediaPlayer = null
                    }
                    setOnErrorListener { _, _, _ ->
                        _isPlayingAdhan.value = false
                        adhanMediaPlayer = null
                        false
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                Log.e("QuranViewModel", "Error playing Adhan: ${e.message}")
                _isPlayingAdhan.value = false
            }
        }
    }

    fun stopAdhan() {
        try {
            adhanMediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.release()
            }
            adhanMediaPlayer = null
        } catch (e: Exception) {
            Log.e("QuranViewModel", "Error stopping Adhan: ${e.message}")
        } finally {
            _isPlayingAdhan.value = false
        }
    }

    fun selectAdhanMuezzin(muezzin: PrayerCalculator.AdhanMuezzin) {
        _selectedAdhanMuezzin.value = muezzin
        prefs.edit().putString("selected_adhan_muezzin_id", muezzin.id).apply()
    }

    fun toggleAdhanEnabled() {
        val newVal = !_isAdhanEnabled.value
        _isAdhanEnabled.value = newVal
        prefs.edit().putBoolean("is_adhan_enabled", newVal).apply()
    }

    fun toggleAdhanNotificationEnabled() {
        val newVal = !_isAdhanNotificationEnabled.value
        _isAdhanNotificationEnabled.value = newVal
        prefs.edit().putBoolean("is_adhan_notification_enabled", newVal).apply()
    }

    private var lastTriggeredAdhanKey = ""

    private fun checkAndTriggerAdhan(cal: Calendar) {
        val times = _prayerTimes.value ?: return
        
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = sdfDate.format(cal.time)
        
        val nowH = cal.get(Calendar.HOUR_OF_DAY)
        val nowM = cal.get(Calendar.MINUTE)
        val currentTimeStr = String.format("%02d:%02d", nowH, nowM)
        
        val prayersToCheck = listOf(
            Pair("Fajr", times.fajr),
            Pair("Dhuhr", times.dhuhr),
            Pair("Asr", times.asr),
            Pair("Maghrib", times.maghrib),
            Pair("Isha", times.isha)
        )
        
        for (prayer in prayersToCheck) {
            val prayerName = prayer.first
            val prayerTime = prayer.second
            
            if (currentTimeStr == prayerTime) {
                val triggerKey = "${prayerName}_${dateStr}_${prayerTime}"
                if (lastTriggeredAdhanKey != triggerKey) {
                    lastTriggeredAdhanKey = triggerKey
                    
                    if (_isAdhanNotificationEnabled.value) {
                        sendPrayerNotification(prayerName)
                    }
                    
                    if (_isAdhanEnabled.value) {
                        playAdhan(_selectedAdhanMuezzin.value)
                    }
                }
                break
            }
        }
    }

    private fun sendPrayerNotification(prayerName: String) {
        val baseContext = getApplication<Application>().applicationContext
        val context = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            baseContext.createAttributionContext("prayer_times")
        } else {
            baseContext
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        val channelId = "adhan_notifications"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Adhan & Prayer Times",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications and Adhan alerts for Islamic prayer times"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val isAr = _appLanguage.value == "Arabic" || _appLanguage.value == "العربية"
        val localizedPrayer = if (isAr) {
            when (prayerName.lowercase()) {
                "fajr" -> "الفجر"
                "sunrise" -> "الشروق"
                "dhuhr" -> "الظهر"
                "asr" -> "العصر"
                "maghrib" -> "المغرب"
                "isha" -> "العشاء"
                else -> prayerName
            }
        } else {
            prayerName
        }

        val title = if (isAr) "حان الآن موعد صلاة $localizedPrayer" else "It is time for $localizedPrayer prayer"
        val text = if (isAr) "تنبيه الأذان بصوت الشيخ ${_selectedAdhanMuezzin.value.nameAr}" else "Adhan alert reciting by Sheikh ${_selectedAdhanMuezzin.value.nameEn}"

        val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}

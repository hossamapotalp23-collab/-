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
import com.example.data.api.QuranApiService
import com.example.data.api.PrayerApiService
import com.example.data.database.*
import com.example.data.prayer.PrayerCalculator
import com.example.data.quran.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

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

    // --- Google Authentication States & Setup ---
    private val _currentUser = MutableStateFlow<GoogleUser?>(null)
    val currentUser = _currentUser.asStateFlow()

    fun loginUser(user: GoogleUser) {
        _currentUser.value = user
        prefs.edit()
            .putString("user_google_id", user.id)
            .putString("user_email", user.email)
            .putString("user_display_name", user.displayName)
            .putString("user_photo_url", user.photoUrl)
            .apply()
    }

    fun logoutUser() {
        _currentUser.value = null
        prefs.edit()
            .remove("user_google_id")
            .remove("user_email")
            .remove("user_display_name")
            .remove("user_photo_url")
            .apply()
    }

    // --- State flows for reactive updates ---
    val bookmarks: StateFlow<List<BookmarkEntity>> = combine(bookmarkDao.getAllBookmarks(), currentUser) { list, user ->
        list.filter { it.userId == user?.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeKhatmah: StateFlow<KhatmahPlanEntity?> = combine(khatmahDao.getActivePlan(), currentUser) { plan, user ->
        if (plan?.userId == user?.id) plan else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val zikrCounters: StateFlow<List<ZikrCounterEntity>> = combine(zikrDao.getAllCounters(), currentUser) { list, user ->
        list.filter { it.userId == (user?.id ?: "") }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quizScores: StateFlow<List<MemorizationScoreEntity>> = combine(memorizationDao.getAllScores(), currentUser) { list, user ->
        list.filter { it.userId == user?.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val prayerLogs: StateFlow<List<PrayerLogEntity>> = combine(prayerDao.getRecentLogs(), currentUser) { list, user ->
        list.filter { it.userId == (user?.id ?: "") }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    private val _isUsingLiveApi = MutableStateFlow(false)
    val isUsingLiveApi = _isUsingLiveApi.asStateFlow()

    private val _hijriDateString = MutableStateFlow(PrayerCalculator.getHijriDate())
    val hijriDateString = _hijriDateString.asStateFlow()

    private var apiPrayerTimes: PrayerCalculator.PrayerTimes? = null
    private var apiLat: Double? = null
    private var apiLng: Double? = null
    private var apiMethod: PrayerCalculator.CalculationMethod? = null
    private var apiDateStr: String? = null
    private var isFetchingLiveTimes = false

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

    // --- DST & Feedback Preferences ---
    private val _isDSTEnabled = MutableStateFlow(prefs.getBoolean("is_dst_enabled", false))
    val isDSTEnabled = _isDSTEnabled.asStateFlow()

    private val _isPrayerApproachingAlertEnabled = MutableStateFlow(prefs.getBoolean("is_prayer_approaching_alert_enabled", true))
    val isPrayerApproachingAlertEnabled = _isPrayerApproachingAlertEnabled.asStateFlow()

    private val _isTasbeehSoundEnabled = MutableStateFlow(prefs.getBoolean("is_tasbeeh_sound_enabled", true))
    val isTasbeehSoundEnabled = _isTasbeehSoundEnabled.asStateFlow()

    private val _isTasbeehVibrationEnabled = MutableStateFlow(prefs.getBoolean("is_tasbeeh_vibration_enabled", true))
    val isTasbeehVibrationEnabled = _isTasbeehVibrationEnabled.asStateFlow()

    // --- Audio Downloading States ---
    sealed class DownloadState {
        object Idle : DownloadState()
        data class Progress(val progress: Float) : DownloadState()
        object Completed : DownloadState()
        data class Error(val message: String) : DownloadState()
    }

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates = _downloadStates.asStateFlow()

    private val _isPlayingAdhan = MutableStateFlow(false)
    val isPlayingAdhan = _isPlayingAdhan.asStateFlow()

    private var adhanMediaPlayer: android.media.MediaPlayer? = null

    // --- UI State Variables ---
    // Quran UI
    private val _selectedSurahId = MutableStateFlow(1)
    val selectedSurahId = _selectedSurahId.asStateFlow()

    private val _selectedSurah = MutableStateFlow<Surah?>(null)
    val selectedSurah = _selectedSurah.asStateFlow()

    private val _isLoadingRealSurah = MutableStateFlow(false)
    val isLoadingRealSurah = _isLoadingRealSurah.asStateFlow()

    private val _quranSearchQuery = MutableStateFlow("")
    val quranSearchQuery = _quranSearchQuery.asStateFlow()

    private val _selectedArabicFont = MutableStateFlow(prefs.getString("selected_arabic_font", "Uthmani") ?: "Uthmani")
    val selectedArabicFont = _selectedArabicFont.asStateFlow()

    private val _quranFontSize = MutableStateFlow(prefs.getFloat("quran_font_size", 24f))
    val quranFontSize = _quranFontSize.asStateFlow()

    // Audio Reciter State
    private val _selectedReciter = MutableStateFlow(QuranDataset.reciters[0])
    val selectedReciter = _selectedReciter.asStateFlow()

    private val _isPlayingAudio = MutableStateFlow(false)
    val isPlayingAudio = _isPlayingAudio.asStateFlow()

    // Live Radio State
    private val _isPlayingRadio = MutableStateFlow(false)
    val isPlayingRadio = _isPlayingRadio.asStateFlow()

    private val _currentPlayingRadioUrl = MutableStateFlow<String?>(null)
    val currentPlayingRadioUrl = _currentPlayingRadioUrl.asStateFlow()

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
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val isDarkMode = _isDarkMode.asStateFlow()

    private val _appLanguage = MutableStateFlow(prefs.getString("app_language", "Arabic") ?: "Arabic")
    val appLanguage = _appLanguage.asStateFlow()

    private val _appTheme = MutableStateFlow(prefs.getString("app_theme", "Emerald") ?: "Emerald")
    val appTheme = _appTheme.asStateFlow()

    private val _isArabicFontFixed = MutableStateFlow(prefs.getBoolean("is_arabic_font_fixed", false))
    val isArabicFontFixed = _isArabicFontFixed.asStateFlow()

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
        // Load persistent Google Sign-In state on launch
        val savedGoogleId = prefs.getString("user_google_id", null)
        if (savedGoogleId != null) {
            _currentUser.value = GoogleUser(
                id = savedGoogleId,
                email = prefs.getString("user_email", "") ?: "",
                displayName = prefs.getString("user_display_name", null),
                photoUrl = prefs.getString("user_photo_url", null)
            )
        }

        // Load default Surah
        loadSurahContent(1)

        // Scan downloaded audio files
        viewModelScope.launch(Dispatchers.IO) {
            val context = application.applicationContext
            val downloadDir = File(context.filesDir, "audio_downloads")
            val initialMap = mutableMapOf<String, DownloadState>()
            if (downloadDir.exists() && downloadDir.isDirectory) {
                downloadDir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".mp3")) {
                        val nameWithoutExt = file.name.substringBeforeLast(".")
                        val parts = nameWithoutExt.split("_")
                        if (parts.size == 2) {
                            val part0 = parts[0]
                            if (part0 == "adhan") {
                                val muezzinId = parts[1]
                                initialMap["adhan_$muezzinId"] = DownloadState.Completed
                            } else {
                                val surahId = parts[1].toIntOrNull() ?: 0
                                initialMap["${part0}_$surahId"] = DownloadState.Completed
                            }
                        }
                    }
                }
            }
            _downloadStates.value = initialMap
        }

        // Initialize Sensor Manager for Qibla Compass
        val baseSensorContext = application.applicationContext
        val sensorContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            baseSensorContext.createAttributionContext("qibla")
        } else {
            baseSensorContext
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
                val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
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
                            dailyMinutes = 20,
                            userId = currentUser.value?.id
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
                                isFavorite = false,
                                userId = currentUser.value?.id ?: ""
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
            // Load local backup instantly for responsiveness
            val localSurah = QuranDataset.getSurahContent(surahId)
            _selectedSurah.value = localSurah

            // If it's not one of the pre-compiled local surahs, fetch the real, authentic text
            val specialSurahIds = listOf(1, 108, 112, 113, 114)
            if (surahId !in specialSurahIds) {
                _isLoadingRealSurah.value = true
                try {
                    val realSurah = QuranApiService.fetchSurah(surahId)
                    if (realSurah != null && realSurah.verses.isNotEmpty()) {
                        _selectedSurah.value = realSurah
                    }
                } catch (e: Exception) {
                    Log.e("QuranViewModel", "Error fetching authentic surah $surahId", e)
                } finally {
                    _isLoadingRealSurah.value = false
                }
            }
        }
    }

    fun searchQuran(query: String) {
        _quranSearchQuery.value = query
    }

    fun setArabicFont(font: String) {
        _selectedArabicFont.value = font
        prefs.edit().putString("selected_arabic_font", font).apply()
    }

    fun setFontSize(size: Float) {
        _quranFontSize.value = size
        prefs.edit().putFloat("quran_font_size", size).apply()
    }

    // Bookmarks
    fun toggleBookmark(surahId: Int, ayahId: Int) {
        viewModelScope.launch {
            val bookmark = bookmarks.value.firstOrNull { it.surahNumber == surahId && it.ayahNumber == ayahId }
            if (bookmark != null) {
                bookmarkDao.deleteBookmarkEntity(bookmark)
            } else {
                val surahHeader = QuranDataset.allSurahHeaders.firstOrNull { it.id == surahId }
                // Use currently loaded authentic surah if it matches the requested ID, otherwise use backup
                val currentSurah = _selectedSurah.value
                val surahContent = if (currentSurah != null && currentSurah.header.id == surahId) {
                    currentSurah
                } else {
                    QuranDataset.getSurahContent(surahId)
                }
                val ayah = surahContent.verses.firstOrNull { it.number == ayahId }
                if (ayah != null && surahHeader != null) {
                    bookmarkDao.insertBookmark(
                        BookmarkEntity(
                            surahNumber = surahId,
                            ayahNumber = ayahId,
                            surahName = surahHeader.name,
                            arabicText = ayah.textArabic,
                            translationText = ayah.textTranslation,
                            userId = currentUser.value?.id
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
        
        val context = getApplication<Application>().applicationContext
        val localFile = File(File(context.filesDir, "audio_downloads"), "${reciter.id}_${formattedSurahId}.mp3")
        val audioUrl = if (localFile.exists()) {
            localFile.absolutePath
        } else {
            "${reciter.audioBaseUrl}$formattedSurahId.mp3"
        }

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
            // Stop live radio if it was playing
            if (_isPlayingRadio.value) {
                stopRadio()
            }
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
                    val uri = if (url.startsWith("/")) {
                        android.net.Uri.fromFile(java.io.File(url))
                    } else {
                        android.net.Uri.parse(url)
                    }
                    setDataSource(context, uri)
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
                        _isPlayingRadio.value = false
                        _currentPlayingAyah.value = null
                    }
                    setOnErrorListener { _, _, _ ->
                        _isPlayingAudio.value = false
                        _isPlayingRadio.value = false
                        _currentPlayingAyah.value = null
                        true
                    }
                }
            } catch (e: Exception) {
                Log.e("QuranViewModel", "Error in playAudioStream: ${e.message}")
                _isPlayingAudio.value = false
                _isPlayingRadio.value = false
            }
        }
    }

    fun playRadio(url: String) {
        // Stop any currently playing Quran surah audio
        if (_isPlayingAudio.value) {
            _isPlayingAudio.value = false
            try {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                }
            } catch (e: Exception) {
                Log.e("QuranViewModel", "Error pausing MediaPlayer: ${e.message}")
            }
        }

        _isPlayingRadio.value = true
        _currentPlayingRadioUrl.value = url
        playAudioStream(url)
    }

    fun stopRadio() {
        _isPlayingRadio.value = false
        _currentPlayingRadioUrl.value = null
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.reset()
        } catch (e: Exception) {
            Log.e("QuranViewModel", "Error stopping radio: ${e.message}")
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

    private fun addOneHourToTime(timeStr: String): String {
        try {
            val parts = timeStr.split(":")
            if (parts.size == 2) {
                val hours = (parts[0].toInt() + 1) % 24
                return String.format("%02d:%s", hours, parts[1])
            }
        } catch (e: Exception) {
            Log.e("QuranViewModel", "Error adjusting DST: ${e.message}")
        }
        return timeStr
    }

    private fun adjustTimesForDST(times: PrayerCalculator.PrayerTimes): PrayerCalculator.PrayerTimes {
        if (!_isDSTEnabled.value) return times
        return PrayerCalculator.PrayerTimes(
            fajr = addOneHourToTime(times.fajr),
            sunrise = addOneHourToTime(times.sunrise),
            dhuhr = addOneHourToTime(times.dhuhr),
            asr = addOneHourToTime(times.asr),
            maghrib = addOneHourToTime(times.maghrib),
            isha = addOneHourToTime(times.isha)
        )
    }

    private fun setAdjustedPrayerTimes(times: PrayerCalculator.PrayerTimes?) {
        _prayerTimes.value = times?.let { adjustTimesForDST(it) }
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

        val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(cal.time)
        val isCacheValid = apiPrayerTimes != null &&
                apiLat == lat &&
                apiLng == lng &&
                apiMethod == method &&
                apiDateStr == dateStr

        if (isCacheValid) {
            _isUsingLiveApi.value = true
            setAdjustedPrayerTimes(apiPrayerTimes)
        } else {
            // Use local calculation as initial/fallback
            _isUsingLiveApi.value = false
            val localTimes = PrayerCalculator.calculatePrayerTimes(lat, lng, cal, method)
            setAdjustedPrayerTimes(localTimes)
            _hijriDateString.value = PrayerCalculator.getHijriDate()

            if (!isFetchingLiveTimes) {
                isFetchingLiveTimes = true
                viewModelScope.launch {
                    try {
                        val methodId = when (method) {
                            PrayerCalculator.CalculationMethod.KARACHI -> 1
                            PrayerCalculator.CalculationMethod.ISNA -> 2
                            PrayerCalculator.CalculationMethod.MUSLIM_WORLD_LEAGUE -> 3
                            PrayerCalculator.CalculationMethod.UMM_AL_QURA -> 4
                            PrayerCalculator.CalculationMethod.EGYPT -> 5
                        }
                        val response = PrayerApiService.fetchPrayerTimes(lat, lng, methodId, dateStr)
                        if (response != null) {
                            apiPrayerTimes = response.times
                            apiLat = lat
                            apiLng = lng
                            apiMethod = method
                            apiDateStr = dateStr
                            setAdjustedPrayerTimes(response.times)
                            _isUsingLiveApi.value = true
                            if (response.hijriDate.isNotEmpty()) {
                                _hijriDateString.value = response.hijriDate
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("QuranViewModel", "Error fetching live prayer times: ${e.message}")
                    } finally {
                        isFetchingLiveTimes = false
                    }
                }
            }
        }

        val times = _prayerTimes.value ?: adjustTimesForDST(PrayerCalculator.calculatePrayerTimes(lat, lng, cal, method))

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
            val existing = prayerLogs.value.firstOrNull { it.dateStr == todayStr } ?: PrayerLogEntity(dateStr = todayStr, userId = currentUser.value?.id ?: "")

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
            val user = currentUser.value?.id ?: ""
            val counter = zikrCounters.value.firstOrNull { it.zikrId == id } ?: ZikrCounterEntity(zikrId = id, userId = user)
            if (counter.count < counter.maxCount) {
                zikrDao.updateCount(id, user, counter.count + 1)
            } else {
                // Reset on complete
                zikrDao.updateCount(id, user, 0)
            }
        }
    }

    fun resetZikr(id: String) {
        viewModelScope.launch {
            val user = currentUser.value?.id ?: ""
            zikrDao.updateCount(id, user, 0)
        }
    }

    fun toggleZikrFavorite(id: String) {
        viewModelScope.launch {
            val user = currentUser.value?.id ?: ""
            val counter = zikrCounters.value.firstOrNull { it.zikrId == id } ?: ZikrCounterEntity(zikrId = id, userId = user)
            zikrDao.toggleFavorite(id, user, !counter.isFavorite)
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
                    dailyMinutes = targetMinutes,
                    userId = currentUser.value?.id
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
                    accuracy = if (isCorrect) 100.0 else 0.0,
                    userId = currentUser.value?.id
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
        val newVal = !_isDarkMode.value
        _isDarkMode.value = newVal
        prefs.edit().putBoolean("is_dark_mode", newVal).apply()
    }

    fun setLanguage(lang: String) {
        _appLanguage.value = lang
        prefs.edit().putString("app_language", lang).apply()
    }

    fun setAppTheme(theme: String) {
        _appTheme.value = theme
        prefs.edit().putString("app_theme", theme).apply()
        if (theme == "Sand") {
            _isDarkMode.value = false
            prefs.edit().putBoolean("is_dark_mode", false).apply()
        } else {
            _isDarkMode.value = true
            prefs.edit().putBoolean("is_dark_mode", true).apply()
        }
    }

    fun toggleArabicFontFixed() {
        val newVal = !_isArabicFontFixed.value
        _isArabicFontFixed.value = newVal
        prefs.edit().putBoolean("is_arabic_font_fixed", newVal).apply()
        if (newVal) {
            _selectedArabicFont.value = "Simple Arabic"
            prefs.edit().putString("selected_arabic_font", "Simple Arabic").apply()
        } else {
            _selectedArabicFont.value = "Uthmani"
            prefs.edit().putString("selected_arabic_font", "Uthmani").apply()
        }
    }

    // --- Adhan & Notification Playback ---
    fun playAdhan(muezzin: PrayerCalculator.AdhanMuezzin) {
        stopAdhan()
        _isPlayingAdhan.value = true
        viewModelScope.launch(Dispatchers.Main) {
            try {
                adhanMediaPlayer = android.media.MediaPlayer().apply {
                    val context = getApplication<Application>().applicationContext
                    val localFile = File(File(context.filesDir, "audio_downloads"), "adhan_${muezzin.id}.mp3")
                    if (localFile.exists()) {
                        setDataSource(localFile.absolutePath)
                    } else {
                        setDataSource(muezzin.audioUrl)
                    }
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

    // --- New Settings & Custom Preferences Modifiers ---
    fun toggleDST() {
        val newVal = !_isDSTEnabled.value
        _isDSTEnabled.value = newVal
        prefs.edit().putBoolean("is_dst_enabled", newVal).apply()
        recalculatePrayerTimes(Calendar.getInstance())
    }

    fun togglePrayerApproachingAlert() {
        val newVal = !_isPrayerApproachingAlertEnabled.value
        _isPrayerApproachingAlertEnabled.value = newVal
        prefs.edit().putBoolean("is_prayer_approaching_alert_enabled", newVal).apply()
    }

    fun toggleTasbeehSound() {
        val newVal = !_isTasbeehSoundEnabled.value
        _isTasbeehSoundEnabled.value = newVal
        prefs.edit().putBoolean("is_tasbeeh_sound_enabled", newVal).apply()
    }

    fun toggleTasbeehVibration() {
        val newVal = !_isTasbeehVibrationEnabled.value
        _isTasbeehVibrationEnabled.value = newVal
        prefs.edit().putBoolean("is_tasbeeh_vibration_enabled", newVal).apply()
    }

    // --- Audio Downloading Engine ---
    fun downloadSurahAudio(reciter: Reciter, surahId: Int) {
        val key = "${reciter.id}_$surahId"
        _downloadStates.value = _downloadStates.value + (key to DownloadState.Progress(0f))
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val formattedSurahId = String.format("%03d", surahId)
                val urlString = "${reciter.audioBaseUrl}$formattedSurahId.mp3"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.connect()
                
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("Server returned code ${connection.responseCode}")
                }
                
                val fileLength = connection.contentLength
                val context = getApplication<Application>().applicationContext
                val downloadDir = File(context.filesDir, "audio_downloads")
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }
                val tempFile = File(downloadDir, "${reciter.id}_${formattedSurahId}.tmp")
                val targetFile = File(downloadDir, "${reciter.id}_${formattedSurahId}.mp3")
                
                connection.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val data = ByteArray(4096)
                        var total: Long = 0
                        var count: Int
                        while (input.read(data).also { count = it } != -1) {
                            total += count
                            if (fileLength > 0) {
                                val progress = total.toFloat() / fileLength
                                _downloadStates.value = _downloadStates.value + (key to DownloadState.Progress(progress))
                            }
                            output.write(data, 0, count)
                        }
                    }
                }
                
                if (tempFile.renameTo(targetFile)) {
                    _downloadStates.value = _downloadStates.value + (key to DownloadState.Completed)
                } else {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                    _downloadStates.value = _downloadStates.value + (key to DownloadState.Completed)
                }
            } catch (e: Exception) {
                Log.e("QuranViewModel", "Error downloading surah: ${e.message}", e)
                _downloadStates.value = _downloadStates.value + (key to DownloadState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun deleteDownloadedSurah(reciter: Reciter, surahId: Int) {
        val key = "${reciter.id}_$surahId"
        viewModelScope.launch(Dispatchers.IO) {
            val formattedSurahId = String.format("%03d", surahId)
            val context = getApplication<Application>().applicationContext
            val targetFile = File(File(context.filesDir, "audio_downloads"), "${reciter.id}_${formattedSurahId}.mp3")
            if (targetFile.exists()) {
                targetFile.delete()
            }
            _downloadStates.value = _downloadStates.value - key
        }
    }

    fun downloadAdhanAudio(muezzin: PrayerCalculator.AdhanMuezzin) {
        val key = "adhan_${muezzin.id}"
        _downloadStates.value = _downloadStates.value + (key to DownloadState.Progress(0f))
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL(muezzin.audioUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.connect()
                
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("Server returned code ${connection.responseCode}")
                }
                
                val fileLength = connection.contentLength
                val context = getApplication<Application>().applicationContext
                val downloadDir = File(context.filesDir, "audio_downloads")
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }
                val tempFile = File(downloadDir, "adhan_${muezzin.id}.tmp")
                val targetFile = File(downloadDir, "adhan_${muezzin.id}.mp3")
                
                connection.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val data = ByteArray(4096)
                        var total: Long = 0
                        var count: Int
                        while (input.read(data).also { count = it } != -1) {
                            total += count
                            if (fileLength > 0) {
                                val progress = total.toFloat() / fileLength
                                _downloadStates.value = _downloadStates.value + (key to DownloadState.Progress(progress))
                            }
                            output.write(data, 0, count)
                        }
                    }
                }
                
                if (tempFile.renameTo(targetFile)) {
                    _downloadStates.value = _downloadStates.value + (key to DownloadState.Completed)
                } else {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                    _downloadStates.value = _downloadStates.value + (key to DownloadState.Completed)
                }
            } catch (e: Exception) {
                Log.e("QuranViewModel", "Error downloading adhan: ${e.message}", e)
                _downloadStates.value = _downloadStates.value + (key to DownloadState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun deleteDownloadedAdhan(muezzin: PrayerCalculator.AdhanMuezzin) {
        val key = "adhan_${muezzin.id}"
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            val targetFile = File(File(context.filesDir, "audio_downloads"), "adhan_${muezzin.id}.mp3")
            if (targetFile.exists()) {
                targetFile.delete()
            }
            _downloadStates.value = _downloadStates.value - key
        }
    }

    private var lastTriggeredAdhanKey = ""
    private var lastTriggeredApproachKey = ""

    private fun parseTimeToMinutes(timeStr: String): Int {
        try {
            val parts = timeStr.split(":")
            if (parts.size == 2) {
                return parts[0].toInt() * 60 + parts[1].toInt()
            }
        } catch (e: Exception) {
            Log.e("QuranViewModel", "Error parsing time string: $timeStr")
        }
        return -1
    }

    private fun checkAndTriggerAdhan(cal: Calendar) {
        val times = _prayerTimes.value ?: return
        
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = sdfDate.format(cal.time)
        
        val nowH = cal.get(Calendar.HOUR_OF_DAY)
        val nowM = cal.get(Calendar.MINUTE)
        val currentTimeInMinutes = nowH * 60 + nowM
        
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
            val prayerTimeInMinutes = parseTimeToMinutes(prayerTime)
            if (prayerTimeInMinutes < 0) continue
            
            // 1. Exact Prayer Time
            if (currentTimeInMinutes == prayerTimeInMinutes) {
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
            }
            
            // 2. Approaching Prayer Time (15 minutes before)
            val diff = prayerTimeInMinutes - currentTimeInMinutes
            if (diff == 15) {
                val approachKey = "${prayerName}_${dateStr}_approach"
                if (lastTriggeredApproachKey != approachKey) {
                    lastTriggeredApproachKey = approachKey
                    
                    if (_isAdhanNotificationEnabled.value && _isPrayerApproachingAlertEnabled.value) {
                        sendApproachingPrayerNotification(prayerName)
                    }
                }
            }
        }
    }

    private fun sendPrayerNotification(prayerName: String) {
        val context = getApplication<Application>().applicationContext
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

    private fun sendApproachingPrayerNotification(prayerName: String) {
        val context = getApplication<Application>().applicationContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        val channelId = "prayer_approach_notifications"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Prayer Approaching Alerts",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts warning you 15 minutes before the next prayer time"
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

        val title = if (isAr) "اقتربت صلاة $localizedPrayer" else "$localizedPrayer prayer is approaching"
        val text = if (isAr) "متبقي ١٥ دقيقة على رفع الأذان، تهيأ للصلاة." else "15 minutes remaining until the Adhan, prepare for prayer."

        val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt() + 1000, builder.build())
    }
}

data class GoogleUser(
    val id: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?
)

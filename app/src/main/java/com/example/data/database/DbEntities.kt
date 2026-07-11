package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahName: String,
    val arabicText: String,
    val translationText: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "khatmah_plans")
data class KhatmahPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startDate: Long,
    val endDate: Long,
    val targetDays: Int,
    val pagesRead: Int = 0,
    val totalPages: Int = 604,
    val isCompleted: Boolean = false,
    val dailyMinutes: Int = 15
)

@Entity(tableName = "zikr_counters")
data class ZikrCounterEntity(
    @PrimaryKey val zikrId: String, // e.g. "morning_1", "evening_3"
    val count: Int = 0,
    val maxCount: Int = 33,
    val isFavorite: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "memorization_scores")
data class MemorizationScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val quizType: String, // e.g., "Complete Verse", "Guess Surah"
    val score: Int,
    val total: Int,
    val accuracy: Double
)

@Entity(tableName = "prayer_logs")
data class PrayerLogEntity(
    @PrimaryKey val dateStr: String, // YYYY-MM-DD
    val fajr: Boolean = false,
    val sunrise: Boolean = false,
    val dhuhr: Boolean = false,
    val asr: Boolean = false,
    val maghrib: Boolean = false,
    val isha: Boolean = false,
    val streakCount: Int = 0
)

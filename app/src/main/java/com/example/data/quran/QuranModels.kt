package com.example.data.quran

import androidx.annotation.Keep

@Keep
data class SurahHeader(
    val id: Int,
    val name: String,
    val arabicName: String,
    val translation: String,
    val type: String, // "Meccan" or "Medinan"
    val versesCount: Int,
    val durationMinutes: Int
)

@Keep
data class Ayah(
    val number: Int,
    val textArabic: String,
    val textTranslation: String,
    val tafsir: String,
    val wordMeanings: List<WordMeaning> = emptyList()
)

@Keep
data class WordMeaning(
    val word: String,
    val meaning: String
)

@Keep
data class Surah(
    val header: SurahHeader,
    val verses: List<Ayah>
)

@Keep
data class Reciter(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    val audioBaseUrl: String // for verse-by-verse streaming or surah streaming
)

@Keep
data class Zikr(
    val id: String,
    val category: String, // Morning, Evening, Sleep, Wake up, Prayer, Mosque, Travel, Food, Wudu
    val textArabic: String,
    val textTranslation: String,
    val source: String,
    val repeatCount: Int
)

@Keep
data class Dua(
    val id: String,
    val category: String, // Quran, Prophets, Ramadan, Rizq, Forgiveness, Anxiety, Illness, Parents, Marriage, Travel
    val title: String,
    val textArabic: String,
    val textTranslation: String,
    val source: String
)

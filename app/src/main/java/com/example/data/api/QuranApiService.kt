package com.example.data.api

import android.util.Log
import com.example.data.quran.Ayah
import com.example.data.quran.QuranDataset
import com.example.data.quran.Surah
import com.example.data.quran.WordMeaning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object QuranApiService {
    private const val TAG = "QuranApiService"
    private const val BASE_URL = "https://api.alquran.cloud/v1/surah"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun fetchSurah(surahId: Int): Surah? = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/$surahId/editions/quran-uthmani,en.sahih,ar.jalalayn"
        val request = Request.Builder().url(url).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to fetch surah $surahId. Code: ${response.code}")
                    return@withContext null
                }

                val bodyString = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyString)
                if (json.optInt("code") != 200) {
                    Log.e(TAG, "API returned non-200 code for surah $surahId")
                    return@withContext null
                }

                val dataArray = json.optJSONArray("data") ?: return@withContext null
                if (dataArray.length() < 3) {
                    Log.e(TAG, "API did not return all 3 requested editions for surah $surahId")
                    return@withContext null
                }

                // 0: quran-uthmani (Arabic text)
                // 1: en.sahih (English translation)
                // 2: ar.jalalayn (Arabic Tafsir)
                val arabicObj = dataArray.getJSONObject(0)
                val englishObj = dataArray.getJSONObject(1)
                val tafsirObj = dataArray.getJSONObject(2)

                val arabicAyahs = arabicObj.getJSONArray("ayahs")
                val englishAyahs = englishObj.getJSONArray("ayahs")
                val tafsirAyahs = tafsirObj.getJSONArray("ayahs")

                val count = arabicAyahs.length()
                val verses = ArrayList<Ayah>()

                for (i in 0 until count) {
                    val arAyah = arabicAyahs.getJSONObject(i)
                    val enAyah = englishAyahs.getJSONObject(i)
                    val tafAyah = tafsirAyahs.getJSONObject(i)

                    val numInSurah = arAyah.getInt("numberInSurah")
                    val textAr = arAyah.getString("text")
                    val textEn = enAyah.getString("text")
                    val tafsirText = tafAyah.getString("text")

                    verses.add(
                        Ayah(
                            number = numInSurah,
                            textArabic = textAr,
                            textTranslation = textEn,
                            tafsir = tafsirText,
                            wordMeanings = emptyList()
                        )
                    )
                }

                val header = QuranDataset.allSurahHeaders.firstOrNull { it.id == surahId }
                    ?: QuranDataset.allSurahHeaders[0]

                return@withContext Surah(header, verses)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching surah $surahId", e)
            null
        }
    }
}

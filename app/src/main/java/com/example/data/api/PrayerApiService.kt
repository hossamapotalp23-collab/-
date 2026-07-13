package com.example.data.api

import android.util.Log
import com.example.data.prayer.PrayerCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object PrayerApiService {
    private const val TAG = "PrayerApiService"
    private const val BASE_URL = "https://api.aladhan.com/v1/timings"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    data class ApiPrayerResponse(
        val times: PrayerCalculator.PrayerTimes,
        val hijriDate: String
    )

    suspend fun fetchPrayerTimes(
        latitude: Double,
        longitude: Double,
        methodId: Int,
        dateStr: String // Format: dd-MM-yyyy
    ): ApiPrayerResponse? = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/$dateStr?latitude=$latitude&longitude=$longitude&method=$methodId"
        val request = Request.Builder().url(url).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to fetch prayer times. Code: ${response.code}")
                    return@withContext null
                }

                val bodyString = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyString)
                if (json.optInt("code") != 200) {
                    Log.e(TAG, "API returned non-200 code: ${json.optInt("code")}")
                    return@withContext null
                }

                val dataObj = json.optJSONObject("data") ?: return@withContext null
                val timingsObj = dataObj.optJSONObject("timings") ?: return@withContext null
                
                // Get times
                val fajr = timingsObj.optString("Fajr", "")
                val sunrise = timingsObj.optString("Sunrise", "")
                val dhuhr = timingsObj.optString("Dhuhr", "")
                val asr = timingsObj.optString("Asr", "")
                val maghrib = timingsObj.optString("Maghrib", "")
                val isha = timingsObj.optString("Isha", "")

                if (fajr.isEmpty() || dhuhr.isEmpty() || maghrib.isEmpty()) {
                    Log.e(TAG, "Invalid timings parsed from response")
                    return@withContext null
                }

                // Construct PrayerTimes
                // Some APIs return "HH:mm (TZ)" format, let's normalize to "HH:mm"
                fun normalizeTime(time: String): String {
                    return time.split(" ")[0]
                }

                val prayerTimes = PrayerCalculator.PrayerTimes(
                    fajr = normalizeTime(fajr),
                    sunrise = normalizeTime(sunrise),
                    dhuhr = normalizeTime(dhuhr),
                    asr = normalizeTime(asr),
                    maghrib = normalizeTime(maghrib),
                    isha = normalizeTime(isha)
                )

                // Parse Hijri date
                val dateObjObj = dataObj.optJSONObject("date")
                val hijriObj = dateObjObj?.optJSONObject("hijri")
                var hijriDateStr = ""
                if (hijriObj != null) {
                    val day = hijriObj.optString("day", "")
                    val monthObj = hijriObj.optJSONObject("month")
                    val monthEn = monthObj?.optString("en", "") ?: ""
                    val year = hijriObj.optString("year", "")
                    if (day.isNotEmpty() && monthEn.isNotEmpty() && year.isNotEmpty()) {
                        hijriDateStr = "$day $monthEn, $year AH"
                    }
                }

                return@withContext ApiPrayerResponse(prayerTimes, hijriDateStr)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching prayer times from Aladhan API", e)
            null
        }
    }
}

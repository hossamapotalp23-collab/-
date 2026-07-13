package com.example.data.prayer

import java.util.*
import kotlin.math.*

object PrayerCalculator {

    // Coordinates of Holy Kaaba in Makkah
    const val KAABA_LAT = 21.4225
    const val KAABA_LNG = 39.8262

    data class CityPreset(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val timezoneId: String
    )

    data class AdhanMuezzin(
        val id: String,
        val nameAr: String,
        val nameEn: String,
        val audioUrl: String
    )

    val cityPresets = listOf(
        CityPreset("Makkah (Saudi Arabia)", 21.4225, 39.8262, "Asia/Riyadh"),
        CityPreset("Cairo (Egypt)", 30.0444, 31.2357, "Africa/Cairo"),
        CityPreset("Jakarta (Indonesia)", -6.2088, 106.8456, "Asia/Jakarta"),
        CityPreset("London (United Kingdom)", 51.5074, -0.1278, "Europe/London"),
        CityPreset("Dubai (UAE)", 25.2048, 55.2708, "Asia/Dubai"),
        CityPreset("New York (USA)", 40.7128, -74.0060, "America/New_York"),
        CityPreset("Kuala Lumpur (Malaysia)", 3.1390, 101.6869, "Asia/Kuala_Lumpur"),
        CityPreset("Istanbul (Turkey)", 41.0082, 28.9784, "Europe/Istanbul")
    )

    val egyptCityPresets = listOf(
        CityPreset("Cairo", 30.0444, 31.2357, "Africa/Cairo"),
        CityPreset("Alexandria", 31.2001, 29.9187, "Africa/Cairo"),
        CityPreset("Giza", 30.0131, 31.2089, "Africa/Cairo"),
        CityPreset("Samanoud (Gharbia)", 30.9614, 31.2428, "Africa/Cairo"),
        CityPreset("El Mahalla El Kubra (Gharbia)", 30.9733, 31.1685, "Africa/Cairo"),
        CityPreset("Tanta (Gharbia)", 30.7865, 31.0004, "Africa/Cairo"),
        CityPreset("Zifta (Gharbia)", 30.7186, 31.2394, "Africa/Cairo"),
        CityPreset("Kafr El Zayat (Gharbia)", 30.8222, 30.8122, "Africa/Cairo"),
        CityPreset("Basyoun (Gharbia)", 30.9389, 30.8122, "Africa/Cairo"),
        CityPreset("Mansoura (Dakahlia)", 31.0409, 31.3785, "Africa/Cairo"),
        CityPreset("Mit Ghamr (Dakahlia)", 30.7194, 31.2611, "Africa/Cairo"),
        CityPreset("Sennelawen (Dakahlia)", 30.8872, 31.4650, "Africa/Cairo"),
        CityPreset("Talkha (Dakahlia)", 31.0544, 31.3772, "Africa/Cairo"),
        CityPreset("Damanhour (Beheira)", 31.0364, 30.4688, "Africa/Cairo"),
        CityPreset("Kafr El Sheikh", 31.1107, 30.9388, "Africa/Cairo"),
        CityPreset("Zagazig (Sharqia)", 30.5877, 31.5024, "Africa/Cairo"),
        CityPreset("Minya El Qamh (Sharqia)", 30.5186, 31.3414, "Africa/Cairo"),
        CityPreset("Belbeis (Sharqia)", 30.4184, 31.5644, "Africa/Cairo"),
        CityPreset("Shibin El Kom (Menofia)", 30.5503, 31.0104, "Africa/Cairo"),
        CityPreset("Menouf (Menofia)", 30.4664, 30.9317, "Africa/Cairo"),
        CityPreset("Ashmoun (Menofia)", 30.2917, 30.9856, "Africa/Cairo"),
        CityPreset("Banha (Qalyubia)", 30.4591, 31.1856, "Africa/Cairo"),
        CityPreset("Qalyub (Qalyubia)", 30.1798, 31.2081, "Africa/Cairo"),
        CityPreset("Shubra El-Kheima", 30.1286, 31.2422, "Africa/Cairo"),
        CityPreset("Port Said", 31.2653, 32.3019, "Africa/Cairo"),
        CityPreset("Suez", 29.9668, 32.5498, "Africa/Cairo"),
        CityPreset("Luxor", 25.6872, 32.6396, "Africa/Cairo"),
        CityPreset("Aswan", 24.0889, 32.8998, "Africa/Cairo"),
        CityPreset("Asyut", 27.1783, 31.1859, "Africa/Cairo"),
        CityPreset("Ismailia", 30.6044, 32.2723, "Africa/Cairo"),
        CityPreset("Faiyum", 29.3084, 30.8428, "Africa/Cairo"),
        CityPreset("Damietta", 31.4175, 31.8144, "Africa/Cairo"),
        CityPreset("Minya", 28.0991, 30.7501, "Africa/Cairo"),
        CityPreset("Beni Suef", 29.0744, 31.0978, "Africa/Cairo"),
        CityPreset("Qena", 26.1551, 32.7160, "Africa/Cairo"),
        CityPreset("Sohag", 26.5592, 31.6957, "Africa/Cairo"),
        CityPreset("Hurghada", 27.2579, 33.8116, "Africa/Cairo"),
        CityPreset("Arish", 31.1321, 33.8032, "Africa/Cairo"),
        CityPreset("Marsa Matruh", 31.3525, 27.2361, "Africa/Cairo"),
        CityPreset("El Tor", 28.2433, 33.6231, "Africa/Cairo"),
        CityPreset("Kharga", 25.4390, 30.5497, "Africa/Cairo")
    )

    val adhanMuezzins = listOf(
        AdhanMuezzin("makkah", "علي أحمد ملا (أذان مكة)", "Ali Ahmad Mulla (Makkah)", "https://www.islamcan.com/audio/adhan/azan1.mp3"),
        AdhanMuezzin("abdul_basit", "عبد الباسط عبد الصمد", "Abdul Basit Abdus Samad", "https://www.islamcan.com/audio/adhan/azan2.mp3"),
        AdhanMuezzin("alafasy", "مشاري راشد العفاسي", "Mishary Rashid Alafasy", "https://www.islamcan.com/audio/adhan/azan20.mp3"),
        AdhanMuezzin("madinah", "أذان المدينة المنورة", "Madinah Adhan", "https://www.islamcan.com/audio/adhan/azan16.mp3"),
        AdhanMuezzin("luhaidan", "محمد اللحيدان", "Muhammad Al-Luhaidan", "https://www.islamcan.com/audio/adhan/azan10.mp3")
    )

    enum class CalculationMethod(val description: String) {
        UMM_AL_QURA("Umm Al-Qura (Makkah)"),
        MUSLIM_WORLD_LEAGUE("Muslim World League"),
        ISNA("Islamic Society of North America (ISNA)"),
        EGYPT("Egyptian General Authority"),
        KARACHI("University of Islamic Sciences, Karachi")
    }

    data class PrayerTimes(
        val fajr: String,
        val sunrise: String,
        val dhuhr: String,
        val asr: String,
        val maghrib: String,
        val isha: String
    )

    /**
     * Calculates the exact bearing angle from the user's location to the Kaaba.
     * Angle is in degrees clockwise from North (0° to 360°).
     */
    fun calculateQiblaDirection(latitude: Double, longitude: Double): Double {
        val userLatRad = Math.toRadians(latitude)
        val userLngRad = Math.toRadians(longitude)
        val kaabaLatRad = Math.toRadians(KAABA_LAT)
        val kaabaLngRad = Math.toRadians(KAABA_LNG)

        val deltaLng = kaabaLngRad - userLngRad

        val y = sin(deltaLng) * cos(kaabaLatRad)
        val x = cos(userLatRad) * sin(kaabaLatRad) - sin(userLatRad) * cos(kaabaLatRad) * cos(deltaLng)

        var bearing = Math.toDegrees(atan2(y, x))
        bearing = (bearing + 360.0) % 360.0
        return bearing
    }

    /**
     * Calculates realistic prayer times based on latitude, longitude, and date.
     * This uses a robust approximation with astronomical offsets, ensuring accuracy
     * across different calendar days and custom calculation methods.
     */
    fun calculatePrayerTimes(
        latitude: Double,
        longitude: Double,
        date: Calendar,
        method: CalculationMethod
    ): PrayerTimes {
        val dayOfYear = date.get(Calendar.DAY_OF_YEAR)
        
        // Solar noon (Dhuhr) shifts slightly throughout the year between 11:45 and 12:15 local standard time
        // We calculate Dhuhr based on timezone, longitude, and equation of time approximation
        val tzOffset = date.timeZone.rawOffset / (1000.0 * 60 * 60)
        val baseNoon = 12.0 + (tzOffset - longitude / 15.0)
        
        // Equation of time and declination of the sun
        val b = 2 * Math.PI * (dayOfYear - 81) / 365.0
        val equationOfTime = 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b) // in minutes
        val declination = 23.45 * sin(2 * Math.PI * (284 + dayOfYear) / 365.0) // in degrees

        val noonMinutes = (baseNoon * 60.0) - equationOfTime
        
        // Determine Fajr and Isha angles based on calculation method
        val (fajrAngle, ishaAngle) = when (method) {
            CalculationMethod.UMM_AL_QURA -> Pair(18.5, 90.0) // Isha is actually 90 mins after Maghrib, but we represent angle or handle offset
            CalculationMethod.MUSLIM_WORLD_LEAGUE -> Pair(18.0, 17.0)
            CalculationMethod.ISNA -> Pair(15.0, 15.0)
            CalculationMethod.EGYPT -> Pair(19.5, 17.5)
            CalculationMethod.KARACHI -> Pair(18.0, 18.0)
        }

        // Calculate twilight times using astronomical formulas:
        // cos(H) = (sin(angle) - sin(lat) * sin(dec)) / (cos(lat) * cos(dec))
        val latRad = Math.toRadians(latitude)
        val decRad = Math.toRadians(declination)

        fun hourAngle(angle: Double, isRising: Boolean): Double {
            val angleRad = Math.toRadians(-angle)
            val cosH = (sin(angleRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
            if (cosH < -1.0 || cosH > 1.0) {
                // Extreme latitudes edge case, provide fallback
                return if (isRising) 6.0 else 18.0
            }
            val h = Math.toDegrees(acos(cosH)) / 15.0
            return h
        }

        // Sunrise and Sunset (approx angle 0.833 degrees due to refraction and altitude)
        val sunriseHourAngle = hourAngle(0.833, true)
        
        // Fajr (morning twilight)
        val fajrHourAngle = hourAngle(fajrAngle, true)
        
        // Isha (night twilight)
        val ishaHourAngle = hourAngle(ishaAngle, false)

        // Asr calculation (Shafi/Hanafi shadow length)
        // Shafi: shadow factor = 1, Hanafi: shadow factor = 2
        val shadowFactor = 1.0
        val g = abs(latitude - declination)
        val acotVal = shadowFactor + tan(Math.toRadians(g))
        val asrAngleRad = atan(1.0 / acotVal)
        val asrAngle = Math.toDegrees(asrAngleRad)
        val asrCosH = (sin(asrAngleRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
        val asrHourAngle = if (asrCosH in -1.0..1.0) Math.toDegrees(acos(asrCosH)) / 15.0 else 3.0

        // Format times
        val dhuhrMin = noonMinutes.roundToInt()
        val fajrMin = (noonMinutes - (fajrHourAngle * 60.0)).roundToInt()
        val sunriseMin = (noonMinutes - (sunriseHourAngle * 60.0)).roundToInt()
        val asrMin = (noonMinutes + (asrHourAngle * 60.0)).roundToInt()
        val maghribMin = (noonMinutes + (sunriseHourAngle * 60.0)).roundToInt() + 3 // 3 mins safety margin
        
        val ishaMin = if (method == CalculationMethod.UMM_AL_QURA) {
            maghribMin + 90 // In Umm Al-Qura, Isha is exactly 90 mins after Maghrib (120 mins during Ramadan)
        } else {
            (noonMinutes + (ishaHourAngle * 60.0)).roundToInt()
        }

        fun formatTime(minutes: Int): String {
            val normalizedMins = (minutes + 1440) % 1440
            val h = normalizedMins / 60
            val m = normalizedMins % 60
            return String.format("%02d:%02d", h, m)
        }

        return PrayerTimes(
            fajr = formatTime(fajrMin),
            sunrise = formatTime(sunriseMin),
            dhuhr = formatTime(dhuhrMin),
            asr = formatTime(asrMin),
            maghrib = formatTime(maghribMin),
            isha = formatTime(ishaMin)
        )
    }

    /**
     * Gets simple Hijri date string approximation
     */
    fun getHijriDate(): String {
        // We can approximate based on Gregorian calendars and standard shifts
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)

        // Standard approximation formula (simplified)
        // Hijri year = (Gregorian - 622) * (33 / 32)
        val hijriYear = ((year - 622) * 32.5 / 31.5).toInt()
        
        val hijriMonths = listOf(
            "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' ath-Thani",
            "Jumada al-Awwal", "Jumada ath-Thani", "Rajab", "Sha'ban",
            "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
        )
        
        // Approximate day and month
        val approxHijriMonthIndex = (month + 8) % 12
        val approxHijriDay = (day + 15) % 30 + 1
        
        return "$approxHijriDay ${hijriMonths[approxHijriMonthIndex]}, $hijriYear AH"
    }
}

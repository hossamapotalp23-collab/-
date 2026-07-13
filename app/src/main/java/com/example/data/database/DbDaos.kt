package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE surahNumber = :surahNum AND ayahNumber = :ayahNum")
    suspend fun deleteBookmark(surahNum: Int, ayahNum: Int)

    @Delete
    suspend fun deleteBookmarkEntity(bookmark: BookmarkEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE surahNumber = :surahNum AND ayahNumber = :ayahNum)")
    fun isBookmarked(surahNum: Int, ayahNum: Int): Flow<Boolean>
}

@Dao
interface KhatmahDao {
    @Query("SELECT * FROM khatmah_plans WHERE isCompleted = 0 LIMIT 1")
    fun getActivePlan(): Flow<KhatmahPlanEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: KhatmahPlanEntity)

    @Update
    suspend fun updatePlan(plan: KhatmahPlanEntity)

    @Query("DELETE FROM khatmah_plans")
    suspend fun deleteAllPlans()
}

@Dao
interface ZikrDao {
    @Query("SELECT * FROM zikr_counters")
    fun getAllCounters(): Flow<List<ZikrCounterEntity>>

    @Query("SELECT * FROM zikr_counters WHERE zikrId = :id AND userId = :userId")
    fun getCounterById(id: String, userId: String): Flow<ZikrCounterEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCounter(counter: ZikrCounterEntity)

    @Query("UPDATE zikr_counters SET count = :count, lastUpdated = :timestamp WHERE zikrId = :id AND userId = :userId")
    suspend fun updateCount(id: String, userId: String, count: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE zikr_counters SET isFavorite = :isFav WHERE zikrId = :id AND userId = :userId")
    suspend fun toggleFavorite(id: String, userId: String, isFav: Boolean)
}

@Dao
interface MemorizationDao {
    @Query("SELECT * FROM memorization_scores ORDER BY date DESC")
    fun getAllScores(): Flow<List<MemorizationScoreEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: MemorizationScoreEntity)

    @Query("SELECT AVG(accuracy) FROM memorization_scores")
    fun getAverageAccuracy(): Flow<Double?>
}

@Dao
interface PrayerDao {
    @Query("SELECT * FROM prayer_logs ORDER BY dateStr DESC LIMIT 30")
    fun getRecentLogs(): Flow<List<PrayerLogEntity>>

    @Query("SELECT * FROM prayer_logs WHERE dateStr = :dateStr")
    fun getLogForDate(dateStr: String): Flow<PrayerLogEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: PrayerLogEntity)
}

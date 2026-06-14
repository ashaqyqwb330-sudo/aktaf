package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ========== برنامج رجال الله ==========
@Dao
interface MenOfGodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvaluation(evaluation: MenOfGodEvaluation)

    @Query("SELECT * FROM men_of_god_evaluation WHERE childId = :childId AND date = :date LIMIT 1")
    suspend fun getEvaluationForDate(childId: Long, date: String): MenOfGodEvaluation?

    @Query("SELECT * FROM men_of_god_evaluation WHERE childId = :childId ORDER BY date DESC LIMIT 7")
    suspend fun getWeeklyEvaluations(childId: Long): List<MenOfGodEvaluation>

    @Query("SELECT AVG(prayerScore + quranScore + athkarScore + faithAwareness + mediaAwareness + gratitudeAwareness + sincerityInGiving + jihadSpirit + knowingAllahNames) FROM men_of_god_evaluation WHERE childId = :childId AND date >= :startDate")
    suspend fun getAverageScore(childId: Long, startDate: String): Double?
}

// ========== تتبع الأدعية ==========
@Dao
interface DailyPrayerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracking(tracking: DailyPrayerTracking)

    @Query("SELECT * FROM daily_prayer_tracking WHERE childId = :childId AND date = :date")
    suspend fun getTrackingForDate(childId: Long, date: String): List<DailyPrayerTracking>

    @Query("SELECT COUNT(*) FROM daily_prayer_tracking WHERE childId = :childId AND isRead = 1")
    suspend fun getReadPrayersCount(childId: Long): Int
}

// ========== نصوص الأدعية ==========
@Dao
interface PrayerTextDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayer(prayer: PrayerText)

    @Query("SELECT * FROM prayer_texts WHERE type = :type")
    suspend fun getPrayersByType(type: String): List<PrayerText>

    @Query("SELECT * FROM prayer_texts WHERE dayOfWeek = :day")
    suspend fun getPrayerForDay(day: Int): PrayerText?

    @Query("SELECT * FROM prayer_texts")
    suspend fun getAllPrayers(): List<PrayerText>
}

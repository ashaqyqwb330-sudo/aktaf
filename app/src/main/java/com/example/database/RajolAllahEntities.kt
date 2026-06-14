package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "rajol_allah_assessments")
data class RajolAllahAssessmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = 1,
    val weekStartDate: String,
    val faithAndTrust: Int = 0,
    val selfPurification: Int = 0,
    val quranicCulture: Int = 0,
    val beneficialKnowledge: Int = 0,
    val historicalUnderstanding: Int = 0,
    val spreadingGuidance: Int = 0,
    val followingLeadership: Int = 0,
    val practicalPerformance: Int = 0,
    val pietyInAction: Int = 0,
    val pietyInSpending: Int = 0,
    val islamicEthics: Int = 0,
    val jihadSpirit: Int = 0,
    val understandingEnemies: Int = 0,
    val politicalAwareness: Int = 0,
    val leadershipSpeeches: Int = 0,
    val mediaBoycott: Int = 0,
    val trustworthiness: Int = 0,
    val avoidWaste: Int = 0,
    val carefulWithResources: Int = 0,
    val avoidLuxury: Int = 0,
    val caringForPeople: Int = 0,
    val prayerCommitment: Int = 0,
    val dhikrCommitment: Int = 0,
    val quranCommitment: Int = 0,
    val parentalRespect: Int = 0,
    val honestyValue: Int = 0,
    val trustValue: Int = 0,
    val patienceInStruggle: Int = 0,
    val individualResponse: Int = 0,
    val deathRemembrance: Int = 0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "heart_check_logs")
data class HeartCheckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateStr: String, // "YYYY-MM-DD"
    val positiveCount: Int,
    val resultText: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "weekly_challenges")
data class WeeklyChallengeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val startDate: String, // "YYYY-MM-DD"
    val isCompleted: Boolean = false,
    val completedAt: Long = 0
)

@Entity(tableName = "scenario_results")
data class ScenarioResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scenarioTitle: String,
    val chosenOption: String,
    val isCorrect: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface RajolAllahDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessment(assessment: RajolAllahAssessmentEntity)
    
    @Query("SELECT * FROM rajol_allah_assessments WHERE userId = :userId ORDER BY weekStartDate DESC")
    fun getAssessmentsForUser(userId: Long): Flow<List<RajolAllahAssessmentEntity>>
    
    @Query("SELECT * FROM rajol_allah_assessments WHERE userId = :userId AND weekStartDate = :weekStartDate LIMIT 1")
    suspend fun getAssessmentForWeek(userId: Long, weekStartDate: String): RajolAllahAssessmentEntity?
    
    @Query("SELECT * FROM rajol_allah_assessments WHERE userId = :userId ORDER BY weekStartDate DESC LIMIT 10")
    suspend fun getLastTenAssessments(userId: Long): List<RajolAllahAssessmentEntity>

    // Heart Check Logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeartCheck(log: HeartCheckEntity)

    @Query("SELECT * FROM heart_check_logs ORDER BY createdAt DESC")
    fun getAllHeartChecksFlow(): Flow<List<HeartCheckEntity>>

    // Challenges
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(challenge: WeeklyChallengeEntity)

    @Query("SELECT * FROM weekly_challenges ORDER BY startDate DESC")
    fun getAllChallengesFlow(): Flow<List<WeeklyChallengeEntity>>

    @Query("UPDATE weekly_challenges SET isCompleted = :completed, completedAt = :completedAt WHERE id = :id")
    suspend fun updateChallengeStatus(id: Long, completed: Boolean, completedAt: Long)

    // Scenario results
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenarioResult(result: ScenarioResultEntity)

    @Query("SELECT * FROM scenario_results ORDER BY timestamp DESC")
    fun getAllScenarioResultsFlow(): Flow<List<ScenarioResultEntity>>
}

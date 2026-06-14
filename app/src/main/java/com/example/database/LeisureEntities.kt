package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "quizzes")
data class QuizEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sourceText: String,
    val type: String = "mcq",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val quizId: Long,
    val question: String,
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",
    val correctAnswer: String,
    val type: String = "mcq"
)

@Entity(tableName = "family_scores")
data class FamilyScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberName: String,
    val quizId: Long,
    val score: Int,
    val totalQuestions: Int,
    val date: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_wisdom")
data class DailyWisdomEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val source: String,
    val date: String,
    val type: String = "wisdom"
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class LeaderboardEntry(
    val memberName: String,
    val total: Int
)

@Dao
interface QuizDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: QuizEntity): Long

    @Query("SELECT * FROM quizzes ORDER BY createdAt DESC")
    fun getAllQuizzesFlow(): Flow<List<QuizEntity>>

    @Query("SELECT * FROM quizzes ORDER BY createdAt DESC")
    suspend fun getAllQuizzes(): List<QuizEntity>

    @Delete
    suspend fun deleteQuiz(quiz: QuizEntity)
}

@Dao
interface QuestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Query("SELECT * FROM questions WHERE quizId = :quizId")
    fun getQuestionsForQuizFlow(quizId: Long): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE quizId = :quizId")
    suspend fun getQuestionsForQuiz(quizId: Long): List<QuestionEntity>
}

@Dao
interface FamilyScoreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: FamilyScoreEntity)

    @Query("SELECT memberName, SUM(score) as total FROM family_scores GROUP BY memberName ORDER BY total DESC")
    fun getLeaderboardFlow(): Flow<List<LeaderboardEntry>>
}

@Dao
interface DailyWisdomDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWisdom(wisdom: DailyWisdomEntity)

    @Query("SELECT * FROM daily_wisdom WHERE date = :date LIMIT 1")
    suspend fun getWisdomForDate(date: String): DailyWisdomEntity?

    @Query("SELECT * FROM daily_wisdom ORDER BY id DESC LIMIT 1")
    suspend fun getLatestWisdom(): DailyWisdomEntity?
}

@Dao
interface SearchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: SearchHistoryEntity)

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 20")
    fun getRecentSearchFlow(): Flow<List<SearchHistoryEntity>>
}

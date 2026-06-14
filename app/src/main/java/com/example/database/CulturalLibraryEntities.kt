package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cultural_books")
data class CulturalBook(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String,
    val category: String,       // فكر، أدب، شعر، دين، تاريخ...
    val filePath: String = "",
    val coverPath: String = "",
    val isDownloaded: Boolean = false,
    val addedDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "cultural_articles")
data class CulturalArticle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String,
    val content: String,
    val category: String,
    val tags: String = "",
    val addedDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "cultural_gems")
data class CulturalGem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String,
    val content: String,
    val category: String,       // فكر ومسؤولية، روحانيات، شعر، مواليد...
    val tags: String = "",
    val addedDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "reading_logs")
data class ReadingLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemType: String,       // book, article, gem
    val itemId: Long,
    val timeSpentMinutes: Int = 0,
    val date: Long = System.currentTimeMillis()
)

@Dao
interface CulturalLibraryDao {
    // Books
    @Query("SELECT * FROM cultural_books ORDER BY addedDate DESC")
    fun getAllBooksFlow(): Flow<List<CulturalBook>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: CulturalBook): Long

    @Query("DELETE FROM cultural_books WHERE id = :id")
    suspend fun deleteBook(id: Long)

    @Query("UPDATE cultural_books SET isDownloaded = :isDownloaded, filePath = :filePath WHERE id = :id")
    suspend fun updateBookDownloadStatus(id: Long, isDownloaded: Boolean, filePath: String)

    @Query("SELECT COUNT(*) FROM cultural_books")
    suspend fun getBooksCount(): Int

    // Articles
    @Query("SELECT * FROM cultural_articles ORDER BY addedDate DESC")
    fun getAllArticlesFlow(): Flow<List<CulturalArticle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: CulturalArticle): Long

    @Query("DELETE FROM cultural_articles WHERE id = :id")
    suspend fun deleteArticle(id: Long)

    @Query("SELECT COUNT(*) FROM cultural_articles")
    suspend fun getArticlesCount(): Int

    // Gems (Selected text gems)
    @Query("SELECT * FROM cultural_gems ORDER BY addedDate DESC")
    fun getAllGemsFlow(): Flow<List<CulturalGem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGem(gem: CulturalGem): Long

    @Query("DELETE FROM cultural_gems WHERE id = :id")
    suspend fun deleteGem(id: Long)

    @Query("SELECT COUNT(*) FROM cultural_gems")
    suspend fun getGemsCount(): Int

    // Reading Logs
    @Query("SELECT * FROM reading_logs ORDER BY date DESC")
    fun getAllReadingLogsFlow(): Flow<List<ReadingLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingLog(log: ReadingLog)

    @Query("SELECT SUM(timeSpentMinutes) FROM reading_logs")
    fun getTotalReadingTimeMinutesFlow(): Flow<Int?>

    @Query("SELECT COUNT(DISTINCT itemId) FROM reading_logs WHERE itemType = :itemType")
    fun getDistinctItemsCountFlow(itemType: String): Flow<Int>
}

package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<LibraryItemEntity>)

    @Query("SELECT * FROM library_items ORDER BY addedDate DESC")
    fun getAllItems(): Flow<List<LibraryItemEntity>>

    @Query("UPDATE library_items SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long)

    @Query("SELECT COUNT(*) FROM library_items")
    suspend fun count(): Int
}

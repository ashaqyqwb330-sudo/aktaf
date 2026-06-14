package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library_items")
data class LibraryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String,
    val author: String,
    val category: String,
    val content: String,
    val tags: String,
    val isFavorite: Boolean = false,
    val addedDate: Long = System.currentTimeMillis()
)

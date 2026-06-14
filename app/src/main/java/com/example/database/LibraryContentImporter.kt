package com.example.database

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

object LibraryContentImporter {

    suspend fun importIfNeeded(context: Context) {
        val db = EktefaaDatabase.getDatabase(context)
        if (db.libraryDao().count() > 0) return

        withContext(Dispatchers.IO) {
            try {
                val json = context.assets.open("library_content.json").bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(json)
                val entities = mutableListOf<LibraryItemEntity>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    entities.add(
                        LibraryItemEntity(
                            type = obj.getString("type"),
                            title = obj.getString("title"),
                            author = obj.getString("author"),
                            category = obj.getString("category"),
                            content = obj.getString("content"),
                            tags = obj.getString("tags")
                        )
                    )
                }
                db.libraryDao().insertItems(entities)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

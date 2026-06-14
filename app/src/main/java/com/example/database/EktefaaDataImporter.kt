package com.example.database

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object EktefaaDataImporter {

    suspend fun importIfNeeded(context: Context) {
        val db = EktefaaDatabase.getDatabase(context)

        withContext(Dispatchers.IO) {
            // 1. Import Lectures from database_part1.json if needed
            try {
                if (db.lectureDetailDao().count() == 0) {
                    Log.d("EktefaaDataImporter", "Starting download/import of database_part1.json...")
                    val jsonStr = context.assets.open("database_part1.json").bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(jsonStr)
                    if (jsonObject.has("V_LectureDetails")) {
                        val arr = jsonObject.getJSONArray("V_LectureDetails")
                        val list = mutableListOf<LectureDetailEntity>()
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            val valueStr = item.optString("value", "")
                            if (valueStr.isNotBlank()) {
                                list.add(
                                    LectureDetailEntity(
                                        id = item.optInt("id"),
                                        parentId = item.optInt("parent_id"),
                                        title = item.optString("title", "محاضرة #${item.optInt("id")}"),
                                        details = if (item.isNull("details")) null else item.optString("details"),
                                        idlec = item.optInt("idlec"),
                                        level = item.optInt("level"),
                                        value = valueStr
                                    )
                                )
                            }
                        }
                        // Chunk inserts to avoid exceeding SQLite binding limits if list is huge
                        list.chunked(200).forEach { chunk ->
                            db.lectureDetailDao().insertLectures(chunk)
                        }
                        Log.d("EktefaaDataImporter", "Successfully imported ${list.size} lectures out of database_part1.json.")
                    }
                }
            } catch (e: Exception) {
                Log.e("EktefaaDataImporter", "Failed to import database_part1.json", e)
            }

            // 2. Import Safha Sora Ayah from database_malazm_part1.json if needed
            try {
                if (db.safhaSoraAyahDao().count() == 0) {
                    Log.d("EktefaaDataImporter", "Starting download/import of database_malazm_part1.json...")
                    val jsonStr = context.assets.open("database_malazm_part1.json").bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(jsonStr)
                    if (jsonObject.has("Safha_Sora_Ayah")) {
                        val arr = jsonObject.getJSONArray("Safha_Sora_Ayah")
                        val list = mutableListOf<SafhaSoraAyahEntity>()
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            val contentStr = item.optString("content", "")
                            if (contentStr.isNotBlank()) {
                                list.add(
                                    SafhaSoraAyahEntity(
                                        safhaNo = item.optInt("safha_no"),
                                        soraId = item.optInt("sora_id"),
                                        startAyah = item.optInt("start_ayah"),
                                        endAyah = item.optInt("end_ayah"),
                                        content = contentStr,
                                        reference = item.optInt("refrence", 0)
                                    )
                                )
                            }
                        }
                        list.chunked(200).forEach { chunk ->
                            db.safhaSoraAyahDao().insertItems(chunk)
                        }
                        Log.d("EktefaaDataImporter", "Successfully imported ${list.size} items out of database_malazm_part1.json.")
                    }
                }
            } catch (e: Exception) {
                Log.e("EktefaaDataImporter", "Failed to import database_malazm_part1.json", e)
            }
        }
    }
}

package com.example.database

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

object AssetManager {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _progressMessage = MutableStateFlow("جاري التحميل المتوازي للبيانات المرجعية...")
    val progressMessage: StateFlow<String> = _progressMessage

    suspend fun importParallelIfNeeded(context: Context) {
        val db = EktefaaDatabase.getDatabase(context)
        
        val lecturesEmpty = withContext(Dispatchers.IO) { db.lectureDetailDao().count() == 0 }
        val quranEmpty = withContext(Dispatchers.IO) { db.safhaSoraAyahDao().count() == 0 }

        if (!lecturesEmpty && !quranEmpty) {
            Log.d("AssetManager", "Reference data already loaded.")
            return
        }

        _isLoading.value = true

        try {
            coroutineScope {
                val lectureJob = async(Dispatchers.IO) {
                    if (lecturesEmpty) {
                        try {
                            _progressMessage.value = "جاري تحميل أرشيف المحاضرات المنهجية..."
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
                                list.chunked(250).forEach { chunk ->
                                    db.lectureDetailDao().insertLectures(chunk)
                                }
                                Log.d("AssetManager", "Lectures import complete")
                            }
                        } catch (e: Exception) {
                            Log.e("AssetManager", "Failed to load database_part1.json", e)
                        }
                    }
                }

                val quranJob = async(Dispatchers.IO) {
                    if (quranEmpty) {
                        try {
                            _progressMessage.value = "جاري تحميل صفحات المصحف الشريف..."
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
                                list.chunked(250).forEach { chunk ->
                                    db.safhaSoraAyahDao().insertItems(chunk)
                                }
                                Log.d("AssetManager", "Quran import complete")
                            }
                        } catch (e: Exception) {
                            Log.e("AssetManager", "Failed to load database_malazm_part1.json", e)
                        }
                    }
                }

                lectureJob.await()
                quranJob.await()
            }
            _progressMessage.value = "اكتمل التهيئة المتوازية لقاعدة البيانات بنجاح!"
        } catch (e: Exception) {
            Log.e("AssetManager", "General load error", e)
        } finally {
            _isLoading.value = false
        }
    }
}

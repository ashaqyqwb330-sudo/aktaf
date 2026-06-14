package com.example.database

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

data class QuranVerse(
    val number: Int,
    val textAr: String,
    val textEn: String
)

data class SearchResult(
    val surahId: Int,
    val surahName: String,
    val verseNumber: Int,
    val verseText: String
)

object QuranLocalDatabase {
    private var surahsArray: JSONArray? = null

    @Synchronized
    fun init(context: Context) {
        if (surahsArray == null) {
            try {
                val jsonString = context.assets.open("database.json").bufferedReader().use { it.readText() }
                surahsArray = JSONArray(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getSurahVerses(context: Context, surahId: Int): List<QuranVerse> {
        init(context)
        val list = mutableListOf<QuranVerse>()
        val array = surahsArray ?: return list
        for (i in 0 until array.length()) {
            val surahObj = array.optJSONObject(i) ?: continue
            val number = surahObj.optInt("number")
            if (number == surahId) {
                val versesArray = surahObj.optJSONArray("verses") ?: continue
                for (j in 0 until versesArray.length()) {
                    val verseObj = versesArray.optJSONObject(j) ?: continue
                    val verseNum = verseObj.optInt("number")
                    val textObj = verseObj.optJSONObject("text") ?: continue
                    val textAr = textObj.optString("ar") ?: ""
                    val textEn = textObj.optString("en") ?: ""
                    list.add(QuranVerse(verseNum, textAr.trim(), textEn.trim()))
                }
                return list
            }
        }
        return list
    }

    fun searchQuran(context: Context, query: String): List<SearchResult> {
        init(context)
        val list = mutableListOf<SearchResult>()
        if (query.trim().isEmpty()) return list
        val array = surahsArray ?: return list
        for (i in 0 until array.length()) {
            val surahObj = array.optJSONObject(i) ?: continue
            val surahNum = surahObj.optInt("number")
            val surahName = surahObj.optJSONObject("name")?.optString("ar") ?: ""
            val versesArray = surahObj.optJSONArray("verses") ?: continue
            for (j in 0 until versesArray.length()) {
                val verseObj = versesArray.optJSONObject(j) ?: continue
                val verseNum = verseObj.optInt("number")
                val textObj = verseObj.optJSONObject("text") ?: continue
                val textAr = textObj.optString("ar") ?: ""
                if (textAr.contains(query)) {
                    list.add(SearchResult(surahNum, surahName, verseNum, textAr.trim()))
                }
            }
        }
        return list
    }

    fun getSurahText(context: Context, surahId: Int): String {
        init(context)
        val array = surahsArray ?: return ""
        for (i in 0 until array.length()) {
            val surahObj = array.optJSONObject(i) ?: continue
            val number = surahObj.optInt("number")
            if (number == surahId) {
                val versesArray = surahObj.optJSONArray("verses") ?: continue
                val sb = StringBuilder()
                for (j in 0 until versesArray.length()) {
                    val verseObj = versesArray.optJSONObject(j) ?: continue
                    val verseNum = verseObj.optInt("number")
                    val textObj = verseObj.optJSONObject("text") ?: continue
                    val textAr = textObj.optString("ar") ?: ""
                    
                    var cleanText = textAr.trim()
                    // If it is NOT Al-Fatihah (id=1) and is the first verse (j=0), remove any Bismillah prefix
                    // because it will be center-aligned at the top of the screen.
                    if (surahId != 1 && j == 0) {
                        val bismillah1 = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
                        val bismillah2 = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ"
                        if (cleanText.startsWith(bismillah1)) {
                            cleanText = cleanText.substring(bismillah1.length).trim()
                        } else if (cleanText.startsWith(bismillah2)) {
                            cleanText = cleanText.substring(bismillah2.length).trim()
                        }
                    }
                    if (cleanText.isNotEmpty()) {
                        sb.append(" ").append(cleanText).append(" ﴿").append(verseNum).append("﴾")
                    }
                }
                return sb.toString().trim()
            }
        }
        return ""
    }

    /**
     * Looks up the audio MP3 link for the specified surah and reciter.
     * Fallback to the first available link if the specified reciter is not found.
     */
    fun getSurahAudioLink(context: Context, surahId: Int, reciterName: String = "محمد صديق المنشاوي"): String? {
        init(context)
        val array = surahsArray ?: return null
        for (i in 0 until array.length()) {
            val surahObj = array.optJSONObject(i) ?: continue
            val number = surahObj.optInt("number")
            if (number == surahId) {
                val audioArray = surahObj.optJSONArray("audio") ?: continue
                if (audioArray.length() == 0) return null
                
                // Search for the specific reciter
                for (j in 0 until audioArray.length()) {
                    val audioItem = audioArray.optJSONObject(j) ?: continue
                    val reciterObj = audioItem.optJSONObject("reciter") ?: continue
                    val reciterAr = reciterObj.optString("ar") ?: ""
                    if (reciterAr.contains(reciterName) || reciterName.contains(reciterAr)) {
                        return audioItem.optString("link")
                    }
                }
                
                // Fallback to the first available audio item
                val firstObj = audioArray.optJSONObject(0)
                return firstObj?.optString("link")
            }
        }
        return null
    }
}

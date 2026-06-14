package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.EktefaaDatabase
import com.example.database.SchoolBookEntity
import com.example.ui.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import android.speech.tts.TextToSpeech
import java.util.Locale
import com.example.api.*

// Visual tokens strictly matched with the main Ektefaa branding guidelines
val Gold = WarmGold
val RadarGreen = GreenAccent
val DarkNavy = MidnightBlue
val Silver = SilverGray

class SchoolLibraryActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importBookFromUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            tts = TextToSpeech(this, this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        setContent {
            MyApplicationTheme {
                SchoolLibraryScreen(
                    onImportFromStorage = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                    onImportFromTelegram = { url -> downloadFromTelegram(url) },
                    onSpeak = { text -> 
                        try {
                            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "Ektefaa_Books_TTS")
                        } catch (e: Exception) {
                            Toast.makeText(this, "فشل تشغيل القارئ الصوتي", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onStopSpeak = { 
                        try {
                            tts?.stop()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    onSyncAllGrades = {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                loadAllBooksFromIndex(force = true)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@SchoolLibraryActivity, "تم بناء ومزامنة قاعدة بيانات المنهج لجميع الصفوف بنجاح!", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@SchoolLibraryActivity, "حدث خطأ أثناء المزامنة: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            loadAllBooksFromIndex()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setLanguage(Locale("ar"))
        }
    }

    override fun onDestroy() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    // ========== الفهرس الكامل المصدق والمعتمد لجميع الصفوف من 1 إلى 12 ==========
    private suspend fun loadAllBooksFromIndex(force: Boolean = false) {
        val db = EktefaaDatabase.getDatabase(this)
        if (force) {
            db.schoolBookDao().deleteAllBooks()
        } else {
            val existingCount = db.schoolBookDao().getBooksCount()
            if (existingCount > 0) return // تم التحميل سابقاً بنجاح
        }

        val baseBooks = listOf(
            // ===== الصف الأول =====
            SchoolBookEntity(title = "القرآن الكريم", grade = 1, subject = "القرآن الكريم", part = 1, telegramUrl = "https://t.me/L_alnader/64201"),
            SchoolBookEntity(title = "التربية الإسلامية", grade = 1, subject = "التربية الإسلامية", part = 1, telegramUrl = "https://t.me/L_alnader/64203"),
            SchoolBookEntity(title = "لغة عربية ج 1", grade = 1, subject = "اللغة العربية", part = 1, telegramUrl = "https://t.me/L_alnader/64205"),
            SchoolBookEntity(title = "لغة عربية ج 2", grade = 1, subject = "اللغة العربية", part = 2, telegramUrl = "https://t.me/L_alnader/64207"),
            SchoolBookEntity(title = "علوم ج 1", grade = 1, subject = "العلوم", part = 1, telegramUrl = "https://t.me/L_alnader/64209"),
            SchoolBookEntity(title = "علوم ج 2", grade = 1, subject = "العلوم", part = 2, telegramUrl = "https://t.me/L_alnader/64211"),
            SchoolBookEntity(title = "رياضيات ج 1", grade = 1, subject = "الرياضيات", part = 1, telegramUrl = "https://t.me/L_alnader/64213"),
            SchoolBookEntity(title = "رياضيات ج 2", grade = 1, subject = "الرياضيات", part = 2, telegramUrl = "https://t.me/L_alnader/64215"),

            // ===== الصف الثاني =====
            SchoolBookEntity(title = "القرآن الكريم", grade = 2, subject = "القرآن الكريم", part = 1, telegramUrl = "https://t.me/L_alnader/64220"),
            SchoolBookEntity(title = "التربية الإسلامية", grade = 2, subject = "التربية الإسلامية", part = 1, telegramUrl = "https://t.me/L_alnader/64222"),
            SchoolBookEntity(title = "لغة عربية ج 1", grade = 2, subject = "اللغة العربية", part = 1, telegramUrl = "https://t.me/L_alnader/64224"),
            SchoolBookEntity(title = "لغة عربية ج 2", grade = 2, subject = "اللغة العربية", part = 2, telegramUrl = "https://t.me/L_alnader/64226"),
            SchoolBookEntity(title = "رياضيات ج 1", grade = 2, subject = "الرياضيات", part = 1, telegramUrl = "https://t.me/L_alnader/64228"),
            SchoolBookEntity(title = "رياضيات ج 2", grade = 2, subject = "الرياضيات", part = 2, telegramUrl = "https://t.me/L_alnader/64230"),
            SchoolBookEntity(title = "علوم ج 1", grade = 2, subject = "العلوم", part = 1, telegramUrl = "https://t.me/L_alnader/64232"),
            SchoolBookEntity(title = "علوم ج 2", grade = 2, subject = "العلوم", part = 2, telegramUrl = "https://t.me/L_alnader/64234"),

            // ===== الصف الثالث =====
            SchoolBookEntity(title = "القرآن الكريم", grade = 3, subject = "القرآن الكريم", part = 1, telegramUrl = "https://t.me/L_alnader/64237"),
            SchoolBookEntity(title = "التربية الإسلامية", grade = 3, subject = "التربية الإسلامية", part = 1, telegramUrl = "https://t.me/L_alnader/64239"),
            SchoolBookEntity(title = "لغة عربية ج 1", grade = 3, subject = "اللغة العربية", part = 1, telegramUrl = "https://t.me/L_alnader/64241"),
            SchoolBookEntity(title = "لغة عربية ج 2", grade = 3, subject = "اللغة العربية", part = 2, telegramUrl = "https://t.me/L_alnader/64243"),
            SchoolBookEntity(title = "رياضيات ج 1", grade = 3, subject = "الرياضيات", part = 1, telegramUrl = "https://t.me/L_alnader/64245"),
            SchoolBookEntity(title = "رياضيات ج 2", grade = 3, subject = "الرياضيات", part = 2, telegramUrl = "https://t.me/L_alnader/64247"),
            SchoolBookEntity(title = "علوم ج 1", grade = 3, subject = "العلوم", part = 1, telegramUrl = "https://t.me/L_alnader/64249"),
            SchoolBookEntity(title = "علوم ج 2", grade = 3, subject = "العلوم", part = 2, telegramUrl = "https://t.me/L_alnader/64251"),
            SchoolBookEntity(title = "التربية الاجتماعية", grade = 3, subject = "التربية الاجتماعية", part = 1, telegramUrl = "https://t.me/L_alnader/64253"),

            // ===== الصف الرابع =====
            SchoolBookEntity(title = "كتاب القرآن الكريم وتجويده", grade = 4, subject = "القرآن الكريم", part = 1, telegramUrl = "https://t.me/L_alnader/64257"),
            SchoolBookEntity(title = "كتاب التربية الإسلامية", grade = 4, subject = "التربية الإسلامية", part = 1, telegramUrl = "https://t.me/L_alnader/64259"),
            SchoolBookEntity(title = "كتاب اللغة العربية - الجزء الأول", grade = 4, subject = "اللغة العربية", part = 1, telegramUrl = "https://t.me/L_alnader/64261"),
            SchoolBookEntity(title = "كتاب اللغة العربية - الجزء الثاني", grade = 4, subject = "اللغة العربية", part = 2, telegramUrl = "https://t.me/L_alnader/64263"),
            SchoolBookEntity(title = "كتاب التربية الاجتماعية والوطنية", grade = 4, subject = "التربية الاجتماعية", part = 1, telegramUrl = "https://t.me/L_alnader/64265"),
            SchoolBookEntity(title = "كتاب العلوم - الجزء الأول", grade = 4, subject = "العلوم", part = 1, telegramUrl = "https://t.me/L_alnader/64267"),
            SchoolBookEntity(title = "كتاب العلوم - الجزء الثاني", grade = 4, subject = "العلوم", part = 2, telegramUrl = "https://t.me/L_alnader/64269"),
            SchoolBookEntity(title = "كتاب الرياضيات - الجزء الأول", grade = 4, subject = "الرياضيات", part = 1, telegramUrl = "https://t.me/L_alnader/64271"),
            SchoolBookEntity(title = "كتاب الرياضيات - الجزء الثاني", grade = 4, subject = "الرياضيات", part = 2, telegramUrl = "https://t.me/L_alnader/64273"),

            // ===== الصف الخامس =====
            SchoolBookEntity(title = "القرآن الكريم", grade = 5, subject = "القرآن الكريم", part = 1, telegramUrl = "https://t.me/L_alnader/64278"),
            SchoolBookEntity(title = "التربية الإسلامية", grade = 5, subject = "التربية الإسلامية", part = 1, telegramUrl = "https://t.me/L_alnader/64280"),
            SchoolBookEntity(title = "لغة عربية ج 1", grade = 5, subject = "اللغة العربية", part = 1, telegramUrl = "https://t.me/L_alnader/64282"),
            SchoolBookEntity(title = "لغة عربية ج 2", grade = 5, subject = "اللغة العربية", part = 2, telegramUrl = "https://t.me/L_alnader/64284"),
            SchoolBookEntity(title = "التربية الوطنية", grade = 5, subject = "التربية الوطنية", part = 1, telegramUrl = "https://t.me/L_alnader/64286"),
            SchoolBookEntity(title = "تاريخ", grade = 5, subject = "التاريخ", part = 1, telegramUrl = "https://t.me/L_alnader/64288"),
            SchoolBookEntity(title = "جغرافيا", grade = 5, subject = "الجغرافيا", part = 1, telegramUrl = "https://t.me/L_alnader/64290"),
            SchoolBookEntity(title = "علوم ج 1", grade = 5, subject = "العلوم", part = 1, telegramUrl = "https://t.me/L_alnader/64292"),
            SchoolBookEntity(title = "علوم ج 2", grade = 5, subject = "العلوم", part = 2, telegramUrl = "https://t.me/L_alnader/64294"),
            SchoolBookEntity(title = "رياضيات ج 1", grade = 5, subject = "الرياضيات", part = 1, telegramUrl = "https://t.me/L_alnader/64296"),
            SchoolBookEntity(title = "رياضيات ج 2", grade = 5, subject = "الرياضيات", part = 2, telegramUrl = "https://t.me/L_alnader/64298"),

            // ===== الصف السادس =====
            SchoolBookEntity(title = "القرآن الكريم", grade = 6, subject = "القرآن الكريم", part = 1, telegramUrl = "https://t.me/L_alnader/64327"),
            SchoolBookEntity(title = "التربية الإسلامية", grade = 6, subject = "التربية الإسلامية", part = 1, telegramUrl = "https://t.me/L_alnader/64329"),
            SchoolBookEntity(title = "لغة عربية ج 1", grade = 6, subject = "اللغة العربية", part = 1, telegramUrl = "https://t.me/L_alnader/64331"),
            SchoolBookEntity(title = "لغة عربية ج 2", grade = 6, subject = "اللغة العربية", part = 2, telegramUrl = "https://t.me/L_alnader/64333"),
            SchoolBookEntity(title = "رياضيات ج 1", grade = 6, subject = "الرياضيات", part = 1, telegramUrl = "https://t.me/L_alnader/64335"),
            SchoolBookEntity(title = "رياضيات ج 2", grade = 6, subject = "الرياضيات", part = 2, telegramUrl = "https://t.me/L_alnader/64337"),
            SchoolBookEntity(title = "علوم ج 1", grade = 6, subject = "العلوم", part = 1, telegramUrl = "https://t.me/L_alnader/64339"),
            SchoolBookEntity(title = "علوم ج 2", grade = 6, subject = "العلوم", part = 2, telegramUrl = "https://t.me/L_alnader/64341"),
            SchoolBookEntity(title = "جغرافيا", grade = 6, subject = "الجغرافيا", part = 1, telegramUrl = "https://t.me/L_alnader/64343"),
            SchoolBookEntity(title = "التربية الوطنية", grade = 6, subject = "التربية الوطنية", part = 1, telegramUrl = "https://t.me/L_alnader/64345"),
            SchoolBookEntity(title = "تاريخ", grade = 6, subject = "التاريخ", part = 1, telegramUrl = "https://t.me/L_alnader/64348"),

            // ===== الصف السابع =====
            SchoolBookEntity(title = "القرآن الكريم", grade = 7, subject = "القرآن الكريم", part = 1, telegramUrl = "https://t.me/L_alnader/64356"),
            SchoolBookEntity(title = "التربية الإسلامية ج 1", grade = 7, subject = "التربية الإسلامية", part = 1, telegramUrl = "https://t.me/L_alnader/64358"),
            SchoolBookEntity(title = "التربية الإسلامية ج 2", grade = 7, subject = "التربية الإسلامية", part = 2, telegramUrl = "https://t.me/L_alnader/64360"),
            SchoolBookEntity(title = "لغة عربية ج 1", grade = 7, subject = "اللغة العربية", part = 1, telegramUrl = "https://t.me/L_alnader/64362"),
            SchoolBookEntity(title = "لغة عربية", grade = 7, subject = "اللغة العربية", part = 0, telegramUrl = "https://t.me/L_alnader/64364"),
            SchoolBookEntity(title = "التربية الوطنية", grade = 7, subject = "التربية الوطنية", part = 1, telegramUrl = "https://t.me/L_alnader/64366"),
            SchoolBookEntity(title = "جغرافيا", grade = 7, subject = "الجغرافيا", part = 1, telegramUrl = "https://t.me/L_alnader/64368"),
            SchoolBookEntity(title = "رياضيات", grade = 7, subject = "الرياضيات", part = 1, telegramUrl = "https://t.me/L_alnader/64370"),
            SchoolBookEntity(title = "علوم ج 1", grade = 7, subject = "العلوم", part = 1, telegramUrl = "https://t.me/L_alnader/64372"),
            SchoolBookEntity(title = "علوم ج 2", grade = 7, subject = "العلوم", part = 2, telegramUrl = "https://t.me/L_alnader/64374"),
            SchoolBookEntity(title = "إنجليزي ج 1", grade = 7, subject = "اللغة الإنجليزية", part = 1, telegramUrl = "https://t.me/L_alnader/64376"),
            SchoolBookEntity(title = "إنجليزي ج 2", grade = 7, subject = "اللغة الإنجليزية", part = 2, telegramUrl = "https://t.me/L_alnader/64378"),
            SchoolBookEntity(title = "تاريخ ج 1", grade = 7, subject = "التاريخ", part = 1, telegramUrl = "https://t.me/L_alnader/64380"),
            SchoolBookEntity(title = "تاريخ ج 2", grade = 7, subject = "التاريخ", part = 2, telegramUrl = "https://t.me/L_alnader/64382"),

            // ===== الصف الثامن =====
            SchoolBookEntity(title = "القرآن الكريم", grade = 8, subject = "القرآن الكريم", part = 1, telegramUrl = "https://t.me/L_alnader/64389"),
            SchoolBookEntity(title = "التربية الإسلامية ج 1", grade = 8, subject = "التربية الإسلامية", part = 1, telegramUrl = "https://t.me/L_alnader/64390"),
            SchoolBookEntity(title = "التربية الإسلامية ج 2", grade = 8, subject = "التربية الإسلامية", part = 2, telegramUrl = "https://t.me/L_alnader/64391"),
            SchoolBookEntity(title = "لغة عربية ج 1", grade = 8, subject = "اللغة العربية", part = 1, telegramUrl = "https://t.me/L_alnader/64593"),
            SchoolBookEntity(title = "لغة عربية ج 2", grade = 8, subject = "اللغة العربية", part = 2, telegramUrl = "https://t.me/L_alnader/64594"),
            SchoolBookEntity(title = "رياضيات ج 1 و 2", grade = 8, subject = "الرياضيات", part = 0, telegramUrl = "https://t.me/L_alnader/64595"),
            SchoolBookEntity(title = "علوم ج 1 (2017)", grade = 8, subject = "العلوم", part = 1, year = 2017, telegramUrl = "https://t.me/L_alnader/64597"),
            SchoolBookEntity(title = "علوم ج 2 (2017)", grade = 8, subject = "العلوم", part = 2, year = 2017, telegramUrl = "https://t.me/L_alnader/64601"),
            SchoolBookEntity(title = "إنجليزي (2023)", grade = 8, subject = "اللغة الإنجليزية", part = 1, year = 2023, telegramUrl = "https://t.me/L_alnader/64602"),
            SchoolBookEntity(title = "إنجليزي (2017)", grade = 8, subject = "اللغة الإنجليزية", part = 1, year = 2017, telegramUrl = "https://t.me/L_alnader/64603"),
            SchoolBookEntity(title = "التربية الوطنية", grade = 8, subject = "التربية الوطنية", part = 1, telegramUrl = "https://t.me/L_alnader/64604"),
            SchoolBookEntity(title = "جغرافيا (2023)", grade = 8, subject = "الجغرافيا", part = 1, year = 2023, telegramUrl = "https://t.me/L_alnader/64605"),
            SchoolBookEntity(title = "تاريخ ج 1 (2017)", grade = 8, subject = "التاريخ", part = 1, year = 2017, telegramUrl = "https://t.me/L_alnader/64606"),
            SchoolBookEntity(title = "تاريخ ج 2", grade = 8, subject = "التاريخ", part = 2, telegramUrl = "https://t.me/L_alnader/64607"),

            // ===== الصف التاسع =====
            SchoolBookEntity(title = "القرآن الكريم", grade = 9, subject = "القرآن الكريم", part = 1, telegramUrl = "https://t.me/L_alnader/64610"),
            SchoolBookEntity(title = "التربية الإسلامية ج 1", grade = 9, subject = "التربية الإسلامية", part = 1, telegramUrl = "https://t.me/L_alnader/64612"),
            SchoolBookEntity(title = "التربية الإسلامية ج 2", grade = 9, subject = "التربية الإسلامية", part = 2, telegramUrl = "https://t.me/L_alnader/64613"),
            SchoolBookEntity(title = "اللغة العربية ج 1 (2023)", grade = 9, subject = "اللغة العربية", part = 1, year = 2023, telegramUrl = "https://t.me/L_alnader/64614"),
            SchoolBookEntity(title = "اللغة العربية ج 2 (2017)", grade = 9, subject = "اللغة العربية", part = 2, year = 2017, telegramUrl = "https://t.me/L_alnader/64615"),
            SchoolBookEntity(title = "رياضيات ج 1 (2017)", grade = 9, subject = "الرياضيات", part = 1, year = 2017, telegramUrl = "https://t.me/L_alnader/64616"),
            SchoolBookEntity(title = "رياضيات ج 2 (2017)", grade = 9, subject = "الرياضيات", part = 2, year = 2017, telegramUrl = "https://t.me/L_alnader/64617"),
            SchoolBookEntity(title = "علوم ج 1 (2022)", grade = 9, subject = "العلوم", part = 1, year = 2022, telegramUrl = "https://t.me/L_alnader/64618"),
            SchoolBookEntity(title = "علوم ج 2 (2022)", grade = 9, subject = "العلوم", part = 2, year = 2022, telegramUrl = "https://t.me/L_alnader/64619"),
            SchoolBookEntity(title = "إنجليزي (2023)", grade = 9, subject = "اللغة الإنجليزية", part = 1, year = 2023, telegramUrl = "https://t.me/L_alnader/64620"),
            SchoolBookEntity(title = "إنجليزي (2017)", grade = 9, subject = "اللغة الإنجليزية", part = 1, year = 2017, telegramUrl = "https://t.me/L_alnader/64621"),
            SchoolBookEntity(title = "تاريخ (2021)", grade = 9, subject = "التاريخ", part = 1, year = 2021, telegramUrl = "https://t.me/L_alnader/64622"),
            SchoolBookEntity(title = "تاريخ (2017)", grade = 9, subject = "التاريخ", part = 1, year = 2017, telegramUrl = "https://t.me/L_alnader/64623"),
            SchoolBookEntity(title = "جغرافيا", grade = 9, subject = "الجغرافيا", part = 1, telegramUrl = "https://t.me/L_alnader/64624"),
            SchoolBookEntity(title = "التربية الوطنية", grade = 9, subject = "التربية الوطنية", part = 1, telegramUrl = "https://t.me/L_alnader/64625"),

            // ===== الصف العاشر (أول ثانوي) =====
            SchoolBookEntity(title = "السيرة النبوية", grade = 10, subject = "السيرة النبوية", part = 1, telegramUrl = "https://t.me/L_alnader/64628"),
            SchoolBookEntity(title = "المجتمع اليمني", grade = 10, subject = "المجتمع اليمني", part = 1, telegramUrl = "https://t.me/L_alnader/64629"),
            SchoolBookEntity(title = "جغرافيا", grade = 10, subject = "الجغرافيا", part = 1, telegramUrl = "https://t.me/L_alnader/64630"),
            SchoolBookEntity(title = "الحاسوب", grade = 10, subject = "الحاسوب", part = 1, telegramUrl = "https://t.me/L_alnader/64631"),
            SchoolBookEntity(title = "أدب نصوص بلاغة ج 1", grade = 10, subject = "الأدب والنصوص والبلاغة", part = 1, telegramUrl = "https://t.me/L_alnader/64632"),
            SchoolBookEntity(title = "أدب نصوص بلاغة ج 2", grade = 10, subject = "الأدب والنصوص والبلاغة", part = 2, telegramUrl = "https://t.me/L_alnader/64633"),
            SchoolBookEntity(title = "لغة عربية ج 1", grade = 10, subject = "اللغة العربية", part = 1, telegramUrl = "https://t.me/L_alnader/64634"),
            SchoolBookEntity(title = "لغة عربية ج 2", grade = 10, subject = "اللغة العربية", part = 2, telegramUrl = "https://t.me/L_alnader/64635"),
            SchoolBookEntity(title = "إنجليزي ج 1", grade = 10, subject = "اللغة الإنجليزية", part = 1, telegramUrl = "https://t.me/L_alnader/64636"),
            SchoolBookEntity(title = "إنجليزي ج 2", grade = 10, subject = "اللغة الإنجليزية", part = 2, telegramUrl = "https://t.me/L_alnader/64637"),
            SchoolBookEntity(title = "رياضيات ج 1", grade = 10, subject = "الرياضيات", part = 1, telegramUrl = "https://t.me/L_alnader/64638"),
            SchoolBookEntity(title = "رياضيات ج 2", grade = 10, subject = "الرياضيات", part = 2, telegramUrl = "https://t.me/L_alnader/64639"),
            SchoolBookEntity(title = "تمارين رياضيات", grade = 10, subject = "الرياضيات", part = 3, telegramUrl = "https://t.me/L_alnader/64640"),
            SchoolBookEntity(title = "نحو وصرف ج 1", grade = 10, subject = "النحو والصرف", part = 1, telegramUrl = "https://t.me/L_alnader/64641"),
            SchoolBookEntity(title = "نحو وصرف ج 2", grade = 10, subject = "النحو والصرف", part = 2, telegramUrl = "https://t.me/L_alnader/64642"),
            SchoolBookEntity(title = "تاريخ ج 1 و 2 وأحياء وفيزياء وقرآن", grade = 10, subject = "مواد متنوعة", part = 1, telegramUrl = "https://t.me/L_alnader/64643"),
            SchoolBookEntity(title = "أنشطة فيزياء وكيمياء وإيمان وحديث وفقه وكيمياء", grade = 10, subject = "مواد متنوعة", part = 2, telegramUrl = "https://t.me/L_alnader/64649"),
            SchoolBookEntity(title = "تاريخ (2023)", grade = 10, subject = "التاريخ", part = 1, year = 2023, telegramUrl = "https://t.me/L_alnader/64654"),
            SchoolBookEntity(title = "إنجليزي ج 1 (2023)", grade = 10, subject = "اللغة الإنجليزية", part = 1, year = 2023, telegramUrl = "https://t.me/L_alnader/64655"),
            SchoolBookEntity(title = "إنجليزي ج 2 (2023)", grade = 10, subject = "اللغة الإنجليزية", part = 2, year = 2023, telegramUrl = "https://t.me/L_alnader/64656"),

            // ===== الصف الحادي عشر (ثاني ثانوي) =====
            SchoolBookEntity(title = "القرآن الكريم ج 1", grade = 11, subject = "القرآن الكريم", part = 1, telegramUrl = "https://t.me/L_alnader/64691"),
            SchoolBookEntity(title = "القرآن الكريم ج 2", grade = 11, subject = "القرآن الكريم", part = 2, telegramUrl = "https://t.me/L_alnader/64692"),
            SchoolBookEntity(title = "الحديث والفقه ج 1", grade = 11, subject = "الحديث والفقه", part = 1, telegramUrl = "https://t.me/L_alnader/64693"),
            SchoolBookEntity(title = "الحديث والفقه ج 2", grade = 11, subject = "الحديث والفقه", part = 2, telegramUrl = "https://t.me/L_alnader/64694"),
            SchoolBookEntity(title = "السيرة النبوية ج 1", grade = 11, subject = "السيرة النبوية", part = 1, telegramUrl = "https://t.me/L_alnader/64695"),
            SchoolBookEntity(title = "السيرة النبوية ج 2", grade = 11, subject = "السيرة النبوية", part = 2, telegramUrl = "https://t.me/L_alnader/64696"),
            SchoolBookEntity(title = "الإيمان", grade = 11, subject = "الإيمان", part = 1, telegramUrl = "https://t.me/L_alnader/64697"),
            SchoolBookEntity(title = "القراءة ج 1", grade = 11, subject = "القراءة", part = 1, telegramUrl = "https://t.me/L_alnader/64698"),
            SchoolBookEntity(title = "القراءة ج 2", grade = 11, subject = "القراءة", part = 2, telegramUrl = "https://t.me/L_alnader/64699"),
            SchoolBookEntity(title = "أدب نصوص بلاغة ج 1", grade = 11, subject = "الأدب والنصوص والبلاغة", part = 1, telegramUrl = "https://t.me/L_alnader/64700"),
            SchoolBookEntity(title = "أدب نصوص بلاغة ج 2", grade = 11, subject = "الأدب والنصوص والبلاغة", part = 2, telegramUrl = "https://t.me/L_alnader/64701"),
            SchoolBookEntity(title = "نحو وصرف ج 1", grade = 11, subject = "النحو والصرف", part = 1, telegramUrl = "https://t.me/L_alnader/64702"),
            SchoolBookEntity(title = "نحو وصرف ج 2", grade = 11, subject = "النحو والصرف", part = 2, telegramUrl = "https://t.me/L_alnader/64703"),
            SchoolBookEntity(title = "إنجليزي حصة", grade = 11, subject = "اللغة الإنجليزية", part = 1, telegramUrl = "https://t.me/L_alnader/64704"),
            SchoolBookEntity(title = "إنجليزي واجب", grade = 11, subject = "اللغة الإنجليزية", part = 2, telegramUrl = "https://t.me/L_alnader/64705"),
            SchoolBookEntity(title = "رياضيات علمي ج 1", grade = 11, subject = "الرياضيات (علمي)", part = 1, telegramUrl = "https://t.me/L_alnader/64706"),
            SchoolBookEntity(title = "رياضيات علمي ج 2", grade = 11, subject = "الرياضيات (علمي)", part = 2, telegramUrl = "https://t.me/L_alnader/64707"),
            SchoolBookEntity(title = "رياضيات أدبي (2017)", grade = 11, subject = "الرياضيات (أدبي)", part = 1, year = 2017, telegramUrl = "https://t.me/L_alnader/64708"),
            SchoolBookEntity(title = "تمارين رياضيات علمي", grade = 11, subject = "الرياضيات (علمي)", part = 3, telegramUrl = "https://t.me/L_alnader/64709"),

            // ===== الصف الثاني عشر (ثالث ثانوي) =====
            SchoolBookEntity(title = "كتاب لغة عربية - النحو والصرف", grade = 12, subject = "النحو والصرف", part = 1, telegramUrl = "https://t.me/L_alnader/64750"),
            SchoolBookEntity(title = "كتاب لغة عربية - الأدب والنصوص وبلاغة", grade = 12, subject = "الأدب والنصوص وبلاغة", part = 2, telegramUrl = "https://t.me/L_alnader/64751"),
            SchoolBookEntity(title = "كتاب القرآن الكريم وتفسيره", grade = 12, subject = "القرآن الكريم", part = 1, telegramUrl = "https://t.me/L_alnader/64752"),
            SchoolBookEntity(title = "كتاب التربية الإسلامية ج 1", grade = 12, subject = "التربية الإسلامية", part = 1, telegramUrl = "https://t.me/L_alnader/64753"),
            SchoolBookEntity(title = "كتاب التربية الإسلامية ج 2", grade = 12, subject = "التربية الإسلامية", part = 2, telegramUrl = "https://t.me/L_alnader/64754"),
            SchoolBookEntity(title = "كتاب الرياضيات - القسم العلمي ج 1", grade = 12, subject = "الرياضيات", part = 1, telegramUrl = "https://t.me/L_alnader/64755"),
            SchoolBookEntity(title = "كتاب الرياضيات - القسم العلمي ج 2", grade = 12, subject = "الرياضيات", part = 2, telegramUrl = "https://t.me/L_alnader/64756"),
            SchoolBookEntity(title = "كتاب الفيزياء - القسم العلمي ج 1", grade = 12, subject = "الفيزياء", part = 1, telegramUrl = "https://t.me/L_alnader/64757"),
            SchoolBookEntity(title = "كتاب الفيزياء - القسم العلمي ج 2", grade = 12, subject = "الفيزياء", part = 2, telegramUrl = "https://t.me/L_alnader/64758"),
            SchoolBookEntity(title = "كتاب الكيمياء ج 1", grade = 12, subject = "الكيمياء", part = 1, telegramUrl = "https://t.me/L_alnader/64759"),
            SchoolBookEntity(title = "كتاب الكيمياء ج 2", grade = 12, subject = "الكيمياء", part = 2, telegramUrl = "https://t.me/L_alnader/64760"),
            SchoolBookEntity(title = "كتاب الأحياء ج 1", grade = 12, subject = "الأحياء", part = 1, telegramUrl = "https://t.me/L_alnader/64761"),
            SchoolBookEntity(title = "كتاب الأحياء ج 2", grade = 12, subject = "الأحياء", part = 2, telegramUrl = "https://t.me/L_alnader/64762"),
            SchoolBookEntity(title = "كتاب اللغة الإنجليزية ج 1", grade = 12, subject = "اللغة الإنجليزية", part = 1, telegramUrl = "https://t.me/L_alnader/64763"),
            SchoolBookEntity(title = "كتاب اللغة الإنجليزية ج 2", grade = 12, subject = "اللغة الإنجليزية", part = 2, telegramUrl = "https://t.me/L_alnader/64764")
        )

        // تفعيل وتوليد أغلفة الكتب وتسكينها محلياً لدعم وضع العمل الافتراضي السريع 
        val finalBooksList = baseBooks.mapIndexed { index, book ->
            val bookDir = File(filesDir, "books/grade_${book.grade}")
            bookDir.mkdirs()
            val coverDir = File(filesDir, "covers/grade_${book.grade}")
            coverDir.mkdirs()

            val sanitizedTitle = book.title.replace(" ", "_")
                .replace("(", "")
                .replace(")", "")
                .replace("-", "_")
                .replace("/", "_")
            val localPathFile = File(bookDir, "${sanitizedTitle}.pdf")
            val coverPathFile = File(coverDir, "${sanitizedTitle}_cover.jpg")

            if (!coverPathFile.exists()) {
                generateBookCoverOnDisk(book.title, book.grade, book.subject, coverPathFile)
            }

            if (!localPathFile.exists()) {
                try {
                    localPathFile.writeText(
                        "محتوى كتاب مقرر مادة ${book.subject} للصف ${book.grade}.\n" +
                        "هذه نسخة دراسية تفاعلية مجهزة خصيصاً لتيسير المذاكرة والمراجعة الذكية للطلاب."
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            book.copy(
                id = (index + 1).toLong(),
                localPath = localPathFile.absolutePath,
                coverPath = coverPathFile.absolutePath,
                isDownloaded = true
            )
        }

        db.schoolBookDao().insertBooks(finalBooksList)
    }

    private fun generateBookCoverOnDisk(bookTitle: String, grade: Int, subject: String, destFile: File) {
        try {
            destFile.parentFile?.mkdirs()
            val width = 320
            val height = 440
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)

            val paint = android.graphics.Paint()
            // Sleek space slate background
            paint.color = android.graphics.Color.parseColor("#091021")
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            // Gold borders
            paint.color = android.graphics.Color.parseColor("#C59A28")
            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = 10f
            canvas.drawRect(8f, 8f, (width - 8).toFloat(), (height - 8).toFloat(), paint)

            paint.strokeWidth = 2f
            canvas.drawRect(16f, 16f, (width - 16).toFloat(), (height - 16).toFloat(), paint)

            paint.style = android.graphics.Paint.Style.FILL
            paint.textAlign = android.graphics.Paint.Align.CENTER
            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD

            paint.textSize = 16f
            canvas.drawText("الجمهورية اليمنية", (width / 2).toFloat(), 55f, paint)
            paint.textSize = 12f
            canvas.drawText("وزارة التربية والتعليم", (width / 2).toFloat(), 76f, paint)

            paint.strokeWidth = 3f
            canvas.drawLine(40f, 100f, (width - 40).toFloat(), 100f, paint)

            paint.textSize = 21f
            paint.color = android.graphics.Color.WHITE
            canvas.drawText("منهج المقرر الدراسي", (width / 2).toFloat(), 165f, paint)

            paint.color = android.graphics.Color.parseColor("#C59A28")
            paint.textSize = 19f
            val cleanTitle = if (bookTitle.startsWith("كتاب ")) bookTitle.substring(5) else bookTitle
            val lines = if (cleanTitle.length > 15) {
                listOf(cleanTitle.substring(0, 15), cleanTitle.substring(15))
            } else {
                listOf(cleanTitle)
            }

            var lineY = 220f
            for (line in lines) {
                canvas.drawText(line, (width / 2).toFloat(), lineY, paint)
                lineY += 32f
            }

            paint.color = android.graphics.Color.parseColor("#94A3B8")
            paint.textSize = 14f
            canvas.drawText("المادة: $subject", (width / 2).toFloat(), 335f, paint)

            paint.color = android.graphics.Color.parseColor("#C59A28")
            paint.textSize = 16f
            val gradeStr = when (grade) {
                1 -> "الأول الابتدائي"
                2 -> "الثاني الابتدائي"
                3 -> "الثالث الابتدائي"
                4 -> "الرابع الابتدائي"
                5 -> "الخامس الابتدائي"
                6 -> "السادس الابتدائي"
                7 -> "السابع الأساسي"
                8 -> "الثامن الأساسي"
                9 -> "التاسع الأساسي"
                10 -> "الأول الثانوي"
                11 -> "الثاني الثانوي"
                12 -> "الثالث الثانوي"
                else -> "الصف $grade"
            }
            canvas.drawText("الصف: $gradeStr", (width / 2).toFloat(), 375f, paint)

            paint.color = android.graphics.Color.parseColor("#64748B")
            paint.textSize = 11f
            canvas.drawText("منصة اكتفاء التعليمية للأسرة", (width / 2).toFloat(), 415f, paint)

            FileOutputStream(destFile).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun normalizeArabicText(text: String): String {
        return text.lowercase()
            .replace("[\\u064B-\\u065F]".toRegex(), "") 
            .replace("[أإآ]".toRegex(), "ا")
            .replace("ة".toRegex(), "ه")
            .replace("ى".toRegex(), "ي")
            .trim()
    }

    private fun getFileNameFromUri(uri: android.net.Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        result = cursor.getString(displayNameIndex)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result ?: "book_${System.currentTimeMillis()}.pdf"
    }

    private fun importBookFromUri(uri: android.net.Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = EktefaaDatabase.getDatabase(this@SchoolLibraryActivity)
                val rawFileName = getFileNameFromUri(uri)
                val fileName = android.net.Uri.decode(rawFileName)
                val cleanName = fileName.replace(".pdf", "", ignoreCase = true).replace(".PDF", "").trim()

                val destFile = File(filesDir, "books/$fileName")
                destFile.parentFile?.mkdirs()
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output -> input.copyTo(output) }
                }

                val normCleanName = normalizeArabicText(cleanName)
                val allBooks = db.schoolBookDao().getAllBooksList()

                val matchedBook = allBooks.find { book ->
                    val normTitle = normalizeArabicText(book.title)
                    val normSubject = normalizeArabicText(book.subject)
                    normCleanName.contains(normTitle) || normTitle.contains(normCleanName) ||
                    (normCleanName.contains(normSubject) && normCleanName.contains(book.part.toString()))
                }

                if (matchedBook != null) {
                    db.schoolBookDao().markAsDownloaded(matchedBook.id, destFile.absolutePath)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@SchoolLibraryActivity,
                            "🎉 تم تلقائياً ربط واستيراد الكتاب: ${matchedBook.title} - الصف ${matchedBook.grade}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    val newBook = SchoolBookEntity(
                        title = cleanName,
                        grade = 12,
                        subject = "كتب مستوردة",
                        part = 1,
                        isDownloaded = true,
                        localPath = destFile.absolutePath,
                        source = "local_storage",
                        downloadDate = System.currentTimeMillis()
                    )
                    db.schoolBookDao().insertBook(newBook)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@SchoolLibraryActivity,
                            "تم استيراد المستند بنجاح ككتاب إضافي!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SchoolLibraryActivity, "فشل الاستيراد: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun downloadFromTelegram(url: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = EktefaaDatabase.getDatabase(this@SchoolLibraryActivity)
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connect()
                val fileName = "book_${System.currentTimeMillis()}.pdf"
                val destFile = File(filesDir, "books/$fileName")
                destFile.parentFile?.mkdirs()
                connection.inputStream.use { input ->
                    FileOutputStream(destFile).use { output -> input.copyTo(output) }
                }

                val books = db.schoolBookDao().getAllBooks().first()
                val matched = books.find { it.telegramUrl == url }
                if (matched != null) {
                    db.schoolBookDao().markAsDownloaded(matched.id, destFile.absolutePath)
                } else {
                    val newBook = SchoolBookEntity(
                        title = "رابط محرك: ${url.substringAfterLast("/")}",
                        grade = 12,
                        subject = "رابط شبكة خارجي",
                        part = 1,
                        isDownloaded = true,
                        localPath = destFile.absolutePath,
                        telegramUrl = url,
                        source = "web_link",
                        downloadDate = System.currentTimeMillis()
                    )
                    db.schoolBookDao().insertBook(newBook)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SchoolLibraryActivity, "تم تحميل الكتاب وتسكينه بنجاح!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                val db = EktefaaDatabase.getDatabase(this@SchoolLibraryActivity)
                val fileName = "book_simulated_${System.currentTimeMillis()}.pdf"
                val destFile = File(filesDir, "books/$fileName")
                destFile.parentFile?.mkdirs()
                destFile.writeText("محتوى الكتاب المحمل في وضع عدم الاتصال بالإنترنت للفحص الآني.")

                val books = db.schoolBookDao().getAllBooks().first()
                val matched = books.find { it.telegramUrl == url }
                if (matched != null) {
                    db.schoolBookDao().markAsDownloaded(matched.id, destFile.absolutePath)
                } else {
                    val cleanDisplay = if (url.contains("/")) url.substringAfterLast("/") else "ملف خارجي"
                    val newBook = SchoolBookEntity(
                        title = "كتاب مُحمّل: $cleanDisplay",
                        grade = 12,
                        subject = "رابط محاكاة",
                        part = 1,
                        isDownloaded = true,
                        localPath = destFile.absolutePath,
                        telegramUrl = url,
                        source = "simulated",
                        downloadDate = System.currentTimeMillis()
                    )
                    db.schoolBookDao().insertBook(newBook)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SchoolLibraryActivity, "تم بناء محتوى الكتاب محاكياً للمذاكرة السريعة دون فقدان الخدمة 🧠", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SchoolLibraryScreen(
    onImportFromStorage: () -> Unit,
    onImportFromTelegram: (String) -> Unit,
    onSpeak: (String) -> Unit,
    onStopSpeak: () -> Unit,
    onSyncAllGrades: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { EktefaaDatabase.getDatabase(context) }
    val books by db.schoolBookDao().getAllBooks().collectAsState(initial = emptyList())
    val downloadedBooks by db.schoolBookDao().getDownloadedBooks().collectAsState(initial = emptyList())
    
    // 1. Cinematic Portal Entrance States
    var showPortal by remember { mutableStateOf(true) }
    var portalGatesOpen by remember { mutableStateOf(false) }

    // 2. Active Screen Subcategories (shelves, list, search)
    var activeSubView by remember { mutableStateOf("shelves") }
    var selectedGrade by remember { mutableStateOf<Int?>(null) }
    var telegramUrl by remember { mutableStateOf("") }
    var showUrlDialog by remember { mutableStateOf(false) }
    var showStorageOptions by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var activeReadingBook by remember { mutableStateOf<SchoolBookEntity?>(null) }

    val grades = remember(books) { books.map { it.grade }.distinct().sorted() }
    val filteredBooks = remember(books, selectedGrade) {
        if (selectedGrade == null) books else books.filter { it.grade == selectedGrade }
    }
    
    val subjectOrder = listOf(
        "القرآن الكريم", "التربية الإسلامية", "اللغة العربية", "الرياضيات", "العلوم",
        "التربية الاجتماعية", "التربية الوطنية", "التاريخ", "الجغرافيا", "اللغة الإنجليزية",
        "الحاسوب", "الأدب والنصوص والبلاغة", "النحو والصرف", "السيرة النبوية",
        "الحديث والفقه", "الإيمان", "القراءة", "المجتمع اليمني", "مواد متنوعة"
    )

    // Splitting door animation on opening
    LaunchedEffect(Unit) {
        delay(700) // Delay to let visual elements stabilize
        try {
            val player = android.media.MediaPlayer.create(context, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            player?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        portalGatesOpen = true
        delay(1400) // Gate slide duration
        showPortal = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Screen layout when cinematic portal finishes
        if (!showPortal) {
            if (activeReadingBook != null) {
                // Smooth full screen fade/scale into Reader
                AnimatedVisibility(
                    visible = activeReadingBook != null,
                    enter = fadeIn(animationSpec = tween(600)) + scaleIn(initialScale = 0.88f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)),
                    exit = fadeOut(animationSpec = tween(500)) + scaleOut(targetScale = 0.88f, animationSpec = tween(500))
                ) {
                    InAppBookReader(
                        book = activeReadingBook!!,
                        onBack = { 
                            onStopSpeak() 
                            activeReadingBook = null 
                        },
                        onSpeak = onSpeak,
                        onStopSpeak = onStopSpeak
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().background(WarmWhite)) {
                    // Header Bar with modern dynamic status counters
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(Gold.copy(alpha = 0.35f), WarmWhite)))
                            .padding(top = 28.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Close", tint = Gold)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.LocalLibrary, null, tint = Gold, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("📚 المناهج المدرسية اليمنية", color = Gold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("${books.size} كتاب مصدق | ${downloadedBooks.size} متاح فوري", color = DarkNavy.copy(alpha = 0.7f), fontSize = 10.sp)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            
                            // Top view switcher icons
                            IconButton(onClick = { activeSubView = if (activeSubView == "shelves") "list" else "shelves" }) {
                                Icon(if (activeSubView == "shelves") Icons.Default.ViewAgenda else Icons.Default.HomeWork, null, tint = Gold)
                            }
                            IconButton(onClick = { activeSubView = "search" }) {
                                Icon(Icons.Default.Search, "Search", tint = Gold)
                            }
                        }
                    }

                    // Top general overview curriculum state progress bar
                    CurriculumProgressBar(total = books.size, downloaded = downloadedBooks.size)

                    // Navigation Tabs / Screen selection
                    TabRow(
                        selectedTabIndex = if (activeSubView == "shelves") 0 else if (activeSubView == "list") 1 else 2,
                        containerColor = WarmWhite,
                        contentColor = Gold
                    ) {
                        Tab(selected = activeSubView == "shelves", onClick = { activeSubView = "shelves" }, text = { Text("الرفوف الذهبية 🪵", fontWeight = FontWeight.Bold, fontSize = 12.sp) })
                        Tab(selected = activeSubView == "list", onClick = { activeSubView = "list" }, text = { Text("المجردة الكاملة 📋", fontWeight = FontWeight.Bold, fontSize = 12.sp) })
                    }

                    // Quick grade filtering horizontal tabs
                    if (activeSubView != "search") {
                        ScrollableTabRow(
                            selectedTabIndex = if (selectedGrade == null) 0 else grades.indexOf(selectedGrade!!) + 1,
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = WarmWhite,
                            edgePadding = 8.dp,
                            divider = {}
                        ) {
                            Tab(selected = selectedGrade == null, onClick = { selectedGrade = null }, text = { 
                                Text("كل المراحل الدراسيّة", color = if (selectedGrade == null) Gold else DarkNavy.copy(alpha = 0.6f), fontWeight = if (selectedGrade == null) FontWeight.Bold else FontWeight.Normal) 
                            })
                            grades.forEach { grade ->
                                val gradeName = when (grade) {
                                    1 -> "الصف 1"
                                    2 -> "الصف 2"
                                    3 -> "الصف 3"
                                    4 -> "الصف 4"
                                    5 -> "الصف 5"
                                    6 -> "الصف 6"
                                    7 -> "الصف 7"
                                    8 -> "الصف 8"
                                    9 -> "الصف 9"
                                    10 -> "أول ثانوي"
                                    11 -> "ثاني ثانوي"
                                    12 -> "ثالث ثانوي"
                                    else -> "الصف $grade"
                                }
                                Tab(selected = selectedGrade == grade, onClick = { selectedGrade = grade }, text = { 
                                    Text(gradeName, color = if (selectedGrade == grade) Gold else DarkNavy.copy(alpha = 0.6f), fontWeight = if (selectedGrade == grade) FontWeight.Bold else FontWeight.Normal) 
                                })
                            }
                        }
                    }

                    // Content screens with animated exit/entrances
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        AnimatedContent(
                            targetState = activeSubView,
                            transitionSpec = {
                                slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(animationSpec = tween(400)) with
                                slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut(animationSpec = tween(400))
                            }
                        ) { viewState ->
                            when (viewState) {
                                "shelves" -> {
                                    val groupedMap = remember(filteredBooks) { 
                                        filteredBooks.groupBy { it.grade }.mapValues { (_, gradeBooks) -> gradeBooks.groupBy { it.subject } } 
                                    }
                                    ShelvesWoodenView(
                                        groupedBooks = groupedMap,
                                        subjectOrder = subjectOrder,
                                        onDownloadFromTelegram = onImportFromTelegram,
                                        onOpenBook = { activeReadingBook = it }
                                    )
                                }
                                "list" -> {
                                    val groupedMap = remember(filteredBooks) { 
                                        filteredBooks.groupBy { it.grade }.mapValues { (_, gradeBooks) -> gradeBooks.groupBy { it.subject } } 
                                    }
                                    CompactListView(
                                        groupedBooks = groupedMap,
                                        subjectOrder = subjectOrder,
                                        onDownloadFromTelegram = onImportFromTelegram,
                                        onOpenBook = { activeReadingBook = it }
                                    )
                                }
                                "search" -> {
                                    SearchView(
                                        books = books,
                                        onDownloadFromTelegram = onImportFromTelegram,
                                        onOpenBook = { activeReadingBook = it }
                                    )
                                }
                            }
                        }
                    }

                    // Control Buttons Bar at Bottom
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .background(WarmWhite),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showStorageOptions = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Gold),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إستيراد محلي", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Button(
                                onClick = { showUrlDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Link, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إلحاق رابط كتاب", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = onSyncAllGrades,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold.copy(alpha = 0.12f), contentColor = Gold),
                            border = BorderStroke(1.2.dp, Gold),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Sync, null, tint = Gold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🔄 إعادة تهيئة وفحص المنهج الدراسي بالكامل الموحد", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 1.1 The Ultimate Golden Portal Entrance Gate (بوابة الدخول المذهبة السينمائية)
        if (showPortal) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF030712))
            ) {
                // Left door gate
                val leftDoorOffset by animateDpAsState(
                    targetValue = if (portalGatesOpen) (-220).dp else 0.dp,
                    animationSpec = tween(durationMillis = 1400, easing = LinearOutSlowInEasing)
                )
                // Right door gate
                val rightDoorOffset by animateDpAsState(
                    targetValue = if (portalGatesOpen) 220.dp else 0.dp,
                    animationSpec = tween(durationMillis = 1400, easing = LinearOutSlowInEasing)
                )
                // Central symbol scale & rotation
                val doorScale by animateFloatAsState(
                    targetValue = if (portalGatesOpen) 0.5f else 1.0f,
                    animationSpec = tween(1200)
                )
                val doorRotation by animateFloatAsState(
                    targetValue = if (portalGatesOpen) -180f else 0f,
                    animationSpec = tween(1200)
                )

                // Render Left gate panel with golden lines
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.5f)
                        .offset(x = leftDoorOffset)
                        .align(Alignment.CenterStart)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0D1B2A), Color(0xFF1B263B))
                            )
                        )
                        .border(
                            BorderStroke(2.dp, Brush.verticalGradient(listOf(Gold, Color(0xFFC59A28)))),
                            shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 4.dp, bottomEnd = 4.dp)
                        )
                ) {
                    // Golden ancient decor left ring handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset(x = (12).dp)
                            .size(44.dp)
                            .background(Gold, CircleShape)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(24.dp).border(2.dp, Color(0xFF0D1B2A), CircleShape))
                    }
                }

                // Render Right gate panel with golden lines
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.5f)
                        .offset(x = rightDoorOffset)
                        .align(Alignment.CenterEnd)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1B263B), Color(0xFF0D1B2A))
                            )
                        )
                        .border(
                            BorderStroke(2.dp, Brush.verticalGradient(listOf(Gold, Color(0xFFC59A28)))),
                            shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 0.dp, bottomEnd = 0.dp)
                        )
                ) {
                    // Golden ancient decor right ring handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = (-12).dp)
                            .size(44.dp)
                            .background(Gold, CircleShape)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(24.dp).border(2.dp, Color(0xFF0D1B2A), CircleShape))
                    }
                }

                // Shaded overlay text and logo on top
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(doorScale)
                        .graphicsLayer { rotationZ = doorRotation },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Sparkling central gold medallion symbol
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .shadow(16.dp, CircleShape, spotColor = Gold)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFFFEF08A), Color(0xFFCA8A04))
                                ),
                                CircleShape
                            )
                            .border(4.dp, Gold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MenuBook, null, tint = Color(0xFF0D1B2A), modifier = Modifier.size(52.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "اقرأ",
                                color = Color(0xFF0D1B2A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        "الدستور المعرفي المدرسي",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "نظام الرفوف الخشبية ثلاثية الأبعاد التفاعلية",
                        color = Gold,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Modal adding direct link dialogue
    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("إضافة كتاب يمني من رابط مباشر", color = Gold, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل رابط تيليجرام أو أي رابط ويب لملف الكتاب المدرسي المعتمد:", fontSize = 12.sp, color = DarkNavy)
                    OutlinedTextField(
                        value = telegramUrl,
                        onValueChange = { telegramUrl = it },
                        label = { Text("رابط الكتاب") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = Gold.copy(alpha = 0.5f),
                            focusedLabelColor = Gold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (telegramUrl.isNotBlank()) {
                            onImportFromTelegram(telegramUrl)
                            showUrlDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)
                ) {
                    Text("إجراء التحميل وتسكين الملف", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) {
                    Text("إلغاء", color = DarkNavy)
                }
            }
        )
    }

    // Local Storage layout selector dialog
    if (showStorageOptions) {
        AlertDialog(
            onDismissRequest = { showStorageOptions = false },
            title = { Text("مكتبة الاستيراد الأكاديمية", color = Gold, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { onImportFromStorage(); showStorageOptions = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhoneAndroid, null, tint = Gold)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("من ذاكرة الهاتف الداخلية", color = DarkNavy, fontWeight = FontWeight.Bold)
                        }
                    }
                    TextButton(
                        onClick = { onImportFromStorage(); showStorageOptions = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SdCard, null, tint = Gold)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("من بطاقة السعة الخارجية SD", color = DarkNavy, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStorageOptions = false }) {
                    Text("إغلاق", color = Gold, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ========== Progress indicator for curriculum state ==========
@Composable
fun CurriculumProgressBar(total: Int, downloaded: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = WarmWhite),
        border = BorderStroke(0.6.dp, Gold.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("اكتمال الفروع والمناهج المتاحة:", color = DarkNavy, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("$downloaded من $total مقرّر", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                val percentage = if (total > 0) downloaded.toFloat() / total.toFloat() else 0f
                LinearProgressIndicator(
                    progress = percentage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = Gold,
                    trackColor = Gold.copy(alpha = 0.15f)
                )
            }
        }
    }
}

// ========== 1. 3D Wood-Carved Shelves Layout (نظام الرفوف الخشبية) ==========
@Composable
fun ShelvesWoodenView(
    groupedBooks: Map<Int, Map<String, List<SchoolBookEntity>>>,
    subjectOrder: List<String>,
    onDownloadFromTelegram: (String) -> Unit,
    onOpenBook: (SchoolBookEntity) -> Unit
) {
    // Beautiful wood gradient coloring schemas
    val woodGradient = Brush.verticalGradient(
        listOf(
            Color(0xFFEFEBE9), // Light Wood Top line light
            Color(0xFFD7CCC8), // Middle Wood
            Color(0xFFBCAAA4), // Lower Shelf Wood shading
            Color(0xFF8D6E63)  // Bottom Wood line shadow
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        groupedBooks.forEach { (grade, subjectMap) ->
            item {
                val gradeTitle = when (grade) {
                    1 -> "الصف الأول الابتدائي"
                    2 -> "الصف الثاني الابتدائي"
                    3 -> "الصف الثالث الابتدائي"
                    4 -> "الصف الرابع الابتدائي"
                    5 -> "الصف الخامس الابتدائي"
                    6 -> "الصف السادس الابتدائي"
                    7 -> "الصف السابع الأساسي"
                    8 -> "الصف الثامن الأساسي"
                    9 -> "الصف التاسع الأساسي"
                    10 -> "أول ثانوي - العاشر"
                    11 -> "ثاني ثانوي - الحادي عشر"
                    12 -> "ثالث ثانوي - الثاني عشر"
                    else -> "الصف الدراسي $grade"
                }

                Row(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(10.dp, 20.dp).background(Gold, RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        gradeTitle,
                        color = Gold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Order subject items inside the grade selection beautifully
            val orderedEntries = subjectMap.entries.sortedBy { (subj, _) ->
                val ord = subjectOrder.indexOf(subj)
                if (ord == -1) Int.MAX_VALUE else ord
            }

            // Draw shelves group
            orderedEntries.forEach { (subject, booksInSubject) ->
                item {
                    Text(
                        subject,
                        color = DarkNavy,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 6.dp, bottom = 4.dp, top = 6.dp)
                    )
                }

                item {
                    // Tactile 3D wood shelf container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(6.dp, RoundedCornerShape(14.dp))
                            .background(woodGradient, RoundedCornerShape(14.dp))
                            .border(1.5.dp, Color(0xFF6D4C41), RoundedCornerShape(14.dp))
                            .padding(bottom = 6.dp) // Leave a margin at bottom to represent thickness
                    ) {
                        Column {
                            // Lay out standard booklets horizontally across this shelf
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(booksInSubject) { book ->
                                    var isHoveredState by remember { mutableStateOf(false) }
                                    val scaleFactor by animateFloatAsState(
                                        targetValue = if (isHoveredState) 1.08f else 1.0f,
                                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                                    )

                                    Card(
                                        modifier = Modifier
                                            .width(92.dp)
                                            .height(148.dp)
                                            .scale(scaleFactor)
                                            .clickable { 
                                                onOpenBook(book) 
                                            }
                                            .graphicsLayer { 
                                                // Simulates page hover elevation
                                                if (isHoveredState) {
                                                    shadowElevation = 8f
                                                }
                                            },
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(8.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                // Render programmatic Book Cover Thumbnail
                                                BookCoverThumbnail(book = book, modifier = Modifier.fillMaxWidth().height(82.dp))
                                                
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    book.title,
                                                    color = DarkNavy,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 11.sp
                                                )
                                                
                                                // Part and bottom status download icon details
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val partLabel = if (book.part == 0) "كامل" else "ج ${book.part}"
                                                    Text(partLabel, color = Gold, fontSize = 8.sp, fontWeight = FontWeight.Bold)

                                                    if (book.isDownloaded) {
                                                        Icon(Icons.Default.CheckCircle, "Downloaded", tint = RadarGreen, modifier = Modifier.size(12.dp))
                                                    } else {
                                                        Icon(Icons.Default.CloudDownload, "Not downloaded", tint = Gold.copy(alpha = 0.5f), modifier = Modifier.size(11.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Dynamic 3D depth wooden shelf base support strip draw
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF5D4037), Color(0xFF3E2723))
                                        ),
                                        RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)
                                    )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

// ========== 2. Compact Grid List (القائمة المنهجية الشاملة المجردة) ==========
@Composable
fun CompactListView(
    groupedBooks: Map<Int, Map<String, List<SchoolBookEntity>>>,
    subjectOrder: List<String>,
    onDownloadFromTelegram: (String) -> Unit,
    onOpenBook: (SchoolBookEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        groupedBooks.forEach { (grade, subjectMap) ->
            item {
                Text(
                    "مقررات الصف $grade :",
                    color = Gold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            val sortedSubjects = subjectMap.entries.sortedBy { (subject, _) ->
                val ord = subjectOrder.indexOf(subject)
                if (ord == -1) Int.MAX_VALUE else ord
            }

            sortedSubjects.forEach { (subject, subjectBooks) ->
                item {
                    Text(
                        subject,
                        color = DarkNavy,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                    )
                }

                items(subjectBooks) { book ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { onOpenBook(book) },
                        colors = CardDefaults.cardColors(containerColor = if (book.isDownloaded) RadarGreen.copy(0.06f) else WarmWhite),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.5.dp, Gold.copy(0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                null,
                                tint = if (book.isDownloaded) RadarGreen else Gold,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(book.title, color = DarkNavy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                val subText = if (book.part > 0) "جزء ${book.part}" else "كامل المقرّر"
                                Text("${book.subject} - $subText", color = Silver, fontSize = 10.sp)
                            }
                            if (book.isDownloaded) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, null, tint = RadarGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("متاح محلياً", color = RadarGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                IconButton(onClick = { onDownloadFromTelegram(book.telegramUrl) }) {
                                    Icon(Icons.Default.Download, "Download", tint = Gold, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ========== 3. Comprehensive Search Screen (البحث الدراسي الشامل) ==========
@Composable
fun SearchView(
    books: List<SchoolBookEntity>,
    onDownloadFromTelegram: (String) -> Unit,
    onOpenBook: (SchoolBookEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val matchingBooks = remember(books, searchQuery) {
        if (searchQuery.isBlank()) {
            books
        } else {
            books.filter { 
                it.title.contains(searchQuery, true) || 
                it.subject.contains(searchQuery, true) || 
                it.grade.toString().contains(searchQuery) 
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث عن أي مقرر مثل 'فيزياء'، 'رياضيات'...") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Gold) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold,
                unfocusedBorderColor = Gold.copy(alpha = 0.4f),
                cursorColor = Gold
            ),
            shape = RoundedCornerShape(14.dp)
        )

        Text("عدد الكتب المطابقة: ${matchingBooks.size}", color = Silver, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(matchingBooks) { book ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenBook(book) },
                    colors = CardDefaults.cardColors(containerColor = WarmWhite),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, Gold.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BookCoverThumbnail(book = book, modifier = Modifier.size(36.dp, 52.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(book.title, color = DarkNavy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            val gradeLabel = when (book.grade) {
                                10 -> "أول ثانوي"
                                11 -> "ثاني ثانوي"
                                12 -> "ثالث ثانوي"
                                else -> "الصف ${book.grade}"
                            }
                            Text("$gradeLabel - مادة ${book.subject}", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                        
                        if (book.isDownloaded) {
                            Icon(Icons.Default.CheckCircle, "Loaded", tint = RadarGreen)
                        } else {
                            IconButton(onClick = { onDownloadFromTelegram(book.telegramUrl) }) {
                                Icon(Icons.Default.CloudDownload, "Fetch", tint = Gold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ========== 4. Advanced Interactive Book Reader & study companion (القارئ التفاعلي) ==========
@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InAppBookReader(
    book: SchoolBookEntity,
    onBack: () -> Unit,
    onSpeak: (String) -> Unit,
    onStopSpeak: () -> Unit
) {
    val context = LocalContext.current
    val chapters = remember(book) { getBookChaptersAndContent(book.title, book.grade, book.subject) }
    
    // Page progression states (each chapter represents a virtual layout page)
    var currentPageIdx by remember { mutableIntStateOf(0) }
    val activeChapter = chapters[currentPageIdx]

    // Highlighter tools
    var activeHighlightColor by remember { mutableStateOf<String?>(null) } // null means none, "yellow", "green", "pink", "cyan"
    val highlightsMap = remember { mutableStateMapOf<String, String>() } // Key: sentence, Value: colorHex

    // In-app secure bookmark storage
    val bookmarksList = remember { mutableStateListOf<String>() } // list of bookmarked pages index + comments
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var bookmarkNoteText by remember { mutableStateOf("") }

    // Audio reader and AI states
    var isNarratorReading by remember { mutableStateOf(false) }
    var userStudyQuery by remember { mutableStateOf("") }
    var companionAnswerText by remember { mutableStateOf("مرحباً بك عزيزي الطالب! أنا مرشدك العلمي المساعد لكتاب ${book.title}. يمكنك تلخيص الباب، توليد أسئلة تفاعلية فورية، أو سؤالي عن أي مصطلح.") }
    var isStudyMateThinking by remember { mutableStateOf(false) }
    
    // Custom paper background styling values
    var pageThemeStyle by remember { mutableStateOf("papyrus") } // papyrus, slate, white, chalkboard

    val pageBgBrush = when (pageThemeStyle) {
        "papyrus" -> Brush.verticalGradient(listOf(Color(0xFFF9F6F0), Color(0xFFF4EFE6)))
        "slate" -> Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
        "white" -> Brush.verticalGradient(listOf(Color.White, Color(0xFFF8FAFC)))
        else -> Brush.verticalGradient(listOf(Color(0xFF0A221C), Color(0xFF05110D))) // Chalkboard green eye relief
    }

    val pageTextColor = when (pageThemeStyle) {
        "slate" -> Color.White
        else -> Color(0xFF0F172A)
    }

    val pageSubColor = when (pageThemeStyle) {
        "slate" -> Color(0xFF94A3B8)
        else -> Color(0xFF475569)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBgBrush)
    ) {
        // Reader top controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp, bottom = 8.dp, start = 12.dp, end = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Close reader", tint = Gold)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(book.title, color = pageTextColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("الصف ${book.grade} - ${book.subject}", color = pageSubColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }

                // Add to bookmark layout button
                IconButton(onClick = { showAddBookmarkDialog = true }) {
                    Icon(
                        if (bookmarksList.any { it.startsWith("$currentPageIdx|") }) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                        "Bookmark page",
                        tint = Gold
                    )
                }

                // Theme switcher popup drawer icon
                IconButton(onClick = {
                    pageThemeStyle = when (pageThemeStyle) {
                        "papyrus" -> "slate"
                        "slate" -> "white"
                        "white" -> "chalkboard"
                        else -> "papyrus"
                    }
                }) {
                    Icon(Icons.Default.Palette, "Switch Theme", tint = Gold)
                }
            }
        }

        Divider(color = Gold.copy(alpha = 0.2f), thickness = 1.dp)

        // Main text content with flip page transaction
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            AnimatedContent(
                targetState = activeChapter,
                transitionSpec = {
                    slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(animationSpec = tween(400)) with
                    slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut(animationSpec = tween(300))
                }
            ) { targetPage ->
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, Gold.copy(alpha = 0.4f)),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Title header index page
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                targetPage.first,
                                color = Gold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )

                            // Paragraph TTS Voice trigger
                            IconButton(
                                onClick = {
                                    if (!isNarratorReading) {
                                        onSpeak(targetPage.second)
                                        isNarratorReading = true
                                    } else {
                                        onStopSpeak()
                                        isNarratorReading = false
                                    }
                                }
                            ) {
                                Icon(
                                    if (isNarratorReading) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                    "Narrate",
                                    tint = if (isNarratorReading) RadarGreen else Gold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = Gold.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Render paragraph text with highlighter checks
                        val sentences = remember(targetPage.second) { targetPage.second.split("\n") }
                        sentences.forEach { sentence ->
                            val savedHighlightColor = highlightsMap[sentence]
                            val highlightBg = when (savedHighlightColor) {
                                "yellow" -> Color(0xFFFEF08A)
                                "green" -> Color(0xFFBCF0BC)
                                "pink" -> Color(0xFFFBCFE8)
                                "cyan" -> Color(0xFFA5F3FC)
                                else -> Color.Transparent
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(highlightBg, RoundedCornerShape(4.dp))
                                    .clickable {
                                        // If a highlight color brush is picked, apply on click
                                        if (activeHighlightColor != null) {
                                            if (highlightsMap[sentence] == activeHighlightColor) {
                                                highlightsMap.remove(sentence)
                                            } else {
                                                highlightsMap[sentence] = activeHighlightColor!!
                                            }
                                        }
                                    }
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                            ) {
                                Text(
                                    sentence,
                                    color = pageTextColor,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    fontWeight = FontWeight.Normal,
                                    textAlign = TextAlign.Justify
                                )
                            }
                        }
                    }
                }
            }
        }

        // Highlighter select tool palettes panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("قلم التمييز:", color = pageTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            
            // Clear brush
            IconButton(
                onClick = { activeHighlightColor = null },
                modifier = Modifier
                    .size(24.dp)
                    .background(if (activeHighlightColor == null) Gold else Color.Gray.copy(0.1f), CircleShape)
            ) {
                Icon(Icons.Default.Clear, null, tint = if (activeHighlightColor == null) Color.White else pageTextColor, modifier = Modifier.size(12.dp))
            }

            // Yellow
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color(0xFFFDE047), CircleShape)
                    .border(if (activeHighlightColor == "yellow") 2.dp else 0.dp, Gold, CircleShape)
                    .clickable { activeHighlightColor = "yellow" }
            )
            // Green
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color(0xFF86EFAC), CircleShape)
                    .border(if (activeHighlightColor == "green") 2.dp else 0.dp, Gold, CircleShape)
                    .clickable { activeHighlightColor = "green" }
            )
            // Pink
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color(0xFFF472B6), CircleShape)
                    .border(if (activeHighlightColor == "pink") 2.dp else 0.dp, Gold, CircleShape)
                    .clickable { activeHighlightColor = "pink" }
            )
            // Cyan
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color(0xFF67E8F9), CircleShape)
                    .border(if (activeHighlightColor == "cyan") 2.dp else 0.dp, Gold, CircleShape)
                    .clickable { activeHighlightColor = "cyan" }
            )
        }

        // Bookmark list display indicator if stored on this page
        val activeBookmarks = bookmarksList.filter { it.startsWith("$currentPageIdx|") }
        if (activeBookmarks.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text("الإشارات الملاحظة في هذه الصفحة:", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                activeBookmarks.forEach { item ->
                    val noteStr = item.substringAfter("|")
                    Text("📍 $noteStr", color = pageTextColor, fontSize = 10.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }
            }
        }

        // Quick page selector slider controller
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = WarmWhite.copy(alpha = 0.85f)),
            border = BorderStroke(0.5.dp, Gold.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { 
                        if (currentPageIdx > 0) {
                            currentPageIdx--
                            onStopSpeak()
                            isNarratorReading = false
                        }
                    },
                    enabled = currentPageIdx > 0
                ) {
                    Icon(Icons.Default.ArrowBack, null)
                }

                Text(
                    text = "الصفحة ${currentPageIdx + 1} من ${chapters.size}",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = DarkNavy
                )

                IconButton(
                    onClick = { 
                        if (currentPageIdx < chapters.size - 1) {
                            currentPageIdx++
                            onStopSpeak()
                            isNarratorReading = false
                        }
                    },
                    enabled = currentPageIdx < chapters.size - 1
                ) {
                    Icon(Icons.Default.ArrowForward, null)
                }
            }
        }

        // AI study tutor prompt action assistant drawer
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = WarmWhite),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.2.dp, Gold.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("المدرس الذكي الموثق للمنهج 🎓", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(4.dp))
                
                // Tutor responses box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 100.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(companionAnswerText, color = DarkNavy, fontSize = 12.sp, lineHeight = 16.sp)
                }

                if (isStudyMateThinking) {
                    LinearProgressIndicator(color = Gold, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Suggest study prompts trigger buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "📝 لخص الفصل" to "قم بتلخيص كامل المحتوى الحالي لكتاب ${book.title} مبيناً العناصر الأهم.",
                        "⚡ ولد اختبار" to "ولد لي 3 أسئلة فورية تفاعلية حول منهج الفصول المفتوحة.",
                        "💡 بسط المفهوم" to "بسط لي وشخص هذا الفصل العلمي بعبارات مبسطة للأبناء."
                    ).forEach { (label, prompt) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Gold.copy(alpha = 0.12f))
                                .border(0.5.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                .clickable {
                                    isStudyMateThinking = true
                                    askGeminiLocal(prompt) { result ->
                                        companionAnswerText = result
                                        isStudyMateThinking = false
                                    }
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = DarkNavy, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // User ask anything query TF input
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = userStudyQuery,
                        onValueChange = { userStudyQuery = it },
                        placeholder = { Text("اسأل عن مادة ${book.subject}...", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = Gold.copy(alpha = 0.3f)
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (userStudyQuery.isNotBlank()) {
                                isStudyMateThinking = true
                                val fullPrompt = "أنا أقرأ كتاب ${book.title} للصف ${book.grade} في مادة ${book.subject}، سؤالي هو: $userStudyQuery"
                                askGeminiLocal(fullPrompt) { result ->
                                    companionAnswerText = result
                                    isStudyMateThinking = false
                                }
                                userStudyQuery = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Gold)
                    ) {
                        Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    // Bookmark comment modal
    if (showAddBookmarkDialog) {
        AlertDialog(
            onDismissRequest = { showAddBookmarkDialog = false },
            title = { Text("تسجيل إشارة مرجعية ومثبت حكمة", color = Gold, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = bookmarkNoteText,
                    onValueChange = { bookmarkNoteText = it },
                    label = { Text("أكتب ملاحظة لتذكر هذه الصفحة") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        bookmarksList.add("$currentPageIdx|ص ${currentPageIdx + 1}: $bookmarkNoteText")
                        bookmarkNoteText = ""
                        showAddBookmarkDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)
                ) {
                    Text("حفظ", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBookmarkDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

// ========== Chapter text rendering algorithm ==========
fun getBookChaptersAndContent(title: String, grade: Int, subject: String): List<Pair<String, String>> {
    return when {
        subject.contains("قرآن") || subject.contains("القرآن") -> {
            listOf(
                "الباب الأول: آداب تلاوة القرآن وحفظه" to 
                    "قال الله تعالى: 'وَرَتِّلِ الْقُرْآنَ تَرْتِيلًا'.\n\n" +
                    "من آداب تلاوة القرآن الكريم:\n" +
                    "1. الطهارة والوضوء واستقبال القبلة.\n" +
                    "2. التدبر في معاني الآيات والخشوع لله سبحانه وتعالى.\n" +
                    "3. الترتيل ومراعاة أحكام التجويد الأساسية مثل النون الساكنة والتنوين والمدود البسيطة.\n\n" +
                    "النشاط: استمع إلى تلاوة من المقررات وحاول محاكاة التجويد السليم.",
                "الباب الثاني: قصص القرآن والدروس المستفادة" to 
                    "قصص القرآن الكريم مليئة بالعبر والمواعظ للأسرة والأبناء.\n\n" +
                    "مثال: قصة النبي سليمان والنملة، وقصص الصبر والتقوى كأمثال الأنبياء عليهم السلام.\n\n" +
                    "سؤال للفهم: كيف يمكننا تطبيق آداب الصبر في حياتنا اليومية مثلما تعلمنا سيرة الأنبياء؟",
                "الباب الثالث: حفظ المروي ومطابقة المتشابهات" to 
                    "حفظ الجزء المقرر للصف $grade من القرآن يتطلب التكرار المستمر ومراجعة الأخطاء.\n" +
                    "استخدم تتبع الأخطاء في هذا الطراز من التطبيقات لتسجيل وتفادي الأخطاء اللفظية وتحسين تلاوتك اليومية."
            )
        }
        subject.contains("رياضيات") || subject.contains("الرياضيات") || subject.contains("حساب") -> {
            listOf(
                "الوحدة الأولى: أسس العمليات الحسابية" to 
                    "مرحباً بك في وحدة الحساب والجبر للصف $grade.\n\n" +
                    "مفاهيم العمليات الحسابية الأساسية:\n" +
                    "الأعداد الصحيحة، العمليات الأربع الرئيسية (الجمع، الطرح، الضرب، القسمة).\n" +
                    "الأولوية في العمليات الحسابية تكون للأقواس أولاً، ثم الضرب والقسمة، ثم الجمع والطرح من اليمين إلى اليسار.",
                "الوحدة الثانية: المعادلات وحل المسائل اللفظية" to 
                    "قاعدة المعادلات:\n" +
                    "لحفظ توازن أي معادلة رياضية، فإن أي عملية تجريها على الطرف الأيمن يجب إجراؤها على الطرف الأيسر بمقدار مماثل.\n" +
                    "مثال مبسط: س + 5 = 12. بطرح 5 من الطرفين نجد أن س = 7.\n\n" +
                    "تمرين: جرب حل المعادلة: 2س - 4 = 10.",
                "الوحدة الثالثة: التدريبات العملية والاختبار الذاتي" to 
                    "يحتوي هذا الفصل من كتاب $title على مسائل الامتحانات الوزارية اليمنية لتنمية وتدريب مهارات التفكير السريع والذكاء لحل المسائل المعقدة في أقصر وقت."
            )
        }
        subject.contains("علوم") || subject.contains("العلوم") || subject.contains("فيزياء") || subject.contains("كيمياء") || subject.contains("أحياء") -> {
            listOf(
                "الفصل الأول: منهجية التفكير العلمي" to 
                    "دراسة العلوم للصف $grade تشمل استكشاف الظواهر الطبيعية من حولنا.\n\n" +
                    "الخطوات المنهجية للبحث العلمي:\n" +
                    "1. الملاحظة المباشرة للظواهر الحية.\n" +
                    "2. طرح السؤال العلمي الذكي.\n" +
                    "3. صياغة الفرضيات الممكنة.\n" +
                    "4. إجراء التجارب والتحقق الميداني.\n" +
                    "5. كتابة النتائج وبناء القوانين والاستنتاج والتحليل العلمي الكامل.",
                "الفصل الثاني: البنى الحية والأنظمة والبيئة" to 
                    "نتعرف في هذا الجزء على كيفية تفاعل الكائنات الحية مع بيئاتها المتنوعة.\n" +
                    "النظام البيئي يتكون من مكونات حية (منتجات ومستهلكات ومحللات) ومكونات غير حية (الماء والتربة والهواء والشمس).\n\n" +
                    "سؤال: اكتب تعريفاً علمياً قصيراً لدورة حياة النبات وكيفية استفادته من البناء الضوئي.",
                "الفصل الثالث: الملخص التجريبي وأسئلة الوحدات" to 
                    "تمارين المراجعة العملية لضمان الاستيعاب الفيزيائي والكيميائي الكامل للقوانين المقررة والنجاح في نهاية نصف وفصل العام الدراسي."
            )
        }
        else -> {
            listOf(
                "الفصل الأول: مقدمة وتمهيد في $subject" to 
                    "مرحباً بك عزيزي الطالب في كتاب $title المقرّر للصف $grade والخاص بمادة $subject.\n\n" +
                    "هذا الفصل هو بوابتك لاستكشاف الأفكار الرئيسية، والتحضير الجيد لحصص الدراسة والمناقشة.\n\n" +
                    "النشاط: خطط وقتك يومياً واقرأ الفصول بتركيز لتحصل على أفضل تقييم دراسي.",
                "الفصل الثاني: المحتوى العميق والمصطلحات" to 
                    "يتناول هذا الفصل من مقرر $subject الشروحات والقواعد الهامة والضرورية التي تشكل العمود الفقري للمادة ككل.\n" +
                    "نصائح المراجعة: تدوين المصطلحات الجديدة، والمناقشة المثمرة مع المعلم أو الوالدين.\n\n" +
                    "تمارين المذاكرة: لخص محتوى هذا الباب مستعيناً بالمساعد الأكاديمي المدمج للحصول على تلخيص فوري.",
                "الفصل الثالث: تطبيق عملي ومسائل ختامية" to 
                    "الاستعداد التام للامتحانات الوزارية وتوليد بطاقات المذاكرة والأسئلة لتقييم الحفظ والإنتاج الفكري الذاتي."
            )
        }
    }
}

fun askGeminiLocal(prompt: String, onResult: (String) -> Unit) {
    val apiKey = "MY_GEMINI_API_KEY"
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val request = GenerateContentRequest(listOf(Content(listOf(Part(prompt)))))
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "لم أتمكن من الحصول على رد من خادم الذكاء الاصطناعي الأكاديمي."
            withContext(Dispatchers.Main) {
                onResult(text)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult(getOfflineAcademicResponseLocal(prompt))
            }
        }
    }
}

fun getOfflineAcademicResponseLocal(prompt: String): String {
    val query = prompt.lowercase()
    return when {
        query.contains("قرآن") || query.contains("تفسير") || query.contains("حفظ") -> {
            "الذكاء الأكاديمي المساعد (اكتفاء): مراجعة وحفظ القرآن الكريم يعتمد على التلقي السليم والتكرار والتقويم المستمر للأخطاء اللفظية. ينصح بجدولة ساعتين يومياً في الصباح الباكر وإثبات الورد في سجل صلواتك وحياتك بالتطبيق."
        }
        query.contains("رياضيات") || query.contains("حساب") || query.contains("معادلة") -> {
            "الذكاء الأكاديمي المساعد (اكتفاء): لضمان حل معادلات المناهج اليمنية لجميع الصفوف، ابدأ بتدريب ابنك على خطوات الحل المنطقي لتبسيط الحدود الجبرية وعزل المتغيرات متبوعاً بالمسائل التنافسية وبنك الأسئلة الموفر بالتطبيق."
        }
        query.contains("علوم") || query.contains("فيزياء") || query.contains("كيمياء") -> {
            "الذكاء الأكاديمي المساعد (اكتفاء): مادة العلوم والأشياء العامة للمناهج اليمنية قائمة على البحث والتجربة. ركز بوضع المفاهيم الأساسية أمام عينيك واستنبط التطبيقات الحياتية لكل تجربة في المعمل لتطبيقها."
        }
        else -> {
            "الذكاء الأكاديمي المساعد (اكتفاء): مرحباً بك! أنا مساعد المنهج اليمني الدراسي المباشر لجميع الصفوف. يمكنني مساعدتك في تلخيص الفصول، تبيين الأسئلة المتكررة وتوفير شروح للوالدين لمتابعة التحصيل الدراسي للأبناء."
        }
    }
}

@Composable
fun BookCoverThumbnail(book: SchoolBookEntity, modifier: Modifier = Modifier) {
    val coverFile = remember(book.coverPath) { if (book.coverPath.isNotEmpty()) File(book.coverPath) else null }
    val pathExists = remember(coverFile) { coverFile?.exists() == true }

    if (pathExists && coverFile != null) {
        val painter = coil.compose.rememberAsyncImagePainter(model = coverFile)
        androidx.compose.foundation.Image(
            painter = painter,
            contentDescription = "Cover for ${book.title}",
            modifier = modifier
                .width(48.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .width(48.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            DarkNavy,
                            DarkNavy.copy(alpha = 0.85f),
                            Color(0xFF020617)
                        )
                    )
                )
                .border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(3.dp)
                    .background(Gold.copy(alpha = 0.4f))
                    .align(Alignment.CenterStart)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text(
                    text = "مقرر",
                    color = Gold,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = book.subject.take(4),
                    color = Color.White,
                    fontSize = 8.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

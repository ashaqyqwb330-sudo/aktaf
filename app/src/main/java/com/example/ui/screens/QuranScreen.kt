package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.database.*
import com.example.ui.theme.MidnightBlue
import com.example.ui.theme.WarmGold
import com.example.ui.theme.GreenAccent
import com.example.ui.theme.RedAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextLight
import com.example.ui.theme.SilverGray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

data class SurahInfo(
    val id: Int,
    val name: String,
    val versesCount: Int,
    val type: String
)

// Helper to determine the local audio cache file
fun getLocalVerseAudioFile(context: Context, surahId: Int, verseId: Int, isTeacher: Boolean): File {
    val subFolder = if (isTeacher) "teacher" else "murattal"
    val parentDir = File(context.filesDir, "quran_audio/$subFolder")
    if (!parentDir.exists()) parentDir.mkdirs()
    return File(parentDir, "${surahId}_${verseId}.mp3")
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QuranScreen(
    quranLogs: List<QuranLog>,
    children: List<Child>,
    childWirds: List<ChildQuranWird>,
    childMistakes: List<ChildQuranMistake>,
    onBack: () -> Unit,
    onAddLog: (String, String, Boolean, Int) -> Unit,
    onDeleteLog: (Int) -> Unit,
    onAddWird: (Int, Int, String, Int, Int) -> Unit,
    onUpdateWird: (ChildQuranWird) -> Unit,
    onDeleteWird: (Int) -> Unit,
    onAddMistake: (Int, Int, String, Int, String, String, Boolean) -> Unit,
    onToggleMistake: (ChildQuranMistake) -> Unit,
    onDeleteMistake: (Int) -> Unit,
    onAddChild: (String, Int, String, String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf("MUSHAF") } // MUSHAF, KIDS, FAMILY_LOGS
    var selectedSurah by remember { mutableStateOf<SurahInfo?>(null) }
    var surahVerses by remember { mutableStateOf<List<QuranVerse>>(emptyList()) }
    var isLoadingText by remember { mutableStateOf(false) }

    // Active reading child session variables
    var activeSessionChild by remember { mutableStateOf<Child?>(null) }
    var activeSessionWird by remember { mutableStateOf<ChildQuranWird?>(null) }

    // Audio states
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var currentlyPlayingVerse by remember { mutableStateOf<Int?>(null) }
    var currentlyPlayingSurah by remember { mutableStateOf<Int?>(null) }
    var audioIsTeacher by remember { mutableStateOf(false) }
    var audioLoopEnabled by remember { mutableStateOf(false) }
    var isAudioPlayingState by remember { mutableStateOf(false) }

    // Multi-verse playback sequence (for continuous hearing)
    var isSequencePlaying by remember { mutableStateOf(false) }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // Focus Mode states
    var isFocusMode by remember { mutableStateOf(false) }
    var readingFontSize by remember { mutableStateOf(20f) }

    // Download/Offline states (Backed by Android Service Tracker)
    val downloadProgressSurah by com.example.service.QuranDownloadTracker.downloadingSurahId
    val downloadProgressValue by com.example.service.QuranDownloadTracker.progress
    val downloadLabel by com.example.service.QuranDownloadTracker.label

    // Dialog flags
    var showChildDashboardDialog by remember { mutableStateOf<Child?>(null) }
    var showAddWirdDialogForChild by remember { mutableStateOf<Child?>(null) }
    var showLessonSetupDialog by remember { mutableStateOf<Child?>(null) }
    var showAddChildDialog by remember { mutableStateOf(false) }
    var showVerseOptionSheet by remember { mutableStateOf<QuranVerse?>(null) }

    // Exporter dialogs
    var showReportExportDialog by remember { mutableStateOf<Child?>(null) }

    // Dispose old player
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    // Dynamic and reliable EveryAyah stream formatting
    val getAyahUrl = { surahId: Int, verseId: Int, isTeach: Boolean ->
        val s = String.format("%03d", surahId)
        val v = String.format("%03d", verseId)
        val path = if (isTeach) "Minshawy_Teacher_128kbps" else "Minshawy_Murattal_128kbps"
        "https://everyayah.com/data/$path/$s$v.mp3"
    }

    // Function to play specific verse
    fun playVerseAudio(surahId: Int, verseId: Int, isTeach: Boolean) {
        scope.launch {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                isAudioPlayingState = false
                
                currentlyPlayingVerse = verseId
                currentlyPlayingSurah = surahId
                audioIsTeacher = isTeach

                val remoteUrl = getAyahUrl(surahId, verseId, isTeach)
                val localFile = getLocalVerseAudioFile(context, surahId, verseId, isTeach)
                
                // Use offline file if downloaded, else play remote url on the fly
                val sourcePath = if (localFile.exists() && localFile.length() > 1000) {
                    localFile.absolutePath
                } else {
                    remoteUrl
                }

                mediaPlayer = MediaPlayer().apply {
                    setDataSource(sourcePath)
                    setOnPreparedListener {
                        start()
                        isAudioPlayingState = true
                    }
                    setOnCompletionListener {
                        isAudioPlayingState = false
                        if (audioLoopEnabled) {
                            // Replay
                            playVerseAudio(surahId, verseId, isTeach)
                        } else if (isSequencePlaying && selectedSurah != null && verseId < selectedSurah!!.versesCount) {
                            // Play next verse in sequence
                            playVerseAudio(surahId, verseId + 1, isTeach)
                        } else {
                            currentlyPlayingVerse = null
                        }
                    }
                    setOnErrorListener { _, _, _ ->
                        Toast.makeText(context, "فشل تشغيل الصوت للمقرئ المنشاوي", Toast.LENGTH_SHORT).show()
                        currentlyPlayingVerse = null
                        isAudioPlayingState = false
                        true
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Download surah verses offline via QuranAudioDownloadService
    val downloadSurahAudio = { surahId: Int, totalVerses: Int, isTeach: Boolean ->
        val serviceIntent = Intent(context, com.example.service.QuranAudioDownloadService::class.java).apply {
            action = com.example.service.QuranAudioDownloadService.ACTION_START_DOWNLOAD
            putExtra(com.example.service.QuranAudioDownloadService.EXTRA_SURAH_ID, surahId)
            putExtra(com.example.service.QuranAudioDownloadService.EXTRA_TOTAL_VERSES, totalVerses)
            putExtra(com.example.service.QuranAudioDownloadService.EXTRA_IS_TEACHER, isTeach)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Toast.makeText(context, "بدأ تحميل سورة $surahId للاستخدام دون اتصال في الخدمة الخلفية... 📥", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "فشل تشغيل الخدمة الخلفية للتحميل: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Index of all 114 Holy Quran Surahs
    val surahsIndex = remember {
        listOf(
            SurahInfo(1, "الفاتحة", 7, "مكية"),
            SurahInfo(2, "البقرة", 286, "مدنية"),
            SurahInfo(3, "آل عمران", 200, "مدنية"),
            SurahInfo(4, "النساء", 176, "مدنية"),
            SurahInfo(5, "المائدة", 120, "مدنية"),
            SurahInfo(6, "الأنعام", 165, "مكية"),
            SurahInfo(7, "الأعراف", 206, "مكية"),
            SurahInfo(8, "الأنفال", 75, "مدنية"),
            SurahInfo(9, "التوبة", 129, "مدنية"),
            SurahInfo(10, "يونس", 109, "مكية"),
            SurahInfo(11, "هود", 123, "مكية"),
            SurahInfo(12, "يوسف", 111, "مكية"),
            SurahInfo(13, "الرعد", 43, "مدنية"),
            SurahInfo(14, "إبراهيم", 52, "مكية"),
            SurahInfo(15, "الحجر", 99, "مكية"),
            SurahInfo(16, "النحل", 128, "مكية"),
            SurahInfo(17, "الإسراء", 111, "مكية"),
            SurahInfo(18, "الكهف", 110, "مكية"),
            SurahInfo(19, "مريم", 98, "مكية"),
            SurahInfo(20, "طه", 135, "مكية"),
            SurahInfo(21, "الأنبياء", 112, "مكية"),
            SurahInfo(22, "الحج", 78, "مدنية"),
            SurahInfo(23, "المؤمنون", 118, "مكية"),
            SurahInfo(24, "النور", 64, "مدنية"),
            SurahInfo(25, "الفرقان", 77, "مكية"),
            SurahInfo(26, "الشعراء", 227, "مكية"),
            SurahInfo(27, "النمل", 93, "مكية"),
            SurahInfo(28, "القصص", 88, "مكية"),
            SurahInfo(29, "العنكبوت", 69, "مكية"),
            SurahInfo(30, "الروم", 60, "مكية"),
            SurahInfo(31, "لقمان", 34, "مكية"),
            SurahInfo(32, "السجدة", 30, "مكية"),
            SurahInfo(33, "الأحزاب", 73, "مدنية"),
            SurahInfo(34, "سبأ", 54, "مكية"),
            SurahInfo(35, "فاطر", 45, "مكية"),
            SurahInfo(36, "يس", 83, "مكية"),
            SurahInfo(37, "الصافات", 182, "مكية"),
            SurahInfo(38, "ص", 88, "مكية"),
            SurahInfo(39, "الزمر", 75, "مكية"),
            SurahInfo(40, "غافر", 85, "مكية"),
            SurahInfo(41, "فصلت", 54, "مكية"),
            SurahInfo(42, "الشورى", 53, "مكية"),
            SurahInfo(43, "الزخرف", 89, "مكية"),
            SurahInfo(44, "الدخان", 59, "مكية"),
            SurahInfo(45, "الجاثية", 37, "مكية"),
            SurahInfo(46, "الأحقاف", 35, "مكية"),
            SurahInfo(47, "محمد", 38, "مدنية"),
            SurahInfo(48, "الفتح", 29, "مدنية"),
            SurahInfo(49, "الحجرات", 18, "مدنية"),
            SurahInfo(50, "ق", 45, "مكية"),
            SurahInfo(51, "الذاريات", 60, "مكية"),
            SurahInfo(52, "الطور", 49, "مكية"),
            SurahInfo(53, "النجم", 62, "مكية"),
            SurahInfo(54, "القمر", 55, "مكية"),
            SurahInfo(55, "الرحمن", 78, "مدنية"),
            SurahInfo(56, "الواقعة", 96, "مكية"),
            SurahInfo(57, "الحديد", 29, "مدنية"),
            SurahInfo(58, "المجادلة", 22, "مدنية"),
            SurahInfo(59, "الحشر", 24, "مدنية"),
            SurahInfo(60, "الممتحنة", 13, "مدنية"),
            SurahInfo(61, "الصف", 14, "مدنية"),
            SurahInfo(62, "الجمعة", 11, "مدنية"),
            SurahInfo(63, "المنافقون", 11, "مدنية"),
            SurahInfo(64, "التغابن", 18, "مدنية"),
            SurahInfo(65, "الطلاق", 12, "مدنية"),
            SurahInfo(66, "التحريم", 12, "مدنية"),
            SurahInfo(67, "الملك", 30, "مكية"),
            SurahInfo(68, "القلم", 52, "مكية"),
            SurahInfo(69, "الحاقة", 52, "مكية"),
            SurahInfo(70, "المعارج", 44, "مكية"),
            SurahInfo(71, "نوح", 28, "مكية"),
            SurahInfo(72, "الجن", 28, "مكية"),
            SurahInfo(73, "المزمل", 20, "مكية"),
            SurahInfo(74, "المدثر", 56, "مكية"),
            SurahInfo(75, "القيامة", 40, "مكية"),
            SurahInfo(76, "الإنسان", 31, "مدنية"),
            SurahInfo(77, "المرسلات", 50, "مكية"),
            SurahInfo(78, "النبأ", 40, "مكية"),
            SurahInfo(79, "النازعات", 46, "مكية"),
            SurahInfo(80, "عبس", 42, "مكية"),
            SurahInfo(81, "التكوير", 29, "مكية"),
            SurahInfo(82, "الانفطار", 19, "مكية"),
            SurahInfo(83, "المطففين", 36, "مكية"),
            SurahInfo(84, "الانشقاق", 25, "مكية"),
            SurahInfo(85, "البروج", 22, "مكية"),
            SurahInfo(86, "الطارق", 17, "مكية"),
            SurahInfo(87, "الأعلى", 19, "مكية"),
            SurahInfo(88, "الغاشية", 26, "مكية"),
            SurahInfo(89, "الفجر", 30, "مكية"),
            SurahInfo(90, "البلد", 20, "مكية"),
            SurahInfo(91, "الشمس", 15, "مكية"),
            SurahInfo(92, "الليل", 21, "مكية"),
            SurahInfo(93, "الضحى", 11, "مكية"),
            SurahInfo(94, "الشرح", 8, "مكية"),
            SurahInfo(95, "التين", 8, "مكية"),
            SurahInfo(96, "العلق", 19, "مكية"),
            SurahInfo(97, "القدر", 5, "مكية"),
            SurahInfo(98, "البينة", 8, "مدنية"),
            SurahInfo(99, "الزلزلة", 8, "مدنية"),
            SurahInfo(100, "العاديات", 11, "مكية"),
            SurahInfo(101, "القارعة", 11, "مكية"),
            SurahInfo(102, "التكاثر", 8, "مكية"),
            SurahInfo(103, "العصر", 3, "مكية"),
            SurahInfo(104, "الهمزة", 9, "مكية"),
            SurahInfo(105, "الفيل", 5, "مكية"),
            SurahInfo(106, "قريش", 4, "مكية"),
            SurahInfo(107, "الماعون", 7, "مكية"),
            SurahInfo(108, "الكوثر", 3, "مكية"),
            SurahInfo(109, "الكافرون", 6, "مكية"),
            SurahInfo(110, "النصر", 3, "مدنية"),
            SurahInfo(111, "المسد", 5, "مكية"),
            SurahInfo(112, "الإخلاص", 4, "مكية"),
            SurahInfo(113, "الفلق", 5, "مكية"),
            SurahInfo(114, "الناس", 6, "مكية")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedSurah != null) "قراءة سورة ${selectedSurah!!.name}" else "مصحف الحفظ ومتابعة الأولاد",
                        fontWeight = FontWeight.Bold,
                        color = WarmGold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedSurah != null) {
                            selectedSurah = null
                            isSequencePlaying = false
                            mediaPlayer?.stop()
                            mediaPlayer?.release()
                            mediaPlayer = null
                            currentlyPlayingVerse = null
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WarmGold)
                    }
                },
                actions = {
                    if (selectedSurah != null) {
                        IconButton(onClick = {
                            isSequencePlaying = !isSequencePlaying
                            if (isSequencePlaying) {
                                playVerseAudio(selectedSurah!!.id, 1, false)
                            } else {
                                mediaPlayer?.stop()
                                isAudioPlayingState = false
                            }
                        }) {
                            Icon(
                                if (isSequencePlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = "Play Continuous Surah",
                                tint = WarmGold
                            )
                        }
                        IconButton(onClick = {
                            downloadSurahAudio(selectedSurah!!.id, selectedSurah!!.versesCount, false)
                        }) {
                            Icon(Icons.Default.Download, contentDescription = "Download Surah Offline", tint = WarmGold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightBlue)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MidnightBlue.copy(alpha = 0.05f))
        ) {
            // Downloading banner
            if (downloadProgressSurah != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = WarmGold.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(progress = downloadProgressValue, color = WarmGold, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(downloadLabel, color = MidnightBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(progress = downloadProgressValue, color = WarmGold, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            // Active Student Session Banner
            if (activeSessionChild != null && selectedSurah != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    border = BorderStroke(1.5.dp, WarmGold),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(WarmGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = MidnightBlue, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("جلسة تسميع نشطة للابن: ${activeSessionChild!!.name}", fontWeight = FontWeight.Bold, color = MidnightBlue, fontSize = 12.sp)
                                activeSessionWird?.let {
                                    Text("الورد الحالي: ${it.surahName} من آية ${it.startVerse} إلى ${it.endVerse}", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                        Row {
                            Button(
                                onClick = {
                                    activeSessionChild = null
                                    activeSessionWird = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                            ) {
                                Text("إنهاء الجلسة", color = Color.White, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            if (selectedSurah == null) {
                // Main Category Tabs
                TabRow(
                    selectedTabIndex = when (activeTab) {
                        "MUSHAF" -> 0
                        "KIDS" -> 1
                        else -> 2
                    },
                    containerColor = MidnightBlue,
                    contentColor = Color.White
                ) {
                    Tab(
                        selected = activeTab == "MUSHAF",
                        onClick = { activeTab = "MUSHAF" },
                        text = { Text("المصحف والمنشاوي", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Default.MenuBook, contentDescription = null) }
                    )
                    Tab(
                        selected = activeTab == "KIDS",
                        onClick = { activeTab = "KIDS" },
                        text = { Text("متابعة الأبناء والورد", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Default.People, contentDescription = null) }
                    )
                    Tab(
                        selected = activeTab == "FAMILY_LOGS",
                        onClick = { activeTab = "FAMILY_LOGS" },
                        text = { Text("سجل قراءات العائلة", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                when (activeTab) {
                    "MUSHAF" -> {
                        // Quran catalog & search
                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                            // Gold-themed Circular Quran Progress Tracker Widget
                            val completedSurahsFromLogs = quranLogs.filter { it.completed }.map { it.surah }.toSet()
                            val completedSurahsFromWirds = childWirds.filter { it.isCompleted }.map { it.surahName }.toSet()
                            val uniqueCompletedSurahs = (completedSurahsFromLogs + completedSurahsFromWirds).size
                            
                            val totalWirds = childWirds.size
                            val completedWirds = childWirds.count { it.isCompleted }
                            
                            val progressFloat = if (uniqueCompletedSurahs > 0) {
                                uniqueCompletedSurahs.toFloat() / 114f
                            } else if (totalWirds > 0) {
                                completedWirds.toFloat() / totalWirds.toFloat()
                            } else {
                                0f
                            }
                            
                            val progressPercent = (progressFloat * 100).toInt()

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            "🏆 المتابع الذهبي لتقدم الحفظ والتلاوة",
                                            color = WarmGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "السور المنجزة بالكامل: $uniqueCompletedSurahs من أصل 114 سورة",
                                            color = Color.White,
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            "التحصيل التراكمي: تم إنجاز $completedWirds من كلف الأسبوع ($totalWirds إجمالاً)",
                                            color = SilverGray,
                                            fontSize = 10.sp
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(72.dp)
                                    ) {
                                        Canvas(modifier = Modifier.size(64.dp)) {
                                            // background gray arc
                                            drawArc(
                                                color = Color.DarkGray.copy(alpha = 0.3f),
                                                startAngle = -90f,
                                                sweepAngle = 360f,
                                                useCenter = false,
                                                style = Stroke(width = 6.dp.toPx())
                                            )
                                            // gold foreground arc
                                            drawArc(
                                                color = WarmGold,
                                                startAngle = -90f,
                                                sweepAngle = progressFloat * 360f,
                                                useCenter = false,
                                                style = Stroke(
                                                    width = 6.dp.toPx(),
                                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                                )
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "$progressPercent%",
                                                color = WarmGold,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "التقدم",
                                                color = SilverGray,
                                                fontSize = 7.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Local Notification Quran Goal Reminder Settings (Alarm Scheduler)
                            var showAlarmSettings by remember { mutableStateOf(false) }
                            var alarmHour by remember { mutableStateOf(16) } // Default 4:00 PM
                            var alarmMinute by remember { mutableStateOf(0) }
                            var isAlarmScheduled by remember { 
                                mutableStateOf(
                                    context.getSharedPreferences("quran_prefs", Context.MODE_PRIVATE)
                                        .getBoolean("alarm_scheduled", false)
                                )
                            }
                            var savedHourText by remember {
                                mutableStateOf(
                                    context.getSharedPreferences("quran_prefs", Context.MODE_PRIVATE)
                                        .getInt("alarm_hour", 16)
                                )
                            }
                            var savedMinuteText by remember {
                                mutableStateOf(
                                    context.getSharedPreferences("quran_prefs", Context.MODE_PRIVATE)
                                        .getInt("alarm_minute", 0)
                                )
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.8f)),
                                border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { showAlarmSettings = !showAlarmSettings },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Alarm,
                                                null,
                                                tint = WarmGold,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "منبه الحفظ والمراجعة التلقائي المبرمج",
                                                color = Color.White,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isAlarmScheduled) {
                                                Text(
                                                    "مفعّل (${String.format("%02d:%02d %s", if(savedHourText == 0 || savedHourText == 12) 12 else savedHourText % 12, savedMinuteText, if(savedHourText >= 12) "م" else "ص")})",
                                                    color = WarmGold,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(end = 4.dp)
                                                )
                                            } else {
                                                Text(
                                                    "غير نشط 📴",
                                                    color = SilverGray,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(end = 4.dp)
                                                )
                                            }
                                            Icon(
                                                if (showAlarmSettings) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                null,
                                                tint = SilverGray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    if (showAlarmSettings) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = WarmGold.copy(alpha = 0.15f))
                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            "قم بجدولة تذكير آلي مبرمج لتنبيهك بورد السمع والحفظ للأبناء يومياً من خلال منبه الجهاز الداخلي:",
                                            color = SilverGray,
                                            fontSize = 10.sp,
                                            lineHeight = 14.sp
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Hour selector
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("الساعة (24س)", color = WarmGold, fontSize = 9.5.sp)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color.Black.copy(0.4f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = { if (alarmHour > 0) alarmHour-- else alarmHour = 23 },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.Remove, null, tint = WarmGold, modifier = Modifier.size(16.dp))
                                                    }
                                                    Text(
                                                        String.format("%02d", alarmHour),
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    IconButton(
                                                        onClick = { if (alarmHour < 23) alarmHour++ else alarmHour = 0 },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.Add, null, tint = WarmGold, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }

                                            // Minute selector
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("الدقيقة", color = WarmGold, fontSize = 9.5.sp)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color.Black.copy(0.4f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = { if (alarmMinute >= 5) alarmMinute -= 5 else alarmMinute = 55 },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.Remove, null, tint = WarmGold, modifier = Modifier.size(16.dp))
                                                    }
                                                    Text(
                                                        String.format("%02d", alarmMinute),
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    IconButton(
                                                        onClick = { if (alarmMinute <= 50) alarmMinute += 5 else alarmMinute = 0 },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.Add, null, tint = WarmGold, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Cancel Button
                                            Button(
                                                onClick = {
                                                    try {
                                                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                                                        val intent = Intent(context, com.example.service.QuranGoalReceiver::class.java)
                                                        val pendingIntent = android.app.PendingIntent.getBroadcast(
                                                            context,
                                                            501,
                                                            intent,
                                                            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                                                        )
                                                        alarmManager.cancel(pendingIntent)
                                                        
                                                        context.getSharedPreferences("quran_prefs", Context.MODE_PRIVATE)
                                                            .edit()
                                                            .putBoolean("alarm_scheduled", false)
                                                            .apply()
                                                        
                                                        isAlarmScheduled = false
                                                        android.widget.Toast.makeText(context, "تم إلغاء منبه الورد القرآني بنجاح", android.widget.Toast.LENGTH_SHORT).show()
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(0.12f), contentColor = Color.Red),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(vertical = 4.dp)
                                            ) {
                                                Text("إلغاء المنبه 📴", fontSize = 11.sp)
                                            }

                                            // Schedule Button
                                            Button(
                                                onClick = {
                                                    try {
                                                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                                                        val intent = Intent(context, com.example.service.QuranGoalReceiver::class.java)
                                                        val pendingIntent = android.app.PendingIntent.getBroadcast(
                                                            context,
                                                            501,
                                                            intent,
                                                            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                                                        )
                                                        
                                                        val calendar = java.util.Calendar.getInstance().apply {
                                                            set(java.util.Calendar.HOUR_OF_DAY, alarmHour)
                                                            set(java.util.Calendar.MINUTE, alarmMinute)
                                                            set(java.util.Calendar.SECOND, 0)
                                                            set(java.util.Calendar.MILLISECOND, 0)
                                                            if (before(java.util.Calendar.getInstance())) {
                                                                add(java.util.Calendar.DATE, 1)
                                                            }
                                                        }

                                                        alarmManager.setRepeating(
                                                            android.app.AlarmManager.RTC_WAKEUP,
                                                            calendar.timeInMillis,
                                                            android.app.AlarmManager.INTERVAL_DAY,
                                                            pendingIntent
                                                        )

                                                        context.getSharedPreferences("quran_prefs", Context.MODE_PRIVATE)
                                                            .edit()
                                                            .putBoolean("alarm_scheduled", true)
                                                            .putInt("alarm_hour", alarmHour)
                                                            .putInt("alarm_minute", alarmMinute)
                                                            .apply()

                                                        isAlarmScheduled = true
                                                        savedHourText = alarmHour
                                                        savedMinuteText = alarmMinute
                                                        
                                                        val formattedTime = String.format("%02d:%02d", alarmHour, alarmMinute)
                                                        android.widget.Toast.makeText(context, "تم ضبط منبه الورد القرآني العائلي بنجاح على الساعة $formattedTime!", android.widget.Toast.LENGTH_LONG).show()
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                        android.widget.Toast.makeText(context, "فشل ضبط المنبه: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = WarmGold.copy(0.12f), contentColor = WarmGold),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(vertical = 4.dp)
                                            ) {
                                                Text("تأكيد المنبه ⏰", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Instant search input
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    if (it.isNotBlank()) {
                                        searchResults = QuranLocalDatabase.searchQuran(context, it)
                                    } else {
                                        searchResults = emptyList()
                                    }
                                },
                                placeholder = { Text("البحث السريع في كلمات وآيات القرآن الكريم... 🔎") },
                                label = { Text("ابحث عن آية أو سورة كاملة") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = WarmGold) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = ""; searchResults = emptyList() }) {
                                            Icon(Icons.Default.Close, contentDescription = null)
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WarmGold,
                                    unfocusedBorderColor = WarmGold.copy(0.4f),
                                    focusedLabelColor = WarmGold,
                                    unfocusedLabelColor = SilverGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = SilverGray,
                                    focusedContainerColor = SurfaceDark,
                                    unfocusedContainerColor = SurfaceDark
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            if (searchQuery.isNotBlank()) {
                                Text("نتائج البحث (${searchResults.size} آية مطابقة):", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(6.dp))

                                if (searchResults.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                        Text("لا توجد نتائج مطابقة، جرب كلمة أخرى (مثال: 'الحمد', 'الرحمن')", color = Color.Gray, fontSize = 12.sp)
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(searchResults) { result ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        // Jump to Surah
                                                        val s = surahsIndex.firstOrNull { it.id == result.surahId }
                                                        if (s != null) {
                                                            selectedSurah = s
                                                            isLoadingText = true
                                                            scope.launch {
                                                                surahVerses = QuranLocalDatabase.getSurahVerses(context, s.id)
                                                                isLoadingText = false
                                                            }
                                                        }
                                                    },
                                                border = BorderStroke(1.dp, WarmGold.copy(0.4f)),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("سورة ${result.surahName}", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 12.sp)
                                                        Text("الآية ${result.verseNumber}", fontSize = 11.sp, color = Color.Gray)
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        result.verseText,
                                                        fontSize = 15.sp,
                                                        color = MidnightBlue,
                                                        fontWeight = FontWeight.Medium,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        textAlign = TextAlign.Right
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Default Surah index grid/list
                                Text("الفهرس العثماني الميسر لـ 114 سورة كاملة:", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(6.dp))

                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(surahsIndex) { surah ->
                                        // Cache status
                                        val isCached = remember(surah.id) {
                                            val firstFile = getLocalVerseAudioFile(context, surah.id, 1, false)
                                            firstFile.exists() && firstFile.length() > 1000
                                        }

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedSurah = surah
                                                    isLoadingText = true
                                                    scope.launch {
                                                        surahVerses = QuranLocalDatabase.getSurahVerses(context, surah.id)
                                                        isLoadingText = false
                                                    }
                                                },
                                            border = BorderStroke(0.5.dp, WarmGold.copy(0.3f)),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .background(WarmGold.copy(0.15f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(surah.id.toString(), color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text("سورة ${surah.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MidnightBlue)
                                                        Text("آياتها ${surah.versesCount} | ${surah.type}", fontSize = 11.sp, color = Color.Gray)
                                                    }
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (isCached) {
                                                        Icon(Icons.Default.OfflinePin, contentDescription = "Downloaded Offline", tint = GreenAccent, modifier = Modifier.size(18.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                    }
                                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = WarmGold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "KIDS" -> {
                        // Portions & children panel
                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("قائمة الأولاد المسجلين للحفظ والقرآن:", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 13.sp)
                                Button(
                                    onClick = { showAddChildDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = MidnightBlue)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إضافة ولد جديد", color = MidnightBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (children.isEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.PersonOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = WarmGold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("لا يوجد أولاد مسجلين حالياً", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("يرجى النقر على زر 'إضافة ولد جديد' في الأعلى لكتابة أسماء ومقررات حفظ الأبناء والبدء بمتابعة التلاوة والأوراد وتصحيح الأخطاء تلقائياً.", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(children) { child ->
                                        val childWirdsCount = childWirds.count { it.childId == child.id }
                                        val childMistakesCount = childMistakes.count { it.childId == child.id && !it.isCorrected }

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            border = BorderStroke(1.dp, WarmGold.copy(0.4f)),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .background(WarmGold, CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(Icons.Default.Person, contentDescription = null, tint = MidnightBlue)
                                                        }
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Column {
                                                            Text(child.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MidnightBlue)
                                                            Text("الصف: ${child.grade} | الفصل/المجموعة: ${child.className.ifEmpty { "أ" }}", fontSize = 11.sp, color = Color.Gray)
                                                        }
                                                    }

                                                    Row {
                                                        Box(
                                                            modifier = Modifier
                                                                .background(WarmGold.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                                        ) {
                                                            Text("الأوراد النشطة: $childWirdsCount", color = MidnightBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .background(if (childMistakesCount > 0) RedAccent.copy(alpha = 0.15f) else GreenAccent.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                                        ) {
                                                            Text("الأخطاء المفتوحة: $childMistakesCount", color = if (childMistakesCount > 0) RedAccent else GreenAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    OutlinedButton(
                                                        onClick = { showChildDashboardDialog = child },
                                                        modifier = Modifier.weight(1.2f),
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmGold),
                                                        border = BorderStroke(1.dp, WarmGold)
                                                    ) {
                                                        Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("لوحة التحكم والأخطاء 📊", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    Button(
                                                        onClick = { showLessonSetupDialog = child },
                                                        modifier = Modifier.weight(1f),
                                                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold)
                                                    ) {
                                                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp), tint = MidnightBlue)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("بدء تسميع جديد 📖", fontSize = 11.sp, color = MidnightBlue, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        // Family Logs
                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                            Text("تتبع حفظ وتصحيح الأخطاء الملون للأسرة:", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))

                            var surahInput by remember { mutableStateOf("") }
                            var versesInput by remember { mutableStateOf("") }
                            var completedInput by remember { mutableStateOf(false) }
                            var mistakesInput by remember { mutableStateOf(0) }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.4f)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = surahInput,
                                            onValueChange = { surahInput = it },
                                            placeholder = { Text("اسم السورة") },
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmGold),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = versesInput,
                                            onValueChange = { versesInput = it },
                                            placeholder = { Text("الآيات والصفحات") },
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmGold),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(checked = completedInput, onCheckedChange = { completedInput = it }, colors = CheckboxDefaults.colors(checkedColor = WarmGold))
                                            Text("تم التثبيت والحفظ", fontSize = 11.sp)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("عدد الأخطاء:", fontSize = 11.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(onClick = { if (mistakesInput > 0) mistakesInput-- }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Remove, contentDescription = null, tint = WarmGold) }
                                                Text(mistakesInput.toString(), modifier = Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold)
                                                IconButton(onClick = { mistakesInput++ }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Add, contentDescription = null, tint = WarmGold) }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            if (surahInput.isNotBlank()) {
                                                onAddLog(surahInput, versesInput, completedInput, mistakesInput)
                                                surahInput = ""
                                                versesInput = ""
                                                completedInput = false
                                                mistakesInput = 0
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("تسجيل وتتبع الحفظ والتلاوة", color = MidnightBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (quranLogs.isNotEmpty()) {
                                Text("سجل تلاوة العائلة الملون:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WarmGold)
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(quranLogs) { log ->
                                        val color = if (log.mistakes == 0 && log.completed) GreenAccent else if (log.mistakes <= 2) WarmGold else RedAccent
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            border = BorderStroke(1.5.dp, color.copy(alpha = 0.7f)),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text("سورة: ${log.surah} | الآيات: ${log.verses}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        if (log.completed) {
                                                            Box(modifier = Modifier.background(GreenAccent.copy(alpha = 0.2f)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                                                                Text("تم تصحيح الأخطاء", color = GreenAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                        Box(modifier = Modifier.background(color.copy(alpha = 0.2f)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                                                            Text("عدد الأخطاء: ${log.mistakes}", color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                                IconButton(onClick = { onDeleteLog(log.id) }, modifier = Modifier.size(36.dp)) {
                                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = RedAccent)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Granular Holy Quran reading view
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MidnightBlue)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("سورة ${selectedSurah!!.name} | عدد الآيات: ${selectedSurah!!.versesCount}", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 14.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!isFocusMode) {
                                        Text("مع التكرار والتحكم الآتي:", color = Color.White.copy(0.7f), fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            if (audioLoopEnabled) Icons.Default.RepeatOne else Icons.Default.TrendingFlat,
                                            contentDescription = null,
                                            tint = WarmGold,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clickable { audioLoopEnabled = !audioLoopEnabled }
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .background(WarmGold.copy(0.2f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("وضع التركيز غامر 💫", color = WarmGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isFocusMode) Icons.Default.FilterCenterFocus else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = if (isFocusMode) WarmGold else Color.White.copy(0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تفعيل وضع التركيز الغامر", color = Color.White, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Switch(
                                        checked = isFocusMode,
                                        onCheckedChange = { isFocusMode = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = WarmGold,
                                            checkedTrackColor = WarmGold.copy(0.4f)
                                        )
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (readingFontSize > 16f) readingFontSize -= 2f },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "تصغير الخط", tint = Color.White)
                                    }
                                    Text(
                                        "${readingFontSize.toInt()}sp", 
                                        color = Color.White, 
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    IconButton(
                                        onClick = { if (readingFontSize < 42f) readingFontSize += 2f },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = "تكبير الخط", tint = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    if (isLoadingText) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = WarmGold)
                        }
                    } else {
                        if (isFocusMode) {
                            val focusScrollState = rememberScrollState()
                            val dynamicBonus = (focusScrollState.value * 0.012f).coerceIn(-4f, 8f)
                            val computedFontSize = (readingFontSize + 4f + dynamicBonus).sp
                            val computedLineHeight = (readingFontSize + 16f + dynamicBonus).sp

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(Color(0xFF06090F)) // Deep Obsidian backdrop
                                    .padding(horizontal = 4.dp, vertical = 10.dp)
                                    .verticalScroll(focusScrollState),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Centered Basmalah
                                if (selectedSurah!!.id != 1 && selectedSurah!!.id != 9) {
                                    Text(
                                        "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = WarmGold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                    )
                                }

                                surahVerses.forEach { verse ->
                                    val isPlaying = currentlyPlayingVerse == verse.number && currentlyPlayingSurah == selectedSurah!!.id
                                    var cleanText = verse.textAr
                                    if (selectedSurah!!.id != 1 && verse.number == 1) {
                                        val bismillah1 = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
                                        val bismillah2 = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ"
                                        if (cleanText.startsWith(bismillah1)) {
                                            cleanText = cleanText.substring(bismillah1.length).trim()
                                        } else if (cleanText.startsWith(bismillah2)) {
                                            cleanText = cleanText.substring(bismillah2.length).trim()
                                        }
                                    }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showVerseOptionSheet = verse },
                                        border = BorderStroke(
                                            width = if (isPlaying) 1.5.dp else 0.5.dp,
                                            color = if (isPlaying) WarmGold else Color(0xFFD4AF37).copy(0.2f)
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isPlaying) Color(0xFF131B26) else Color(0xFF0A0E15) // Deep Obsidian Cards
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = "$cleanText ﴿${verse.number}﴾",
                                                fontSize = computedFontSize,
                                                lineHeight = computedLineHeight,
                                                color = Color(0xFFC0C0C0), // Silver text
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            if (isPlaying) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.Start,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    IconButton(
                                                        onClick = { playVerseAudio(selectedSurah!!.id, verse.number, false) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.PauseCircleFilled, contentDescription = null, tint = WarmGold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                // Centered Basmalah
                                if (selectedSurah!!.id != 1 && selectedSurah!!.id != 9) {
                                    item {
                                        Text(
                                            "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 19.sp,
                                            color = WarmGold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp)
                                        )
                                    }
                                }

                                items(surahVerses) { verse ->
                                    val isPlaying = currentlyPlayingVerse == verse.number && currentlyPlayingSurah == selectedSurah!!.id

                                    // Clean verse display text (remove the initial bismillah prefix if stored in offline text)
                                    var cleanText = verse.textAr
                                    if (selectedSurah!!.id != 1 && verse.number == 1) {
                                        val bismillah1 = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
                                        val bismillah2 = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ"
                                        if (cleanText.startsWith(bismillah1)) {
                                            cleanText = cleanText.substring(bismillah1.length).trim()
                                        } else if (cleanText.startsWith(bismillah2)) {
                                            cleanText = cleanText.substring(bismillah2.length).trim()
                                        }
                                    }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                showVerseOptionSheet = verse
                                            },
                                        border = BorderStroke(
                                            width = if (isPlaying) 2.dp else 0.5.dp,
                                            color = if (isPlaying) WarmGold else Color.LightGray.copy(0.4f)
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isPlaying) WarmGold.copy(alpha = 0.08f) else Color.White
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = if (isFocusMode) "$cleanText ﴿${verse.number}﴾" else cleanText,
                                                fontSize = if (isFocusMode) (readingFontSize + 4).sp else readingFontSize.sp,
                                                lineHeight = if (isFocusMode) (readingFontSize + 16).sp else (readingFontSize + 14).sp,
                                                color = MidnightBlue,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            if (!isFocusMode || isPlaying) Spacer(modifier = Modifier.height(6.dp))
                                            if (!isFocusMode || isPlaying) Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(
                                                        onClick = {
                                                            playVerseAudio(selectedSurah!!.id, verse.number, false)
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            if (isPlaying && !audioIsTeacher) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircle,
                                                            contentDescription = "Standard Recitation",
                                                            tint = WarmGold
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            playVerseAudio(selectedSurah!!.id, verse.number, true)
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            if (isPlaying && audioIsTeacher) Icons.Default.RecordVoiceOver else Icons.Default.School,
                                                            contentDescription = "Teacher Session",
                                                            tint = GreenAccent
                                                        )
                                                    }
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .background(WarmGold.copy(0.12f), CircleShape)
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text("آية ${verse.number}", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!isFocusMode) {
                        Button(
                            onClick = {
                                selectedSurah = null
                                isSequencePlaying = false
                                mediaPlayer?.stop()
                                mediaPlayer?.release()
                                mediaPlayer = null
                                currentlyPlayingVerse = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("العودة إلى المصحف الفهرسي ومتابعة الطلاب", color = MidnightBlue, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                selectedSurah = null
                                isSequencePlaying = false
                                mediaPlayer?.stop()
                                mediaPlayer?.release()
                                mediaPlayer = null
                                currentlyPlayingVerse = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("الخروج من وضع التلاوة والتركيز 🚪", fontSize = 11.sp, color = MidnightBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet Option for a clicked Verse
    if (showVerseOptionSheet != null) {
        val verse = showVerseOptionSheet!!
        val isPlayingObj = currentlyPlayingVerse == verse.number && currentlyPlayingSurah == selectedSurah?.id

        AlertDialog(
            onDismissRequest = { showVerseOptionSheet = null },
            containerColor = SurfaceDark,
            title = {
                Text(
                    text = "الآية رقم ${verse.number} - خيارات المنشاوي والمتابعة والتصحيح",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = WarmGold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        verse.textAr,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextLight,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("الشيخ محمد صديق المنشاوي 🎧", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WarmGold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                showVerseOptionSheet = null
                                playVerseAudio(selectedSurah!!.id, verse.number, false)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = WarmGold)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("المنشاوي المرتل", fontSize = 10.sp, color = MidnightBlue)
                        }

                        Button(
                            onClick = {
                                showVerseOptionSheet = null
                                playVerseAudio(selectedSurah!!.id, verse.number, true)
                            },
                            modifier = Modifier.weight(1.1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenAccent)
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("المنشاوي المعلم المعيّد", fontSize = 10.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Replay toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("إعادة تكرار هذه الآية تلقائياً:", fontSize = 11.sp, color = Color.Gray)
                        Switch(
                            checked = audioLoopEnabled,
                            onCheckedChange = { audioLoopEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = WarmGold)
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 10.dp))

                    // Copy action and Log Mistake
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(verse.textAr))
                            Toast.makeText(context, "تم نسخ الآية الكريمة بنجاح! 📋", Toast.LENGTH_SHORT).show()
                            showVerseOptionSheet = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MidnightBlue)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("نسخ الآية كاملة للمذكرة", color = Color.White, fontSize = 11.sp)
                    }

                    // Log mistake directly if session is active
                    if (activeSessionChild != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        var customMistakeText by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = customMistakeText,
                            onValueChange = { customMistakeText = it },
                            placeholder = { Text("مثال: لحن في الكلمة أو نقص حرف", color = SilverGray.copy(alpha = 0.5f)) },
                            label = { Text("تسجيل تغليط للابن (${activeSessionChild!!.name})") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedBorderColor = WarmGold,
                                focusedLabelColor = WarmGold,
                                unfocusedLabelColor = SilverGray
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                val mistakeWord = customMistakeText.ifBlank { "خطأ في النطق أو المخرجات" }
                                onAddMistake(
                                    activeSessionChild!!.id,
                                    selectedSurah!!.id,
                                    selectedSurah!!.name,
                                    verse.number,
                                    verse.textAr,
                                    mistakeWord,
                                    false
                                )
                                Toast.makeText(context, "تم تدوين الغلط وتسجيله على الابن بنجاح! ❌", Toast.LENGTH_SHORT).show()
                                showVerseOptionSheet = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تسجيل خطأ وحفظ بمصفوفة الطالب", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVerseOptionSheet = null }) {
                    Text("إغلاق الخيارات", color = WarmGold)
                }
            }
        )
    }

    // Add Child Dialog
    if (showAddChildDialog) {
        var nameInput by remember { mutableStateOf("") }
        var ageInput by remember { mutableStateOf("") }
        var gradeInput by remember { mutableStateOf("") }
        var notesInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddChildDialog = false },
            containerColor = SurfaceDark,
            title = { Text("تسجيل ولد جديد للحفظ ومتابعة التلاوة والأوراد", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WarmGold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nameInput, 
                        onValueChange = { nameInput = it }, 
                        label = { Text("اسم الابن بالكامل") }, 
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = WarmGold, focusedLabelColor = WarmGold, unfocusedLabelColor = SilverGray)
                    )
                    OutlinedTextField(
                        value = ageInput, 
                        onValueChange = { ageInput = it }, 
                        label = { Text("العمر") }, 
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = WarmGold, focusedLabelColor = WarmGold, unfocusedLabelColor = SilverGray)
                    )
                    OutlinedTextField(
                        value = gradeInput, 
                        onValueChange = { gradeInput = it }, 
                        label = { Text("الصف الدراسي (مثال: السابع، الثامن)") }, 
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = WarmGold, focusedLabelColor = WarmGold, unfocusedLabelColor = SilverGray)
                    )
                    OutlinedTextField(
                        value = notesInput, 
                        onValueChange = { notesInput = it }, 
                        label = { Text("أهداف أو ملاحظات خاصة للحفظ") }, 
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = WarmGold, focusedLabelColor = WarmGold, unfocusedLabelColor = SilverGray)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            onAddChild(nameInput, ageInput.toIntOrNull() ?: 8, gradeInput, notesInput)
                            showAddChildDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold)
                ) {
                    Text("تسجيل الابن كلياً", color = MidnightBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddChildDialog = false }) {
                    Text("إلغاء", color = SilverGray)
                }
            }
        )
    }

    // Lesson Setup Dialog
    if (showLessonSetupDialog != null) {
        val child = showLessonSetupDialog!!
        var currSurahSelected by remember { mutableStateOf(surahsIndex[0]) }
        var startingVerseText by remember { mutableStateOf("1") }

        AlertDialog(
            onDismissRequest = { showLessonSetupDialog = null },
            containerColor = SurfaceDark,
            title = { Text("جدولة تسميع درس جديد للابن ${child.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WarmGold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("اختر السورة والآية التي سيبدأ الابن القراءة فيها لتوجيه المصحف التفاعلي إليها تلقائياً والبدء برصد الأغلاط مباشرة:", fontSize = 11.sp, color = SilverGray)

                    var dropdownExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { dropdownExpanded = true }, 
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmGold)
                        ) {
                            Text("السورة المختارة: سورة ${currSurahSelected.name}")
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded, 
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            surahsIndex.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text("سورة ${s.name}", color = TextLight) },
                                    onClick = {
                                        currSurahSelected = s
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = startingVerseText,
                        onValueChange = { startingVerseText = it },
                        label = { Text("رقم آية البدء للتسميع") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = WarmGold, focusedLabelColor = WarmGold, unfocusedLabelColor = SilverGray)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLessonSetupDialog = null
                        activeSessionChild = child
                        val sVerse = startingVerseText.toIntOrNull() ?: 1
                        
                        // Create a temporary active wird session helper
                        activeSessionWird = ChildQuranWird(
                            childId = child.id,
                            surahId = currSurahSelected.id,
                            surahName = currSurahSelected.name,
                            startVerse = sVerse,
                            endVerse = currSurahSelected.versesCount,
                            currentVerse = sVerse,
                            isCompleted = false,
                            assignedDate = ""
                        )

                        // Open Mushaf at that surah
                        selectedSurah = currSurahSelected
                        isLoadingText = true
                        scope.launch {
                            surahVerses = QuranLocalDatabase.getSurahVerses(context, currSurahSelected.id)
                            isLoadingText = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold)
                ) {
                    Text("ابدأ التسميع الآن 📖", color = MidnightBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLessonSetupDialog = null }) {
                    Text("إلغاء", color = SilverGray)
                }
            }
        )
    }

    // Detailed Child Dashboard Dialog
    if (showChildDashboardDialog != null) {
        val child = showChildDashboardDialog!!
        val activeWirds = childWirds.filter { it.childId == child.id }
        val activeMistakes = childMistakes.filter { it.childId == child.id }
        var dashboardTab by remember { mutableStateOf("PROGRESS") }

        AlertDialog(
            onDismissRequest = { showChildDashboardDialog = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("لوحة إدارة قرآن الابن: ${child.name}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = WarmGold)
                    IconButton(onClick = { showChildDashboardDialog = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = RedAccent)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Absolute.Right,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showAddWirdDialogForChild = child },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmGold)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تعيين ورد/تكليف جديد للأسبوع", fontSize = 10.sp, color = MidnightBlue, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { showReportExportDialog = child },
                            colors = ButtonDefaults.buttonColors(containerColor = MidnightBlue)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("استخراج التقارير اليومية والشاملة 📊", fontSize = 10.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dynamic Compact Custom Segmented Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "PROGRESS" to "التحليل والتقدم 📊",
                            "BADGES" to "الأوسمة والتشجيع 🏆",
                            "MISTAKES" to "سجل الأخطاء والتكليف ❌"
                        ).forEach { (tabId, tabName) ->
                            val isSelected = dashboardTab == tabId
                            Button(
                                onClick = { dashboardTab = tabId },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) WarmGold else MidnightBlue.copy(0.08f),
                                    contentColor = if (isSelected) MidnightBlue else MidnightBlue
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                            ) {
                                Text(tabName, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (dashboardTab == "PROGRESS") {
                        val completedWirdsForChild = activeWirds.filter { it.isCompleted }
                        val totalAssignedVerses = activeWirds.sumOf { it.endVerse - it.startVerse + 1 }
                        val totalMemorizedVerses = completedWirdsForChild.sumOf { it.endVerse - it.startVerse + 1 }
                        val remainingVerses = totalAssignedVerses - totalMemorizedVerses
                        val progressPercent = if (totalAssignedVerses > 0) (totalMemorizedVerses.toFloat() / totalAssignedVerses * 100).toInt() else 0

                        Column(modifier = Modifier.fillMaxSize()) {
                            // Custom Donut Progress Canvas Drawing
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(90.dp)) {
                                    drawCircle(
                                        color = MidnightBlue.copy(alpha = 0.08f),
                                        style = Stroke(width = 10.dp.toPx())
                                    )
                                    drawArc(
                                        color = WarmGold,
                                        startAngle = -90f,
                                        sweepAngle = if (totalAssignedVerses > 0) (totalMemorizedVerses.toFloat() / totalAssignedVerses * 360f) else 0f,
                                        useCenter = false,
                                        style = Stroke(
                                            width = 10.dp.toPx(),
                                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                                        )
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$progressPercent%",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MidnightBlue
                                    )
                                    Text(
                                        text = "نسبة إنجاز الحفظ",
                                        fontSize = 8.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "إجمالي الآيات بالتكليف" to totalAssignedVerses,
                                    "تم الحفظ والإتقان" to totalMemorizedVerses,
                                    "المتبقي للحفظ والتلاوة" to remainingVerses
                                ).forEach { (label, value) ->
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(0.5.dp, Color.LightGray.copy(0.5f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(label, fontSize = 8.sp, color = Color.Gray, textAlign = TextAlign.Center)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("$value", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MidnightBlue)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            ) {
                                item {
                                    Text("تفاصيل تقدم السور الحالية المكلفة:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MidnightBlue)
                                }
                                if (activeWirds.isEmpty()) {
                                    item {
                                        Text("لا توجد أوراد مسجلة لإظهار تحليلات تقدم الحفظ التفصيلية.", fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(16.dp))
                                    }
                                } else {
                                    items(activeWirds) { wird ->
                                        val totalWirdV = wird.endVerse - wird.startVerse + 1
                                        val wirdProgressRatio = if (wird.isCompleted) 1f else 0.25f
                                        
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            border = BorderStroke(0.5.dp, Color.LightGray.copy(0.3f))
                                        ) {
                                            Column(modifier = Modifier.padding(4.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("سورة ${wird.surahName}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MidnightBlue)
                                                    Text(if (wird.isCompleted) "متقن كامل ✅" else "قيد المتابعة والتكرار ⏳", fontSize = 9.sp, color = if (wird.isCompleted) GreenAccent else WarmGold, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                LinearProgressIndicator(
                                                    progress = wirdProgressRatio,
                                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                                    color = if (wird.isCompleted) GreenAccent else WarmGold,
                                                    trackColor = MidnightBlue.copy(0.04f)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text("التكليف: من آية ${wird.startVerse} إلى ${wird.endVerse} ($totalWirdV آية)", fontSize = 8.sp, color = Color.Gray)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (dashboardTab == "BADGES") {
                        val isFatihahCompleted = activeWirds.any { it.surahId == 1 && it.isCompleted }
                        val isMulkCompleted = activeWirds.any { it.surahId == 67 && it.isCompleted }
                        val isBaqarahCompleted = activeWirds.any { it.surahId == 2 && it.isCompleted }
                        val isKahfCompleted = activeWirds.any { it.surahId == 18 && it.isCompleted }
                        val completedWirdsCount = activeWirds.filter { it.isCompleted }.size
                        val outstandingCorrected = activeMistakes.isNotEmpty() && activeMistakes.all { it.isCorrected }

                        data class BadgeItem(
                            val name: String,
                            val description: String,
                            val reason: String,
                            val isUnlocked: Boolean,
                            val activeColor: Color
                        )

                        val badgesList = listOf(
                            BadgeItem("أرجوان البركة (الفاتحة) 🌟", "وسام بدء الحفظ السديد والانطلاق من فاتحة الكتاب", "أكمل تكليف سورة الفاتحة كاملاً بامتياز", isFatihahCompleted, WarmGold),
                            BadgeItem("تاج المغفرة والوقاية🛡️", "وسام الحفظ المتقن لسورة الملك المانعة من العذاب", "أكمل سورة الملك كاملةً", isMulkCompleted, GreenAccent),
                            BadgeItem("حصن الزهراء (البقرة) 🏰", "وسام الشجاعة والهمة لحفَظة سورة البقرة المباركة", "أكمل أي تكليف في سورة البقرة بنجاح", isBaqarahCompleted, MidnightBlue),
                            BadgeItem("سراج الجمعة العاصم 🕯️", "وسام الإتقان وحفظ سورة الكهف العاصمة للقلب", "أكمل سورة الكهف المباركة لإضاءة نوريها", isKahfCompleted, GreenAccent),
                            BadgeItem("فارس التقدم والإتقان 🚩", "وسام المثابرة على إتمام 3 تكليفات كاملة بدون صعوبة", "احفظ وأتقن 3 أوراد مسجلة بنجاح", completedWirdsCount >= 3, WarmGold),
                            BadgeItem("النجم الأسعد واللسان السديد ⭐", "وسام المراجعة الذهبي بتصحيح جميع الأخطاء الصوتية", "قم بتجاوز وتصحيح كافة أخطاء مخارج الآيات", outstandingCorrected, RedAccent)
                        )

                        Column(modifier = Modifier.fillMaxSize()) {
                            Text("أوسمة الإنجاز التشجيعية 🏆:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WarmGold)
                            Text("أكمل السور والتكالييف لتفتح الأوسمة والميداليات التشجيعية للابن لتعزيز حافز التكرار والتعلم المتقن:", fontSize = 9.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            ) {
                                items(badgesList) { b ->
                                    val cardBg = if (b.isUnlocked) Color.White else Color.White.copy(0.6f)
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = cardBg),
                                        border = BorderStroke(
                                            width = if (b.isUnlocked) 1.2.dp else 0.5.dp,
                                            color = if (b.isUnlocked) b.activeColor else Color.LightGray.copy(0.3f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .background(
                                                        color = if (b.isUnlocked) b.activeColor.copy(0.12f) else Color.LightGray.copy(0.15f),
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (b.isUnlocked) "🏆" else "🔒",
                                                    fontSize = 16.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = b.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = if (b.isUnlocked) MidnightBlue else Color.Gray
                                                )
                                                Text(
                                                    text = b.description,
                                                    fontSize = 8.sp,
                                                    color = Color.Gray
                                                )
                                                Spacer(modifier = Modifier.height(1.dp))
                                                Text(
                                                    text = b.reason,
                                                    fontSize = 8.sp,
                                                    color = if (b.isUnlocked) GreenAccent else RedAccent,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            if (b.isUnlocked) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(GreenAccent.copy(0.12f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text("مفتوح! 🏆", color = GreenAccent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color.LightGray.copy(0.12f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text("مغلق 🔒", color = Color.Gray, fontSize = 8.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // "MISTAKES_AND_WIRDS" TAB (An absolute, perfect replica of previous list ensuring zero loss of prior features)
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text("الورد اليومي والتكاليف المجدولة للحفظ:", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 11.sp)
                            }

                            if (activeWirds.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MidnightBlue.copy(0.04f))
                                    ) {
                                        Text(
                                            "لا توجد أوراد معينة حالياً لهذا الابن. انقر على 'تعيين ورد جديد' لإضافة تكليف وتتبع آيات الحفظ والمخارج.",
                                            modifier = Modifier.padding(12.dp),
                                            fontSize = 9.sp,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                items(activeWirds) { wird ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        border = BorderStroke(1.dp, WarmGold.copy(0.3f)),
                                        colors = CardDefaults.cardColors(containerColor = Color.White)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("ورد سورة ${wird.surahName}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MidnightBlue)
                                                Text("الآيات من ${wird.startVerse} إلى ${wird.endVerse}", fontSize = 10.sp, color = Color.Gray)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text("تاريخ التعيين: ${wird.assignedDate}", fontSize = 8.sp, color = Color.LightGray)
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (wird.isCompleted) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(GreenAccent.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                                    ) {
                                                        Text("تم الإنجاز والإتقان! ✅", color = GreenAccent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                } else {
                                                    Button(
                                                        onClick = {
                                                            onUpdateWird(wird.copy(isCompleted = true))
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("اعتماد الإتقان", fontSize = 8.sp, color = Color.White)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                IconButton(onClick = { onDeleteWird(wird.id) }, modifier = Modifier.size(24.dp)) {
                                                    Icon(Icons.Default.Delete, contentDescription = null, tint = RedAccent, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("سجل الأخطاء ومتابعة مستويات التجاوز والتكرار:", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 11.sp)
                            }

                            if (activeMistakes.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MidnightBlue.copy(0.04f))
                                    ) {
                                        Text(
                                            "ما شاء الله! لا توجد أخطاء مسجلة أو تم تصحيحها بالكامل. ممتاز جداً! 🎉",
                                            modifier = Modifier.padding(12.dp),
                                            fontSize = 9.sp,
                                            color = GreenAccent,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                items(activeMistakes) { mistake ->
                                    val mistColor = if (mistake.isCorrected) GreenAccent else RedAccent
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        border = BorderStroke(1.dp, mistColor.copy(0.4f)),
                                        colors = CardDefaults.cardColors(containerColor = Color.White)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("سورة ${mistake.surahName} | الآية ${mistake.verseNumber}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MidnightBlue)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(mistColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                                    ) {
                                                        Text(
                                                            if (mistake.isCorrected) "تم الإتقان والتصحيح ✅" else "يحتاج مراجعة ❌",
                                                            color = mistColor,
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    IconButton(onClick = { onDeleteMistake(mistake.id) }, modifier = Modifier.size(18.dp)) {
                                                        Icon(Icons.Default.Delete, contentDescription = null, tint = RedAccent, modifier = Modifier.size(12.dp))
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                "الآية: ${mistake.verseText}",
                                                fontSize = 11.sp,
                                                color = MidnightBlue.copy(alpha = 0.7f),
                                                lineHeight = 18.sp,
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("موضع الغلط: ${mistake.mistakeWord}", fontWeight = FontWeight.Bold, color = RedAccent, fontSize = 9.sp, textAlign = TextAlign.Right, modifier = Modifier.weight(1f))
                                                if (!mistake.isCorrected) {
                                                    Button(
                                                        onClick = { onToggleMistake(mistake) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("اعتماد التصحيح والتجاوز", fontSize = 8.sp, color = Color.White)
                                                    }
                                                } else {
                                                    OutlinedButton(
                                                        onClick = { onToggleMistake(mistake) },
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("إعادة مراجعة الأخطاء", fontSize = 8.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChildDashboardDialog = null }) {
                    Text("حفظ والعودة", color = WarmGold, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Add Wird Dialog For Child
    if (showAddWirdDialogForChild != null) {
        val child = showAddWirdDialogForChild!!
        var wirdSurahSelected by remember { mutableStateOf(surahsIndex[0]) }
        var isWirdExp by remember { mutableStateOf(false) }
        var startVerseStr by remember { mutableStateOf("1") }
        var endVerseStr by remember { mutableStateOf("10") }

        AlertDialog(
            onDismissRequest = { showAddWirdDialogForChild = null },
            containerColor = SurfaceDark,
            title = { Text("تعيين تكليف للابن ${child.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WarmGold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("عين السور والآيات التي سيتم تكليفه بقراءتها وحفظها هذا الأسبوع:", fontSize = 11.sp, color = SilverGray)

                    Box {
                        OutlinedButton(
                            onClick = { isWirdExp = true }, 
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmGold)
                        ) {
                            Text("سورة الورد: ${wirdSurahSelected.name}")
                        }
                        DropdownMenu(
                            expanded = isWirdExp, 
                            onDismissRequest = { isWirdExp = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            surahsIndex.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text("سورة ${s.name}", color = TextLight) },
                                    onClick = {
                                        wirdSurahSelected = s
                                        isWirdExp = false
                                    }
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startVerseStr,
                            onValueChange = { startVerseStr = it },
                            label = { Text("من آية") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = WarmGold, focusedLabelColor = WarmGold, unfocusedLabelColor = SilverGray)
                        )
                        OutlinedTextField(
                            value = endVerseStr,
                            onValueChange = { endVerseStr = it },
                            label = { Text("إلى آية") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = WarmGold, focusedLabelColor = WarmGold, unfocusedLabelColor = SilverGray)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sV = startVerseStr.toIntOrNull() ?: 1
                        val eV = endVerseStr.toIntOrNull() ?: wirdSurahSelected.versesCount
                        onAddWird(child.id, wirdSurahSelected.id, wirdSurahSelected.name, sV, eV)
                        showAddWirdDialogForChild = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold)
                ) {
                    Text("تعيين للتتبع", color = MidnightBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWirdDialogForChild = null }) {
                    Text("إلغاء", color = SilverGray)
                }
            }
        )
    }

    // Exporter Reporting dialog (Point 4 & 7: Export Word text outline or HTML reporting)
    if (showReportExportDialog != null) {
        val child = showReportExportDialog!!
        val activeWirds = childWirds.filter { it.childId == child.id }
        val activeMistakes = childMistakes.filter { it.childId == child.id }

        // Plain WhatsApp format text
        val plainTextReport = remember(child, activeWirds, activeMistakes) {
            val sb = StringBuilder()
            sb.append("📊 *تقرير متابعة القرآن الكريم وتصحيح الأخطاء الملون لطلب المنهج*\n")
            sb.append("👤 *اسم الابن:* ${child.name}\n")
            sb.append("🎓 *الصف الدراسي:* ${child.grade}\n")
            sb.append("📅 *تاريخ التقرير:* ${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())}\n")
            sb.append("=========================\n\n")
            sb.append("📖 *أوراد الحفظ والتقدم للأسبوع:*\n")
            if (activeWirds.isEmpty()) {
                sb.append("• لا توجد أوراد معينة هذا الأسبوع.\n")
            } else {
                activeWirds.forEach { w ->
                    val status = if (w.isCompleted) "✅ تم الحفظ والإتقان" else "⏳ جاري العمل والتكرار"
                    sb.append("• سورة ${w.surahName} (الآيات من ${w.startVerse} إلى ${w.endVerse}) -> $status\n")
                }
            }
            sb.append("\n❌ *سجل الأخطاء الصوتية والتصحيحية:*\n")
            if (activeMistakes.isEmpty()) {
                sb.append("🎉 ما شاء الله! خالي من الأخطاء كلياً!\n")
            } else {
                activeMistakes.forEachIndexed { i, m ->
                    val status = if (m.isCorrected) "✅ تم الإتقان والتجاوز" else "❌ يحتاج مراجعة وتكرار"
                    sb.append("${i+1}) سورة ${m.surahName} آية ${m.verseNumber}\n")
                    sb.append("   • الخطأ: ${m.mistakeWord}\n")
                    sb.append("   • الحالة: $status\n")
                }
            }
            sb.append("\n*تطبيق اكتفاء العائلي للقرآن والتأسيس والتربية 🏠*")
            sb.toString()
        }

        // Beautiful styled HTML format ready to export/print
        val htmlReport = remember(child, activeWirds, activeMistakes) {
            val sb = StringBuilder()
            sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>")
            sb.append("<style>body{font-family:Arial,sans-serif;direction:rtl;padding:20px;background:#f9f9f9;color:#333;}")
            sb.append(".card{background:#fff;border-radius:8px;padding:20px;box-shadow:0 4px 6px rgba(0,0,0,0.1);max-width:800px;margin:auto;}")
            sb.append("h2{color:#0D1B2A;text-align:center;border-bottom:2px solid #E5C158;padding-bottom:10px;}")
            sb.append("table{width:100%;border-collapse:collapse;margin:20px 0;}")
            sb.append("th,td{border:1px solid #ddd;padding:12px;text-align:right;}")
            sb.append("th{background-color:#0D1B2A;color:#fff;}")
            sb.append(".badge-green{background:#d4edda;color:#155724;padding:4px 8px;border-radius:4px;font-size:12px;font-weight:bold;}")
            sb.append(".badge-red{background:#f8d7da;color:#721c24;padding:4px 8px;border-radius:4px;font-size:12px;font-weight:bold;}")
            sb.append(".footer{text-align:center;margin-top:20px;font-size:12px;color:#777;}</style></head><body>")
            sb.append("<div class='card'>")
            sb.append("<h2>📊 تقرير أخطاء وتلاوة القرآن لـ ${child.name}</h2>")
            sb.append("<p><strong>الصف الدراسي:</strong> ${child.grade} <span style='float:left;'><strong>التاريخ:</strong> ${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())}</span></p>")
            sb.append("<h3>📖 التكاليف والأوراد المقررة</h3>")
            sb.append("<table><tr><th>السورة</th><th>الآيات التكليفية</th><th>الحالة العامة</th></tr>")
            activeWirds.forEach { w ->
                val statusBadge = if (w.isCompleted) "<span class='badge-green'>متقن</span>" else "<span class='badge-red'>تحت المراجعة</span>"
                sb.append("<tr><td>سورة ${w.surahName}</td><td>من ${w.startVerse} إلى ${w.endVerse}</td><td>$statusBadge</td></tr>")
            }
            sb.append("</table>")
            sb.append("<h3>❌ رصد الأخطاء والتصحيحات الملونة</h3>")
            sb.append("<table><tr><th>السورة والآية</th><th>موضع الغلط</th><th>الآية الكاملة</th><th>التقييم والتجاوز</th></tr>")
            activeMistakes.forEach { m ->
                val statusBadge = if (m.isCorrected) "<span class='badge-green'>تجاوز وأتقن</span>" else "<span class='badge-red'>غير متقن</span>"
                sb.append("<tr><td>سورة ${m.surahName} (${m.verseNumber})</td><td><strong>${m.mistakeWord}</strong></td><td>${m.verseText}</td><td>$statusBadge</td></tr>")
            }
            sb.append("</table>")
            sb.append("<div class='footer'><p>تم التوليد والتصدير بواسطة تطبيق اكتفاء العائلي للتربية والتأسيس</p></div>")
            sb.append("</div></body></html>")
            sb.toString()
        }

        AlertDialog(
            onDismissRequest = { showReportExportDialog = null },
            title = { Text("استخراج وتصدير تقرير القرآن للأبناء 📥", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WarmGold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("بإمكانك استخراج تقرير للولد وإرساله مباشرة للمعلمين عبر الواتساب أو نسخه بصيغة مستند HTML ويب ململم ومنظم لطباعته:", fontSize = 11.sp, color = Color.Gray)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 120.dp),
                        colors = CardDefaults.cardColors(containerColor = MidnightBlue.copy(0.04f))
                    ) {
                        Box(modifier = Modifier.padding(10.dp).verticalScroll(rememberScrollState())) {
                            Text(plainTextReport, fontSize = 11.sp, color = MidnightBlue)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(plainTextReport))
                            Toast.makeText(context, "تم نسخ تقرير تلاوة الواتساب بنجاح! 📋", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("نسخ صيغة الواتساب العائلية", color = MidnightBlue, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(htmlReport))
                            Toast.makeText(context, "تم نسخ كود تقرير الـ HTML لطباعته وحفظه! 📑", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MidnightBlue)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("نسخ كود الـ HTML للطباعة والتصدير", color = Color.White, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReportExportDialog = null }) {
                    Text("تم", color = WarmGold, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// Function with specialized scope to handle continuous loops
private fun ScopeLaunchLoopPlay(surahId: Int, verseId: Int, isTeacher: Boolean) {
    // Handled natively inside MediaPlayer setup structure
}

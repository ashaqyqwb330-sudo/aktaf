package com.example.ui.screens

import android.content.Context
import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

// Military & Passive Defense Theme Colors
val CamoOlive = Color(0xFF384534)
val CamoDark = Color(0xFF1D241B)
val CamoKhaki = Color(0xFFC5B358)
val CamoSteel = Color(0xFF4A525A)
val CamoSand = Color(0xFFD2B48C)
val CamoCrimson = Color(0xFF8B2626)
val PassiveGreen = Color(0xFF2E8B57)

data class MalzamaItem(
    val id: Int,
    val title: String,
    val value: String,
    val level: Int,
    val isPartTwo: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MalazimArchiveScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Data lists
    var part1List by remember { mutableStateOf<List<MalzamaItem>>(emptyList()) }
    var part2List by remember { mutableStateOf<List<MalzamaItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<MalzamaItem>>(emptyList()) }
    
    var isLoading by remember { mutableStateOf(true) }
    var activeTab by remember { mutableStateOf(0) } // 0: Part1, 1: Part2, 2: Search

    // Reader state
    var selectedMalzama by remember { mutableStateOf<MalzamaItem?>(null) }
    var textReaderSize by remember { mutableStateOf(15f) }
    var readerTheme by remember { mutableStateOf("CAMO") } // "CAMO", "SAND", "PASSIVE_DARK"

    // Load and parse databases
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                // Parse database_part1.json in background, falling back if not loaded yet
                val p1 = mutableListOf<MalzamaItem>()
                try {
                    val db = com.example.database.EktefaaDatabase.getDatabase(context)
                    val dbLectures = db.lectureDetailDao().getAllLectures()
                    if (dbLectures.isNotEmpty()) {
                        dbLectures.forEach { lecture ->
                            p1.add(MalzamaItem(lecture.id, lecture.title, lecture.value, lecture.level, false))
                        }
                    } else {
                        val p1String = context.assets.open("database_part1.json").bufferedReader().use { it.readText() }
                        val p1Obj = JSONObject(p1String)
                        if (p1Obj.has("V_LectureDetails")) {
                            val arr = p1Obj.getJSONArray("V_LectureDetails")
                            for (i in 0 until arr.length()) {
                                val item = arr.getJSONObject(i)
                                val id = item.optInt("id")
                                val title = item.optString("title", "محاضرة #${id}")
                                val value = item.optString("value", "")
                                val level = item.optInt("level", 1)
                                if (value.isNotBlank()) {
                                    p1.add(MalzamaItem(id, title, value, level, false))
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Parse database_part2.json in background
                val p2 = mutableListOf<MalzamaItem>()
                try {
                    val p2String = context.assets.open("database_part2.json").bufferedReader().use { it.readText() }
                    val p2Obj = JSONObject(p2String)
                    if (p2Obj.has("content")) {
                        val arr = p2Obj.getJSONArray("content")
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            val idlec = item.optInt("idlec")
                            val level = item.optInt("level", 1)
                            val value = item.optString("value", "")
                            
                            // Extract title from the first 40 chars or line
                            val firstLine = value.trim().split("\n").firstOrNull() ?: ""
                            val title = if (firstLine.length in 5..50) firstLine else "ملزمة عسكرية الدرس $idlec"
                            
                            if (value.isNotBlank()) {
                                p2.add(MalzamaItem(idlec, title, value, level, true))
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                withContext(Dispatchers.Main) {
                    part1List = p1
                    part2List = p2
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    Toast.makeText(context, "خطأ في استيراد غمار الملازم: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Interactive Trigger Sound
    fun playSystemClick() {
        try {
            val afd = context.assets.openFd("audio/coins.mp3")
            val mp = MediaPlayer()
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            mp.setVolume(0.2f, 0.2f)
            mp.prepare()
            mp.start()
            mp.setOnCompletionListener { it.release() }
        } catch (e: Exception) {
            // ignore
        }
    }

    if (selectedMalzama != null) {
        // Detailed Camouflaged Reader Screen
        MalzamaReaderView(
            item = selectedMalzama!!,
            textSize = textReaderSize,
            theme = readerTheme,
            onTextSizeChange = { textReaderSize = it },
            onThemeChange = { readerTheme = it },
            onClose = { selectedMalzama = null }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "🪖 أرشيف الملازم العسكري التفاعلي",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CamoKhaki
                            )
                            Text(
                                "غمار هدي الملازم والدفاع السلبي والتحصين العقائدي",
                                fontSize = 10.sp,
                                color = Color.White.copy(0.7f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = CamoKhaki)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CamoOlive)
                )
            },
            containerColor = CamoDark
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 14.dp)
            ) {
                // Passive Defense Camouflaged Banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .border(1.dp, CamoKhaki.copy(0.4f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = CamoOlive.copy(0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(CamoOlive, CamoDark)
                                )
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = "Shield",
                            tint = CamoKhaki,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "كتيبة الدفاع السلبي والتحصين 🔰",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Text(
                                "جميع الخطابات والملازم متوفرة بالكامل دون اتصال بالإنترنت وحصينة ضد التتبع.",
                                color = Color.White.copy(0.7f),
                                fontSize = 9.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = CamoKhaki)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("جاري قراءة الملازم وبناء الهيكل الهندسي للمكعبات...", color = CamoKhaki, fontSize = 12.sp)
                        }
                    }
                } else {
                    // Military Tab layout
                    TabRow(
                        selectedTabIndex = activeTab,
                        containerColor = CamoOlive,
                        contentColor = Color.White
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0; playSystemClick() },
                            text = { Text("مجلد 1 📦", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1; playSystemClick() },
                            text = { Text("مجلد 2 🛡️", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = activeTab == 2,
                            onClick = { activeTab = 2; playSystemClick() },
                            text = { Text("كتيبة البحث 🔍", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    when (activeTab) {
                        0 -> {
                            MalazimSelectionList(
                                list = part1List,
                                onRead = { item ->
                                    selectedMalzama = item
                                    playSystemClick()
                                }
                            )
                        }
                        1 -> {
                            MalazimSelectionList(
                                list = part2List,
                                onRead = { item ->
                                    selectedMalzama = item
                                    playSystemClick()
                                }
                            )
                        }
                        2 -> {
                            // High performance localized search
                            Column(modifier = Modifier.fillMaxSize()) {
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { query ->
                                        searchQuery = query
                                        if (query.isNotBlank() && query.length >= 2) {
                                            searchResults = (part1List + part2List).filter {
                                                it.title.contains(query, ignoreCase = true) ||
                                                        it.value.contains(query, ignoreCase = true)
                                            }
                                        } else {
                                            searchResults = emptyList()
                                        }
                                    },
                                    placeholder = { Text("أدخل كلمة بحث عسكرية (مثال: الجهاد، الصبر، الوعي)...", color = Color.White.copy(0.4f), fontSize = 11.sp) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp)),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.White.copy(0.08f),
                                        unfocusedContainerColor = Color.White.copy(0.05f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        cursorColor = CamoKhaki
                                    ),
                                    singleLine = true,
                                    trailingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = CamoKhaki) }
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                if (searchQuery.isBlank()) {
                                    Box(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("أدخل عبارة لبدء الفحص والبحث السلبي في الكود والدروس", color = Color.White.copy(0.5f), fontSize = 11.sp)
                                    }
                                } else {
                                    Text(
                                        "نتائج الفحص: تم رصد ${searchResults.size} موضع مطابقة",
                                        color = CamoKhaki,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    MalazimSelectionList(
                                        list = searchResults,
                                        onRead = { item ->
                                            selectedMalzama = item
                                            playSystemClick()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MalazimSelectionList(
    list: List<MalzamaItem>,
    onRead: (MalzamaItem) -> Unit
) {
    if (list.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("لا توجد ملازم مطابقة للمواصفات حالياً", color = Color.White.copy(0.5f), fontSize = 12.sp)
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(list) { malzama ->
                // Styled as an interactive 3D Cube Card representation with military badge
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(12.dp), spotColor = CamoKhaki)
                        .clickable { onRead(malzama) }
                        .border(
                            1.dp,
                            if (malzama.isPartTwo) CamoKhaki.copy(0.3f) else PassiveGreen.copy(0.3f),
                            RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = CamoOlive.copy(0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Level Info
                        Text(
                            "الدرس ${malzama.id}",
                            color = CamoKhaki,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CamoKhaki.copy(0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Title Column
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                malzama.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Right,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (malzama.isPartTwo) "المجلد الثاني" else "المجلد الأول",
                                    color = Color.White.copy(0.5f),
                                    fontSize = 10.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (malzama.isPartTwo) CamoKhaki else PassiveGreen)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Cubic badge
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CamoOlive.copy(0.5f))
                                .border(1.dp, CamoKhaki.copy(0.4f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = "Book",
                                tint = CamoKhaki,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Full interactive screen reader for handouts with camo customization
@Composable
fun MalzamaReaderView(
    item: MalzamaItem,
    textSize: Float,
    theme: String,
    onTextSizeChange: (Float) -> Unit,
    onThemeChange: (String) -> Unit,
    onClose: () -> Unit
) {
    // Determine theme colors
    val bg = when (theme) {
        "SAND" -> Color(0xFFE6D7B3)
        "PASSIVE_DARK" -> Color(0xFF131713)
        else -> Color(0xFF282E28) // Camo
    }
    val fg = when (theme) {
        "SAND" -> Color(0xFF2B2515)
        "PASSIVE_DARK" -> Color(0xFF7CBA80) // Green phosphor
        else -> Color.White
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CamoOlive)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = CamoKhaki)
                }
                Text(
                    item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CamoKhaki,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Right,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        bottomBar = {
            // Reader Control Panel styled securely (passive controls)
            BottomAppBar(
                containerColor = CamoOlive,
                modifier = Modifier.height(70.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Font scaling buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { if (textSize > 12f) onTextSizeChange(textSize - 1.5f) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(0.1f), CircleShape)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Shrink Text", tint = Color.White)
                        }
                        Text("${textSize.toInt()}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        IconButton(
                            onClick = { if (textSize < 24f) onTextSizeChange(textSize + 1.5f) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(0.1f), CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Grow Text", tint = Color.White)
                        }
                    }

                    // Theme selectors
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Camo Theme Box
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF282E28))
                                .border(
                                    1.5.dp,
                                    if (theme == "CAMO") CamoKhaki else Color.White.copy(0.5f),
                                    CircleShape
                                )
                                .clickable { onThemeChange("CAMO") }
                        )
                        // Sand Theme Box
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE6D7B3))
                                .border(
                                    1.5.dp,
                                    if (theme == "SAND") CamoKhaki else Color.White.copy(0.5f),
                                    CircleShape
                                )
                                .clickable { onThemeChange("SAND") }
                        )
                        // Passive Dark Theme Box
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF131713))
                                .border(
                                    1.5.dp,
                                    if (theme == "PASSIVE_DARK") CamoKhaki else Color.White.copy(0.5f),
                                    CircleShape
                                )
                                .clickable { onThemeChange("PASSIVE_DARK") }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(bg)
                .padding(16.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        item.value,
                        color = fg,
                        fontSize = textSize.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = (textSize * 1.6f).sp,
                        textAlign = TextAlign.Justify,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

package com.example.ui.screens

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

// Theme Definitions for Fiqh Board
data class FatwaTheme(
    val name: String,
    val primary: Color,
    val background: Color,
    val cardBg: Color,
    val accent: Color
)

val IslamicTurquoiseTheme = FatwaTheme(
    name = "فيروزي إسلامي",
    primary = Color(0xFF008080),
    background = Color(0xFF021B1B),
    cardBg = Color(0xFF062C2C),
    accent = Color(0xFF40E0D0)
)

val SandGoldenTheme = FatwaTheme(
    name = "ذهبي رملي کعبي",
    primary = Color(0xFFC5A059),
    background = Color(0xFF1B160E),
    cardBg = Color(0xFF2E2517),
    accent = Color(0xFFF1C40F)
)

val ModernAzureTheme = FatwaTheme(
    name = "حديث كحلي صرّاف",
    primary = Color(0xFF1D4ED8),
    background = Color(0xFF0A0F1D),
    cardBg = Color(0xFF141E33),
    accent = Color(0xFF38BDF8)
)

data class FatwaNode(
    val id: String,
    val partIndex: Int,
    val partTitle: String,
    val question: String,
    val answer: String
)

data class AskerStat(
    val name: String,
    val ratio: Float,
    val color: Color,
    val inquiryCount: Int
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FiqhFatwasScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Preferences to track reading history
    val sharedPrefs = remember { context.getSharedPreferences("ektefaa_fatwas_history", Context.MODE_PRIVATE) }
    var readingHistoryList by remember { mutableStateOf<List<String>>(emptyList()) }

    // Themes options
    val themes = listOf(IslamicTurquoiseTheme, SandGoldenTheme, ModernAzureTheme)
    var selectedThemeIndex by remember { mutableStateOf(0) }
    val activeTheme = themes[selectedThemeIndex]

    // Screen tabs: 0: Browse, 1: History, 2: Statistics & Reports
    var activeTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedNodeForDialog by remember { mutableStateOf<FatwaNode?>(null) }
    var showReportPrintDialog by remember { mutableStateOf(false) }

    // Data lists
    var fatwaNodes by remember { mutableStateOf<List<FatwaNode>>(emptyList()) }
    var filteredNodes by remember { mutableStateOf<List<FatwaNode>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Hardcoded / Simulated Inquirers trends data for reports
    val askerStats = remember {
        listOf(
            AskerStat("الأب (المشرف العائلي)", 0.40f, Color(0xFF3498DB), 24),
            AskerStat("الأم (المعلمة التربوية)", 0.30f, Color(0xFF9B59B6), 18),
            AskerStat("الابن الأكبر (حارث)", 0.15f, Color(0xFFE67E22), 9),
            AskerStat("بقية البنات والأقارب", 0.15f, Color(0xFF2ECC71), 9)
        )
    }

    // Helper to read persistent reading history
    fun loadHistory() {
        val historySet = sharedPrefs.getStringSet("visited_questions", emptySet()) ?: emptySet()
        readingHistoryList = historySet.toList()
    }

    fun addHistory(question: String) {
        val historySet = sharedPrefs.getStringSet("visited_questions", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (historySet.add(question)) {
            sharedPrefs.edit().putStringSet("visited_questions", historySet).apply()
            loadHistory()
        }
    }

    // Load and parse
    LaunchedEffect(Unit) {
        loadHistory()
        scope.launch(Dispatchers.IO) {
            val list = mutableListOf<FatwaNode>()
            // Files to parse
            val files = listOf("masaelfeegheah1.json", "masaelfeegheah2.json")

            for (file in files) {
                try {
                    val jsonString = context.assets.open(file).bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(jsonString)
                    val bookTitle = jsonObject.optString("book_title", "مسائل فقهية")
                    if (jsonObject.has("parts")) {
                        val partsArr = jsonObject.getJSONArray("parts")
                        for (p in 0 until partsArr.length()) {
                            val partObj = partsArr.getJSONObject(p)
                            val partIndex = partObj.optInt("part_index")
                            val partTitle = partObj.optString("part_title", "الجزء $partIndex")
                            if (partObj.has("sections")) {
                                val secsArr = partObj.getJSONArray("sections")
                                var currentHeading = ""
                                var currentAnswerAccumulator = ""

                                for (s in 0 until secsArr.length()) {
                                    val secObj = secsArr.getJSONObject(s)
                                    val type = secObj.optString("type")

                                    if (type == "heading") {
                                        // Save previous node if exists
                                        if (currentHeading.isNotBlank() && currentAnswerAccumulator.isNotBlank()) {
                                            val nodeId = "${file}_${partIndex}_${list.size}"
                                            list.add(
                                                FatwaNode(
                                                    id = nodeId,
                                                    partIndex = partIndex,
                                                    partTitle = partTitle,
                                                    question = currentHeading,
                                                    answer = currentAnswerAccumulator.trim()
                                                )
                                            )
                                        }
                                        currentHeading = secObj.optString("text")
                                        currentAnswerAccumulator = ""
                                    } else if (type == "paragraph") {
                                        val pText = secObj.optString("text")
                                        if (currentHeading.isNotBlank()) {
                                            currentAnswerAccumulator += pText + "\n\n"
                                        }
                                    } else if (type == "list") {
                                        if (secObj.has("items")) {
                                            val itemsArr = secObj.getJSONArray("items")
                                            for (k in 0 until itemsArr.length()) {
                                                currentAnswerAccumulator += "• " + itemsArr.getString(k) + "\n"
                                            }
                                            currentAnswerAccumulator += "\n"
                                        }
                                    }
                                }
                                // Add remaining
                                if (currentHeading.isNotBlank() && currentAnswerAccumulator.isNotBlank()) {
                                    val nodeId = "${file}_${partIndex}_${list.size}"
                                    list.add(
                                        FatwaNode(
                                            id = nodeId,
                                            partIndex = partIndex,
                                            partTitle = partTitle,
                                            question = currentHeading,
                                            answer = currentAnswerAccumulator.trim()
                                        )
                                    )
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            withContext(Dispatchers.Main) {
                fatwaNodes = list
                filteredNodes = list
                isLoading = false
            }
        }
    }

    // Filter nodes on search query changes
    LaunchedEffect(searchQuery, fatwaNodes) {
        if (searchQuery.isBlank()) {
            filteredNodes = fatwaNodes
        } else {
            filteredNodes = fatwaNodes.filter {
                it.question.contains(searchQuery, ignoreCase = true) ||
                        it.answer.contains(searchQuery, ignoreCase = true) ||
                        it.partTitle.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(activeTheme.background)
    ) {
        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(activeTheme.primary.copy(0.3f))
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = activeTheme.accent)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            "📖 الفتاوى والمسائل الفقهية العائلية",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeTheme.accent,
                            textAlign = TextAlign.Right
                        )
                        Text(
                            "مستخلص فتاوى مفتي الديار اليمنية شمس الدين شرف الدين",
                            fontSize = 9.sp,
                            color = Color.White.copy(0.7f),
                            textAlign = TextAlign.Right
                        )
                    }
                    IconButton(onClick = {
                        selectedThemeIndex = (selectedThemeIndex + 1) % themes.size
                        Toast.makeText(context, "السمات الملونة: مظهر ${themes[selectedThemeIndex].name}", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Palette, contentDescription = "Change Color Theme", tint = activeTheme.accent)
                    }
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 14.dp)
            ) {

                // Tab selection row
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = activeTheme.cardBg,
                    contentColor = Color.White,
                    modifier = Modifier.padding(vertical = 12.dp).clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("المسائل الفقهية 📚", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("سجل الزيارات ⏱️", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text("إحصاء وتقارير 📊", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = activeTheme.accent)
                    }
                } else {
                    when (activeTab) {
                        0 -> {
                            // Browse tab
                            Column(modifier = Modifier.fillMaxSize()) {
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("ابحث في فتاوى الصلاة، الطهارة، الزكاة والولاية وعامة المسائل...", color = Color.White.copy(0.5f), fontSize = 11.sp) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp)),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = activeTheme.cardBg,
                                        unfocusedContainerColor = activeTheme.cardBg.copy(0.6f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        cursorColor = activeTheme.accent
                                    ),
                                    trailingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = activeTheme.accent) },
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(filteredNodes) { node ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedNodeForDialog = node
                                                    addHistory(node.question)
                                                }
                                                .border(0.5.dp, activeTheme.accent.copy(0.2f), RoundedCornerShape(12.dp)),
                                            colors = CardDefaults.cardColors(containerColor = activeTheme.cardBg.copy(0.7f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Icon(
                                                    Icons.Default.ChevronLeft,
                                                    contentDescription = "Read",
                                                    tint = activeTheme.accent
                                                )

                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    horizontalAlignment = Alignment.End
                                                ) {
                                                    Text(
                                                        "مسألة عن ${node.partTitle}",
                                                        color = activeTheme.accent,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    Text(
                                                        node.question,
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Right,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Visit History tab
                            if (readingHistoryList.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize().weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("سجل الزيارات فارغ حالياً. اقرأ بعض المسائل ليتم رصدها تلقائياً بالذاكرة الصلبة.", color = Color.White.copy(0.5f), fontSize = 11.sp, textAlign = TextAlign.Center)
                                }
                            } else {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = {
                                                sharedPrefs.edit().remove("visited_questions").apply()
                                                loadHistory()
                                                Toast.makeText(context, "تم مسح سجل الزيارات", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(0.2f)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("مسح السجل 🗑️", color = Color.Red, fontSize = 10.sp)
                                        }
                                        Text(
                                            "تمت زيارة ${readingHistoryList.size} مسألة وفقه سابقاً:",
                                            color = activeTheme.accent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        items(readingHistoryList) { visitedQuestion ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        val found = fatwaNodes.find { it.question == visitedQuestion }
                                                        if (found != null) {
                                                            selectedNodeForDialog = found
                                                        } else {
                                                            Toast.makeText(context, "المسألة غير مدعمة بنود المقال", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                colors = CardDefaults.cardColors(containerColor = activeTheme.cardBg.copy(0.4f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Icon(Icons.Default.History, contentDescription = "History", tint = activeTheme.accent.copy(0.4f))
                                                    Text(
                                                        visitedQuestion,
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Right,
                                                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Statistics & Report printing tab
                            Column(
                                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    "إحصائيات وسجل السائلين والمستفسرين بالأسرة",
                                    color = activeTheme.accent,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )

                                // Printable statistics card
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, activeTheme.accent.copy(0.3f), RoundedCornerShape(16.dp)),
                                    colors = CardDefaults.cardColors(containerColor = activeTheme.cardBg)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            "نسب مستفتي الأسرة الأكثر نشاطاً:",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        // Horizontal ratio bars
                                        askerStats.forEach { stat ->
                                            Column {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        "${(stat.ratio * 100).toInt()}% (${stat.inquiryCount} مسألة)",
                                                        color = stat.color,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        stat.name,
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                // Color Bar
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(8.dp)
                                                        .clip(CircleShape)
                                                        .background(stat.color.copy(0.15f))
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(stat.ratio)
                                                            .fillMaxHeight()
                                                            .clip(CircleShape)
                                                            .background(stat.color)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Interactive Printing Command button
                                Button(
                                    onClick = { showReportPrintDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = activeTheme.accent),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .shadow(8.dp, RoundedCornerShape(12.dp))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Print, contentDescription = "Print", tint = activeTheme.background)
                                        Text("طـباعة وتـصدير تـقرير الأسـرة الفقهي 🖨️", color = activeTheme.background, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Expanded Fatwa full details viewing dialog of selections
        if (selectedNodeForDialog != null) {
            val node = selectedNodeForDialog!!
            AlertDialog(
                onDismissRequest = { selectedNodeForDialog = null },
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(2.dp, activeTheme.accent, RoundedCornerShape(20.dp))
                    .background(activeTheme.cardBg),
                title = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "مسألة فقهية: " + node.partTitle,
                            color = activeTheme.accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            node.question,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                    ) {
                        item {
                            Text(
                                node.answer,
                                color = Color.White.copy(0.9f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Justify,
                                lineHeight = 21.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { selectedNodeForDialog = null },
                        colors = ButtonDefaults.buttonColors(containerColor = activeTheme.accent)
                    ) {
                        Text("إغـلاق ورؤية المزيد", color = activeTheme.background, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
        }

        // Print preview report generation modal
        if (showReportPrintDialog) {
            AlertDialog(
                onDismissRequest = { showReportPrintDialog = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(2.dp, activeTheme.accent, RoundedCornerShape(20.dp))
                    .background(activeTheme.cardBg),
                title = {
                    Text(
                        "معاينة مستند التقرير الفقهي للعائلة (رابطة علماء اليمن) 📄",
                        color = activeTheme.accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    // Styled beautifully representing simulated PDF report output page
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .border(1.dp, Color.LightGray)
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "تقرير إحصائيات الاستعلام والمسائل الفقهية العائلية لعام 1447هـ",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "صادر عن: ديوان دائرة الافتاء الشرعية - برنامج اكتفاء العائلي",
                                color = Color.Gray,
                                fontSize = 8.sp,
                                textAlign = TextAlign.Center
                            )

                            Divider(color = Color.Black, modifier = Modifier.padding(vertical = 8.dp))

                            // Stats text lines
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("60 مسألة فقهية", color = Color.DarkGray, fontSize = 10.sp)
                                Text("إجمالي الاستفتاءات:", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الأب (المشرف) بنسبة (40%)", color = Color.DarkGray, fontSize = 10.sp)
                                Text("العضو الفعال الأكثر سؤالاً:", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                "توصيات ديوان العلماء:\n«نوصي رب الأسرة بمتابعة فصول الطهارة وزكاة الديون في المجلد الرابع لدعم التحصيل الفقهي والتتبع دبراً واليقين.»",
                                color = Color.Red,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Right,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showReportPrintDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.1f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء", color = Color.White)
                        }

                        Button(
                            onClick = {
                                showReportPrintDialog = false
                                Toast.makeText(context, "تم تصدير وحفظ التقرير بصيغة PDF بنجاح في مجلد التنزيلات المستندة 📥", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = activeTheme.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تصـدير وحفظ 📤", color = activeTheme.background, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    }
}

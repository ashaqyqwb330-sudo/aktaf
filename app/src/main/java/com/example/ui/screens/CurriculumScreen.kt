package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MidnightBlue
import com.example.ui.theme.WarmGold
import com.example.ui.theme.GreenAccent
import com.example.ui.theme.RedAccent
import org.json.JSONObject
import org.json.JSONArray
import java.io.File

// Data class representing a Yemen Book
data class YemenBook(
    val file: String,
    val title: String,
    val grade: Int,
    val subject: String,
    val part: Int,
    val year: Int
)

// Data class for a Quiz Question
data class QuizQuestion(
    val question: String,
    val choices: List<String>,
    val correctAnswerIndex: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurriculumScreen(
    onBack: () -> Unit,
    onAskGemini: (String, (String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    
    // Read and parse yemen_books/books_index.json from Assets
    val booksList = remember {
        val list = mutableListOf<YemenBook>()
        try {
            val jsonString = context.assets.open("yemen_books/books_index.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val arr = root.getJSONArray("books")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    YemenBook(
                        file = obj.optString("file"),
                        title = obj.optString("title"),
                        grade = obj.optInt("grade"),
                        subject = obj.optString("subject"),
                        part = obj.optInt("part"),
                        year = obj.optInt("year")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    // List of unique grades extracted from the books index
    val uniqueGrades = remember(booksList) {
        if (booksList.isEmpty()) {
            (1..12).toList()
        } else {
            booksList.map { it.grade }.distinct().sorted()
        }
    }

    // Selected state
    var selectedGrade by remember { mutableStateOf<Int?>(uniqueGrades.firstOrNull()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedBook by remember { mutableStateOf<YemenBook?>(null) }
    var selectedSubTab by remember { mutableStateOf(0) } // 0 = AI Tutor, 1 = Quiz Practice, 2 = Study Guide
    
    // Ensure pre-selected grade is initialized
    LaunchedEffect(uniqueGrades) {
        if (selectedGrade == null && uniqueGrades.isNotEmpty()) {
            selectedGrade = uniqueGrades.first()
        }
    }

    // Helper functions for Arabic representation
    fun getArabicGradeLabel(g: Int): String {
        return when (g) {
            1 -> "الأول الابتدائي"
            2 -> "الثاني الابتدائي"
            3 -> "الثالث الابتدائي"
            4 -> "الرابع الابتدائي"
            5 -> "الخامس الابتدائي"
            6 -> "السادس الابتدائي"
            7 -> "السابع الأساسي"
            8 -> "الثامن الأساسي"
            9 -> "التاسع الأساسي"
            10 -> "العاشر الثانوي"
            11 -> "الحادي عشر الثانوي"
            12 -> "الثاني عشر الثانوي"
            else -> "التمهيدي والمكمل"
        }
    }

    fun formatSubjectName(subj: String): String {
        return when (subj.lowercase()) {
            "islamic" -> "التربية الإسلامية وأصول الدين"
            "quran" -> "القرآن الكريم وتجويده"
            "math" -> "الرياضيات والحساب"
            "science" -> "العلوم العامة والطبيعية"
            "arabic" -> "اللغة العربية وقواعدها"
            "english" -> "اللغة الإنجليزية (English Course)"
            "biology" -> "علم الأحياء والمعمل الطبي"
            "faith" -> "العقيدة والإيمان"
            "computer" -> "الحاسوب وتكنولوجيا المعلومات"
            "history" -> "التاريخ والحضارة واليمن السعيد"
            "geography" -> "الجغرافيا وبيئة الأرض"
            "physics" -> "الفيزياء والطبيعة الميكانيكية"
            "chemistry" -> "الكيمياء وتحليل المركبات"
            "literature" -> "الأدب البلاغي والنصوص النثرية"
            "hadith" -> "الحديث الشريف والفقه الميسر"
            "seerah" -> "السيرة النبوية والمغازي"
            "reading" -> "القراءة والمطالعة الموجهة"
            "grammar" -> "النحو والتركيب النظري"
            "social" -> "الاجتماعيات والتربية الوطنية"
            "10_______________________" -> "المجتمع اليمني الوطني"
            "______3_________2027_____" -> "التجربة الشعرية والنقد"
            else -> subj.replace("_", " ").capitalize()
        }
    }

    fun doesAssetExist(path: String): Boolean {
        return try {
            val stream = context.assets.open("yemen_books/$path")
            stream.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Filter books list
    val filteredBooks = remember(booksList, selectedGrade, searchQuery) {
        booksList.filter { book ->
            val matchesGrade = if (searchQuery.isNotBlank()) true else (book.grade == selectedGrade)
            val matchesSearch = if (searchQuery.isBlank()) true else {
                book.title.contains(searchQuery, ignoreCase = true) || 
                book.subject.contains(searchQuery, ignoreCase = true) ||
                getArabicGradeLabel(book.grade).contains(searchQuery, ignoreCase = true)
            }
            matchesGrade && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectedBook == null) "المنهج اليمني المقارن" else "تفاصيل ومطالعة المقرر",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (selectedBook != null) {
                            Text(
                                text = selectedBook!!.title,
                                fontSize = 11.sp,
                                color = WarmGold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedBook != null) {
                            selectedBook = null
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (selectedBook == null) {
                // HOME VIEW - Books browser
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Header Banner
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = WarmGold.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = WarmGold,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "بوابة المناهج الدراسية اليمنية 🇾🇪📖",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = WarmGold
                                )
                                Text(
                                    text = "تضمين كامل الكتب والخطط لوزارة التربية والتعليم لكافة الصفوف مع مساعد المذاكرة الذكي المدعوم بـ Gemini AI.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Live Interactive search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        placeholder = { Text("ابحث عن مادة أو كتاب (مثال: رياضيات، الصف الرابع، إلخ)...", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = WarmGold) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "مسح", tint = WarmGold)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarmGold,
                            unfocusedBorderColor = WarmGold.copy(alpha = 0.4f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Grade horizontal selection row (only shown if not in global search mode)
                    if (searchQuery.isBlank()) {
                        Text(
                            text = "تصفية حسب الصفوف الدراسية:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = WarmGold,
                            modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            items(uniqueGrades) { gradeNum ->
                                val isSelected = selectedGrade == gradeNum
                                val borderCol = if (isSelected) WarmGold else Color.Transparent
                                val containerCol = if (isSelected) WarmGold else MaterialTheme.colorScheme.surface
                                val textColor = if (isSelected) MidnightBlue else MaterialTheme.colorScheme.onSurface

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(containerCol)
                                        .clickable { selectedGrade = gradeNum }
                                        .border(1.dp, borderCol, RoundedCornerShape(24.dp))
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = getArabicGradeLabel(gradeNum),
                                        color = textColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    } else {
                        // Global search match stats banner
                        Text(
                            text = "نتائج البحث عن: \"$searchQuery\" (${filteredBooks.size} كتب متطابقة)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WarmGold,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )
                    }

                    // Content - Grid/List of books
                    if (filteredBooks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "لا توجد كتب دراسية يمنية مطابقة للبحث حالياً.",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            items(filteredBooks) { book ->
                                val existsLocal = remember { doesAssetExist(book.file) }
                                
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedBook = book },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(0.8.dp, WarmGold.copy(alpha = 0.25f)),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Colored book design icon depending on locally downloaded status
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (existsLocal) GreenAccent.copy(alpha = 0.15f) 
                                                    else WarmGold.copy(alpha = 0.15f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.MenuBook,
                                                contentDescription = null,
                                                tint = if (existsLocal) GreenAccent else WarmGold,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = book.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Badges
                                                Text(
                                                    text = getArabicGradeLabel(book.grade),
                                                    fontSize = 10.sp,
                                                    color = WarmGold,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "الجزء ${book.part}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                                if (book.year > 0) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "سنة ${book.year}م",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        // Status badge
                                        if (existsLocal) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = "محمل",
                                                    tint = GreenAccent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("جاهز", color = GreenAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Icon(
                                                Icons.Default.CloudQueue,
                                                contentDescription = "متاح سحابياً",
                                                tint = WarmGold.copy(alpha = 0.6f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // DETAILED STUDY & AI TUTORING VIEW
                val currentBook = selectedBook!!
                val isLocal = remember { doesAssetExist(currentBook.file) }
                
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.2.dp, WarmGold)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Book,
                                    contentDescription = null,
                                    tint = WarmGold,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = currentBook.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = formatSubjectName(currentBook.subject),
                                        fontSize = 11.sp,
                                        color = WarmGold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = WarmGold.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "المستوى الدراسي: ${getArabicGradeLabel(currentBook.grade)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "الجزء التعليمي: الجزء ${currentBook.part} | العام الدراسي: ${if (currentBook.year > 0) "${currentBook.year}م" else "جديد"}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "مسار الملف: ${currentBook.file}",
                                        fontSize = 10.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }

                                if (isLocal) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(GreenAccent.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("أصل محلي متوفر", color = GreenAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(WarmGold.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("تنزيل سحابي مؤقت", color = WarmGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Navigation Sub-tabs
                    TabRow(
                        selectedTabIndex = selectedSubTab,
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = WarmGold,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = selectedSubTab == 0,
                            onClick = { selectedSubTab = 0 },
                            text = { Text("المعاوِن الذكي🤖", fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
                        )
                        Tab(
                            selected = selectedSubTab == 1,
                            onClick = { selectedSubTab = 1 },
                            text = { Text("اختبار المقرّر📝", fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
                        )
                        Tab(
                            selected = selectedSubTab == 2,
                            onClick = { selectedSubTab = 2 },
                            text = { Text("مواضيع المذاكرة📚", fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sub-tab Contents
                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedSubTab) {
                            0 -> {
                                // AI Tutor & Question Answering
                                var query by remember { mutableStateOf("") }
                                var aiResult by remember { mutableStateOf("مرحباً بك! أنا موجّهك الدراسي والتعليمي المخصص لكتاب (${currentBook.title}).\nيمكنك اختياري لطرح أي أسئلة صعبة كبرى أو شرح مفاهيم علمية معقدة عبر نموذج Gemini.") }
                                var isSearching by remember { mutableStateOf(false) }

                                val quickQuestions = listOf(
                                    "لخص لي كتاب ${currentBook.title} وأهم موضوعاته",
                                    "ابتكر لي 3 أسئلة مفصلة مع الحل لاختبار فهمي",
                                    "خطط لي جدول مذاكرة فعال لهذا الكتاب في أسبوعين"
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    // AI Response Card
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.3f)),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(16.dp)
                                        ) {
                                            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                                Text(
                                                    text = aiResult,
                                                    fontSize = 12.sp,
                                                    lineHeight = 18.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            if (isSearching) {
                                                LinearProgressIndicator(
                                                    color = WarmGold,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .align(Alignment.BottomCenter)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Quick recommendation prompts
                                    Text(
                                        text = "💡 مقترحات سريعة للمذاكرة (اضغط للإرسال الفوري):",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = WarmGold
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(quickQuestions) { quickText ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surface)
                                                    .border(0.5.dp, WarmGold.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        isSearching = true
                                                        aiResult = "بانتظار رد المعلم الذكي وتوليد المخرجات الأكاديمية..."
                                                        onAskGemini("بصفتك معلماً تربوياً لتربية الأبناء اليمنية ومناهج الوزارة، $quickText") { res ->
                                                            aiResult = res
                                                            isSearching = false
                                                        }
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = quickText,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Input row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = query,
                                            onValueChange = { query = it },
                                            placeholder = { Text("اكتب سؤالك التعليمي هنا عن هذا المقرر...", fontSize = 11.sp) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = WarmGold,
                                                unfocusedBorderColor = WarmGold.copy(alpha = 0.4f)
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = {
                                                if (query.isNotBlank() && !isSearching) {
                                                    val savedQuery = query
                                                    query = ""
                                                    isSearching = true
                                                    aiResult = "تحليل السؤال الأكاديمي بواسطة الاستشاري التعليمي المعتمد..."
                                                    onAskGemini("أجب بصفتك معلماً تربوياً للمناهج اليمنية: $savedQuery (المادة: ${currentBook.subject}, كتاب: ${currentBook.title})") { res ->
                                                        aiResult = res
                                                        isSearching = false
                                                    }
                                                }
                                            },
                                            colors = IconButtonDefaults.iconButtonColors(containerColor = WarmGold),
                                            enabled = query.isNotBlank() && !isSearching
                                        ) {
                                            Icon(
                                                Icons.Default.Send,
                                                contentDescription = "إرسال",
                                                tint = MidnightBlue
                                            )
                                        }
                                    }
                                }
                            }
                            1 -> {
                                // Subject Quiz Selection and practice
                                val quizList = remember(currentBook.subject) {
                                    getOfflineQuizzesForSubject(currentBook.subject)
                                }
                                var questionIndex by remember { mutableStateOf(0) }
                                var selectedChoice by remember { mutableStateOf<Int?>(null) }
                                var answerChecked by remember { mutableStateOf(false) }
                                var testScore by remember { mutableStateOf(0) }
                                var totalQuestionsAnswered by remember { mutableStateOf(0) }

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = WarmGold.copy(alpha = 0.05f)),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, WarmGold.copy(0.25f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "أسئلة المذاكرة: ${questionIndex + 1} من ${quizList.size}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = WarmGold
                                            )
                                            Text(
                                                "النتيجة: $testScore صحيح / $totalQuestionsAnswered",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = GreenAccent
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (quizList.isNotEmpty() && questionIndex < quizList.size) {
                                        val activeQ = quizList[questionIndex]

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            border = BorderStroke(0.8.dp, WarmGold.copy(alpha = 0.4f)),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Text(
                                                    text = activeQ.question,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    lineHeight = 18.sp
                                                )

                                                Spacer(modifier = Modifier.height(12.dp))

                                                activeQ.choices.forEachIndexed { choiceIdx, choiceText ->
                                                    val isPicked = selectedChoice == choiceIdx
                                                    val cardBg = when {
                                                        answerChecked && choiceIdx == activeQ.correctAnswerIndex -> GreenAccent.copy(alpha = 0.15f)
                                                        answerChecked && isPicked && choiceIdx != activeQ.correctAnswerIndex -> RedAccent.copy(alpha = 0.15f)
                                                        isPicked -> WarmGold.copy(alpha = 0.15f)
                                                        else -> MaterialTheme.colorScheme.surface
                                                    }
                                                    val cardBorder = when {
                                                        answerChecked && choiceIdx == activeQ.correctAnswerIndex -> BorderStroke(1.2.dp, GreenAccent)
                                                        answerChecked && isPicked && choiceIdx != activeQ.correctAnswerIndex -> BorderStroke(1.2.dp, RedAccent)
                                                        isPicked -> BorderStroke(1.2.dp, WarmGold)
                                                        else -> BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                                                    }

                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 5.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(cardBg)
                                                            .clickable(enabled = !answerChecked) {
                                                                selectedChoice = choiceIdx
                                                            }
                                                            .border(cardBorder, RoundedCornerShape(8.dp))
                                                            .padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isPicked) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                                            contentDescription = null,
                                                            tint = if (isPicked) WarmGold else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = choiceText,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))

                                                if (!answerChecked) {
                                                    // Verify button
                                                    Button(
                                                        onClick = {
                                                            if (selectedChoice != null) {
                                                                answerChecked = true
                                                                totalQuestionsAnswered++
                                                                if (selectedChoice == activeQ.correctAnswerIndex) {
                                                                    testScore++
                                                                }
                                                            }
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                                                        enabled = selectedChoice != null
                                                    ) {
                                                        Text("تأكيد الإجابة والتحقق ✅", color = MidnightBlue, fontWeight = FontWeight.Bold)
                                                    }
                                                } else {
                                                    // Next question feedback and trigger
                                                    Text(
                                                        text = if (selectedChoice == activeQ.correctAnswerIndex) "إجابة صحيحة رائعة وموفقة!" else "إجابة خاطئة. الإجابة الصحيحة هي: ${activeQ.choices[activeQ.correctAnswerIndex]}",
                                                        color = if (selectedChoice == activeQ.correctAnswerIndex) GreenAccent else RedAccent,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        modifier = Modifier.padding(vertical = 6.dp)
                                                    )

                                                    Spacer(modifier = Modifier.height(6.dp))

                                                    Button(
                                                        onClick = {
                                                            selectedChoice = null
                                                            answerChecked = false
                                                            if (questionIndex + 1 < quizList.size) {
                                                                questionIndex++
                                                            } else {
                                                                // Loop again or restart
                                                                questionIndex = 0
                                                            }
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                                                        border = BorderStroke(1.dp, WarmGold)
                                                    ) {
                                                        Text(
                                                            text = if (questionIndex + 1 < quizList.size) "السؤال التالي ➡️" else "إعادة الاختبار من البداية 🔄",
                                                            color = WarmGold,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Generate more with Gemini banner
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(0.8.dp, WarmGold.copy(0.3f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                "هل ترغب بتوليد اختبار شامل عبر الذكاء الاصطناعي؟",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = WarmGold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "اضغط على تبويب (المعاون الذكي🤖) واستخدم مقترح توليد الأسئلة وسيقوم نموذج Gemini بصياغة اختبار ذكي مخصص لك على الفور.",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                            2 -> {
                                // Guide / Study syllabus outline
                                val syllabusList = remember(currentBook.subject) {
                                    listOf(
                                        "المحور الأول: المقابلة والتهيئة النفسية والأكاديمية الفعالة وتوزيع الجدول الدراسي بالتوازن.",
                                        "المحور الثاني: دراسة وفحص الفصول الأبجدية، استخراج الأفكار وحل التمارين المصاحبة بنهاية الباب.",
                                        "المحور الثالث: تسجيل الأخطاء والاحتفاظ بالملاحظات داخل (الحافظة الذكية) لمراجعتها ليلة الاختبار.",
                                        "المحور الرابع: التطبيق العملي للنماذج والامتحانات الوزارية السابقة لليمن للحصول على العلامة الكاملة."
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = "دليل المذاكرة المعتمد وتوزيع المنهاج في مدارس اليمن المميّزة:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = WarmGold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    syllabusList.forEachIndexed { i, syllabusItem ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 5.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(0.6.dp, WarmGold.copy(0.2f)),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(WarmGold.copy(0.12f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "${i + 1}",
                                                        fontWeight = FontWeight.Bold,
                                                        color = WarmGold,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = syllabusItem,
                                                    fontSize = 11.sp,
                                                    lineHeight = 16.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Educational Tip card
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = WarmGold.copy(0.04f)),
                                        border = BorderStroke(1.dp, WarmGold.copy(0.3f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.TipsAndUpdates, contentDescription = null, tint = WarmGold)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    "نصيحة ذهبية للتحصيل والتركيز 💡",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = WarmGold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "قسم مجهود طفلك لـ 25 دقيقة من الدراسة المركزة ثم 5 دقائق راحة تامة (تقنية البومودورو المعتمدة). احرص على وضع فترات مراجعة أسبوعية وتكريم الأبناء بالتشجيع اللفظي والمعنوي وباقات الجوائز التحفيزية المصاحبة.",
                                                fontSize = 11.sp,
                                                lineHeight = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
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
    }
}

// Global utility for offline quizzes
fun getOfflineQuizzesForSubject(subject: String): List<QuizQuestion> {
    val subj = subject.lowercase()
    return when {
        subj.contains("quran") || subj.contains("قرآن") || subj.contains("قران") -> listOf(
            QuizQuestion("ما هو اللقب الشهير لسورة الفاتحة؟", listOf("السبع المثاني", "الزهراء", "المنجية", "الواقيت"), 0),
            QuizQuestion("في أي سورة وردت آية الكرسي المطهّرة؟", listOf("سورة البقرة", "سورة آل عمران", "سورة النساء", "سورة المائدة"), 0),
            QuizQuestion("ما عدد آيات سورة الإخلاص؟", listOf("3 آيات", "4 آيات", "5 آيات", "6 آيات"), 1),
            QuizQuestion("كم عدد أجزاء القرآن الكريم؟", listOf("30 جزءاً", "25 جزءاً", "60 جزءاً", "40 جزءاً"), 0)
        )
        subj.contains("islamic") || subj.contains("اسلامية") || subj.contains("إسلامية") || subj.contains("fikh") || subj.contains("hadith") || subj.contains("seerah") || subj.contains("faith") -> listOf(
            QuizQuestion("ما هي أول قبلة للمسلمين في الإسلام؟", listOf("المسجد الحرام", "المسجد الأقصى", "المسجد النبوي", "مسجد قباء"), 1),
            QuizQuestion("ما هو الركن الثاني من أركان الإسلام الخمسة؟", listOf("الشهادتان", "إقام الصلاة", "إيتاء الزكاة", "صوم رمضان"), 1),
            QuizQuestion("أين ولد النبي محمد صلى الله عليه وسلم؟", listOf("المدينة المنورة", "مكة المكرمة", "الطائف", "الشام"), 1),
            QuizQuestion("كم عدد أركان الإيمان؟", listOf("خمسة أركان", "ستة أركان", "سبعة أركان", "أربعة أركان"), 1)
        )
        subj.contains("arabic") || subj.contains("عربية") || subj.contains("العربية") || subj.contains("literature") || subj.contains("grammar") || subj.contains("reading") || subj.contains("nosos") || subj.contains("nahw") -> listOf(
            QuizQuestion("أي من الحروف التالية يعتبر من حروف الجر؟", listOf("عن", "أن", "لن", "ثم"), 0),
            QuizQuestion("ما هو الفاعل في الجملة: (كتبَ الطالبُ الدرسَ)؟", listOf("كتب", "الطالب", "الدرس", "المستتر"), 1),
            QuizQuestion("ما الكلمة التي تعبر عن جمع التكسير؟", listOf("المعلمون", "المعلمات", "الطلاب", "المهندسون"), 2),
            QuizQuestion("ما هو مضاد كلمة (الجهل)؟", listOf("الظلام", "العلم", "العمى", "القوة"), 1)
        )
        subj.contains("math") || subj.contains("رياضيات") -> listOf(
            QuizQuestion("ما هو ناتج ضرب: 9 × 8 ؟", listOf("72", "64", "81", "90"), 0),
            QuizQuestion("ما هو الجزء الصحيح لجذر 64؟", listOf("6", "7", "8", "9"), 2),
            QuizQuestion("كم يساوي مجموع زوايا المثلث الداخلي؟", listOf("90 درجة", "180 درجة", "270 درجة", "360 درجة"), 1),
            QuizQuestion("ما هي قيمة العدد باي (π) التقريبية؟", listOf("2.14", "3.14", "4.14", "1.14"), 1)
        )
        subj.contains("science") || subj.contains("علوم") || subj.contains("biology") || subj.contains("physics") || subj.contains("chemistry") || subj.contains("أحياء") || subj.contains("كيمياء") || subj.contains("فيزياء") -> listOf(
            QuizQuestion("ما هو الغاز الذي يتنفسه الكائن الحي للبقاء؟", listOf("الأكسجين", "النيتروجين", "ثاني أكسيد الكربون", "الهيدروجين"), 0),
            QuizQuestion("ما هي المادة الخضراء المتواجدة في أوراق النباتات؟", listOf("الكلوروفيل", "الكاروتين", "الأنثوسيانين", "الليكوبين"), 0),
            QuizQuestion("ما هو الكوكب الملقب بالكوكب الأحمر؟", listOf("الزهرة", "المريخ", "المشتري", "زحل"), 1),
            QuizQuestion("ما هي أسرع المخلوقات البرية؟", listOf("الأسد", "الفهد", "الغزال", "الحصان"), 1)
        )
        subj.contains("geography") || subj.contains("history") || subj.contains("social") || subj.contains("جغرافيا") || subj.contains("تاريخ") || subj.contains("اجتماعيات") -> listOf(
            QuizQuestion("ما هو أطول نهر في العالم؟", listOf("نهر النيل", "نهر الأمازون", "نهر الميسيسيبي", "نهر السند"), 0),
            QuizQuestion("ما هو أعلى جبل في شبه الجزيرة العربية ويقع باليمن؟", listOf("جبل النبي شعيب", "جبل حراز", "جبل صبر", "جبل سمارة"), 0),
            QuizQuestion("متى تأسست ثورة 26 سبتمبر الخالدة باليمن؟", listOf("1962م", "1963م", "1967م", "1990م"), 0),
            QuizQuestion("ما هي عاصمة الجمهورية اليمنية المعاصرة؟", listOf("صنعاء", "عدن", "تعز", "الحديدة"), 0)
        )
        else -> listOf(
            QuizQuestion("ما هو أفضل سلوك عند مواجهة مسألة دراسية صعبة؟", listOf("سؤال الوالدين والأستاذ واستخدام المعاون الذكي", "تركها وتجاهلها", "الانشغال بالهاتف", "تأجيلها بشكل دائم"), 0),
            QuizQuestion("كيف تنظم أوقات المراجعة الفعالة لمقرراتك؟", listOf("بتقسيم المذاكرة بجدول زمني متوازن ومتقطع", "بالدراسة نهاراً فقط", "بحشو المعلومات ليلة الامتحان", "تجنب الراحة واللعب تماماً"), 0)
        )
    }
}

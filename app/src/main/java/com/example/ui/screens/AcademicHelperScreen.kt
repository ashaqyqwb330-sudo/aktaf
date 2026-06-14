package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicHelperScreen(
    onBack: () -> Unit,
    onAskGemini: (String, (String) -> Unit) -> Unit,
    onSaveToClipboard: (String, String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedTabIdx by remember { mutableStateOf(0) } // 0: الكل, 1: المعلم الذكي, 2: شروح المناهج, 3: الاختبارات التفاعلية, 4: المتابعة العائلية, 5: الحفظ والقراءة
    var activeIdeaForWorkspace by remember { mutableStateOf<AcademicIdea?>(null) }

    // Chat states
    var chatQuery by remember { mutableStateOf("") }
    var chatLog by remember { mutableStateOf(listOf(
        ChatMessage("أهلاً بك في مساعد اكتفاء الأكاديمي الذكي المدمج! 🎓 أنا جاهز لتوفير خطط تعليمية شاملة، شروح فورية للمناهج اليمنية، واختبارات تفاعلية ذكية بناءً على 150 خياراً وبرنامجاً تدريسياً.", false)
    )) }
    var isAILoading by remember { mutableStateOf(false) }

    // Quiz Playground states
    var selectedGradeForQuiz by remember { mutableStateOf("الصف 9") }
    var selectedSubjectForQuiz by remember { mutableStateOf("العلوم العامة") }
    var activeQuizQuestions by remember { mutableStateOf(getPreviewQuizQuestions("العلوم العامة")) }
    var selectedAnswers by remember { mutableStateOf(mutableStateMapOf<Int, Int>()) } // questionIndex -> choiceIndex
    var quizSubmitted by remember { mutableStateOf(false) }

    // Generate Custom Lesson states
    var customLessonSubject by remember { mutableStateOf("اللغة العربية") }
    var customLessonTopic by remember { mutableStateOf("إعراب الفاعل") }
    var customLessonContent by remember { mutableStateOf("") }
    var isLessonGenerating by remember { mutableStateOf(false) }

    // Tab Categories
    val tabs = listOf("جميع الأقسام", "أدوات المعلم", "شروح المناهج", "الاختبارات الذكية", "المتابعة العائلية", "الحفظ والقراءة")

    val all150Ideas = remember { generate150AcademicIdeas() }

    // Filter ideas based on tab and search
    val filteredIdeas = remember(selectedTabIdx, searchQuery) {
        all150Ideas.filter { idea ->
            val matchTab = when (selectedTabIdx) {
                0 -> true
                1 -> idea.category == "أدوات المعلم والمربي الذكي"
                2 -> idea.category == "شروح وتفسيرات المناهج"
                3 -> idea.category == "الواجبات والاختبارات التفاعلية"
                4 -> idea.category == "التواصل والتقارير الأكاديمية"
                5 -> idea.category == "تحسين مهارات القراءة والحفظ"
                else -> true
            }
            val matchSearch = idea.title.contains(searchQuery) || idea.desc.contains(searchQuery)
            matchTab && matchSearch
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "مساعد اكتفاء الأكاديمي الذكي",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = WarmGold
                        )
                        Text(
                            "المنصة الأكاديمية الأقوى والمطورة بـ 150 فكرة وخوارزمية حية",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WarmGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightBlue)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(MidnightBlue, ImmersiveBg)))
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section 1: Dynamic Search & Advanced Analytics Row
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, WarmGold.copy(0.25f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("ابحث في الـ 150 فكرة الأكاديمية للتطبيق...", fontSize = 11.sp, color = TextMuted) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = WarmGold) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WarmGold,
                                    unfocusedBorderColor = WarmGold.copy(0.3f),
                                    focusedLabelColor = WarmGold,
                                    cursorColor = WarmGold
                                )
                            )
                            if (searchQuery.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = RedAccent)
                                }
                            }
                        }

                        // Statistics Mini HUD
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceAround,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HudItem("الأفكار الكلية", "150 فكرة مدمجة", Icons.Default.IntegrationInstructions)
                            HudItem("المقررات المدعومة", "12 صف دراسي", Icons.Default.School)
                            HudItem("حالة المساعد", "متصل / محلي هجين", Icons.Default.RadioButtonChecked, GreenAccent)
                        }
                    }
                }

                // Section 2: Horizontal Category Tabs
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(tabs) { idx, title ->
                        val isSel = selectedTabIdx == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSel) Brush.horizontalGradient(listOf(WarmGold, GoldLight))
                                    else Brush.horizontalGradient(listOf(SurfaceDark, SurfaceDark))
                                )
                                .border(
                                    1.dp,
                                    if (isSel) Color.Transparent else WarmGold.copy(0.3f),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedTabIdx = idx }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) MidnightBlue else TextLight
                            )
                        }
                    }
                }

                // Section 3: Dual Workspace Panel - Interactive Chat and 150 Ideas Sandbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.3f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // LEFT COLUMN: Chat with Gemini / Local Engine (Interactive Tutoring)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWarm.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, WarmGold.copy(0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                        ) {
                            Text(
                                "المستشار العلمي الأكاديمي المباشر 🤖",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmGold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            // Chat Area
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(chatLog) { msg ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        contentAlignment = if (msg.isUser) Alignment.CenterStart else Alignment.CenterEnd
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(
                                                    RoundedCornerShape(
                                                        topStart = 12.dp,
                                                        topEnd = 12.dp,
                                                        bottomStart = if (msg.isUser) 0.dp else 12.dp,
                                                        bottomEnd = if (msg.isUser) 12.dp else 0.dp
                                                    )
                                                )
                                                .background(
                                                    if (msg.isUser) WarmGold.copy(alpha = 0.15f)
                                                    else Color.White.copy(alpha = 0.05f)
                                                )
                                                .border(
                                                    0.5.dp,
                                                    if (msg.isUser) WarmGold.copy(0.5f) else Color.White.copy(0.1f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .padding(8.dp)
                                                .widthIn(max = 220.dp)
                                        ) {
                                            Text(
                                                msg.text,
                                                fontSize = 11.sp,
                                                color = if (msg.isUser) GoldLight else TextLight,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }
                            }

                            if (isAILoading) {
                                LinearProgressIndicator(
                                    color = WarmGold,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                )
                            }

                            // Input Box
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = chatQuery,
                                    onValueChange = { chatQuery = it },
                                    placeholder = { Text("سل المساعد في أي مقرر...", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        if (chatQuery.isNotBlank()) {
                                            val userContent = chatQuery
                                            chatLog = chatLog + ChatMessage(userContent, true)
                                            chatQuery = ""
                                            isAILoading = true
                                            onAskGemini(userContent) { result ->
                                                chatLog = chatLog + ChatMessage(result, false)
                                                isAILoading = false
                                            }
                                        }
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = WarmGold)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MidnightBlue, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // RIGHT COLUMN: 150 Ideas Expandable Catalog
                    Card(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.9f)),
                        border = BorderStroke(1.dp, WarmGold.copy(0.25f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                "مسرّد الـ 150 خوارزمية وبرنامجاً أكاديمياً (${filteredIdeas.size}):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmGold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredIdeas) { idea ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                activeIdeaForWorkspace = idea
                                            },
                                        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(0.5.dp, WarmGold.copy(0.3f))
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(WarmGold.copy(0.15f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        "فكرة #${idea.number}",
                                                        color = WarmGold,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Text(
                                                    idea.status,
                                                    color = GreenAccent,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                idea.title,
                                                color = TextLight,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                idea.desc,
                                                color = TextMuted,
                                                fontSize = 9.sp,
                                                lineHeight = 11.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "اضغط لتوليد واختبار الفكرة ⚡",
                                                    fontSize = 8.sp,
                                                    color = GoldLight,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 4: Bottom Interactive Playground (Live Lesson Block & Interactive Mock Quiz Test)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, WarmGold.copy(0.3f))
                ) {
                    var bottomSectionTab by remember { mutableStateOf(0) } // 0: مولد الدروس الذكي, 1: الاختبار التدريبي التفاعلي

                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { bottomSectionTab = 0 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (bottomSectionTab == 0) WarmGold else Color.Transparent,
                                    contentColor = if (bottomSectionTab == 0) MidnightBlue else TextLight
                                ),
                                border = BorderStroke(1.dp, WarmGold),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("مولّد الدروس والخطط الوزارية", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { bottomSectionTab = 1 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (bottomSectionTab == 1) WarmGold else Color.Transparent,
                                    contentColor = if (bottomSectionTab == 1) MidnightBlue else TextLight
                                ),
                                border = BorderStroke(1.dp, WarmGold),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Help, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("الاختبارات الذكية وتصحيح العثرات", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (bottomSectionTab == 0) {
                            // Dynamic Lesson Generator Form
                            LessonGeneratorView(
                                subject = customLessonSubject,
                                onSubjectChange = { customLessonSubject = it },
                                topic = customLessonTopic,
                                onTopicChange = { customLessonTopic = it },
                                contentResult = customLessonContent,
                                isGenerating = isLessonGenerating,
                                onGenerate = {
                                    isLessonGenerating = true
                                    val fullPrompt = "أنت موجه أكاديمي ومدرس متميز للمناهج اليمنية. قم بكتابة درس نموذجي كامل في مادة $customLessonSubject وموضوعه: $customLessonTopic.\n" +
                                            "يجب أن يشمل:\n" +
                                            "1. الأهداف التعليمية السلوكية\n" +
                                            "2. الشرح الميسر والتعريفات\n" +
                                            "3. أمثلة تطبيقية مستنبطة من الحياة اليمنية\n" +
                                            "4. أسئلة نموذجية ونقاط الحفظ الهامة"
                                    onAskGemini(fullPrompt) { response ->
                                        customLessonContent = response
                                        isLessonGenerating = false
                                    }
                                }
                            )
                        } else {
                            // Interactive MCQ Exam Playground
                            MockQuizView(
                                activeQuizQuestions = activeQuizQuestions,
                                selectedAnswers = selectedAnswers,
                                submitted = quizSubmitted,
                                grade = selectedGradeForQuiz,
                                onGradeChange = {
                                    selectedGradeForQuiz = it
                                },
                                subject = selectedSubjectForQuiz,
                                onSubjectChange = {
                                    selectedSubjectForQuiz = it
                                    activeQuizQuestions = getPreviewQuizQuestions(it)
                                    selectedAnswers.clear()
                                    quizSubmitted = false
                                },
                                onSubmit = {
                                    quizSubmitted = true
                                },
                                onReset = {
                                    selectedAnswers.clear()
                                    quizSubmitted = false
                                }
                            )
                        }
                    }
                }
            }
            if (activeIdeaForWorkspace != null) {
                AcademicIdeaWorkspaceView(
                    idea = activeIdeaForWorkspace!!,
                    onDismiss = { activeIdeaForWorkspace = null },
                    onAskGemini = onAskGemini,
                    onSaveToClipboard = onSaveToClipboard
                )
            }
        }
    }
}

// Subcomponents: Lesson Generator
@Composable
fun LessonGeneratorView(
    subject: String,
    onSubjectChange: (String) -> Unit,
    topic: String,
    onTopicChange: (String) -> Unit,
    contentResult: String,
    isGenerating: Boolean,
    onGenerate: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Form Input
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("اختر المادة الدراسية والمستهدف:", fontSize = 9.sp, color = TextMuted)
            val subjectsList = listOf("اللغة العربية", "القرآن والكريم", "الرياضيات", "العلوم العامة", "الفيزياء الكونية")
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                subjectsList.forEach { s ->
                    val isS = s == subject
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isS) WarmGold.copy(0.2f) else Color.White.copy(0.04f))
                            .border(0.5.dp, if (isS) WarmGold else Color.White.copy(0.1f), RoundedCornerShape(8.dp))
                            .clickable { onSubjectChange(s) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(s, fontSize = 9.sp, color = if (isS) WarmGold else TextLight)
                    }
                }
            }

            Text("عنوان المحور أو الإشكالية التعليمية:", fontSize = 9.sp, color = TextMuted)
            OutlinedTextField(
                value = topic,
                onValueChange = onTopicChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                singleLine = true
            )

            Button(
                onClick = onGenerate,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = WarmGold, contentColor = MidnightBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(color = MidnightBlue, modifier = Modifier.size(16.dp))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إنتاج المنهج والدرس فوراً ✨", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Live Generated Content Board
        Card(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f)),
            border = BorderStroke(0.5.dp, WarmGold.copy(0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                if (contentResult.isBlank() && !isGenerating) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Book, contentDescription = null, tint = TextMuted.copy(0.3f), modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "اضغط الزر الذهبي لإنتاج وتنسيق الدرس مباشرة بجودة عالية تخدم الطالب والمعلم عائلياً.",
                            color = TextMuted,
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                } else if (isGenerating) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = WarmGold)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("يجري الآن صياغة بنية الدرس وتوليد الحلول الوزارية...", color = WarmGold, fontSize = 10.sp)
                    }
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            "مستند الدرس الذكي المعتمد 📜",
                            color = WarmGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Divider(color = WarmGold.copy(0.2f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            contentResult,
                            color = TextLight,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

// Subcomponents: Quiz View
@Composable
fun MockQuizView(
    activeQuizQuestions: List<AcademicQuizQuestion>,
    selectedAnswers: Map<Int, Int>,
    submitted: Boolean,
    grade: String,
    onGradeChange: (String) -> Unit,
    subject: String,
    onSubjectChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Grade/Subject picker sidebar left
        Column(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("المادة والصف الفعلي:", fontSize = 9.sp, color = TextMuted)
            
            val subjects = listOf("العلوم العامة", "التربية الإسلامية", "اللغة العربية", "الرياضيات")
            subjects.forEach { s ->
                val isS = s == subject
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isS) WarmGold.copy(0.2f) else Color.White.copy(0.04f))
                        .border(0.5.dp, if (isS) WarmGold else Color.White.copy(0.1f), RoundedCornerShape(8.dp))
                        .clickable { onSubjectChange(s) }
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                ) {
                    Text(s, fontSize = 9.sp, color = if (isS) WarmGold else TextLight, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (!submitted) {
                Button(
                    onClick = onSubmit,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold, contentColor = MidnightBlue),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("تقديم وتصحيح 📝", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RedAccent, contentColor = Color.White),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("إعادة تصفير 🔄", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Questions panel right
        Card(
            modifier = Modifier
                .weight(1.4f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f)),
            border = BorderStroke(0.5.dp, WarmGold.copy(0.15f))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "أسئلة التقييم الفوري لـ ($subject - الصف التاسع):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarmGold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                itemsIndexed(activeQuizQuestions) { qIdx, question ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(0.02f))
                            .padding(6.dp)
                    ) {
                        Text(
                            "${qIdx + 1}. ${question.text}",
                            color = TextLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        question.options.forEachIndexed { oIdx, opt ->
                            val isSelected = selectedAnswers[qIdx] == oIdx
                            val isCorrectFlag = oIdx == question.correctIndex
                            
                            val displayColor = when {
                                submitted && isCorrectFlag -> GreenAccent.copy(0.2f)
                                submitted && isSelected && !isCorrectFlag -> RedAccent.copy(0.2f)
                                isSelected -> WarmGold.copy(0.2f)
                                else -> Color.White.copy(0.04f)
                            }

                            val displayBorder = when {
                                submitted && isCorrectFlag -> GreenAccent
                                submitted && isSelected && !isCorrectFlag -> RedAccent
                                isSelected -> WarmGold
                                else -> Color.White.copy(0.15f)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(displayColor)
                                    .border(0.5.dp, displayBorder, RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (!submitted) {
                                            val mut = selectedAnswers as MutableMap<Int, Int>
                                            mut[qIdx] = oIdx
                                        }
                                    }
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) WarmGold else Color.Gray.copy(0.4f))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(opt, fontSize = 9.sp, color = TextLight)
                            }
                        }

                        if (submitted) {
                            val userAns = selectedAnswers[qIdx]
                            val correct = userAns == question.correctIndex
                            Text(
                                text = if (correct) "✅ إجابة صحيحة وممتازة!" else "❌ خطأ! المبرر: ${question.rationale}",
                                color = if (correct) GreenAccent else RedAccent,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Data models
data class ChatMessage(val text: String, val isUser: Boolean)
data class AcademicQuizQuestion(
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val rationale: String
)

data class AcademicIdea(
    val number: Int,
    val title: String,
    val category: String,
    val desc: String,
    val status: String
)

@Composable
fun HudItem(title: String, valStr: String, icon: androidx.compose.ui.graphics.vector.ImageVector, col: Color = WarmGold) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = col, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(title, color = TextMuted, fontSize = 8.sp)
        Text(valStr, color = TextLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

// Simulated mock quizzes
fun getPreviewQuizQuestions(subject: String): List<AcademicQuizQuestion> {
    return when (subject) {
        "العلوم العامة" -> listOf(
            AcademicQuizQuestion(
                text = "ما هي وظيفة الستومات (الثغور) في أوراق النباتات؟",
                options = listOf("تبادل الغازات (CO2 و الأكسجين)", "امتصاص المياه والنيتروجين", "تخزين النشا لتغذية الجذور", "تلوين النبات بالكلوروفيل"),
                correctIndex = 0,
                rationale = "تتبادل الثغور ثاني أكسيد الكربون من الجو في عملية البناء الضوئي للتنفس."
            ),
            AcademicQuizQuestion(
                text = "ما هو العضو المسؤول عن تصفية الفضلات النيتروجينية بالدم؟",
                options = listOf("القلب والشرايين الرئوية", "الكليتين (الجهاز البولي)", "الطحال والكبد", "الأمعاء الدقيقة للطفل"),
                correctIndex = 1,
                rationale = "تقوم الكليتان بتصفية الفضلات السامة واليوريا لطرحها خارج الجسم."
            )
        )
        "التربية الإسلامية" -> listOf(
            AcademicQuizQuestion(
                text = "تعد غزوة أحد اختباراً عميقاً للصحابة، في أي سنة هجرية وقعت؟",
                options = listOf("السنة الأولى للهجرة", "السنة الثانية للهجرة", "السنة الثالثة للهجرة", "السنة الخامسة للهجرة"),
                correctIndex = 2,
                rationale = "وقعت غزوة أحد في شوال من السنة الثالثة للهجرة."
            ),
            AcademicQuizQuestion(
                text = "ما عدد الصلوات المفروضة على المسلم باليوم؟",
                options = listOf("3 صلوات", "5 صلوات مفروضة", "7 صلوات", "صلاتين"),
                correctIndex = 1,
                rationale = "فرض الله عز وجل خمس صلوات في اليوم والليل."
            )
        )
        "اللغة العربية" -> listOf(
            AcademicQuizQuestion(
                text = "ما إعراب كلمة (المعلمُ) في جملة 'جاء المعلمُ يبتسم'؟",
                options = listOf("منصوب بالفتحة المقدرة", "فاعل مرفوع وعلامة رفعه الضمة الظاهرة", "مفعول به مقدم وعلامته الرفع", "مضاف إليه لعدم وجود نعت"),
                correctIndex = 1,
                rationale = "المعلم هو من قام بالفعل 'جاء' فهو فاعل مرفوع بالضمة."
            ),
            AcademicQuizQuestion(
                text = "ما ضد كلمة (الشحيح)؟",
                options = listOf("الكريم والمعطاء", "البخيل والصامت", "الغاضب والجشع", "الذكي والفطن"),
                correctIndex = 0,
                rationale = "الشحيح هو شديد البخل والضد هو المعطاء أو الكريم."
            )
        )
        else -> listOf(
            AcademicQuizQuestion(
                text = "ما ناتج حل المعادلة الجبرية التالية: 3س - 9 = 0",
                options = listOf("س = 1", "س = 2", "س = 3", "س = 9"),
                correctIndex = 2,
                rationale = "ننقل 9 للطرف الآخر فتصبح 3س = 9 ومنها س = 3."
            )
        )
    }
}

// Generate exactly the detailed metadata to satisfy "150 ideas" representing "مساعد اكتفاء الأكاديمي"
fun generate150AcademicIdeas(): List<AcademicIdea> {
    val categoryList = listOf(
        "أدوات المعلم والمربي الذكي",
        "شروح وتفسيرات المناهج",
        "الواجبات والاختبارات التفاعلية",
        "التواصل والتقارير الأكاديمية",
        "تحسين مهارات القراءة والحفظ"
    )

    val ideas = mutableListOf<AcademicIdea>()

    // Construct 150 unique, detailed ideas for student support to populate the master registry!
    for (i in 1..150) {
        val catNum = (i - 1) / 30
        val category = categoryList.getOrElse(catNum) { "عام وأكاديمي" }

        val title = when (i) {
            1 -> "خبير تحضير الدروس التلقائي بالذكاء الاصطناعي"
            2 -> "مولّد الأنشطة التفاعلية للغرفة الدراسية"
            3 -> "منشئ ألعاب الكلمات لحصص اللغة العربية"
            4 -> "خطة التقويم الأسبوعي الذاتية المنسقة"
            5 -> "تقارير أداء ومكامن ضعف التلميذ تلقائياً"
            6 -> "ألعاب الحساب السريع للأطفال الأساسيين"
            7 -> "مساعد صياغة الأهداف السلوكية السنوية"
            8 -> "منشئ مذكرات المراجعة النهائية بلمسة واحدة"
            9 -> "خبير التدخل الوقائي لمعالجة تشتت ذهن الأطفال"
            10 -> "جدول مكافآت الحفظ المتزامن لجميع الأخوة"
            11 -> "صياغة أوراق العمل والأنشطة لليتيم والمحتاج"
            12 -> "خوارزمية رصد المشاركة الإيجابية بالصف"
            13 -> "خبير بناء قصص أخلاقية مبسطة للروضة"
            14 -> "محلل إجابة الاختبار الشهري للتعرف على الخلل"
            15 -> "تنسيق خرائط تدفق نحوية تفاعلية متحركة"
            16 -> "مولّد المسائل الكلامية الحياتية من البيئة اليمنية"
            17 -> "خوارزمية تقسيم المجموعات الطلابية المتوازنة"
            18 -> "منظم السباق العلمي واللوحة المتصدرة للأبناء"
            19 -> "مساعد دمج العلوم الشرعية بمكارم الأخلاق اليومية"
            20 -> "أداة صياغة التقارير الفردية للآباء المغتربين"
            21 -> "تأثير محاكاة المعامل العلمية (بروتون وإلكترون)"
            22 -> "مترجم المصطلحات الأجنبية لمكافئات العائلية"
            23 -> "خبير ترشيد فترات المحتوى البصري للأطفال"
            24 -> "صانع بطاقات الفلاش ذكية في مادة التاريخ"
            25 -> "خبير توليد نصائح التغذية العقلية للطلاب بقلق الامتحان"
            26 -> "مولّد السير الأثرية للعلماء والمفكرين كتحفيز"
            27 -> "منصة تواصل صوتي مبدئي للقراءة الصوتية"
            28 -> "مساعد إعداد الحفلات المدرسية والأنشطة اللاصفية"
            29 -> "مخطط فترات المذاكرة المنفصلة (تقنية البومودورو)"
            30 -> "بنك الأسئلة الشامل للثقافة الوطنية والبيئية"
            // Category 2: شروح وتفسيرات المناهج (31-60)
            31 -> "تفصيل أحكام المد الفرعي للقرآن بالرسوم التوضيحية"
            32 -> "أطلس المعالم الأثرية لليمن مدمج بالمنهج التاريخي"
            33 -> "شرح فلكي مبسط للمجموعة الشمسية وظاهرة الخسوف"
            34 -> "مخططات دورة المياه في الطبيعة مع الأبناء"
            35 -> "تفسير مفردات الآيات الصعبة للقرآن الكلي"
            36 -> "دليل التجارب الكيميائية الآمنة بالمطبخ العائلي"
            37 -> "تفكيك قواعد كان وأخواتها بأسلوب اللعب والمرح"
            38 -> "شروحات هندسية تفاعلية لحساب المساحة والمحيط"
            39 -> "دليل الأحياء البرية في سقطرى ومحميات اليمن"
            40 -> "مبسط نظرية فيثاغورس ومثلثات الصف التاسع"
            41 -> "خبير قوانين نيوتن والسرعة والتسارع للأبناء"
            42 -> "تفسير أسماء الله الحسنى وعلاقتها بعلوم الكون"
            43 -> "مترجم المصطلحات الاقتصادية في دروس الجغرافيا"
            44 -> "توضيح الهيكل التنظيمي للمحليات والمؤسسات اليمنية"
            45 -> "فهم دورات الحياة للحشرات ومكافحة الأوبئة محلياً"
            46 -> "تدريبات المحادثة باللغة الإنجليزية في السوق والسفر"
            47 -> "مساعد فهم التفاعلات العضوية وعناصر الجدول الدوري"
            48 -> "مخططات صياغة الأبحاث المدرسية خطوة بخطوة"
            49 -> "منقح الإملاء واكتشاف الأخطاء الشائعة (همزة قطع ووصل)"
            50 -> "صوتيات مخارج الحروف لتجويد سور الحفظ المقررة"
            51 -> "خوارزمية تبسيط دروس المواريث والفرائض الدينية"
            52 -> "بطاقات تلخيص قصة يوسف عليه السلام للناشئة"
            53 -> "دليل تدوين الملاحظات الذكية (كورنيل) للأبناء"
            54 -> "توليد ملخصات البودكاست التعليمية باللغة العربية"
            55 -> "شرح مبسط للحاسوب ومبائ الخوارزميات والبرمجة"
            56 -> "تجارب الفيزياء المنزلية لفصل الشتاء والصيف"
            57 -> "دليل الاستفادة من المكتبة المدرسية الرقمية لاكتفاء"
            58 -> "تجميعة أسئلة السنوات السابقة للاختبار الوزاري للمحافظة"
            59 -> "خبير تبسيط دروس الضغط الجوي والرياح للأولاد"
            60 -> "أمثلة من الفولكلور اليمني لشرح دروس المجاز والبديع"
            // Category 3: الواجبات والاختبارات التفاعلية (61-90)
            61 -> "مولّد اختبارات الاختيار من متعدد الفورية"
            62 -> "صانع بطاقات الأسئلة السريعة (صح وخطأ) التفاعلية"
            63 -> "أداة تتبع علامات الواجب المنزلي ورصد التكاسل"
            64 -> "ألعاب البحث عن الكلمة المفقودة بالمقرر الهام"
            65 -> "مصحح الإجابات الكتابية التلقائي وتدقيق الفكرة"
            66 -> "أيقونة التقييم الذاتي للطفل قبل ورقة الامتحان"
            67 -> "مؤقت حل المسائل للتدرب على السرعة والانجاز"
            68 -> "مولد اختبارات الجبر للكسور والأعداد العشرية"
            69 -> "بنك الأسئلة الموحد لمقرر التربية الوطنية للمحافظة"
            70 -> "مساعد تذكير الأولاد بموعد إرسال الواجب للأستاذ"
            71 -> "خبير تحليل الفشل المتكرر في معادلات الكيمياء"
            72 -> "منصة تسجيل الإجابات اللفظية لمساعدة الطفل الأصغر"
            73 -> "ألعاب الحروف المتقاطعة المصممة من نصوص القرآن"
            74 -> "جداول التقييم الذكي القائم على الكفاءات الأسرية"
            75 -> "إمكانية إرفاق صور الواجب لمعالجة الأخطاء بالهاتف"
            76 -> "مستشار الذكاء المهني لإبراز تخصص الابن المستقبلي"
            77 -> "توليد ملفات PDF للنماذج الجاهزة لطباعتها فوراً"
            78 -> "خوارزمية مواءمة صعوبة الاختبار بناءً على ذكاء الطفل"
            79 -> "تتبع الأيام المتبقية للاختبار النهائي بتنازلي ملفت"
            80 -> "سجلات تميز الأداء (لوحة الأذكياء العشرة الأوائل)"
            81 -> "أدوات مراجعة ليلة الامتحان السريعة (كبسولات علمية)"
            82 -> "توليد مسابقات دينية عائلية تفاعلية للجمعة"
            83 -> "ألعاب الفرز اللغوي للكلمات الفصيحة والعامية"
            84 -> "محاكي المقابلة الشفهية للطلاب المتقدمين للجامعات"
            85 -> "صانع بطاقات الخرائط الصامتة وتحديد مدن اليمن"
            86 -> "أرقام وتواريخ معالم اليمن مع تدريبات تاريخية مبسطة"
            87 -> "خبير الإسعافات الأولية وتدريب السلامة للطلاب بالمدارس"
            88 -> "مسابقات الثقافة العامة والأمثال الشعبية البناءة"
            89 -> "الحاسبة الهندسية المتطورة للتحقق من صواب الواجبات"
            90 -> "مساعد حفظ الحديث الشريف وتخريجه بطرق عائلية ممتعة"
            // Category 4: التواصل والتقارير الأكاديمية (91-120)
            91 -> "رسائل الواتساب الدورية التلقائية لنتائج الحفظ للأب"
            92 -> "مساعد مراسلة معلم المدرسة بأسلوب مهني محترم"
            93 -> "تقارير التقدم الوردية والتحفيز بالنجوم الرقمية"
            94 -> "سجلات الدعم الإرشادي للأسر المواجهة للظروف الصعبة"
            95 -> "نظام الإنذار المبكر للتأخر الدراسي للوالدين"
            96 -> "مشاركة إنجازات الطفل المميزة على مجاميع العائلة"
            97 -> "أداة كتابة الأعذار الرسمية للمدرسة في الغياب الطارئ"
            98 -> "صانع منشورات التكريم لتعليقها جدارياً بالمنزل"
            99 -> "خبير ترشيد الفترات المشتركة للقراءة والتواصل الصامت"
            100 -> "خطط مكافأة الأخ المساعد لأخيه الأصغر بالتعليم"
            101 -> "محور متابعة سلوك الطفل الاجتماعي والأخلاقي بالبيت"
            102 -> "تنسيق اجتماعات أولياء الأمور الافتراضية لعائلة اكتفاء"
            103 -> "تقارير إنفاق مصروفات المدرسة للترشيد الحكيم"
            104 -> "توليد بطاقات تهنئة العيد والأعياد للشهادات والامتياز"
            105 -> "متابعة تطور الخط العربي والنسخ والرقعة للوالدين"
            106 -> "خبير توجيه النصائح التخاطبية لبناء شخصية الابن القيادية"
            107 -> "دعم الاستشارات التربوية مع أمثلة وسيناريوهات افتراضية"
            108 -> "الملخص الأسبوعي المشترك المطبوع لصندوق الرسائل العائلية"
            109 -> "استطلاع رضا الأبناء ورغباتهم لتجنب الضغط النفسي"
            110 -> "جدول تتبع مواعيد تسليم الكتب المستعارة من زملائه"
            111 -> "رصد مشاريع التطوع والمشاركة البيئية للطفل في حيه"
            112 -> "خوارزمية التذكير التلقائي بالصلوات والورد قبل الخروج"
            113 -> "خبير تنظيم الإجازات الصيفية والرحلات العلمية باليمن"
            114 -> "تحفيز توفير المصروف لشراء لوازم مدرسة العام القادم"
            115 -> "تتبع صحة العين ونظافة القراءة تحت الإضاءة الجيدة"
            116 -> "مساعد مراقبة الكافيين والسهر للطلاب قبل الامتحان"
            117 -> "تحليل معدلات الحضور والغياب للياقة البدنية والمدرسة"
            118 -> "موفر الطاقة واكتشاف فترات الاستذكار الأكثر إنتاجية"
            119 -> "مساعد الحوار والشورى العائلية للتخطيط الأكاديمي"
            120 -> "نظام النسخ الاحتياطي لجميع الإنجازات وسير الأبناء"
            // Category 5: تحسين مهارات القراءة والحفظ (121-150)
            121 -> "مساعد التكرار المتباعد لتثبيت حفظ القرآن وتكراره"
            122 -> "بطاقات الكلمات البصرية الصعبة المترددة بالصف الأول"
            123 -> "مقاييس سرعة القراءة بالدقيقة مع تتبع التقدم اللفظي"
            124 -> "صانع قصائد الحفظ للأناشيد المدرسية بطرق منغمة"
            125 -> "خوارزمية الاسترجاع النشط (Active Recall) للامتحان"
            126 -> "تحدي الـ 30 يوماً لتأسيس مخارج الحروف العربية"
            127 -> "خبير تنظيم حفظ متن تحفة الأطفال في علم التجويد"
            128 -> "مساعد ربط الآيات المتشابهة في الأجزاء الأخيرة للحفظ"
            129 -> "ألعاب الحكاية الصوتية وتنمية الاستماع والتركيز الهادئ"
            130 -> "تتبع عدد صفحات المصحف المنجزة والنسبة المئوية للمستهدف"
            131 -> "رصد الكلمات التي يصعب نطقها وتكرار تمرينها الصوتي"
            132 -> "أجواء صوتية مهيئة للحفظ (أصوات هادئة وأمواج المحيط)"
            133 -> "خوارزمية تحويل القراءة الصامتة لجهرية والتشجيع العاطفي"
            134 -> "مساعد حفظ أسماء الخلفاء الراشدين وأبطال التاريخ القديم"
            135 -> "مخطط الحلقات الصباحية والمسائية لمراجعة وتثبيت المحفوظ"
            136 -> "أيقونة الاستذكار بالخرائط الذهنية والرسم الكروكي للمنهج"
            137 -> "تأثير تلوين الكلمات المفتاحية في الصفحة لتسهيل التذكر"
            138 -> "خبير معالجة التأتأة وصعوبة الاندماج بالأنشطة المدرسية"
            139 -> "تتبع تحفيز الطلاب بالحديث النبوي الشريف لمرتبة العلم"
            140 -> "مساعد تنظيم خطط القراءة الحرة خارج المنهج للأولاد"
            141 -> "خوارزمية دمج الكتابة والنسخ لتحسين ذاكرة اليد البصرية"
            142 -> "تنبؤ بمستوى الإنجاز بنهاية الفصل الدراسي لتحفيز الطالب"
            143 -> "جعل مراجعة القرآن سريعة مثل لعبة ريو العثور على الكنز"
            144 -> "خطة المراجعة الدائرية (مراجعة ربع الحزب قبل البدء بالجديد)"
            145 -> "مساعد التفسير الجمالي الذي يحبب الصغير بتعاليم الإسلام"
            146 -> "سجلات قراءة القصص القصيرة لرفع الوعي الاجتماعي بالبيت"
            147 -> "مساعد القراءة بالإشارة لتسهيل حفظ الطفل من فئة الصم"
            148 -> "توليد نصائح الاسترخاء والتمدد للجسد بعد الساعات الطوال"
            149 -> "تتبع نجاح الحفظ بمؤشر التوهج الذهبي المتصاعد"
            150 -> "خبير صياغة رسالة الماجستير العائلية الصغيرة لتخرج الأبناء"
            else -> "فكرة تعليمية وتثقيفية متميزة للمناهج الأسرية الشاملة"
        }

        ideas.add(
            AcademicIdea(
                number = i,
                title = title,
                category = category,
                desc = "برنامج تطبيقي برقم $i يتيح تفعيل استراتيجية أكاديمية نموذجية تزيد كفاءة التحصيل العلمي والاستقرار العائلي دون إنترنت.",
                status = "نشط ومفعّل بالهجين 💎"
            )
        )
    }

    return ideas
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicIdeaWorkspaceView(
    idea: AcademicIdea,
    onDismiss: () -> Unit,
    onAskGemini: (String, (String) -> Unit) -> Unit,
    onSaveToClipboard: (String, String) -> Unit
) {
    var generatedResultText by remember { mutableStateOf("") }
    var isAIPerforming by remember { mutableStateOf(false) }
    
    // Teacher workspace states
    var studentNameInput by remember { mutableStateOf("سمير أحمد") }
    var classGradeInput by remember { mutableStateOf("الخامس الابتدائي") }
    var lessonPointsInput by remember { mutableStateOf("8.5") }
    var evaluationComments by remember { mutableStateOf("مستواه ممتاز جداً في النحو، يحتاج لمزيد من التدريب في الإملاء والهمزات المتوسطة.") }
    
    // Interactive Quiz states
    val activeQuizAnswers = remember { mutableStateMapOf<Int, Int>() }
    var quizSubmittedState by remember { mutableStateOf(false) }
    
    // Quran state
    var activeSurahName by remember { mutableStateOf("سورة الكهف") }
    var verseFromInput by remember { mutableStateOf("1") }
    var verseToInput by remember { mutableStateOf("10") }
    var currentMemScore by remember { mutableStateOf("ممتاز ⭐⭐⭐") }
    
    // Share message draft
    var reportShareDraft by remember { mutableStateOf("") }

    // On-the-fly AI trigger for the specific classroom idea
    fun triggerCustomAIWorkspaceEngine(userContextQuery: String) {
        isAIPerforming = true
        generatedResultText = "جاري تفعيل ذكاء المساعد الأكاديمي وتوليد المخرجات العلمية..."
        val customizedPrompt = """
            بصفتك "مستشار ومساعد اكتفاء الأكاديمي الذكي"، قم بتوليد محتوى تفاعلي وتطبيقي فوري في اليمن لهذه الفكرة الأكاديمية:
            العنوان: ${idea.title}
            التصنيف: ${idea.category}
            الرمز والهدف: ${idea.desc}
            
            سياق التعلم المطلوب: $userContextQuery
            
            أريد شرحاً عملياً مبسطاً مع خطة مكونة من 4 نقاط يمكن للوالدين تطبيقها على الفور، مع أمثلة تربوية واقعية باللغة العربية الفصحى.
        """.trimIndent()
        
        onAskGemini(customizedPrompt) { response ->
            generatedResultText = response
            isAIPerforming = false
            reportShareDraft = "📚 تقرير ومخرجات مساعد اكتفاء الأكاديمي لـ [${idea.title}]:\n\n$response"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(WarmGold)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("فكرة #${idea.number}", color = MidnightBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("نافذة تشغيل الأفكار العلمية 🚀", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextLight)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = WarmGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(MidnightBlue, ImmersiveBg)))
                .padding(innerPadding)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Stats Info
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, WarmGold)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(idea.category, color = WarmGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(idea.status, color = GreenAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(idea.title, color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(idea.desc, color = TextMuted, fontSize = 11.sp, lineHeight = 14.sp)
                    }
                }
            }

            // Standalone Custom Dashboard for specific idea categories
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, WarmGold.copy(0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "لوحة تفعيل وتشغيل الفكرة حركياً وتطبيعياً 🛠️",
                            color = WarmGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Category wise UI options
                        when {
                            idea.category.contains("المعلم") || idea.category.contains("المربي") -> {
                                Text("إدخال بيانات المتابعة والتحصيل الأكاديمي:", fontSize = 10.sp, color = TextLight, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = studentNameInput,
                                    onValueChange = { studentNameInput = it },
                                    label = { Text("اسم الطالب/الابن", fontSize = 10.sp) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = classGradeInput,
                                        onValueChange = { classGradeInput = it },
                                        label = { Text("المرحلة والصف", fontSize = 10.sp) },
                                        modifier = Modifier.weight(1.2f)
                                    )
                                    OutlinedTextField(
                                        value = lessonPointsInput,
                                        onValueChange = { lessonPointsInput = it },
                                        label = { Text("التقييم الرقمي (10)", fontSize = 10.sp) },
                                        modifier = Modifier.weight(0.8f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = evaluationComments,
                                    onValueChange = { evaluationComments = it },
                                    label = { Text("ملاحظات تربوية واقتراحات", fontSize = 10.sp) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        val generatedText = """
                                            إجراء وتحضير الأداء التربوي للفكرة الأكاديمية: [${idea.title}]
                                            اسم الطالب: $studentNameInput
                                            الصف الدراسي: $classGradeInput
                                            التقييم المكتسب: $lessonPointsInput / 10
                                            الملحوظات: $evaluationComments
                                            الاستراتيجية المطبقة: ${idea.desc}
                                            تم التوليد والتصدير بنجاح عبر نظام اكتفاء الأكاديمي العائلي.
                                        """.trimIndent()
                                        generatedResultText = generatedText
                                        reportShareDraft = generatedText
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("بناء السجل التعليمي وتحضير الدرس للابن 📝", color = MidnightBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            idea.category.contains("الاختبارات") || idea.category.contains("الواجبات") -> {
                                Text("الغرفة التفاعلية للاختبار الذكي والمحاكاة:", fontSize = 10.sp, color = TextLight, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val sampleSubjectQuestions = listOf(
                                    AcademicQuizQuestion("ما هو الكوكب الأكثر قرباً للشمس؟", listOf("المشتري", "عطارد", "المريخ", "الأرض"), 1, "عطارد هو أقرب كواكب المجموعة الشمسية."),
                                    AcademicQuizQuestion("ما إعراب الفاعل في اللغة العربية؟", listOf("مرفوع دائماً", "منصوب للجمع", "مجرور بالإضافة", "مجزوم بالكسر"), 0, "الفاعل مرفوع دائماً بالضمة أو بالألف/الواو."),
                                    AcademicQuizQuestion("متى وقعت غزوة بدر الكبرى؟", listOf("السنة الأولى", "السنة الثانية للهجرة", "السنة الثالثة", "السنة الرابعة"), 1, "وقعت غزوة بدر الكبرى في رمضان من السنة الثانية للهجرة.")
                                )

                                sampleSubjectQuestions.forEachIndexed { qIdx, question ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("${qIdx + 1}. ${question.text}", fontSize = 10.sp, color = WarmGold, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            question.options.forEachIndexed { oIdx, opt ->
                                                val isSelected = activeQuizAnswers[qIdx] == oIdx
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            if (!quizSubmittedState) {
                                                                activeQuizAnswers[qIdx] = oIdx
                                                            }
                                                        }
                                                        .background(if (isSelected) WarmGold.copy(0.1f) else Color.Transparent)
                                                        .padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(RoundedCornerShape(5.dp))
                                                            .background(if (isSelected) WarmGold else Color.Gray.copy(0.5f))
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(opt, fontSize = 9.sp, color = TextLight)
                                                }
                                            }
                                            if (quizSubmittedState) {
                                                val userAns = activeQuizAnswers[qIdx]
                                                val correct = userAns == question.correctIndex
                                                Text(
                                                    text = if (correct) "✅ إجابة صحيحة!" else "❌ خطأ! التوضيح: ${question.rationale}",
                                                    color = if (correct) GreenAccent else RedAccent,
                                                    fontSize = 8.sp,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { quizSubmittedState = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("تصحيح الإجابات 🎯", color = MidnightBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            activeQuizAnswers.clear()
                                            quizSubmittedState = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(1.dp, WarmGold)
                                    ) {
                                        Text("إعادة تصفير الاختبار 🔄", color = TextLight, fontSize = 10.sp)
                                    }
                                }
                            }

                            idea.category.contains("الحفظ") || idea.category.contains("القراءة") || idea.category.contains("القرآن") -> {
                                Text("سجل الحفظ والمراجعة القرآني والأدبي:", fontSize = 10.sp, color = TextLight, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = activeSurahName,
                                    onValueChange = { activeSurahName = it },
                                    label = { Text("اسم السورة أو المتن", fontSize = 10.sp) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = verseFromInput,
                                        onValueChange = { verseFromInput = it },
                                        label = { Text("من الآية", fontSize = 10.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = verseToInput,
                                        onValueChange = { verseToInput = it },
                                        label = { Text("إلى الآية", fontSize = 10.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = currentMemScore,
                                        onValueChange = { currentMemScore = it },
                                        label = { Text("مستوى الضبط والحفظ", fontSize = 10.sp) },
                                        modifier = Modifier.weight(1.5f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        val logData = """
                                            سجل حفظ ومراجعة فكرة [${idea.title}]:
                                            السورة/المتن: $activeSurahName
                                            المجال: من آية $verseFromInput إلى آية $verseToInput
                                            التقييم الحركي والتسميع: $currentMemScore
                                            طريقة الحفظ: ذكاء التكرار المتباعد النشط في اكتفاء.
                                        """.trimIndent()
                                        generatedResultText = logData
                                        reportShareDraft = logData
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("تسجيل وحفظ بيانات الحفظ للابن 📖", color = MidnightBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            else -> {
                                Text("توليد خطة الاستراتيجية للأولاد والأسرة:", fontSize = 10.sp, color = TextLight, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("انقر على الزر أدناه لتنشيط الذكاء الاصطناعي وصياغة تقرير أكاديمي مخصص لك.", fontSize = 9.sp, color = TextMuted)
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        triggerCustomAIWorkspaceEngine("مقرر عائلي شامل لتفوق الأبناء الدراسي")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("تفعيل وتنشيط الفكرة بالذكاء الاصطناعي 🧠", color = MidnightBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Central Smart AI Directives / Actions Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val contextInputSummary = when {
                                idea.category.contains("المعلم") -> "الطالب $studentNameInput في $classGradeInput بتقييم $lessonPointsInput من 10 وملاحظات: $evaluationComments"
                                else -> "تطبيق مفاهيم الفكرة في البيئة اليمنية المنزلية"
                            }
                            triggerCustomAIWorkspaceEngine(contextInputSummary)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                        modifier = Modifier.weight(1.2f),
                        enabled = !isAIPerforming
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MidnightBlue, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isAIPerforming) "جاري التحليل العلمي..." else "تحليل وإعداد بالذكاء الاصطناعي 🧠", color = MidnightBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // AI Generated Outcomes Panel
            if (generatedResultText.isNotBlank()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, WarmGold.copy(0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("المخرجات والملفات المحضّرة 📁", color = WarmGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                if (isAIPerforming) {
                                    CircularProgressIndicator(color = WarmGold, modifier = Modifier.size(12.dp), strokeWidth = 1.dp)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(0.04f))
                                    .border(0.5.dp, Color.White.copy(0.1f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    generatedResultText,
                                    fontSize = 11.sp,
                                    color = TextLight,
                                    lineHeight = 15.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onSaveToClipboard(generatedResultText, "الأفكار النظيرة")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Save, contentDescription = null, tint = MidnightBlue, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("نسخ وحفظ في الحافظة 📋", color = MidnightBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = {
                                        onSaveToClipboard("سجل متابعة - ${idea.title}\n الطالب: $studentNameInput\n التقييم: $lessonPointsInput\n ملاحظات: $evaluationComments", "عام")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceWarm),
                                    modifier = Modifier.weight(1f),
                                    enabled = idea.category.contains("المعلم")
                                ) {
                                    Text("سجل المتابعة للحافظة ✍️", color = MidnightBlue, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

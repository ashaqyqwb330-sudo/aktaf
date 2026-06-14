package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.Child
import com.example.database.LessonFeedback
import com.example.database.ClipItem
import com.example.ui.theme.MidnightBlue
import com.example.ui.theme.WarmGold
import com.example.ui.theme.GreenAccent
import com.example.ui.theme.RedAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildrenScreen(
    children: List<Child>,
    clipboardItems: List<ClipItem> = emptyList(),
    onBack: () -> Unit,
    onAddChild: (String, Int, String, String) -> Unit,
    onDeleteChild: (Int) -> Unit,
    getLessons: (Int) -> kotlinx.coroutines.flow.Flow<List<LessonFeedback>>,
    onAddFeedback: (Int, String, Int, String, String) -> Unit,
    onAddClipItem: (String, String) -> Unit = { _, _ -> },
    onDeleteClipItem: (Int) -> Unit = { _ -> }
) {
    var selectedChild by remember { mutableStateOf<Child?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Aggregate feedbacks for all children to display live alerts
    val childrenFeedbacks = children.associate { child ->
        child.id to remember(child.id) { getLessons(child.id) }.collectAsState(initial = emptyList())
    }

    val performanceAlertsByChild = remember(children, childrenFeedbacks) {
        children.mapNotNull { child ->
            val feedbacks = childrenFeedbacks[child.id]?.value ?: emptyList()
            if (feedbacks.isNotEmpty()) {
                val avg = feedbacks.map { it.score }.average()
                val latest = feedbacks.firstOrNull()
                if (latest != null && latest.score < avg) {
                    Triple(child, latest, avg)
                } else null
            } else null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text(if (selectedChild == null) "متابعة الأبناء الأكاديمية" else "صفحة الابن: ${selectedChild!!.name}", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = {
                    if (selectedChild != null) {
                        selectedChild = null
                    } else {
                        onBack()
                    }
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        if (selectedChild == null) {
            // Main list of children
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MidnightBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إضافة ابن/ابنة للمتابعة", color = MidnightBlue, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (children.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("يرجى إضافة الأبناء لمعاينة خطط ومناهج الدراسة والتقييم اليومي.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Alerts center at the top of the scrollable list
                        item {
                            if (performanceAlertsByChild.isNotEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = RedAccent.copy(alpha = 0.08f)),
                                    border = BorderStroke(1.2.dp, RedAccent.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = RedAccent)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "مركز إشعارات تراجع الأداء المدمج 🔔",
                                                fontWeight = FontWeight.Bold,
                                                color = RedAccent,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            "تم رصد تراجع مؤقت في أداء بعض الأبناء عن معدلهم الأكاديمي. انتبه لمكامن الصعوبات المسجلة وسارع باحتوائهم تربوياً:",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                            lineHeight = 16.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            performanceAlertsByChild.forEach { (child, latest, avg) ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                                        .border(0.5.dp, RedAccent.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                                        .padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Default.Warning,
                                                        contentDescription = null,
                                                        tint = RedAccent,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "${child.name}: تراجع في مادة (${latest.lessonName}) إلى ${latest.score}/10",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = "المعدل الأسبوعي: ${String.format("%.1f", avg)}/10 | التحدي: ${latest.difficulties.ifBlank { "لم يحدد" }}",
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = GreenAccent.copy(alpha = 0.08f)),
                                    border = BorderStroke(1.dp, GreenAccent.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenAccent, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "أداء الأبناء ممتاز وثابت ومبشر! لا توجد انخفاضات أكاديمية مرصودة حالياً 🌟",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = GreenAccent,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        items(children) { child ->
                            val feedbacks = childrenFeedbacks[child.id]?.value ?: emptyList()
                            val avgScore = if (feedbacks.isNotEmpty()) feedbacks.map { it.score }.average() else 0.0
                            val lessonsCount = feedbacks.size
                            val latestFeedback = feedbacks.firstOrNull()
                            val hasAlert = latestFeedback != null && latestFeedback.score < avgScore

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedChild = child },
                                border = BorderStroke(1.5.dp, WarmGold.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(child.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = WarmGold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("العمر: ${child.age} سنوات | الصف: ${child.grade}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                        }
                                        IconButton(onClick = { onDeleteChild(child.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Divider(color = WarmGold.copy(0.15f))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("معدل الاستيعاب الأكاديمي:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                                            Text(
                                                text = if (lessonsCount > 0) String.format("%.1f / 10", avgScore) else "لا يوجد تقييم حالياً",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (avgScore >= 8) GreenAccent else if (avgScore >= 5) WarmGold else RedAccent
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(WarmGold.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text("الدروس والواجبات: $lessonsCount", fontSize = 11.sp, color = WarmGold, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (lessonsCount > 0) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LinearProgressIndicator(
                                            progress = (avgScore / 10.0).toFloat(),
                                            color = if (avgScore >= 8) GreenAccent else if (avgScore >= 5) WarmGold else RedAccent,
                                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                        )
                                    }

                                    if (hasAlert && latestFeedback != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(RedAccent.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                                .border(0.5.dp, RedAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.TrendingDown, contentDescription = null, tint = RedAccent, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "تراجع في درس (${latestFeedback.lessonName}): حصل على ${latestFeedback.score}/10 وهو أقل من معدله العام (${String.format("%.1f", avgScore)})",
                                                    fontSize = 10.sp,
                                                    color = RedAccent,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    if (child.notes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("أهداف التربية: ${child.notes}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Child detail subview
            ChildDetailsSubScreen(
                child = selectedChild!!,
                getLessons = getLessons,
                onAddFeedback = onAddFeedback,
                clipboardItems = clipboardItems,
                onAddClipItem = onAddClipItem,
                onDeleteClipItem = onDeleteClipItem
            )
        }

        // Add Child Dialog
        if (showAddDialog) {
            var name by remember { mutableStateOf("") }
            var ageStr by remember { mutableStateOf("") }
            var grade by remember { mutableStateOf("") }
            var note by remember { mutableStateOf("") }

            val tfColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedLabelColor = WarmGold,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                focusedBorderColor = WarmGold,
                unfocusedBorderColor = WarmGold.copy(alpha = 0.5f),
                cursorColor = WarmGold
            )

            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text("إضافة ابن جديد", color = WarmGold, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("الاسم") },
                            colors = tfColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = ageStr,
                            onValueChange = { ageStr = it },
                            label = { Text("العمر") },
                            colors = tfColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = grade,
                            onValueChange = { grade = it },
                            label = { Text("الصف الدراسي (مثل الأول الثانوي، الخامس)") },
                            colors = tfColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("ملاحظات البداية") },
                            colors = tfColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                        onClick = {
                            val age = ageStr.toIntOrNull() ?: 10
                            if (name.isNotBlank() && grade.isNotBlank()) {
                                onAddChild(name, age, grade, note)
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text("حفظ", color = MidnightBlue, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("إلغاء", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            )
        }
    }
}

@Composable
fun ChildDetailsSubScreen(
    child: Child,
    getLessons: (Int) -> kotlinx.coroutines.flow.Flow<List<LessonFeedback>>,
    onAddFeedback: (Int, String, Int, String, String) -> Unit,
    clipboardItems: List<ClipItem> = emptyList(),
    onAddClipItem: (String, String) -> Unit = { _, _ -> },
    onDeleteClipItem: (Int) -> Unit = { _ -> }
) {
    val feedbacks by getLessons(child.id).collectAsState(initial = emptyList())
    var currentTab by remember { mutableStateOf(0) } // 0 = الحصص, 1 = تقييم جديد, 2 = التقارير اليومية, 3 = الرسوم البيانية الأسبوعية, 4 = الحافظة الذكية

    val motivationPhrase = when {
        feedbacks.isEmpty() -> "ابدأ بإضافة تقييم دروس اليوم لإنتاج عبارات التشجيع!"
        feedbacks.any { it.score >= 9 } -> "يا بطل! واصل هذا التميز فالعلم يبني المستقبل ويصنع المعجزات."
        feedbacks.any { it.score <= 5 } -> "لا بأس يا ذكي، المحاولة الصادقة تصنع المتميزين. دعنا نعمل معاً غداً لنتجاوز التحدي!"
        else -> "رائع! استمر في المثابرة، خطوات صغيرة ثابتة تقودك إلى قمة النجاح!"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Motivation banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = WarmGold.copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, WarmGold),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("عبارة تشجيعية للابن: ${child.name}", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("\"$motivationPhrase\"", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation Tabs for Child Subview (using ScrollableTabRow for adaptive 5 tabs)
        ScrollableTabRow(
            selectedTabIndex = currentTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = WarmGold,
            edgePadding = 0.dp
        ) {
            Tab(selected = currentTab == 0, onClick = { currentTab = 0 }) {
                Text("جدول الحصص", modifier = Modifier.padding(8.dp), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            }
            Tab(selected = currentTab == 1, onClick = { currentTab = 1 }) {
                Text("تقييم جديد", modifier = Modifier.padding(8.dp), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            }
            Tab(selected = currentTab == 2, onClick = { currentTab = 2 }) {
                Text("تقارير الأداء والأخطاء", modifier = Modifier.padding(8.dp), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            }
            Tab(selected = currentTab == 3, onClick = { currentTab = 3 }) {
                Text("الرسوم الأسبوعية", modifier = Modifier.padding(8.dp), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            }
            Tab(selected = currentTab == 4, onClick = { currentTab = 4 }) {
                Text("الحافظة الذكية 📝", modifier = Modifier.padding(8.dp), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (currentTab) {
            0 -> {
                // Schedule tab
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val schoolDays = listOf(
                        "السبت" to listOf("قرآن كريم", "علوم عامة", "رياضيات حصة 1", "لغة عربية"),
                        "الأحد" to listOf("تربية إسلامية", "لغة إنجليزية", "حساب هندسي", "تاريخ وجغرافيا"),
                        "الإثنين" to listOf("قرآن مراجعة", "علوم فيزيائية", "رياضيات حصة 2", "قرآن تفسير"),
                        "الثلاثاء" to listOf("حاسوب وصيانة", "تربية وطنية", "أدب ونحو وبلاغة", "ألعاب وصحة"),
                        "الأربعاء" to listOf("مراجعة امتحانات عامة", "حصص تقوية فردية", "مذاكرة جماعية", "أذكار ومسابقات")
                    )

                    schoolDays.forEach { (day, subjects) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.3f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(day, fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    subjects.forEach { subject ->
                                        Box(
                                            modifier = Modifier
                                                .background(WarmGold.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Text(subject, fontSize = 10.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // Log lesson progress
                var subjectInput by remember { mutableStateOf("") }
                var ratingInput by remember { mutableStateOf(8f) }
                var difficultsInput by remember { mutableStateOf("") }
                var positivesInput by remember { mutableStateOf("") }

                val tfColors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedLabelColor = WarmGold,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    focusedBorderColor = WarmGold,
                    unfocusedBorderColor = WarmGold.copy(alpha = 0.5f),
                    cursorColor = WarmGold
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تسجيل تحصيل درس جديد ومراجعته", fontWeight = FontWeight.Bold, color = WarmGold)
                    OutlinedTextField(
                        value = subjectInput,
                        onValueChange = { subjectInput = it },
                        label = { Text("المادة والدرس الدراسي") },
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("تقييم الاستيعاب والحفظ: ${ratingInput.toInt()} / 10", fontSize = 12.sp)
                    Slider(
                        value = ratingInput,
                        onValueChange = { ratingInput = it },
                        valueRange = 1f..10f,
                        colors = SliderDefaults.colors(
                            thumbColor = WarmGold,
                            activeTrackColor = WarmGold
                        )
                    )

                    OutlinedTextField(
                        value = difficultsInput,
                        onValueChange = { difficultsInput = it },
                        label = { Text("الصعوبات ومكامن الضعف") },
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = positivesInput,
                        onValueChange = { positivesInput = it },
                        label = { Text("الإيجابيات ومواطن القوة والواجبات المنزلية") },
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (subjectInput.isNotBlank()) {
                                onAddFeedback(child.id, subjectInput, ratingInput.toInt(), difficultsInput, positivesInput)
                                subjectInput = ""
                                difficultsInput = ""
                                positivesInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تسجيل التقرير اليومي", color = MidnightBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
            2 -> {
                // Performance reports list
                if (feedbacks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        Text("لا يوجد تقارير مسجلة لهذا الابن بعد. أدخل التقييمات أولاً.")
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            InteractiveProgressChart(feedbacks = feedbacks)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        items(feedbacks) { feedback ->
                            val color = if (feedback.score >= 8) GreenAccent else if (feedback.score >= 5) WarmGold else RedAccent
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(feedback.lessonName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("التقييم: ${feedback.score}/10", color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("التاريخ: ${feedback.dateStr}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    if (feedback.difficulties.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("التحديات: ${feedback.difficulties}", fontSize = 12.sp, color = RedAccent)
                                    }
                                    if (feedback.positiveNotes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("ملاحظات الأستاذ والواجبات: ${feedback.positiveNotes}", fontSize = 12.sp, color = GreenAccent)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            3 -> {
                // Weekly Graphical Performance Dashboard
                WeeklyPerformanceDashboard(feedbacks = feedbacks)
            }
            4 -> {
                // Smart Notebook / Clipboard for daily notes and educational tips
                var parentNoteText by remember { mutableStateOf("") }

                val eduTips = listOf(
                    "تحسين خط اليد: التدرب اليومي لمدة 10 دقائق على سطر بخط رقعة ومنظم.",
                    "المكافأة المعنوية: الثناء على جهد الابن أمام العائلة لتعزيز ثقته وجدارته.",
                    "تنسيق الوقت: تخصيص ساعة للمذاكرة وساعة كاملة للعب بحرية تامة دون قيود.",
                    "القراءة اليومية: حث الابن على قراءة صفحتين من قصة مشوقة ومفيدة قبل النوم.",
                    "تثبيت القرآن: سماع معاني الكلمات الصعبة وتلقين السورة بصوت ممتع لتثبيتها."
                )

                val filteredNotes = remember(clipboardItems, child.id) {
                    clipboardItems.filter { it.category == "child_notes_${child.id}" }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Input Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.4f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "مفكرة المذاكرة وحافظة الأبناء الذكية 📝",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = WarmGold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "دَوّن التوجيهات، الملاحظات اليومية، أو التوصيات المنهجية التي تود التركيز عليها مع ${child.name}:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = parentNoteText,
                                    onValueChange = { parentNoteText = it },
                                    label = { Text("اكتب ملاحظتك اليومية أو نصيحتك هنا...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedBorderColor = WarmGold,
                                        unfocusedBorderColor = WarmGold.copy(alpha = 0.4f)
                                    ),
                                    minLines = 3,
                                    maxLines = 5
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        if (parentNoteText.isNotBlank()) {
                                            onAddClipItem(parentNoteText, "child_notes_${child.id}")
                                            parentNoteText = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, tint = MidnightBlue)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("حفظ في مفكرة الابن", color = MidnightBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Educational Tips Library (Instant Copy/Insert)
                    item {
                        Column {
                            Text(
                                "💡 بنك النصائح التربوية السريعة (اضغط للإدراج الفوري):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = WarmGold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(eduTips) { tip ->
                                    Card(
                                        modifier = Modifier
                                            .width(220.dp)
                                            .heightIn(min = 90.dp)
                                            .clickable {
                                                parentNoteText = tip
                                            },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.2.dp, WarmGold.copy(0.2f)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(10.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Text(
                                                text = tip,
                                                fontSize = 11.sp,
                                                lineHeight = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Saved Notes Header
                    item {
                        Divider(color = WarmGold.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "مفكرة ${child.name} السابقة وملاحظات الأداء التربوي:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = WarmGold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    if (filteredNotes.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(0.5f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    "لا توجد ملاحظات تربوية مرصودة حالياً لـ ${child.name}. استخدم صندوق النصائح التربوية أعلاه أو اكتب ملاحظتك المخصصة لتراها مخزنة كحافظة ذكية مستدامة.",
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    } else {
                        items(filteredNotes) { clipItem ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(0.6.dp, WarmGold.copy(0.25f)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = clipItem.content,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 18.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))

                                        val dateFormatted = try {
                                            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                                                .format(java.util.Date(clipItem.timestamp))
                                        } catch (e: Exception) {
                                            "ملاحظة مسجلة"
                                        }

                                        Text(
                                            text = "⏱️ $dateFormatted",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(onClick = { onDeleteClipItem(clipItem.id) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "حذف الملاحظة",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
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

@Composable
fun WeeklyPerformanceDashboard(feedbacks: List<LessonFeedback>) {
    var isSimulated by remember { mutableStateOf(false) }

    // Choose data to display (real or simulated)
    val displayFeedbacks = remember(feedbacks) {
        if (feedbacks.isEmpty()) {
            isSimulated = true
            listOf(
                LessonFeedback(1, 1, "القرآن الكريم", 9, "تجويد سورة النبأ", "حفظ متين ومخارج حروف ممتازة", "السبت"),
                LessonFeedback(2, 1, "الرياضيات", 7, "المجموعات الحسابية", "يحتاج لمراجعة الرموز والأقواس", "الأحد"),
                LessonFeedback(3, 1, "العلوم العامة", 8, "الخواص الفيزيائية للماء", "تمت تلبية الواجب كاملاً", "الإثنين"),
                LessonFeedback(4, 1, "اللغة العربية", 10, "لا يوجد", "أداء بليغ وتلاوة شعرية معبرة", "الثلاثاء"),
                LessonFeedback(5, 1, "اللغة الإنجليزية", 6, "زمن المضارع البسيط", "تحديات في الأفعال الشاذة", "الأربعاء")
            )
        } else {
            isSimulated = false
            feedbacks
        }
    }

    // Calculations
    val totalScore = displayFeedbacks.map { it.score }.sum()
    val avgOverall = if (displayFeedbacks.isNotEmpty()) totalScore.toFloat() / displayFeedbacks.size else 0f

    // Group by Subject
    val subjectsMap = remember(displayFeedbacks) {
        displayFeedbacks.groupBy { it.lessonName }
            .mapValues { entry -> entry.value.map { it.score }.average() }
    }

    // Find highest and lowest
    val highestSubject = remember(subjectsMap) {
        subjectsMap.maxByOrNull { it.value }
    }
    val lowestSubject = remember(subjectsMap) {
        subjectsMap.minByOrNull { it.value }
    }

    // Map to study days
    val weekdays = listOf("السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء")
    val dailyScores = remember(displayFeedbacks) {
        val scores = mutableMapOf<String, MutableList<Int>>()
        weekdays.forEach { scores[it] = mutableListOf() }

        displayFeedbacks.forEachIndexed { idx, fb ->
            var matched = false
            for (day in weekdays) {
                if (fb.dateStr.contains(day) || fb.lessonName.contains(day) || fb.positiveNotes.contains(day)) {
                    scores[day]?.add(fb.score)
                    matched = true
                    break
                }
            }
            if (!matched) {
                val cyclicDay = weekdays[idx % 5]
                scores[cyclicDay]?.add(fb.score)
            }
        }

        weekdays.map { day ->
            val list = scores[day] ?: emptyList()
            val avg = if (list.isNotEmpty()) list.average() else 0.0
            day to avg
        }
    }

    var activeChartTab by remember { mutableStateOf(0) } // 0 = الأعمدة الأسبوعية, 1 = أداء المواد, 2 = مؤشر الجهد
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isSimulated) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = WarmGold.copy(0.12f)),
                    border = BorderStroke(1.dp, WarmGold.copy(0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = WarmGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "لوحة الرسوم مفعّلة بنموذج محاكاة",
                                fontWeight = FontWeight.Bold,
                                color = WarmGold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "الابن ليس لديه تقارير كافية مسجلة بعد. نعرض الآن بيانات محاكاة تفاعلية لتوضيح كفاءة الرسوم البيانية. ابدأ بتسجيل تقدّم مادة لتظهر هنا مباشرةً!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Overview KPI Cards
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f)),
                border = BorderStroke(1.dp, WarmGold.copy(0.2f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "🔍 كفاءة الأداء والتحصيل الأسبوعي",
                        fontWeight = FontWeight.Bold,
                        color = WarmGold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, WarmGold.copy(0.15f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("المعدل العام", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                                Text(
                                    String.format("%.1f / 10", avgOverall),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = if (avgOverall >= 8) GreenAccent else if (avgOverall >= 5) WarmGold else RedAccent
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (avgOverall >= 8) "ممتاز جداً 🌟" else if (avgOverall >= 5) "جيد ومثابر 📈" else "بحاجة لمتابعة ⚠️",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (avgOverall >= 8) GreenAccent else if (avgOverall >= 5) WarmGold else RedAccent
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, WarmGold.copy(0.15f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("الدروس والتقييمات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                                Text(
                                    "${displayFeedbacks.size} حصص",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = WarmGold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("مسجلة هذا الأسبوع", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }

                    if (highestSubject != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = WarmGold.copy(0.1f))
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🌟 المادة الأكثر تفوقاً:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                            Text("${highestSubject.key} (${String.format("%.1f", highestSubject.value)})", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = GreenAccent)
                        }
                    }

                    if (lowestSubject != null && lowestSubject.value < 8.0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("⚠️ التحدي الدراسي الأبرز:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                            Text("${lowestSubject.key} (${String.format("%.1f", lowestSubject.value)})", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = RedAccent)
                        }
                    }
                }
            }
        }

        // Subtab Navigation
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("مُنحنى الأيام 📊", "بيان المواد 📚", "مؤشر الاجتهاد 🎯").forEachIndexed { index, title ->
                    val isSelected = activeChartTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) WarmGold else WarmGold.copy(0.06f),
                                RoundedCornerShape(10.dp)
                            )
                            .border(1.dp, WarmGold.copy(if (isSelected) 1f else 0.2f), RoundedCornerShape(10.dp))
                            .clickable { activeChartTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isSelected) MidnightBlue else WarmGold
                        )
                    }
                }
            }
        }

        // Chart Content
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, WarmGold.copy(0.3f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (activeChartTab) {
                        0 -> {
                            Text(
                                "📅 مخطط الأعمدة الأسبوعي لتوزيع درجات التحصيل",
                                fontWeight = FontWeight.Bold,
                                color = WarmGold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            WeeklyBarChart(
                                daysData = dailyScores,
                                selectedIndex = selectedDayIndex,
                                onSelectIndex = { selectedDayIndex = it }
                            )

                            // Tapped bar feedback details
                            Spacer(modifier = Modifier.height(12.dp))
                            selectedDayIndex?.let { idx ->
                                if (idx in dailyScores.indices) {
                                    val (dayName, score) = dailyScores[idx]
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = WarmGold.copy(0.08f)),
                                        border = BorderStroke(1.dp, WarmGold.copy(0.2f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("يوم الحصص: $dayName", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                Text(
                                                    "المعدل للمواد: ${if (score > 0) String.format("%.1f", score) else "0.0"}/10",
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (score >= 8) GreenAccent else if (score >= 5) WarmGold else RedAccent,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = when {
                                                    score >= 8.5 -> "مستوى استيعاب متميز جداً. جهز مكافأة بسيطة اليوم تشجيعاً للمواصلة والاستمرار 🌟"
                                                    score >= 6.5 -> "تقدم جيد ومراجعة متزنة، ينصح بمناقشة الدرس الشفهي مع الابن اليوم."
                                                    score > 0 -> "هناك صعوبات طفيفة في مواد اليوم، يرجى مراجعة التحديات المسجلة بمفردة الأداء والتركيز على حل الواجبات."
                                                    else -> "لا توجد حصص مسجلة أو مقصودة في هذا اليوم الأكاديمي."
                                                },
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }
                            } ?: run {
                                Text(
                                    "💡 تلميح: اضغط على أي عمود في المخطط لقراءة التقرير التعليمي لليوم والتوجيهات المناسبة.",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        1 -> {
                            Text(
                                "📚 تقييم استيعاب المواد الأساسية للأبناء",
                                fontWeight = FontWeight.Bold,
                                color = WarmGold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            SubjectHorizontalBarChart(subjectScores = subjectsMap)
                        }
                        2 -> {
                            Text(
                                "🎯 قياس مؤشر السلوك والهمة الدراسية للابن",
                                fontWeight = FontWeight.Bold,
                                color = WarmGold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            EffortGaugeChart(averageScore = avgOverall)

                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)),
                                border = BorderStroke(1.dp, WarmGold.copy(0.1f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "التقييم الذكي للهمة والمذاكرة:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = WarmGold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (avgOverall >= 8) {
                                            "الابن يظهر التزاماً استثنائياً ونشاطاً ممتازاً في الاستفسار وحل الواجبات. استغل هذه الهمة العالية للاندماج في المسابقات الثقافية أو تحفيظ القرآن الكريم بمستويات متقدمة."
                                        } else if (avgOverall >= 5) {
                                            "أداء الابن متوازّن ومقنع. هناك رغبة جيدة للتعلم ولكن قد يواجه بعض الملل أو التشتت في فترات الصعوبات. ينصح بتقليص وقت الأجهزة الذكية وتنظيم أوقات النوم."
                                        } else {
                                            "أداء منخفض يحتاج إلى تدخلك الحنون والتفتيش عن تحديات نفسية أو أكاديمية قد يمر بها. لا تتردد في تشجيعه ومنحه فترات راحة منتظمة وتقليل الأعباء المفاجئة."
                                        },
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                                        lineHeight = 16.sp
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
fun WeeklyBarChart(
    daysData: List<Pair<String, Double>>,
    selectedIndex: Int?,
    onSelectIndex: (Int) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val padding = 35f

        val chartWidth = width - padding * 2
        val chartHeight = height - padding * 2

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(daysData) {
                    detectTapGestures { offset ->
                        if (daysData.size > 1) {
                            val stepX = chartWidth / (daysData.size - 1)
                            var closestIdx = -1
                            var minDiff = Float.MAX_VALUE
                            for (i in daysData.indices) {
                                val pointX = padding + i * stepX
                                val diff = kotlin.math.abs(offset.x - pointX)
                                if (diff < minDiff && diff < stepX / 2f) {
                                    minDiff = diff
                                    closestIdx = i
                                }
                            }
                            if (closestIdx != -1) {
                                onSelectIndex(closestIdx)
                            }
                        }
                    }
                }
        ) {
            // Draw grid lines
            val gridLines = listOf(10f, 8f, 6f, 4f, 2f, 0f)
            gridLines.forEach { rating ->
                val y = padding + chartHeight * (1f - rating / 10f)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.12f),
                    start = Offset(padding, y),
                    end = Offset(width - padding, y),
                    strokeWidth = 2f
                )
                // draw marks text
                drawContext.canvas.nativeCanvas.drawText(
                    rating.toInt().toString(),
                    padding - 12f,
                    y + 8f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 22f
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }
                )
            }

            // Draw bars
            val stepX = if (daysData.size > 1) chartWidth / (daysData.size - 1) else chartWidth
            val barWidth = 42f

            daysData.forEachIndexed { i, (dayLabel, score) ->
                val x = padding + i * stepX
                val y = padding + chartHeight * (1f - score.toFloat() / 10f)
                val isSelected = selectedIndex == i

                val rectLeft = x - barWidth / 2f
                val rectTop = y
                val rectRight = x + barWidth / 2f
                val rectBottom = padding + chartHeight

                // Draw background empty rail
                drawRoundRect(
                    color = Color.Gray.copy(alpha = 0.05f),
                    topLeft = Offset(rectLeft, padding),
                    size = Size(barWidth, chartHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                if (score > 0) {
                    // Draw fill bar
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                if (isSelected) GreenAccent else WarmGold,
                                if (isSelected) GreenAccent.copy(0.4f) else WarmGold.copy(0.3f)
                            )
                        ),
                        topLeft = Offset(rectLeft, rectTop),
                        size = Size(barWidth, rectBottom - rectTop),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Draw outline for selected day
                    if (isSelected) {
                        drawRoundRect(
                            color = GreenAccent,
                            topLeft = Offset(rectLeft - 2f, rectTop - 2f),
                            size = Size(barWidth + 4f, rectBottom - rectTop + 4f),
                            style = Stroke(width = 3f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }

                    // Score label on top of bar
                    drawContext.canvas.nativeCanvas.drawText(
                        String.format("%.1f", score),
                        x,
                        y - 12f,
                        android.graphics.Paint().apply {
                            color = if (isSelected) android.graphics.Color.GREEN else android.graphics.Color.WHITE
                            textSize = 20f
                            textAlign = android.graphics.Paint.Align.CENTER
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                    )
                }

                // Day text label at the bottom
                drawContext.canvas.nativeCanvas.drawText(
                    dayLabel,
                    x,
                    padding + chartHeight + 25f,
                    android.graphics.Paint().apply {
                        color = if (isSelected) android.graphics.Color.parseColor("#E6C681") else android.graphics.Color.LTGRAY
                        textSize = 22f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }
    }
}

@Composable
fun SubjectHorizontalBarChart(subjectScores: Map<String, Double>) {
    if (subjectScores.isEmpty()) return

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        subjectScores.forEach { (subject, score) ->
            val color = if (score >= 8.0) GreenAccent else if (score >= 5.0) WarmGold else RedAccent
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.15f)),
                border = BorderStroke(0.5.dp, WarmGold.copy(0.15f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = subject,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = WarmGold
                        )
                        Text(
                            text = String.format("%.1f / 10", score),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = color
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(5.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((score / 10.0).toFloat())
                                .fillMaxHeight()
                                .background(color, RoundedCornerShape(5.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EffortGaugeChart(averageScore: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2f, height - 30f)
            val radius = size.minDimension / 1.7f

            // Draw background gray arc (semi-circle)
            drawArc(
                color = Color.Gray.copy(alpha = 0.15f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 32f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )

            // Draw colorful indicator arc
            val sweep = (averageScore / 10f) * 180f
            val brush = Brush.sweepGradient(
                colors = listOf(RedAccent, WarmGold, GreenAccent, GreenAccent),
                center = center
            )

            drawArc(
                brush = brush,
                startAngle = 180f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 32f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )

            // Draw major gauge markers (0, 5, 10)
            val markerAngles = listOf(180f, 225f, 270f, 315f, 360f)
            val markersText = listOf("0", "2.5", "5", "7.5", "10")

            markerAngles.forEachIndexed { index, angleDeg ->
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val innerRadius = radius - 45f
                val outerRadius = radius - 15f
                val start = Offset(
                    center.x + (innerRadius * Math.cos(angleRad)).toFloat(),
                    center.y + (innerRadius * Math.sin(angleRad)).toFloat()
                )
                val end = Offset(
                    center.x + (outerRadius * Math.cos(angleRad)).toFloat(),
                    center.y + (outerRadius * Math.sin(angleRad)).toFloat()
                )
                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = start,
                    end = end,
                    strokeWidth = 3f
                )

                // Draw text labels for gauge division numbers
                val textRadius = radius - 65f
                val textX = center.x + (textRadius * Math.cos(angleRad)).toFloat()
                val textY = center.y + (textRadius * Math.sin(angleRad)).toFloat()

                drawContext.canvas.nativeCanvas.drawText(
                    markersText[index],
                    textX,
                    textY + 10f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 22f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }

            // Draw custom modern pointer needle pointing to our sweep
            val targetAngleRad = Math.toRadians((180f + sweep).toDouble())
            val needleLen = radius - 30f
            val needleTip = Offset(
                center.x + (needleLen * Math.cos(targetAngleRad)).toFloat(),
                center.y + (needleLen * Math.sin(targetAngleRad)).toFloat()
            )

            drawLine(
                color = MidnightBlue,
                start = center,
                end = needleTip,
                strokeWidth = 8f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawLine(
                color = WarmGold,
                start = center,
                end = needleTip,
                strokeWidth = 4f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            // Draw center cap pin
            drawCircle(color = WarmGold, radius = 16f, center = center)
            drawCircle(color = MidnightBlue, radius = 8f, center = center)
        }
    }
}

@Composable
fun InteractiveProgressChart(feedbacks: List<LessonFeedback>) {
    if (feedbacks.isEmpty()) return

    // Reverse if needed to ensure chronological order (left to right)
    val sortedFeedbacks = remember(feedbacks) {
        feedbacks.sortedBy { it.id }
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MidnightBlue.copy(0.04f), RoundedCornerShape(16.dp))
            .border(1.dp, WarmGold.copy(0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            "📈 المنحنى البياني لمعدل الفهم والتحصيل العلمي",
            color = WarmGold,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()
            val padding = 40f

            val chartWidth = width - padding * 2
            val chartHeight = height - padding * 2

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(sortedFeedbacks) {
                        detectTapGestures { offset ->
                            if (sortedFeedbacks.size > 1) {
                                val stepX = chartWidth / (sortedFeedbacks.size - 1)
                                var closestIdx = -1
                                var minDiff = Float.MAX_VALUE
                                for (i in sortedFeedbacks.indices) {
                                    val pointX = padding + i * stepX
                                    val diff = kotlin.math.abs(offset.x - pointX)
                                    if (diff < minDiff && diff < stepX / 2) {
                                        minDiff = diff
                                        closestIdx = i
                                    }
                                }
                                if (closestIdx != -1) {
                                    selectedIndex = closestIdx
                                }
                            } else if (sortedFeedbacks.size == 1) {
                                selectedIndex = 0
                            }
                        }
                    }
            ) {
                // 1. Draw rating levels (grid lines: 10, 8, 6, 4, 2)
                val gridLines = listOf(10f, 8f, 6f, 4f, 2f, 0f)
                gridLines.forEach { rating ->
                    val y = padding + chartHeight * (1f - rating / 10f)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.2f),
                        start = Offset(padding, y),
                        end = Offset(width - padding, y),
                        strokeWidth = 2f
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        rating.toInt().toString(),
                        padding - 15f,
                        y + 10f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 24f
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }
                    )
                }

                // 2. Plot data points
                if (sortedFeedbacks.isNotEmpty()) {
                    val points = mutableListOf<Offset>()
                    val stepX = if (sortedFeedbacks.size > 1) chartWidth / (sortedFeedbacks.size - 1) else chartWidth

                    sortedFeedbacks.forEachIndexed { index, fb ->
                        val x = padding + index * stepX
                        val y = padding + chartHeight * (1f - fb.score.toFloat() / 10f)
                        points.add(Offset(x, y))
                    }

                    // Draw gradient fill under the line
                    if (points.size > 1) {
                        val fillPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(points.first().x, padding + chartHeight)
                            points.forEach { point ->
                                lineTo(point.x, point.y)
                            }
                            lineTo(points.last().x, padding + chartHeight)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(WarmGold.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )
                    }

                    // Draw connection path
                    if (points.size > 1) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = WarmGold,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 4f,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                    }

                    // Draw circles
                    points.forEachIndexed { index, point ->
                        val isSelected = selectedIndex == index
                        drawCircle(
                            color = if (isSelected) GreenAccent else WarmGold,
                            radius = if (isSelected) 10f else 6f,
                            center = point
                        )
                        drawCircle(
                            color = MidnightBlue,
                            radius = if (isSelected) 5f else 3f,
                            center = point
                        )
                    }
                }
            }
        }

        // Selected interactive point info
        selectedIndex?.let { idx ->
            if (idx in sortedFeedbacks.indices) {
                val selectedFeedback = sortedFeedbacks[idx]
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = WarmGold.copy(0.1f)),
                    border = BorderStroke(1.dp, WarmGold.copy(0.3f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "المادة والدرس: ${selectedFeedback.lessonName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = WarmGold
                            )
                            Text(
                                "الدرجة: ${selectedFeedback.score}/10",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = if (selectedFeedback.score >= 8) GreenAccent else WarmGold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "التاريخ: ${selectedFeedback.dateStr}",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        if (selectedFeedback.difficulties.isNotBlank()) {
                            Text(
                                "التحديات: ${selectedFeedback.difficulties}",
                                fontSize = 10.sp,
                                color = RedAccent
                            )
                        }
                    }
                }
            }
        } ?: run {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "💡 تلميح: انقر على أي نقطة في المنحنى البياني لعرض تفاصيل التحصيل وتوثيق التقدم.",
                fontSize = 10.sp,
                color = Color.Gray,
                style = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

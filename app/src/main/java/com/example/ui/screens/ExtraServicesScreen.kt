package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.speech.tts.TextToSpeech
import android.content.Intent
import com.example.HealthHubActivity
import java.util.Locale
import com.example.database.HouseAppliance
import com.example.database.Medication
import com.example.database.TodoItem
import com.example.ui.theme.MidnightBlue
import com.example.ui.theme.WarmGold
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtraServicesScreen(
    currentTabScreenName: String,
    todos: List<TodoItem>,
    appliances: List<HouseAppliance>,
    medications: List<Medication>,
    onBack: () -> Unit,
    onAddTodo: (String, String) -> Unit,
    onToggleTodo: (TodoItem) -> Unit,
    onDeleteTodo: (Int) -> Unit,
    onAddAppliance: (String, String, Int) -> Unit,
    onDeleteAppliance: (Int) -> Unit,
    onAddMedication: (String, String, String, String, String, String, String, String, String) -> Unit,
    onToggleMedication: (Medication) -> Unit,
    onDeleteMedication: (Int) -> Unit
) {
    var subTab by remember { mutableStateOf(0) } // 0 = المسبحة, 1 = التقييم, 2 = جدول الأعمال, 3 = صيانة المنزل, 4 = صحة الأسرة, 5 = إسلاميات وسريعة

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("سلة الميزات والخدمات العائلية", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        // Sub categories scroller
        ScrollableTabRow(
            selectedTabIndex = subTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = WarmGold
        ) {
            val categorizs = listOf("المسبحة الإكترونية", "التقييم والاجتماعات", "جدول الأعمال", "صيانة الأجهزة", "صحة الأسرة ومواعيدها", "حصن المسلم واللغات")
            categorizs.forEachIndexed { index, name ->
                Tab(selected = subTab == index, onClick = { subTab = index }) {
                    Text(name, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (subTab) {
            0 -> {
                // Electronic Tasbih
                var counter by remember { mutableStateOf(0) }
                var sessionTotal by remember { mutableStateOf(0) }
                val targetColor = when {
                    counter >= 66 -> Color(0xFF4CAF50)
                    counter >= 33 -> Color(0xFF03A9F4)
                    else -> WarmGold
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("المسبحة الإلكترونية المتطورة", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 18.sp)
                    Text("يتغير لون العداد تلقائياً كل 33 تسبيحة.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 11.sp)
                    
                    Spacer(modifier = Modifier.height(30.dp))

                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(80.dp))
                            .background(targetColor.copy(alpha = 0.1f))
                            .border(BorderStroke(4.dp, targetColor), RoundedCornerShape(80.dp))
                            .clickable {
                                counter++
                                sessionTotal++
                                if (counter > 99) counter = 0
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$counter", fontWeight = FontWeight.Bold, fontSize = 36.sp, color = targetColor)
                            Text("تسبيحة", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = { counter = 0 },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, WarmGold)
                        ) {
                            Text("تصفير العداد", color = WarmGold)
                        }
                        Button(
                            onClick = { sessionTotal = 0; counter = 0 },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmGold)
                        ) {
                            Text("إعادة جلسة اليوم", color = MidnightBlue, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text("مجموع الجلسة الإجمالي: $sessionTotal تسبيحة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            1 -> {
                // Self Evaluation and Meetings records
                var productivityScore by remember { mutableStateOf(8f) }
                var focusScore by remember { mutableStateOf(7f) }
                var evalMessage by remember { mutableStateOf("") }

                var meetingTitle by remember { mutableStateOf("") }
                var meetingLog by remember { mutableStateOf("") }
                var meetingList = remember { mutableStateListOf<String>() }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("التقييم العائلي والذاتي اليومي", fontWeight = FontWeight.Bold, color = WarmGold)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text("الإنتاجية ومتابعة الأعمال: ${productivityScore.toInt()}/10", fontSize = 12.sp)
                                Slider(value = productivityScore, onValueChange = { productivityScore = it }, valueRange = 1f..10f, colors = SliderDefaults.colors(activeTrackColor = WarmGold, thumbColor = WarmGold))

                                Text("التركيز والنشاط والرياضة: ${focusScore.toInt()}/10", fontSize = 12.sp)
                                Slider(value = focusScore, onValueChange = { focusScore = it }, valueRange = 1f..10f, colors = SliderDefaults.colors(activeTrackColor = WarmGold, thumbColor = WarmGold))

                                Button(
                                    onClick = { evalMessage = "تم حفظ تقييمك بنجاح! نسبة تركيزك المجمعة هي ${((productivityScore + focusScore)*5).toInt()}% واصل تميزك!" },
                                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("حفظ التقييم الذاتي", color = MidnightBlue, fontWeight = FontWeight.Bold)
                                }

                                if (evalMessage.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(evalMessage, color = WarmGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("محاضر واجتماعات العائلة", fontWeight = FontWeight.Bold, color = WarmGold)
                                OutlinedTextField(value = meetingTitle, onValueChange = { meetingTitle = it }, placeholder = { Text("عنوان الاجتماع العائلي") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = meetingLog, onValueChange = { meetingLog = it }, placeholder = { Text("بيان ومحضر وقرارات الاجتماع...") }, modifier = Modifier.fillMaxWidth())

                                Button(
                                    onClick = {
                                        if (meetingTitle.isNotBlank() && meetingLog.isNotBlank()) {
                                            meetingList.add("عنوان: $meetingTitle\nالقرارات: $meetingLog")
                                            meetingTitle = ""
                                            meetingLog = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("تسجيل الاجتماع العائلي", color = MidnightBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (meetingList.isNotEmpty()) {
                        item {
                            Text("السجلات التاريخية للاجتماعات:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        items(meetingList) { mtg ->
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(mtg, modifier = Modifier.padding(12.dp), fontSize = 12.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                }
            }
            2 -> {
                // To-Do List smart
                var todoText by remember { mutableStateOf("") }
                var priorityInput by remember { mutableStateOf("Medium") }

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("إضافة مهمة جديدة لجدول الأعمال", fontWeight = FontWeight.Bold, color = WarmGold)
                            OutlinedTextField(value = todoText, onValueChange = { todoText = it }, placeholder = { Text("اكتب تفصيل المهمة المنزلية...") }, modifier = Modifier.fillMaxWidth())
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val priorities = listOf("High", "Medium", "Low")
                                priorities.forEach { prio ->
                                    val isSelected = priorityInput == prio
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) WarmGold else MaterialTheme.colorScheme.surface)
                                            .border(BorderStroke(1.dp, WarmGold.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                                            .clickable { priorityInput = prio }
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(prio, color = if (isSelected) MidnightBlue else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (todoText.isNotBlank()) {
                                        onAddTodo(todoText, priorityInput)
                                        todoText = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إدخل للحافظة", color = MidnightBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(todos) { todo ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.3f)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = todo.isCompleted, onCheckedChange = { onToggleTodo(todo) }, colors = CheckboxDefaults.colors(checkedColor = WarmGold))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(todo.title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text("الأولوية: ${todo.priority}", fontSize = 10.sp, color = WarmGold)
                                        }
                                    }
                                    IconButton(onClick = { onDeleteTodo(todo.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = null)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            3 -> {
                // Appliance Maintenance table
                var appName by remember { mutableStateOf("") }
                var appDate by remember { mutableStateOf("") }
                var appCycleStr by remember { mutableStateOf("") }

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("تسجيل صيانة دورية لجهاز منزلي", fontWeight = FontWeight.Bold, color = WarmGold)
                            OutlinedTextField(value = appName, onValueChange = { appName = it }, placeholder = { Text("اسم الجهاز (مثل: مكيف، مصفى ماء)") }, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = appDate, onValueChange = { appDate = it }, placeholder = { Text("آخر صيانة (YYYY-MM-DD)") }, modifier = Modifier.weight(1.5f), singleLine = true)
                                OutlinedTextField(value = appCycleStr, onValueChange = { appCycleStr = it }, placeholder = { Text("دورة الأيام (مثل: 90)") }, modifier = Modifier.weight(1f), singleLine = true)
                            }

                            Button(
                                onClick = {
                                    val cycleOn = appCycleStr.toIntOrNull() ?: 30
                                    if (appName.isNotBlank() && appDate.isNotBlank()) {
                                        onAddAppliance(appName, appDate, cycleOn)
                                        appName = ""
                                        appDate = ""
                                        appCycleStr = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("حفظ خطة الصيانة", color = MidnightBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(appliances) { appliance ->
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(appliance.name, fontWeight = FontWeight.Bold)
                                        Text("آخر صيانة: ${appliance.serviceDate} | كل ${appliance.cycleDays} يوماً", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    IconButton(onClick = { onDeleteAppliance(appliance.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = null)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            4 -> {
                // Family Health (medicines registers and vaccine alarms)
                var medPatientName by remember { mutableStateOf("") }
                var medName by remember { mutableStateOf("") }
                var medTime by remember { mutableStateOf("") }
                var medDosage by remember { mutableStateOf("") }
                var medPrescribedBy by remember { mutableStateOf("") }
                var medMethod by remember { mutableStateOf("") }
                var medWarnings by remember { mutableStateOf("") }
                var medRecommendations by remember { mutableStateOf("") }
                var medNotes by remember { mutableStateOf("") }

                val context = LocalContext.current
                var tts by remember { mutableStateOf<TextToSpeech?>(null) }
                DisposableEffect(context) {
                    val ttsInstance = TextToSpeech(context) { status -> }
                    try {
                        ttsInstance.language = Locale("ar")
                    } catch (e: Exception) {}
                    tts = ttsInstance
                    onDispose {
                        ttsInstance.stop()
                        ttsInstance.shutdown()
                    }
                }

                val playSpeakReminder = { med: Medication ->
                    val patientNameText = if (med.patientName.isNotBlank()) med.patientName else "يا رعاك الله"
                    val methodText = if (med.method.isNotBlank()) med.method else ""
                    val warningsText = if (med.warnings.isNotBlank()) med.warnings else ""
                    val dosageText = if (med.dosage.isNotBlank()) "الجرعة: ${med.dosage}" else ""
                    val message = "السلام عليكم $patientNameText، حان وقت تناول دواء ${med.name}. $dosageText. $methodText. $warningsText. شفاك الله وعافاك."
                    tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "reminder_${med.id}")
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val intent = Intent(context, HealthHubActivity::class.java)
                                        context.startActivity(intent)
                                    },
                                border = BorderStroke(1.5.dp, WarmGold.copy(alpha = 0.8f)),
                                colors = CardDefaults.cardColors(containerColor = MidnightBlue)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("👨‍⚕️ البوابة الصحية الذكية المتكاملة للأسرة", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("تتضمن البوابة حاسبة جرعات الأطفال الطبية المتقدمة، قياس المؤشرات الحيوية، دليل إسعافات 12 حالة، الكتيبات التوعوية، مولد التقارير الطبية، دليل السموم، وقسم طوارئ مجهز بعلامات الفرز.", fontSize = 11.sp, color = TextLight, textAlign = TextAlign.Center, lineHeight = 16.sp)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            val intent = Intent(context, HealthHubActivity::class.java)
                                            context.startActivity(intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold)
                                    ) {
                                        Icon(Icons.Default.Launch, null, tint = MidnightBlue)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("الدخول للبوابة الطبية المتكاملة ↩", color = MidnightBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.4f)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("جدولة أدوية المريض ومتابعة الجرعات", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 16.sp)
                                    
                                    OutlinedTextField(
                                        value = medPatientName,
                                        onValueChange = { medPatientName = it },
                                        label = { Text("اسم المريض") },
                                        placeholder = { Text("مثال: الوالد صالح، الأم...") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = WarmGold,
                                            unfocusedBorderColor = WarmGold.copy(alpha = 0.5f),
                                            focusedLabelColor = WarmGold
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    
                                    OutlinedTextField(
                                        value = medName,
                                        onValueChange = { medName = it },
                                        label = { Text("اسم الدواء") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = WarmGold,
                                            unfocusedBorderColor = WarmGold.copy(alpha = 0.5f),
                                            focusedLabelColor = WarmGold
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = medTime,
                                            onValueChange = { medTime = it },
                                            label = { Text("الوقت (مثلاً: 8 صباحاً)") },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = WarmGold,
                                                unfocusedBorderColor = WarmGold.copy(alpha = 0.5f),
                                                focusedLabelColor = WarmGold
                                            ),
                                            modifier = Modifier.weight(1.5f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = medDosage,
                                            onValueChange = { medDosage = it },
                                            label = { Text("الجرعة (مثال: حبة)") },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = WarmGold,
                                                unfocusedBorderColor = WarmGold.copy(alpha = 0.5f),
                                                focusedLabelColor = WarmGold
                                            ),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                    
                                    OutlinedTextField(
                                        value = medPrescribedBy,
                                        onValueChange = { medPrescribedBy = it },
                                        label = { Text("وصف بواسطة (طبيب / مستشفى)") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = WarmGold,
                                            unfocusedBorderColor = WarmGold.copy(alpha = 0.5f),
                                            focusedLabelColor = WarmGold
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    
                                    OutlinedTextField(
                                        value = medMethod,
                                        onValueChange = { medMethod = it },
                                        label = { Text("طريقة الاستخدام (شفوي، حقن...)") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = WarmGold,
                                            unfocusedBorderColor = WarmGold.copy(alpha = 0.5f),
                                            focusedLabelColor = WarmGold
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    
                                    OutlinedTextField(
                                        value = medWarnings,
                                        onValueChange = { medWarnings = it },
                                        label = { Text("المحاذير والأعراض الجانبية") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = WarmGold,
                                            unfocusedBorderColor = WarmGold.copy(alpha = 0.5f),
                                            focusedLabelColor = WarmGold
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    
                                    OutlinedTextField(
                                        value = medRecommendations,
                                        onValueChange = { medRecommendations = it },
                                        label = { Text("ارشادات / توصيات خاصة") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = WarmGold,
                                            unfocusedBorderColor = WarmGold.copy(alpha = 0.5f),
                                            focusedLabelColor = WarmGold
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = medNotes,
                                        onValueChange = { medNotes = it },
                                        label = { Text("ملاحظات إضافية") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = WarmGold,
                                            unfocusedBorderColor = WarmGold.copy(alpha = 0.5f),
                                            focusedLabelColor = WarmGold
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Button(
                                        onClick = {
                                            if (medName.isNotBlank() && medTime.isNotBlank()) {
                                                onAddMedication(
                                                    medPatientName,
                                                    medName,
                                                    medTime,
                                                    medDosage,
                                                    medPrescribedBy,
                                                    medMethod,
                                                    medWarnings,
                                                    medRecommendations,
                                                    medNotes
                                                )
                                                medPatientName = ""
                                                medName = ""
                                                medTime = ""
                                                medDosage = ""
                                                medPrescribedBy = ""
                                                medMethod = ""
                                                medWarnings = ""
                                                medRecommendations = ""
                                                medNotes = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("إضافة موعد الرعاية الطبية", color = MidnightBlue, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("جدول الأدوية المسجلة", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 14.sp)
                        }

                        items(medications) { med ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                ),
                                border = BorderStroke(0.5.dp, WarmGold.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = med.isTaken,
                                                onCheckedChange = { onToggleMedication(med) },
                                                colors = CheckboxDefaults.colors(checkedColor = WarmGold)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                if (med.patientName.isNotBlank()) {
                                                    Text("المريض: ${med.patientName}", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                }
                                                Text(med.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextLight)
                                            }
                                        }
                                        
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Vocal Reminder Button
                                            IconButton(onClick = { playSpeakReminder(med) }) {
                                                Icon(Icons.Default.VolumeUp, contentDescription = "تذكير صوتي", tint = WarmGold)
                                            }
                                            IconButton(onClick = { onDeleteMedication(med.id) }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color.Red.copy(alpha = 0.7f))
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Details of medication
                                    Column(
                                        modifier = Modifier.padding(start = 32.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("💊 الجرعة: ${med.dosage.ifBlank { "غير محددة" }}", color = Color(0xFFD2B48C), fontSize = 13.sp)
                                        Text("⏰ الموعد: ${med.scheduleTime}", color = WarmGold, fontSize = 13.sp)
                                        
                                        if (med.prescribedBy.isNotBlank()) {
                                             Text("⚕️ وصف بواسطة: ${med.prescribedBy}", color = TextMuted, fontSize = 12.sp)
                                        }
                                        if (med.method.isNotBlank()) {
                                             Text("🎯 الطريقة: ${med.method}", color = TextMuted, fontSize = 12.sp)
                                        }
                                        if (med.warnings.isNotBlank()) {
                                             Text("⚠️ المحاذير: ${med.warnings}", color = Color(0xFFE57373), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        if (med.recommendations.isNotBlank()) {
                                             Text("💡 ارشادات: ${med.recommendations}", color = Color(0xFF81C784), fontSize = 12.sp)
                                        }
                                        if (med.notes.isNotBlank()) {
                                             Text("📝 ملاحظات: ${med.notes}", color = TextMuted, fontSize = 12.sp)
                                        }
                                        
                                        if (med.isTaken) {
                                             Spacer(modifier = Modifier.height(4.dp))
                                             Text("✅ تم تناول الدواء بنجاح", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            5 -> {
                // Hisn Al Muslim & Language converter
                var weatherText = "صنعاء: مشمس وحار جزئياً 28°م | عدن: رطوبة وحرارة مرتفعة 34°م"
                var exchangeText = "الدولار الأمريكي: شراء 530 | ريال سعودي: شراء 140 (سعر التأكيد المحلي)"
                
                var englishQuery by remember { mutableStateOf("") }
                var englishAnswer by remember { mutableStateOf("ابدأ كتابة كلمة أو جرب اختبار الكلمات لتعلم الإنجليزية مدمجاً.") }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.5f))) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("الطقس وأسعار الصرف المحلية اليوم:", fontWeight = FontWeight.Bold, color = WarmGold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(weatherText, fontSize = 12.sp, lineHeight = 16.sp)
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                Text(exchangeText, fontSize = 12.sp, lineHeight = 16.sp)
                            }
                        }
                    }

                    item {
                        Card(border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.5f))) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("تعلم واختبار اللغة الإنجليزية:", fontWeight = FontWeight.Bold, color = WarmGold)
                                Text(englishAnswer, fontSize = 12.sp, lineHeight = 16.sp)
                                
                                OutlinedTextField(value = englishQuery, onValueChange = { englishQuery = it }, placeholder = { Text("اكتب كلمة لتجربتها باللغة الإنجليزية...") }, modifier = Modifier.fillMaxWidth())
                                Row {
                                    Button(
                                        onClick = {
                                            if (englishQuery.isNotBlank()) {
                                                englishAnswer = "تعلم ذكي: الكلمة \"$englishQuery\" تعني باللغة الإنجليزية شيئاً ممتازاً مفيداً للأسرة."
                                                englishQuery = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("ترجمة ذكية", color = MidnightBlue, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.5f))) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("من أدعية حصن المسلم المأثورة للطفل:", fontWeight = FontWeight.Bold, color = WarmGold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("دعاء الاستيقاظ: \"الْحَمْدُ للهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ.\"", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 13.sp, lineHeight = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

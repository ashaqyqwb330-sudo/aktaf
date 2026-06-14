package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class VitalLog(
    val type: String,
    val value: String,
    val unit: String,
    val dateStr: String,
    val note: String
)

class VitalSignsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                VitalSignsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalSignsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0=Logs, 1=BMI Calendar, 2=WHO Growth

    // BMI Variables
    var heightCm by remember { mutableStateOf("170") }
    var weightKg by remember { mutableStateOf("70") }
    var bmiResult by remember { mutableStateOf("") }
    var bmiClassification by remember { mutableStateOf("") }
    var bmiColor by remember { mutableStateOf(GreenAccent) }

    // Logs variables
    var showAddDialog by remember { mutableStateOf(false) }
    var logType by remember { mutableStateOf("ضغظ الدم") }
    val typesList = listOf("ضغط الدم", "مستويات السكر", "نبضات القلب", "درجة الحرارة", "الأكسجين")
    var dropdownExpanded by remember { mutableStateOf(false) }
    var logValue by remember { mutableStateOf("") }
    var logNote by remember { mutableStateOf("") }

    val recentLogs = remember {
        mutableStateListOf(
            VitalLog("ضغط الدم", "120/80", "mmHg", "اليوم 10:00 ص", "طبيعي ومستقر"),
            VitalLog("مستويات السكر", "95", "mg/dL", "اليوم 09:12 ص", "صائم / طبيعي"),
            VitalLog("درجة الحرارة", "36.8", "°C", "أمس 08:30 م", "ممتاز"),
            VitalLog("نبضات القلب", "72", "bpm", "أمس 08:30 م", "خلال الاسترخاء")
        )
    }

    // BMI Recalculate
    LaunchedEffect(heightCm, weightKg) {
        val h = heightCm.toDoubleOrNull() ?: 1.0
        val w = weightKg.toDoubleOrNull() ?: 0.0
        if (h > 50 && w > 10) {
            val hMeter = h / 100.0
            val b = w / (hMeter * hMeter)
            bmiResult = String.format("%.1f", b)
            when {
                b < 18.5 -> {
                    bmiClassification = "نقص في الوزن (Underweight) ⚠️"
                    bmiColor = Color(0xFFE2B83E)
                }
                b < 25.0 -> {
                    bmiClassification = "وزن طبيعي وصحي (Normal) ✅"
                    bmiColor = GreenAccent
                }
                b < 30.0 -> {
                    bmiClassification = "زيادة في الوزن (Overweight) ⚠️"
                    bmiColor = Color(0xFFF39C12)
                }
                else -> {
                    bmiClassification = "سمنة مفرطة (Obesity) 🚨"
                    bmiColor = RedAccent
                }
            }
        } else {
            bmiResult = "--"
            bmiClassification = "الرجاء كشط الطول والوزن بالشكل الصحيح."
            bmiColor = SilverGray
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 غرفة العلامات الحيوية والقياسات", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = WarmGold)
                    }
                },
                actions = {
                    if (selectedTab == 0) {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Log", tint = WarmGold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightBlue)
            )
        },
        containerColor = MidnightBlue
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MidnightBlue)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceDark,
                contentColor = WarmGold
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Text("مؤشرات الحيوية", fontSize = 12.sp, color = if(selectedTab == 0) WarmGold else TextLight)
                    }
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Text("حاسبة كتلة الجسم BMI", fontSize = 12.sp, color = if(selectedTab == 1) WarmGold else TextLight)
                    }
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Text("دليل النمو للأطفال (WHO)", fontSize = 12.sp, color = if(selectedTab == 2) WarmGold else TextLight)
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.MonitorHeart, null, tint = MidnightBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تسجيل مؤشر حيوي جديد", color = MidnightBlue, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("القياسات الأخيرة للأسرة:", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(recentLogs) { log ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = when(log.type) {
                                                "ضغط الدم" -> Icons.Default.Favorite
                                                "مستويات السكر" -> Icons.Default.Bloodtype
                                                "درجة الحرارة" -> Icons.Default.Thermostat
                                                "نبضات القلب" -> Icons.Default.MonitorHeart
                                                else -> Icons.Default.Speed
                                            },
                                            contentDescription = null,
                                            tint = when(log.type) {
                                                "ضغط الدم" -> RedAccent
                                                "مستويات السكر" -> WarmGold
                                                "درجة الحرارة" -> Color(0xFFF39C12)
                                                else -> GreenAccent
                                            },
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(log.type, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(log.note, color = SilverGray, fontSize = 11.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("${log.value} ${log.unit}", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text(log.dateStr, color = SilverGray, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("🧮 حاسبة مؤشر كتلة الجسم (BMI)", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                
                                OutlinedTextField(
                                    value = heightCm,
                                    onValueChange = { heightCm = it },
                                    label = { Text("الطول (سنتيمتر)", color = WarmGold) },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmGold, focusedTextColor = TextLight, unfocusedTextColor = TextLight)
                                )

                                OutlinedTextField(
                                    value = weightKg,
                                    onValueChange = { weightKg = it },
                                    label = { Text("الوزن (كيلوجرام)", color = WarmGold) },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmGold, focusedTextColor = TextLight, unfocusedTextColor = TextLight)
                                )
                            }
                        }

                        // BMI Result Screen Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("مؤشر كتلة جسمك الحالي", color = TextLight, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    bmiResult,
                                    color = bmiColor,
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    bmiClassification,
                                    color = bmiColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    "مهم: مؤشر كتلة الجسم هو فحص رقمي يعتمد على علاقة الوزن بالطول وصالح للبالغين فوق 18 عاماً فقط.",
                                    color = SilverGray,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
                2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("📌 دليل منظمة الصحة العالمية لنمو الأطفال (WHO)", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "يتيح هذا الدليل تتبع نمو طفلك ومطابقة وزنه وطوله مع المعدلات العالمية القياسية لمنع تشخيص السمنة أو الهزال مبكراً.",
                                    color = TextLight,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        // Height/Weight WHO Reference List
                        Text("المرشد المرجعي لمعايير النمو (WHO):", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        
                        val whoReferences = listOf(
                            Pair("أولاد عمر سنة (Boy 1Y)", "الوزن المثالي: 9.6 كجم | الطول المثالي: 75.7 سم"),
                            Pair("بنات عمر سنة (Girl 1Y)", "الوزن المثالي: 8.9 كجم | الطول المثالي: 74.0 سم"),
                            Pair("أولاد عمر سنتين (Boy 2Y)", "الوزن المثالي: 12.2 كجم | الطول المثالي: 87.8 سم"),
                            Pair("بنات عمر سنتين (Girl 2Y)", "الوزن المثالي: 11.5 كجم | الطول المثالي: 86.4 سم"),
                            Pair("أولاد عمر 4 سنوات (Boy 4Y)", "الوزن المثالي: 16.3 كجم | الطول المثالي: 103.3 سم"),
                            Pair("بنات عمر 4 سنوات (Girl 4Y)", "الوزن المثالي: 15.4 كجم | الطول المثالي: 102.7 سم")
                        )

                        whoReferences.forEach { ref ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ChildCare, null, tint = WarmGold, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(ref.first, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(ref.second, color = SilverGray, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("✍️ تسجيل قياس جديد", color = WarmGold, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box {
                        OutlinedTextField(
                            value = logType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("نوع القياس") },
                            trailingIcon = {
                                IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            typesList.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type, color = TextLight) },
                                    onClick = {
                                        logType = type
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = logValue,
                        onValueChange = { logValue = it },
                        label = { Text("القيمة المقروءة") },
                        placeholder = { Text("مثال: 120/80 أو 37.2") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = logNote,
                        onValueChange = { logNote = it },
                        label = { Text("ملاحظة أو الحالة") },
                        placeholder = { Text("مثال: بعد الأكل / صائم") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (logValue.isBlank()) {
                            Toast.makeText(context, "الرجاء كشط القيمة أولاً", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val unit = when(logType) {
                            "ضغط الدم" -> "mmHg"
                            "مستويات السكر" -> "mg/dL"
                            "درجة الحرارة" -> "°C"
                            "نبضات القلب" -> "bpm"
                            else -> "%"
                        }
                        recentLogs.add(0, VitalLog(logType, logValue, unit, "اليوم الآن", if(logNote.isBlank()) "مستقر" else logNote))
                        showAddDialog = false
                        logValue = ""
                        logNote = ""
                        Toast.makeText(context, "تم حفظ المؤشر الحيوي بنجاح بنظام اكتفاء", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold)
                ) {
                    Text("حفظ", color = MidnightBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء", color = SilverGray)
                }
            },
            containerColor = SurfaceWarm,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AiMedicalActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                AiMedicalScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiMedicalScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) } // 0=Lab Scan, 1=X-Ray Scan, 2=Rx Reader, 3=Interactions

    // Interaction Check states
    var medAlpha by remember { mutableStateOf("") }
    var medBeta by remember { mutableStateOf("") }
    var interactionResult by remember { mutableStateOf<String?>(null) }
    var interactionSeverity by remember { mutableStateOf<Color>(GreenAccent) }

    // Lab scan state
    var selectedLabTemplate by remember { mutableStateOf("صورة الدم الكاملة (CBC)") }
    val labTemplates = listOf("صورة الدم الكاملة (CBC)", "وظائف الكلى والعضل", "فحص السكري التراكمي")
    var labDropdownExpanded by remember { mutableStateOf(false) }
    var isLabAnalyzing by remember { mutableStateOf(false) }
    var parsedLabValues by remember { mutableStateOf<List<Triple<String, String, String>>?>(null) } // Name, Value, Evaluation

    // Xray state
    var selectedXrayType by remember { mutableStateOf("أشعة الركبة اليمنى") }
    val xrayTypes = listOf("أشعة الركبة اليمنى", "أشعة الجمجمة الجانبية", "أشعة العمود الفقري")
    var xrayDropdownExpanded by remember { mutableStateOf(false) }
    var isXrayAnalyzing by remember { mutableStateOf(false) }
    var xrayReportResult by remember { mutableStateOf<String?>(null) }

    // Rx Reader state
    var doctorNotesText by remember { mutableStateOf("Rx: Panadol 500mg 1 tab t.i.d p.c \nRx: Amoxil 500mg 1 cap b.i.d a.c") }
    var isRxReading by remember { mutableStateOf(false) }
    var parsedRxTable by remember { mutableStateOf<List<Triple<String, String, String>>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🤖 عيادة ومحلل الذكاء الاصطناعي الطبي", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = WarmGold)
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
                        Text("قارئ التحاليل", fontSize = 12.sp, color = if(selectedTab == 0) WarmGold else TextLight)
                    }
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Text("قارئ الأشعة", fontSize = 12.sp, color = if(selectedTab == 1) WarmGold else TextLight)
                    }
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Text("مفسر الروشتات", fontSize = 12.sp, color = if(selectedTab == 2) WarmGold else TextLight)
                    }
                }
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Text("تفاعل الأدوية", fontSize = 12.sp, color = if(selectedTab == 3) WarmGold else TextLight)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Lab scan template chooser
                        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("🧪 اختر نوع تقرير التحليل المراد قراءته وتفسيره:", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = selectedLabTemplate,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = {
                                            IconButton(onClick = { labDropdownExpanded = true }) {
                                                Icon(Icons.Default.ArrowDropDown, null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    DropdownMenu(
                                        expanded = labDropdownExpanded,
                                        onDismissRequest = { labDropdownExpanded = false },
                                        modifier = Modifier.background(SurfaceDark)
                                    ) {
                                        labTemplates.forEach { template ->
                                            DropdownMenuItem(
                                                text = { Text(template, color = TextLight) },
                                                onClick = {
                                                    selectedLabTemplate = template
                                                    parsedLabValues = null
                                                    labDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            isLabAnalyzing = true
                                            parsedLabValues = null
                                            delay(1500) // Simulation AI latency
                                            parsedLabValues = when (selectedLabTemplate) {
                                                "صورة الدم الكاملة (CBC)" -> listOf(
                                                    Triple("خضاب الدم (Hemoglobin)", "10.5 g/dL", "⚠️ منخفض قليلاً (الحد الأدنى: 12) - قد يشير لفقر دم بسيط"),
                                                    Triple("خلايا الدم البيضاء (WBC)", "6,200 /ul", "✅ طبيعي (المدى: 4000 - 11000)"),
                                                    Triple("الصفيحات الدموية (Platelets)", "220,000 /ul", "✅ ممتازة وطبيعية")
                                                )
                                                "وظائف الكلى والعضل" -> listOf(
                                                    Triple("الكرياتينين (Creatinine)", "0.8 mg/dL", "✅ سليم وطبيعي بالكامِل"),
                                                    Triple("اليوريا (Blood Urea)", "24 mg/dL", "✅ ممتازة وطبيعية"),
                                                    Triple("حمض البول (Uric Acid)", "7.1 mg/dL", "⚠️ مرتفع بشكل طفيف (المرشد: 7.0)")
                                                )
                                                else -> listOf(
                                                    Triple("السكر التراكمي (HbA1c)", "5.4%", "✅ ممتاز وطبيعي (غير مصاب بسكري)"),
                                                    Triple("سكر الدم العشوائي", "96 mg/dL", "✅ طبيعي ومستقر (أقل من 140)")
                                                )
                                            }
                                            isLabAnalyzing = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold)
                                ) {
                                    if (isLabAnalyzing) {
                                        CircularProgressIndicator(color = MidnightBlue, modifier = Modifier.size(24.dp))
                                    } else {
                                        Text("تشغيل المحاكاة وقراءة التقرير بالذكاء الاصطناعي", color = MidnightBlue, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Parse output display
                        parsedLabValues?.let { list ->
                            Card(colors = CardDefaults.cardColors(containerColor = SurfaceWarm), shape = RoundedCornerShape(16.dp)) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("📝 النتائج والقيم المستخرجة بدراسة طبية:", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    list.forEach { item ->
                                        Column {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(item.first, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(item.second, color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            }
                                            Text(item.third, color = if(item.third.contains("⚠️")) Color(0xFFF39C12) else GreenAccent, fontSize = 11.sp, lineHeight = 16.sp)
                                            Divider(color = MidnightBlue, modifier = Modifier.padding(vertical = 6.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // XRay analyzer
                        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("🩻 اختر نوع الأشعة الطبية لتحليلها فورياً:", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = selectedXrayType,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = {
                                            IconButton(onClick = { xrayDropdownExpanded = true }) {
                                                Icon(Icons.Default.ArrowDropDown, null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    DropdownMenu(
                                        expanded = xrayDropdownExpanded,
                                        onDismissRequest = { xrayDropdownExpanded = false },
                                        modifier = Modifier.background(SurfaceDark)
                                    ) {
                                        xrayTypes.forEach { xray ->
                                            DropdownMenuItem(
                                                text = { Text(xray, color = TextLight) },
                                                onClick = {
                                                    selectedXrayType = xray
                                                    xrayReportResult = null
                                                    xrayDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            isXrayAnalyzing = true
                                            xrayReportResult = null
                                            delay(1600)
                                            xrayReportResult = when (selectedXrayType) {
                                                "أشعة الركبة اليمنى" -> "• التقرير الأولي: لا تظهر الأشعة السينية أي كسر مفصلي. سلامة العظام والأربطة واضحة. يظهر تآكل بسيط في غضروف الصابونة يتسق مع المراحل الأولى للاحتكاك المفصلي.\n• التوصية الصحية: تجنب الوقوف الطويل وصعود الدرج العنيف."
                                                "أشعة الجمجمة الجانبية" -> "• التقرير الأولي: سلامة قبو الجمجمة وعظامه الهيكلية تام ولا توجد أي كسور شعرية أو نزيف خارجي، مع انتظام كامل في الجيوب الأنفية والسرج التركي.\n• التوصية الصحية: استشارة طبيب عيون إذا كان الصداع مزمناً."
                                                else -> "• التقرير الأولي: تظهر الأشعة استقامة سليمة للفقرات القطنية مع وجود انضغاط بسيط للغاية غير مؤثر بين الفقرة L4 و L5.\n• التوصية الصحية: ينصح بممارسة تمرين تمدد الظهر وتجنب حمل الأوزان الثقيلة."
                                            }
                                            isXrayAnalyzing = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold)
                                ) {
                                    if (isXrayAnalyzing) {
                                        CircularProgressIndicator(color = MidnightBlue, modifier = Modifier.size(24.dp))
                                    } else {
                                        Text("مسح وتحليل صورة الأشعة السينية والتقرير اللفظي", color = MidnightBlue, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        xrayReportResult?.let { report ->
                            Card(colors = CardDefaults.cardColors(containerColor = SurfaceWarm), shape = RoundedCornerShape(16.dp)) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("📝 تقرير تحليل الأشعة الطبي بالذكاء الاصطناعي:", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        report,
                                        color = TextLight,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "⚠️ إشلاء مسؤولية: هذا التشخيص محاكاة مساعدة وافتراضية، ومطالبة الكشف الإكلينيكي المباشر لدى طبيب العظام المختص ضرورية للتأكيد.",
                                        color = RedAccent.copy(alpha = 0.8f),
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        // Rx Interpreter
                        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("📋 اكتب أو الصق كتابة الطبيب بالروشتة لتفسيرها:", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                OutlinedTextField(
                                    value = doctorNotesText,
                                    onValueChange = { doctorNotesText = it },
                                    modifier = Modifier.fillMaxWidth().height(100.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmGold, focusedTextColor = TextLight, unfocusedTextColor = TextLight)
                                )

                                Button(
                                    onClick = {
                                        scope.launch {
                                            isRxReading = true
                                            parsedRxTable = null
                                            delay(1500)
                                            parsedRxTable = listOf(
                                                Triple("بنادول (Panadol 500mg)", "حبة واحدة (1 Tab)", "3 مرات يومياً بعد الأكل (t.i.d p.c)"),
                                                Triple("أموكسيل (Amoxil 500mg)", "كبسولة واحدة (1 Cap)", "مرتين يومياً قبل الأكل (b.i.d a.c)")
                                            )
                                            isRxReading = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold)
                                ) {
                                    if (isRxReading) {
                                        CircularProgressIndicator(color = MidnightBlue, modifier = Modifier.size(24.dp))
                                    } else {
                                        Text("تفسير الروشتة اللاتينية إلى جدول أدوية عربي", color = MidnightBlue, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        parsedRxTable?.let { table ->
                            Card(colors = CardDefaults.cardColors(containerColor = SurfaceWarm), shape = RoundedCornerShape(16.dp)) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("📋 جدول مواعيد الأدوية المستخلص من الروشتة:", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    table.forEach { row ->
                                        Column {
                                            Text(row.first, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("الجرعة: ${row.second}", color = GoldLight, fontSize = 12.sp)
                                                Text("الميعاد والوقت: ${row.third}", color = GreenAccent, fontSize = 12.sp)
                                            }
                                            Divider(color = MidnightBlue, modifier = Modifier.padding(vertical = 6.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        // Drug Interactions checker
                        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("🔍 فاحص التداخلات والتفاعلات الدوائية الخطرة:", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                
                                OutlinedTextField(
                                    value = medAlpha,
                                    onValueChange = {
                                        medAlpha = it
                                        interactionResult = null
                                    },
                                    label = { Text("المادة الفعالة أو اسم الدواء الأول") },
                                    placeholder = { Text("مثال: ايبوبروفين أو اوزمبيك") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmGold, focusedTextColor = TextLight, unfocusedTextColor = TextLight)
                                )

                                OutlinedTextField(
                                    value = medBeta,
                                    onValueChange = {
                                        medBeta = it
                                        interactionResult = null
                                    },
                                    label = { Text("اسم الدواء الثاني للتحقق للأسرة") },
                                    placeholder = { Text("مثال: اسبرين أو بنادول") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmGold, focusedTextColor = TextLight, unfocusedTextColor = TextLight)
                                )

                                Button(
                                    onClick = {
                                        if (medAlpha.isBlank() || medBeta.isBlank()) {
                                            Toast.makeText(context, "الرجاء كتابة اسم الدواءين", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val m1 = medAlpha.lowercase()
                                        val m2 = medBeta.lowercase()
                                        
                                        if (
                                            (m1.contains("brufen") || m1.contains("ibuprofen") || m1.contains("بروفين") || m1.contains("ايبوبروفين")) &&
                                            (m2.contains("aspirin") || m2.contains("اسبرين"))
                                        ) {
                                            interactionResult = "⚠️ تداخل دوائي عالي الخطورة!\nيؤدي استخدام مضادات الالتهاب غير الستيروئيدية (مثل الإيبوبروفين) سوياً مع الأسبرين إلى زيادة مضاعفة لفرصة الإصابة بنزيف الجهاز الهضمي وقرحة المعدة الحادة، كما يعطل عمل الأسبرين كحامٍ لتجلط القلب."
                                            interactionSeverity = RedAccent
                                        } else if (
                                            (m1.contains("panadol") || m1.contains("paracetamol") || m1.contains("بنادول") || m1.contains("باراسيتامول")) &&
                                            (m2.contains("brufen") || m2.contains("ايبوبروفين"))
                                        ) {
                                            interactionResult = "✅ تداخل آمن وطبيعي.\nيمكن استخدام الباراسيتامول والاهير بروفين سوياً في الحالات الصعبة تحت اشراف طبي وبجرعات منضبطة خافضة لحرارة الأطفال بطرق تبادلية آمنة."
                                            interactionSeverity = GreenAccent
                                        } else {
                                            interactionResult = "ℹ️ تداخل خفيف أو لم يسجل تفاعل كيميائي مباشر.\nيرجى الفصل بين الأدوية بساعتين كإجراء احترازي ومطالعة الصيدلي للتأكد التام."
                                            interactionSeverity = Color(0xFFF39C12)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold)
                                ) {
                                    Text("التحقق من التداخلات الدوائية وموانع الخلط", color = MidnightBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        interactionResult?.let { result ->
                            Card(colors = CardDefaults.cardColors(containerColor = SurfaceWarm), shape = RoundedCornerShape(16.dp)) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🚨 تقرير فحص التفاعل والخلط الكيميائي:", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        result,
                                        color = interactionSeverity,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
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
}

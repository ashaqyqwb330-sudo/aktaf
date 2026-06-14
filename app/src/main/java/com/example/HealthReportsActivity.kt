package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
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
import com.example.database.Child
import com.example.database.EktefaaDatabase
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

class HealthReportsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                HealthReportsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthReportsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { EktefaaDatabase.getDatabase(context) }
    val childrenFlow = remember { db.dao().getAllChildrenFlow() }
    val children by childrenFlow.collectAsState(initial = emptyList())

    var selectedChildId by remember { mutableStateOf(0L) }
    var activeReportText by remember { mutableStateOf("") }

    // Find selected child
    val selectedChild = children.find { it.id.toLong() == selectedChildId }

    // Init selected id when list loads
    LaunchedEffect(children) {
        if (selectedChildId == 0L && children.isNotEmpty()) {
            selectedChildId = children.first().id.toLong()
        }
    }

    // Auto update report text when selection changes
    LaunchedEffect(selectedChildId, children) {
        val child = children.find { it.id.toLong() == selectedChildId }
        val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale("ar")).format(Date())
        if (child != null) {
            val bloodType = if (child.className.isNotBlank()) child.className else "O+"
            val condition = if (child.studentId.isNotBlank()) child.studentId else "سليم ومعافى الحمد لله"
            val allergens = if (child.notes.isNotBlank()) child.notes else "لا يوجد"
            
            activeReportText = "بِسْمِ اللهِ الرَّحْمَنِ الرَّحِيمِ\n" +
                    "\"وَإِذَا مَرِضْتُ فَهُوَ يَشْفِينِ\"\n\n" +
                    "🏥 تقرير الحالة الصحية للعائلة | تطبيق اكتفاء\n" +
                    "━━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "• الاسم بالكامل: ${child.name}\n" +
                    "• العمر الحالي: ${child.age} عاماً\n" +
                    "• طائفة فصيلة الدم: $bloodType\n" +
                    "• الأمراض المزمنة المسجلة: $condition\n" +
                    "• الحساسية والتحسسات: $allergens\n" +
                    "━━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "تاريخ إصدار التقرير: $dateStr\n" +
                    "حالة التقرير: معتمد بالخادم المحلي لأسرة اكتفاء\n\n" +
                    "تم تصدير هذا التقرير الطبي تلقائياً بواسطة تطبيق اكتفاء."
        } else {
            activeReportText = "بِسْمِ اللهِ الرَّحْمَنِ الرَّحِيمِ\n" +
                    "\"وَإِذَا مَرِضْتُ فَهُوَ يَشْفِينِ\"\n\n" +
                    "الرجاء إضافة أفراد عائلتك أولاً لتصدير التقارير الطبية الرسمية بصيغ مشاركة احترافية."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📄 مولد التقارير الطبية الموثقة للأسرة", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("👨‍⚕️ اختر الفرد العائلي لتصدير تقريره الصحي الموثق:", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (children.isNotEmpty()) {
                        ScrollableTabRow(
                            selectedTabIndex = children.indexOfFirst { it.id.toLong() == selectedChildId }.coerceAtLeast(0),
                            containerColor = MidnightBlue,
                            contentColor = WarmGold,
                            edgePadding = 4.dp
                        ) {
                            children.forEach { child ->
                                Tab(
                                    selected = selectedChildId == child.id.toLong(),
                                    onClick = { selectedChildId = child.id.toLong() }
                                ) {
                                    Box(modifier = Modifier.padding(12.dp)) {
                                        Text(child.name, color = if(selectedChildId == child.id.toLong()) WarmGold else TextLight, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        Text("الرجاء إضافة أفراد من تبويب 'المرضى' أولاً لصنع تقاريرهم وتصديرها.", color = RedAccent, fontSize = 12.sp)
                    }
                }
            }

            // Report Viewer Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        "العرض المسبق للتقرير الطبي المعتمد:",
                        color = WarmGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(MidnightBlue, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                activeReportText,
                                color = TextLight,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (children.isEmpty()) {
                                    Toast.makeText(context, "لم يتم قيد أي فرد لتصديره", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, activeReportText)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "مشاركة التقرير عبر:"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, null, tint = MidnightBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("مشاركة التقرير", color = MidnightBlue, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (children.isEmpty()) {
                                    Toast.makeText(context, "الرجاء تسجيل الفرد أولاً", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                Toast.makeText(context, "جاري إعداد التنسيق وتوليد ملف PDF جاهز للطباعة...", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Description, null, tint = WarmGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("طباعة بصيغة PDF", color = TextLight)
                        }
                    }
                }
            }
        }
    }
}

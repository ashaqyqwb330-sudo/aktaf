package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import java.util.Calendar
import com.example.service.PatientReminderReceiver
import com.example.database.EktefaaDatabase
import com.example.database.Patient
import com.example.service.PatientReminderService
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class PatientsListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                PatientsListScreen(onBack = { finish() })
            }
        }
    }
}

// Local helper to parse patient medication objects
data class MedItem(
    val name: String,
    val dosage: String,
    val duration: String,
    val frequency: String,
    val times: String,
    val method: String,
    val shelf: Int
)

fun exportPatientPdf(context: Context, patient: Patient) {
    try {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Decorative Header banner
        val paintHeaderBand = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#131A26") // Deep Obsidian dark blue theme matching App style
        }
        canvas.drawRect(0f, 0f, 595f, 100f, paintHeaderBand)

        // Accent gold line
        val paintAccentLine = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#D4AF37") // Warm Gold
        }
        canvas.drawRect(0f, 100f, 595f, 106f, paintAccentLine)

        // Title and Subtitle text drawing
        val paintTitle = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#D4AF37")
            textSize = 21f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.drawText("اكتفاء - بوابة الرعاية والملف الصحي الذكي", 297f, 45f, paintTitle)

        val paintWhiteSub = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 12f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("التقرير الطبي والجدول الزمني للجرعات والأدوية", 297f, 75f, paintWhiteSub)

        var currentY = 140f

        // Content panel setup
        val framePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#F4F6F9")
            style = android.graphics.Paint.Style.FILL
        }
        val borderPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#D1D5DB")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 1f
        }

        // Section header paint style
        val sectionPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#D4AF37")
            isFakeBoldText = true
            textSize = 13f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.RIGHT
        }

        // Section 1: Basic Clinical info
        canvas.drawRoundRect(25f, currentY, 570f, currentY + 115f, 8f, 8f, framePaint)
        canvas.drawRoundRect(25f, currentY, 570f, currentY + 115f, 8f, 8f, borderPaint)

        val infoPaintVal = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#131A26")
            textSize = 11f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.RIGHT
        }

        canvas.drawText("معلومات المريض الأساسية :", 550f, currentY + 28f, sectionPaint)
        canvas.drawText("اسم المريض الثلاثي: ${patient.fullName}", 550f, currentY + 52f, infoPaintVal)
        canvas.drawText("العمر: ${patient.age} عاماً  |  فصيلة الدم: ${patient.bloodType}", 550f, currentY + 76f, infoPaintVal)
        canvas.drawText("التشخيص الطبي العام: ${patient.diagnosis}", 550f, currentY + 100f, infoPaintVal)

        currentY += 135f

        // Section 2: Clinical Details & doctor instructions
        canvas.drawRoundRect(25f, currentY, 570f, currentY + 105f, 8f, 8f, framePaint)
        canvas.drawRoundRect(25f, currentY, 570f, currentY + 105f, 8f, 8f, borderPaint)

        canvas.drawText("تفاصيل الشكوى الحالية والتعليمات :", 550f, currentY + 28f, sectionPaint)
        canvas.drawText("الشكوى المرضية: ${patient.currentComplaint.ifBlank { "لا توجد شكوى مسجلة حركياً" }}", 550f, currentY + 52f, infoPaintVal)
        canvas.drawText("توجيهات الدكتور (${patient.treatingDoctor}): ${patient.doctorInstructions.ifBlank { "التزام برصيد رفوف الصيدلية الموضح" }}", 550f, currentY + 76f, infoPaintVal)

        currentY += 125f

        // Section 3: Medication Schedule table
        canvas.drawText("صيدلية ورفوف الأدوية والمنبه الصوتي الذكي :", 550f, currentY + 25f, sectionPaint)
        currentY += 35f

        // Table headers background
        val tableHeaderPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#131A26")
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawRect(25f, currentY, 570f, currentY + 30f, tableHeaderPaint)

        val tableHeaderTxt = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        canvas.drawText("دواء وصيدلية الرفوف المخططة", 550f, currentY + 20f, tableHeaderTxt)
        canvas.drawText("مواعيد التنبيه المستحب", 340f, currentY + 20f, tableHeaderTxt)
        canvas.drawText("تصنيف الرف", 170f, currentY + 20f, tableHeaderTxt)

        currentY += 30f

        val medications = parseMedications(patient.medicationsJson)
        val shelfNames = listOf("الرف الأول (طوارئ ومسكنات)", "الرف الثاني (حموضة ومعدة)", "الرف الثالث (مزمن ومضادات)", "الرف الرابع (فيتامينات ومكملات)")

        if (medications.isEmpty()) {
            canvas.drawRect(25f, currentY, 570f, currentY + 35f, framePaint)
            canvas.drawRect(25f, currentY, 570f, currentY + 35f, borderPaint)
            val infoCenterPaint = android.graphics.Paint(infoPaintVal).apply {
                textAlign = android.graphics.Paint.Align.CENTER
            }
            canvas.drawText("لم يتم إضافة أي أدوية للصيدلية المنزلية للرفوف بعد.", 297f, currentY + 22f, infoCenterPaint)
            currentY += 35f
        } else {
            medications.forEach { med ->
                canvas.drawRect(25f, currentY, 570f, currentY + 35f, framePaint)
                canvas.drawRect(25f, currentY, 570f, currentY + 35f, borderPaint)

                val contentTxt = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 9.5f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.RIGHT
                }

                val shelfName = if (med.shelf in 1..4) shelfNames[med.shelf - 1] else "عام / غير مصنف"
                canvas.drawText("${med.name} (${med.dosage}) / تكرار: ${med.frequency}", 550f, currentY + 22f, contentTxt)
                canvas.drawText(med.times, 340f, currentY + 22f, contentTxt)
                canvas.drawText(shelfName, 170f, currentY + 22f, contentTxt)

                currentY += 35f
            }
        }

        // Draw Disclaimer / footer info
        currentY = 800f
        val paintFooter = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 8.5f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("هذا التقرير عولج رقمياً من تطبيق عائلتي الصحي (اكتفاء). الرجاء اتباع تعليمات المعالج بشكل دوري.", 297f, currentY, paintFooter)

        pdfDocument.finishPage(page)

        // Write the PDF file
        val file = File(context.cacheDir, "medical_report_${patient.fullName.replace(" ", "_")}.pdf")
        pdfDocument.writeTo(file.outputStream())
        pdfDocument.close()

        // Launch Share Chooser Intent using standard FileProvider
        val authority = "${context.packageName}.provider"
        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "تصدير وطباعة الملف الطبي للعيادة"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "فشل تصدير التقرير الطبي بصيغة PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun parseMedications(jsonStr: String): List<MedItem> {
    if (jsonStr.isBlank()) return emptyList()
    return try {
        val arr = JSONArray(jsonStr)
        val list = mutableListOf<MedItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                MedItem(
                    name = obj.optString("name", ""),
                    dosage = obj.optString("dosage", ""),
                    duration = obj.optString("duration", ""),
                    frequency = obj.optString("frequency", ""),
                    times = obj.optString("times", ""),
                    method = obj.optString("method", ""),
                    shelf = obj.optInt("shelf", 1)
                )
            )
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

fun scheduleMedicationAlarms(context: Context, patient: Patient) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val allMeds = parseMedications(patient.medicationsJson)
    var scheduledCount = 0
    allMeds.forEach { drug ->
        val times = drug.times.split(",").map { it.trim() }
        times.forEachIndexed { idx, t ->
            val timeParts = t.split(":")
            if (timeParts.size == 2) {
                val hour = timeParts[0].toIntOrNull() ?: return@forEachIndexed
                val minute = timeParts[1].toIntOrNull() ?: return@forEachIndexed
                
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (before(Calendar.getInstance())) {
                        add(Calendar.DATE, 1)
                    }
                }
                
                val receiverIntent = Intent(context, PatientReminderReceiver::class.java).apply {
                    putExtra("PATIENT_NAME", patient.fullName)
                    putExtra("MEDICATION_NAME", drug.name)
                    putExtra("MEDICATION_DOSAGE", "جرعة ${drug.dosage} - ${drug.method}")
                    putExtra("PATIENT_AGE", patient.age)
                }
                
                val requestCode = (patient.id * 1000) + (drug.shelf * 100) + idx
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    receiverIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    }
                    scheduledCount++
                } catch (e: SecurityException) {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    scheduledCount++
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientsListScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { EktefaaDatabase.getDatabase(context) }
    
    // Theme colors matching DeepObsidian design guidelines
    val themeBg = Color(0xFF0A0E14) // Absolute DeepObsidian background
    val themeSurface = Color(0xFF131A26) // DeepObsidian surface card
    val accentGold = Color(0xFFD4AF37) // Warm gold accents
    val textLight = Color(0xFFFFFFFF) // White primary text
    val textSilver = Color(0xFFC0C0C0) // Silver secondary text
    val redColor = Color(0xFFE57373) // Light coral red for alert/danger

    // Reactive patients data flow
    val patientsFlow = remember { db.patientDao().getAllPatientsFlow() }
    val patients by patientsFlow.collectAsState(initial = emptyList())

    // UI state states
    var showAddDialog by remember { mutableStateOf(false) }
    var expandedPatientId by remember { mutableStateOf<Int?>(null) }

    // Dialog Input states
    var fullName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }
    var treatingDoctor by remember { mutableStateOf("") }
    var currentComplaint by remember { mutableStateOf("") }
    var doctorInstructions by remember { mutableStateOf("") }
    var followUpDate by remember { mutableStateOf("") }
    var chronicDisease by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var bloodType by remember { mutableStateOf("O+") }
    
    // Drafted medication lists for current add dialog (up to 20 items list)
    val draftMeds = remember { mutableStateListOf<MedItem>() }

    // Medication builder state inside dialog
    var currMedName by remember { mutableStateOf("") }
    var currMedDosage by remember { mutableStateOf("") }
    var currMedDuration by remember { mutableStateOf("") }
    var currMedFrequency by remember { mutableStateOf("") }
    var currMedTimes by remember { mutableStateOf("") }
    var currMedMethod by remember { mutableStateOf("") }
    var currMedShelf by remember { mutableStateOf(1) } // Default to Shelf 1

    val bloodTypes = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
    var bloodDropdownExpanded by remember { mutableStateOf(false) }
    var rxShelfDropdownExpanded by remember { mutableStateOf(false) }

    val shelfNames = listOf(
        "الرف الأول: أدوية الطوارئ والمسكنات",
        "الرف الثاني: أدوية الأمراض المزمنة",
        "الرف الثالث: المضادات والجرعات النظامية",
        "الرف الرابع: المكملات والـفيتامينات"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "👨‍⚕️ سجل المرضى وصيدلية الأدوية عائلتي", 
                        color = accentGold, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 17.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = accentGold)
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        draftMeds.clear()
                        showAddDialog = true 
                    }) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Add Patient", tint = accentGold, modifier = Modifier.size(28.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = themeBg)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    draftMeds.clear()
                    showAddDialog = true 
                },
                containerColor = accentGold,
                contentColor = themeBg
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Patient File")
            }
        },
        containerColor = themeBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(themeBg)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // General info about smart medical cabinet
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = themeSurface),
                border = BorderStroke(1.dp, accentGold.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MedicalServices, null, tint = accentGold, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "حافظة الرعاية الصحية الرقمية ورفوف الخزن الذكية. تتبع المواعيد، تشخيص الأطباء، وحوّل الأجهزة والمستلزمات لرفوف صوتية لتنبيه الأهل بمواعيد الأدوية بدقة.",
                            color = textLight,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = accentGold.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val localScope = rememberCoroutineScope()
                        Button(
                            onClick = {
                                try {
                                    val rootArray = org.json.JSONArray()
                                    patients.forEach { patient ->
                                        val obj = org.json.JSONObject().apply {
                                            put("id", patient.id)
                                            put("fullName", patient.fullName)
                                            put("age", patient.age)
                                            put("diagnosis", patient.diagnosis)
                                            put("treatingDoctor", patient.treatingDoctor)
                                            put("currentComplaint", patient.currentComplaint)
                                            put("medicationsJson", patient.medicationsJson)
                                            put("doctorInstructions", patient.doctorInstructions)
                                            put("followUpDate", patient.followUpDate)
                                            put("chronicDisease", patient.chronicDisease)
                                            put("allergies", patient.allergies)
                                            put("bloodType", patient.bloodType)
                                        }
                                        rootArray.put(obj)
                                    }
                                    val file = File(context.filesDir, "patients_backup.json")
                                    file.writeText(rootArray.toString(4))
                                    Toast.makeText(context, "تم حفظ النسخة الاحتياطية بنظام المزامنة الداخلي بنجاح 📁", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "فشل حفظ الاحتياطي: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentGold.copy(0.12f), contentColor = accentGold),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نسخة احتياطية JSON 📤", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                localScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        val file = File(context.filesDir, "patients_backup.json")
                                        if (!file.exists()) {
                                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                Toast.makeText(context, "لم يتم العثور على أي نسخة احتياطية سابقة محفوظة ❌", Toast.LENGTH_SHORT).show()
                                            }
                                            return@launch
                                        }
                                        val jsonContent = file.readText()
                                        val rootArray = org.json.JSONArray(jsonContent)
                                        
                                        for (i in 0 until rootArray.length()) {
                                            val obj = rootArray.getJSONObject(i)
                                            val p = Patient(
                                                id = 0,
                                                fullName = obj.optString("fullName", ""),
                                                age = obj.optInt("age", 0),
                                                diagnosis = obj.optString("diagnosis", ""),
                                                treatingDoctor = obj.optString("treatingDoctor", ""),
                                                currentComplaint = obj.optString("currentComplaint", ""),
                                                medicationsJson = obj.optString("medicationsJson", "[]"),
                                                doctorInstructions = obj.optString("doctorInstructions", ""),
                                                followUpDate = obj.optString("followUpDate", ""),
                                                chronicDisease = obj.optString("chronicDisease", ""),
                                                allergies = obj.optString("allergies", ""),
                                                bloodType = obj.optString("bloodType", "O+")
                                            )
                                            db.patientDao().insertPatient(p)
                                        }
                                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            Toast.makeText(context, "تم استيراد واستعادة ${rootArray.length()} ملف مريض بنجاح! 🎉", Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            Toast.makeText(context, "فشل استعادة البيانات المريض: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentGold.copy(0.12f), contentColor = accentGold),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استعادة النسخة JSON 📥", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (patients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FolderOpen, null, tint = textSilver.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "لا توجد ملفات طبية مسجلة بعد.", 
                            color = textLight, 
                            fontSize = 15.sp, 
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "انقر على زر الإضافة (+) لإنشاء أول ملف مريض، وتنظيم أدويته داخل الرفوف.", 
                            color = textSilver, 
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(patients) { patient ->
                        val isExpanded = expandedPatientId == patient.id
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    expandedPatientId = if (isExpanded) null else patient.id 
                                },
                            colors = CardDefaults.cardColors(containerColor = themeSurface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, if (isExpanded) accentGold else Color.Transparent)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Master Header
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AccountBox, 
                                        null, 
                                        tint = accentGold, 
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            patient.fullName, 
                                            color = textLight, 
                                            fontWeight = FontWeight.Bold, 
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            "العمر: ${patient.age} سنة | الطبيب: ${patient.treatingDoctor.ifBlank { "غير مسجل" }}", 
                                            color = textSilver, 
                                            fontSize = 12.sp
                                        )
                                    }
                                    
                                    // Blood badge
                                    Box(
                                        modifier = Modifier
                                            .background(redColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                            .border(1.dp, redColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            patient.bloodType,
                                            color = redColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                
                                // Compact Quick info row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "🩺 التشخيص: ${patient.diagnosis.ifBlank { "غير محدد" }}",
                                        color = accentGold,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "Show Shelves",
                                        tint = textSilver
                                    )
                                }

                                // Interactive expanded medication shelves & history
                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(modifier = Modifier.padding(top = 12.dp)) {
                                        HorizontalDivider(color = themeBg, thickness = 1.dp)
                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Detailed text fields
                                        Text("📝 الشكوى الحالية:", color = accentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(patient.currentComplaint.ifBlank { "لا يوجد شكوى مسجلة" }, color = textLight, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))

                                        Text("⚠️ الأمراض المزمنة والحساسية:", color = accentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("مزمن: ${patient.chronicDisease.ifBlank { "سليم" }} | حساسية: ${patient.allergies.ifBlank { "لا يوجد" }}", color = textLight, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))

                                        Text("📋 تعليمات الطبيب الخاصة:", color = accentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(patient.doctorInstructions.ifBlank { "لا توجد تعليمات خاصة" }, color = textLight, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column {
                                                Text("⏰ موعد المراجعة القادمة:", color = accentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Text(patient.followUpDate.ifBlank { "لم يحدد موعد" }, color = textLight, fontSize = 12.sp)
                                            }
                                            
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Button(
                                                    onClick = {
                                                        scheduleMedicationAlarms(context, patient)
                                                        Toast.makeText(context, "تمت جدولة الأدوية وتفعيل منبه الأجهزة ⏰", Toast.LENGTH_LONG).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = accentGold),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text("تفعيل المنبه النظامي ⏰", color = themeBg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }

                                                Button(
                                                    onClick = {
                                                        // Stop any alarms playing first
                                                        val cleanIntent = Intent(context, PatientReminderService::class.java).apply {
                                                            action = PatientReminderService.ACTION_CLEANUP_AUDIO
                                                        }
                                                        context.startService(cleanIntent)
                                                        Toast.makeText(context, "تم إيقاف صوت المنبه والتحذير", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = redColor.copy(alpha = 0.8f)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text("إسكات المنبه 🔕", color = Color.White, fontSize = 10.sp)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // PDF Export Button
                                        Button(
                                            onClick = {
                                                exportPatientPdf(context, patient)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = accentGold.copy(0.12f), contentColor = accentGold),
                                            border = BorderStroke(1.dp, accentGold.copy(0.3f)),
                                            modifier = Modifier.fillMaxWidth().height(36.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(16.dp), tint = accentGold)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("تصدير التقرير الطبي ومواعيد الرفوف كـ PDF 📄", color = textLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        // Dynamic Racks & Shelves UI
                                        Text(
                                            "🗄️ صيدلية المريض - رفوف الأدوية والمنبه الصوتي الذكي", 
                                            color = accentGold, 
                                            fontSize = 12.sp, 
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        val allMeds = parseMedications(patient.medicationsJson)

                                        for (shelfIdx in 1..4) {
                                            val shelfTitle = shelfNames[shelfIdx - 1]
                                            val shelfMeds = allMeds.filter { it.shelf == shelfIdx }

                                            // Wooden Rack Background container
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .border(BorderStroke(1.dp, Color(0xFF8B5A2B).copy(alpha = 0.3f)), RoundedCornerShape(10.dp))
                                                    .background(
                                                        brush = Brush.verticalGradient(
                                                            colors = listOf(Color(0xFF2C1910), Color(0xFF1E100A))
                                                        ),
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .padding(10.dp)
                                            ) {
                                                // Shelf title header
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Layers, null, tint = accentGold, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        shelfTitle, 
                                                        color = accentGold, 
                                                        fontSize = 11.sp, 
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))

                                                if (shelfMeds.isEmpty()) {
                                                    Text(
                                                        "لا توجد أدوية مضافة في هذا الرف بعد.", 
                                                        color = textSilver.copy(alpha = 0.5f), 
                                                        fontSize = 10.sp, 
                                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                        modifier = Modifier.padding(start = 22.dp)
                                                    )
                                                } else {
                                                    // Shelved drugs mapping
                                                    shelfMeds.forEach { drug ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 4.dp)
                                                                .background(themeBg.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                                                .padding(8.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(Icons.Default.Medication, null, tint = redColor, modifier = Modifier.size(24.dp))
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    "${drug.name} (${drug.dosage})", 
                                                                    color = textLight, 
                                                                    fontWeight = FontWeight.Bold, 
                                                                    fontSize = 11.sp
                                                                )
                                                                Text(
                                                                    "جرعة: ${drug.frequency} لمدة ${drug.duration} | ${drug.method}", 
                                                                    color = textSilver, 
                                                                    fontSize = 10.sp
                                                                )
                                                                Text(
                                                                    "الأوقات المحددة: ${drug.times}", 
                                                                    color = accentGold, 
                                                                    fontSize = 9.sp
                                                                )
                                                            }

                                                            // Launch sound reminder simulation trigger
                                                            IconButton(
                                                                onClick = {
                                                                    val remindIntent = Intent(context, PatientReminderService::class.java).apply {
                                                                        putExtra("PATIENT_NAME", patient.fullName)
                                                                        putExtra("MEDICATION_NAME", drug.name)
                                                                        putExtra("MEDICATION_DOSAGE", "جرعة ${drug.dosage} - ${drug.method}")
                                                                        putExtra("PATIENT_AGE", patient.age)
                                                                    }
                                                                    context.startService(remindIntent)
                                                                    Toast.makeText(
                                                                        context, 
                                                                        "تم تشغيل جرس السبيكر والمنبه الصوتي لـ ${drug.name}", 
                                                                        Toast.LENGTH_LONG
                                                                    ).show()
                                                                },
                                                                modifier = Modifier.size(32.dp)
                                                            ) {
                                                                Icon(
                                                                    Icons.Default.VolumeUp, 
                                                                    contentDescription = "Test Notification Speaker", 
                                                                    tint = accentGold, 
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))
                                        
                                        // Delete Patient File button
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    scope.launch {
                                                        db.patientDao().deletePatient(patient.id)
                                                        Toast.makeText(context, "تم حذف ملف المريض وكافة رفوف الأدوية", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.textButtonColors(contentColor = redColor)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete File", modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("حذف الملف الطبي نهائياً", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

    // Modal dialog to add a new detailed patient
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { 
                Text(
                    "👨‍⚕️ إضافة ملف طبي وجدولة صيدلية المريض والرفوف", 
                    color = accentGold, 
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ) 
            },
            text = {
                val scrollState = rememberScrollState()
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("البيانات الشخصية والسريرية الأساسية", color = accentGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("الاسم الثلاثي واللقب للـمريض") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentGold,
                            unfocusedBorderColor = textSilver.copy(alpha = 0.4f),
                            focusedTextColor = textLight,
                            unfocusedTextColor = textLight,
                            focusedLabelColor = accentGold,
                            unfocusedLabelColor = textSilver
                        )
                    )

                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it },
                        label = { Text("العمر (بالسنوات)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentGold,
                            unfocusedBorderColor = textSilver.copy(alpha = 0.4f),
                            focusedTextColor = textLight,
                            unfocusedTextColor = textLight,
                            focusedLabelColor = accentGold,
                            unfocusedLabelColor = textSilver
                        )
                    )

                    // Blood type dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = "فصيلة الدم المحددة: $bloodType",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("فصيلة الدم وطائفتها للـطوارئ") },
                            trailingIcon = {
                                IconButton(onClick = { bloodDropdownExpanded = !bloodDropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, null, tint = accentGold)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentGold,
                                unfocusedBorderColor = textSilver.copy(alpha = 0.4f),
                                focusedTextColor = textLight,
                                unfocusedTextColor = textLight,
                                focusedLabelColor = accentGold,
                                unfocusedLabelColor = textSilver
                            )
                        )
                        DropdownMenu(
                            expanded = bloodDropdownExpanded,
                            onDismissRequest = { bloodDropdownExpanded = false },
                            modifier = Modifier.background(themeSurface)
                        ) {
                            bloodTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type, color = textLight) },
                                    onClick = {
                                        bloodType = type
                                        bloodDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = diagnosis,
                        onValueChange = { diagnosis = it },
                        label = { Text("التشخيص المرضي والطبي") },
                        placeholder = { Text("مثال: انسداد مزمن بالشعب") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentGold,
                            unfocusedBorderColor = textSilver.copy(alpha = 0.4f),
                            focusedTextColor = textLight,
                            unfocusedTextColor = textLight,
                            focusedLabelColor = accentGold,
                            unfocusedLabelColor = textSilver
                        )
                    )

                    OutlinedTextField(
                        value = treatingDoctor,
                        onValueChange = { treatingDoctor = it },
                        label = { Text("اسم الطبيب المعالج والعيادة") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentGold,
                            unfocusedBorderColor = textSilver.copy(alpha = 0.4f),
                            focusedTextColor = textLight,
                            unfocusedTextColor = textLight,
                            focusedLabelColor = accentGold,
                            unfocusedLabelColor = textSilver
                        )
                    )

                    OutlinedTextField(
                        value = currentComplaint,
                        onValueChange = { currentComplaint = it },
                        label = { Text("الشكوى الحالية للمريض") },
                        placeholder = { Text("مثال: سعال وارتفاع بالحرارة") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentGold,
                            unfocusedBorderColor = textSilver.copy(alpha = 0.4f),
                            focusedTextColor = textLight,
                            unfocusedTextColor = textLight,
                            focusedLabelColor = accentGold,
                            unfocusedLabelColor = textSilver
                        )
                    )

                    OutlinedTextField(
                        value = chronicDisease,
                        onValueChange = { chronicDisease = it },
                        label = { Text("الأمراض المزمنة (إن وجدت)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentGold,
                            unfocusedBorderColor = textSilver.copy(alpha = 0.4f),
                            focusedTextColor = textLight,
                            unfocusedTextColor = textLight,
                            focusedLabelColor = accentGold,
                            unfocusedLabelColor = textSilver
                        )
                    )

                    OutlinedTextField(
                        value = allergies,
                        onValueChange = { allergies = it },
                        label = { Text("الحساسية والتحسس الدوائي والغذائي") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentGold,
                            unfocusedBorderColor = textSilver.copy(alpha = 0.4f),
                            focusedTextColor = textLight,
                            unfocusedTextColor = textLight,
                            focusedLabelColor = accentGold,
                            unfocusedLabelColor = textSilver
                        )
                    )

                    OutlinedTextField(
                        value = doctorInstructions,
                        onValueChange = { doctorInstructions = it },
                        label = { Text("التعليمات والإرشادات الطبية المباشرة") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentGold,
                            unfocusedBorderColor = textSilver.copy(alpha = 0.4f),
                            focusedTextColor = textLight,
                            unfocusedTextColor = textLight,
                            focusedLabelColor = accentGold,
                            unfocusedLabelColor = textSilver
                        )
                    )

                    OutlinedTextField(
                        value = followUpDate,
                        onValueChange = { followUpDate = it },
                        label = { Text("موعد المراجعة القادمة ورقم التذكرة") },
                        placeholder = { Text("مثال: 2026-07-20 عيادة الصدر") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentGold,
                            unfocusedBorderColor = textSilver.copy(alpha = 0.4f),
                            focusedTextColor = textLight,
                            unfocusedTextColor = textLight,
                            focusedLabelColor = accentGold,
                            unfocusedLabelColor = textSilver
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = accentGold.copy(alpha = 0.3f), thickness = 1.dp)
                    
                    // Multi-medications Builder (supports to 20 medications!)
                    Text("💊 إضافة وتنزيل الأدوية داخل الرف الفضي والمنبه الصوتي", color = accentGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = currMedName,
                        onValueChange = { currMedName = it },
                        label = { Text("اسم الدواء التجاري أو العلمي") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentGold,
                            unfocusedBorderColor = textSilver.copy(alpha = 0.4f),
                            focusedTextColor = textLight,
                            unfocusedTextColor = textLight,
                            focusedLabelColor = accentGold,
                            unfocusedLabelColor = textSilver
                        )
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = currMedDosage,
                            onValueChange = { currMedDosage = it },
                            label = { Text("الجرعة (مثال: حبة)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentGold,
                                unfocusedBorderColor = textSilver.copy(alpha = 0.4f),
                                focusedTextColor = textLight,
                                unfocusedTextColor = textLight,
                                focusedLabelColor = accentGold,
                                unfocusedLabelColor = textSilver
                            )
                        )
                        OutlinedTextField(
                            value = currMedDuration,
                            onValueChange = { currMedDuration = it },
                            label = { Text("المدة (مثال: 5 أيام)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentGold,
                                unfocusedBorderColor = textSilver.copy(alpha = 0.4f),
                                focusedTextColor = textLight,
                                unfocusedTextColor = textLight,
                                focusedLabelColor = accentGold,
                                unfocusedLabelColor = textSilver
                            )
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = currMedFrequency,
                            onValueChange = { currMedFrequency = it },
                            label = { Text("معدل التكرار (مثال: مرتين بيوم)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentGold,
                                unfocusedBorderColor = textSilver.copy(alpha = 0.4f),
                                focusedTextColor = textLight,
                                unfocusedTextColor = textLight,
                                focusedLabelColor = accentGold,
                                unfocusedLabelColor = textSilver
                            )
                        )
                        OutlinedTextField(
                            value = currMedTimes,
                            onValueChange = { currMedTimes = it },
                            label = { Text("الأوقات (مثال: 08:00, 20:00)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentGold,
                                unfocusedBorderColor = textSilver.copy(alpha = 0.4f),
                                focusedTextColor = textLight,
                                unfocusedTextColor = textLight,
                                focusedLabelColor = accentGold,
                                unfocusedLabelColor = textSilver
                            )
                        )
                    }

                    OutlinedTextField(
                        value = currMedMethod,
                        onValueChange = { currMedMethod = it },
                        label = { Text("طريقة الاستعمال والإلمامات") },
                        placeholder = { Text("مثال: بعد الأكل بمصف ساعة مباشرة") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentGold,
                            unfocusedBorderColor = textSilver.copy(alpha = 0.4f),
                            focusedTextColor = textLight,
                            unfocusedTextColor = textLight,
                            focusedLabelColor = accentGold,
                            unfocusedLabelColor = textSilver
                        )
                    )

                    // Shelf Selector dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = shelfNames[currMedShelf - 1],
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("اختر نوع الرف للحفظ") },
                            trailingIcon = {
                                IconButton(onClick = { rxShelfDropdownExpanded = !rxShelfDropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, null, tint = accentGold)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentGold,
                                unfocusedBorderColor = textSilver.copy(alpha = 0.4f),
                                focusedTextColor = textLight,
                                unfocusedTextColor = textLight,
                                focusedLabelColor = accentGold,
                                unfocusedLabelColor = textSilver
                            )
                        )
                        DropdownMenu(
                            expanded = rxShelfDropdownExpanded,
                            onDismissRequest = { rxShelfDropdownExpanded = false },
                            modifier = Modifier.background(themeSurface)
                        ) {
                            for (idx in 1..4) {
                                DropdownMenuItem(
                                    text = { Text(shelfNames[idx - 1], color = textLight) },
                                    onClick = {
                                        currMedShelf = idx
                                        rxShelfDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Button to append medicine to draft
                    Button(
                        onClick = {
                            if (currMedName.isBlank()) {
                                Toast.makeText(context, "الرجاء كتابة اسم الدواء أولاً", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (draftMeds.size >= 20) {
                                Toast.makeText(context, "لا يمكن إضافة أكثر من 20 دواء لملف المريض الواحد", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            draftMeds.add(
                                MedItem(
                                    name = currMedName,
                                    dosage = currMedDosage.ifBlank { "غير محدد" },
                                    duration = currMedDuration.ifBlank { "غير محدد" },
                                    frequency = currMedFrequency.ifBlank { "غير محدد" },
                                    times = currMedTimes.ifBlank { "بأي وقت" },
                                    method = currMedMethod.ifBlank { "لا توجد" },
                                    shelf = currMedShelf
                                )
                            )
                            // Clear inputs for next drug input
                            currMedName = ""
                            currMedDosage = ""
                            currMedDuration = ""
                            currMedFrequency = ""
                            currMedTimes = ""
                            currMedMethod = ""
                            Toast.makeText(context, "تم إرساء الدواء إلى رف الصيدلية بنجاح", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentGold.copy(alpha = 0.2f), contentColor = accentGold),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إرساء الدواء للرفوف المخططة 📥", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Preview of drafted medicines to save
                    if (draftMeds.isNotEmpty()) {
                        Text("الأدوية المضافة حالياً (${draftMeds.size} من 20):", color = accentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(themeSurface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            draftMeds.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${index + 1}. ${item.name} (${item.dosage}) - الرف ${item.shelf}", 
                                        color = textLight, 
                                        fontSize = 11.sp
                                    )
                                    IconButton(
                                        onClick = { draftMeds.removeAt(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = redColor, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ageInt = age.toIntOrNull()
                        if (fullName.isBlank() || ageInt == null) {
                            Toast.makeText(context, "الرجاء تعبئة الاسم الثلاثي واللقب وعمر المريض بشكل صحيح", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        // Serialize drafted medications list to JSON
                        val jsonArr = JSONArray()
                        draftMeds.forEach { m ->
                            val obj = JSONObject().apply {
                                put("name", m.name)
                                put("dosage", m.dosage)
                                put("duration", m.duration)
                                put("frequency", m.frequency)
                                put("times", m.times)
                                put("method", m.method)
                                put("shelf", m.shelf)
                            }
                            jsonArr.put(obj)
                        }

                        scope.launch {
                            db.patientDao().insertPatient(
                                Patient(
                                    fullName = fullName,
                                    age = ageInt,
                                    diagnosis = diagnosis,
                                    treatingDoctor = treatingDoctor,
                                    currentComplaint = currentComplaint,
                                    medicationsJson = jsonArr.toString(),
                                    doctorInstructions = doctorInstructions,
                                    followUpDate = followUpDate,
                                    chronicDisease = chronicDisease,
                                    allergies = allergies,
                                    bloodType = bloodType
                                )
                            )
                            Toast.makeText(context, "تم حفظ ملف المريض وصيدلية رفوفه الذكية بنجاح", Toast.LENGTH_LONG).show()
                            
                            // Reset state & Close
                            fullName = ""
                            age = ""
                            diagnosis = ""
                            treatingDoctor = ""
                            currentComplaint = ""
                            doctorInstructions = ""
                            followUpDate = ""
                            chronicDisease = ""
                            allergies = ""
                            bloodType = "O+"
                            draftMeds.clear()
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentGold)
                ) {
                    Text("إغلاق وحفظ الملف 💾", color = themeBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء", color = textSilver)
                }
            },
            containerColor = themeSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

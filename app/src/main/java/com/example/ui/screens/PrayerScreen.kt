package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.PrayerLog
import com.example.ui.theme.MidnightBlue
import com.example.ui.theme.WarmGold
import com.example.ui.theme.GreenAccent
import com.example.ui.theme.RedAccent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerScreen(
    prayerLogs: List<PrayerLog>,
    onBack: () -> Unit,
    onSaveLog: (String, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    val context = LocalContext.current
    val currentDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    
    // Auto find today's active prayer state from DB
    val todaysLog = remember(prayerLogs, currentDateStr) {
        prayerLogs.find { it.dateStr == currentDateStr } ?: PrayerLog(currentDateStr)
    }

    var fajr by remember { mutableStateOf(todaysLog.fajr) }
    var dhuhr by remember { mutableStateOf(todaysLog.dhuhr) }
    var asr by remember { mutableStateOf(todaysLog.asr) }
    var maghrib by remember { mutableStateOf(todaysLog.maghrib) }
    var isha by remember { mutableStateOf(todaysLog.isha) }
    var qiyam by remember { mutableStateOf(todaysLog.qiyam) }
    var azkar by remember { mutableStateOf(todaysLog.azkar) }

    LaunchedEffect(todaysLog) {
        fajr = todaysLog.fajr
        dhuhr = todaysLog.dhuhr
        asr = todaysLog.asr
        maghrib = todaysLog.maghrib
        isha = todaysLog.isha
        qiyam = todaysLog.qiyam
        azkar = todaysLog.azkar
    }

    var activeTab by remember { mutableStateOf(0) } // 0: اليومي (التسجيل), 1: الكشف والتقارير الأسبوعية والشهرية
    var showWordReportDialog by remember { mutableStateOf(false) }

    // Statistics compilation
    val totalLoggedDays = prayerLogs.size
    val last7DaysLogs = prayerLogs.takeLast(7)
    val last30DaysLogs = prayerLogs.takeLast(30)

    val getSuccessPercentages: (List<PrayerLog>) -> Map<String, Int> = { logs ->
        if (logs.isEmpty()) {
            mapOf("fajr" to 0, "dhuhr" to 0, "asr" to 0, "maghrib" to 0, "isha" to 0, "qiyam" to 0, "azkar" to 0, "overall" to 0)
        } else {
            val total = logs.size
            val f = (logs.count { it.fajr } * 100) / total
            val d = (logs.count { it.dhuhr } * 100) / total
            val a = (logs.count { it.asr } * 100) / total
            val m = (logs.count { it.maghrib } * 100) / total
            val i = (logs.count { it.isha } * 100) / total
            val q = (logs.count { it.qiyam } * 100) / total
            val az = (logs.count { it.azkar } * 100) / total
            
            // Average overall adherence
            val doneCount = logs.sumOf {
                listOf(it.fajr, it.dhuhr, it.asr, it.maghrib, it.isha, it.qiyam, it.azkar).count { done -> done }
            }
            val possibleCount = total * 7
            val ov = (doneCount * 100) / possibleCount
            
            mapOf("fajr" to f, "dhuhr" to d, "asr" to a, "maghrib" to m, "isha" to i, "qiyam" to q, "azkar" to az, "overall" to ov)
        }
    }

    val weeklyStats = remember(last7DaysLogs) { getSuccessPercentages(last7DaysLogs) }
    val monthlyStats = remember(last30DaysLogs) { getSuccessPercentages(last30DaysLogs) }

    // Helper to generate the styled "Word/Excel" table report text
    val generateWordReportText: () -> String = {
        val totalDays = prayerLogs.size
        val wPercent = weeklyStats["overall"] ?: 0
        val mPercent = monthlyStats["overall"] ?: 0
        
        buildString {
            append("=========================================\n")
            append("       📝 تقارير برنامج رجال الله العائلية      \n")
            append("=========================================\n")
            append("تاريخ التصدير: $currentDateStr\n")
            append("المصدر: تطبيق الاكتفاء العائلي الذكي لتنظيم المجتمع\n")
            append("-----------------------------------------\n\n")
            append("1. الإحصائيات الشاملة للملتزمين:\n")
            append("   - إجمالي الأيام الطيبة المسجلة: $totalDays يوم\n")
            append("   - معدل الالتزام الأسبوعي العام: $wPercent%\n")
            append("   - معدل الالتزام الشهري الشامل: $mPercent%\n\n")
            append("=========================================\n")
            append("       📊 كشف التزام الفروض والسنن الأسبوعي     \n")
            append("=========================================\n")
            append("  العبادة والطاعة   |  نسبة الإنجاز  |     التوصية العامة\n")
            append("-----------------------------------------\n")
            append(String.format("  صلاة الفجر في وقتها |      %2d%%      | %s\n", weeklyStats["fajr"], if ((weeklyStats["fajr"]?:0)>=80) "ممتاز جداً - ثبتكم الله" else "يحتاج حرص واستيقاظ مبكر"))
            append(String.format("  صلاة الظهر جماعة    |      %2d%%      | %s\n", weeklyStats["dhuhr"], if ((weeklyStats["dhuhr"]?:0)>=80) "محافظ مثالي على الجماعة" else "بادر للمسجد فور الأذان"))
            append(String.format("  صلاة العصر جماعة    |      %2d%%      | %s\n", weeklyStats["asr"], if ((weeklyStats["asr"]?:0)>=80) "انضباط متميز بالوسطى" else "احذر فوات صلاة العصر"))
            append(String.format("  صلاة المغرب والذكر  |      %2d%%      | %s\n", weeklyStats["maghrib"], if ((weeklyStats["maghrib"]?:0)>=80) "عناية متفوقة بذكر الله" else "اجلس بحلقات تلاوة وتدبر"))
            append(String.format("  صلاة العشاء جماعة   |      %2d%%      | %s\n", weeklyStats["isha"], if ((weeklyStats["isha"]?:0)>=80) "ختام طيب ليومك العظيم" else "احرص على أداء العشاء جماعة"))
            append(String.format("  وراتب وقيام الليل    |      %2d%%      | %s\n", weeklyStats["qiyam"], if ((weeklyStats["qiyam"]?:0)>=50) "مستوى جهادي روحاني متميز" else "حافظ على قيام كشرف وعزة"))
            append(String.format("  الأذكار والورد اليومي   |      %2d%%      | %s\n", weeklyStats["azkar"], if ((weeklyStats["azkar"]?:0)>=80) "لسانك رطب بذكر رب العباد" else "اجعل لقلبك حظاً من الأذكار"))
            append("-----------------------------------------\n\n")
            append("=========================================\n")
            append("       📈 كشف الالتزام الشهري (30 يوماً)        \n")
            append("=========================================\n")
            append(String.format("  صلاة الفجر: %2d%%   | صلاة الظهر: %2d%%   | صلاة العصر: %2d%%\n", monthlyStats["fajr"], monthlyStats["dhuhr"], monthlyStats["asr"]))
            append(String.format("  صلاة المغرب: %2d%%  | صلاة العشاء: %2d%%  | قيام الليل: %2d%%\n", monthlyStats["maghrib"], monthlyStats["isha"], monthlyStats["qiyam"]))
            append("-----------------------------------------\n")
            append("التوصيات الروحية للمجتمع الصالح:\n")
            append("- قال تعالى: 'إن الصلاة كانت على المؤمنين كتاباً موقوتاً'.\n")
            append("- البرنامج يهدف لتثبيت الرعيل وعمارة بيوت الهدي.\n")
            append("=========================================\n")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("برنامج رجال الله والصلوات", fontWeight = FontWeight.Bold, color = WarmGold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = WarmGold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        // Section Tabs Row
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = WarmGold
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("📝 المتابعة اليومية", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("📊 التقارير والكشوفات", fontWeight = FontWeight.Bold) }
            )
        }

        if (activeTab == 0) {
            // Tab 0: Daily registration view
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Computed Prayer Times Widget
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MidnightBlue),
                        border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.8f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("مواقيت الصلاة المسجلة بوزارة الأوقاف", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 13.sp)
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = WarmGold, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val times = listOf(
                                    "الفجر" to "04:32 ص",
                                    "الظهر" to "12:15 م",
                                    "العصر" to "03:41 م",
                                    "المغرب" to "06:45 م",
                                    "العشاء" to "08:12 م"
                                )
                                times.forEach { (name, time) ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(name, color = WarmGold.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        Text(time, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Checklist Section
                item {
                    Text("سجل الطاعات والمتابعة اليومية للصلوات:", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            val prayers = listOf(
                                Triple("صلاة الفجر في وقتها بالمسجد", fajr) { v: Boolean -> fajr = v },
                                Triple("صلاة الظهر جماعة صفا أول", dhuhr) { v: Boolean -> dhuhr = v },
                                Triple("صلاة العصر جماعة وانضباط", asr) { v: Boolean -> asr = v },
                                Triple("صلاة المغرب وتلاوة السور", maghrib) { v: Boolean -> maghrib = v },
                                Triple("صلاة العشاء والرواتب", isha) { v: Boolean -> isha = v },
                                Triple("قيام الليل والصلوات المسنونة", qiyam) { v: Boolean -> qiyam = v },
                                Triple("أذكار الصباح والمساء والورد اليومي", azkar) { v: Boolean -> azkar = v }
                            )

                            prayers.forEach { (label, value, onValueChange) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onValueChange(!value) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Checkbox(
                                        checked = value,
                                        onCheckedChange = { onValueChange(it ?: false) },
                                        colors = CheckboxDefaults.colors(checkedColor = WarmGold)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    onSaveLog(currentDateStr, fajr, dhuhr, asr, maghrib, isha, qiyam, azkar)
                                    Toast.makeText(context, "تم حفظ وتحديث سجل اليوم بنجاح وربطه بالتقارير!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.DoneAll, contentDescription = null, tint = MidnightBlue)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تحديث سجل العبادة وحفظه", color = MidnightBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Motivation Tip Card
                item {
                    val completedCount = listOf(fajr, dhuhr, asr, maghrib, isha, qiyam, azkar).count { it }
                    val encouragement = if (completedCount == 7) {
                        "🎉 ما شاء الله! كملت كل الطاعات والصلوات اليوم كفارس مجاهد لله!"
                    } else if (completedCount >= 5) {
                        "👍 تقدم ملحوظ وبطل حقيقي! حافظ على قيام الليل لتكمل مئة طاعة."
                    } else {
                        "⚠️ الصلاة صلتك بالحي الدائم؛ استعن بالوضوء المسبق وسارع لتكمل طاعات الغد."
                    }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, WarmGold.copy(0.4f)),
                        colors = CardDefaults.cardColors(containerColor = WarmGold.copy(0.12f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = WarmGold)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(encouragement, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 16.sp)
                        }
                    }
                }
            }
        } else {
            // Tab 1: Detailed Reports (Daily, Weekly, Monthly) & Word Export
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Word Export Callout Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MidnightBlue),
                        border = BorderStroke(1.5.dp, WarmGold)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Article, contentDescription = null, tint = WarmGold, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("تصدير الكشف الفخري والتقرير الصالح", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 14.sp)
                                    Text("قم بإنشاء كشف مصمم للمستندات والطباعة مباشرة", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), fontSize = 11.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { showWordReportDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.BackupTable, contentDescription = null, tint = MidnightBlue)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("توليد وتصدير كشف البيانات الإيمانية", color = MidnightBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Weekly stats section
                item {
                    Text("القرير الأسبوعي للالتزام بالطاعات (آخر 7 أيام):", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("المعدل الأسبوعي العام للالتزام والجاهزية:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("${weeklyStats["overall"]}%", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            LinearProgressIndicator(
                                progress = { (weeklyStats["overall"] ?: 0) / 100.2f },
                                color = WarmGold,
                                trackColor = WarmGold.copy(0.12f),
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Text("درجة الالتزام حسب كل شعبة وطاعة منفردة:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WarmGold)

                            val statsKeys = listOf(
                                "fajr" to "الفجر بالمسجد",
                                "dhuhr" to "الظهر صفا أول",
                                "asr" to "العصر جماعة",
                                "maghrib" to "المغرب وتلاوة",
                                "isha" to "العشاء والرواتب",
                                "qiyam" to "قيام الليل والوتر",
                                "azkar" to "الأذكار والورد"
                            )

                            statsKeys.forEach { (key, title) ->
                                val score = weeklyStats[key] ?: 0
                                val color = if (score >= 80) GreenAccent else if (score >= 50) WarmGold else RedAccent
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.8f))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        LinearProgressIndicator(
                                            progress = { score / 100.0f },
                                            color = color,
                                            trackColor = color.copy(0.15f),
                                            modifier = Modifier.width(80.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("$score%", color = color, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Monthly stats section
                item {
                    Text("التقرير الشامل التراكمي لآخر 30 يوماً:", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("إجمالي الصلوات والعبادات الصحيحة المنجزة:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("${monthlyStats["overall"]}%", color = GreenAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("الالتزام التراكمي للفريضة يمثل عماد المجتمع وأفراده الطيبين لخدمة أسرهم ومجتمعاتهم.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f), lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
    }

    // Word Document Design styled popup dialog
    if (showWordReportDialog) {
        val reportText = remember { generateWordReportText() }
        AlertDialog(
            onDismissRequest = { showWordReportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CardMembership, contentDescription = null, tint = WarmGold)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("📝 الكشف الفخري المصمم بمظهر Word", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 15.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("التقرير منسق بمظهر الكشوف الرسمية للطباعة والمشاركة المباشرة مع عائلتك والمجمعات:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.8f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MidnightBlue)
                            .border(BorderStroke(1.dp, WarmGold.copy(0.4f)), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            reportText,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = Color.White,
                            textAlign = TextAlign.Right,
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
                        onClick = {
                            try {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("prayer_word_report", reportText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ الكشف والتقرير إلى الحافظة!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "خطأ بالنسخ", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نسخ الكشف", color = MidnightBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = {
                            try {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, reportText)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "مشاركة التقرير عبر:"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "المشاركة غير متاحة", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مشاركة كغرس", color = MidnightBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showWordReportDialog = false }) {
                    Text("إغلاق", color = WarmGold, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

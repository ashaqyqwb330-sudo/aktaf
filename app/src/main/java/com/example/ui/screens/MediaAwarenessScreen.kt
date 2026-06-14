package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class UsageLog(
    val id: Long,
    val date: String,
    val platformName: String,
    val durationMinutes: Int,
    val intention: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaAwarenessScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var inputMinutes by remember { mutableStateOf("") }
    var inputPlatform by remember { mutableStateOf("واتساب") }
    var inputIntention by remember { mutableStateOf("تواصل هادف ونشر توعية") }

    val platformOptions = listOf("واتساب", "تيليجرام", "تويتر / X", "فيسبوك", "يوتيوب", "أخرى")
    val intentionOptions = listOf("تواصل هادف ونشر توعية", "متابعة الأخبار والمظلومية", "دراسة وبحث علمي", "تسلية ترويحية", "تصفح عام")

    val usageLogs = remember {
        mutableStateListOf(
            UsageLog(1, "اليوم", "تيليجرام", 45, "نشر خطابات وتوجيهات"),
            UsageLog(2, "اليوم", "واتساب", 30, "تواصل عائلي صلة رحم"),
            UsageLog(3, "أمس", "يوتيوب", 60, "مشاهدة برنامج وثائقي")
        )
    }

    val totalMinutes = remember(usageLogs) {
        derivedStateOf { usageLogs.sumOf { it.durationMinutes } }
    }

    Column(modifier = Modifier.fillMaxSize().background(MidnightBlue)) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceWarm)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "الرجوع",
                    tint = WarmGold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    "📡 رادار الوعي الإعلامي والرقمي",
                    color = TextLight,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "تحصين الوعي العائلي ضد الحرب الاستباقية",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                // Warning Alert Box
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, RedAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = RedAccent.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = "تحذير مهم", tint = RedAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "⚠️ تحذير وعي إجباري قبل فتح وسائل التواصل",
                                color = RedAccent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "قبل تصفح أي شبكة اجتماعية، ذكّر نفسك بـ الأركان الثلاثة:\n" +
                                "1. استشعار الرقابة الإلهية (مَا يَلْفِظُ مِن قَوْلٍ إِلَّا لَدَيْهِ رَقِيبٌ عَتِيدٌ)\n" +
                                "2. رفض الإشاعات وعدم المساهمة في الهوس المعنوي لزعزعة جبهة شعبنا.\n" +
                                "3. مقاطعة الحسابات الصهيونية والصفحات المثبطة للهمم بشكل واعٍ وصارم.\n" +
                                "4. التحكم بالوقت وصيانة العقل من توافه المأكولات والمناظر المبتذلة.",
                            color = TextLight,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Interactive Tracker Form
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = "عداد وقت", tint = WarmGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "⏱️ تسجيل زمن الاستخدام الهادف للأسرة",
                                color = WarmGold,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Platforms selector
                        Text("المنصة الإيرادية والاجتماعية:", color = TextLight, fontSize = 12.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            platformOptions.take(4).forEach { plat ->
                                val sel = inputPlatform == plat
                                Button(
                                    onClick = { inputPlatform = plat },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (sel) WarmGold else SurfaceDark
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(plat, fontSize = 10.sp, color = if (sel) MidnightBlue else TextLight)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Duration Input
                        OutlinedTextField(
                            value = inputMinutes,
                            onValueChange = { inputMinutes = it },
                            label = { Text("مدة التصفح (بالدقائق)", color = TextMuted) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedBorderColor = WarmGold,
                                unfocusedBorderColor = SurfaceDark,
                                focusedLabelColor = WarmGold
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Purpose / Intention dropdown selection (represented by custom simple row buttons)
                        Text("غاية الاستخدام واستشعار الهدف الإيماني:", color = TextLight, fontSize = 12.sp)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            intentionOptions.take(3).forEach { option ->
                                Card(
                                    onClick = { inputIntention = option },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (inputIntention == option) WarmGold.copy(0.15f) else SurfaceDark
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp,
                                            if (inputIntention == option) WarmGold else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    Text(
                                        text = option,
                                        modifier = Modifier.padding(8.dp),
                                        color = if (inputIntention == option) WarmGold else TextLight,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val min = inputMinutes.toIntOrNull() ?: 0
                                if (min <= 0) {
                                    Toast.makeText(context, "الرجاء إدخال دقائق صحيحة", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                usageLogs.add(
                                    0,
                                    UsageLog(
                                        id = System.currentTimeMillis(),
                                        date = "اليوم",
                                        platformName = inputPlatform,
                                        durationMinutes = min,
                                        intention = inputIntention
                                    )
                                )
                                inputMinutes = ""
                                Toast.makeText(context, "تم قيد زمن التصفح تحت رقابة الوعي", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("قيد الزمن تحت الرقابة الواعية", color = MidnightBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Summary metrics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("إجمالي زمن اليوم", color = TextMuted, fontSize = 11.sp)
                            Text("${totalMinutes.value} دقيقة", color = WarmGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("مؤشر الاستخدام الرقمي", color = TextMuted, fontSize = 11.sp)
                            val evaluation = when {
                                totalMinutes.value > 120 -> "تنبيه غرق رقمي!"
                                totalMinutes.value > 60 -> "استهلاك متوسط"
                                else -> "توازن واعتدال ممتاز"
                            }
                            Text(evaluation, color = if (totalMinutes.value > 120) RedAccent else GreenAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // List of recorded media usage sessions
            item {
                Text(
                    text = "سجل التصفح الأسري المرصود للوعي:",
                    color = TextLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(usageLogs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(WarmGold.copy(0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = log.platformName.take(2),
                                color = WarmGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(log.platformName, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${log.durationMinutes} دقيقة", color = WarmGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("النية: ${log.intention}", color = TextMuted, fontSize = 11.sp)
                            Text("التاريخ: ${log.date}", color = TextMuted, fontSize = 9.sp)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

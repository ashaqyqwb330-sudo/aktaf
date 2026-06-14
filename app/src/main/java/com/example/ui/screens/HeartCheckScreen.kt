package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.EktefaaDatabase
import com.example.database.HeartCheckEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class HeartCheckQuestion(val id: Int, val question: String, val category: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartCheckScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { EktefaaDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    val questions = remember {
        listOf(
            HeartCheckQuestion(1, "هل شعرت اليوم بشك في أمر عادل أو أخلاقي؟", "البصيرة القلبية"),
            HeartCheckQuestion(2, "هل تكاسلت عن تلاوة وردك القرآني أو شعرت بفتور في قلبك؟", "لين وتأثر القلب"),
            HeartCheckQuestion(3, "هل وجدت راحة نفسية وعميقة عند الصلاة أو تلاوة القرآن اليوم؟", "طمأنينة الإيمان"),
            HeartCheckQuestion(4, "هل دافعت أو جادلت بالباطل لتبرير خطأ شخصي أو سلوكي؟", "سلامة المقصد"),
            HeartCheckQuestion(5, "هل تسرب إلى قلبك رياء أو مَنّ على الله والناس بالتزامك الفاضل؟", "إخلاص النية لله"),
            HeartCheckQuestion(6, "هل انقدت بسهولة وراء شائعة أو تحليل تضليلي على وسائل التواصل؟", "اليقظة الفكرية والوعي"),
            HeartCheckQuestion(7, "هل جعلت القرآن حكماً ومقياساً لك في مواقفك وعلاقاتك اليوم؟", "المقياس القرآني في العمل"),
            HeartCheckQuestion(8, "هل شعرت أن وتيرة يقينك بالله وثقتك بوعده في النصر راسخة ومتينة؟", "قوة اليقين والثبات"),
            HeartCheckQuestion(9, "هل واجهت أي شبهة فكرية أو تشتت، وبادرت بالاعتصام بالحق؟", "التحصين والرسوخ العقائدي"),
            HeartCheckQuestion(10, "هل داهنك الخوف من كلام الآخرين أو ملامتهم أكثر من خشية الله؟", "الخشية والحياء من الله"),
            HeartCheckQuestion(11, "هل التزمت بتسبيحاتك ومناجاتك بحضور قلب أم بلسان لاهٍ؟", "الذكر التدبري الفاعل"),
            HeartCheckQuestion(12, "هل حاسبت نفسك بنهاية يومك وجددت التوبة النصوح لله رب العالمين؟", "الاستعداد والتوبة المستمرة")
        )
    }

    val answers = remember { mutableStateMapOf<Int, Boolean>() }
    
    // Initialize answers to false
    LaunchedEffect(Unit) {
        questions.forEach { q ->
            if (answers[q.id] == null) answers[q.id] = false
        }
    }

    var resultText by remember { mutableStateOf("") }
    var resultCategory by remember { mutableStateOf("") }
    var showResult by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(MidnightBlue)) {
        // App Bar
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
                    "🫀 فحص سلامة القلب اليومي",
                    color = TextLight,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "التشخيص الإيماني: $todayDateStr",
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Favorite, contentDescription = "Heart", tint = RedAccent, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "تزكية وفحص نبضات القلب",
                                color = WarmGold,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "القلب هو موطن الهداية والتقوى، تفقده يومك لتصفية الشوائب والتخلص من الران قبل تراكمه. أجب بصدق بنعم أو لا.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Justify
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            itemsIndexed(questions) { index, question ->
                val isChecked = answers[question.id] ?: false
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (isChecked) WarmGold.copy(alpha = 0.5f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${index + 1}. ${question.question}",
                                color = TextLight,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Justify
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "محور القياس: ${question.category}",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Switch(
                            checked = isChecked,
                            onCheckedChange = { answers[question.id] = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = WarmGold,
                                checkedTrackColor = WarmGold.copy(alpha = 0.4f),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceWarm
                            )
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val positiveCount = answers.values.count { it }
                        
                        // Diagnose & determine spiritual counseling
                        val advice = when {
                            positiveCount >= 10 -> {
                                resultCategory = "🟢 القلب المطمئن المنيب الراسخ"
                                "ما شاء الله! قلبك يعيش حالة طمأنينة عالية ويقظة تامة. استمر بتلاوة سورة الإخلاص وآية الكرسي دبراً لكل صلاة صيانةً للروح."
                            }
                            positiveCount >= 7 -> {
                                resultCategory = "🟡 القلب السليم الساعي للرشاد"
                                "إيمانك طيب وقلبك منبه. للتخلص من الفتور الحاصل، ننصحك بالمواظبة على الاستغفار 100 مرة صباحاً ومساءً وقراءة أوراد سورة يس والواقعة."
                            }
                            positiveCount >= 4 -> {
                                resultCategory = "🟠 القلب الفاتر المجهد"
                                "قلبك يعاني من هجمات الصوارف الدنيوية وتراكم الفتور والران. بادر بسماع هدى الله بنشاط، وتلاوة سورة السجدة، مع المداومة التامة على تلاوة سورة الملك قبل النوم."
                            }
                            else -> {
                                resultCategory = "🔴 القلب القاسي أو الخامل"
                                "تحذير: قلبك محاط بظلمات الغفلة وفتور الهمة الإيمانية الشديد. ننصحك فوراً بالتوبة والاسترجاع والنهوض لصلاة ركعتين خاشعتين لسبيل الخلاص."
                            }
                        }

                        resultText = advice
                        showResult = true

                        scope.launch {
                            db.rajolAllahDao().insertHeartCheck(
                                HeartCheckEntity(
                                    dateStr = todayDateStr,
                                    positiveCount = positiveCount,
                                    resultText = "$resultCategory: $advice"
                                )
                            )
                            Toast.makeText(context, "تم حفظ نتيجة تشخيص القلب بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "🔍 بدء فحص وتجلي القلب الإيماني",
                        color = MidnightBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (showResult) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, WarmGold.copy(0.4f), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "🩺 النتيجة والتشخيص الروحي",
                                color = WarmGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = resultCategory,
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = resultText,
                                color = TextMuted,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Justify,
                                lineHeight = 19.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }
            } else {
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

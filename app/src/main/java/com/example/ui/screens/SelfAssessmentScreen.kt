package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
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
import com.example.database.RajolAllahAssessmentEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class AssessmentItem(val id: Int, val title: String, val description: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfAssessmentScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { EktefaaDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    val weekStartDate = remember {
        val cal = Calendar.getInstance()
        // Get the starting day (Saturday in Arab cultures)
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    val assessmentItems = remember {
        listOf(
            AssessmentItem(1, "الإيمان الصادق بالله", "الثقة بالله والإيمان الصادق به والتوكل عليه في الشدائد والمواقف"),
            AssessmentItem(2, "تزكية النفس", "الاهتمام الدائم بتزكية النفس والارتقاء الأخلاقي وتطهير السريرة والعلانية"),
            AssessmentItem(3, "الثقافة القرآنية", "الاهتمام والعناية بهدى الله والاستمساك بالمنهج والوعي القرآني الشامل"),
            AssessmentItem(4, "العلم النافع", "الارتقاء المستمر بتحصيل العلم النافع والعمل بمقتضى الحكمة في التصرف"),
            AssessmentItem(5, "فهم التاريخ والواقع", "فهم القضايا التاريخية والسياسية المعاصرة بروح البصيرة والتحليل الواعي"),
            AssessmentItem(6, "نشر هدى الله والوعي", "العناية بنشر القيم الإيمانية ومكافحة الانحراف والباطل كمسؤولية جماعية"),
            AssessmentItem(7, "الالتزام بالقيادة واستشعار المسؤولية", "سرعة التلبية والتنفيذ العملي للتوجيهات بحرص وثبات"),
            AssessmentItem(8, "الأداء والاستقامة في العمل", "السعي الدائم للارتقاء في الأداء العملي العام وتجويد المخرجات"),
            AssessmentItem(9, "التقوى في السلوك اليومي", "مخافة الله عز وجل في السر والعلن واجتناب الشبهات والكبائر"),
            AssessmentItem(10, "تقوى الإنفاق وتجنب الشح", "تقوى الله في الإنفاق والمشاركة بالمال والجهد لسبيل الله دون منّ"),
            AssessmentItem(11, "الأخلاق الإسلامية والآداب", "الالتزام بالأمانة والوفاء بالعهود وحسن المعاملة وخفض الجناح"),
            AssessmentItem(12, "الروحية الجهادية والاستعداد", "حمل الروحية الجهادية العالية والاستعداد العالي للتضحية والعطاء"),
            AssessmentItem(13, "فهم مخططات الأعداء ومؤامراتهم", "الوعي المستمر بمكائد الأعداء واليقظة لأحابيل التضليل العقائدي والمادي"),
            AssessmentItem(14, "الوعي السياسي والتحليلي", "التحليل السياسي الرصين ومتابعة تطورات الأحداث بالبصيرة القرآنية"),
            AssessmentItem(15, "الاهتمام بخطابات وتوجيهات القيادة", "الاستماع الواعي بانتظام للخطابات وتطبيق دروس المنهج في الحياة"),
            AssessmentItem(16, "مقاطعة إعلام العدو والتضليل", "المقاطعة المنهجية الكاملة للمنصات المشبوهة ووسائل الإشاع والمواقع الهابطة"),
            AssessmentItem(17, "الورع وحفظ الحقوق والامتداد", "الصيانة الكاملة لأموال الأوقاف، حقوق الضعفاء وممتلكات بيت المال"),
            AssessmentItem(18, "الحذر من الإسراف والتبذير", "التزام التوازن والاعتدال والابتعاد عن التفاخر الاستهلاكي المذموم"),
            AssessmentItem(19, "حسن إدارة وتوزيع الإمكانات", "عدم تضييع الإمكانات أو تسليم مسؤوليات حساسة لغير الأكفاء"),
            AssessmentItem(20, "الحذر من حياة الترف ومظاهر البذخ", "المحافظة على العيش النبيل غير المترف واستشعار معاناة المستضعفين"),
            AssessmentItem(21, "الاهتمام بعباد الله وتلمس الاحتياجات", "بذل المعروف والمساعدة والإصلاح الاجتماعي وحل النزاعات بين الأفراد"),
            AssessmentItem(22, "الصلاة المكتوبة بوقتها وتدبر", "إقامة الصلوات الخمس بخشوع وحضور قلب واستشعار عظمة المعبود"),
            AssessmentItem(23, "الأذكار والاستغفار الموظف", "المواظبة الدورية على الأذكار الصباحية والمسائية والتعقيب وتلاوة الأدعية"),
            AssessmentItem(24, "تلاوة القرآن وتدبر آياته", "الارتباط اليومي الروحي للمصحف وتخصيص ورد للتلاوة والحفظ الفاعل"),
            AssessmentItem(25, "بر الوالدين وصلة الأرحام", "الطاعة والرفق بالوالدين والسؤال الدائم عن ذوي القربى والإحسان إليهم"),
            AssessmentItem(26, "الصدق في القول والعمل", "ملازمة الصدق ومطابقة الظاهر للباطن وتجنب الكذب تماماً"),
            AssessmentItem(27, "الأمانة وحفظ الأسرار", "أداء الأمانات لأهلها والصيانة الكاملة للمجالس والخصوصيات العائلية"),
            AssessmentItem(28, "الصبر العملي والثبات بوجه الطغيان", "الثبات النفسي والميداني وتطوير أدوات المواجهة والمقاومة"),
            AssessmentItem(29, "الاستجابة الفردية وعدم الانتظار", "المبادرة بالتحرك في أعمال الخير فوراً ودون الالتفات لتقاعس الآخرين"),
            AssessmentItem(30, "تذكر الموت وحقيقة الآخرة", "عيش الاستعداد الدائم للقاء الله وزيادة رصيد التقوى لليوم الوعيد"),
            AssessmentItem(31, "رفض الفساد والمقاطعة المجتمعية المنظمة", "الإنكار العملي للمنكر المفسد ومقاطعة منتجات الأعداء وحماتهم بقوة")
        )
    }

    val scores = remember { mutableStateMapOf<Int, Int>() }
    
    // Load existing assessment for current week if exists
    LaunchedEffect(weekStartDate) {
        scope.launch {
            val existing = db.rajolAllahDao().getAssessmentForWeek(userId = 1, weekStartDate = weekStartDate)
            if (existing != null) {
                scores[1] = existing.faithAndTrust
                scores[2] = existing.selfPurification
                scores[3] = existing.quranicCulture
                scores[4] = existing.beneficialKnowledge
                scores[5] = existing.historicalUnderstanding
                scores[6] = existing.spreadingGuidance
                scores[7] = existing.followingLeadership
                scores[8] = existing.practicalPerformance
                scores[9] = existing.pietyInAction
                scores[10] = existing.pietyInSpending
                scores[11] = existing.islamicEthics
                scores[12] = existing.jihadSpirit
                scores[13] = existing.understandingEnemies
                scores[14] = existing.politicalAwareness
                scores[15] = existing.leadershipSpeeches
                scores[16] = existing.mediaBoycott
                scores[17] = existing.trustworthiness
                scores[18] = existing.avoidWaste
                scores[19] = existing.carefulWithResources
                scores[20] = existing.avoidLuxury
                scores[21] = existing.caringForPeople
                scores[22] = existing.prayerCommitment
                scores[23] = existing.dhikrCommitment
                scores[24] = existing.quranCommitment
                scores[25] = existing.parentalRespect
                scores[26] = existing.honestyValue
                scores[27] = existing.trustValue
                scores[28] = existing.patienceInStruggle
                scores[29] = existing.individualResponse
                scores[30] = existing.deathRemembrance
            } else {
                assessmentItems.forEach { scores[it.id] = 0 }
            }
        }
    }

    // Average total progress
    val currentAverage = remember {
        derivedStateOf {
            val answered = scores.values.filter { it > 0 }
            if (answered.isEmpty()) 0f else (answered.sum().toFloat() / (assessmentItems.size * 5)) * 100f
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MidnightBlue)) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceWarm)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Star,
                    contentDescription = "الرجوع",
                    tint = WarmGold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "📋 التقييم الذاتي التفصيلي",
                    color = TextLight,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "أسبوع يبدأ من: $weekStartDate",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
            
            // Score circle indicator
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(27.dp))
                    .background(WarmGold.copy(alpha = 0.2f))
                    .border(1.dp, WarmGold, RoundedCornerShape(27.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${currentAverage.value.toInt()}%",
                    color = WarmGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Checklist body
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
                        Text(
                            "ميثاق رجال الله للارتقاء الذاتي",
                            color = WarmGold,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "هذا التقييم أداة عملية شخصية بينك وبين الله عز وجل. أجب بكل وضوح وصدق لتتعرف على نقاط ضعفك وتعمل على تزقيتها.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Justify
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            itemsIndexed(assessmentItems) { index, item ->
                val currentScore = scores[item.id] ?: 0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (currentScore > 0) WarmGold.copy(alpha = 0.4f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}. ${item.title}",
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (currentScore > 0) {
                                Text(
                                    text = "$currentScore / 5",
                                    color = if (currentScore >= 4) GreenAccent else if (currentScore >= 3) WarmGold else RedAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.description,
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Justify
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Score options 1 to 5
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val optionLabels = listOf("ضعيف", "مقبول", "جيد", "ممتاز", "قدوة")
                            for (i in 1..5) {
                                val isSelected = currentScore == i
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    IconButton(
                                        onClick = { scores[item.id] = i },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(
                                                if (isSelected) {
                                                    if (i >= 4) GreenAccent.copy(alpha = 0.25f)
                                                    else if (i >= 3) WarmGold.copy(alpha = 0.25f)
                                                    else RedAccent.copy(alpha = 0.25f)
                                                } else Color.Transparent
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) {
                                                    if (i >= 4) GreenAccent
                                                    else if (i >= 3) WarmGold
                                                    else RedAccent
                                                } else TextMuted.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(18.dp)
                                            )
                                    ) {
                                        Text(
                                            text = "$i",
                                            color = if (isSelected) {
                                                if (i >= 4) GreenAccent
                                                else if (i >= 3) WarmGold
                                                else RedAccent
                                            } else TextMuted,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = optionLabels[i - 1],
                                        color = if (isSelected) TextLight else TextMuted,
                                        fontSize = 8.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        scope.launch {
                            val entity = RajolAllahAssessmentEntity(
                                userId = 1,
                                weekStartDate = weekStartDate,
                                faithAndTrust = scores[1] ?: 0,
                                selfPurification = scores[2] ?: 0,
                                quranicCulture = scores[3] ?: 0,
                                beneficialKnowledge = scores[4] ?: 0,
                                historicalUnderstanding = scores[5] ?: 0,
                                spreadingGuidance = scores[6] ?: 0,
                                followingLeadership = scores[7] ?: 0,
                                practicalPerformance = scores[8] ?: 0,
                                pietyInAction = scores[9] ?: 0,
                                pietyInSpending = scores[10] ?: 0,
                                islamicEthics = scores[11] ?: 0,
                                jihadSpirit = scores[12] ?: 0,
                                understandingEnemies = scores[13] ?: 0,
                                politicalAwareness = scores[14] ?: 0,
                                leadershipSpeeches = scores[15] ?: 0,
                                mediaBoycott = scores[16] ?: 0,
                                trustworthiness = scores[17] ?: 0,
                                avoidWaste = scores[18] ?: 0,
                                carefulWithResources = scores[19] ?: 0,
                                avoidLuxury = scores[20] ?: 0,
                                caringForPeople = scores[21] ?: 0,
                                prayerCommitment = scores[22] ?: 0,
                                dhikrCommitment = scores[23] ?: 0,
                                quranCommitment = scores[24] ?: 0,
                                parentalRespect = scores[25] ?: 0,
                                honestyValue = scores[26] ?: 0,
                                trustValue = scores[27] ?: 0,
                                patienceInStruggle = scores[28] ?: 0,
                                individualResponse = scores[29] ?: 0,
                                deathRemembrance = scores[30] ?: 0,
                                notes = "تقييم أسبوع $weekStartDate"
                            )
                            db.rajolAllahDao().insertAssessment(entity)
                            Toast.makeText(context, "تم حفظ سجل التقويم الأسبوعي بنجاح", Toast.LENGTH_LONG).show()
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "💾 حفظ التقييم الأسبوعي والتمكين",
                        color = MidnightBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

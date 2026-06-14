package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.EktefaaDatabase
import com.example.database.HeartCheckEntity
import com.example.database.RajolAllahAssessmentEntity
import com.example.database.ScenarioResultEntity
import com.example.database.WeeklyChallengeEntity
import com.example.ui.theme.*
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RajolAllahReportsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { EktefaaDatabase.getDatabase(context) }

    var assessmentsList by remember { mutableStateOf(emptyList<RajolAllahAssessmentEntity>()) }
    var heartChecksList by remember { mutableStateOf(emptyList<HeartCheckEntity>()) }
    var challengesList by remember { mutableStateOf(emptyList<WeeklyChallengeEntity>()) }
    var scenariosList by remember { mutableStateOf(emptyList<ScenarioResultEntity>()) }

    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            assessmentsList = db.rajolAllahDao().getAssessmentsForUser(1).first()
            heartChecksList = db.rajolAllahDao().getAllHeartChecksFlow().first()
            challengesList = db.rajolAllahDao().getAllChallengesFlow().first()
            scenariosList = db.rajolAllahDao().getAllScenarioResultsFlow().first()
        } catch (e: Exception) {
            // Fallback empty list or handle gracefully
        } finally {
            isLoading = false
        }
    }

    val latestAssessment = assessmentsList.firstOrNull()
    val challengeCompletionRatio = remember(challengesList) {
        derivedStateOf {
            if (challengesList.isEmpty()) 0f
            else (challengesList.count { it.isCompleted }.toFloat() / challengesList.size) * 100f
        }
    }
    val averageAssessmentScore = remember(latestAssessment) {
        derivedStateOf {
            if (latestAssessment == null) 0
            else {
                val sum = listOf(
                    latestAssessment.faithAndTrust, latestAssessment.selfPurification,
                    latestAssessment.quranicCulture, latestAssessment.beneficialKnowledge,
                    latestAssessment.historicalUnderstanding, latestAssessment.spreadingGuidance,
                    latestAssessment.followingLeadership, latestAssessment.practicalPerformance,
                    latestAssessment.pietyInAction, latestAssessment.pietyInSpending,
                    latestAssessment.islamicEthics, latestAssessment.jihadSpirit,
                    latestAssessment.understandingEnemies, latestAssessment.politicalAwareness,
                    latestAssessment.leadershipSpeeches, latestAssessment.mediaBoycott,
                    latestAssessment.trustworthiness, latestAssessment.avoidWaste,
                    latestAssessment.carefulWithResources, latestAssessment.avoidLuxury,
                    latestAssessment.caringForPeople, latestAssessment.prayerCommitment,
                    latestAssessment.dhikrCommitment, latestAssessment.quranCommitment,
                    latestAssessment.parentalRespect, latestAssessment.honestyValue,
                    latestAssessment.trustValue, latestAssessment.patienceInStruggle,
                    latestAssessment.individualResponse, latestAssessment.deathRemembrance
                ).sum()
                ((sum.toFloat() / (30 * 5)) * 100).toInt()
            }
        }
    }

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
                    "📊 تقارير ميثاق الوعي الإيماني الدورية",
                    color = TextLight,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "تحليلات التقويم الذاتي وصحة الفؤاد العائلية",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WarmGold)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "🎯 لوحة قياس الأثر والارتقاء العام للوالد",
                                color = WarmGold,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Metric Card 1
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceWarm)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("التزام المعايير", color = TextMuted, fontSize = 11.sp)
                                        Text("${averageAssessmentScore.value}%", color = WarmGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                        Text(if (latestAssessment != null) "أحدث تقويم أسبوعي" else "لا توجد بيانات غامرة", color = TextMuted, fontSize = 9.sp)
                                    }
                                }

                                // Metric Card 2
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceWarm)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("إنجاز التحديات", color = TextMuted, fontSize = 11.sp)
                                        Text("${challengeCompletionRatio.value.toInt()}%", color = GreenAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                        Text("${challengesList.count { it.isCompleted }} من أصل ${challengesList.size}", color = TextMuted, fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Brief custom bar graph representation using simple Compose boxes
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BarChart, contentDescription = "رسم بياني", tint = WarmGold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تطور مستوى الوعي والالتزام للأربع أسابيع المنصرمة", color = WarmGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (assessmentsList.isEmpty()) {
                                Text("لا توجد بيانات كافية لرسم المنحدر الإيماني حالياً. ابدأ أولاً بملء التقييم الذاتي الأسبوعي.", color = TextMuted, fontSize = 11.sp)
                            } else {
                                val displayWeeks = assessmentsList.take(4).reversed()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    displayWeeks.forEachIndexed { idx, ass ->
                                        val sum = listOf(
                                            ass.faithAndTrust, ass.selfPurification,
                                            ass.quranicCulture, ass.beneficialKnowledge,
                                            ass.historicalUnderstanding, ass.spreadingGuidance,
                                            ass.followingLeadership, ass.practicalPerformance
                                        ).sum()
                                        // Max sum = 40
                                        val normHeight = ((sum.toFloat() / 40f) * 100).coerceIn(10f, 100f)
                                        
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Bottom,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(text = "${(normHeight).toInt()}%", color = WarmGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .width(28.dp)
                                                    .height(normHeight.dp)
                                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                    .background(if (normHeight >= 70) GreenAccent else WarmGold)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = "الأسبوع ${idx + 1}", color = TextMuted, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Immersive historical heart check diagnose log
                item {
                    Text(
                        text = "🩺 الأرشيف التشخيصي لسلامة القلب:",
                        color = TextLight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                if (heartChecksList.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                        ) {
                            Text(
                                "لم تسجل أي فحص معنوي فؤادي لليوم. قم بقيد تشخيصك بتبويب فحص القلب.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                } else {
                    items(heartChecksList.take(6)) { check ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = "نبض",
                                    tint = if (check.positiveCount >= 8) GreenAccent else if (check.positiveCount >= 4) WarmGold else RedAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("تشخيص النبضات الإيمانية", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(check.dateStr, color = TextMuted, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(check.resultText, color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp)
                                }
                            }
                        }
                    }
                }

                // Simulation responses completed
                item {
                    Text(
                        text = "🎭 نتائج مواقف المحاكاة واتخاذ القرار:",
                        color = TextLight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                if (scenariosList.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                        ) {
                            Text(
                                "لا توجد نتائج مسجلة للمحاكاة بعد. زُر تبويب محاكاة الأزمات والمواقف الصعبة.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                } else {
                    items(scenariosList) { scRes ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (scRes.isCorrect) Icons.Default.CheckCircle else Icons.Default.History,
                                    contentDescription = "Status",
                                    tint = if (scRes.isCorrect) GreenAccent else RedAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(scRes.scenarioTitle, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("الخيار المعتمد: ${scRes.chosenOption}", color = TextMuted, fontSize = 11.sp)
                                    Text(
                                        text = if (scRes.isCorrect) "صائب بصير" else "يحتاج تفرس وروية عقائدية",
                                        color = if (scRes.isCorrect) GreenAccent else RedAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
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
}

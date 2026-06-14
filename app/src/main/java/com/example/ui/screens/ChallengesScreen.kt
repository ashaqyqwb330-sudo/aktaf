package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.database.WeeklyChallengeEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class LocalChallenge(val title: String, val description: String, val icon: String, val pointValue: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { EktefaaDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    val challenges = remember {
        listOf(
            LocalChallenge("تحدي الصبر والمثابرة العملية", "تجنب التبرم أو التذمر طوال الأسبوع من أي متاعب أو أعمال شاقة، وحوّلها إلى احتساب صامد للأجر الإلهي.", "🛡️", 40),
            LocalChallenge("تحدي الصدق ومطابقة القول الفعل", "تجنب المبالغات أو الكلمات المرسلة التي لا تنوي أو لا تقدر على فعلها. الصدق ثوانٍ وأفعال.", "🤝", 50),
            LocalChallenge("تحدي الحواريين (المبادرة الذاتية)", "بادر للقيام بمسؤولية عامة أو خدمة عائلية من تلقاء نفسك دون انتظار توجيه أو طلب من أحد.", "⚔️", 45),
            LocalChallenge("تحدي الإخلاص ولجام المَنّ", "افعل خمسة أعمال إحسان لأشخاص أو للمجتمع، واكتمها تماماً عن المحيطين حيطةً من تسلل العجب بالنفس.", "🤲", 60),
            LocalChallenge("تحدي الوعي والمقاطعة المنظمة", "مقاطعة شاملة واعية ومقنعة لكل منتج يدعم الأعداء ونشر دليل المقاطعة لثلاثة أفراد على الأقل.", "📣", 35),
            LocalChallenge("تحدي صلاة قيام الليل والخشوع", "المداومة التامة على ركعات قيام السحر والدعاء للضعفاء والمجاهدين طيلة ليالي الأسبوع بنية التزكية.", "🕌", 50),
            LocalChallenge("تحدي الابتكار والبناء الفكري", "ابتكر فكرة عملية في ركن الصيانة أو ميزانية التوفير في منزل العائلة توفر الجهد والمال.", "💡", 40),
            LocalChallenge("تحدي عفة اللسان وحفظ المجالس", "اجتناب الغيبة، النميمة، واللغو تماماً في المجالس واستبدالها بنقاش واعد وتوعوي.", "🤐", 35),
            LocalChallenge("تحدي الرعاية والعطف الإنساني", "تلمس حاجة مريض عائلي أو مسكين ودعمه بالدواء، الأنسجة والاهتمام طوال الأسبوع.", "❤️", 45),
            LocalChallenge("تحدي الالتزام اليومي للورد", "إتمام قراءات ورد القرآن والتسبيح اليومي مع تدبر وتدوين 3 استنباطات في الحافظة.", "📿", 30)
        )
    }

    // Load active logs
    val databaseLogs = remember { mutableStateListOf<WeeklyChallengeEntity>() }
    
    val reloadLogs: () -> Unit = {
        scope.launch {
            db.rajolAllahDao().getAllChallengesFlow().collect { list ->
                databaseLogs.clear()
                databaseLogs.addAll(list)
            }
        }
    }

    LaunchedEffect(Unit) {
        reloadLogs()
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
                    "🎯 التحديات الأسبوعية لرجال الله",
                    color = TextLight,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "شحذ الهمم وتفعيل المسؤولية الإيمانية",
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
                // Info Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "ميدان البناء والترويض العملي للجهاد الأكبر",
                            color = WarmGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "التحديات هي ترجمة واقعية للقيم لكي لا تبقى مجرد شعارات. اختر التحدي المناسب لواقعك هذا الأسبوع والتزم بإنجازه بنجاح.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Justify
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Challenge Cards
            itemsIndexed(challenges) { index, chal ->
                val matchingDbRecord = databaseLogs.find { it.title == chal.title }
                val isAccepted = matchingDbRecord != null
                val isCompleted = matchingDbRecord?.isCompleted == true

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (isCompleted) GreenAccent.copy(0.4f) else if (isAccepted) WarmGold.copy(0.4f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCompleted) SurfaceDark.copy(0.4f) else SurfaceDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isCompleted) GreenAccent.copy(0.12f)
                                        else if (isAccepted) WarmGold.copy(0.12f)
                                        else SurfaceWarm
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(chal.icon, fontSize = 22.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = chal.title,
                                    color = if (isCompleted) GreenAccent else TextLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "مستوى التحدي الإيماني: +${chal.pointValue} نقطة وعي",
                                    color = WarmGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = chal.description,
                            color = TextMuted,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            textAlign = TextAlign.Justify
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (!isAccepted) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            db.rajolAllahDao().insertChallenge(
                                                WeeklyChallengeEntity(
                                                    title = chal.title,
                                                    description = chal.description,
                                                    startDate = todayStr,
                                                    isCompleted = false
                                                )
                                            )
                                            Toast.makeText(context, "تم قبول غمار التحدي الإيماني بنجاح!", Toast.LENGTH_SHORT).show()
                                            reloadLogs()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("🗡️ قبول غمار التحدي", color = MidnightBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                if (!isCompleted) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                scope.launch {
                                                    db.rajolAllahDao().updateChallengeStatus(
                                                        id = matchingDbRecord.id,
                                                        completed = true,
                                                        completedAt = System.currentTimeMillis()
                                                    )
                                                    Toast.makeText(context, "🎉 مبارك! تم إنجاز التحدي والارتقاء بنجاح!", Toast.LENGTH_SHORT).show()
                                                    reloadLogs()
                                                }
                                            },
                                            border = BorderStroke(1.dp, GreenAccent),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenAccent),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Text("✔️ تم الإنجاز بنجاح", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        
                                        IconButton(onClick = {
                                            scope.launch {
                                                db.rajolAllahDao().updateChallengeStatus(
                                                    id = matchingDbRecord.id,
                                                    completed = false,
                                                    completedAt = 0
                                                )
                                                // Just delete it to allow re-applying
                                                // (Since there is no specific delete query but we can delete from Dao or wait)
                                                // Let's reload logs
                                                reloadLogs()
                                            }
                                        }) {
                                            Icon(Icons.Default.Star, contentDescription = "Active", tint = WarmGold)
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = {},
                                        colors = ButtonDefaults.buttonColors(containerColor = GreenAccent.copy(0.12f)),
                                        enabled = false,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("🏆 مكتمل وموثق للتزكية", color = GreenAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
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

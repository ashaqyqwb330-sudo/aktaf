package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.*

data class DivineDoor(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val note: String
)

class ProgramRajolAllahActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                RajolAllahNavigationCoordinator(onBackToMain = { finish() })
            }
        }
    }
}

@Composable
fun RajolAllahNavigationCoordinator(onBackToMain: () -> Unit) {
    var activeScreen by remember { mutableStateOf("GATES") }

    AnimatedContent(
        targetState = activeScreen,
        transitionSpec = {
            slideInHorizontally { width -> width } + fadeIn() togetherWith
                    slideOutHorizontally { width -> -width } + fadeOut()
        },
        label = "RajolAllahNavigation"
    ) { screen ->
        when (screen) {
            "GATES" -> RajolAllahGatesScreen(
                onBack = onBackToMain,
                onNavigate = { screenId -> activeScreen = screenId }
            )
            "SELF_ASSESSMENT" -> SelfAssessmentScreen(onBack = { activeScreen = "GATES" })
            "HEART_CHECK" -> HeartCheckScreen(onBack = { activeScreen = "GATES" })
            "MEDIA_AWARENESS" -> MediaAwarenessScreen(onBack = { activeScreen = "GATES" })
            "GUIDELINES_LIBRARY" -> QuotesLibraryScreen(onBack = { activeScreen = "GATES" })
            "WEEKLY_CHALLENGES" -> ChallengesScreen(onBack = { activeScreen = "GATES" })
            "SIMULATION" -> SimulationScreen(onBack = { activeScreen = "GATES" })
            "REPORTS" -> RajolAllahReportsScreen(onBack = { activeScreen = "GATES" })
            "MALAZIM" -> MalazimArchiveScreen(onBack = { activeScreen = "GATES" })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RajolAllahGatesScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val doors = remember {
        listOf(
            DivineDoor("SELF_ASSESSMENT", "📋 التقييم الذاتي", "نموذج معايير الوعي الـ 31 والتقويم الأسبوعي المتكامل", "📋", "الأول"),
            DivineDoor("HEART_CHECK", "🫀 فحص سلامة القلب", "12 سؤالاً لتشخيص نبل الفؤاد وتصفية الران الإيماني دبراً", "🫀", "الثاني"),
            DivineDoor("WEEKLY_CHALLENGES", "🎯 التحديات الأسبوعية", "10 تحديات وغمار لتوثيق همم التمكين وبناء الأبطال", "🎯", "الثالث"),
            DivineDoor("SIMULATION", "🎭 محاكاة الأزمات", "5 سيناريوهات لاتخاذ القرار الرشيد مع الشرح القرآني", "🎭", "الرابع"),
            DivineDoor("GUIDELINES_LIBRARY", "📚 مكتبة التوجيهات", "سماع وقراءة دروس التزكية والأسئلة التفاعلية للفهم", "📚", "الخامس"),
            DivineDoor("MEDIA_AWARENESS", "📡 الوعي الإعلامي", "تحذير ومقاطعة تضليل العدو وقهر الحرب المعرفية المستبدة", "📡", "السادس"),
            DivineDoor("REPORTS", "📊 التقارير والإحصاء", "تطوير مستوى الوعي والتزام الأفراد مع الرسوم البيانية الدورية", "📊", "السابع"),
            DivineDoor("MALAZIM", "🪖 أرشيف الملازم العسكري", "ملازم هدي القرآن العسكري وملازم السيد القائد بالتمويه والدفاع السلبي", "🪖", "الثامن")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "🕌 برنامج رجال الله (الأبواب السبعة)",
                            color = TextLight,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "منصة متكاملة للارتقاء الإيماني والتزكية الفردية والعائلية",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "الرجوع للرئيسية",
                            tint = WarmGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWarm)
            )
        },
        containerColor = MidnightBlue
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Beautiful header motive banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WarmGold.copy(0.35f), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "الآية",
                        tint = WarmGold,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "«وَمَنْ يَتَّقِ اللَّهَ يَجْعَلْ لَهُ مَخْرَجًا وَيَرْزُقْهُ مِنْ حَيْثُ لَا يَحْتَسِبُ»",
                            color = TextLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Justify,
                            lineHeight = 17.sp
                        )
                        Text(
                            text = "من ميثاق الوعي الشامل لعائلات التمكين",
                            color = WarmGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            
            Text(
                text = "اختر باب العبور والارتقاء لتزكية النفس والتمكين الكوني وعائلتك:",
                color = TextLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(doors) { door ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(width = 1.dp, color = WarmGold.copy(alpha = 0.25f), shape = RoundedCornerShape(14.dp))
                            .clickable { onNavigate(door.id) },
                        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(WarmGold.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = door.emoji,
                                    fontSize = 24.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = door.title,
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "الباب ${door.note}",
                                color = WarmGold,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = door.description,
                                color = TextMuted,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 14.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Developer Signature
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "إدريس يوسف المداني | idrismedani20271448@gmail.com",
                    color = TextMuted.copy(0.7f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

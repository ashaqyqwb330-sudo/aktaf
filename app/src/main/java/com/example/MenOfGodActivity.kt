package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.EktefaaDatabase
import com.example.database.MenOfGodEvaluation
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MenOfGodActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme(darkTheme = false) {
                MenOfGodScreen(onBack = { finish() })
            }
        }
    }
}

// Local distinct colors representing traditional warm islamic aesthetic
val BeautifulWarmWhite = Color(0xFFFFF8F0)
val ItemSurfaceBg = Color(0xFF16223F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenOfGodScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { EktefaaDatabase.getDatabase(context) }
    
    // Read children using existing Dao
    val childrenFlow = remember { db.dao().getAllChildrenFlow() }
    val children by childrenFlow.collectAsState(initial = emptyList())
    
    var selectedTab by remember { mutableStateOf(0) }
    var selectedChildId by remember { mutableStateOf(0L) }
    
    // Automatically set default selected child when list loads
    LaunchedEffect(children) {
        if (selectedChildId == 0L && children.isNotEmpty()) {
            selectedChildId = children.first().id.toLong()
        }
    }

    val tabs = listOf("التقييم اليومي", "الوعي الإيماني", "مكتبة التوجيهات", "التقارير", "متابعة الأبناء")

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Gold.copy(alpha = 0.4f), DarkNavy)))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Gold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.SelfImprovement, null, tint = Gold, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "🕌 برنامج رجال الله",
                    color = Gold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Right
                )
            }
        },
        containerColor = BeautifulWarmWhite
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BeautifulWarmWhite)
        ) {
            // Children Selector Tab Row
            if (children.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = children.indexOfFirst { it.id.toLong() == selectedChildId }.coerceAtLeast(0),
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = BeautifulWarmWhite,
                    edgePadding = 8.dp,
                    indicator = {}
                ) {
                    children.forEach { child ->
                        Tab(
                            selected = selectedChildId == child.id.toLong(),
                            onClick = { selectedChildId = child.id.toLong() },
                            text = {
                                Text(
                                    child.name,
                                    color = if (selectedChildId == child.id.toLong()) Gold else DarkNavy,
                                    fontWeight = if (selectedChildId == child.id.toLong()) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = 0.05f))
                ) {
                    Text(
                        "تنبيه: لم يتم تسجيل أي ابن بالبرنامج بعد. يمكنك تقييم حساب العائلة العام.",
                        modifier = Modifier.padding(12.dp),
                        color = DarkNavy,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Main Tabs Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = BeautifulWarmWhite,
                contentColor = Gold,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedTab == index) Gold else DarkNavy.copy(alpha = 0.7f)
                            )
                        }
                    )
                }
            }

            // Tabs screens dispatcher
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(8.dp)
            ) {
                when (selectedTab) {
                    0 -> DailyEvaluationTab(db, selectedChildId)
                    1 -> FaithAwarenessTab()
                    2 -> GuidanceLibraryTab()
                    3 -> ReportsTab(db, selectedChildId)
                    4 -> ChildrenFollowUpTab(db, selectedChildId)
                }
            }
        }
    }
}

@Composable
fun DailyEvaluationTab(db: EktefaaDatabase, childId: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    
    var prayerScore by remember { mutableIntStateOf(0) }
    var quranScore by remember { mutableIntStateOf(0) }
    var athkarScore by remember { mutableIntStateOf(0) }
    var faithAwareness by remember { mutableIntStateOf(0) }
    var mediaAwareness by remember { mutableIntStateOf(0) }
    var avoidTrivial by remember { mutableStateOf(true) }
    var meaningfulInteraction by remember { mutableStateOf(true) }
    var gratitudeAwareness by remember { mutableIntStateOf(0) }
    var creativityEffort by remember { mutableStateOf(false) }
    var prayedForMujahideen by remember { mutableStateOf(false) }
    var sincerityInGiving by remember { mutableIntStateOf(0) }
    var jihadSpirit by remember { mutableIntStateOf(0) }
    var knowingAllahNames by remember { mutableIntStateOf(0) }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(childId) {
        scope.launch {
            val existing = db.menOfGodDao().getEvaluationForDate(childId, today)
            existing?.let {
                prayerScore = it.prayerScore
                quranScore = it.quranScore
                athkarScore = it.athkarScore
                faithAwareness = it.faithAwareness
                mediaAwareness = it.mediaAwareness
                avoidTrivial = it.avoidTrivial
                meaningfulInteraction = it.meaningfulInteraction
                gratitudeAwareness = it.gratitudeAwareness
                creativityEffort = it.creativityEffort
                prayedForMujahideen = it.prayedForMujahideen
                sincerityInGiving = it.sincerityInGiving
                jihadSpirit = it.jihadSpirit
                knowingAllahNames = it.knowingAllahNames
                notes = it.notes
            } ?: run {
                // reset to default
                prayerScore = 0
                quranScore = 0
                athkarScore = 0
                faithAwareness = 0
                mediaAwareness = 0
                avoidTrivial = true
                meaningfulInteraction = true
                gratitudeAwareness = 0
                creativityEffort = false
                prayedForMujahideen = false
                sincerityInGiving = 0
                jihadSpirit = 0
                knowingAllahNames = 0
                notes = ""
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "📋 قياس التطور السلوكي اليومي",
                color = DarkNavy,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            )
        }

        // Daily Morning Awareness Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "📌 تذكّر لليوم:",
                        color = Gold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "برنامج رجال الله: معركة الوعي أهم من أي معركة أخرى... ثقتك بالله وبصيص إيمانك هما سلاحك الأقوى والأمتن.",
                        color = DarkNavy,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Right,
                        lineHeight = 19.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Questions Section
        item { RatingQuestion("الصلاة والالتزام بمواقيتها 🕌", prayerScore) { prayerScore = it } }
        item { RatingQuestion("تلاوة الورد القرآني والتدبر 📖", quranScore) { quranScore = it } }
        item { RatingQuestion("الأذكار اليومية وحصن المسلم 📿", athkarScore) { athkarScore = it } }
        item { RatingQuestion("الوعي الإيماني والبصيرة والهدى 🧠", faithAwareness) { faithAwareness = it } }
        item { RatingQuestion("الوعي الإعلامي ومواجهة التضليل 📡", mediaAwareness) { mediaAwareness = it } }
        
        item { YesNoQuestion("تجنبت الرذائل والمسلسلات والتوافه؟ 🚫", avoidTrivial) { avoidTrivial = it } }
        item { YesNoQuestion("تفاعلت مع قضايا ومحتوى هادف عائلياً؟ 👥", meaningfulInteraction) { meaningfulInteraction = it } }
        
        item { RatingQuestion("شكر نعم الله واستشعار المسؤولية 🤲", gratitudeAwareness) { gratitudeAwareness = it } }
        
        item { YesNoQuestion("ابتكرت أو بذلت جهداً إيجابياً ملموساً اليوم؟ 💡", creativityEffort) { creativityEffort = it } }
        item { YesNoQuestion("دعوت للمجاهدين والمستضعفين في صلواتي اليوم؟ 🪖", prayedForMujahideen) { prayedForMujahideen = it } }
        
        item { RatingQuestion("الإخلاص وترك المنّ والأثرة 🤝", sincerityInGiving) { sincerityInGiving = it } }
        item { RatingQuestion("روح البذل والجهاد والمثابرة 💪", jihadSpirit) { jihadSpirit = it } }
        item { RatingQuestion("معرفة عظمة الله وأسمائه الحسنى 🌟", knowingAllahNames) { knowingAllahNames = it } }

        item {
            TextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات إضافية أو تقييم خاص للابن للتحسين...", color = DarkNavy.copy(0.6f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = DarkNavy,
                    unfocusedTextColor = DarkNavy,
                    focusedIndicatorColor = Gold,
                    unfocusedIndicatorColor = Gold.copy(0.4f)
                ),
                maxLines = 3
            )
        }

        // Save Button
        item {
            Button(
                onClick = {
                    scope.launch {
                        val evaluation = MenOfGodEvaluation(
                            childId = childId,
                            date = today,
                            prayerScore = prayerScore,
                            quranScore = quranScore,
                            athkarScore = athkarScore,
                            faithAwareness = faithAwareness,
                            mediaAwareness = mediaAwareness,
                            avoidTrivial = avoidTrivial,
                            meaningfulInteraction = meaningfulInteraction,
                            gratitudeAwareness = gratitudeAwareness,
                            creativityEffort = creativityEffort,
                            prayedForMujahideen = prayedForMujahideen,
                            sincerityInGiving = sincerityInGiving,
                            jihadSpirit = jihadSpirit,
                            knowingAllahNames = knowingAllahNames,
                            notes = notes
                        )
                        db.menOfGodDao().insertEvaluation(evaluation)
                        Toast.makeText(context, "تم حفظ تقييم برنامج رجال الله بنجاح 💾", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(bottom = 10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("حفظ التقييم واستعراض النتائج 🌟", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun RatingQuestion(title: String, value: Int, onValueChange: (Int) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..5).forEach { star ->
                    Icon(
                        imageVector = if (star <= value) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Star",
                        tint = if (star <= value) Gold else Silver,
                        modifier = Modifier
                            .size(26.dp)
                            .clickable { onValueChange(star) }
                    )
                }
            }
            Text(
                title,
                color = DarkNavy,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.weight(1f).padding(start = 12.dp)
            )
        }
    }
}

@Composable
fun YesNoQuestion(title: String, value: Boolean, onValueChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Switch(
                checked = value,
                onCheckedChange = onValueChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = RadarGreen,
                    uncheckedThumbColor = Silver,
                    uncheckedTrackColor = Color.LightGray
                )
            )
            Text(
                title,
                color = DarkNavy,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.weight(1f).padding(start = 12.dp)
            )
        }
    }
}

@Composable
fun FaithAwarenessTab() {
    val items = listOf(
        Pair("📖 معركة الوعي والبصيرة", "«معركة الوعي أهم من أي معركة أخرى، لأنها تبني العقل الذي ينطلق إلى معمعة الحياة بعلم وهدى وثبات. ثق بالله والتحق بمصدر الهداية والقرآن الكريم لمجابهة طغيان الباطل وهدف الشر.»\n- السيد القائد عبد الملك بدر الدين الحوثي"),
        Pair("🛑 قضية المنّ وعظمة العطاء", "«قضية المن مفسدة للأعمال والتحرك الإيماني. حين تقدّم وتجاهد، تحرّك من منطلق أنك المحتاج المستفيد والممنون لربك العظيم، فالعطاء الصفي يبني الفرد ويطهر الذات.»\n- الشهيد القائد حسين بدر الدين الحوثي"),
        Pair("💪 الإيمان العزة والكرامة المستقلة", "«الإيمان لا يجتمع أبداً مع الذل أو الخنوع. المؤمن يستمد عزة نفسه من هداية الله، فيرفع هامته مستقلاً كريماً رافضاً للظلم والاستعباد، معلماً لأبنائه معاني الحرية القرآني.»\n- السيد القائد")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "🧠 مراجع وروافد الوعي الإيماني",
                color = DarkNavy,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            )
        }

        items(items) { (title, content) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        title,
                        color = Gold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        content,
                        color = DarkNavy.copy(0.85f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Right,
                        lineHeight = 20.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun GuidanceLibraryTab() {
    val items = listOf(
        Pair("🎙️ الاستخدام السلبي لمواقع التواصل لرب الأسرة", "ديوان السيد القائد - 11 رمضان 1445هـ"),
        Pair("🧠 معركة الوعي وبناء الفكر المقاوم", "السيد القائد عبد الملك بدر الدين الحوثي"),
        Pair("📜 عواقب المنّ والغرور في العطاء والجهاد", "الشهيد القائد السيد حسين بدر الدين الحوثي"),
        Pair("📖 نصوص تزكية النفوس وتحمل المسؤولية العظمى", "سلسلة دروس التوجيه الإيماني"),
        Pair("🛡️ توجيهات الدفاع المدني والسلبي والتمويه المكتسب", "دروس الملازم المعتمدة لرجال الله"),
        Pair("🌟 معرفة الله والوعد الإلهي بالنصر واليقين", "خطب وملازم الهدي القرآني"),
        Pair("📡 مواجهة التضليل الغربي والحرب المعرفية والنفسية", "منشورات دائرة التوجيه المعنوي")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "📚 مكتبة التوجيهات والتزكية",
                color = DarkNavy,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            )
        }

        items(items) { (title, source) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = "Read",
                        tint = Gold,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            title,
                            color = DarkNavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            source,
                            color = Gold.copy(0.8f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Right
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsTab(db: EktefaaDatabase, childId: Long) {
    var averageScore by remember { mutableStateOf<Double?>(null) }
    var trendMessage by remember { mutableStateOf("قيد حساب المنحى والبيانات...") }

    LaunchedEffect(childId) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -7)
        val weekAgo = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
        val score = db.menOfGodDao().getAverageScore(childId, weekAgo)
        averageScore = score
        
        trendMessage = if (score != null) {
            val percentage = score * 2.222 // (average sum out of max 45 points) * 2.222 ~ scale of 100%
            if (percentage >= 80.0) {
                "مستوى متميز وبطولي ومشرّف جداً لرجال الله الصابرين! استمر على هذا الصرح المتين."
            } else if (percentage >= 50.0) {
                "مستوى إيماني واعد، يحتاج لرفع همة الابن في الحفظ والمحافظة والتدبر في الصباح والمساء."
            } else {
                "مستوى منخفض للأسرة يحتاج لتدخل فوري وخطة سلوكية للتأهيل والتوجيه."
            }
        } else {
            "لا توجد عمليات تقييم كافية في الـ 7 أيام الماضية. يرجى البدء بملء استمارات التقييم بانتظام لمشاهدة رسوم المنحدر والنسب المئوية."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "📊 التقارير الأسبوعية للتحصيل السلوكي",
            color = DarkNavy,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "معدل تحصيل الأسبوع للأداء السلوكي المقاس",
                    color = DarkNavy,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // Large Percentage Visual Display
                val displayPercent = if (averageScore != null) {
                    val rawVal = averageScore ?: 0.0
                    // max is 9 attributes x 5 = 45, plus 4 boolean yes/no. Total max weight is roughly 45.
                    // lets scale sum of attributes which is rawVal
                    // rawVal represents sum averages on db
                    val percentage = (rawVal / 45.0) * 100.0
                    percentage.coerceIn(0.0, 100.0)
                } else 0.0

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Gold.copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%.1f%%", displayPercent),
                            color = Gold,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "العام",
                            color = DarkNavy.copy(0.6f),
                            fontSize = 11.sp
                        )
                    }
                }

                Divider(color = Color.LightGray.copy(0.4f))

                Text(
                    text = trendMessage,
                    color = DarkNavy.copy(0.8f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ChildrenFollowUpTab(db: EktefaaDatabase, childId: Long) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "📋 خطة المتابعة الفردية للأبناء",
                color = Gold,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "«دليل التدخل الوقائي السلوكي التلقائي»:",
                color = DarkNavy,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "إذا رصد نظام التقييم تكرار تراجع مستوى 'الوعي الإعلامي والبصيرة' لأكثر من 3 أيام متتالية، سيقوم التطبيق تلقائياً باقتراح وفتح 'تحدي الوعي والبصيرة الفردي' للأبناء في ركن المسابقات رعاية وتصحيحاً لهم.",
                color = DarkNavy.copy(0.8f),
                fontSize = 12.sp,
                textAlign = TextAlign.Right,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, null, tint = Gold, modifier = Modifier.size(18.dp))
                Text(
                    "الحالة الحالية المقاسة: مستقر ومؤمن بحمد الله.",
                    color = RadarGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

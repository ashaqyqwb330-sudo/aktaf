package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.database.DailyPrayerTracking
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PrayersActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme(darkTheme = false) {
                PrayersScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayersScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { EktefaaDatabase.getDatabase(context) }
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("مكتبة الأدعية", "أدعية الوقت", "القرآن", "الصحيفة", "تقييم الدعاء")

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
                Icon(Icons.Default.MenuBook, null, tint = Gold, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "📿 الأدعية والزيارات العائلية",
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
            // Tab Header Row
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(10.dp)
            ) {
                when (selectedTab) {
                    0 -> PrayerLibraryTab()
                    1 -> TimePrayersTab(db)
                    2 -> QuranPrayersTab()
                    3 -> SahifaTab()
                    4 -> PrayerEvaluationTab(db)
                }
            }
        }
    }
}

@Composable
fun PrayerLibraryTab() {
    val prayers = listOf(
        Pair("أدعية أيام الأسبوع", "الرواد اليومي"),
        Pair("أدعية المجاهدين", "حرس الثغور والنصر"),
        Pair("أدعية الفرج", "الفرج الكلي وقضاء الخوف"),
        Pair("أدعية التمجيد", "الثناء على الله عز وجل"),
        Pair("أدعية الصباح والمساء", "ورد الحفظ والتنوير"),
        Pair("أدعية مأثورة عن النبي ﷺ", "النبوة والبركات"),
        Pair("أدعية مأثورة عن أهل البيت", "مدائح وأسرار آل عترة"),
        Pair("أدعية السفر والترحال", "الحصن السفر المكين"),
        Pair("أدعية الشفاء من المرض", "الصحة والتصبّر"),
        Pair("أدعية الفرج وكشف الهم والغم", "راحة الصدور والرضا"),
        Pair("أدعية سعة الرزق والبركة", "الكسب الطيب الصفي"),
        Pair("أدعية صلاح ودعاء الأبناء وبقائهم", "تربية وحب ونسل خير")
    )

    var expandedPrayerTitle by remember { mutableStateOf<String?>(null) }
    var expandedPrayerContent by remember { mutableStateOf("") }

    if (expandedPrayerTitle != null) {
        AlertDialog(
            onDismissRequest = { expandedPrayerTitle = null },
            confirmButton = {
                Button(
                    onClick = { expandedPrayerTitle = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)
                ) {
                    Text("إغلاق الدعاء", color = Color.White)
                }
            },
            title = {
                Text(
                    text = expandedPrayerTitle ?: "",
                    color = Gold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = expandedPrayerContent,
                    color = DarkNavy,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 22.sp
                )
            },
            containerColor = Color.White
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "📚 فهرس ومكتبة الأدعية والورد المعين:",
                color = DarkNavy,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(prayers) { (title, subtitle) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                onClick = {
                    expandedPrayerTitle = title
                    expandedPrayerContent = when (title) {
                        "أدعية أيام الأسبوع" -> "الحمد لله الذي لم يشهد أحداً حين فطر السموات والأرض، ولا اتخذ معيناً حين برأ النسمات... نسألك هداية متينة في أيامنا وحياتنا كلها."
                        "أدعية المجاهدين" -> "اللهم صل على محمد وآله، وحصّن ثغور المسلمين بعزتك، وأيّد حماتها بقوتك، وأسبغ عطاياك عليهم من فيض نصرك العظيم... اللهم ثبّت الأقدام والهمهم الصبر واليقين."
                        "أدعية الفرج" -> "يا من تحل به عقد المكاره، ويا من يفثأ به حد الشدائد، ويا من يلتمس منه المخرج إلى روح الفرج... ذلّت لقدرتك الصعاب والتجأ اليك الضعفاء."
                        "أدعية التمجيد" -> "ما من إله غيرك، الخالق المبدع، عظيم الشأن وجلّ ثناؤك، نسبح بحمدك بكرة وأصيلاً وعشياً."
                        "أدعية الصباح والمساء" -> "اللهم بك أصبحنا وبك أمسينا، وبك نحيا وبك نموت وإليك النشور. رضيت بالله رباً وبالإسلام ديناً وبمحمد ﷺ نبياً مرسلاً."
                        "أدعية مأثورة عن النبي ﷺ" -> "«اللهم اقسم لنا من خشيتك ما تحول به بيننا وبين معصيتك، ومن طاعتك ما تبلغنا به جنتك، ومن اليقين ما تهون به علينا مصائب الدنيا.»"
                        "أدعية مأثورة عن أهل البيت" -> "«إلهي عظم البلاء وبرح الخفاء وانكشف الغطاء وانقطع الرجاء وضاقت الأرض ومنعت السماء وأنت المستعان وإليك المشتكى وعليك المعول في الشدة والرخاء.»"
                        "أدعية السفر والترحال" -> "«اللهم أنت الصاحب في السفر والخليفة في الأهل، اللهم إنا نعوذ بك من وعثاء السفر وكآبة المنظر وسوء المنقلب في المال والأهل والولد.»"
                        "أدعية الشفاء من المرض" -> "«اللهم رب الناس أذهب البأس اشف أنت الشافي لا شفاء إلا شفاؤك شفاء لا يغادر سقماً.»"
                        "أدعية الفرج وكشف الهم والغم" -> "«اللهم إني عبدك بن عبدك وابن أمتك، ناصيتي بيدك، ماضٍ فيّ حكمك، عدلٌ فيّ قضاؤك، أسألك بكل اسم هو لك سميت به نفسك أن تجعل القرآن الكريم ربيع قلبي.»"
                        "أدعية سعة الرزق والبركة" -> "إلهي نسألك برزق واسع طيب حلال صفي تقضي به حاجات عائلتنا، وتبعدنا به عن الحرام وسؤال الخلق يا ذا الجلال والإكرام."
                        "أدعية صلاح ودعاء الأبناء وبقائهم" -> "«اللهم اجعل أبنائي هداة مهتدين غير ضالين ولا مضلين، اللهم حبب إليهم الإيمان وزينه في قلوبهم وكره إليهم الكفر والفسوق والعصيان واجعلهم من الراشدين.»"
                        else -> "نسألك الله البركة والتوفيق لأسرتنا جميعاً في حلّنا وترحالنا."
                    }
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MenuBook, null, tint = Gold, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
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
                            subtitle,
                            color = Gold.copy(0.8f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Right
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(Icons.Default.ChevronLeft, null, tint = Gold)
                }
            }
        }
    }
}
@Composable
fun TimePrayersTab(db: EktefaaDatabase) {
    val days = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
    var selectedDayIndex by remember { mutableStateOf(Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1) }
    var showContemplationDialog by remember { mutableStateOf(false) }

    var dbPrayerText by remember { mutableStateOf<com.example.database.PrayerText?>(null) }

    LaunchedEffect(selectedDayIndex) {
        // dayOfWeek is 1-indexed (1=Sunday, 2=Monday... 7=Saturday)
        dbPrayerText = db.prayerTextDao().getPrayerForDay(selectedDayIndex + 1)
    }

    val currentTitle = dbPrayerText?.title ?: days[selectedDayIndex]
    val currentText = dbPrayerText?.fullText ?: "جاري جلب الدعاء من قاعدة البيانات..."
    val contemplationQuestions = dbPrayerText?.contemplationQuestions?.split("|")?.filter { it.isNotBlank() } ?: listOf(
        "ما هي الصفات الإلهية التي افتتح بها الإمام الدعاء وكيف نرسخ عروتها في قلوبنا؟",
        "كيف يعيننا التبتل والاعتصام بالله في مجابهة ضلال ومكائد شياطين الإنس والجن؟",
        "ما هي الخطوة العملية التي تأثرت بها واستشعرت معها الخشوع?"
    )

    if (showContemplationDialog) {
        AlertDialog(
            onDismissRequest = { showContemplationDialog = false },
            confirmButton = {
                Button(
                    onClick = { showContemplationDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)
                ) {
                    Text("تم التثبيت والتأمل", color = Color.White)
                }
            },
            title = {
                Text(
                    "🤔 أسئلة التأمل والتدبر القرآني والوجداني",
                    color = Gold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "أسئلة تأمل دعاء يوم ${days[selectedDayIndex]}:",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    contemplationQuestions.forEachIndexed { index, question ->
                        Text("${index + 1}. $question", fontSize = 12.sp, color = Color.White.copy(0.85f), textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            containerColor = SurfaceDark
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "📅 أدعية الأيام والمواقيت الأسبوعية:",
            color = WarmGold,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )

        ScrollableTabRow(
            selectedTabIndex = selectedDayIndex,
            containerColor = SurfaceDark,
            contentColor = Gold,
            edgePadding = 4.dp,
            indicator = {}
        ) {
            days.forEachIndexed { index, day ->
                Tab(
                    selected = selectedDayIndex == index,
                    onClick = { selectedDayIndex = index },
                    text = {
                        Text(
                            text = day,
                            color = if (selectedDayIndex == index) Gold else Color.White,
                            fontWeight = if (selectedDayIndex == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentTitle,
                        color = Gold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = currentText,
                                color = Color.White.copy(0.9f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Right,
                                lineHeight = 22.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showContemplationDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "تأمل وتدبر في الدعاء اليومي 🤔",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun QuranPrayersTab() {
    val quranPrayers = listOf(
        Pair("رَبَّنَا أَفْرِغْ عَلَيْنَا صَبْرًا وَثَبِّتْ أَقْدَامَنَا وَانصُرْنَا عَلَى الْقَوْمِ الْكَافِرِينَ", "سورة البقرة - الآية 250"),
        Pair("رَبَّنَا لاَ تُؤَاخِذْنَا إِن نَّسِينَا أَوْ أَخْطَأْنَا رَبَّنَا وَلاَ تَحْمِلْ عَلَيْنَا إِصْرًا كَمَا حَمَلْتَهُ عَلَى الَّذِينَ مِن قَبْلِنَا", "سورة البقرة - الآية 286"),
        Pair("رَبَّنَا لاَ تُزِغْ قُلُوبَنَا بَعْدَ إِذْ هَدَيْتَنَا وَهَبْ لَنَا مِن لَّدُنكَ رَحْمَةً إِنَّكَ أَنتَ الْوَهَّابُ", "سورة آل عمران - الآية 8"),
        Pair("رَبِّ اجْعَلْنِي مُقِيمَ الصَّلاَةِ وَمِن ذُرِّيَّتِي رَبَّنَا وَتَقَبَّلْ دُعَاءِ", "سورة إبراهيم - الآية 40"),
        Pair("رَّبِّ أَدْخِلْنِي مُدْخَلَ صِدْقٍ وَأَخْرِجْنِي مُخْرَجَ صِدْقٍ وَاجْعَل لِّي مِن لَّدُنكَ سُلْطَانًا نَّصِيرًا", "سورة الإسراء - الآية 80"),
        Pair("رَبَّنَا هَبْ لَنَا مِنْ أَزْوَاجِنَا وَذُرِّيَّاتِنَا قُرَّةَ أَعْيُنٍ وَاجْعَلْنَا لِلْمُتَّقِينَ إِمَامًا", "سورة الفرقان - الآية 74")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "🌟 أدعية الهدى والذكر الحكيم من القرآن الكريم:",
                color = DarkNavy,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(quranPrayers) { (text, verse) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = text,
                        color = DarkNavy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Right,
                        lineHeight = 22.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = verse,
                        color = Gold,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Left,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun SahifaTab() {
    val sahifaPrayers = listOf(
        Pair("التحميد والتمجيد لله جل جلاله", "«الْحَمْدُ لِلَّهِ الْأَوَّلِ بِلَا أَوَّلٍ كَانَ قَبْلَهُ ، وَالْآخِرِ بِلَا آخِرٍ يَكُونُ بَعْدَهُ...»"),
        Pair("الصلاة والبركات على الرسول وعترته", "«وَالْحَمْدُ لِلَّهِ الَّذِي مَنَّ عَلَيْنَا بِمُحَمَّدٍ نَبِيِّهِ صَلَّى اللَّهُ عَلَيْهِ وَآلِهِ دُونَ الْأُمَمِ الْمَاضِيَةِ...»"),
        Pair("دعاء الصلاة لحملة العرش ومقربين", "«اللَّهُمَّ وَحَمَلَةُ عَرْشِكَ الَّذِينَ لَا يَفْتُرُونَ مِنْ تَسْبِيحِكَ، وَلَا يَسْأَمُونَ مِنْ تَقْدِيسِكَ...»"),
        Pair("دعاء لطلب العافية واللطف والصحة", "«اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَآلِهِ، وَأَلْبِسْنِي عَافِيَتَكَ، وَجَلِّلْنِي عَافِيَتَكَ، وَحَصِّنِّي بِعَافِيَتِكَ...»"),
        Pair("دعاء التوبة وطلب الغفران والإنابة", "«اللَّهُمَّ يَا مَنْ لَا يَصِفُهُ نَعْتُ الْوَاصِفِينَ، وَيَا مَنْ لَا يُجَاوِزُهُ رَجَاءُ الرَّاجِينَ...»"),
        Pair("دعاء الاستعاذة من المكاره ومذام الأخلاق", "«اللَّهُمَّ إِّني أَعُوذُ بِكَ مِنْ هَيَجَانِ الْحِرْصِ، وَسَوْرَةِ الْغَضَبِ، وَاسْتِيلَاءِ الْحَسَدِ...»")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "🕋 أدعية الصحيفة السجادية المباركة الكاملة:",
                color = DarkNavy,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(sahifaPrayers) { (title, snippet) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = title,
                        color = Gold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = snippet,
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
fun PrayerEvaluationTab(db: EktefaaDatabase) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var prayedToday by remember { mutableStateOf(false) }
    var feltKhushu by remember { mutableIntStateOf(0) }
    var prayedForMujahideen by remember { mutableStateOf(false) }
    var rememberedAnsweredPrayer by remember { mutableStateOf(false) }
    var answeredPrayerNote by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "📊 التقييم الروحي والصلة بالله لليوم:",
                color = DarkNavy,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item { YesNoQuestion("هل وُفّقت لدعاء الورد والوقت اليوم؟ 🤲", prayedToday) { prayedToday = it } }
        item { RatingQuestion("مدى استشعار الخشوع والتدبر في الدعاء? ✨", feltKhushu) { feltKhushu = it } }
        item { YesNoQuestion("هل دعوت للمجاهدين وحماة الدين اليوم؟ ⚔️", prayedForMujahideen) { prayedForMujahideen = it } }
        item { YesNoQuestion("هل استشعرت وتذكرت جواب دعاء سابق اليوم؟ 💡", rememberedAnsweredPrayer) { rememberedAnsweredPrayer = it } }

        item {
            TextField(
                value = answeredPrayerNote,
                onValueChange = { answeredPrayerNote = it },
                label = { Text("سجل فضل وثمرات استجابة الدعاء أو آثاره الإيمانية...", color = DarkNavy.copy(0.6f)) },
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
                        val tracking = DailyPrayerTracking(
                            childId = 1L, // Default family user id represent base log
                            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                            prayerType = "day_prayer",
                            prayerTitle = "تقييم الدعاء اليومي الفردي",
                            isRead = prayedToday,
                            isContemplated = feltKhushu > 3,
                            answeredPrayerNote = answeredPrayerNote
                        )
                        db.dailyPrayerDao().insertTracking(tracking)
                        Toast.makeText(context, "تم حفظ تقييم صلة الدعاء والمناجاة بنجاح 💾📿", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "حفظ وإرسال تقرير الدعاء 📿",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class GlossaryTerm(
    val english: String,
    val arabic: String,
    val explanation: String
)

data class HealthBook(
    val title: String,
    val author: String,
    val pages: Int,
    val content: String
)

class MedicalLibraryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                MedicalLibraryScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalLibraryScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) } // 0=Glossary, 1=E-Books
    var searchQuery by remember { mutableStateOf("") }
    var selectedBook by remember { mutableStateOf<HealthBook?>(null) }

    val glossaryList = listOf(
        GlossaryTerm("Anesthesia", "تخدير (تبنيج)", "فقدان مؤقت للاحساس بالألم أو الوعي يجرى قبل العمليات الطبية الجراحية."),
        GlossaryTerm("Hypertension", "ارتفاع ضغط الدم", "حالة جدارية مزمنة يرتفع خلالها ضغط الدم بالشرايين فوق معدل 140/90 مليمتر زئبق."),
        GlossaryTerm("Diabetes Mellitus", "مرض السكري الطاحن", "مرض غدي استقلابي مزمن يتسم بارتفاع مستويات الجلوكوز بالدم نتيجة نقص هرمون الانسولين."),
        GlossaryTerm("Appendicitis", "التهاب الزائدة الدودية", "التهاب حاد ومفاجئ يصيب الزائدة الدودية بأسفل البطن الأيمن ويتطلب تدخلاً جراحياً عاجلاً."),
        GlossaryTerm("Pneumonia", "التهاب رئة (ذات الرئة)", "عدوى تصيب الفصوص والأسناخ الرئوية مسببة تجمع السوائل وضيق حاد بالتنفس والسعال المصحوب بحرارة."),
        GlossaryTerm("Anemia", "فقر الدم (الأنيميا)", "نقص فادح بخلايا الدم الحمراء السليمة أو نقص الهيموجلوبين المسؤول عن تغذية الأوكسجين بالجسم."),
        GlossaryTerm("Atherosclerosis", "تصلب الشرايين", "تراكم الدهون والترسبات على الجدران الشريانية مما يحد من تدفق الدم وصحة القلب.")
    )

    val filteredGlossary = remember(searchQuery) {
        if (searchQuery.isBlank()) glossaryList
        else glossaryList.filter {
            it.english.contains(searchQuery, true) ||
            it.arabic.contains(searchQuery, true) ||
            it.explanation.contains(searchQuery, true)
        }
    }

    val booksList = listOf(
        HealthBook(
            title = "دليل رعاية مريض السكري العائلي بالمنزل",
            author = "أ.د. يوسف المداني",
            pages = 32,
            content = "1. تنظيم الوجبات الغذائية وتفادي السكريات والنشويات المكررة.\n" +
                    "2. الفحص الدوري ومراقبة مستويات السكر صائماً وبعد ساعتين وحفظ السجل بانتظام.\n" +
                    "3. العناية التامة بالقدمين وفحصهما يومياً وغسلهما بالماء الفاتر لتشخيص أي جروح لتجنب القدم السكرية.\n" +
                    "4. ممارسة الرياضات البدنية المعتدلة مثل المشي السريع 30 دقيقة يومياً لزيادة جودة وحساسية الإنسولين."
        ),
        HealthBook(
            title = "أسرار المناعة الذاتية ومكافحة الفيروسات الموسمية",
            author = "د. سليمان الفارسي",
            pages = 48,
            content = "1. النوم الطبيعي الهادئ والكافي لمدة 7-8 ساعات يومياً لتنشيط الكريات البيضاء.\n" +
                    "2. دمج الأغذية الطبيعية الغنية بفيتامين C (كالحمضيات والفلفل الرومي) والزنك في الوجبات الأساسية.\n" +
                    "3. شرب الكميات الصحية الكافية من المياه والسوائل الساخنة لتطهير الحلق والجسم من الفيروسات العالقة.\n" +
                    "4. تفادي الكربوهيدرات السريعة والتوتر النفسي اللذين يعطلان عمل الجهاز المناعي بشكل مؤقت."
        ),
        HealthBook(
            title = "المرشد الصحي الشامل لأمراض ضغط الدم الشرياني",
            author = "د. إدريس يوسف",
            pages = 25,
            content = "1. الحد الشامل من تناول صوديوم الملح (أقل من ملعقة شاي صغيرة يومياً للكبار) وتجنب الأغذية المصنعة والمعلبة.\n" +
                    "2. زيادة تناول الأطعمة الغنية بالبوتاسيوم (كالموز، الكاكاو الداكن، الطماطم والجريش) لدعم إرخاء الأوعية الدموية.\n" +
                    "3. الحفاظ على الوزن الصحي وتجنب التدخين أو تنشق الملوثات الهوائية ومطالعة الطبيب لمراقبة تقدم قراءات الضغط الحيوية."
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📚 المكتبة الطبية والقاموس الصحي", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = WarmGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepObsidian)
            )
        },
        containerColor = DeepObsidian
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DeepObsidian)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceDark,
                contentColor = WarmGold
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Text("القاموس الطبي (En/Ar)", fontSize = 12.sp, color = if(selectedTab == 0) WarmGold else TextLight)
                    }
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Text("كتيبات التوعية الصحية", fontSize = 12.sp, color = if(selectedTab == 1) WarmGold else TextLight)
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    // Search dictionary
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث بالإنجليزية أو العربية في القاموس...", color = SilverGray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = WarmGold) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = SilverGray,
                            focusedBorderColor = WarmGold,
                            unfocusedBorderColor = WarmGold.copy(0.4f),
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark
                        )
                    )

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredGlossary) { term ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(modifier = Modifier.padding(14.dp)) {
                                    Icon(Icons.Default.Book, null, tint = WarmGold, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(term.english, color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(term.arabic, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(term.explanation, color = SilverGray, fontSize = 12.sp, lineHeight = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    if (selectedBook == null) {
                        // Display list of books
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(booksList) { book ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedBook = book },
                                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.MenuBook, null, tint = WarmGold, modifier = Modifier.size(28.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(book.title, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("للكاتب: ${book.author} | عدد الصفحات: ${book.pages}", color = SilverGray, fontSize = 11.sp)
                                        }
                                        Icon(Icons.Default.ChevronLeft, null, tint = WarmGold)
                                    }
                                }
                            }
                        }
                    } else {
                        // Display active book contents with Dynamic Typography and Silver on DeepObsidian
                        val bookScrollState = rememberScrollState()
                        val dynamicBonus = (bookScrollState.value * 0.015f).coerceIn(-3f, 7f)
                        val computedFontSize = (14f + dynamicBonus).sp
                        val computedLineHeight = (23f + dynamicBonus).sp

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(bookScrollState)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { selectedBook = null },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)
                                ) {
                                    Icon(Icons.Default.ArrowBack, null, tint = WarmGold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("الرجوع للقائمة", color = TextLight)
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF080C14)), // DeepObsidian card
                                shape = RoundedCornerShape(16.dp),
                                modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(selectedBook!!.title, color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("المؤلف: ${selectedBook!!.author}", color = Color(0xFFC0C0C0), fontSize = 12.sp) // Silver
                                    Divider(color = Color(0xFFD4AF37).copy(0.2f), modifier = Modifier.padding(vertical = 10.dp))
                                    Text(
                                        selectedBook!!.content,
                                        color = Color(0xFFC0C0C0), // Silver primary text
                                        fontSize = computedFontSize,
                                        lineHeight = computedLineHeight
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

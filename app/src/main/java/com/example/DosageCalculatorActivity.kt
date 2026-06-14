package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MedicationDatabase
import com.example.data.Drug
import com.example.ui.theme.*

class DosageCalculatorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                DosageCalculatorScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DosageCalculatorScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(1) } // Default to Calculator
    var searchQuery by remember { mutableStateOf("") }
    var selectedDrug by remember { mutableStateOf<Drug?>(null) }
    var weight by remember { mutableStateOf("15") } // Default weight in kg
    var age by remember { mutableStateOf("4") }
    var resultDosage by remember { mutableStateOf("") }
    var showDrugDetails by remember { mutableStateOf(false) }

    val allDrugs = remember { MedicationDatabase.getAllDrugs() }
    val favorites = remember { mutableStateListOf<Drug>() }

    // Init with first drug if none selected
    if (selectedDrug == null && allDrugs.isNotEmpty()) {
        selectedDrug = allDrugs.first()
    }

    // Filter drugs
    val filteredDrugs = remember(searchQuery, allDrugs) {
        if (searchQuery.isBlank()) allDrugs
        else allDrugs.filter {
            it.name.contains(searchQuery, true) ||
            it.category.contains(searchQuery, true) ||
            it.uses.contains(searchQuery, true)
        }
    }

    // Recalculate automatic dosage
    LaunchedEffect(selectedDrug, weight) {
        val wVal = weight.toDoubleOrNull()
        if (selectedDrug != null && wVal != null && wVal > 0) {
            val calcMg = selectedDrug!!.factor * wVal
            resultDosage = "الجرعة المقترحة هي: ${String.format("%.1f", calcMg)} ملغ (mg) لكل منفذ جرعة.\n" +
                    "الجرعة اليومية الإجمالية الموصى بها: ${String.format("%.1f", calcMg * 3)} ملغ (موزعة على 3 جرعات عادةً)."
        } else {
            resultDosage = "الرجاء إدخال وزن صحيح لمعاينة الجرعة الدقيقة."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("💊 حاسبة جرعات الأدوية للأطفال والعائلة", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = WarmGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightBlue)
            )
        },
        containerColor = MidnightBlue
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MidnightBlue)
        ) {
            // Tabs selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceDark,
                contentColor = WarmGold
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Text("مكتبة الأدوية", fontSize = 12.sp, color = if(selectedTab == 0) WarmGold else TextLight)
                    }
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Text("حاسبة الجرعة العائلية", fontSize = 12.sp, color = if(selectedTab == 1) WarmGold else TextLight)
                    }
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Text("المفضلة (${favorites.size})", fontSize = 12.sp, color = if(selectedTab == 2) WarmGold else TextLight)
                    }
                }
            }

            // Tabs Content
            when (selectedTab) {
                0 -> {
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث عن دواء أو مادة فعالة...", color = SilverGray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = WarmGold) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarmGold,
                            unfocusedBorderColor = SurfaceDark,
                            cursorColor = WarmGold,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        )
                    )

                    // Drug Library List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredDrugs) { drug ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedDrug = drug
                                        showDrugDetails = true
                                    },
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Medication, null, tint = WarmGold, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(drug.name, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(drug.category, color = WarmGold, fontSize = 11.sp)
                                        Text(drug.uses, color = SilverGray, fontSize = 10.sp, maxLines = 1)
                                    }
                                    IconButton(
                                        onClick = {
                                            if (favorites.contains(drug)) favorites.remove(drug)
                                            else favorites.add(drug)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (favorites.contains(drug)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = null,
                                            tint = if (favorites.contains(drug)) RedAccent else SilverGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Calculator Screen
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Drug Selection Quick Dropdown or chooser card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("💡 الدواء النشط للاحتساب:", color = WarmGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { selectedTab = 0 } // Switch to library to pick
                                ) {
                                    Icon(Icons.Default.Healing, null, tint = WarmGold, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(selectedDrug?.name ?: "لم يتم اختيار دواء", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("الفئة: ${selectedDrug?.category ?: ""}", color = SilverGray, fontSize = 12.sp)
                                    }
                                    Text("تغيير ↩", color = WarmGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Input fields weight and age
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = weight,
                                onValueChange = { weight = it },
                                label = { Text("الوزن (كيلو غرام)", color = WarmGold) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WarmGold,
                                    unfocusedBorderColor = SurfaceDark,
                                    focusedTextColor = TextLight,
                                    unfocusedTextColor = TextLight
                                )
                            )

                            OutlinedTextField(
                                value = age,
                                onValueChange = { age = it },
                                label = { Text("العمر (سنوات)", color = WarmGold) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WarmGold,
                                    unfocusedBorderColor = SurfaceDark,
                                    focusedTextColor = TextLight,
                                    unfocusedTextColor = TextLight
                                )
                            )
                        }

                        // Calculated Dosage output board
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Scale, null, tint = GreenAccent, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("الجرعة الطبية الدقيقة المحتسبة", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    resultDosage,
                                    color = GoldLight,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    "⚠️ تنبيه حاسم: هذه الحاسبة هي وسيلة استرشادية طبية مساعدة ومبنية على وزن الطفل، ولا تغني مطلقاً عن استشارة الطبيب المختص أو الصيدلي للتأكد من ملاءمة التركيز التجاري المتوفر.",
                                    color = RedAccent.copy(alpha = 0.9f),
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        // Selected medication profile summary
                        selectedDrug?.let { drug ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("📝 بطاقة الدواء الطبية:", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("• الاسـتـعـمـالات: ${drug.uses}", color = TextLight, fontSize = 12.sp)
                                    Text("• الـجـرعـة الـقـيـاسـيـة: ${drug.standardDose}", color = TextLight, fontSize = 12.sp)
                                    Text("• طريق الإعطاء: ${drug.adminRoute}", color = TextLight, fontSize = 12.sp)
                                    Text("• مـوانـع الاستعلام: ${drug.contraindications}", color = SilverGray, fontSize = 11.sp)
                                    Text("• الأعراض الجانبية المحتملة: ${drug.sideEffects}", color = SilverGray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Favorites List
                    if (favorites.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.FavoriteBorder, null, tint = SilverGray, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("المفضلة فارغة حالياً.", color = SilverGray, fontSize = 13.sp)
                                Text("احفظ أدويتك المفضلة من تبويب مكتبة الأدوية.", color = SilverGray.copy(alpha = 0.7f), fontSize = 11.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(favorites) { drug ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedDrug = drug
                                            selectedTab = 1
                                        },
                                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Favorite, null, tint = RedAccent, modifier = Modifier.size(26.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(drug.name, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(drug.category, color = WarmGold, fontSize = 11.sp)
                                        }
                                        Text("احسب الجرعة ↩", color = WarmGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Dialog
    if (showDrugDetails && selectedDrug != null) {
        AlertDialog(
            onDismissRequest = { showDrugDetails = false },
            confirmButton = {
                Button(
                    onClick = { showDrugDetails = false },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold)
                ) {
                    Text("إغلاق", color = MidnightBlue)
                }
            },
            title = {
                Text(selectedDrug!!.name, color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("• الـتـصـنـيـف: ${selectedDrug!!.category}", color = TextLight, fontSize = 13.sp)
                    Text("• الاسـتـخـدامـات: ${selectedDrug!!.uses}", color = TextLight, fontSize = 13.sp)
                    Text("• الـجـرعـة الـمـعـيـاريـة: ${selectedDrug!!.standardDose}", color = TextLight, fontSize = 13.sp)
                    Text("• طريقة الإعطاء: ${selectedDrug!!.adminRoute}", color = TextLight, fontSize = 13.sp)
                    Text("• محاذير الاستعمال وموانعه: ${selectedDrug!!.contraindications}", color = SilverGray, fontSize = 12.sp)
                }
            },
            containerColor = SurfaceWarm,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

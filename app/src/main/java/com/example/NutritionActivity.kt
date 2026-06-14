package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

data class MealLog(
    val type: String, // فطور ، غداء ، عشاء
    val items: String,
    val calories: Int
)

data class TherapeuticRecipe(
    val name: String,
    val calories: Int,
    val suitableFor: String,
    val ingredients: String,
    val preparation: String
)

class NutritionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                NutritionScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0=Meals, 1=Water Cups, 2=Therapeutic Cooking

    // Water cups (array of 8 boolean indices representing filled status)
    val waterCups = remember { mutableStateListOf(false, false, false, false, false, false, false, false) }
    val filledWaterCount = waterCups.count { it }

    // Meal Logs List
    val mealLogs = remember {
        mutableStateListOf(
            MealLog("فطور", "بيضتان مسلوقتان مع ربع خبز قمح وحبة خيار", 250),
            MealLog("غداء", "صدر دجاج مشوي (200 جرام) مع 6 ملاعق أرز أبيض وسلطة خضراء", 550),
            MealLog("عشاء", "علبة زبادي خالية الدسم مع ملعقة بذور كتان ونصف تفاحة", 180)
        )
    }

    // Add Meal Dialog state
    var showAddMealDialog by remember { mutableStateOf(false) }
    var mealType by remember { mutableStateOf("فطور") }
    val mealTypes = listOf("فطور", "غداء", "عشاء", "وجبة خفيفة")
    var mealTypeDropdownExpanded by remember { mutableStateOf(false) }
    var mealFoodText by remember { mutableStateOf("") }
    var mealCaloriesText by remember { mutableStateOf("") }

    val totalCalories = mealLogs.sumOf { it.calories }

    // Static Therapeutic Recipes
    val recipeList = listOf(
        TherapeuticRecipe(
            name = "شوربة الخضار الصحية منخفضة الصوديوم",
            calories = 145,
            suitableFor = "مرضى ضغط الدم المرتفع والقلب",
            ingredients = "كوسا، دجاج مسلوق، جزر، كرفس، بصل، ثوم، بهارات أعشاب خفيفة بدون ملح.",
            preparation = "تسلق المكونات سوياً في كوبين من مرق الدجاج منزوع الدسم وتطهى على نار هادئة لمدة 20 دقيقة وتزين بالبقدونس."
        ),
        TherapeuticRecipe(
            name = "سلطة الكينوا وتفاح السايبر لمرضى السكري",
            calories = 210,
            suitableFor = "مرضى السكري والرجيم",
            ingredients = "كينوا مسلوقة، حبة تفاح أخضر مبشور، لوز محمص، أوراق جرجير طازجة، ليمون، ملعقة زيت زيتون البكر.",
            preparation = "تخلط الكينوا الباردة بالجرجير والتفاح الأخضر ويضاف إليها رذاذ زيت الزيتون وعصير الحامض وتزين بالبروتينات واللوز لحفظ سكر الدم مستقراً."
        ),
        TherapeuticRecipe(
            name = "عصير الشوفان الغني بالألياف لخفض الكولسترول",
            calories = 180,
            suitableFor = "مرضى الكولسترول المرتفع والجهاز الهضمي",
            ingredients = "3 ملاعق شوفان كامل، كوب حليب لوز أو صويا، نصف موزة، رشة قرفة صغيرة.",
            preparation = "توضع المكونات كاملة بخلاط يدوي سريع وتخلط جيداً وتؤخذ كوجبة فطور أو خفيفة صباحية غنية بألياف البيتا-جلوكان."
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🍎 مطبخ الحياة والتغذية السليمة", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = WarmGold)
                    }
                },
                actions = {
                    if (selectedTab == 0) {
                        IconButton(onClick = { showAddMealDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Meal", tint = WarmGold)
                        }
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
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceDark,
                contentColor = WarmGold
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Text("الوجبات والسعرات", fontSize = 12.sp, color = if(selectedTab == 0) WarmGold else TextLight)
                    }
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Text("تحدي شرب الماء", fontSize = 12.sp, color = if(selectedTab == 1) WarmGold else TextLight)
                    }
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Text("وصفات علاجية", fontSize = 12.sp, color = if(selectedTab == 2) WarmGold else TextLight)
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Total Calorie Status Header
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("إجمالي السعرات المستهلكة لليوم", color = TextLight, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$totalCalories / 2000 سعرة حرارية", color = WarmGold, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { (totalCalories / 2000f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = GreenAccent,
                                    trackColor = MidnightBlue
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("قائمة الطعام المستهلكة:", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            TextButton(onClick = { showAddMealDialog = true }) {
                                Text("+ إضافة وجبة", color = WarmGold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(mealLogs) { meal ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(WarmGold.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Text(meal.type, color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(meal.items, color = TextLight, fontSize = 13.sp, lineHeight = 18.sp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("${meal.calories} Cal", color = GreenAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("💧 تحدي شرب الماء اليومي (Ektefaa Hydration)", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "قم بشرب 8 أكواب كافية من المياه النظيفة يومياً لتنشيط الكليتين، خفض السموم، والحفاظ على نضارة البشرة وحيوية الجسم.",
                                    color = TextLight,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        // Hydro Display Progress Circle
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("الأكواب المستهلكة لليوم", color = SilverGray, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "$filledWaterCount من 8 أكواب (أكواب ملؤها ${filledWaterCount * 250} مل)",
                                    color = Color(0xFF3498DB),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { filledWaterCount / 8f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .clip(CircleShape),
                                    color = Color(0xFF3498DB),
                                    trackColor = MidnightBlue
                                )
                            }
                        }

                        Text("اضغط على الكوب بعد الشرب لتأكيد شرب كوب الماء:", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        
                        // Grid of 8 Water Glasses which represent water toggled
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(waterCups.toList()) { idx, filled ->
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (filled) Color(0xFF3498DB).copy(alpha = 0.25f) else SurfaceDark)
                                        .clickable {
                                            waterCups[idx] = !waterCups[idx]
                                        }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            if (filled) "🥛" else "🍺",
                                            fontSize = 28.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "كوب ${idx + 1}",
                                            color = if (filled) Color(0xFF3498DB) else SilverGray,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Therapeutic eating list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.SoupKitchen, null, tint = WarmGold, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        "مجموعة الوجبات والغذاء العلاجي: وصفات حمية ميسرة مصممة من مأكولات عائلية بسيطة لدعم الاستجابة الطبية للأمراض الشائعة.",
                                        color = TextLight,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        items(recipeList) { recipe ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.RestaurantMenu, null, tint = WarmGold)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(recipe.name, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .background(GreenAccent.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("تناسب: ${recipe.suitableFor}", color = GreenAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }

                                    Text("🧪 المقادير الفعالة: ${recipe.ingredients}", color = SilverGray, fontSize = 12.sp, lineHeight = 16.sp)
                                    Text("👩‍🍳 طريقة الإعداد والطبخ المعتمد: ${recipe.preparation}", color = TextLight, fontSize = 12.sp, lineHeight = 18.sp)
                                    
                                    Divider(color = MidnightBlue)
                                    
                                    Text(
                                        "السعرات الحرارية المقدرة: ${recipe.calories} سعرة حرارية",
                                        color = WarmGold,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddMealDialog) {
        AlertDialog(
            onDismissRequest = { showAddMealDialog = false },
            title = { Text("✍️ لتسجيل وجبة جديدة", color = WarmGold, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box {
                        OutlinedTextField(
                            value = mealType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("نوع الوجبة") },
                            trailingIcon = {
                                IconButton(onClick = { mealTypeDropdownExpanded = !mealTypeDropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = mealTypeDropdownExpanded,
                            onDismissRequest = { mealTypeDropdownExpanded = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            mealTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type, color = TextLight) },
                                    onClick = {
                                        mealType = type
                                        mealTypeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = mealFoodText,
                        onValueChange = { mealFoodText = it },
                        label = { Text("مكونات الطعام المستهلك") },
                        placeholder = { Text("مثال: صحن سلطة صغير + 100g أرز") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = mealCaloriesText,
                        onValueChange = { mealCaloriesText = it },
                        label = { Text("السعرات المقدرة (بالتقريب)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cal = mealCaloriesText.toIntOrNull()
                        if (mealFoodText.isBlank() || cal == null) {
                            Toast.makeText(context, "الرجاء كشط مكونات الطعام وسعراتها بشكل صحيح", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        mealLogs.add(MealLog(mealType, mealFoodText, cal))
                        showAddMealDialog = false
                        mealFoodText = ""
                        mealCaloriesText = ""
                        Toast.makeText(context, "تم حفظ وجبتك الغذائية بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold)
                ) {
                    Text("إضافة وجبة", color = MidnightBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMealDialog = false }) {
                    Text("إلغاء", color = SilverGray)
                }
            },
            containerColor = SurfaceWarm,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

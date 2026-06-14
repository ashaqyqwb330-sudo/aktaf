package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.ClipItem
import com.example.service.ClipboardMonitorService
import com.example.ui.theme.MidnightBlue
import com.example.ui.theme.WarmGold

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ClipboardScreen(
    items: List<ClipItem>,
    onBack: () -> Unit,
    onAddItem: (String, String) -> Unit,
    onDeleteItem: (Int) -> Unit,
    onUpdateItem: (ClipItem) -> Unit = {}
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

    // Load categories from SharedPreferences dynamically
    var customCategories by remember {
        mutableStateOf(
            sharedPrefs.getStringSet("clipboard_custom_categories", null)?.toList()?.sorted()
                ?: listOf("عام", "خطب", "أدعية", "فقه", "نصائح", "تربية", "مقالات أعجبتني", "أخبار", "هام", "متابعة")
        )
    }

    var selectedCategory by remember { mutableStateOf("الكل") }
    var textInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("عام") }
    var menuExpanded by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showCategoryMgmtDialog by remember { mutableStateOf(false) }
    var selectedClipForReader by remember { mutableStateOf<ClipItem?>(null) }

    // Service controller state synchronized with preferences
    var isServiceActive by remember {
        mutableStateOf(sharedPrefs.getBoolean("clip_monitor_active", false))
    }

    // Dynamic filtering matching loaded categories
    val categoriesForTabs = remember(customCategories) {
        listOf("الكل") + customCategories
    }

    var searchQuery by remember { mutableStateOf("") }
    var searchType by remember { mutableStateOf("contains") } // "contains", "starts_with", "exact_word"
    var searchTarget by remember { mutableStateOf("all") }    // "all", "content", "category"
    var sortBy by remember { mutableStateOf("newest") }        // "newest", "oldest", "alphabetical"
    var showAdvancedSearch by remember { mutableStateOf(false) }

    val filteredItems = remember(items, selectedCategory, searchQuery, searchType, searchTarget, sortBy) {
        var resultList = items

        // 1. Filter by tab category
        if (selectedCategory != "الكل") {
            resultList = resultList.filter { it.category == selectedCategory }
        }

        // 2. Apply search query
        if (searchQuery.isNotBlank()) {
            val queryClean = searchQuery.trim().lowercase()
            resultList = resultList.filter { item ->
                val contentClean = item.content.lowercase()
                val categoryClean = item.category.lowercase()

                val matchesText = when (searchType) {
                    "starts_with" -> {
                        when (searchTarget) {
                            "content" -> contentClean.startsWith(queryClean)
                            "category" -> categoryClean.startsWith(queryClean)
                            else -> contentClean.startsWith(queryClean) || categoryClean.startsWith(queryClean)
                        }
                    }
                    "exact_word" -> {
                        val words = contentClean.split("\\s+".toRegex())
                        when (searchTarget) {
                            "content" -> words.contains(queryClean)
                            "category" -> categoryClean == queryClean
                            else -> words.contains(queryClean) || categoryClean == queryClean
                        }
                    }
                    else -> { // contains
                        when (searchTarget) {
                            "content" -> contentClean.contains(queryClean)
                            "category" -> categoryClean.contains(queryClean)
                            else -> contentClean.contains(queryClean) || categoryClean.contains(queryClean)
                        }
                    }
                }
                matchesText
            }
        }

        // 3. Apply sorting
        when (sortBy) {
            "oldest" -> resultList.sortedBy { it.id }
            "alphabetical" -> resultList.sortedBy { it.content }
            else -> resultList.sortedByDescending { it.id } // newest
        }
    }

    val categoryStats = remember(items) {
        items.groupBy { it.category }.mapValues { it.value.size }
    }

    // Standard high-contrast TextField styling
    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedLabelColor = WarmGold,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        focusedBorderColor = WarmGold,
        unfocusedBorderColor = WarmGold.copy(alpha = 0.4f),
        cursorColor = WarmGold
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TopAppBar
        TopAppBar(
            title = {
                Column {
                    Text("الحافظة العائلية الذكية", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = WarmGold)
                    Text("مراقبة وتصنيف النصوص تلقائياً", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WarmGold)
                }
            },
            actions = {
                IconButton(onClick = { showCategoryMgmtDialog = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "إدارة التصنيفات", tint = WarmGold)
                }
                IconButton(onClick = { showReportDialog = true }) {
                    Icon(Icons.Default.Assessment, contentDescription = "Report", tint = WarmGold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        // Live Background Monitor Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (isServiceActive) WarmGold else WarmGold.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (isServiceActive) Color(0xFF4CAF50) else Color(0xFFF44336))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            if (isServiceActive) "مراقبة الخلفية نشطة" else "المراقبة متوقفة حالياً",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (isServiceActive) "انسخ أي نص في أي تطبيق لحفظه وتصنيفه تلقائياً" else "شغل الخدمة لمراقبة الحافظة والنسخ السريع",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Switch(
                    checked = isServiceActive,
                    onCheckedChange = { active ->
                        val intent = Intent(context, ClipboardMonitorService::class.java)
                        if (active) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                            Toast.makeText(context, "تم بدء الخدمة ومراقبة الحافظة!", Toast.LENGTH_SHORT).show()
                        } else {
                            val stopIntent = Intent(context, ClipboardMonitorService::class.java).apply {
                                action = "STOP"
                            }
                            context.startService(stopIntent)
                            Toast.makeText(context, "تم إيقاف المراقبة الخلفية", Toast.LENGTH_SHORT).show()
                        }
                        isServiceActive = active
                        sharedPrefs.edit()
                            .putBoolean("clip_monitor_active", active)
                            .apply()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MidnightBlue,
                        checkedTrackColor = WarmGold
                    )
                )
            }
        }

        // Manual addition panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("إضافة نص جديد يدوياً للحافظة:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WarmGold)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("اكتب أو الصق النص هنا ليتم تصنيفه...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = tfColors
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        OutlinedButton(
                            onClick = { menuExpanded = true },
                            border = BorderStroke(1.dp, WarmGold),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Category, contentDescription = null, tint = WarmGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("التصنيف: $categoryInput", color = WarmGold, fontSize = 11.sp)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            customCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        categoryInput = cat
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                onAddItem(textInput, categoryInput)
                                textInput = ""
                                Toast.makeText(context, "تم الحفظ بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MidnightBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إرسال", color = MidnightBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // ADVANCED SEARCH PANEL (البحث المتقدم)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = WarmGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("البحث المتقدم في الحافظة", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WarmGold)
                    }
                    IconButton(
                        onClick = { showAdvancedSearch = !showAdvancedSearch },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (showAdvancedSearch) Icons.Default.Close else Icons.Default.FilterList,
                            contentDescription = "عرض الفلاتر المتقدمة",
                            tint = WarmGold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن أي كلمة أو عبارة...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = tfColors,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = WarmGold)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح", tint = WarmGold)
                            }
                        }
                    },
                    singleLine = true
                )

                AnimatedVisibility(
                    visible = showAdvancedSearch,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Divider(color = WarmGold.copy(alpha = 0.2f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Search Mode Options
                        Text("نوع المطابقة:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WarmGold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "contains" to "يحتوي",
                                "starts_with" to "يبدأ بـ",
                                "exact_word" to "كلمة كاملة"
                            ).forEach { (mode, label) ->
                                val isSelected = searchType == mode
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { searchType = mode },
                                    label = { Text(label, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = WarmGold,
                                        selectedLabelColor = MidnightBlue
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Target Fields Options
                        Text("نطاق البحث المتخصص:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WarmGold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "all" to "كل الحقول",
                                "content" to "المحتوى فقط",
                                "category" to "التصنيف فقط"
                            ).forEach { (target, label) ->
                                val isSelected = searchTarget == target
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { searchTarget = target },
                                    label = { Text(label, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = WarmGold.copy(0.2f),
                                        selectedLabelColor = WarmGold,
                                        containerColor = Color.Transparent,
                                        labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = if (isSelected) WarmGold else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                        enabled = true,
                                        selected = isSelected
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Sorting Options
                        Text("ترتيب النتائج المسترجعة:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WarmGold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "newest" to "الأحدث أولاً",
                                "oldest" to "الأقدم أولاً",
                                "alphabetical" to "أبجدياً"
                            ).forEach { (sortMode, label) ->
                                val isSelected = sortBy == sortMode
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { sortBy = sortMode },
                                    label = { Text(label, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = WarmGold.copy(0.2f),
                                        selectedLabelColor = WarmGold,
                                        containerColor = Color.Transparent,
                                        labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = if (isSelected) WarmGold else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                        enabled = true,
                                        selected = isSelected
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Category selection scroller with Dynamic categories
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categoriesForTabs) { cat ->
                val isSelected = selectedCategory == cat
                val bgColor = if (isSelected) WarmGold else MaterialTheme.colorScheme.surface
                val txtColor = if (isSelected) MidnightBlue else MaterialTheme.colorScheme.onSurface

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(cat, color = txtColor, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        if (cat != "الكل" && (categoryStats[cat] ?: 0) > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(txtColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                  Text(
                                    (categoryStats[cat] ?: 0).toString(),
                                    color = txtColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Clipboard elements list
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "لا توجد عناصر محفوظة في هذا التصنيف حالياً.\nشغّل مراقبة الخلفية ثم انسخ نصوصك من أي مكان لتظهر هنا تلقائياً!",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredItems) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Open detailed reader instead of just silently copying
                                selectedClipForReader = item
                            },
                        border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Badge(
                                    containerColor = WarmGold.copy(alpha = 0.2f),
                                    contentColor = WarmGold
                                ) {
                                    Text(
                                        item.category,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            try {
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, item.content)
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "مشاركة النص عبر:"))
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "المشاركة غير متاحة", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = "Share",
                                            tint = WarmGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(
                                        onClick = { onDeleteItem(item.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                item.content,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("اضغط لفتحه بالقارئ الداخلي وتغيير تصنيفه...", fontSize = 10.sp, color = WarmGold, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    // Dynamic Category Management Dialog
    if (showCategoryMgmtDialog) {
        var newCatInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCategoryMgmtDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = WarmGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إدارة تصنيفات الحافظة", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    Text("يمكنك إضافة تصنيف جديد أو حذف تصنيف مخصص من القائمة الحالية:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newCatInput,
                            onValueChange = { newCatInput = it },
                            placeholder = { Text("تصنيف جديد (مثال: مقالات)") },
                            modifier = Modifier.weight(1f),
                            colors = tfColors,
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                val trimmed = newCatInput.trim()
                                if (trimmed.isNotBlank() && !customCategories.contains(trimmed) && trimmed != "الكل") {
                                    val updated = customCategories + trimmed
                                    customCategories = updated.sorted()
                                    sharedPrefs.edit()
                                        .putStringSet("clipboard_custom_categories", updated.toSet())
                                        .apply()
                                    newCatInput = ""
                                    Toast.makeText(context, "تمت إضافة التصنيف بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("أضف", color = MidnightBlue, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = WarmGold.copy(0.3f))
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(customCategories) { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cat, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)

                                val isDeletable = cat != "عام"
                                if (isDeletable) {
                                    IconButton(
                                        onClick = {
                                            val updated = customCategories - cat
                                            customCategories = updated.sorted()
                                            sharedPrefs.edit()
                                                .putStringSet("clipboard_custom_categories", updated.toSet())
                                                .apply()
                                            Toast.makeText(context, "تم حذف التصنيف", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Category", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showCategoryMgmtDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold)
                ) {
                    Text("حفظ وإغلاق", color = MidnightBlue, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Detailed Reader & Sorter Window for Long Text
    if (selectedClipForReader != null) {
        val currentItem = selectedClipForReader!!
        var readerMenuExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { selectedClipForReader = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = WarmGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("القارئ والفرز الداخلي", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 16.sp)
                    }
                    Badge(
                        containerColor = WarmGold.copy(alpha = 0.2f),
                        contentColor = WarmGold
                    ) {
                        Text(currentItem.category, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                ) {
                    // Sorting Control Panel inside reader
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                        border = BorderStroke(0.5.dp, WarmGold.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("فرز هذا النص إلى:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Box {
                                OutlinedButton(
                                    onClick = { readerMenuExpanded = true },
                                    border = BorderStroke(1.dp, WarmGold),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(currentItem.category, color = WarmGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = WarmGold, modifier = Modifier.size(12.dp))
                                }
                                DropdownMenu(
                                    expanded = readerMenuExpanded,
                                    onDismissRequest = { readerMenuExpanded = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    customCategories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp) },
                                            onClick = {
                                                val updatedItem = currentItem.copy(category = cat)
                                                onUpdateItem(updatedItem)
                                                selectedClipForReader = updatedItem
                                                readerMenuExpanded = false
                                                Toast.makeText(context, "تم تصنيف وفرز النص بنجاح إلى: $cat", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Markdown Parser Reader Container
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                            .border(BorderStroke(0.5.dp, WarmGold.copy(alpha = 0.25f)), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        SimpleMarkdownReader(text = currentItem.content)
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("ektefaa_clipboard", currentItem.content)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم النسخ من القارئ!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "خطأ بالنسخ", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نسخ النص", color = MidnightBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { selectedClipForReader = null },
                        border = BorderStroke(1.dp, WarmGold),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إغلاق القارئ", color = WarmGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Comprehensive Stats and Classification Report Dialog
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Assessment, contentDescription = null, tint = WarmGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("📊 تقرير تصنيفات الحافظة", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "إجمالي النصوص المحفوظة بالمنصة: ${items.size}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Divider(color = WarmGold.copy(0.3f))

                    // Listing the custom dynamic categories count
                    customCategories.forEach { cat ->
                        val count = categoryStats[cat] ?: 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cat, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(WarmGold.copy(0.12f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    count.toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = WarmGold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showReportDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = WarmGold)
                ) {
                    Text("إغلاق", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun SimpleMarkdownReader(text: String, modifier: Modifier = Modifier) {
    val lines = text.split("\n")
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        lines.forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("# ") -> {
                    Text(
                        text = trimmed.substring(2),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarmGold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = trimmed.substring(3),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarmGold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    )
                }
                trimmed.startsWith("* ") -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = trimmed.substring(2),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.weight(1f)
                        )
                        Text("  •", fontSize = 14.sp, color = WarmGold, fontWeight = FontWeight.Bold)
                    }
                }
                trimmed.startsWith("- ") -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = trimmed.substring(2),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.weight(1f)
                        )
                        Text("  •", fontSize = 14.sp, color = WarmGold, fontWeight = FontWeight.Bold)
                    }
                }
                trimmed.isBlank() -> {
                    Spacer(modifier = Modifier.height(3.dp))
                }
                else -> {
                    Text(
                        text = line,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

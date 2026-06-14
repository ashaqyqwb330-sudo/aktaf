package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.DebtItem
import com.example.database.ExpenseLog
import com.example.ui.theme.MidnightBlue
import com.example.ui.theme.WarmGold
import com.example.ui.theme.GreenAccent
import com.example.ui.theme.RedAccent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancesScreen(
    currentRole: String,
    expenseLogs: List<ExpenseLog>,
    debtItems: List<DebtItem>,
    onBack: () -> Unit,
    onAddExpense: (String, Double, String, String, Long) -> Unit,
    onDeleteExpense: (Int) -> Unit,
    onAddDebt: (String, String, Double, Boolean, String) -> Unit,
    onDeleteDebt: (Int) -> Unit
) {
    val context = LocalContext.current
    var isAuthenticated by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    // Bypass check for simplicity if role is already Father in current session
    val requirePin = !isAuthenticated && currentRole != "Father"

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

    if (requirePin) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = WarmGold, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("صفحة المالية والمصروفات والديون العائلية", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = WarmGold)
            Text("هذه الصفحة مخصصة للأب الأب المسؤول فقط. يرجى إدخال رمز الأمان للمتابعة لسرية البيانات.", textAlign = TextAlign.Center, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(20.dp))
            
            OutlinedTextField(
                value = pinInput,
                onValueChange = { pinInput = it },
                label = { Text("رمز أمان الأب المسؤول") },
                visualTransformation = PasswordVisualTransformation(),
                colors = tfColors,
                modifier = Modifier.fillMaxWidth(0.9f)
            )
            if (errorMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (pinInput == "1234") {
                        isAuthenticated = true
                        errorMessage = ""
                    } else {
                        errorMessage = "رمز المرور غير صحيح! الرمز الافتراضي هو: 1234"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Text("تأكيد الدخول", color = MidnightBlue, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(onClick = onBack) {
                Text("العودة للخلف", color = WarmGold, fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    // Normal Finance view once unlocked
    var financeTab by remember { mutableStateOf(0) } // 0 = مصروفات وواردات, 1 = ديون البقالة, 2 = الكشف الختامي والتقارير
    var showReportDialog by remember { mutableStateOf(false) }

    // Calculating totals
    val totalExpense = expenseLogs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val totalIncome = expenseLogs.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalDebts = debtItems.sumOf { it.amount }
    val netBalance = totalIncome - totalExpense - totalDebts

    // Helper to generate text report
    val generateFinancialReportText: () -> String = {
        buildString {
            append("=========================================\n")
            append("     📝 كشف الحساب المالي والختامي للأسرة     \n")
            append("=========================================\n")
            append("تاريخ التقرير: ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}\n")
            append("الحالة العائلية: معتمد ومطابق للمعايير\n")
            append("-----------------------------------------\n\n")
            append("==== [ الملخص والمركز المالي الختامي ] ====\n")
            append("  - إجمالي الإيرادات والواردات: ${"%.2f".format(totalIncome)} ريال\n")
            append("  - إجمالي منصرفات العائلة: ${"%.2f".format(totalExpense)} ريال\n")
            append("  - إجمالي الديون المستحقة المتبقية: ${"%.2f".format(totalDebts)} ريال\n")
            append("  ---------------------------------------\n")
            append("  ★ صافي المركز المالي الحر للأسرة: ${"%.2f".format(netBalance)} ريال\n\n")
            
            append("=========================================\n")
            append("        📋 كشف المنصرفات والواردات اليومية       \n")
            append("=========================================\n")
            append("   التاريخ   | التصنيف |   المبلغ  | البيان والملاحظات\n")
            append("-----------------------------------------\n")
            if (expenseLogs.isEmpty()) {
                append("  (لا توجد قيود مصروفات مسجلة حالياً)\n")
            } else {
                expenseLogs.forEach { log ->
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(log.timestamp))
                    val typeChar = if (log.type == "EXPENSE") "-" else "+"
                    append(String.format("  %s | %-7s | %s%7.1f | %s\n", dateStr, log.category, typeChar, log.amount, log.note))
                }
            }
            append("-----------------------------------------\n\n")
            
            append("=========================================\n")
            append("        💔 كشف الديون والفواتير المستحقة        \n")
            append("=========================================\n")
            append("  المحل التجاري |  المبلغ المطلوب  | موعد وتاريخ السداد المتوقع\n")
            append("-----------------------------------------\n")
            if (debtItems.isEmpty()) {
                append("  (الحمد لله لا توجد فواتير ديون معلقة على العائلة)\n")
            } else {
                debtItems.forEach { debt ->
                    append(String.format("  %-13s | %10.1f ريال | %s\n", debt.shopName, debt.amount, debt.dueDate))
                }
            }
            append("-----------------------------------------\n")
            append("ملاحظة هامة: يجب توجيه الفائض المالي لسداد مطالبات الدائنين أولاً.\n")
            append("=========================================\n")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("لوحة الحسابات والمالية العائلية", fontWeight = FontWeight.Bold, color = WarmGold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = WarmGold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        // Finances Tabs Selection
        TabRow(
            selectedTabIndex = financeTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = WarmGold
        ) {
            Tab(selected = financeTab == 0, onClick = { financeTab = 0 }) {
                Text("📋 اليومي والمصروفات", modifier = Modifier.padding(10.dp), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            }
            Tab(selected = financeTab == 1, onClick = { financeTab = 1 }) {
                Text("💔 الديون والمشتريات", modifier = Modifier.padding(10.dp), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            }
            Tab(selected = financeTab == 2, onClick = { financeTab = 2 }) {
                Text("📈 التقرير والكشف الختامي", modifier = Modifier.padding(10.dp), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Balance Summary Hero Widget
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightBlue),
            border = BorderStroke(1.5.dp, WarmGold)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("صافي رصيد الأسرة والخزينة الحرة مجمعاً:", color = WarmGold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        "${"%.2f".format(netBalance)} ريال يمني",
                        fontWeight = FontWeight.Bold,
                        color = if (netBalance >= 0) GreenAccent else RedAccent,
                        fontSize = 20.sp
                    )
                    IconButton(
                        onClick = { showReportDialog = true },
                        modifier = Modifier.background(WarmGold, RoundedCornerShape(8.dp)).size(36.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = MidnightBlue, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (financeTab) {
            0 -> {
                // Section 0: Expenses Ledger
                var type by remember { mutableStateOf("EXPENSE") } // "EXPENSE" or "INCOME"
                var amountStr by remember { mutableStateOf("") }
                var category by remember { mutableStateOf("") }
                var note by remember { mutableStateOf("") }
                var customDateInput by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.4f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("تسجيل وتوثيق المعاملة المالية بالتاريخ:", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 12.sp)
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { type = "EXPENSE" },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (type == "EXPENSE") RedAccent else MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, if (type == "EXPENSE") RedAccent else WarmGold.copy(0.4f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("مصروف عائلي", color = if (type == "EXPENSE") Color.White else WarmGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { type = "INCOME" },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (type == "INCOME") GreenAccent else MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, if (type == "INCOME") GreenAccent else WarmGold.copy(0.4f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("دخل ووارد", color = if (type == "INCOME") MidnightBlue else WarmGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = amountStr,
                                    onValueChange = { amountStr = it },
                                    placeholder = { Text("المبلغ (ريال)") },
                                    colors = tfColors,
                                    modifier = Modifier.weight(1.2f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = category,
                                    onValueChange = { category = it },
                                    placeholder = { Text("التصنيف (مدرسة/بيت)") },
                                    colors = tfColors,
                                    modifier = Modifier.weight(1.8f),
                                    singleLine = true
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = note,
                                    onValueChange = { note = it },
                                    placeholder = { Text("البيان والملاحظة") },
                                    colors = tfColors,
                                    modifier = Modifier.weight(1.8f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = customDateInput,
                                    onValueChange = { customDateInput = it },
                                    placeholder = { Text("التاريخ YYYY-MM-DD") },
                                    colors = tfColors,
                                    modifier = Modifier.weight(1.2f),
                                    singleLine = true
                                )
                            }

                            Button(
                                onClick = {
                                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                                    if (amt > 0.0 && category.isNotBlank()) {
                                        val parseFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                        val specificTime = try {
                                            parseFormat.parse(customDateInput)?.time ?: System.currentTimeMillis()
                                        } catch (e: Exception) {
                                            System.currentTimeMillis()
                                        }
                                        
                                        onAddExpense(type, amt, category, note, specificTime)
                                        amountStr = ""
                                        category = ""
                                        note = ""
                                        Toast.makeText(context, "تم تسجيل وإدخال المعاملة بنجاح بالحسابات!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "الرجاء كمالة قيم المبلغ والصنف مسبقاً!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("تأكيد وحفظ القيد المالي", color = MidnightBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("السجل العام الحسابي التنازلي:", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(expenseLogs) { item ->
                            val color = if (item.type == "EXPENSE") RedAccent else GreenAccent
                            val formatDay = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(item.timestamp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(item.category + " | " + item.note, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(if (item.type == "EXPENSE") "مصرف عائلي" else "دخل وارد", fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("التاريخ: $formatDay", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${if (item.type == "EXPENSE") "-" else "+"}${item.amount} ريال", color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(onClick = { onDeleteExpense(item.id) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = RedAccent, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // Section 1: Debts and Grocery Stores Tracking
                var shopName by remember { mutableStateOf("") }
                var desc by remember { mutableStateOf("") }
                var amountStr by remember { mutableStateOf("") }
                var dueDate by remember { mutableStateOf("") }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.4f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("تسجيل وتأريخ دين أو فاتورة محل جديدة:", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 12.sp)
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = shopName,
                                    onValueChange = { shopName = it },
                                    placeholder = { Text("المحل / الدائن أو البقالة") },
                                    colors = tfColors,
                                    modifier = Modifier.weight(1.5f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = amountStr,
                                    onValueChange = { amountStr = it },
                                    placeholder = { Text("المبلغ المطلوب") },
                                    colors = tfColors,
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                            OutlinedTextField(
                                value = desc,
                                onValueChange = { desc = it },
                                placeholder = { Text("بيان المشتريات (مثال: مستلزمات غذاء ودقيق عائلي)") },
                                colors = tfColors,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = dueDate,
                                onValueChange = { dueDate = it },
                                placeholder = { Text("تاريخ السداد المحدد (مثال: نهاية شهر يوليو)") },
                                colors = tfColors,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                                    if (shopName.isNotBlank() && amt > 0.0) {
                                        onAddDebt(shopName, desc, amt, false, dueDate.ifBlank { "غير محدد" })
                                        shopName = ""
                                        desc = ""
                                        amountStr = ""
                                        dueDate = ""
                                        Toast.makeText(context, "تم قيد وبناء الدين المسحوب بنجاح!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "يرجى تعبئة الدائن والمبلغ المطلوبات!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("تقرير وحفظ فاتورة الدين العام", color = MidnightBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("كشف مطالبات الديون المعلقة الحالية:", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(debtItems) { debt ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.5f)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(debt.shopName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WarmGold)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("${debt.amount} ريال", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RedAccent)
                                            IconButton(onClick = { onDeleteDebt(debt.id) }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = RedAccent, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("البيان: " + debt.desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("موعد وتاريخ السداد: " + debt.dueDate, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // Section 2: Detailed visual totals, Ledger report & savings
                val targetAmount = 50000.0
                val progress = if (netBalance > 0) (netBalance / targetAmount).toFloat().coerceIn(0f..1f) else 0f
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("تقرير كشف الحساب والمركز المالي الختامي للأسرة:", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("إجمالي المداخيل والمنح الواردة:", fontSize = 12.sp)
                                Text("$totalIncome ريال يمني", color = GreenAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("إجمالي مصاريف العائلة والمشتريات:", fontSize = 12.sp)
                                Text("$totalExpense ريال يمني", color = RedAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("إجمالي الديون والالتزامات للغير:", fontSize = 12.sp)
                                Text("$totalDebts ريال يمني", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Divider(color = WarmGold.copy(0.3f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("المركز الحقيقي الصافي المتبقي:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("$netBalance ريال يمني", color = if (netBalance>=0) GreenAccent else RedAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("صندوق المدخرات وبناء الأهداف المشترك العائلي:", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Savings, contentDescription = null, tint = WarmGold, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("نسبة بناء الأهداف والتوفير المالي:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            LinearProgressIndicator(
                                progress = { progress },
                                color = WarmGold,
                                trackColor = WarmGold.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("النسبة المئوية: ${(progress * 100).toInt()}% (المستهدف: $targetAmount ريال يمني)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WarmGold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, WarmGold.copy(0.3f)),
                        colors = CardDefaults.cardColors(containerColor = WarmGold.copy(0.08f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("توجيه مجمع الرقابة والمالية:", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("الكشف الختامي يضع أسس متينة لقرارات الشراء القادمة. بضط وتنظيم المصاريف وقيد الفواتير أولاً بأول يحمي استدامة معيشة أفراد الأسرة ويعينهم.", fontSize = 11.sp, lineHeight = 16.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Word Document Design styled Invoice and financial popup
    if (showReportDialog) {
        val reportContent = remember(expenseLogs, debtItems, totalIncome, totalExpense, totalDebts, netBalance) {
            generateFinancialReportText()
        }
        
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CardMembership, contentDescription = null, tint = WarmGold)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("📝 كشف الحساب والتقرير الختامي (Word)", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 15.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("التقرير منسق ومجهّز للتصدير المباشر بأسلوب جداول وسطور الجرد المالي للطباعة الفورية والمطالبة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.8f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MidnightBlue)
                            .border(BorderStroke(1.dp, WarmGold.copy(0.4f)), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            reportContent,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = Color.White,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
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
                                val clip = ClipData.newPlainText("finances_word_report", reportContent)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ كشف الحساب المتكامل المطبوع إلى الحافظة!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "خطأ بالنسخ", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نسخ المعاملة", color = MidnightBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = {
                            try {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, reportContent)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "مشاركة الفاتورة والكشف الختامي عبر:"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "المشاركة غير متاحة", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مشاركة الكشف", color = MidnightBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("إغلاق", color = WarmGold, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

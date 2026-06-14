package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MidnightBlue
import com.example.ui.theme.WarmGold
import com.example.ui.theme.GreenAccent
import com.example.ui.theme.RedAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(onBack: () -> Unit) {
    var calculatorTab by remember { mutableStateOf(0) } // 0 = آلة علمية, 1 = حل مسائل بالصورة والنص

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("الحاسبة الذكية العائلية والحلول", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        TabRow(
            selectedTabIndex = calculatorTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = WarmGold
        ) {
            Tab(selected = calculatorTab == 0, onClick = { calculatorTab = 0 }) {
                Text("منصة الآلة علمية", modifier = Modifier.padding(10.dp), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            }
            Tab(selected = calculatorTab == 1, onClick = { calculatorTab = 1 }) {
                Text("محلل ومفسر المسائل الذكي", modifier = Modifier.padding(10.dp), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (calculatorTab) {
            0 -> {
                // Calculator pad
                var calcInput by remember { mutableStateOf("") }
                var calcResult by remember { mutableStateOf("0") }

                Column(modifier = Modifier.padding(16.dp)) {
                    // Result display screen
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        border = BorderStroke(1.5.dp, WarmGold),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(calcInput.ifBlank { "0" }, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text(calcResult, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = WarmGold)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Buttons list
                    val buttons = listOf(
                        "C", "sin", "cos", "/",
                        "7", "8", "9", "*",
                        "4", "5", "6", "-",
                        "1", "2", "3", "+",
                        "0", ".", "^", "="
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(buttons) { btn ->
                            val isAction = btn == "=" || btn == "C"
                            val isOperator = btn == "/" || btn == "*" || btn == "-" || btn == "+" || btn == "sin" || btn == "cos"

                            val containerColor = if (isAction) WarmGold else if (isOperator) WarmGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                            val contentColor = if (isAction) MidnightBlue else if (isOperator) WarmGold else MaterialTheme.colorScheme.onSurface

                            Box(
                                modifier = Modifier
                                    .aspectRatio(1.2f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(containerColor)
                                    .border(BorderStroke(1.dp, WarmGold.copy(alpha = 0.3f)), RoundedCornerShape(12.dp))
                                    .clickable {
                                        when (btn) {
                                            "C" -> {
                                                calcInput = ""
                                                calcResult = "0"
                                            }
                                            "=" -> {
                                                try {
                                                    // Simple math parser dummy logic
                                                    if (calcInput.contains("sin")) {
                                                        calcResult = "0.866"
                                                    } else if (calcInput.contains("cos")) {
                                                        calcResult = "0.5"
                                                    } else if (calcInput.isNotBlank()) {
                                                        // Evaluate numbers
                                                        calcResult = "النتيجة: 42"
                                                    }
                                                } catch (e: Exception) {
                                                    calcResult = "خطأ"
                                                }
                                            }
                                            else -> {
                                                calcInput += btn
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(btn, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = contentColor)
                            }
                        }
                    }
                }
            }
            1 -> {
                // OCR Math photo scanner simulator
                var textQuery by remember { mutableStateOf("") }
                var solutionSteps by remember { mutableStateOf("قم بكتابة المعادلة، أو اضغط زر الكاميرا لمسح المسألة وتفكيك الحلول بالتفصيل.") }
                var cameraActive by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.5f))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("الخطوات التوضيحية للمسألة:", fontWeight = FontWeight.Bold, color = WarmGold)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(solutionSteps, fontSize = 13.sp, lineHeight = 20.sp)
                        }
                    }

                    if (cameraActive) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            border = BorderStroke(1.5.dp, WarmGold),
                            colors = CardDefaults.cardColors(containerColor = MidnightBlue)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = WarmGold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("جاري فحص المسألة وتطبيق فحص الكاميرا (OCR)...", fontSize = 12.sp, color = WarmGold)
                                TextButton(
                                    onClick = {
                                        cameraActive = false
                                        solutionSteps = "نتيجة فحص الذكاء الاصطناعي (مادة الرياضيات):\nالمعادلة: 2x + 5 = 15\nحل الخطوات:\n1) اطرح 5 من الطرفين: 2x = 10\n2) اقسم على 2: x = 5"
                                    }
                                ) {
                                    Text("التقاط لقطة للمسألة المكتوبة", color = WarmGold)
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = { cameraActive = true },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MidnightBlue
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("مسح المسابقات والكتاب بالترجمة الكاميرا", color = MidnightBlue, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = textQuery,
                            onValueChange = { textQuery = it },
                            placeholder = { Text("أو اكتب المعادلة هنا (x^2 - 4 = 0)...") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmGold)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            colors = IconButtonDefaults.iconButtonColors(containerColor = WarmGold),
                            onClick = {
                                if (textQuery.isNotBlank()) {
                                    solutionSteps = "خطوات حل المسألة المكتوبة \"$textQuery\":\nنعزل المتغيرات إلى طرف ونبسط النتيجة لتخرج بدقة ممتازة لمراجعة الطالب."
                                    textQuery = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Functions, contentDescription = null, tint = MidnightBlue)
                        }
                    }
                }
            }
        }
    }
}

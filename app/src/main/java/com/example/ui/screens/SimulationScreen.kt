package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OfflineBolt
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
import com.example.database.ScenarioResultEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class SimulationScenario(
    val id: Int,
    val title: String,
    val crisisText: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { EktefaaDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    val scenarios = remember {
        listOf(
            SimulationScenario(
                id = 1,
                title = "مكافحة التضليل الحربي المعرفي",
                crisisText = "تلقيت خبراً مرعباً ومجهول المصدر عبر مجموعة واتساب يزعم وقوع خطب جلل في خطوط المواجهة، والجميع يقوم بمشاركته بذعر شديد. ماذا تفعل؟",
                options = listOf(
                    "أقوم بإعادة مشاركته فوراً في بقية المجموعات لأحذر الأهل والأصدقاء وأبدو بمظهر المطلع والسباق.",
                    "أتجاهل الخبر تماماً، وأقوم فوراً بوعظ المجموعة بعدم نقل الشائعات، مبصراً إياهم بآية (يا أيها الذين آمنوا إن جاءكم فاسق بنبأ فتبينوا).",
                    "أرد بموجة من السخرية أو الهلع وأقوم بإشعاص المخاوف ولقاء التهم جزافاً."
                ),
                correctIndex = 1,
                explanation = "الخيار الصائب هو التثبت والنهي الصارم عن الإرجاف والتضليل لما له من تدمير معنوي خطير يخدم استخبارات وخطط الأعداء."
            ),
            SimulationScenario(
                id = 2,
                title = "صيانة الأمانة وبيت المال",
                crisisText = "كُلفت بالإشراف على توزيع معونات عائلية وأموال في منطقتك، وطلب منك قريب عزيز ومحتاج جداً تقديم دوره واستثنائه ببعض المغانم بشكل خاص وسري. ماذا تختار؟",
                options = listOf(
                    "أرفض تماماً، ملتزماً بمعايير العدالة المستوحاة من هدى الله: (إن الله يأمر بالعدل والإحسان)، فالأمانة مطلقة ولا تهاون في المحسوبية.",
                    "أعطيه طلباً استثنائياً كبيراً سراً مستنداً لمبرر صلة الرحم وكسب قلوب الأقارب الأقربين.",
                    "أنقل الإشراف لشخص آخر متقاعس لأتهرب من المسؤولية والحرج وتأنيب الضمير."
                ),
                correctIndex = 0,
                explanation = "الأمانة في غسيل أو استخدام سبيل الله بيت مال الضعفاء مطلقة، والتقديم لصلة الرحم بغير وجه حق هو من الكبائر المفسدة."
            ),
            SimulationScenario(
                id = 3,
                title = "محور الاستجابة والمبادرة الذاتية",
                crisisText = "حدث تسريب في خطوط تمديدات الصرف الصحي والبلدية في حيك السكني، فتقاعس الجميع منتظرين تدخل المشرف أو عمدة الحي. ما السلوك النبيل؟",
                options = listOf(
                    "أبقى جالساً بالبيت أشتكي من تقاعس الدولة وتخلف العائلات الأخرى المحيطة.",
                    "أجمع الأبناء وأبادر فوراً بشراء مستلزمات الصيانة والوقوف لتطويق التسريب عملياً كحواريين دون انتظار أحد.",
                    "أنشر مقطع فيديو وصم وشتائم على الإنترنت لسب جميع المحيطين في بلدتي."
                ),
                correctIndex = 1,
                explanation = "الاستجابة الفردية وعدم انتظار المتقاعسين هو لب 'برنامج حواريي رجال الله'. المبادرة الإيمانية تولد همة جماعية حاسمة."
            ),
            SimulationScenario(
                id = 4,
                title = "الانضباط والتسليم لتوجيهات القيادة",
                crisisText = "صدر توجيه صريح من القيادة بمقاطعة بضاعة معينة للأعداء، ولكنك تجد صعوبة شخصية وتفضل مذاق أو جودة هذا المنتج بشدة. كيف تزن الموقف؟",
                options = listOf(
                    "أستمر بشرائها سراً تذرعاً بأن تأثير الفرد الواحد غير ملموس في الحرب الاقتصادية الكلية.",
                    "ألتزم بالمقاطعة فوراً وبجرأة تامة مستشعراً التسليم الواعي والمسؤول، مرتقياً بنظرتي من ترف النفس للأفق الاستراتيجي العالي.",
                    "أحاول مجادلة المحيطين وإقناعهم لتبخيس دور المقاطعة لتبرير مواصلة شرائي الفاضح."
                ),
                correctIndex = 1,
                explanation = "التسليم العملي هو محك التزكية والارتقاء. كل منتج يدعم جيوش الاستكبار تساهم أمواله في سفك دماء المستضعفين والأبرياء."
            ),
            SimulationScenario(
                id = 5,
                title = "الورع والحماية من حياة البذخ والترف",
                crisisText = "كسبت مبلغ مالي طارئ، واشتدت رغبتك في شراء أثاث فخم غاية في مظاهر البذخ والخيلاء لتتباهى به العائلة أمام الضيوف. ما الخيار الأبرق؟",
                options = listOf(
                    "أشتريه متباهياً مستعرضاً الثراء الفاره لتسجيل حضور مجتمعي بين الجيران والأقارب.",
                    "أوفر الأساسيات البسيطة النبيلة، وأوجه بقية الفائض المالي لدعم صندوق التثبيت العائلي ومساندة الضعفاء من غارمي ديون وصيانة المنازل المتهالكة.",
                    "أنفق نصفه على الاستعراض والآخر على التوفير ممسكاً العصا من المنتصف."
                ),
                correctIndex = 1,
                explanation = "الحذر من الترف ومظاهر البذخ من معايير القرآن: (وإذا أردنا أن نهلك قرية أمرنا مترفيها ففسقوا فيها). تلمس ثغرات الناس كفاح للشدة."
            )
        )
    }

    var currentScenarioIndex by remember { mutableStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf(-1) }
    var evaluationResultText by remember { mutableStateOf("") }
    var hasAnswered by remember { mutableStateOf(false) }

    val currentScenario = scenarios[currentScenarioIndex]

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
                    "🎭 محاكاة الأزمات والمواقف الصعبة",
                    color = TextLight,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "اختبر بصيرتك وقرارك الإيماني والوعي العملي",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                // Scenario selection indicator header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    scenarios.forEach { sc ->
                        val isCurrent = sc.id - 1 == currentScenarioIndex
                        val isPassed = sc.id - 1 < currentScenarioIndex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isCurrent) WarmGold else if (isPassed) GreenAccent else SurfaceDark
                                )
                        )
                    }
                }
            }

            // Crisis Scenario Description Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, WarmGold.copy(0.3f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.OfflineBolt, contentDescription = "أزمة", tint = WarmGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "السيناريو #${currentScenario.id} : ${currentScenario.title}",
                                color = WarmGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentScenario.crisisText,
                            color = TextLight,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Justify
                        )
                    }
                }
            }

            // Options List
            item {
                Text(
                    "ما هي خطوتك الإيمانية الواعية والناهضة؟",
                    color = TextLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            items(currentScenario.options.size) { optIdx ->
                val optText = currentScenario.options[optIdx]
                val isSelected = selectedOptionIndex == optIdx
                val isCorrect = optIdx == currentScenario.correctIndex

                val borderColor = if (hasAnswered) {
                    if (isCorrect) GreenAccent else if (isSelected) RedAccent else Color.Transparent
                } else {
                    if (isSelected) WarmGold else Color.Transparent
                }

                val cardBg = if (hasAnswered) {
                    if (isCorrect) GreenAccent.copy(0.08f) else if (isSelected) RedAccent.copy(0.08f) else SurfaceDark
                } else {
                    if (isSelected) WarmGold.copy(0.08f) else SurfaceDark
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        if (!hasAnswered) {
                            selectedOptionIndex = optIdx
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                if (!hasAnswered) {
                                    selectedOptionIndex = optIdx
                                }
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = if (hasAnswered) (if (isCorrect) GreenAccent else RedAccent) else WarmGold,
                                unselectedColor = TextMuted
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = optText,
                            color = if (hasAnswered && isCorrect) GreenAccent else TextLight,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Results of Evaluation
            if (hasAnswered) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (selectedOptionIndex == currentScenario.correctIndex) GreenAccent.copy(0.4f) else RedAccent.copy(0.4f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWarm)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "تفسير الموقف",
                                    tint = if (selectedOptionIndex == currentScenario.correctIndex) GreenAccent else RedAccent
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (selectedOptionIndex == currentScenario.correctIndex) "احسنت! قرار مبطن بالبصيرة" else "تنبه: قرار يحتاج إلى تصحيح!",
                                    color = if (selectedOptionIndex == currentScenario.correctIndex) GreenAccent else RedAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentScenario.explanation,
                                color = TextLight,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Justify
                            )
                        }
                    }
                }
            }

            // Bottom Buttons
            item {
                Spacer(modifier = Modifier.height(8.dp))
                if (!hasAnswered) {
                    Button(
                        onClick = {
                            if (selectedOptionIndex == -1) {
                                Toast.makeText(context, "الرجاء اختيار خيار من الخيارات المطروحة أولاً", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            hasAnswered = true
                            
                            // Save to database
                            scope.launch {
                                db.rajolAllahDao().insertScenarioResult(
                                    ScenarioResultEntity(
                                        scenarioTitle = currentScenario.title,
                                        chosenOption = currentScenario.options[selectedOptionIndex],
                                        isCorrect = selectedOptionIndex == currentScenario.correctIndex
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🗳️ تأكيد قراري وتقييم الخيار بصرياً", color = MidnightBlue, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            if (currentScenarioIndex < scenarios.size - 1) {
                                currentScenarioIndex++
                                selectedOptionIndex = -1
                                hasAnswered = false
                            } else {
                                Toast.makeText(context, "🎉 مبارك! أكملت جميع سيناريوهات المحاكاة الإيمانية بنجاح!", Toast.LENGTH_LONG).show()
                                onBack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (currentScenarioIndex < scenarios.size - 1) WarmGold else GreenAccent),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (currentScenarioIndex < scenarios.size - 1) "الذهاب للسيناريو التالي ◀" else "إنهاء واعتماد تقييم المحاكاة",
                            color = MidnightBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

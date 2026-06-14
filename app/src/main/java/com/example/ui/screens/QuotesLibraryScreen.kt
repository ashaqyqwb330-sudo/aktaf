package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import com.example.ui.theme.*

data class GuidelineQuote(
    val id: Int,
    val text: String,
    val source: String,
    val audioDuration: String,
    val question: String,
    val options: List<String>,
    val correctIdx: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotesLibraryScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    val quotes = remember {
        listOf(
            GuidelineQuote(
                id = 1,
                text = "«إن المسؤولية جماعية ولا يُعفى منها فرداً لمجرد تقاعس مَن حوله. المقياس القرآني المعتمد هو الاستباق والمبادرة والمساهمة الفاعلة في صيانة الكرامة والهوية والتمكين الإيماني المبرر.»",
                source = "توجيهات تزكية الأنفس والقيام بالمسؤولية",
                audioDuration = "03:45",
                question = "ما هو المقياس القرآني المذكور للاستجابة؟",
                options = listOf(
                    "انتظار توجيهات الآخرين والنهوض الجماعي الموحد فقط.",
                    "الاستباق والمبادرة الذاتية الفاعلة دون الالتفات للمتقاعسين.",
                    "تجاهل الأزمة والانسحاب للمصالح الضيقة."
                ),
                correctIdx = 1
            ),
            GuidelineQuote(
                id = 2,
                text = "«الحياة في سبيل الله ليست حياة كسل أو دعة، بل هي بذل مستمر وجهاد مستمر للأهواء والأنانية وشح النفوس بالمال والوقت والنفس حتى تنال كرامة الله ورضاه العظيم.»",
                source = "من هدي القرآن والتزكية النبيلة",
                audioDuration = "05:12",
                question = "بماذا تبتلى وتزكى الأنفس حسب هذه المقولة؟",
                options = listOf(
                    "بالمقاطعة المادية والعزلة التامة عن شؤون الناس.",
                    "بالبذل والجهاد المستمر للأهواء والأنانية وشح النفس.",
                    "بالمظاهر الاستعراضية وجمع ثراء الدنيا."
                ),
                correctIdx = 1
            ),
            GuidelineQuote(
                id = 3,
                text = "«الذين يحملون الوعي بصدق هم كالصخور التي تتحطم عليها شائعات ومؤامرات التضليل. لا تؤثر فيهم العواطف العبثية ولا يخافون لومة لائم في الحق لأنهم معتصمون بالله وبمقياسه الخالد.»",
                source = "حماية الجبهة المعرفية والتثقيف السياسي",
                audioDuration = "04:30",
                question = "بماذا تم تشبيه من يحملون الوعي بصدق؟",
                options = listOf(
                    "كالصخور التي تتحطم عليها شائعات ومؤامرات التضليل والأراجيف.",
                    "كالزجاج الذي يتأكسد بمرور عواصف النفس والرياح.",
                    "بالمتقاعسين المنتظرين لدور الرقيب المشرف."
                ),
                correctIdx = 0
            )
        )
    }

    // Active simulated audio playback state
    var playingId by remember { mutableStateOf(-1) }
    var currentReflectionId by remember { mutableStateOf(-1) }
    var selectedAnswerIdx by remember { mutableStateOf(-1) }
    var answeredCorrectly by remember { mutableStateOf<Boolean?>(null) }

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
                    "📚 مكتبة التوجيهات والدروس لرجال الله",
                    color = TextLight,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "استماع وقراءة فاعلة لتعزيز الرصيد الفكري والتزكية",
                    color = TextMuted,
                    fontSize = 11.sp
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "التدبر وسماع الحكمة",
                            color = WarmGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "عزز وعيك وسر نظرتك الحية بانتظام. استمع للدروس والتنبيهات المسموعة وأجب عن أسئلتها التفاعلية للتأكيد والارتقاء بالفهم.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Justify
                        )
                    }
                }
            }

            // Quotes/Audio Lessons List
            items(quotes) { quote ->
                val isPlaying = playingId == quote.id
                val showingReflection = currentReflectionId == quote.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (isPlaying) WarmGold else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = quote.source,
                                color = WarmGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            
                            // Audio progress button
                            OutlinedButton(
                                onClick = {
                                    playingId = if (isPlaying) -1 else quote.id
                                    if (playingId != -1) {
                                        Toast.makeText(context, "▶ جاري محاكاة بث الصوت العائلي الهادف بتقدير ${quote.audioDuration} دقيقة...", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "⏹ تم إنهاء محاكاة التشغيل الصوتي.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isPlaying) RedAccent else WarmGold),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isPlaying) RedAccent else WarmGold
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "بث",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isPlaying) "إيقاف" else "استماع (${quote.audioDuration})", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = quote.text,
                            color = TextLight,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            textAlign = TextAlign.Justify
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Reflection action button
                        Button(
                            onClick = {
                                if (showingReflection) {
                                    currentReflectionId = -1
                                    selectedAnswerIdx = -1
                                    answeredCorrectly = null
                                } else {
                                    currentReflectionId = quote.id
                                    selectedAnswerIdx = -1
                                    answeredCorrectly = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showingReflection) SurfaceWarm else WarmGold.copy(0.15f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = "فكر",
                                    tint = if (showingReflection) TextLight else WarmGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (showingReflection) "إخفاء الأسئلة التفاعلية" else "💡 اختبار الفهم وتدبر الدرس",
                                    color = if (showingReflection) TextLight else WarmGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Reflection Section Drawer
                        if (showingReflection) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = SurfaceWarm)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "سؤال تفاعلي: ${quote.question}",
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            quote.options.forEachIndexed { optIdx, option ->
                                Card(
                                    onClick = {
                                        if (answeredCorrectly == null) {
                                            selectedAnswerIdx = optIdx
                                        }
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectedAnswerIdx == optIdx) WarmGold.copy(alpha = 0.15f) else SurfaceWarm
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .border(
                                            1.dp,
                                            if (selectedAnswerIdx == optIdx) WarmGold else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    Text(
                                        text = option,
                                        modifier = Modifier.padding(10.dp),
                                        color = if (selectedAnswerIdx == optIdx) WarmGold else TextLight,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Justify
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            
                            if (answeredCorrectly == null) {
                                Button(
                                    onClick = {
                                        if (selectedAnswerIdx == -1) {
                                            Toast.makeText(context, "الرجاء تحديد خيار كجواب!", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        answeredCorrectly = selectedAnswerIdx == quote.correctIdx
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                                    modifier = Modifier.fillMaxWidth().height(32.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("تأكيد إجابتي", color = MidnightBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (answeredCorrectly == true) GreenAccent.copy(0.1f) else RedAccent.copy(0.1f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp,
                                            if (answeredCorrectly == true) GreenAccent else RedAccent,
                                            RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    Text(
                                        text = if (answeredCorrectly == true) "✔️ إجابة بطل سليمة ومطابقة لمعايير الوعي!" else "❌ حاول مرة أخرى وركز في مدلول التوجيه.",
                                        modifier = Modifier.padding(10.dp),
                                        color = if (answeredCorrectly == true) GreenAccent else RedAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

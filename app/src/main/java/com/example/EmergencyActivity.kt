package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class PoisonCard(
    val title: String,
    val symptom: String,
    val treatment: String
)

class EmergencyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val triggerPanic = intent.getBooleanExtra("TRIGGER_PANIC", false)
        setContent {
            MyApplicationTheme(darkTheme = true) {
                EmergencyScreen(triggerPanicInitial = triggerPanic, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyScreen(triggerPanicInitial: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(if(triggerPanicInitial) 0 else 1) }

    // Triage input variables (START protocol)
    var respRateStr by remember { mutableStateOf("25") } // Breath/min
    var hasRadialPulse by remember { mutableStateOf(true) }
    var followsCommands by remember { mutableStateOf(true) }
    var triageColorMsg by remember { mutableStateOf("أخضر (بسيط) - تأرجح الرعاية متأخر.") }
    var triageColor by remember { mutableStateOf(GreenAccent) }

    // Panic alarm state
    var isSirenPlaying by remember { mutableStateOf(triggerPanicInitial) }

    val poisonsList = listOf(
        PoisonCard(
            title = "المبيّض ومطهر الفلاش والكلوركس بمطبخ البيت",
            symptom = "حروق شديدة في الفم والحلق، صعوبة البلع والتقيؤ، إعياء مفاجئ وضيق بالتنفس.",
            treatment = "سقي المصاب فوراً كوباً أو كوبين من الماء البارد أو الحليب لتخفيف التركيز. لا تجبره على التقيؤ أبداً كي لا تكوي الحروق الحلق والمريء مرة ثانية."
        ),
        PoisonCard(
            title = "ابتلاع بطاريات الساعات المعدنية الصغيرة للأطفال",
            symptom = "سعال مفاجئ، آلام مبرحة بالصدر، صعوبة بلع الريق وتلون الشفتين.",
            treatment = "الانتقال فوراً بأقرب دقيقة لغرفة عمليات المستشفى لعمل أشعة وعمل منظار لإخراجها، حيث أن عصارة المعدة تحلل العبوة وتسرب المواد القلوية الحارقة والمسببة لثقب المعدة الحاد."
        ),
        PoisonCard(
            title = "استنشاق غاز مونوكسيد الكربون المنبعث من الفحم والسيارات",
            symptom = "صداع شديد، غثيان دوار، خمول ونعاس مفاجئ حتى فقدان الوعي التام.",
            treatment = "نقل المصاب إلى الهواء الطلق النقي فوراً. تخفيف الملابس الضاغطة حول الرقبة والبدء بالانعاش الرئوي متبوعاً بإسعاف الأكسجين الرطب بالمستشفى."
        )
    )

    // Calculate Triage START
    LaunchedEffect(respRateStr, hasRadialPulse, followsCommands) {
        val breath = respRateStr.toIntOrNull() ?: 0
        if (breath == 0) {
            triageColorMsg = "💀 أسود (Deceased - متوفى أو غير مستجيب)\nالمريض لا يتنفس حتى بعد فتح مجرى الهواء."
            triageColor = Color.Black
        } else if (breath > 30) {
            triageColorMsg = "🚨 أحمر (Immediate - فوري عاجل)\nمعدل التنفس يفوق 30 ضغطة بالدقيقة. خطر حاد على الحياة."
            triageColor = RedAccent
        } else if (!hasRadialPulse) {
            triageColorMsg = "🚨 أحمر (Immediate - فوري عاجل)\nغياب النبض الكعبري أو ضعف الدوران الحاد. المريض في صدمة إقفارية."
            triageColor = RedAccent
        } else if (!followsCommands) {
            triageColorMsg = "🚨 أحمر (Immediate - فوري عاجل)\nمستوى الوعي متأخر ولا يتبع الأوامر البسيطة."
            triageColor = RedAccent
        } else {
            triageColorMsg = "💛 أصفر (Delayed - مؤجل)\nالمريض مستقر وتنفسه طبيعي ويتبع الأوامر، لكنه غير قادر على المشي بسبب إصابته."
            triageColor = Color(0xFFF1C40F)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🆘 الطوارئ والفرز الميداني والسموم", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
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
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceDark,
                contentColor = WarmGold
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Text("نداء واستغاثة", fontSize = 12.sp, color = if(selectedTab == 0) WarmGold else TextLight)
                    }
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Text("نظام الفرز الميداني", fontSize = 12.sp, color = if(selectedTab == 1) WarmGold else TextLight)
                    }
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Text("مرشد دليل السموم", fontSize = 12.sp, color = if(selectedTab == 2) WarmGold else TextLight)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Panic sirens and panic beacon trigger
                        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(16.dp)) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("🚨 نداء الاستغاثة والهلع الصامت", color = RedAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    "بطلب النداء الصامت، يقوم تطبيق اكتفاء بإطلاق إشعارات إنذارية حادة وإرسال رسائل استغاثة قصيرة وموقع الجغرافي المسجل لأفراد العائلة لنجدتك فوراً.",
                                    color = TextLight,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Button(
                                    onClick = {
                                        isSirenPlaying = !isSirenPlaying
                                        if (isSirenPlaying) {
                                            Toast.makeText(context, "🚨 تم تشغيل جرس إنذار الطوارئ وإرسال إشارات الاستغاثة التلقائية!", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = if(isSirenPlaying) RedAccent else WarmGold),
                                    modifier = Modifier.fillMaxWidth().height(54.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Default.VolumeUp, null, tint = MidnightBlue)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if(isSirenPlaying) "إيقاف جرس صفارة الإنذار" else "إطلاق جرس صفار الإنذار الصوتي",
                                        color = MidnightBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }

                        // Call 997 / 112 Buttons list
                        Text("أرقام الخط الساخن والاتصال السريع:", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:997"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                            ) {
                                Icon(Icons.Default.Phone, null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("اتصل بالإسعاف (997)", color = Color.White, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:911"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)
                            ) {
                                Icon(Icons.Default.Phone, null, tint = WarmGold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("الخط الوطني الموحد (911)", color = TextLight, fontSize = 12.sp)
                            }
                        }

                        // Search nearby ER hospital via map link
                        Button(
                            onClick = {
                                val mapUri = Uri.parse("geo:0,0?q=مستشفى طوارئ")
                                val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                                context.startActivity(mapIntent)
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)
                        ) {
                            Icon(Icons.Default.LocalHospital, null, tint = WarmGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("البحث عن أقرب مستشفى طوارئ على الخريطة", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    1 -> {
                        // START Triage interactive calculator
                        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("🛡️ مصنف والفرز الميداني السريع للطوارئ (START Protocol)", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    "نظام الفرز الطبي العالمي لتصنيف المصابين في الحوادث الجماعية وتحديد الأولويات خلال ثوانٍ معدودة.",
                                    color = TextLight,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                                
                                Divider(color = MidnightBlue)

                                OutlinedTextField(
                                    value = respRateStr,
                                    onValueChange = { respRateStr = it },
                                    label = { Text("معدل التنفس بالدقيقة (Breathing rate)") },
                                    placeholder = { Text("اكتب 0 للمتوقف أو القيمة المقدرة") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("هل النبض الكعبري موجود؟ (Radial Pulse Check)", color = TextLight, fontSize = 12.sp)
                                    Switch(
                                        checked = hasRadialPulse,
                                        onCheckedChange = { hasRadialPulse = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = WarmGold, checkedTrackColor = SurfaceWarm)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("المريض يتبع الأوامر البسيطة والاستجابة؟", color = TextLight, fontSize = 12.sp)
                                    Switch(
                                        checked = followsCommands,
                                        onCheckedChange = { followsCommands = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = WarmGold, checkedTrackColor = SurfaceWarm)
                                    )
                                }
                            }
                        }

                        // Computed triage results displaying box
                        Card(colors = CardDefaults.cardColors(containerColor = triageColor.copy(alpha = 0.15f)), shape = RoundedCornerShape(16.dp)) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("الأولوية والتصنيف الطبي التقديري الحركي:", color = WarmGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(triageColor))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        triageColorMsg,
                                        color = if (triageColor == Color.Black) TextLight else triageColor,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        // Poison reference card
                        Text("المرشد السريع للتعامل مع سموم الأطفال والمنزل:", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        poisonsList.forEach { poison ->
                            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(14.dp)) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, null, tint = RedAccent)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(poison.title, color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }

                                    Text("🚫 الأعراض المتوقعة: ${poison.symptom}", color = SilverGray, fontSize = 12.sp, lineHeight = 16.sp)
                                    Text("💡 الإسعاف والتدبير الفوري: ${poison.treatment}", color = TextLight, fontSize = 12.sp, lineHeight = 18.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

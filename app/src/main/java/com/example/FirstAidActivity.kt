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
import androidx.compose.foundation.shape.RoundedCornerShape
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

data class FirstAidCase(
    val title: String,
    val emoji: String,
    val description: String,
    val steps: List<String>
)

class FirstAidActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                FirstAidScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstAidScreen(onBack: () -> Unit) {
    var expandedCaseTitle by remember { mutableStateOf<String?>(null) }

    val aidCases = listOf(
        FirstAidCase(
            title = "الإنعاش القلبي الرئوي (CPR)",
            emoji = "🫀",
            description = "تدبير طارئ منقذ للحياة للمريض فاقد الوعي والتنفس.",
            steps = listOf(
                "1. تأكد من أمان الموقع وهز المريض بلطف للتأكد من درجة الوعي.",
                "2. اطلب الإسعاف فوراً (رقم 997).",
                "3. افتح مجرى الهواء برفع الذقن وإمالة الرأس للوراء.",
                "4. ابدأ بضغطات الصدر: 30 ضغطة متتالية بقوة وعمق (5-6 سم) في منتصف الصدر بمعدل 100-120 ضغطة بالدقيقة.",
                "5. اعطِ المريض نَفَسَي إنقاذ ثم كرر الدورة (30 ضغطة ونَفَسان) حتى وصول الفريق الطبي."
            )
        ),
        FirstAidCase(
            title = "إيقاف النزيف الحاد",
            emoji = "🩸",
            description = "منع الفقدان المستمر للدماء الغزيرة من الشرايين أو الأوردة.",
            steps = listOf(
                "1. ارتداء القفازات الواقية إن أمكن.",
                "2. اضغط مباشرة على موقع الجرح بقطعة شاش نظيفة ومعقمة.",
                "3. ارفع الطرف المصاب بوضعيته فوق مستوى القلب للحد من تدفق الدم.",
                "4. إذا تشبع الشاش بالكامل، ضع قطعة أخرى فوقه ولا تقم بإزالة الشاش القديم.",
                "5. ثبت الضماد برباط ضاغط بشكل محكم."
            )
        ),
        FirstAidCase(
            title = "الحروق النارية والكيميائية",
            emoji = "🔥",
            description = "تهدئة الأنسجة المتضررة من درجات الحرارة المرتفعة لمنع الالتهاب.",
            steps = listOf(
                "1. ابعد المصاب فوراً عن مصدر الخطر والنيران.",
                "2. صب مياه الصنبور الباردة المعتدلة على الحرق لمدة لا تقل عن 10 إلى 15 دقيقة (تجنب الثلج تماماً لمنع تلف الأنسجة).",
                "3. انزع الخواتم أو الملابس الملتصقة بموضع الحرق برفق قبل تورم الجلد.",
                "4. غطِّ الحرق بضمادة نظيفة ومعقمة وغير لاصقة.",
                "5. لا تقم بفقء الفقاعات المائية المتكونة أبداً."
            )
        ),
        FirstAidCase(
            title = "تثبيت الكسور العظمية",
            emoji = "🦴",
            description = "منع الحركة الزائدة للعظم المكسور للوقاية من تلف الأعصاب والأورام.",
            steps = listOf(
                "1. حافظ على الطرف المصاب ثابتاً تماماً في مكانه دون رده أو تعديله.",
                "2. ضع جبيرة داعمة مبسطة (باستخدام قطعة خشب أو كرتون سميك مقوى) أسفل وأعلى منطقة الكسر.",
                "3. اربط الجبيرة برباط شاش دون شده بقوة تعطل تروية الدم للطرف.",
                "4. إذا وجد جرح مفتوح، قم بتغطيته بشاش معقم نظيف أولاً برفق قبل الجبيرة."
            )
        ),
        FirstAidCase(
            title = "غصة الحلق والاختناق (Heimlich)",
            emoji = "🤢",
            description = "تحرير طعام أو جسم غريب يسد مجرى التنفس كلياً.",
            steps = listOf(
                "1. اسأل المريض: هل تختنق؟ إذا كان قادراً على السعال شجعه على السعال بقوة.",
                "2. قف خلف المصاب ولف ذراعيك حول خصره.",
                "3. اصنع قبضة بإحدى يديك وضعها فوق سرة المريض بقليل.",
                "4. اقبض على يدك الأخرى واضغط بقوة وسرعة للداخل وللأعلى (دفعات همليك المتكررة).",
                "5. للرضع: اعطِ 5 ضربات على الظهر بين الكتفين متبوعة بـ 5 ضغطات على الصدر بواسطة إصبعين فقط حتى خروج الجسم."
            )
        ),
        FirstAidCase(
            title = "الصدمة الكهربائية",
            emoji = "⚡",
            description = "فصل وسحب المصاب عن التيار الكهربائي بأمان.",
            steps = listOf(
                "1. لا تلمس المصاب بيديك العاريتين وهو لا يزال ملامساً للتيار الكهربائي.",
                "2. افصل القاطع العام الرئيسي للكهرباء فوراً.",
                "3. إذا لم تستطع، استخدم مادة عازلة جافة تماماً (مثل قطعة خشب أو بلاستيك سميك) لابعاد المصاب عن السلك.",
                "4. افحص وعي المصاب وتنفسه وابدأ الإنعاش الرئوي إن استدعى.",
                "5. غطِّ الحروق الكهربائية بشاش نظيف ريثما تصل سيارة الإسعاف."
            )
        ),
        FirstAidCase(
            title = "إنقاذ وإسعاف الغريق",
            emoji = "🏊",
            description = "استرجاع التنفس وطرد المياه الزائدة من الرئتين.",
            steps = listOf(
                "1. اسحب الغريق من الماء بسرعة وأمان.",
                "2. ضعه مستلقياً على ظهره على أرضية صلبة ومستوية.",
                "3. افتح مجرى تنفسه وتفحص النبض وحركة الصدر.",
                "4. ابدأ بإعطائه 5 أنفاس إنقاذ بدئية (نظراً لأن سبب التوقف هو نقص الأكسجين).",
                "5. ابدأ دورات الإنعاش القياسية (30 ضغطة ونفسين) مكررة حتى يستعيد وعيه أو يتقيأ الماء."
            )
        ),
        FirstAidCase(
            title = "التسمم والابتلاع العرضي للكيماويات",
            emoji = "🧪",
            description = "التعامل الفوري مع السموم والمنظفات المنزلية الحارقة.",
            steps = listOf(
                "1. حدد بدقة المادة التي تم ابتلاعها وخذ العبوة معك للإسعاف.",
                "2. لا تحاول إجبار المصاب على التقيؤ أبداً إذا كانت المادة حارقة (كالفلاش أو الصودا) كي لا تكوي المريء مرتين.",
                "3. اتصل فورا بمركز السموم القريب للاستشارة الطارئة.",
                "4. إذا انسكبت المادة على عينه أو جلده، اغسلها بماء جارٍ مستمر لمدة 20 دقيقة."
            )
        ),
        FirstAidCase(
            title = "الولادة الطارئة في المنزل أو السيارة",
            emoji = "👶",
            description = "مساعدة الأم في وضع جنينها بنظافة وأمان عند تعذر الوصول للمستشفى.",
            steps = listOf(
                "1. حافظ على الهدوء التام والتعقيم التام وغسل الأيدي جيداً.",
                "2. امدد الحامل على ظهرها مع ثني الركبتين، وتجهيز مناشف دافئة ونظيفة.",
                "3. عند دفع الجنين برأس المولد، ادعم رأسه برفق لمنع خروجه السريع دون شده العنيف.",
                "4. امسح أنف وفم المولود فور خروجه لتأمين التنفس وتأكد من صراخه.",
                "5. اربط الحبل السري برباطين نظيفين على بعد مسافة ولا تقطعه بآلة ملوثة، انتظر وصول الطواقم."
            )
        ),
        FirstAidCase(
            title = "التعرف وإسعاف الأزمة القلبية",
            emoji = "🫀",
            description = "التعامل السريع مع الم الصدر الشديد وضيق التنفس الذبحي.",
            steps = listOf(
                "1. اجعل المريض مستريحاً بوضعية الجلوس (نصف متكئ) لتقليل العبء على عضلة القلب.",
                "2. اتصل بالإسعاف فوراً (997).",
                "3. اطلب منه مضغ حبة أسبرين كاملة أو طحنها بالأسنان لتقليل التخثّرات.",
                "4. راقب تنفسه ودرجة وعيه باهتمام تام، وكن مستعداً لإجراء CPR إذا فقد النبض."
            )
        ),
        FirstAidCase(
            title = "السكتة الدماغية (Stroke)",
            emoji = "🧠",
            description = "ملاحظة أعراض الجلطة الدماغية باستخدام معيار FAST السريع.",
            steps = listOf(
                "1. تفقد معيار FAST: F (الوجه: هل تظهر استمالة عند الابتسام؟)، A (الذراعين: هل تسقط ذراعه عند رفعهما؟)، S (الكلام: هل يتمتم بنطق غير مفهوم؟)، T (الوقت: اتصل فوراً بالإسعاف).",
                "2. ضع المريض بوجه هادئ مستلقياً على جانبه مع إبقاء مجرى التنفس مفتوحاً (وضعية الإفاقة للوقاية من الاختناق باللعاب).",
                "3. لا تعطه أي سوائل أو طعام أو إسبرين بالفم إطلاقاً."
            )
        ),
        FirstAidCase(
            title = "لدغات الأفاعي ولدغ العقرب",
            emoji = "🦂",
            description = "تثبيت الطرف لمنع انتشار السم العصبي بالجسم.",
            steps = listOf(
                "1. حافظ على هدوء المصاب وثباته، حيث أن زيادة وتسارع دقات القلب ينشر السم ومكوناته أسرع بجسمه.",
                "2. اغسل مكان اللدغة بالماء والصابون جيداً.",
                "3. اربط رباطاً خفيفاً عريضاً أعلى اللدغة بمسافة بسيطة واجعل الطرف المصاب أدنى من مستوى القلب.",
                "4. لا تجرح مكان عضة الأفعى أو تمتص السم بفمك إطلاقاً.\n5. انتقل بالمريض إلى الطوارئ لأخذ المصل المعالج الفعال."
            )
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🚑 دليل الإسعافات الأولية الشامل (12 حالة سنوية)", color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
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
                .padding(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MedicalServices, null, tint = WarmGold, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "هذا المساق الطبي يغطي الإسعافات الأولية الأساسية والمنقذة للحياة من مصادر علمية موثقة. اضغط على أي حالة طارئة لاستكشاف الخطوات الإجرائية الدقيقة بالتفصيل.",
                        color = TextLight,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(aidCases) { item ->
                    val isExpanded = expandedCaseTitle == item.title
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedCaseTitle = if (isExpanded) null else item.title
                            },
                        colors = CardDefaults.cardColors(containerColor = if (isExpanded) SurfaceWarm else SurfaceDark),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.emoji, fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, color = WarmGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(item.description, color = SilverGray, fontSize = 12.sp)
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = WarmGold
                                )
                            }

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = MidnightBlue)
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item.steps.forEach { step ->
                                        Text(
                                            step,
                                            color = TextLight,
                                            fontSize = 13.sp,
                                            lineHeight = 20.sp
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
}

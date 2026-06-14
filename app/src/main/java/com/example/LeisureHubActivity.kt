package com.example

import android.content.Context
import android.os.Bundle
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class LeisureHubActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                LeisureMainContainer()
            }
        }
    }
}

// Particle details for beautiful dynamic Canvas background
data class DriftParticle(
    var x: Float,
    var y: Float,
    val speedX: Float,
    val speedY: Float,
    val radius: Float,
    val alpha: Float
)

@Composable
fun ParticleBackground() {
    val particles = remember {
        List(25) {
            DriftParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                speedX = (Random.nextFloat() * 0.002f) - 0.001f,
                speedY = (Random.nextFloat() * 0.003f) + 0.001f,
                radius = (Random.nextFloat() * 3.5f) + 1.5f,
                alpha = (Random.nextFloat() * 0.4f) + 0.15f
            )
        }
    }

    var frameState by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(20)
            frameState++
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        particles.forEach { p ->
            p.x += p.speedX
            p.y += p.speedY

            if (p.x < 0f) p.x = 1f
            if (p.x > 1f) p.x = 0f
            if (p.y < 0f) p.y = 1f
            if (p.y > 1f) p.y = 0f

            val drawX = p.x * width
            val drawY = p.y * height

            drawCircle(
                color = WarmGold.copy(alpha = p.alpha),
                radius = p.radius,
                center = Offset(drawX, drawY)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeisureMainContainer() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { EktefaaDatabase.getDatabase(context) }

    // State navigation variables
    var currentScreen by remember { mutableStateOf("HUB") } // HUB, MCQ, TRUE_FALSE, FILL_BLANK, CROSSWORD, LEADERBOARD, IMPORT
    var selectedQuizId by remember { mutableStateOf<Long?>(null) }
    var selectedQuizTitle by remember { mutableStateOf("") }
    var mockStatusMessage by remember { mutableStateOf("") }

    // Dialog state for selecting a quiz
    var showQuizSelectorDialog by remember { mutableStateOf(false) }
    var targetScreenAfterSelection by remember { mutableStateOf("MCQ") }

    // Load static and custom data
    var quizzesList by remember { mutableStateOf<List<QuizEntity>>(emptyList()) }
    var dailyWisdom by remember { mutableStateOf<DailyWisdomEntity?>(null) }
    var isImportingData by remember { mutableStateOf(false) }

    // Refresh function
    val refreshData = {
        coroutineScope.launch {
            db.quizDao().getAllQuizzesFlow().collect { list ->
                quizzesList = list
            }
        }
        coroutineScope.launch {
            val wisdom = db.dailyWisdomDao().getLatestWisdom()
            dailyWisdom = wisdom
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
        // Try importing assets if db is completely empty
        coroutineScope.launch {
            isImportingData = true
            QuizImporter.importFromAssets(context)
            isImportingData = false
            refreshData()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ImmersiveBg)
    ) {
        // Starfield Canvas Background
        ParticleBackground()

        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                (scaleIn(initialScale = 0.3f) + fadeIn()).togetherWith(scaleOut(targetScale = 0.8f) + fadeOut())
            },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                "HUB" -> LeisureHomeScreen(
                    quizzesCount = quizzesList.size,
                    dailyWisdom = dailyWisdom,
                    onNavigateToMode = { mode ->
                        targetScreenAfterSelection = mode
                        showQuizSelectorDialog = true
                    },
                    onNavigateToLeaderboard = {
                        currentScreen = "LEADERBOARD"
                    },
                    onNavigateToImport = {
                        currentScreen = "IMPORT"
                    },
                    isImporting = isImportingData,
                    onTriggerForceImport = {
                        coroutineScope.launch {
                            isImportingData = true
                            QuizImporter.importFromAssets(context)
                            isImportingData = false
                            refreshData()
                            Toast.makeText(context, "تم استيراد كافة المسابقات الـ 25 بنجاح!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                "MCQ" -> MCQScreen(
                    quizId = selectedQuizId ?: 1,
                    quizTitle = selectedQuizTitle,
                    onBack = { currentScreen = "HUB" }
                )

                "TRUE_FALSE" -> TrueFalseScreen(
                    quizId = selectedQuizId ?: 1,
                    quizTitle = selectedQuizTitle,
                    onBack = { currentScreen = "HUB" }
                )

                "FILL_BLANK" -> FillBlankScreen(
                    quizId = selectedQuizId ?: 1,
                    quizTitle = selectedQuizTitle,
                    onBack = { currentScreen = "HUB" }
                )

                "CROSSWORD" -> CrosswordScreen(
                    quizId = selectedQuizId ?: 1,
                    quizTitle = selectedQuizTitle,
                    onBack = { currentScreen = "HUB" }
                )

                "LEADERBOARD" -> LeaderboardScreen(
                    onBack = { currentScreen = "HUB" }
                )

                "IMPORT" -> TextImportScreen(
                    onBack = { currentScreen = "HUB" },
                    onImportSuccessful = { qId, qTitle ->
                        selectedQuizId = qId
                        selectedQuizTitle = qTitle
                        currentScreen = "MCQ"
                    }
                )
            }
        }

        // Dialog for choosing one of the 25 quizzes/lessons
        if (showQuizSelectorDialog) {
            AlertDialog(
                onDismissRequest = { showQuizSelectorDialog = false },
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(2.dp, WarmGold.copy(0.3f), RoundedCornerShape(20.dp))
                    .background(MidnightBlue),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "اختر درساً للمسابقة 📚",
                            color = WarmGold,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.weight(1f)
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxHeight(0.6f)
                    ) {
                        Text(
                            "تحتوي القائمة على 25 موضوعاً من الهوية الإيمانية والدروس التوعوية الصافية:",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(quizzesList) { quiz ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceWarm.copy(0.7f))
                                        .border(
                                            1.dp,
                                            if (selectedQuizId == quiz.id) WarmGold else Color.Transparent,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            selectedQuizId = quiz.id
                                            selectedQuizTitle = quiz.title
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(
                                        imageVector = when (quiz.id % 5) {
                                            0L -> Icons.Default.MenuBook
                                            1L -> Icons.Default.AutoStories
                                            2L -> Icons.Default.Bookmark
                                            3L -> Icons.Default.SelfImprovement
                                            else -> Icons.Default.Star
                                        },
                                        contentDescription = "Lesson Icon",
                                        tint = if (selectedQuizId == quiz.id) GoldLight else WarmGold.copy(0.7f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        quiz.title,
                                        color = if (selectedQuizId == quiz.id) Color.White else TextMuted,
                                        fontSize = 13.sp,
                                        fontWeight = if (selectedQuizId == quiz.id) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (selectedQuizId != null) {
                                showQuizSelectorDialog = false
                                currentScreen = targetScreenAfterSelection
                            } else {
                                Toast.makeText(context, "الرجاء اختيار موضوع أولاً!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ابدأ المغامرة الآن ⚔️", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showQuizSelectorDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إلغاء", color = TextMuted)
                    }
                }
            )
        }
    }
}

// 3.1 LeisureHub Screen
@Composable
fun LeisureHomeScreen(
    quizzesCount: Int,
    dailyWisdom: DailyWisdomEntity?,
    onNavigateToMode: (String) -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToImport: () -> Unit,
    isImporting: Boolean,
    onTriggerForceImport: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    // Rotation angles for the 3D objects on the 6 pedestals
    val rotAngle0 = remember { androidx.compose.animation.core.Animatable(0f) }
    val rotAngle1 = remember { androidx.compose.animation.core.Animatable(0f) }
    val rotAngle2 = remember { androidx.compose.animation.core.Animatable(0f) }
    val rotAngle3 = remember { androidx.compose.animation.core.Animatable(0f) }
    val rotAngle4 = remember { androidx.compose.animation.core.Animatable(0f) }
    val rotAngle5 = remember { androidx.compose.animation.core.Animatable(0f) }

    // Trigger navigation with 360 rotation effect
    val triggerNavigation = { idx: Int, target: String ->
        coroutineScope.launch {
            val anim = when (idx) {
                0 -> rotAngle0
                1 -> rotAngle1
                2 -> rotAngle2
                3 -> rotAngle3
                4 -> rotAngle4
                else -> rotAngle5
            }
            anim.snapTo(0f)
            anim.animateTo(
                targetValue = 360f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 800)
            )
            // Perform action
            when (target) {
                "MCQ" -> onNavigateToMode("MCQ")
                "TRUE_FALSE" -> onNavigateToMode("TRUE_FALSE")
                "FILL_BLANK" -> onNavigateToMode("FILL_BLANK")
                "CROSSWORD" -> onNavigateToMode("CROSSWORD")
                "LEADERBOARD" -> onNavigateToLeaderboard()
                "IMPORT" -> onNavigateToImport()
            }
            anim.snapTo(0f) // reset
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "ركن الاستراحة والمسابقات الإيمانية 🧩",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = WarmGold,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(0.9f),
                            offset = Offset(0f, 2f),
                            blurRadius = 6f
                        )
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    "انقر على المعالم ثلاثية الأبعاد لتفعيل بوابات التحدي وتدوير الكاميرا",
                    fontSize = 11.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Immersive Royal 3D Hall Visual Frame with absolute touch overlays
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(310.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.5.dp, WarmGold.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                        .background(Color.Black)
                ) {
                    // 1. Draw Royal Hall background on Canvas
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // A. Dark wood paneled wall
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF1B1006), Color(0xFF0C0603))
                            ),
                            topLeft = Offset(0f, 0f),
                            size = Size(w, h * 0.45f)
                        )
                        // Molding stripes for wood panels with golden hue
                        val stripesCount = 7
                        val stripeWidth = w / stripesCount
                        for (i in 0..stripesCount) {
                            drawLine(
                                color = Color(0xFFD4AF37).copy(alpha = 0.12f),
                                start = Offset(i * stripeWidth, 0f),
                                end = Offset(i * stripeWidth, h * 0.45f),
                                strokeWidth = 1.5f
                            )
                        }
                        // Golden rich ceiling board
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF8A731F), Color(0xFFD4AF37), Color(0xFF5A4911))
                            ),
                            topLeft = Offset(0f, h * 0.43f),
                            size = Size(w, h * 0.02f)
                        )

                        // B. Reflective shiny Dark Marble Floor
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF060912), Color(0xFF020408))
                            ),
                            topLeft = Offset(0f, h * 0.45f),
                            size = Size(w, h * 0.55f)
                        )
                        // Diagonal reflection lines representing marble texture
                        drawLine(
                            color = Color.White.copy(alpha = 0.08f),
                            start = Offset(0f, h * 0.55f),
                            end = Offset(w, h * 0.90f),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = Color(0xFFD4AF37).copy(alpha = 0.05f),
                            start = Offset(w, h * 0.50f),
                            end = Offset(0f, h * 0.85f),
                            strokeWidth = 1.5f
                        )

                        // C. central crystal hanging chandelier
                        val chX = w / 2f
                        val chY = h * 0.14f
                        // Cord
                        drawLine(
                            color = Color(0xFFD4AF37),
                            start = Offset(chX, 0f),
                            end = Offset(chX, chY - 18f),
                            strokeWidth = 2f
                        )
                        // Glowing crystal aura
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFFF6D1).copy(0.6f), Color.Transparent),
                                center = Offset(chX, chY),
                                radius = 60f
                            ),
                            radius = 60f,
                            center = Offset(chX, chY)
                        )
                        // Branches
                        drawLine(
                            color = Color(0xFFD4AF37),
                            start = Offset(chX - 35f, chY - 10f),
                            end = Offset(chX + 35f, chY - 10f),
                            strokeWidth = 3f
                        )
                        // Sparkles
                        val sparkOffsets = listOf(
                            Offset(chX, chY + 6f),
                            Offset(chX - 18f, chY + 4f),
                            Offset(chX + 18f, chY + 4f),
                            Offset(chX - 32f, chY),
                            Offset(chX + 32f, chY)
                        )
                        sparkOffsets.forEach { off ->
                            drawCircle(color = Color.White, radius = 3f, center = off)
                            drawLine(color = Color.White.copy(alpha = 0.5f), start = Offset(off.x, chY - 10f), end = off, strokeWidth = 1f)
                        }

                        // D. Draw 6 Cylindric Pedestals & 3D Objects on Top
                        val pxList = listOf(w * 0.22f, w * 0.78f, w * 0.22f, w * 0.78f, w * 0.22f, w * 0.78f)
                        val pyList = listOf(h * 0.24f, h * 0.24f, h * 0.54f, h * 0.54f, h * 0.81f, h * 0.81f)
                        val rotList = listOf(rotAngle0.value, rotAngle1.value, rotAngle2.value, rotAngle3.value, rotAngle4.value, rotAngle5.value)

                        for (idx in 0..5) {
                            val rx = pxList[idx]
                            val ry = pyList[idx]
                            val curRot = rotList[idx]

                            val bW = 85f
                            val bH = 20f
                            val colH = 45f

                            // Shadow under pedestal
                            drawOval(
                                color = Color.Black.copy(0.55f),
                                topLeft = Offset(rx - bW/2f - 6f, ry + colH + 1f),
                                size = Size(bW + 12f, bH + 4f)
                            )
                            // Lower cylinder base
                            drawOval(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF242A33), Color(0xFF404A59), Color(0xFF161B21))
                                ),
                                topLeft = Offset(rx - bW/2f, ry + colH),
                                size = Size(bW, bH)
                            )
                            // Pedestal column
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF2D3540), Color(0xFF515E70), Color(0xFF1C2229))
                                ),
                                topLeft = Offset(rx - bW * 0.38f, ry),
                                size = Size(bW * 0.76f, colH + bH/2f)
                            )
                            // Top elliptic cap
                            drawOval(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF5B697A), Color(0xFF8BA1BB), Color(0xFF384351))
                                ),
                                topLeft = Offset(rx - bW * 0.38f, ry - bH * 0.38f),
                                size = Size(bW * 0.76f, bH * 0.76f)
                            )

                            // Draw the 3D Vector Object centered on pedestal with Rotation angle
                            val objY = ry - 14f
                            
                            // A drop-shadow specifically for the object to project depth onto the pedestal cap
                            drawOval(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.Black.copy(0.7f), Color.Transparent),
                                    center = Offset(rx, ry - 3f),
                                    radius = 35f
                                ),
                                topLeft = Offset(rx - 30f, ry - 6f),
                                size = Size(60f, 8f)
                            )
                            
                            rotate(degrees = curRot, pivot = Offset(rx, objY)) {
                                when (idx) {
                                    0 -> { // MCQ: Golden 3D open scripture book with upwards glowing path and ambient page shading
                                        // Central ambient magic glow
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(Color(0xFFD4AF37).copy(0.4f), Color.Transparent),
                                                center = Offset(rx, objY),
                                                radius = 36f
                                            ),
                                            radius = 36f,
                                            center = Offset(rx, objY)
                                        )
                                        // Draw 3D Book Thickness (Dark golden block under the pages)
                                        val thickLeft = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(rx - 20f, objY + 4f)
                                            lineTo(rx - 2f, objY + 11f)
                                            lineTo(rx - 2f, objY + 16f)
                                            lineTo(rx - 20f, objY + 9f)
                                            close()
                                        }
                                        drawPath(thickLeft, color = Color(0xFF8B731F))
                                        
                                        val thickRight = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(rx + 2f, objY + 11f)
                                            lineTo(rx + 20f, objY + 4f)
                                            lineTo(rx + 20f, objY + 9f)
                                            lineTo(rx + 2f, objY + 16f)
                                            close()
                                        }
                                        drawPath(thickRight, color = Color(0xFF5A4911))

                                        // Left book page (with gradient shading for deep 3D curve look)
                                        val lp = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(rx - 18f, objY - 10f)
                                            lineTo(rx - 2f, objY - 3f)
                                            lineTo(rx - 2f, objY + 11f)
                                            lineTo(rx - 18f, objY + 4f)
                                            close()
                                        }
                                        drawPath(
                                            lp, 
                                            brush = Brush.linearGradient(
                                                colors = listOf(Color(0xFF8B731F), Color(0xFFD4AF37), Color(0xFFFFF6D1)),
                                                start = Offset(rx - 18f, objY - 10f),
                                                end = Offset(rx - 2f, objY + 11f)
                                            )
                                        )
                                        
                                        // Left page lines
                                        drawLine(Color(0xFF5A4911).copy(0.35f), Offset(rx - 14f, objY - 4f), Offset(rx - 5f, objY + 1f), strokeWidth = 1.2f)
                                        drawLine(Color(0xFF5A4911).copy(0.35f), Offset(rx - 14f, objY + 1f), Offset(rx - 5f, objY + 6f), strokeWidth = 1.2f)

                                        // Right book page
                                        val rp = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(rx + 2f, objY - 3f)
                                            lineTo(rx + 18f, objY - 10f)
                                            lineTo(rx + 18f, objY + 4f)
                                            lineTo(rx + 2f, objY + 11f)
                                            close()
                                        }
                                        drawPath(
                                            rp, 
                                            brush = Brush.linearGradient(
                                                colors = listOf(Color(0xFFFFF9D9), Color(0xFFEADB8C), Color(0xFFB0942F)),
                                                start = Offset(rx + 2f, objY - 3f),
                                                end = Offset(rx + 18f, objY + 4f)
                                            )
                                        )
                                        
                                        // Right page lines
                                        drawLine(Color(0xFF5A4911).copy(0.35f), Offset(rx + 5f, objY + 1f), Offset(rx + 14f, objY - 4f), strokeWidth = 1.2f)
                                        drawLine(Color(0xFF5A4911).copy(0.35f), Offset(rx + 5f, objY + 6f), Offset(rx + 14f, objY + 1f), strokeWidth = 1.2f)

                                        // Light beam rising from the center spine
                                        val cone = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(rx - 6f, objY + 2f)
                                            lineTo(rx + 6f, objY + 2f)
                                            lineTo(rx + 25f, objY - 40f)
                                            lineTo(rx - 25f, objY - 40f)
                                            close()
                                        }
                                        drawPath(
                                            cone, 
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Color(0xFFFFF9D9).copy(0.45f), Color.Transparent),
                                                startY = objY + 2f,
                                                endY = objY - 40f
                                            )
                                        )
                                    }
                                    1 -> { // True/False: Gold & Bronze 3D Justice Scales with realistic hollow pans and deep shadows
                                        // Main central golden pole with radial gradient depth
                                        drawLine(
                                            brush = Brush.linearGradient(
                                                colors = listOf(Color(0xFF5A4911), Color(0xFFD4AF37), Color(0xFFFFF6D1), Color(0xFF8B731F))
                                            ), 
                                            start = Offset(rx, objY + 10f), 
                                            end = Offset(rx, objY - 20f), 
                                            strokeWidth = 3f
                                        )
                                        // Center pole decorative ring
                                        drawCircle(Color(0xFFFFF6D1), radius = 3.5f, center = Offset(rx, objY - 8f))
                                        
                                        // Upper double-bevel crossbar
                                        drawLine(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(Color(0xFF8B731F), Color(0xFFFFF6D1), Color(0xFF5A4911))
                                            ), 
                                            start = Offset(rx - 22f, objY - 15f), 
                                            end = Offset(rx + 22f, objY - 15f), 
                                            strokeWidth = 2.5f
                                        )
                                        
                                        // Heavy metallic base plate
                                        drawOval(
                                            brush = Brush.horizontalGradient(listOf(Color(0xFF2C1E0A), Color(0xFF8B731F), Color(0xFF150E04))), 
                                            topLeft = Offset(rx - 10f, objY + 8f), 
                                            size = Size(20f, 5f)
                                        )

                                        // LEFT TRAY - hanging strings and 3D hollow copper dish
                                        // Fine wire suspension
                                        drawLine(Color(0xFFFFF6D1).copy(0.7f), start = Offset(rx - 20f, objY - 15f), end = Offset(rx - 25f, objY + 2f), strokeWidth = 0.8f)
                                        drawLine(Color(0xFFFFF6D1).copy(0.7f), start = Offset(rx - 20f, objY - 15f), end = Offset(rx - 15f, objY + 2f), strokeWidth = 0.8f)
                                        // Hollow bowl drawing: dark shade first, then inner highlight
                                        drawOval(
                                            color = Color(0xFF42350C),
                                            topLeft = Offset(rx - 26f, objY + 2f),
                                            size = Size(12f, 4.5f)
                                        )
                                        drawOval(
                                            brush = Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF5A4911))),
                                            topLeft = Offset(rx - 26f, objY + 3.5f),
                                            size = Size(12f, 2.5f)
                                        )

                                        // RIGHT TRAY - hanging strings and 3D hollow copper dish
                                        drawLine(Color(0xFFFFF6D1).copy(0.7f), start = Offset(rx + 20f, objY - 15f), end = Offset(rx + 15f, objY + 2f), strokeWidth = 0.8f)
                                        drawLine(Color(0xFFFFF6D1).copy(0.7f), start = Offset(rx + 20f, objY - 15f), end = Offset(rx + 25f, objY + 2f), strokeWidth = 0.8f)
                                        drawOval(
                                            color = Color(0xFF42350C),
                                            topLeft = Offset(rx + 14f, objY + 2f),
                                            size = Size(12f, 4.5f)
                                        )
                                        drawOval(
                                            brush = Brush.verticalGradient(listOf(Color(0xFFD4AF37), Color(0xFF5A4911))),
                                            topLeft = Offset(rx + 14f, objY + 3.5f),
                                            size = Size(12f, 2.5f)
                                        )
                                    }
                                    2 -> { // Fill Blank: Silver writing parchment scroll with roll-curve 3D effects
                                        // Bottom roller base shadow
                                        drawOval(Color.Black.copy(0.35f), topLeft = Offset(rx - 15f, objY + 13f), size = Size(22f, 4f))
                                        
                                        // Parchment paper body (styled with vertical and diagonal cream gradients)
                                        val pap = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(rx - 15f, objY + 4f)
                                            lineTo(rx + 12f, objY - 11f)
                                            lineTo(rx + 19f, objY + 2f)
                                            lineTo(rx - 8f, objY + 17f)
                                            close()
                                        }
                                        drawPath(
                                            pap, 
                                            brush = Brush.linearGradient(
                                                colors = listOf(Color(0xFFFAF6EB), Color(0xFFEADB8C), Color(0xFFC4B891))
                                            )
                                        )
                                        
                                        // Scroll top cylindric roll (giving it 3D thickness)
                                        drawOval(
                                            brush = Brush.horizontalGradient(listOf(Color(0xFFFAF6EB), Color(0xFFFAF6EB), Color(0xFFC4B891))),
                                            topLeft = Offset(rx + 8f, objY - 14f),
                                            size = Size(12f, 5f)
                                        )
                                        
                                        // Silver pen quill diagonally writing
                                        drawLine(
                                            brush = Brush.linearGradient(listOf(Color(0xFFE6E6E6), Color(0xFF999999), Color(0xFFFFFFFF))), 
                                            start = Offset(rx + 12f, objY - 20f), 
                                            end = Offset(rx - 4f, objY + 4f), 
                                            strokeWidth = 3f
                                        )
                                        // feather fluff rings along the quill
                                        drawCircle(Color.White.copy(0.8f), radius = 3.5f, center = Offset(rx + 8f, objY - 14f))
                                        drawCircle(Color.White.copy(0.6f), radius = 2.5f, center = Offset(rx + 4f, objY - 8f))
                                        
                                        // Pen tip ink splash
                                        drawCircle(Color(0xFF5A4911).copy(0.7f), radius = 1.5f, center = Offset(rx - 4f, objY + 5f))
                                    }
                                    3 -> { // Crossword: 3D Isometric cuboids with shaded face lighting (L: Shaded, R: Ambient, T: Shiny)
                                        // Function to draw realistic cubes with distinct directional lighting
                                        fun drawInteractive3DCube(cx: Float, cy: Float, sizeLen: Float, mainColor: Color) {
                                            // Top face (Brightest / Shiny)
                                            val topF = androidx.compose.ui.graphics.Path().apply {
                                                moveTo(cx, cy - sizeLen/2f)
                                                lineTo(cx + sizeLen, cy - sizeLen * 0.8f)
                                                lineTo(cx + sizeLen * 2f, cy - sizeLen/2f)
                                                lineTo(cx + sizeLen, cy - sizeLen * 0.2f)
                                                close()
                                            }
                                            drawPath(topF, color = mainColor.copy(alpha = 0.95f))
                                            
                                            // Left Face (Moderate shading)
                                            val leftF = androidx.compose.ui.graphics.Path().apply {
                                                moveTo(cx, cy - sizeLen/2f)
                                                lineTo(cx + sizeLen, cy - sizeLen * 0.2f)
                                                lineTo(cx + sizeLen, cy + sizeLen * 0.5f)
                                                lineTo(cx, cy + sizeLen * 0.2f)
                                                close()
                                            }
                                            drawPath(leftF, color = mainColor.copy(alpha = 0.8f))
                                            
                                            // Right Face (Deep shading - Shadowed)
                                            val rightF = androidx.compose.ui.graphics.Path().apply {
                                                moveTo(cx + sizeLen, cy - sizeLen * 0.2f)
                                                lineTo(cx + sizeLen * 2f, cy - sizeLen/2f)
                                                lineTo(cx + sizeLen * 2f, cy + sizeLen * 0.2f)
                                                lineTo(cx + sizeLen, cy + sizeLen * 0.5f)
                                                close()
                                            }
                                            drawPath(rightF, color = mainColor.copy(alpha = 0.62f))
                                            
                                            // Draw borders for 3D crispiness
                                            drawPath(topF, color = Color.White.copy(0.18f), style = Stroke(1f))
                                        }
                                        
                                        // Render three stacked 3D blocks
                                        drawInteractive3DCube(rx - 16f, objY + 4f, 11f, Color(0xFF8B5A2B)) // Base Left
                                        drawInteractive3DCube(rx + 2f, objY + 1f, 11f, Color(0xFFA06C3E))  // Base Right
                                        drawInteractive3DCube(rx - 7f, objY - 8f, 11f, Color(0xFFD2B48C))  // Stacked Top
                                    }
                                    4 -> { // Leaderboard: Shining Golden 3D Trophy Cup with deep hollow brim and marble stand
                                        // Beveled square base stand (Dark marble look)
                                        drawRect(
                                            brush = Brush.horizontalGradient(listOf(Color(0xFF1B232E), Color(0xFF384351), Color(0xFF0F141B))), 
                                            topLeft = Offset(rx - 10f, objY + 8f), 
                                            size = Size(20f, 6f)
                                        )
                                        
                                        // Slender stem
                                        drawLine(
                                            brush = Brush.horizontalGradient(listOf(Color(0xFF8B731F), Color(0xFFFFF6D1), Color(0xFF5A4911))), 
                                            start = Offset(rx, objY + 8f), 
                                            end = Offset(rx, objY - 2f), 
                                            strokeWidth = 3.5f
                                        )
                                        
                                        // 3D Bowl of the trophy (Gradients + Brim depth)
                                        val bowl = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(rx - 13f, objY - 16f)
                                            lineTo(rx + 13f, objY - 16f)
                                            lineTo(rx + 10f, objY - 2f)
                                            lineTo(rx - 10f, objY - 2f)
                                            close()
                                        }
                                        drawPath(
                                            bowl, 
                                            brush = Brush.linearGradient(
                                                colors = listOf(Color(0xFF5A4911), Color(0xFFD4AF37), Color(0xFFFFF6D1), Color(0xFF8B731F)),
                                                start = Offset(rx - 13f, objY - 16f),
                                                end = Offset(rx + 10f, objY - 2f)
                                            )
                                        )
                                        
                                        // Left handle with gradient ring
                                        drawCircle(Color(0xFFFFF6D1), radius = 5.5f, center = Offset(rx - 12f, objY - 9f), style = Stroke(2f))
                                        // Right handle with gradient ring
                                        drawCircle(Color(0xFFFFF6D1), radius = 5.5f, center = Offset(rx + 12f, objY - 9f), style = Stroke(2f))
                                        
                                        // Cup hollow brim ellipse (giving depth inside the trophy)
                                        drawOval(
                                            brush = Brush.verticalGradient(listOf(Color(0xFF3A2E05), Color(0xFF8B731F))),
                                            topLeft = Offset(rx - 13f, objY - 18.5f),
                                            size = Size(26f, 4.5f)
                                        )
                                    }
                                    else -> { // Import: Glowing 3D green digital vortex
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(Color(0xFF43C586).copy(alpha = 0.45f), Color.Transparent)
                                            ), 
                                            radius = 28f, 
                                            center = Offset(rx, objY)
                                        )
                                        drawCircle(Color(0xFF43C586), radius = 11f, center = Offset(rx, objY), style = Stroke(3f))
                                        
                                        // Futuristic green orbital segments
                                        for (deg in 0..360 step 45) {
                                            val rad = Math.toRadians((deg + curRot).toDouble())
                                            val sx = (rx + Math.cos(rad) * 11f).toFloat()
                                            val sy = (objY + Math.sin(rad) * 11f).toFloat()
                                            val ex = (rx + Math.cos(rad) * 18f).toFloat()
                                            val ey = (objY + Math.sin(rad) * 18f).toFloat()
                                            drawLine(
                                                color = Color(0xFFC0FCDC), 
                                                start = Offset(sx, sy), 
                                                end = Offset(ex, ey), 
                                                strokeWidth = 2.5f
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Clickable Transparent regions placed precisely on top of the 6 pedestals
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Row 1: MCQ & True/False
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { triggerNavigation(0, "MCQ") },
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Text(
                                    "مسابقة MCQ 📖",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(Color.Black.copy(0.6f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .padding(bottom = 6.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { triggerNavigation(1, "TRUE_FALSE") },
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Text(
                                    "صح أم خطأ ⚖️",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(Color.Black.copy(0.6f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .padding(bottom = 6.dp)
                                )
                            }
                        }
                        // Row 2: Fill Blank & Crossword
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { triggerNavigation(2, "FILL_BLANK") },
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Text(
                                    "إستكمال الفراغ ✍️",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(Color.Black.copy(0.6f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .padding(bottom = 6.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { triggerNavigation(3, "CROSSWORD") },
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Text(
                                    "كلمات متقاطعة 🧱",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(Color.Black.copy(0.6f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .padding(bottom = 6.dp)
                                )
                            }
                        }
                        // Row 3: Leaderboard & Import Archive
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { triggerNavigation(4, "LEADERBOARD") },
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Text(
                                    "لوحة الشرف العائلية 🏆",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(Color.Black.copy(0.6f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .padding(bottom = 4.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { triggerNavigation(5, "IMPORT") },
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Text(
                                    "صانع المسابقات 📥",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(Color.Black.copy(0.6f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .padding(bottom = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Wisdom of the day illuminating banner
            item {
                WisdomCarousel(dailyWisdom = dailyWisdom)
            }

            // Premium Rwad Golden Competition Card
            item {
                val actContext = LocalContext.current
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = WarmGold)
                        .border(
                            1.dp,
                            Brush.linearGradient(listOf(WarmGold, Color.Transparent, WarmGold)),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            val intent = android.content.Intent(actContext, RwadCompetitionActivity::class.java)
                            actContext.startActivity(intent)
                        },
                    colors = CardDefaults.cardColors(containerColor = CardGradStart.copy(0.85f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(CardGradStart, CardGradEnd)
                                )
                            )
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(WarmGold.copy(0.12f))
                                .border(1.dp, WarmGold.copy(0.25f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Rwad Cup",
                                tint = WarmGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                "مسابقة الرواد الذهبيّة الكبرى 🏆",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                "تحدي الوعي والبصيرة التفاعلي ثلاثي الأبعاد والسينمائي لتثقيف الجيل",
                                color = TextMuted,
                                fontSize = 9.5.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            // Direct lesson importer statistics & action bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardGradStart.copy(0.6f))
                        .border(1.dp, WarmGold.copy(0.15f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(color = WarmGold, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        Text("جاري استيراد كافة المسابقات الـ 25 الأصلية...", color = TextMuted, fontSize = 11.sp)
                    } else {
                        IconButton(
                            onClick = onTriggerForceImport,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(WarmGold.copy(alpha = 0.12f))
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Import", tint = WarmGold, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            "يحتوي أرشيف الدروس على: $quizzesCount درساً جاهزاً",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// Wisdom Carousel (Raf Da'eri for daily wisdom display)
@Composable
fun WisdomCarousel(dailyWisdom: DailyWisdomEntity?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(SurfaceWarm, MidnightBlue)))
            .border(2.dp, WarmGold.copy(0.4f), RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Spark", tint = GoldLight)
                Text(
                    "حكمة اليوم المضيئة ✨",
                    color = WarmGold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = dailyWisdom?.text ?: "قِيمَةُ كُلِّ امْرِئٍ مَا يُحْسِنُهُ. الوعي هو طوق النجاة وصانع المنعة.",
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                style = TextStyle(lineHeight = 22.sp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "— " + (dailyWisdom?.source ?: "من هدي القرآن والآثار العطرة"),
                color = GoldLight.copy(0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun NeumorphicModeCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(MidnightBlue, SurfaceWarm.copy(0.7f))))
            .border(1.dp, accentColor.copy(0.3f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(accentColor.copy(0.12f))
                .border(1.dp, accentColor.copy(0.4f), CircleShape)
                .padding(8.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Right
            )
            Text(
                description,
                color = TextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// 3.2 MCQ Screen
@Composable
fun MCQScreen(
    quizId: Long,
    quizTitle: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }
    val coroutineScope = rememberCoroutineScope()
    val db = remember { EktefaaDatabase.getDatabase(context) }

    var questionsList by remember { mutableStateOf<List<QuestionEntity>>(emptyList()) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var hasAnswered by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf("") }
    var progressAnim by remember { mutableStateOf(0f) }
    var isQuizFinished by remember { mutableStateOf(false) }

    // Participant name submission
    var participantName by remember { mutableStateOf("") }
    var showConfetti by remember { mutableStateOf(false) }

    LaunchedEffect(quizId) {
        db.questionDao().getQuestionsForQuizFlow(quizId).collect { list ->
            // Filter only MCQ type
            questionsList = list.filter { it.type == "mcq" }
            if (questionsList.isEmpty()) {
                // Generate fallback MCQs dynamically
                val qZ = db.quizDao().getAllQuizzes().find { it.id == quizId }
                val src = qZ?.sourceText ?: "نص افتراضي من المسيرة القرآنية واليقين الإيماني"
                questionsList = QuizGenerator.generateMCQ(src, quizId, 5)
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WarmGold)
                }
                Text(
                    quizTitle,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            if (questionsList.isNotEmpty() && !isQuizFinished) {
                val currentQuestion = questionsList[currentQuestionIndex]
                val progress = (currentQuestionIndex + 1).toFloat() / questionsList.size

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Golden Glowing Progress Bar
                    LinearProgressIndicator(
                        progress = { progress },
                        color = WarmGold,
                        trackColor = SurfaceWarm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "النتيجة: $score / ${questionsList.size}",
                            color = GoldLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "السؤال ${currentQuestionIndex + 1} من ${questionsList.size}",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }

                    // Rotating animated question Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brush.verticalGradient(listOf(SurfaceWarm, MidnightBlue)))
                            .border(1.dp, WarmGold.copy(0.5f), RoundedCornerShape(18.dp))
                            .padding(20.dp)
                    ) {
                        Text(
                            currentQuestion.question,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth(),
                            style = TextStyle(lineHeight = 24.sp)
                        )
                    }

                    // Multiple choices buttons
                    val options = listOf(
                        currentQuestion.optionA,
                        currentQuestion.optionB,
                        currentQuestion.optionC,
                        currentQuestion.optionD
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        options.forEach { option ->
                            if (option.isNotBlank()) {
                                val isCorrectOption = option == currentQuestion.correctAnswer
                                val isSelected = option == selectedOption
                                val btnBgColor = when {
                                    hasAnswered && isCorrectOption -> GreenAccent.copy(0.25f)
                                    hasAnswered && isSelected && !isCorrectOption -> RedAccent.copy(0.25f)
                                    isSelected -> WarmGold.copy(0.15f)
                                    else -> MidnightBlue.copy(0.8f)
                                }
                                val btnBorderColor = when {
                                    hasAnswered && isCorrectOption -> GreenAccent
                                    hasAnswered && isSelected && !isCorrectOption -> RedAccent
                                    isSelected -> WarmGold
                                    else -> WarmGold.copy(0.2f)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(btnBgColor)
                                        .border(1.dp, btnBorderColor, RoundedCornerShape(12.dp))
                                        .clickable(enabled = !hasAnswered) {
                                            selectedOption = option
                                            hasAnswered = true
                                            if (isCorrectOption) {
                                                score++
                                                showConfetti = true
                                                vibrator?.vibrate(80)
                                            } else {
                                                vibrator?.vibrate(200)
                                            }
                                        }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        option,
                                        color = if (hasAnswered && isCorrectOption) Color.White else TextMuted,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Right
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Next button or complete
                    if (hasAnswered) {
                        Button(
                            onClick = {
                                hasAnswered = false
                                selectedOption = ""
                                showConfetti = false
                                if (currentQuestionIndex + 1 < questionsList.size) {
                                    currentQuestionIndex++
                                } else {
                                    isQuizFinished = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            Text(
                                if (currentQuestionIndex + 1 == questionsList.size) "إنهاء المسابقة والحفظ 🏁" else "المتابعة للسؤال التالي ⬅️",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else if (isQuizFinished) {
                // Score card and name submitter
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = "Success",
                        tint = WarmGold,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "أحسنت صنعاً! انتهت الجولة 🎉",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "أحرزت نقاطاً قدرها: $score من أصل ${questionsList.size}",
                        color = GoldLight,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        "سجل نتيجتك العائلية في لوحة الشرف 🏆",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = participantName,
                        onValueChange = { participantName = it },
                        placeholder = { Text("اسم المتسابق (الأب، الأم، الأبناء...)", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarmGold,
                            unfocusedBorderColor = WarmGold.copy(0.4f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (participantName.isNotBlank()) {
                                coroutineScope.launch {
                                    db.familyScoreDao().insertScore(
                                        FamilyScoreEntity(
                                            memberName = participantName,
                                            quizId = quizId,
                                            score = score,
                                            totalQuestions = questionsList.size
                                        )
                                    )
                                    Toast.makeText(context, "تم حفظ النتيجة للفرسان!", Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                            } else {
                                Toast.makeText(context, "أدخل الاسم للحفظ!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حفظ وتسجيل في لوحة المتصدرين 💾", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = onBack) {
                        Text("العودة للرئيسية", color = WarmGold)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WarmGold)
                }
            }
        }
    }
}

// 3.3 True / False Screen
@Composable
fun TrueFalseScreen(
    quizId: Long,
    quizTitle: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }
    val coroutineScope = rememberCoroutineScope()
    val db = remember { EktefaaDatabase.getDatabase(context) }

    var questionsList by remember { mutableStateOf<List<QuestionEntity>>(emptyList()) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var hasAnswered by remember { mutableStateOf(false) }
    var selectedAnswer by remember { mutableStateOf("") }
    var isQuizFinished by remember { mutableStateOf(false) }
    var participantName by remember { mutableStateOf("") }

    LaunchedEffect(quizId) {
        db.questionDao().getQuestionsForQuizFlow(quizId).collect { list ->
            questionsList = list.filter { it.type == "truefalse" }
            if (questionsList.isEmpty()) {
                val qZ = db.quizDao().getAllQuizzes().find { it.id == quizId }
                val src = qZ?.sourceText ?: "الوعي بالله يثمر الثبات الإيماني"
                questionsList = QuizGenerator.generateTrueFalse(src, quizId, 5)
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WarmGold)
                }
                Text(
                    quizTitle,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            if (questionsList.isNotEmpty() && !isQuizFinished) {
                val currentQuestion = questionsList[currentQuestionIndex]
                val progress = (currentQuestionIndex + 1).toFloat() / questionsList.size

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        color = GreenAccent,
                        trackColor = SurfaceWarm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "النتيجة: $score / ${questionsList.size}",
                            color = GoldLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "السؤال ${currentQuestionIndex + 1} من ${questionsList.size}",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }

                    // Question Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brush.verticalGradient(listOf(SurfaceWarm, MidnightBlue)))
                            .border(1.dp, GreenAccent.copy(0.5f), RoundedCornerShape(18.dp))
                            .padding(24.dp)
                    ) {
                        Text(
                            currentQuestion.question,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            style = TextStyle(lineHeight = 26.sp)
                        )
                    }

                    // Giant True / False Gradient Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // True Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(110.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (hasAnswered && currentQuestion.correctAnswer == "صح") GreenAccent.copy(alpha = 0.3f)
                                    else if (hasAnswered && selectedAnswer == "صح" && currentQuestion.correctAnswer != "صح") RedAccent.copy(alpha = 0.3f)
                                    else GreenAccent.copy(alpha = 0.1f)
                                )
                                .border(
                                    2.dp,
                                    if (hasAnswered && currentQuestion.correctAnswer == "صح") GreenAccent
                                    else if (hasAnswered && selectedAnswer == "صح" && currentQuestion.correctAnswer != "صح") RedAccent
                                    else GreenAccent.copy(0.4f),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable(enabled = !hasAnswered) {
                                    selectedAnswer = "صح"
                                    hasAnswered = true
                                    if (currentQuestion.correctAnswer == "صح") {
                                        score++
                                        vibrator?.vibrate(80)
                                    } else {
                                        vibrator?.vibrate(200)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Check, contentDescription = "صح", tint = GreenAccent, modifier = Modifier.size(36.dp))
                                Text("صَحِيح ✅", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        // False Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(110.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (hasAnswered && currentQuestion.correctAnswer == "خطأ") GreenAccent.copy(alpha = 0.3f)
                                    else if (hasAnswered && selectedAnswer == "خطأ" && currentQuestion.correctAnswer != "خطأ") RedAccent.copy(alpha = 0.3f)
                                    else RedAccent.copy(alpha = 0.1f)
                                )
                                .border(
                                    2.dp,
                                    if (hasAnswered && currentQuestion.correctAnswer == "خطأ") GreenAccent
                                    else if (hasAnswered && selectedAnswer == "خطأ" && currentQuestion.correctAnswer != "خطأ") RedAccent
                                    else RedAccent.copy(0.4f),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable(enabled = !hasAnswered) {
                                    selectedAnswer = "خطأ"
                                    hasAnswered = true
                                    if (currentQuestion.correctAnswer == "خطأ") {
                                        score++
                                        vibrator?.vibrate(80)
                                    } else {
                                        vibrator?.vibrate(200)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Close, contentDescription = "خطأ", tint = RedAccent, modifier = Modifier.size(36.dp))
                                Text("خَاطِئ ❌", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (hasAnswered) {
                        Button(
                            onClick = {
                                hasAnswered = false
                                selectedAnswer = ""
                                if (currentQuestionIndex + 1 < questionsList.size) {
                                    currentQuestionIndex++
                                } else {
                                    isQuizFinished = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                if (currentQuestionIndex + 1 == questionsList.size) "حفظ التحدي للمفرزة 🏁" else "المتابعة للسؤال التالي ➡️",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else if (isQuizFinished) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Stars, contentDescription = "Winner", tint = GreenAccent, modifier = Modifier.size(72.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("انتهى تحدي الصح والخطأ بنجاح ⚖️", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("نقاطك: $score من أصل ${questionsList.size}", color = GoldLight, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))

                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedTextField(
                        value = participantName,
                        onValueChange = { participantName = it },
                        placeholder = { Text("ثبّت اسم المدافع عن البصيرة", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarmGold,
                            unfocusedBorderColor = WarmGold.copy(0.4f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (participantName.isNotBlank()) {
                                coroutineScope.launch {
                                    db.familyScoreDao().insertScore(
                                        FamilyScoreEntity(
                                            memberName = participantName,
                                            quizId = quizId,
                                            score = score,
                                            totalQuestions = questionsList.size
                                        )
                                    )
                                    Toast.makeText(context, "تم حفظ النتيجة لفرسان الصدق!", Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                            } else {
                                Toast.makeText(context, "أدخل الاسم للاعتماد المعتمد!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حفظ وتسجيل النتيجة 🥇", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    TextButton(onClick = onBack) {
                        Text("العودة للستارة الرئيسية", color = WarmGold)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WarmGold)
                }
            }
        }
    }
}

// 3.4 Fill in the Blanks Screen
@Composable
fun FillBlankScreen(
    quizId: Long,
    quizTitle: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }
    val coroutineScope = rememberCoroutineScope()
    val db = remember { EktefaaDatabase.getDatabase(context) }

    var questionsList by remember { mutableStateOf<List<QuestionEntity>>(emptyList()) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var userAnswer by remember { mutableStateOf("") }
    var hasChecked by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }
    var isQuizFinished by remember { mutableStateOf(false) }
    var participantName by remember { mutableStateOf("") }

    LaunchedEffect(quizId) {
        db.questionDao().getQuestionsForQuizFlow(quizId).collect { list ->
            questionsList = list.filter { it.type == "fillblank" }
            if (questionsList.isEmpty()) {
                val qZ = db.quizDao().getAllQuizzes().find { it.id == quizId }
                val src = qZ?.sourceText ?: "اليقين هو رصيد الأبرار لصد الأعداء"
                questionsList = QuizGenerator.generateFillBlank(src, quizId, 5)
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WarmGold)
                }
                Text(
                    quizTitle,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            if (questionsList.isNotEmpty() && !isQuizFinished) {
                val currentQuestion = questionsList[currentQuestionIndex]
                val progress = (currentQuestionIndex + 1).toFloat() / questionsList.size

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        color = GoldLight,
                        trackColor = SurfaceWarm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "النتيجة: $score / ${questionsList.size}",
                            color = GoldLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "السؤال ${currentQuestionIndex + 1} من ${questionsList.size}",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }

                    // Question view with hollow blanks
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(SurfaceWarm.copy(0.8f))
                            .border(1.dp, WarmGold.copy(0.4f), RoundedCornerShape(18.dp))
                            .padding(20.dp)
                    ) {
                        Text(
                            currentQuestion.question,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth(),
                            style = TextStyle(lineHeight = 24.sp)
                        )
                    }

                    // Fill Input Field
                    OutlinedTextField(
                        value = userAnswer,
                        onValueChange = { if (!hasChecked) userAnswer = it },
                        placeholder = { Text("أكتب هنا الكلمة المفقودة...", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarmGold,
                            unfocusedBorderColor = WarmGold.copy(0.4f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )

                    if (hasChecked) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isCorrect) GreenAccent.copy(0.15f) else RedAccent.copy(0.15f))
                                .border(1.dp, if (isCorrect) GreenAccent else RedAccent, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (isCorrect) "إجابتك دقيقة ومحكمة مائة بالمائة! 🌟" 
                                else "الإجابة الصحيحة هي: \"${currentQuestion.correctAnswer}\"",
                                color = if (isCorrect) GreenAccent else RedAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            if (!hasChecked) {
                                if (userAnswer.trim().isNotBlank()) {
                                    hasChecked = true
                                    isCorrect = userAnswer.trim().equals(currentQuestion.correctAnswer.trim(), ignoreCase = true) ||
                                            currentQuestion.correctAnswer.trim().contains(userAnswer.trim())
                                    if (isCorrect) {
                                        score++
                                        vibrator?.vibrate(80)
                                    } else {
                                        vibrator?.vibrate(200)
                                    }
                                } else {
                                    Toast.makeText(context, "الرجاء كتابة الفراغ أولاً!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                hasChecked = false
                                userAnswer = ""
                                if (currentQuestionIndex + 1 < questionsList.size) {
                                    currentQuestionIndex++
                                } else {
                                    isQuizFinished = true
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            if (!hasChecked) "تأكيد وصيانة الإجابة 🔍"
                            else if (currentQuestionIndex + 1 == questionsList.size) "رؤية النتيجة الإجمالية 🏁"
                            else "التالي ➡️",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (isQuizFinished) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.School, contentDescription = "Educator", tint = WarmGold, modifier = Modifier.size(72.dp))
                    Text("انتهت مسابقة الفراغات بنجاح 🏆", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("أحرزت نقاطاً: $score من أصل ${questionsList.size}", color = GoldLight, fontSize = 15.sp)

                    Spacer(modifier = Modifier.height(20.dp))
                    OutlinedTextField(
                        value = participantName,
                        onValueChange = { participantName = it },
                        placeholder = { Text("اسم البطل المتحدي", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarmGold,
                            unfocusedBorderColor = WarmGold.copy(0.4f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (participantName.isNotBlank()) {
                                coroutineScope.launch {
                                    db.familyScoreDao().insertScore(
                                        FamilyScoreEntity(
                                            memberName = participantName,
                                            quizId = quizId,
                                            score = score,
                                            totalQuestions = questionsList.size
                                        )
                                    )
                                    Toast.makeText(context, "تم الحفظ بنجاح!", Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                            } else {
                                Toast.makeText(context, "الرجاء إدخال الاسم للحفظ!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حفظ وتسجيل النتيجة 🏁", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    TextButton(onClick = onBack) {
                        Text("العودة للقاعة", color = WarmGold)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WarmGold)
                }
            }
        }
    }
}
                  // 3.5 Crossword Screen
@Composable
fun CrosswordScreen(
    quizId: Long,
    quizTitle: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { EktefaaDatabase.getDatabase(context) }

    var crosswordEntries by remember { mutableStateOf<List<QuizGenerator.CrosswordEntry>>(emptyList()) }
    val userCells = remember { mutableStateMapOf<Pair<Int, Int>, String>() }
    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var isVerified by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }

    // Bounce animation for the clicked cube
    val bounceScale = remember { androidx.compose.animation.core.Animatable(1f) }

    // Load Crossword Entries
    LaunchedEffect(quizId) {
        coroutineScope.launch {
            val quiz = db.quizDao().getAllQuizzes().find { it.id == quizId }
            val src = quiz?.sourceText ?: "يقين جهاد صبر هدى قرآن حياة استبسال وعي بصيرة تقوى"
            crosswordEntries = QuizGenerator.generateCrossword(src)
            
            // Auto select first entry coordinate
            if (crosswordEntries.isNotEmpty()) {
                val f = crosswordEntries[0]
                selectedCell = Pair((f.startRow - 1) % 7, (f.startCol - 1) % 7)
            }
        }
    }

    // Grid Mapping (7x7)
    val correctGrid = remember(crosswordEntries) { Array(7) { CharArray(7) { ' ' } } }
    val cellToClueMap = remember(crosswordEntries) { mutableMapOf<Pair<Int, Int>, String>() }

    LaunchedEffect(crosswordEntries) {
        // Clear old mapping
        for (r in 0..6) {
            for (c in 0..6) {
                correctGrid[r][c] = ' '
            }
        }
        cellToClueMap.clear()
        userCells.clear()
        isVerified = false

        crosswordEntries.forEachIndexed { idx, entry ->
            val w = entry.word
            for (i in w.indices) {
                val r = (entry.startRow - 1 + if (entry.direction == "رأسي") i else 0) % 7
                val c = (entry.startCol - 1 + if (entry.direction == "أفقي") i else 0) % 7
                correctGrid[r][c] = w[i]
                cellToClueMap[Pair(r, c)] = "دليل #${idx + 1} (${entry.direction}): ${entry.clue}"
            }
        }
    }

    // Confetti particles state
    var confettiParticles by remember { mutableStateOf<List<DriftParticle>>(emptyList()) }
    LaunchedEffect(showConfetti) {
        if (showConfetti) {
            confettiParticles = List(35) {
                DriftParticle(
                    x = Random.nextFloat(),
                    y = -0.1f - (Random.nextFloat() * 0.4f),
                    speedX = (Random.nextFloat() * 0.04f) - 0.02f,
                    speedY = (Random.nextFloat() * 0.03f) + 0.015f,
                    radius = (Random.nextFloat() * 8f) + 4f,
                    alpha = (Random.nextFloat() * 0.5f) + 0.5f
                )
            }
            // Animate confetti fall
            for (tick in 1..90) {
                delay(25)
                confettiParticles = confettiParticles.map { p ->
                    p.x += p.speedX
                    p.y += p.speedY
                    p
                }
            }
            showConfetti = false
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WarmGold)
                }
                Text(
                    "أدلة الكلمات المتقاطعة العائلية 🗺️",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "الموضوع: $quizTitle",
                    color = GoldLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                // Render Selected Clue Display
                val currentClue = selectedCell?.let { cellToClueMap[it] } ?: "انقر على أحد المكعبات ثلاثية الأبعاد المدرجة لقراءة دليله"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MidnightBlue.copy(0.7f))
                        .border(1.dp, WarmGold.copy(0.3f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        currentClue,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Immersive Canvas for 3D Crossword Blocks
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(0.4f))
                        .pointerInput(crosswordEntries) {
                            detectTapGestures { offset ->
                                val w = this.size.width.toFloat()
                                val h = this.size.height.toFloat()
                                val cellW = w / 7f
                                val cellH = h / 7f
                                val col = (offset.x / cellW).toInt().coerceIn(0, 6)
                                val row = (offset.y / cellH).toInt().coerceIn(0, 6)

                                if (correctGrid[row][col] != ' ') {
                                    selectedCell = Pair(row, col)
                                    // Trigger bounce physics
                                    coroutineScope.launch {
                                        bounceScale.snapTo(1f)
                                        bounceScale.animateTo(1.24f, androidx.compose.animation.core.tween(120))
                                        bounceScale.animateTo(1f, androidx.compose.animation.core.tween(100))
                                    }
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val cellW = w / 7f
                        val cellH = h / 7f

                        for (r in 0..6) {
                            for (c in 0..6) {
                                val correctChar = correctGrid[r][c]
                                if (correctChar == ' ') continue

                                val isSelected = selectedCell == Pair(r, c)
                                val currentInput = userCells[Pair(r, c)] ?: ""
                                val isCorrect = isVerified && currentInput == correctChar.toString()

                                // Canvas dimensions
                                val originalW = cellW - 6f
                                val originalH = cellH - 6f
                                val scale = if (isSelected) bounceScale.value else 1f

                                val drawW = originalW * scale
                                val drawH = originalH * scale
                                val leftOffset = c * cellW + (cellW - drawW) / 2f
                                val topOffset = r * cellH + (cellH - drawH) / 2f

                                // Side extrusion look (3D thickness depth)
                                val extX = 6f * scale
                                val extY = 5f * scale
                                val sideCol = if (isCorrect) Color(0xFF997A20) else if (isSelected) Color(0xFF53A17B) else Color(0x7FFFFFFF)

                                drawRoundRect(
                                    color = Color.Black.copy(alpha = 0.35f),
                                    topLeft = Offset(leftOffset + extX + 2f, topOffset + extY + 2f),
                                    size = Size(drawW, drawH),
                                    cornerRadius = CornerRadius(4f * scale, 4f * scale)
                                )

                                drawRoundRect(
                                    color = sideCol,
                                    topLeft = Offset(leftOffset + extX, topOffset + extY),
                                    size = Size(drawW, drawH),
                                    cornerRadius = CornerRadius(4f * scale, 4f * scale)
                                )

                                // Front Face
                                val faceCol = if (isCorrect) Color(0xFFD4AF37) else if (isSelected) Color(0x3B43C586) else Color(0x13FFFFFF)
                                drawRoundRect(
                                    color = faceCol,
                                    topLeft = Offset(leftOffset, topOffset),
                                    size = Size(drawW, drawH),
                                    cornerRadius = CornerRadius(4f * scale, 4f * scale),
                                    style = androidx.compose.ui.graphics.drawscope.Fill
                                )
                                drawRoundRect(
                                    color = if (isCorrect) Color(0xFFEADB8C) else if (isSelected) Color(0xFF43C586) else Color(0xFFD4AF37).copy(0.4f),
                                    topLeft = Offset(leftOffset, topOffset),
                                    size = Size(drawW, drawH),
                                    cornerRadius = CornerRadius(4f * scale, 4f * scale),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                                )

                                // Text Character inside
                                if (currentInput.isNotEmpty()) {
                                    drawContext.canvas.nativeCanvas.drawText(
                                        currentInput,
                                        leftOffset + drawW / 2f,
                                        topOffset + drawH / 2f + (10f * scale),
                                        android.graphics.Paint().apply {
                                            color = if (isCorrect) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                                            textSize = 34f * scale
                                            textAlign = android.graphics.Paint.Align.CENTER
                                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Interactive control panel row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            var allCorrect = true
                            for (r in 0..6) {
                                for (c in 0..6) {
                                    val correctC = correctGrid[r][c]
                                    if (correctC != ' ') {
                                        val u = userCells[Pair(r, c)] ?: ""
                                        if (u != correctC.toString()) {
                                            allCorrect = false
                                        }
                                    }
                                }
                            }
                            isVerified = true
                            if (allCorrect) {
                                showConfetti = true
                                Toast.makeText(context, "أحسنتم بقوة! حليتم الكلمات المتقاطعة بالكامل بنجاح 🏆", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "بعض الحروف غير صحيحة، يرجى المراجعة وتعديلها", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("التحقق من الكلمات ✅", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            userCells.clear()
                            isVerified = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceWarm),
                        modifier = Modifier.weight(0.5f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إعادة البناء 🛡️", color = Color.White, fontSize = 11.sp)
                    }
                }

                // Custom Arabic Alphabet Keyboard (Supports 3 Rows)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardGradStart.copy(0.5f), RoundedCornerShape(16.dp))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val r1 = listOf("أ", "ب", "ت", "ث", "ج", "ح", "خ", "د")
                    val r2 = listOf("ذ", "ر", "ز", "س", "ش", "ص", "ض", "ط")
                    val r3 = listOf("ظ", "ع", "غ", "ف", "ق", "ك", "ل", "م")
                    val r4 = listOf("ن", "هـ", "و", "ي", "ء", "⌫") // ⌫ backspace / clear

                    listOf(r1, r2, r3, r4).forEach { keyboardRow ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            keyboardRow.forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (key == "⌫") Color(0x40FF5E5E) else SurfaceWarm)
                                        .clickable {
                                            selectedCell?.let { cell ->
                                                if (key == "⌫") {
                                                    userCells.remove(cell)
                                                } else {
                                                    userCells[cell] = key
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        key,
                                        color = if (key == "⌫") RedAccent else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Confetti Overlay
            if (showConfetti) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    confettiParticles.forEach { p ->
                        val dx = p.x * w
                        val dy = p.y * h
                        drawCircle(
                            color = WarmGold.copy(alpha = p.alpha),
                            radius = p.radius,
                            center = Offset(dx, dy)
                        )
                    }
                }
            }
        }
    }
}

// 3.6 Leaderboard Screen
@Composable
fun LeaderboardScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { EktefaaDatabase.getDatabase(context) }
    var rankings by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.familyScoreDao().getLeaderboardFlow().collect { list ->
            rankings = list
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WarmGold)
                }
                Text(
                    "لوحة الشرف الصوفية العائلية 🏆",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.verticalGradient(listOf(SurfaceWarm, MidnightBlue)))
                        .border(1.dp, WarmGold.copy(0.4f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Trophy", tint = WarmGold, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "لوحة فرسان الوعي والتمكين",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            "مجموع رصيد الإجابات السليمة والمسابقات المجتازة للأسرة",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (rankings.isNotEmpty()) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(rankings.withIndex().toList()) { (index, entry) ->
                            val medal = when (index) {
                                0 -> "🥇"
                                1 -> "🥈"
                                2 -> "🥉"
                                else -> "🏅"
                            }
                            val cardBorder = when (index) {
                                0 -> BorderStroke(1.5.dp, WarmGold)
                                1 -> BorderStroke(1.dp, SilverGray)
                                2 -> BorderStroke(1.dp, Color(0xFFCD7F32)) // Bronze
                                else -> BorderStroke(1.dp, WarmGold.copy(alpha = 0.15f))
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MidnightBlue.copy(alpha = 0.7f))
                                    .border(
                                        border = cardBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "$medal الفارس #${index + 1}",
                                        color = when (index) {
                                            0 -> GoldLight
                                            1 -> Color.White
                                            else -> TextMuted
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        "${entry.total} نقطة",
                                        color = WarmGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                    Text(
                                        entry.memberName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Right
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            "لا توجد نتائج مسجلة حتى الآن.\nابدأ مسابقة وسجل اسمك لتعتلي الصدارة! 🏆",
                            color = TextMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Text("العودة للقاعة الرئيسية", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

// 3.7 Text Import Screen (صانع المسابقات الذكي)
@Composable
fun TextImportScreen(
    onBack: () -> Unit,
    onImportSuccessful: (Long, String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { EktefaaDatabase.getDatabase(context) }

    var customTitle by remember { mutableStateOf("") }
    var customTextBody by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WarmGold)
                }
                Text(
                    "صانع المسابقات الذكي من النصوص 📥",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "الصق أي مقالة توعوية أو فقرة من المنهج لتجزيئها آلياً وتوليد 15 سؤالاً تفاعلياً ومتقاطعاً على الفور ✨",
                    color = TextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    style = TextStyle(lineHeight = 18.sp)
                )

                OutlinedTextField(
                    value = customTitle,
                    onValueChange = { customTitle = it },
                    placeholder = { Text("عنوان موضوع المحتوى (مثال: فضل التسبيح)", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmGold,
                        unfocusedBorderColor = WarmGold.copy(0.4f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = customTextBody,
                    onValueChange = { customTextBody = it },
                    placeholder = { Text("أدخل النص التوعوي كاملاً هنا (الحد الأدنى 40 كلمة)...", color = TextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmGold,
                        unfocusedBorderColor = WarmGold.copy(0.4f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    maxLines = 15
                )

                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(WarmGold.copy(0.12f))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = WarmGold, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                            Text(
                                "جاري تحليل المفردات والتركيب الذاتي وصناعة الفئات على الفور...",
                                color = GoldLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        if (customTitle.trim().isNotBlank() && customTextBody.trim().length > 40) {
                            coroutineScope.launch {
                                isProcessing = true
                                val newQuiz = QuizEntity(
                                    title = customTitle.trim(),
                                    sourceText = customTextBody.trim(),
                                    type = "mcq"
                                )
                                val addedQuizId = db.quizDao().insertQuiz(newQuiz)

                                // Generate on-the-fly questions
                                val mcqs = QuizGenerator.generateMCQ(customTextBody, addedQuizId, 5)
                                val tfs = QuizGenerator.generateTrueFalse(customTextBody, addedQuizId, 5)
                                val fbs = QuizGenerator.generateFillBlank(customTextBody, addedQuizId, 5)

                                db.questionDao().insertQuestions(mcqs + tfs + fbs)

                                // Success dialog or transfer
                                isProcessing = false
                                Toast.makeText(context, "نجحت عملية التركيب والتصنيف الست وعشرون!", Toast.LENGTH_SHORT).show()
                                onImportSuccessful(addedQuizId, customTitle.trim())
                            }
                        } else {
                            Toast.makeText(context, "يرجى تقديم محتوى حقيقي متكامل وكافٍ للتوليد!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Text("شغّل المعالجة والتركيب والبدء بالمسابقة ⚡", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

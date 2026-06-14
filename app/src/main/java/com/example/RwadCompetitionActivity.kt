package com.example

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.InputStream

// Deep Premium Colors for the Rwad Golden Competition
val MidnightNavy = Color(0xFF070014)
val DeepIndigo = Color(0xFF0F0426)
val GoldenBronze = Color(0xFF91722C)
val RadiantGold = Color(0xFFE2C06B)
val PlatinumSilver = Color(0xFFD4DAE6)
val SmoothCrimson = Color(0xFFC72828)
val EmeraldGlow = Color(0xFF1CB055)

data class RwadQuestion(
    val id: Int,
    val level: Int,
    val value: String,
    val a1: String,
    val a2: String,
    val a3: String,
    val a4: String,
    val rightAnswer: Int
)

class RwadCompetitionActivity : ComponentActivity() {
    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setContent {
            RwadCompetitionPortalScreen(onExit = { finish() })
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RwadCompetitionPortalScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Screen State: "ENTRY", "GAME", "FINISH"
    var gameState by remember { mutableStateOf("ENTRY") }

    // Quiz Questions State
    var allQuestions by remember { mutableStateOf<List<RwadQuestion>>(emptyList()) }
    var currentQuestionsList by remember { mutableStateOf<List<RwadQuestion>>(emptyList()) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedLevelForGame by remember { mutableStateOf(1) }
    var score by remember { mutableStateOf(0) }
    var coinsEarned by remember { mutableStateOf(0) }
    
    // Answering dynamics
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var isAnswerCorrect by remember { mutableStateOf<Boolean?>(null) }
    var hasAnswered by remember { mutableStateOf(false) }
    var totalLevelsCount by remember { mutableStateOf(1) }

    // 3D Angle values for interactive dragging on entry screen
    var rotationAngleX by remember { mutableStateOf(15f) }
    var rotationAngleY by remember { mutableStateOf(-20f) }

    // Parse the database
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val questions = mutableListOf<RwadQuestion>()
                val jsonString = context.assets.open("database_rwad.json").bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(jsonString)
                if (jsonObject.has("qm")) {
                    val qmArray = jsonObject.getJSONArray("qm")
                    for (i in 0 until qmArray.length()) {
                        val obj = qmArray.getJSONObject(i)
                        val id = obj.optInt("q_id")
                        val level = obj.optInt("q_level")
                        val value = obj.optString("q_value")

                        // Skip encrypted base64 elements
                        if (value.contains("[a-zA-Z]".toRegex()) && value.length > 50) {
                            continue
                        }

                        val a1 = obj.optString("q_a1")
                        val a2 = obj.optString("q_a2")
                        val a3 = obj.optString("q_a3")
                        val a4 = obj.optString("q_a4")
                        val rightAnswer = obj.optInt("q_rightAnswer")

                        questions.add(
                            RwadQuestion(
                                id = id,
                                level = level,
                                value = value,
                                a1 = a1,
                                a2 = a2,
                                a3 = a3,
                                a4 = a4,
                                rightAnswer = rightAnswer
                            )
                        )
                    }
                }
                allQuestions = questions
                if (questions.isNotEmpty()) {
                    totalLevelsCount = questions.distinctBy { it.level }.map { it.level }.maxOrNull() ?: 1
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "خطأ في تحميل قاعدة مسابقة الرواد: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Audio Playback Helpers directly connected to Assets
    fun playAssetAudio(filename: String) {
        try {
            val afd = context.assets.openFd("audio/$filename")
            val mp = MediaPlayer()
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            mp.prepare()
            mp.start()
            mp.setOnCompletionListener {
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Composable
    fun CosmosStarsBackground() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MidnightNavy, DeepIndigo)
                    )
                )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightNavy)
    ) {
        // Cosmos backdrop
        CosmosStarsBackground()

        AnimatedContent(
            targetState = gameState,
            transitionSpec = {
                slideInVertically(animationSpec = tween(700)) { it } + fadeIn(animationSpec = tween(500)) with
                        slideOutVertically(animationSpec = tween(500)) { -it } + fadeOut(animationSpec = tween(400))
            }
        ) { state ->
            when (state) {
                "ENTRY" -> {
                    RwadEntryView(
                        totalQuestionsCount = allQuestions.size,
                        totalLevels = totalLevelsCount,
                        rotationX = rotationAngleX,
                        rotationY = rotationAngleY,
                        onRotate = { dx, dy ->
                            rotationAngleY += dx * 0.5f
                            rotationAngleX = (rotationAngleX - dy * 0.5f).coerceIn(-45f, 45f)
                        },
                        onPlayClick = {
                            playAssetAudio("coins.mp3")
                            // Extract questions for selected level
                            val levelQuestions = allQuestions.filter { it.level == selectedLevelForGame }
                            if (levelQuestions.isNotEmpty()) {
                                currentQuestionsList = levelQuestions.shuffled().take(10) // up to 10 questions per run
                                currentQuestionIndex = 0
                                score = 0
                                coinsEarned = 0
                                selectedOptionIndex = null
                                isAnswerCorrect = null
                                hasAnswered = false
                                gameState = "GAME"
                            } else {
                                Toast.makeText(context, "لا توجد أسئلة غير مشفرة متاحة لهذا المستوى بعد!", Toast.LENGTH_SHORT).show()
                                // Fallback: Take random questions
                                if (allQuestions.isNotEmpty()) {
                                    currentQuestionsList = allQuestions.shuffled().take(10)
                                    currentQuestionIndex = 0
                                    score = 0
                                    coinsEarned = 0
                                    selectedOptionIndex = null
                                    isAnswerCorrect = null
                                    hasAnswered = false
                                    gameState = "GAME"
                                }
                            }
                        },
                        selectedLevel = selectedLevelForGame,
                        onLevelChange = { selectedLevelForGame = it },
                        onBack = onExit
                    )
                }
                "GAME" -> {
                    if (currentQuestionsList.isNotEmpty() && currentQuestionIndex < currentQuestionsList.size) {
                        val question = currentQuestionsList[currentQuestionIndex]
                        RwadQuestionGameplayView(
                            question = question,
                            questionNumber = currentQuestionIndex + 1,
                            totalQuestions = currentQuestionsList.size,
                            score = score,
                            coins = coinsEarned,
                            selectedOptionIndex = selectedOptionIndex,
                            isCorrect = isAnswerCorrect,
                            onOptionSelected = { opIndex ->
                                if (!hasAnswered) {
                                    selectedOptionIndex = opIndex
                                    hasAnswered = true
                                    val isRight = (opIndex == question.rightAnswer)
                                    isAnswerCorrect = isRight
                                    if (isRight) {
                                        score += 1
                                        coinsEarned += 15
                                        playAssetAudio("correct.mp3")
                                        playAssetAudio("coins.mp3")
                                    } else {
                                        playAssetAudio("wrong.mp3")
                                    }
                                }
                            },
                            onNext = {
                                coroutineScope.launch {
                                    if (currentQuestionIndex < currentQuestionsList.size - 1) {
                                        currentQuestionIndex += 1
                                        selectedOptionIndex = null
                                        isAnswerCorrect = null
                                        hasAnswered = false
                                    } else {
                                        // Game Finished
                                        playAssetAudio("finish.mp3")
                                        gameState = "FINISH"
                                    }
                                }
                            },
                            onQuit = {
                                gameState = "ENTRY"
                            }
                        )
                    } else {
                        // Safe fallback
                        gameState = "ENTRY"
                    }
                }
                "FINISH" -> {
                    RwadFinishView(
                        score = score,
                        totalQuestions = currentQuestionsList.size,
                        coins = coinsEarned,
                        level = selectedLevelForGame,
                        onReplay = {
                            gameState = "ENTRY"
                        },
                        onExit = onExit
                    )
                }
            }
        }
    }
}

@Composable
fun RwadEntryView(
    totalQuestionsCount: Int,
    totalLevels: Int,
    rotationX: Float,
    rotationY: Float,
    onRotate: (Float, Float) -> Unit,
    onPlayClick: () -> Unit,
    selectedLevel: Int,
    onLevelChange: (Int) -> Unit,
    onBack: () -> Unit
) {
    var expandedLevelDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper Title bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(0.1f))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = RadiantGold)
            }

            Text(
                "مسابقة الرواد الذهبية",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = RadiantGold,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black,
                        offset = Offset(0f, 2f),
                        blurRadius = 6f
                    )
                )
            )

            Icon(
                Icons.Default.Star,
                contentDescription = "Star",
                tint = RadiantGold,
                modifier = Modifier.scale(1.2f)
            )
        }

        // Beautiful 3D Interactive Rotating Card Centerpiece
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onRotate(dragAmount.x, dragAmount.y)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .graphicsLayer {
                        val rx = rotationX
                        val ry = rotationY
                        this.rotationX = -rx
                        this.rotationY = ry
                        cameraDistance = 12f * density
                    }
                    .shadow(35.dp, RoundedCornerShape(24.dp), spotColor = RadiantGold)
                    .border(
                        2.dp,
                        Brush.linearGradient(listOf(RadiantGold, Color.Transparent, RadiantGold)),
                        RoundedCornerShape(24.dp)
                    )
                    .background(
                        Brush.verticalGradient(listOf(DeepIndigo, MidnightNavy))
                    )
                    .width(220.dp)
                    .height(260.dp)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Crest Badge symbol
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(RadiantGold.copy(0.4f), Color.Transparent)))
                        .border(1.5.dp, RadiantGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = "Cup",
                        tint = RadiantGold,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Title info
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "المنصّة الذهبيّة الكبرى",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "اسحب لتدوير الرمز ثلاثي الأبعاد والتحكم في الزواية تفاعلياً",
                        color = TextMuted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Stats badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(0.06f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("الأسئلة", color = RadiantGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("$totalQuestionsCount", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Divider(color = Color.White.copy(0.15f), modifier = Modifier.height(18.dp).width(1.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("المستويات", color = RadiantGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("$totalLevels", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Lower Control Panel with Level Selector and Cinematic Play Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Level Selector Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(0.06f))
                    .border(1.dp, RadiantGold.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .clickable { expandedLevelDropdown = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Drop", tint = RadiantGold)
                    Text(
                        "مستوى المسابقة المحدد: $selectedLevel",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = RadiantGold)
                }

                DropdownMenu(
                    expanded = expandedLevelDropdown,
                    onDismissRequest = { expandedLevelDropdown = false },
                    modifier = Modifier
                        .background(DeepIndigo)
                        .border(1.dp, RadiantGold.copy(0.4f))
                ) {
                    for (level in 1..totalLevels) {
                        DropdownMenuItem(
                            text = { Text("المستوى $level", color = Color.White, fontWeight = FontWeight.Bold) },
                            onClick = {
                                onLevelChange(level)
                                expandedLevelDropdown = false
                            }
                        )
                    }
                }
            }

            // High impact 3D Launcher button
            Button(
                onClick = onPlayClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = RadiantGold)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(GoldenBronze, RadiantGold, GoldenBronze)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "بـث الـمـسـابـقـة والمؤثرات ⚡",
                        color = MidnightNavy,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.White.copy(0.5f),
                                offset = Offset(0f, 1f),
                                blurRadius = 2f
                            )
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun RwadQuestionGameplayView(
    question: RwadQuestion,
    questionNumber: Int,
    totalQuestions: Int,
    score: Int,
    coins: Int,
    selectedOptionIndex: Int?,
    isCorrect: Boolean?,
    onOptionSelected: (Int) -> Unit,
    onNext: () -> Unit,
    onQuit: () -> Unit
) {
    val progress = questionNumber.toFloat() / totalQuestions

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Stats and Quit Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onQuit,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(0.08f))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Quit", tint = Color.White)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "المستوى ${question.level}",
                    color = RadiantGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(RadiantGold.copy(0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "السؤال $questionNumber من $totalQuestions",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // Rewards
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("$coins", color = RadiantGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Icon(Icons.Default.MonetizationOn, contentDescription = "Coins", tint = RadiantGold, modifier = Modifier.size(18.dp))
            }
        }

        // Circular dynamic loading bar
        LinearProgressIndicator(
            progress = progress,
            color = RadiantGold,
            trackColor = Color.White.copy(0.1f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clip(CircleShape)
        )

        // The Majestic Question card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 12.dp)
                .border(1.dp, RadiantGold.copy(0.2f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DeepIndigo.copy(0.85f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    question.value,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )
            }
        }

        // Multi-choice Plain Answers Board (q_a1 to q_a4)
        val optionTexts = listOf(question.a1, question.a2, question.a3, question.a4)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            optionTexts.forEachIndexed { i, text ->
                val optionIndex = i + 1
                if (!text.isNullOrBlank()) {
                    val isThisSelected = (selectedOptionIndex == optionIndex)
                    val isThisCorrect = (question.rightAnswer == optionIndex)

                    val itemColor = when {
                        selectedOptionIndex == null -> Color.White.copy(0.06f)
                        isThisCorrect -> EmeraldGlow.copy(0.2f)
                        isThisSelected -> SmoothCrimson.copy(0.2f)
                        else -> Color.White.copy(0.02f)
                    }

                    val borderColor = when {
                        selectedOptionIndex == null -> Color.White.copy(0.12f)
                        isThisCorrect -> EmeraldGlow
                        isThisSelected -> SmoothCrimson
                        else -> Color.Transparent
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(itemColor)
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable(enabled = selectedOptionIndex == null) {
                                onOptionSelected(optionIndex)
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Answer Indicator icon
                        Icon(
                            imageVector = when {
                                selectedOptionIndex == null -> Icons.Default.Circle
                                isThisCorrect -> Icons.Default.CheckCircle
                                isThisSelected -> Icons.Default.Cancel
                                else -> Icons.Default.Circle
                            },
                            contentDescription = "Status",
                            tint = when {
                                selectedOptionIndex == null -> Color.White.copy(0.2f)
                                isThisCorrect -> EmeraldGlow
                                isThisSelected -> SmoothCrimson
                                else -> Color.White.copy(0.06f)
                            },
                            modifier = Modifier.size(18.dp)
                        )

                        Text(
                            text,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                    }
                }
            }
        }

        // Lower next screen controller
        AnimatedVisibility(
            visible = selectedOptionIndex != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut()
        ) {
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = RadiantGold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(48.dp)
            ) {
                Text(
                    if (questionNumber < totalQuestions) "التالي ➡️" else "إنهاء المسابقة 🏁",
                    color = MidnightNavy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// Cinematic Resolution view running HTML assets
@Composable
fun RwadFinishView(
    score: Int,
    totalQuestions: Int,
    coins: Int,
    level: Int,
    onReplay: () -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Finished banner
        Text(
            "تمت المهمة بنجاح! 🎉",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = RadiantGold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )

        // Web view display for the requested dynamic assetsfinish.html wrapping t2.gif
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .border(2.dp, RadiantGold, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        loadUrl("file:///android_asset/files/finish.html")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Stats card layout
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(1.dp, RadiantGold.copy(0.3f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DeepIndigo.copy(0.8f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "مـلـخّـص نـقـاط الـفـوارس",
                    fontWeight = FontWeight.Bold,
                    color = RadiantGold,
                    fontSize = 15.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("الإجابات الصحيحة", color = TextMuted, fontSize = 11.sp)
                        Text("$score / $totalQuestions", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("الذهب المضاف", color = TextMuted, fontSize = 11.sp)
                        Text("+$coins 🪙", color = RadiantGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        // Lower action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.12f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("خروج", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onReplay,
                colors = ButtonDefaults.buttonColors(containerColor = RadiantGold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("إعادة المحاولة", color = MidnightNavy, fontWeight = FontWeight.Bold)
            }
        }
    }
}

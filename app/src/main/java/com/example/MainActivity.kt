package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import com.example.ui.EktefaaViewModel
import com.example.ui.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay

object EktefaaAudioManager {
    private var backgroundPlayer: android.media.MediaPlayer? = null
    private var splashPlayer: android.media.MediaPlayer? = null
    var isMutedState = false

    fun playBackgroundLoop(context: android.content.Context) {
        // Halt splash first to prevent overlap
        stopSplashSound()
        if (backgroundPlayer != null) return
        try {
            backgroundPlayer = android.media.MediaPlayer.create(context, R.raw.qarar_alula)
            backgroundPlayer?.apply {
                isLooping = true
                val vol = if (isMutedState) 0f else 0.4f
                setVolume(vol, vol)
                start()
            }
        } catch (e: Exception) {
            android.util.Log.e("EktefaaAudio", "Failed to load/play background music", e)
        }
    }

    fun playSplashSound(context: android.content.Context) {
        if (splashPlayer != null) return
        try {
            splashPlayer = android.media.MediaPlayer.create(context, R.raw.nabd_albidaya)
            splashPlayer?.apply {
                isLooping = false
                val vol = if (isMutedState) 0f else 0.5f
                setVolume(vol, vol)
                start()
            }
        } catch (e: Exception) {
            android.util.Log.e("EktefaaAudio", "Failed to play splash sound", e)
        }
    }

    fun stopSplashSound() {
        try {
            splashPlayer?.stop()
            splashPlayer?.release()
            splashPlayer = null
        } catch (e: Exception) {
            // ignore
        }
    }

    fun setMuted(muted: Boolean) {
        isMutedState = muted
        try {
            val bgVol = if (isMutedState) 0f else 0.4f
            backgroundPlayer?.setVolume(bgVol, bgVol)
            val splashVol = if (isMutedState) 0f else 0.5f
            splashPlayer?.setVolume(splashVol, splashVol)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun playSoundOnce(context: android.content.Context, rawResId: Int) {
        try {
            val player = android.media.MediaPlayer.create(context, rawResId)
            player?.setOnCompletionListener { it.release() }
            player?.start()
        } catch (e: Exception) {
            android.util.Log.e("EktefaaAudio", "Failed to play sound: $rawResId", e)
        }
    }

    fun pauseLoop() {
        try {
            if (backgroundPlayer?.isPlaying == true) {
                backgroundPlayer?.pause()
            }
            if (splashPlayer?.isPlaying == true) {
                splashPlayer?.pause()
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    fun resumeLoop() {
        try {
            if (backgroundPlayer != null && !isMutedState) {
                backgroundPlayer?.start()
            }
            if (splashPlayer != null && !isMutedState) {
                splashPlayer?.start()
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    fun stopBackgroundLoop(context: android.content.Context? = null) {
        try {
            backgroundPlayer?.stop()
            backgroundPlayer?.release()
            backgroundPlayer = null
        } catch (e: Exception) {
            // ignore
        }
        stopSplashSound()
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel: EktefaaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                EktefaaApp(viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        EktefaaAudioManager.resumeLoop()
    }

    override fun onStop() {
        super.onStop()
        EktefaaAudioManager.pauseLoop()
    }

    override fun onDestroy() {
        super.onDestroy()
        EktefaaAudioManager.stopBackgroundLoop(this)
    }
}

@Composable
fun EktefaaApp(viewModel: EktefaaViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showSplash by rememberSaveable { mutableStateOf(true) }
    var userRole by rememberSaveable { mutableStateOf("") } // "Father", "Mother", "Child"
    var loggedInUser by rememberSaveable { mutableStateOf<String?>(null) }
    var currentScreen by rememberSaveable { mutableStateOf("DASHBOARD") } // "DASHBOARD", "SOCIAL", "CLIPBOARD", "CHILDREN", "CURRICULUM", "QURAN", "PRAYER", "CALCULATOR", "FINANCES", "EXTRA"
    var showAccountSettings by rememberSaveable { mutableStateOf(false) }
    var isMusicMuted by rememberSaveable { mutableStateOf(false) }
    var showExitPortal by remember { mutableStateOf(false) }

    // Start proper sound stage based on splash screen status to prevent any sound overlapping
    LaunchedEffect(showSplash) {
        if (showSplash) {
            EktefaaAudioManager.playSplashSound(context)
        } else {
            EktefaaAudioManager.playBackgroundLoop(context)
        }
    }

    // React to manual toggle mute
    LaunchedEffect(isMusicMuted) {
        EktefaaAudioManager.setMuted(isMusicMuted)
    }

    // Back button handling
    BackHandler(enabled = currentScreen != "DASHBOARD" || showSplash || loggedInUser == null) {
        if (showSplash) {
            // let they finish gates
        } else if (currentScreen != "DASHBOARD") {
            currentScreen = "DASHBOARD"
        } else if (loggedInUser != null) {
            loggedInUser = null
            userRole = ""
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = when {
                showSplash -> "SPLASH"
                loggedInUser == null -> "LOGIN"
                else -> currentScreen
            },
            transitionSpec = {
                fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
            },
            label = "ScreenTransition"
        ) { state ->
            when (state) {
                "SPLASH" -> EktefaaSplashScreen(
                    onSkip = { showSplash = false },
                    onPlaySound = { rawId -> EktefaaAudioManager.playSoundOnce(context, rawId) }
                )
                "LOGIN" -> EktefaaLoginGate(
                    viewModel = viewModel,
                    onLoginSuccess = { role, name ->
                        userRole = role
                        loggedInUser = name
                        currentScreen = "DASHBOARD"
                    }
                )
                "DASHBOARD" -> EktefaaDashboard(
                    userName = loggedInUser ?: "المستخدم",
                    userRole = userRole,
                    isMusicMuted = isMusicMuted,
                    onToggleMusic = { isMusicMuted = !isMusicMuted },
                    onNavigate = { screen ->
                        if (screen == "HEALTH_HUB") {
                            val intent = android.content.Intent(context, HealthHubActivity::class.java).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                            }
                            context.startActivity(intent)
                        } else if (screen == "FAMILY_HUB") {
                            currentScreen = "FAMILY_HUB"
                        } else {
                            currentScreen = screen
                        }
                    },
                    onOpenSettings = { showAccountSettings = true },
                    onLogout = {
                        loggedInUser = null
                        userRole = ""
                    },
                    onExitApp = {
                        showExitPortal = true
                    }
                )
                "FAMILY_HUB" -> EktefaaFamilyHubScreen(
                    userName = loggedInUser ?: "المستخدم",
                    userRole = userRole,
                    isMusicMuted = isMusicMuted,
                    onToggleMusic = { isMusicMuted = !isMusicMuted },
                    onNavigate = { screen ->
                        if (screen == "LIBRARY") {
                            val intent = android.content.Intent(context, SchoolLibraryActivity::class.java).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                            }
                            context.startActivity(intent)
                        } else if (screen == "LEISURE") {
                            val intent = android.content.Intent(context, LeisureHubActivity::class.java).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                            }
                            context.startActivity(intent)
                        } else if (screen == "PRAYER") {
                            val intent = android.content.Intent(context, ProgramRajolAllahActivity::class.java).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                            }
                            context.startActivity(intent)
                        } else if (screen == "MEN_OF_GOD") {
                            val intent = android.content.Intent(context, MenOfGodActivity::class.java).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                            }
                            context.startActivity(intent)
                        } else if (screen == "PRAYERS") {
                            val intent = android.content.Intent(context, PrayersActivity::class.java).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                            }
                            context.startActivity(intent)
                        } else {
                            currentScreen = screen
                        }
                    },
                    onBack = {
                        currentScreen = "DASHBOARD"
                    }
                )
                "SOCIAL" -> {
                    val clipboardItems by viewModel.allClipboard.collectAsState()
                    SocialHubScreen(
                        onBack = { currentScreen = "FAMILY_HUB" },
                        onSaveToClipboard = { content ->
                            viewModel.addClipItem(content, "عام")
                        }
                    )
                }
                "CLIPBOARD" -> {
                    val clipboardItems by viewModel.allClipboard.collectAsState()
                    ClipboardScreen(
                        items = clipboardItems,
                        onBack = { currentScreen = "FAMILY_HUB" },
                        onAddItem = { text, cat -> viewModel.addClipItem(text, cat) },
                        onDeleteItem = { id -> viewModel.deleteClipItem(id) },
                        onUpdateItem = { item -> viewModel.updateClipItem(item) }
                    )
                }
                "CHILDREN" -> {
                    val children by viewModel.allChildren.collectAsState()
                    val clipboardItems by viewModel.allClipboard.collectAsState()
                    ChildrenScreen(
                        children = children,
                        clipboardItems = clipboardItems,
                        onBack = { currentScreen = "FAMILY_HUB" },
                        onAddChild = { name, age, grade, notes ->
                            viewModel.addChild(name, age, grade, notes)
                        },
                        onDeleteChild = { id -> viewModel.deleteChild(id) },
                        getLessons = { id -> viewModel.getLessonsForChild(id) },
                        onAddFeedback = { childId, lesson, score, diffs, positives ->
                            viewModel.addLessonFeedback(childId, lesson, score, diffs, positives)
                        },
                        onAddClipItem = { text, cat -> viewModel.addClipItem(text, cat) },
                        onDeleteClipItem = { id -> viewModel.deleteClipItem(id) }
                    )
                }
                "CURRICULUM" -> {
                    CurriculumScreen(
                        onBack = { currentScreen = "FAMILY_HUB" },
                        onAskGemini = { query, onResult ->
                            viewModel.askGemini(query, "MY_GEMINI_API_KEY", onResult)
                        }
                    )
                }
                "QURAN" -> {
                    val quranLogs by viewModel.allQuranLogs.collectAsState()
                    val children by viewModel.allChildren.collectAsState()
                    val childWirds by viewModel.allChildQuranWirds.collectAsState()
                    val childMistakes by viewModel.allChildQuranMistakes.collectAsState()
                    QuranScreen(
                        quranLogs = quranLogs,
                        children = children,
                        childWirds = childWirds,
                        childMistakes = childMistakes,
                        onBack = { currentScreen = "FAMILY_HUB" },
                        onAddLog = { surah, verses, completed, mistakes ->
                            viewModel.addQuranLog(surah, verses, completed, mistakes)
                        },
                        onDeleteLog = { id -> viewModel.deleteQuranLog(id) },
                        onAddWird = { childId, surahId, surahName, sVerse, eVerse ->
                            viewModel.addChildQuranWird(childId, surahId, surahName, sVerse, eVerse)
                        },
                        onUpdateWird = { wird -> viewModel.updateChildQuranWird(wird) },
                        onDeleteWird = { id -> viewModel.deleteChildQuranWird(id) },
                        onAddMistake = { childId, surahId, surahName, vNum, vText, mWord, corrected ->
                            viewModel.addChildQuranMistake(childId, surahId, surahName, vNum, vText, mWord, corrected)
                        },
                        onToggleMistake = { mistake -> viewModel.toggleChildQuranMistake(mistake) },
                        onDeleteMistake = { id -> viewModel.deleteChildQuranMistake(id) },
                        onAddChild = { name, age, grade, notes ->
                            viewModel.addChild(name, age, grade, notes)
                        }
                    )
                }
                "PRAYER" -> {
                    val prayerLogs by viewModel.allPrayerLogs.collectAsState()
                    PrayerScreen(
                        prayerLogs = prayerLogs,
                        onBack = { currentScreen = "FAMILY_HUB" },
                        onSaveLog = { dateStr, f, d, a, m, i, q, az ->
                            viewModel.logPrayer(dateStr, f, d, a, m, i, q, az)
                        }
                    )
                }
                "CALCULATOR" -> {
                    CalculatorScreen(onBack = { currentScreen = "FAMILY_HUB" })
                }
                "FINANCES" -> {
                    val expenseLogs by viewModel.allExpenseLogs.collectAsState()
                    val debtItems by viewModel.allDebts.collectAsState()
                    FinancesScreen(
                        currentRole = userRole,
                        expenseLogs = expenseLogs,
                        debtItems = debtItems,
                        onBack = { currentScreen = "FAMILY_HUB" },
                        onAddExpense = { type, amt, cat, note, timestamp ->
                            viewModel.addExpense(type, amt, cat, note, timestamp)
                        },
                        onDeleteExpense = { id -> viewModel.deleteExpense(id) },
                        onAddDebt = { shopName, desc, amt, paid, dueDate ->
                            viewModel.addDebt(shopName, desc, amt, paid, dueDate)
                        },
                        onDeleteDebt = { id -> viewModel.deleteDebt(id) }
                    )
                }
                "EXTRA" -> {
                    val todos by viewModel.allTodos.collectAsState()
                    val appliances by viewModel.allAppliances.collectAsState()
                    val medications by viewModel.allMedications.collectAsState()
                    ExtraServicesScreen(
                        currentTabScreenName = userRole,
                        todos = todos,
                        appliances = appliances,
                        medications = medications,
                        onBack = { currentScreen = "FAMILY_HUB" },
                        onAddTodo = { text, priority -> viewModel.addTodo(text, priority) },
                        onToggleTodo = { item -> viewModel.toggleTodo(item) },
                        onDeleteTodo = { id -> viewModel.deleteTodo(id) },
                        onAddAppliance = { name, date, cycle -> viewModel.addAppliance(name, date, cycle) },
                        onDeleteAppliance = { id -> viewModel.deleteAppliance(id) },
                        onAddMedication = { pName, name, time, dosage, prescribedBy, method, warnings, recommendations, notes ->
                            viewModel.addMedication(pName, name, time, dosage, prescribedBy, method, warnings, recommendations, notes)
                        },
                        onToggleMedication = { item -> viewModel.toggleMedication(item) },
                        onDeleteMedication = { id -> viewModel.deleteMedication(id) }
                    )
                }
                "FATWAS" -> {
                    FiqhFatwasScreen(onBack = { currentScreen = "FAMILY_HUB" })
                }
                "DESIGN_OASIS" -> {
                    DesignOasisScreen(
                        onBack = { currentScreen = "FAMILY_HUB" }
                    )
                }
                "CULTURAL_LIBRARY" -> {
                    LibraryScreen(
                        onBack = { currentScreen = "FAMILY_HUB" }
                    )
                }
                "ACADEMIC_HELPER" -> {
                    AcademicHelperScreen(
                        onBack = { currentScreen = "FAMILY_HUB" },
                        onAskGemini = { query, onResult ->
                            viewModel.askGemini(query, "MY_GEMINI_API_KEY", onResult)
                        },
                        onSaveToClipboard = { content, category ->
                            viewModel.addClipItem(content, category)
                        }
                    )
                }
                "COMMUNICATION_CENTER" -> {
                    CommunicationCenterScreen(
                        onBack = { currentScreen = "FAMILY_HUB" },
                        onSaveToClipboard = { content, category ->
                            viewModel.addClipItem(content, category)
                        }
                    )
                }
            }
        }

        if (showAccountSettings) {
            FamilyAccountSettingsDialog(
                viewModel = viewModel,
                onDismiss = { showAccountSettings = false }
            )
        }

        if (showExitPortal) {
            EktefaaExitPortal(
                onDismiss = { showExitPortal = false },
                onFinish = {
                    val activity = findActivity(context)
                    activity?.finish()
                }
            )
        }
    }
}

// Utility to find the activity from standard compose context
fun findActivity(context: android.content.Context): android.app.Activity? {
    var ctx = context
    while (ctx is android.content.ContextWrapper) {
        if (ctx is android.app.Activity) {
            return ctx
        }
        ctx = ctx.baseContext
    }
    return null
}

// 1. Splash Screen - Cinematic Multi-Gate Welcomer (Services Gathering + The Light Gate)
@Composable
fun GoldenShieldIcon(
    modifier: Modifier = Modifier,
    glowRadius: Float = 0f
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = glowRadius.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = WarmGold,
                spotColor = WarmGold
            )
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF1AC), WarmGold, Color(0xFF9E7815))
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .border(3.dp, Color(0xFFFFF1AC), RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.6f),
            tint = MidnightBlue
        )
    }
}

@Composable
fun EktefaaSplashScreen(
    onSkip: () -> Unit,
    onPlaySound: (Int) -> Unit
) {
    val isAssetLoading by com.example.database.AssetManager.isLoading.collectAsState()
    val assetProgress by com.example.database.AssetManager.progressMessage.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Splash animation stages: 0 = Services Gathering, 1 = Great Portal Gate, 2 = Welcome Info Gate Hub
    var splashStage by rememberSaveable { mutableStateOf(0) }
    var servicesProgress by remember { mutableStateOf(0f) }
    var centerPulseScale by remember { mutableStateOf(1f) }
    var isGateOpen by remember { mutableStateOf(false) }

    // Speech synthesis for developer attribution statement
    var ttsEngine by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val tts = android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                ttsEngine?.language = java.util.Locale("ar")
                // Warm verbal cue played immediately
                ttsEngine?.speak("تطبيق اكتفاء، إعداد وتطوير المصمم إدريس المداني", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "Ektefaa_Splash_TTS")
            }
        }
        ttsEngine = tts
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    // Phase 1 Animation: Services Gathering fly in over 1.6s
    LaunchedEffect(splashStage) {
        if (splashStage == 0) {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(1600, easing = FastOutSlowInEasing)
            ) { value, _ ->
                servicesProgress = value
            }
            // Trigger glorious core shield explosion pulse
            animate(
                initialValue = 1f,
                targetValue = 1.25f,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) { value, _ ->
                centerPulseScale = value
            }
            animate(
                initialValue = 1.25f,
                targetValue = 1.0f,
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            ) { value, _ ->
                centerPulseScale = value
            }
            delay(400)
            // Progress seamlessly to Stage 1 (Gate opening)
            splashStage = 1
        } else if (splashStage == 1) {
            isGateOpen = false
            delay(150)
            isGateOpen = true
            delay(1200)
            splashStage = 2
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        CosmosStarsBackground()

        if (splashStage == 0) {
            // Stage 0: Services Gathering Visual Layout
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(MidnightBlue, Color.Black)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Central Shield Target
                GoldenShieldIcon(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(centerPulseScale),
                    glowRadius = if (servicesProgress >= 0.95f) 30f else 0f
                )

                // 8 flying service nodes with high-contrast text label / indicators
                val serviceIcons = listOf(
                    Icons.Default.Chat to "التواصل",
                    Icons.Default.ContentCopy to "الحافظة",
                    Icons.Default.People to "المتابعة",
                    Icons.Default.MenuBook to "المكتبة",
                    Icons.Default.Mosque to "القرآن",
                    Icons.Default.SelfImprovement to "رجال الله",
                    Icons.Default.AttachMoney to "المالية",
                    Icons.Default.LocalHospital to "الصحة"
                )

                val angles = listOf(0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0)

                serviceIcons.forEachIndexed { idx, pair ->
                    val angleRad = Math.toRadians(angles[idx])
                    // Start from outer frame edge (350dp distance) and flight towards center
                    val startDistance = 350f
                    val currentDistance = startDistance * (1f - servicesProgress)

                    val offsetX = (Math.cos(angleRad) * currentDistance).dp
                    val offsetY = (Math.sin(angleRad) * currentDistance).dp
                    val rotation = (1f - servicesProgress) * 360f
                    val alpha = servicesProgress

                    Box(
                        modifier = Modifier
                            .offset(x = offsetX, y = offsetY)
                            .rotate(rotation)
                            .scale(0.4f + servicesProgress * 0.6f)
                            .size(54.dp)
                            .background(WarmGold.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .border(1.dp, WarmGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = pair.first,
                                contentDescription = pair.second,
                                tint = WarmGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Stage 1 & 2: The Sliding Light Gate Aperture
            EktefaaTwoPanelsGate(
                isOpen = isGateOpen,
                centerContent = {
                    GoldenShieldIcon(
                        modifier = Modifier.size(100.dp),
                        glowRadius = 15f
                    )
                },
                revealedContent = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Text(
                            text = "أهلاً بكم في تطبيق اكتفاء - رفيقك الشامل الذي يجمع كل ما تحتاجه في مكان واحد",
                            color = WarmGold,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 26.sp,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = WarmGold.copy(0.4f),
                                    offset = Offset(0f, 2f),
                                    blurRadius = 6f
                                )
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Tribute Card: Developer ادريس يوسف المداني
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(0.92f)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, WarmGold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(20.dp, RoundedCornerShape(16.dp), spotColor = WarmGold, ambientColor = WarmGold)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "إعداد وتطوير المصمم والمبرمج",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "إدريس يوسف المداني ✍️",
                                    color = WarmGold,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    style = TextStyle(
                                        shadow = Shadow(
                                            color = WarmGold.copy(0.6f),
                                            blurRadius = 12f
                                        )
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = WarmGold.copy(0.2f), thickness = 1.dp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "في تطبيقٍ واحد، تلتقي كل تفاصيل حياتك، من متابعة من تحب في دروسهم وحفظهم للقرآن، إلى تنظيم شؤونك أياً كانت، في البيت أو في العمل أو في أي وجهة تمضي إليها. هنا حيث تتصفح وسائل تواصلك، وتحفظ خواطرك في حافظةٍ ذكية تفرزها لك، وحيث تفتح مكتبتك الثقافية لتنهل من الثقافة القرآنية وروائع الأدب وجواهر الحكمة. في اكتفاء، لا تحتاج إلى غيره، لأن كل ما يهمك قد اجتمع في كفٍّ واحد، يفيض بالخير والراحة، ويجعل حياتك أكثر اتساقًا وطمأنينة.",
                                    color = TextLight,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        if (isAssetLoading) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = WarmGold, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(assetProgress, color = WarmGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    onSkip()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .height(50.dp)
                                        .shadow(15.dp, RoundedCornerShape(12.dp), spotColor = WarmGold)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        "بِسْمِ اللَّهِ.. ادْخُلْ الآنَ 🚀",
                                        color = MidnightBlue,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

// 1b. The Two-Panel Sliding Gateway (Dual Door Aperture with trailing radiant glow)
@Composable
fun EktefaaTwoPanelsGate(
    isOpen: Boolean,
    centerContent: @Composable () -> Unit = {},
    revealedContent: @Composable () -> Unit = {}
) {
    val leftOffsetAnim by animateDpAsState(
        targetValue = if (isOpen) (-350).dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "LeftGateOffset"
    )
    val rightOffsetAnim by animateDpAsState(
        targetValue = if (isOpen) 350.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "RightGateOffset"
    )
    val gateAlpha by animateFloatAsState(
        targetValue = if (isOpen) 0f else 1f,
        animationSpec = tween(800),
        label = "GateAlpha"
    )
    val trailingGlowAlpha by animateFloatAsState(
        targetValue = if (isOpen) 0f else 0.9f,
        animationSpec = tween(1200),
        label = "TrailingGlow"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Revealed Content Area behind gates
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            revealedContent()
        }

        // Trailing Light Leak Glow layer
        if (!isOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(WarmGold.copy(alpha = trailingGlowAlpha), Color.Transparent),
                            radius = 1200f
                        )
                    )
            )
        }

        // 2. Left sliding gate
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .align(Alignment.CenterStart)
                .offset(x = leftOffsetAnim)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF070B14), Color(0xFF131A2B))
                    )
                )
                .border(2.dp, WarmGold.copy(0.4f), RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 24.dp, bottomEnd = 24.dp)),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(WarmGold.copy(alpha = gateAlpha))
            )
        }

        // 3. Right sliding gate
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .align(Alignment.CenterEnd)
                .offset(x = rightOffsetAnim)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF131A2B), Color(0xFF070B14))
                    )
                )
                .border(2.dp, WarmGold.copy(0.4f), RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 0.dp, bottomEnd = 0.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(WarmGold.copy(alpha = gateAlpha))
            )
        }

        // 4. Center emblem and label
        AnimatedVisibility(
            visible = !isOpen,
            enter = fadeIn(tween(400)) + scaleIn(tween(400)),
            exit = fadeOut(tween(400)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF131A2B), Color(0xFF030712))
                        )
                    )
                    .border(3.dp, WarmGold, androidx.compose.foundation.shape.CircleShape)
                    .shadow(16.dp, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                centerContent()
            }
        }
    }
}

// 1c. Cinematic Exit Portal Container
@Composable
fun EktefaaExitPortal(
    onDismiss: () -> Unit,
    onFinish: () -> Unit
) {
    var isGateOpen by remember { mutableStateOf(true) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        // Play the dramatic wada_an_ektefaa sound track
        EktefaaAudioManager.stopBackgroundLoop(context)
        EktefaaAudioManager.playSoundOnce(context, R.raw.wada_an_ektefaa)
        delay(100)
        isGateOpen = false // close doors!
        delay(2600)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        EktefaaTwoPanelsGate(
            isOpen = isGateOpen,
            centerContent = {
                Text(
                    text = "فِي أَمَانِ اللَّهِ",
                    color = WarmGold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        shadow = Shadow(
                            color = WarmGold.copy(0.7f),
                            blurRadius = 10f
                        )
                    )
                )
            },
            revealedContent = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "وداعاً عائلة اكتفاء.. نراكم على خير 🕊️",
                            color = WarmGold,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = WarmGold.copy(0.6f),
                                    blurRadius = 15f
                                )
                            )
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            "استودعكم الله الذي لا تضيع ودائعه",
                            color = Color.White.copy(0.7f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        )
    }
}

// 2. Multi-Role Login gate with dynamic DB profiles and gold accents - 'بوابة الاكتفاء'
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EktefaaLoginGate(
    viewModel: EktefaaViewModel,
    onLoginSuccess: (String, String) -> Unit
) {
    val usersList by viewModel.allUsers.collectAsState(initial = emptyList())
    var currentSelectedUser by remember { mutableStateOf<com.example.database.User?>(null) }
    var pinText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showUserDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(usersList) {
        if (currentSelectedUser == null && usersList.isNotEmpty()) {
            currentSelectedUser = usersList.find { it.role == "Father" } ?: usersList.first()
        }
    }

    // Breathing logo scale transformation
    val infiniteTransition = rememberInfiniteTransition(label = "LoginLogoScaleTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseLogo"
    )

    // Button pulse scale transformation
    val buttonScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseButton"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(MidnightBlue, Color.Black)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        CosmosStarsBackground()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 2. Head layout: Golden Shield App Icon
            GoldenShieldIcon(
                modifier = Modifier
                    .size(80.dp)
                    .scale(pulseScale),
                glowRadius = 15f
            )

            Text(
                "بوابة الاكتفاء",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = WarmGold,
                style = TextStyle(
                    shadow = Shadow(
                        color = WarmGold.copy(alpha = 0.5f),
                        offset = Offset(0f, 2f),
                        blurRadius = 8f
                    )
                )
            )

            Text(
                "الرجاء اختيار اسم المستخدم وإدخال رمز الأمان للولوج الأمن:",
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Username selector (Glassmorphism design)
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = currentSelectedUser?.name ?: "يرجى تحديد اسم المستخدم",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                        focusedBorderColor = WarmGold,
                        unfocusedBorderColor = WarmGold.copy(0.4f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = WarmGold) },
                    trailingIcon = {
                        IconButton(onClick = { showUserDropdown = true }) {
                            Icon(Icons.Default.ArrowDropDown, null, tint = WarmGold)
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                // Hidden transparent overlay to easily click the whole textfield
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showUserDropdown = true }
                )

                DropdownMenu(
                    expanded = showUserDropdown,
                    onDismissRequest = { showUserDropdown = false },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .background(MidnightBlue)
                        .border(1.dp, WarmGold, RoundedCornerShape(8.dp))
                ) {
                    usersList.forEach { user ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = user.name + " (" + when (user.role) {
                                        "Father" -> "المشرف العام"
                                        "Mother" -> "المتابع المساند"
                                        else -> "المستفيد"
                                    } + ")",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            },
                            onClick = {
                                currentSelectedUser = user
                                pinText = ""
                                errorMessage = ""
                                showUserDropdown = false
                            }
                        )
                    }
                }
            }

            // 4. Word Passcode entry (Glassmorphism design)
            OutlinedTextField(
                value = pinText,
                onValueChange = { pinText = it; errorMessage = "" },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword
                ),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                placeholder = { Text("أدخل رمز الأمان", color = Color.White.copy(0.5f)) },
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = WarmGold) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = WarmGold
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.1f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                    focusedBorderColor = WarmGold,
                    unfocusedBorderColor = WarmGold.copy(0.4f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            if (errorMessage.isNotBlank()) {
                Text(errorMessage, color = Color(0xFFFF5252), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 5. Secure hint card
            Card(
                colors = CardDefaults.cardColors(containerColor = WarmGold.copy(0.08f)),
                border = BorderStroke(0.5.dp, WarmGold.copy(0.3f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "💡 الرمز الافتراضي لحساب المشرف العام الرئيسي هو: 1234",
                        color = GoldLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 6. Login Action buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val sel = currentSelectedUser
                        if (sel != null) {
                            if (sel.pinCode.isEmpty() || pinText == sel.pinCode) {
                                onLoginSuccess(sel.role, sel.name)
                            } else {
                                errorMessage = "من فضلك أدخل رمز الأمان الصحيح!"
                            }
                        } else {
                            errorMessage = "يرجى اختيار مستخدم أولاً."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .scale(buttonScale)
                        .shadow(12.dp, RoundedCornerShape(12.dp), spotColor = WarmGold),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("دخول", color = MidnightBlue, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }

                Button(
                    onClick = {
                        // Demo immediate login bypass
                        onLoginSuccess("Father", "المشرف العام")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenAccent.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GreenAccent)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = GreenAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("الدخول الـمباشر كـمشرف ⚡", color = GreenAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// 2.5 Dynamic Family Accounts Settings dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyAccountSettingsDialog(
    viewModel: EktefaaViewModel,
    onDismiss: () -> Unit
) {
    val usersList by viewModel.allUsers.collectAsState(initial = emptyList())
    var newUserName by remember { mutableStateOf("") }
    var newUserPin by remember { mutableStateOf("") }
    var newUserRole by remember { mutableStateOf("Child") } // "Father", "Mother", "Child"
    var showAddUserSection by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "إشراف وضبط صلاحيات العائلة",
                color = WarmGold,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "قائمة حسابات الحماية المثبتة على قاعدة البيانات:",
                    fontSize = 12.sp,
                    color = Color.White.copy(0.7f),
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                // List existing users
                usersList.forEach { user ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.5.dp, WarmGold.copy(0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "رقم ${user.id} : ${user.name}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                val roleAr = when (user.role) {
                                    "Father" -> "الوالد (مشرف)"
                                    "Mother" -> "الأم (مساعد)"
                                    else -> "الابن (مستعرض)"
                                }
                                Text(
                                    "الصلاحية: $roleAr | رمز PIN: ${if (user.pinCode.isEmpty()) "لا يوجد" else user.pinCode}",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                            // Don't allow deleting the root/default admin if it's the only one left to prevent lockouts
                            val canDelete = usersList.size > 1 && user.id != 1
                            if (canDelete) {
                                IconButton(onClick = { viewModel.deleteUser(user.id) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "حذف الحساب",
                                        tint = RedAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Divider(color = WarmGold.copy(0.2f), thickness = 1.dp)

                if (!showAddUserSection) {
                    Button(
                        onClick = { showAddUserSection = true },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MidnightBlue)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة مستخدم عائلي جديد", color = MidnightBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, WarmGold.copy(0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "بيانات الحساب الجديد",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = WarmGold
                            )

                            OutlinedTextField(
                                value = newUserName,
                                onValueChange = { newUserName = it },
                                label = { Text("اسم الحساب (مثال: الأم مريم)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WarmGold,
                                    unfocusedBorderColor = WarmGold.copy(0.4f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            OutlinedTextField(
                                value = newUserPin,
                                onValueChange = { newUserPin = it },
                                label = { Text("كلمة المرور / رمز PIN الحامي") },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WarmGold,
                                    unfocusedBorderColor = WarmGold.copy(0.4f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Text("تحديد الدور والصلاحية التراخيص:", fontSize = 10.sp, color = TextMuted)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Father" to "مشرف", "Mother" to "مساعد", "Child" to "ابن").forEach { (roleCode, roleName) ->
                                    val isSelected = newUserRole == roleCode
                                    Button(
                                        onClick = { newUserRole = roleCode },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) WarmGold else SurfaceDark
                                        ),
                                        border = BorderStroke(1.dp, WarmGold),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            roleName,
                                            color = if (isSelected) MidnightBlue else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (newUserName.isNotBlank()) {
                                            viewModel.addUser(newUserName, newUserRole, newUserPin)
                                            newUserName = ""
                                            newUserPin = ""
                                            newUserRole = "Child"
                                            showAddUserSection = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("تسجيل", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = { showAddUserSection = false },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("إلغاء", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = WarmGold)
            ) {
                Text("إغلاق ضبط الحسابات", color = MidnightBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    )
}

// 3. Main Dashboard grid with cinematic 3D premium gold cards
@Composable
fun EktefaaDashboard(
    userName: String,
    userRole: String,
    isMusicMuted: Boolean = false,
    onToggleMusic: () -> Unit = {},
    onNavigate: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
    onExitApp: () -> Unit = {}
) {
    val items = listOf(
        DashboardCard("الصرح الاجتماعي والأسري الشامل", "FAMILY_HUB", Icons.Default.FamilyRestroom, "يحتوي على القرآن، المناهج، وسائل الترفيه والتواصل، برنامج رجال الله والميزانية الأسرية"),
        DashboardCard("البوابة الصحية والطبية لسلامة الأسرة", "HEALTH_HUB", Icons.Default.LocalHospital, "يحتوي على تتبع المرضى، الأدوية الموصوفة والجرعات وعيادة الذكاء الاصطناعي")
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "تطبيق اكتفاء 🛡️",
                            fontWeight = FontWeight.Bold,
                            color = WarmGold,
                            fontSize = 20.sp,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color.Black.copy(0.7f),
                                    offset = Offset(0f, 1.5f),
                                    blurRadius = 4f
                                )
                            )
                        )
                        Text(
                            "اسم المستخدم: $userName",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onToggleMusic,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(WarmGold.copy(alpha = 0.12f))
                                .border(1.0.dp, WarmGold.copy(0.4f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                if (isMusicMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "Muted",
                                tint = WarmGold
                            )
                        }
                        IconButton(
                            onClick = { onNavigate("DESIGN_OASIS") },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(WarmGold.copy(alpha = 0.12f))
                                .border(1.0.dp, WarmGold.copy(0.4f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.Brush, contentDescription = "Design Oasis", tint = WarmGold)
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(WarmGold.copy(alpha = 0.12f))
                                .border(1.0.dp, WarmGold.copy(0.4f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Family Settings", tint = WarmGold)
                        }
                        IconButton(
                            onClick = onLogout,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(WarmGold.copy(alpha = 0.12f))
                                .border(1.0.dp, WarmGold.copy(0.4f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = WarmGold)
                        }
                        IconButton(
                            onClick = onExitApp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                .border(1.0.dp, Color(0xFFEF4444).copy(0.4f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = "Exit Portal", tint = Color(0xFFFCA5A5))
                        }
                    }
                }
                Divider(color = WarmGold.copy(alpha = 0.15f), thickness = 1.dp)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            CosmosStarsBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                // Majestic 3D Quote banner at the top
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .shadow(10.dp, RoundedCornerShape(18.dp), spotColor = WarmGold)
                        .border(
                            1.dp,
                            Brush.linearGradient(listOf(GoldLight, WarmGold.copy(0.3f), GoldLight)),
                            RoundedCornerShape(18.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(CardGradStart, CardGradEnd)
                                )
                            )
                            .padding(14.dp)
                    ) {
                        Text(
                            "\"وَمَنْ يَتَّقِ اللَّهَ يَجْعَلْ لَهُ مَخْرَجًا وَيَرْزُقْهُ مِنْ حَيْثُ لَا يَحْتَسِبُ\"",
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = WarmGold,
                            fontSize = 13.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                // Beautiful Active Academic Advisor Card embodying the "150 Ideas"
                var currentIdeaIndex by remember { mutableStateOf(0) }
                val ideasList = remember {
                    listOf(
                        Triple(1, "خبير تحضير الدروس التلقائي بالذكاء الاصطناعي", "أدوات المعلم والمربي الذكي"),
                        Triple(10, "جدول مكافآت الحفظ المتزامن لجميع الأخوة", "أدوات المعلم والمربي الذكي"),
                        Triple(37, "تفكيك قواعد كان وأخواتها بأسلوب اللعب والمرح", "شروح وتفسيرات المناهج"),
                        Triple(61, "مولّد اختبارات الاختيار من متعدد الفورية", "الواجبات والاختبارات التفاعلية"),
                        Triple(91, "رسائل الواتساب الدورية التلقائية لنتائج الحفظ للأب", "التواصل والتقارير الأكاديمية"),
                        Triple(121, "مساعد التكرار المتباعد لتثبيت حفظ القرآن وتكراره", "تحسين مهارات القراءة والحفظ"),
                        Triple(130, "تتبع عدد صفحات المصحف المنجزة والنسبة المئوية للمستهدف", "تحسين مهارات القراءة والحفظ")
                    )
                }
                val activeIdea = ideasList[currentIdeaIndex % ideasList.size]

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(
                            1.dp,
                            Brush.linearGradient(listOf(WarmGold.copy(0.5f), Color.Transparent, GreenAccent.copy(0.4f))),
                            RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWarm.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(WarmGold.copy(0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "فكرة #${activeIdea.first}",
                                        color = WarmGold,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "من ركيزة مساعد اكتفاء الأكاديمي الذكي (150 فكرة)",
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            IconButton(
                                onClick = { currentIdeaIndex++ },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Next Idea", tint = WarmGold, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            activeIdea.second,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "التصنيف الحالي: ${activeIdea.third}",
                            color = TextMuted,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "تحديث حي ومباشر ومدرج بالقاعدة ⚡",
                                fontSize = 9.sp,
                                color = GreenAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { onNavigate("ACADEMIC_HELPER") },
                                colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MidnightBlue, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("تشغيل الفكرة الآن", color = MidnightBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Text("لوحة العمليات والخدمات الذكية للأسرة:", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(items) { card ->
                        Cinematic3DCard(
                            title = card.title,
                            subtitle = card.subtitle,
                            icon = card.icon,
                            onClick = { onNavigate(card.screen) }
                        )
                    }
                }
            }
        }
    }
}

// Custom 3D Cinematic elevated layout for the dashboard items
@Composable
fun Cinematic3DCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1.0f, label = "cardScale")
    val glowShadow = WarmGold.copy(alpha = 0.35f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.15f)
            .scale(scale)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = glowShadow,
                spotColor = WarmGold
            )
            .clip(RoundedCornerShape(22.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SurfaceDark.copy(alpha = 0.95f),
                        CardGradEnd
                    )
                )
            )
            .clickable {
                onClick()
            }
            .border(
                BorderStroke(
                    1.5.dp,
                    Brush.linearGradient(
                        listOf(GoldLight, WarmGold.copy(alpha = 0.4f), GoldLight)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(2.dp)
    ) {
        // Metallic gold decorative ribbon corner accent
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(width = 30.dp, height = 4.dp)
                .clip(RoundedCornerShape(bottomStart = 4.dp))
                .background(Brush.horizontalGradient(listOf(GoldLight, WarmGold)))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = WarmGold)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(WarmGold.copy(0.25f), Color.Transparent)
                        )
                    )
                    .border(1.dp, WarmGold.copy(0.4f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = GoldLight,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = TextLight,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black,
                        offset = Offset(0f, 2f),
                        blurRadius = 4f
                    )
                )
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                subtitle,
                fontSize = 9.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

data class DashboardCard(
    val title: String,
    val screen: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val subtitle: String
)

// 4. Custom animated Cosmos star background for Dark theme (with tiny twinkles)
@Composable
fun CosmosStarsBackground() {
    val isDark = isSystemInDarkTheme()
    if (!isDark) return

    val infiniteTransition = rememberInfiniteTransition(label = "TwinkleTransition")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaTwinkle"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Draw static but twinkling stars
        val stars = listOf(
            Pair(0.12f, 0.15f), Pair(0.85f, 0.25f), Pair(0.45f, 0.35f),
            Pair(0.22f, 0.65f), Pair(0.72f, 0.55f), Pair(0.35f, 0.85f),
            Pair(0.61f, 0.78f), Pair(0.91f, 0.91f), Pair(0.08f, 0.48f)
        )
        stars.forEach { (x, y) ->
            drawCircle(
                color = WarmGold.copy(alpha = alphaAnim),
                radius = 4f,
                center = androidx.compose.ui.geometry.Offset(x * size.width, y * size.height)
            )
        }
    }
}

// 5. Consolidated Family & Social Hub screen grouping all secondary tools
@Composable
fun EktefaaFamilyHubScreen(
    userName: String,
    userRole: String,
    isMusicMuted: Boolean = false,
    onToggleMusic: () -> Unit = {},
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val items = listOf(
        DashboardCard("ركن الاستراحة والمسابقات", "LEISURE", androidx.compose.material.icons.Icons.Default.Extension, "ألعاب عائلية، صح وخطأ، وفراغات بمسابقات وعي عائلية قرآنية"),
        DashboardCard("المكتبة المدرسية", "LIBRARY", androidx.compose.material.icons.Icons.Default.LibraryBooks, "كتب المقررات الرسمية والتحميل"),
        DashboardCard("المكتبة الثقافية الشاملة", "CULTURAL_LIBRARY", androidx.compose.material.icons.Icons.Default.MenuBook, "مجلدات فكرية، بطاقات توعوية ونصوص"),
        DashboardCard("وسائل التواصل ويب", "SOCIAL", androidx.compose.material.icons.Icons.Default.Language, "تصفح والتقاط الحكمة"),
        DashboardCard("الحافظة الذكية العائلية", "CLIPBOARD", androidx.compose.material.icons.Icons.Default.ContentCopy, "مشاركة النصوص والخطب"),
        DashboardCard("متابعة الأبناء والتحصيل", "CHILDREN", androidx.compose.material.icons.Icons.Default.FamilyRestroom, "تقييم يومي وحصص"),
        DashboardCard("مساعد اكتفاء الأكاديمي", "ACADEMIC_HELPER", androidx.compose.material.icons.Icons.Default.SmartToy, "150 برنامجاً وإرشاداً دراسياً لتفوق الأبناء"),
        DashboardCard("مركز تواصل اكتفاء الذكي", "COMMUNICATION_CENTER", androidx.compose.material.icons.Icons.Default.Chat, "دمج الرسائل والأسماء بالفلترة والنسخ للأسرة"),
        DashboardCard("مناهج المقرّرات المقارنة", "CURRICULUM", androidx.compose.material.icons.Icons.Default.LibraryBooks, "تضمين المقررات الدراسية"),
        DashboardCard("القرآن الكريم والورد", "QURAN", androidx.compose.material.icons.Icons.Default.AutoStories, "المنشاوي وتتبع الأخطاء"),
        DashboardCard("سجل برنامج رجال الله", "PRAYER", androidx.compose.material.icons.Icons.Default.DoneAll, "خمس صلوات وقيام الليل"),
        DashboardCard("الحاسبة العلمية والخطوات", "CALCULATOR", androidx.compose.material.icons.Icons.Default.Functions, "تصوير وحلول الخطوات"),
        DashboardCard("المالية والمصروفات والديون", "FINANCES", androidx.compose.material.icons.Icons.Default.AccountBalanceWallet, "صندوق التوفير وحسابات الأب"),
        DashboardCard("سلة الميزات والخدمات", "EXTRA", androidx.compose.material.icons.Icons.Default.Apps, "المسبحة والأطباء والصيانة"),
        DashboardCard("الفتاوى الفقهية العائلية", "FATWAS", androidx.compose.material.icons.Icons.Default.Gavel, "فتاوى ومسائل فقهية شاملة مستوردة مع سجل زيارات وإحصاءات ملونة وتقارير للسايلين"),
        DashboardCard("برنامج رجال الله الجديد", "MEN_OF_GOD", androidx.compose.material.icons.Icons.Default.SelfImprovement, "برنامج رجال الله والتقييم اليومي والوعي الإيماني"),
        DashboardCard("برنامج الأدعية والصلة", "PRAYERS", androidx.compose.material.icons.Icons.Default.MenuBook, "مكتبة الأدعية والزيارات وأدعية الوقت والقرآن والتمجيد")
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(WarmGold.copy(alpha = 0.12f))
                            .border(1.0.dp, WarmGold.copy(0.4f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.ArrowBack, contentDescription = "Back", tint = WarmGold)
                    }
                    Text(
                        "الصرح الأسرى والاجتماعي الشامل",
                        fontWeight = FontWeight.Bold,
                        color = WarmGold,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )
                }
                Divider(color = WarmGold.copy(alpha = 0.15f), thickness = 1.dp)
            }
        },
        containerColor = DeepObsidian
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            CosmosStarsBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(items) { card ->
                        Cinematic3DCard(
                            title = card.title,
                            subtitle = card.subtitle,
                            icon = card.icon,
                            onClick = { onNavigate(card.screen) }
                        )
                    }
                }
            }
        }
    }
}

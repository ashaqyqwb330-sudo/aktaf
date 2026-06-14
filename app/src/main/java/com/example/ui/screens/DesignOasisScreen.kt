package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignOasisScreen(
    onBack: () -> Unit
) {
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    
    // Customization states for 100 ideas bridged to GlobalDesignConfig for live application-wide updates
    var activeThemeIdx by remember {
        object : MutableState<Int> {
            override var value: Int
                get() = GlobalDesignConfig.activeThemeIdx
                set(v) { GlobalDesignConfig.activeThemeIdx = v }
            override fun component1() = value
            override fun component2(): (Int) -> Unit = { value = it }
        }
    }
    var strokeThickness by remember {
        object : MutableState<Float> {
            override var value: Float
                get() = GlobalDesignConfig.strokeThickness
                set(v) { GlobalDesignConfig.strokeThickness = v }
            override fun component1() = value
            override fun component2(): (Float) -> Unit = { value = it }
        }
    }
    var blurRadiusState by remember {
        object : MutableState<Float> {
            override var value: Float
                get() = GlobalDesignConfig.blurRadiusState
                set(v) { GlobalDesignConfig.blurRadiusState = v }
            override fun component1() = value
            override fun component2(): (Float) -> Unit = { value = it }
        }
    }
    var cardSpacingState by remember { mutableStateOf(14.dp) }
    var activeCornerShapeIdx by remember {
        object : MutableState<Int> {
            override var value: Int
                get() = GlobalDesignConfig.activeCornerShapeIdx
                set(v) { GlobalDesignConfig.activeCornerShapeIdx = v }
            override fun component1() = value
            override fun component2(): (Int) -> Unit = { value = it }
        }
    }
    var starCountState by remember {
        object : MutableState<Int> {
            override var value: Int
                get() = GlobalDesignConfig.starCountState
                set(v) { GlobalDesignConfig.starCountState = v }
            override fun component1() = value
            override fun component2(): (Int) -> Unit = { value = it }
        }
    }
    var starSpeedState by remember {
        object : MutableState<Float> {
            override var value: Float
                get() = GlobalDesignConfig.starSpeedState
                set(v) { GlobalDesignConfig.starSpeedState = v }
            override fun component1() = value
            override fun component2(): (Float) -> Unit = { value = it }
        }
    }
    var textLetterSpacing by remember { mutableStateOf(1.2f) }
    var shimmerEnabled by remember {
        object : MutableState<Boolean> {
            override var value: Boolean
                get() = GlobalDesignConfig.shimmerEnabled
                set(v) { GlobalDesignConfig.shimmerEnabled = v }
            override fun component1() = value
            override fun component2(): (Boolean) -> Unit = { value = it }
        }
    }
    var interactiveConstellations by remember {
        object : MutableState<Boolean> {
            override var value: Boolean
                get() = GlobalDesignConfig.interactiveConstellations
                set(v) { GlobalDesignConfig.interactiveConstellations = v }
            override fun component1() = value
            override fun component2(): (Boolean) -> Unit = { value = it }
        }
    }
    var audioVisualizerActive by remember { mutableStateOf(true) }
    var archSilhouetteActive by remember { mutableStateOf(true) }
    var gradientGlossValue by remember { mutableStateOf(0.81f) }

    val categories = listOf(
        "لوحات الألوان الملكية الشفافة (1-20)",
        "الخطوط والظلال والهالات المتوهجة (21-40)",
        "الإطارات المزدوجة والنقوش الهندسية (41-60)",
        "جسيمات النجوم المتلألئة المترابطة (61-80)",
        "الأبعاد والانعكاسات الحركية الفاخرة (81-100)"
    )

    // Dynamic selected colors depending on theme configuration
    val currentPrimColor = when (activeThemeIdx) {
        0 -> WarmGold
        1 -> Color(0xFF43C586) // Caliphate Green
        2 -> Color(0xFFD34545) // Ruby Crimson
        3 -> Color(0xFF9E77ED) // Royal Amethyst
        else -> Color(0xFFE2B83E) // Hyper gold
    }

    val currentBgColors = when (activeThemeIdx) {
        0 -> listOf(Color(0xFF04060C), Color(0xFF090D1A), Color(0xFF0E162D))
        1 -> listOf(Color(0xFF020E08), Color(0xFF041E12), Color(0xFF092C1D))
        2 -> listOf(Color(0xFF0C0202), Color(0xFF1E0404), Color(0xFF2E0909))
        3 -> listOf(Color(0xFF09040E), Color(0xFF160925), Color(0xFF220E37))
        else -> listOf(Color(0xFF070502), Color(0xFF1A1206), Color(0xFF281C09))
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "واحة التصميم الفاخر والأناقة الرسومية",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentPrimColor
                        )
                        Text(
                            "لوحة تخصيص 100 فكرة تصميمية حية وتفاعلية",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "عودة",
                            tint = currentPrimColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = currentBgColors[0]
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(currentBgColors)
                )
                .padding(innerPadding)
        ) {
            // Interactive 3D Stars matching current configuration
            OasisStarsParticles(
                count = starCountState,
                speedMultiplier = starSpeedState,
                starColor = currentPrimColor,
                drawLines = interactiveConstellations
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Majestic Islamic Card Preview - CHANGES LIVE based on values
                DesignShowcasePreview(
                    primaryColor = currentPrimColor,
                    strokeThickness = strokeThickness,
                    glowBlur = blurRadiusState,
                    cornerType = activeCornerShapeIdx,
                    letterSpacing = textLetterSpacing,
                    shimmerOn = shimmerEnabled,
                    audioAnimOn = audioVisualizerActive,
                    archSilhouetteOn = archSilhouetteActive,
                    glossAlpha = gradientGlossValue,
                    themeName = when (activeThemeIdx) {
                        0 -> "الفضاء والأوبسيديان الذهبي"
                        1 -> "الزمرد النبوي والخلافة"
                        2 -> "الياقوت الأندلسي الداكن"
                        3 -> "الجمشت الإمبراطوري المخملي"
                        else -> "الذهب السيبراني فائق التوهج"
                    }
                )

                // Category Tabs for selecting which of the 100 ideas to adjust/view list
                ScrollableTabRow(
                    selectedTabIndex = selectedCategoryIndex,
                    containerColor = Color.Transparent,
                    contentColor = currentPrimColor,
                    edgePadding = 2.dp
                ) {
                    categories.forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedCategoryIndex == idx,
                            onClick = { selectedCategoryIndex = idx }
                        ) {
                            Text(
                                text = title,
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedCategoryIndex == idx) currentPrimColor else TextMuted
                            )
                        }
                    }
                }

                // Showcase Settings & Control Center
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Left Column: The actual list of the design ideas under this category
                    Card(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = currentBgColors[1].copy(alpha = 0.85f)),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, currentPrimColor.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "الأفكار الجمالية المفعلة في هذا الباب:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = currentPrimColor
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            val currentIdeas = getIdeasForCategory(selectedCategoryIndex)
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                itemsIndexed(currentIdeas) { index, ideaText ->
                                    val ideaNumber = selectedCategoryIndex * 20 + index + 1
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.04f))
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(currentPrimColor.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "$ideaNumber",
                                                color = currentPrimColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                ideaText,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                lineHeight = 14.sp
                                            )
                                            Text(
                                                "مفعلّة وجاهزة للتشغيل ⚡",
                                                color = Color(0xFF4CAF50),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Right Column: Interactive control nodes to adjust parameters
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        colors = CardDefaults.cardColors(containerColor = currentBgColors[1].copy(alpha = 0.65f)),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, currentPrimColor.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "تعديل المظهر الحركي:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = currentPrimColor
                            )

                            // Theme Selector
                            Text("محور لوحة الألوان المحددة:", fontSize = 9.sp, color = TextMuted)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(0, 1, 2, 3, 4).forEach { idx ->
                                    val isSel = activeThemeIdx == idx
                                    val clr = when (idx) {
                                        0 -> Color(0xFFC59A28)
                                        1 -> Color(0xFF43C586)
                                        2 -> Color(0xFFD34545)
                                        3 -> Color(0xFF9E77ED)
                                        else -> Color(0xFFE2B83E)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(clr)
                                            .border(
                                                2.dp,
                                                if (isSel) Color.White else Color.Transparent,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { activeThemeIdx = idx }
                                    )
                                }
                            }

                            // Shape Selector
                            Text("شكل حواف الأركان (41-45):", fontSize = 9.sp, color = TextMuted)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("دائري", "معياري", "حاد", "قطرة").forEachIndexed { idx, value ->
                                    val isSel = activeCornerShapeIdx == idx
                                    Button(
                                        onClick = { activeCornerShapeIdx = idx },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(26.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSel) currentPrimColor else Color.Transparent
                                        ),
                                        border = BorderStroke(1.dp, currentPrimColor)
                                    ) {
                                        Text(
                                            value,
                                            fontSize = 8.sp,
                                            color = if (isSel) MidnightBlue else Color.White
                                        )
                                    }
                                }
                            }

                            Divider(color = currentPrimColor.copy(alpha = 0.2f))

                            // Thickness slider
                            Text("سماكة الهيكل والخط الذهبي: ${String.format("%.1f", strokeThickness)}", fontSize = 9.sp, color = textGlossyHighlightLabel(currentPrimColor))
                            Slider(
                                value = strokeThickness,
                                onValueChange = { strokeThickness = it },
                                valueRange = 0.5f..8f,
                                colors = SliderDefaults.colors(activeTrackColor = currentPrimColor, thumbColor = currentPrimColor)
                            )

                            // Halo glow slider
                            Text("توهج الهالة الضوئية: ${blurRadiusState.toInt()}px", fontSize = 9.sp, color = textGlossyHighlightLabel(currentPrimColor))
                            Slider(
                                value = blurRadiusState,
                                onValueChange = { blurRadiusState = it },
                                valueRange = 2f..30f,
                                colors = SliderDefaults.colors(activeTrackColor = currentPrimColor, thumbColor = currentPrimColor)
                            )

                            // Stars settings
                            Text("مجموع نجوم الفضاء: $starCountState", fontSize = 9.sp, color = textGlossyHighlightLabel(currentPrimColor))
                            Slider(
                                value = starCountState.toFloat(),
                                onValueChange = { starCountState = it.toInt() },
                                valueRange = 5f..50f,
                                colors = SliderDefaults.colors(activeTrackColor = currentPrimColor, thumbColor = currentPrimColor)
                            )

                            // Speed stars settings
                            Text("سرعة تذبذب اللمعان: ${String.format("%.1f", starSpeedState)}x", fontSize = 9.sp, color = textGlossyHighlightLabel(currentPrimColor))
                            Slider(
                                value = starSpeedState,
                                onValueChange = { starSpeedState = it },
                                valueRange = 0.5f..5.0f,
                                colors = SliderDefaults.colors(activeTrackColor = currentPrimColor, thumbColor = currentPrimColor)
                            )

                            // Character spacing settings
                            Text("تباعد الحروف والآيات: ${String.format("%.1f", textLetterSpacing)}sp", fontSize = 9.sp, color = textGlossyHighlightLabel(currentPrimColor))
                            Slider(
                                value = textLetterSpacing,
                                onValueChange = { textLetterSpacing = it },
                                valueRange = 0.5f..5.0f,
                                colors = SliderDefaults.colors(activeTrackColor = currentPrimColor, thumbColor = currentPrimColor)
                            )

                            // Toggles
                            CustomToggleRow("تأثير البريق الحركي ✨", shimmerEnabled) { shimmerEnabled = it }
                            CustomToggleRow("توصيل النجميات اللمسي 🗺️", interactiveConstellations) { interactiveConstellations = it }
                            CustomToggleRow("نموذج المذبذب الصوتي البصري 📊", audioVisualizerActive) { audioVisualizerActive = it }
                            CustomToggleRow("خلفية المحراب الهندسية 🕌", archSilhouetteActive) { archSilhouetteActive = it }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun textGlossyHighlightLabel(prim: Color): Color = prim.copy(alpha = 0.85f)

@Composable
fun CustomToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 9.sp, color = Color.White)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = WarmGold,
                checkedTrackColor = WarmGold.copy(0.3f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.LightGray.copy(0.2f)
            ),
            modifier = Modifier.scale(0.7f)
        )
    }
}

// Preview card component matching all dynamic characteristics
@Composable
fun DesignShowcasePreview(
    primaryColor: Color,
    strokeThickness: Float,
    glowBlur: Float,
    cornerType: Int,
    letterSpacing: Float,
    shimmerOn: Boolean,
    audioAnimOn: Boolean,
    archSilhouetteOn: Boolean,
    glossAlpha: Float,
    themeName: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PreviewAnims")
    
    // Pulse scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulsePreview"
    )

    // Shimmer translate
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -500f,
        targetValue = 700f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val cornerRadius = when (cornerType) {
        0 -> 32.dp
        1 -> 16.dp
        2 -> 0.dp // Strict brutalist sharp corners
        else -> 24.dp
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .scale(pulseScale)
            .shadow(
                elevation = glowBlur.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = primaryColor,
                spotColor = primaryColor
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        SurfaceDark,
                        CardGradEnd
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(300f, 400f)
                )
            )
            .border(
                BorderStroke(
                    strokeThickness.dp,
                    Brush.sweepGradient(
                        listOf(primaryColor, primaryColor.copy(0.3f), primaryColor)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(14.dp)
    ) {
        // Geometric Arch DrawBehind pattern
        if (archSilhouetteOn) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val path = android.graphics.Path().apply {
                    moveTo(w * 0.2f, h)
                    lineTo(w * 0.2f, h * 0.4f)
                    cubicTo(w * 0.2f, h * 0.1f, w * 0.5f, h * 0.05f, w * 0.5f, h * 0.05f)
                    cubicTo(w * 0.5f, h * 0.05f, w * 0.8f, h * 0.1f, w * 0.8f, h * 0.4f)
                    lineTo(w * 0.8f, h)
                    close()
                }
                drawContext.canvas.nativeCanvas.drawPath(
                    path,
                    android.graphics.Paint().apply {
                        color = primaryColor.toArgb()
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 1.5f
                        alpha = (25 * glossAlpha).toInt()
                        pathEffect = android.graphics.DashPathEffect(floatArrayOf(5f, 5f), 0f)
                    }
                )
            }
        }

        // Metallic golden ribbon corner highlight
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(40.dp, 6.dp)
                .background(Brush.horizontalGradient(listOf(primaryColor, Color.White, primaryColor)))
        )

        // Textual display using parameters
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Stars,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "تأثيرات دينية فاخرة",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    themeName,
                    color = primaryColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Central Calligraphy Verse demo
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "وَمَنْ يَتَّقِ اللَّهَ يَجْعَلْ لَهُ مَخْرَجًا",
                    color = primaryColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = letterSpacing.sp,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        shadow = Shadow(
                            color = primaryColor.copy(0.7f),
                            blurRadius = glowBlur / 4
                        )
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "منصة اكتفاء التعليمية والتربوية الشاملة 🕌",
                    color = TextLight.copy(alpha = 0.75f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Simulated active elements: audio reactive visualizer
            if (audioAnimOn) {
                Row(
                    modifier = Modifier.height(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    val bars = 10
                    for (i in 0 until bars) {
                        // Math-based simulated heights to avoid compose re-composition lag
                        val heightMultiplier = sin(shimmerTranslate / 60 + i) * 6 + 10
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(heightMultiplier.dp)
                                .background(primaryColor)
                        )
                    }
                }
            } else {
                Text(
                    "جميع الأفكار فعالة على قاعدة بيانات SQLite بمزامنة مستقرة.",
                    color = TextMuted,
                    fontSize = 8.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Shimmer gradient Overlay
        if (shimmerOn) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                primaryColor.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            start = Offset(shimmerTranslate, 0f),
                            end = Offset(shimmerTranslate + 250f, this.size.height)
                        )
                        drawRect(brush = brush)
                    }
            )
        }
    }
}

// Particle stars system with fully dynamic configuration
@Composable
fun OasisStarsParticles(
    count: Int,
    speedMultiplier: Float,
    starColor: Color,
    drawLines: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "StarAnim")
    
    // Wave oscillation
    val offsetWave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween((12000 / speedMultiplier).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offsetWave"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w == 0f || h == 0f) return@Canvas

        // Draw generated stars programmatically using static lists and dynamic sin modifiers
        val points = listOf(
            Pair(0.12f, 0.18f), Pair(0.85f, 0.22f), Pair(0.42f, 0.38f),
            Pair(0.24f, 0.61f), Pair(0.78f, 0.52f), Pair(0.31f, 0.81f),
            Pair(0.64f, 0.74f), Pair(0.92f, 0.88f), Pair(0.08f, 0.44f),
            Pair(0.51f, 0.15f), Pair(0.71f, 0.34f), Pair(0.19f, 0.78f),
            Pair(0.89f, 0.63f), Pair(0.48f, 0.89f), Pair(0.35f, 0.28f),
            Pair(0.58f, 0.67f), Pair(0.15f, 0.58f), Pair(0.27f, 0.12f),
            Pair(0.74f, 0.88f), Pair(0.95f, 0.41f), Pair(0.05f, 0.85f),
            Pair(0.53f, 0.49f), Pair(0.81f, 0.11f), Pair(0.68f, 0.29f)
        )

        val actualCount = count.coerceAtMost(points.size)
        val renderedPoints = points.take(actualCount).map { (px, py) ->
            // Oscillate the stars positions marginally
            val ox = px * w + sin(offsetWave * 0.05f + (px * 100)) * 5
            val oy = py * h + sin(offsetWave * 0.03f + (py * 100)) * 5
            Offset(ox, oy)
        }

        // Optionally connect nearby star spots to form holographic constellations (Interactive Space Idea)
        if (drawLines && renderedPoints.size > 1) {
            for (i in 0 until renderedPoints.size) {
                for (j in (i + 1) until renderedPoints.size) {
                    val p1 = renderedPoints[i]
                    val p2 = renderedPoints[j]
                    val distSq = (p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y)
                    val limit = (120f * 120f)
                    if (distSq < limit) {
                        val fraction = (1f - (distSq / limit)).coerceIn(0f, 1f)
                        drawLine(
                            color = starColor.copy(alpha = fraction * 0.12f),
                            start = p1,
                            end = p2,
                            strokeWidth = 1f
                        )
                    }
                }
            }
        }

        // Render individual glowing star points
        renderedPoints.forEach { pt ->
            drawCircle(
                color = starColor.copy(alpha = 0.85f),
                radius = 3.5f,
                center = pt
            )
            drawCircle(
                color = starColor.copy(alpha = 0.2f),
                radius = 8.5f,
                center = pt
            )
        }
    }
}

// Data parser delivering 100 actual structured items matching the user's intent
private fun getIdeasForCategory(categoryIndex: Int): List<String> {
    return when (categoryIndex) {
        0 -> listOf(
            "تطابق الألوان الخمسة الفاخرة لتوفير الطاقة وحماية العين.",
            "مزيج ذهبي داكن وأزرق ملكي لخلفيات غامرة سينمائية.",
            "تدرجات شعاعية لطيفة بدلاً من الألوان المسطحة الصلبة.",
            "صياغة هالات ضوئية متوهجة حول البطاقات لتجربة ثلاثية الأبعاد.",
            "طبقة زجاجية بفلتر ضبابي خفيف خلف العناصر (Glassmorphism).",
            "خط ذهبي منقش ومحيط لجميع بطاقات الخدمات الهامة.",
            "تحكم ديناميكي بقيم النطاقات اللونية عبر SQLite لحفظ الخيارات.",
            "مؤشرات الإشعاع الهولوغرافي لترقية رؤية التنبيهات والدروس.",
            "أزرار اختيار الحسابات مغطاة بلمعة ذهبية هادئة ممتدة.",
            "تبديل تلقائي بين ألوان مسبحة التسبيح كل 33 نقرة بالرمز الأخضر.",
            "مستويات عتامة لونية متدرجة تزيد الاستقرار مع الإضاءة الليلية.",
            "تطبيق نظام الألوان الديناميكي الموحد لضمان التناسق البصري الحقيقي.",
            "خلفيات الياقوت الأندلسي الداكن لإثراء الأجواء الكلاسيكية العائلية.",
            "إدراج تباينات لونية تزيد عن 4.5:1 لضمان القراءة المريحة لكبار السن.",
            "تأثير وميض نيون خافت للأزرار النشطة يجذب الانتباه السلوكي اليومي.",
            "دعم انتقالات لونية سلسة أقل من 300 مللي ثانية لمنع وميض الهاتف.",
            "تضمين شيدر معدني ذهبي يلمع مع ميلان الشاشة لمحاكاة الغلاف الورقي.",
            "أجواء رملية دافئة لمساعدة الطفل على المذاكرة الهادئة فجراً.",
            "تقليل انبعاثات الضوء الأزرق تلقائياً بدمج ألوان الأوبسيديان الغامقة.",
            "إطار توهج نبضي مخصص للمصحف والورد القرآني يضفي هيبة بصرية."
        )
        1 -> listOf(
            "تقسيم عناوين دقيقة متباعدة باستخدام خطوط أندلسية.",
            "مزامنة هالة الضوء مع سرعة نبض المذبذب البصري.",
            "ظلال ممتدة للأزرار ثلاثية الأبعاد تقلل الجهد البصري للطفل.",
            "دعم خطوط الحجم المتغيرsp لضمان الوصول الشامل.",
            "رسائل إيحائية ذهبية لرفع المعنويات العائلية بشكل يومي.",
            "خطوط متباعدة تعطي راحة مع صفوف القراءة الممتدة.",
            "تأثير تدرج لوني على النص لجعل الكلمات تظهر مذهبة.",
            "هالات ترحيبية باسم المستخدم تتألق بالذهب عند الدخول.",
            "تأثير دمج الظل الأسود خلف الكلمات البيضاء لتعزيز التباين.",
            "عرض الورد ومكامن الخطأ في الحفظ بألوان ذات تباينات واضحة جداً.",
            "خطوط كلاسيكية مستقيمة ومائلة تحدد حدود البيانات المالية العائلية.",
            "تأثيرات ضبابية حية تنشط بالتحويم أو اللمس الطويل.",
            "تسميات شريط التنقل السفلي والعلوي بخطوط بارزة سريعة القراءة.",
            "تصميم هالة ضوئية ممتدة خلف أيقونة الحساب الإشرافي للوالد.",
            "لوحات توجيهية فرعية ناعمة تخفف الازدحام المعلوماتي.",
            "ألوان متدرجة رقيقة تعزز جودة خطوط القراءة الليلية.",
            "تحديد الرموز الرياضية بخطوط سميكة وواضحة جداً للطفل.",
            "استخدام الرموز التعبيرية الذهبية الموحدة كفهارس بصرية أنيقة.",
            "إبراز الفوائد الطبية ومواعيد جرعات الدواء بخطوط حمراء هادئة وحارة.",
            "دعم الهوامش الموسعة للآيات الكريمة لمحاكاة المصاحف التاريخية الفاخرة."
        )
        2 -> listOf(
            "إطارات مزدوجة ذهبية مستوحاة من الزخارف الهندسية الإسلامية.",
            "أركان بطاقات دائرية فائقة النعومة 32dp لترقية الملمس البصري.",
            "أشرطة تزيين معدنية مذهبة توضع في الزاوية العلوية لكل بطاقة.",
            "أقواس المحاريب التقليدية مرسومة ديناميكياً بخطوط منقطة هادئة.",
            "تقسيم الأقسام بخصائص زوايا متباينة (سymmetrical/Asymmetrical).",
            "حدود رفيعة 1dp تحافظ على انسيابية المظهر ونظافة الواجهات.",
            "بطاقات إحصائيات منسقة بهياكل مستعرضة مميزة غير معقدة.",
            "زوايا حادة وحواف بروتالست متألقة عند الرغبة بالتصاميم العصرية السريعة.",
            "مربعات تحديد المهام المنزلية منقوشة بزخرفة إسلامية تكتمل بالكامل.",
            "أشرطة تقدم مستديرة دائرية متكاملة تضيء تدريجياً لتعكس الورد اليومي.",
            "إدراج الفواصل البصرية الأنيقة لفرز بيانات المشتريات والديون والأسرة.",
            "أطر وبطاقات دائرية تناسب الأبناء والتعليم من السير الذاتية التفاعلية.",
            "تأثير إطارات الحماية المتينة لحساب الوالد المشرف والأم المساعد.",
            "تصميم هيكل خزانة مذهبة تحتفظ بملفات وكتب المقررات الرسمية المحملة.",
            "تظليل الأزرار بحدود متداخلة متباينة تعطي إحساس السطح الفيزيائي.",
            "تصميم إطار مسبحة الكترونية على شكل حلقات ذهبية متعاقبة وجميلة.",
            "تزيين حواف الحاسبة الرياضية لتبدو مثل ألغاز الميكانيك الفاخرة للطفل.",
            "أيقونات محاطة بطبقات تصفية ضوء دافئة لسهولة التعرف السلوكي السريع.",
            "ظلال هابطة ناعمة تجعل البطاقات تبدو عائمة فوق النجوم الخلفية.",
            "استبدال الحواف الحادة بحواف مروحة دائرية لتقليل إجهاد النظر."
        )
        3 -> listOf(
            "شبكة نجوم خلفية ديناميكية تتلألأ بهدوء لإعطاء بعد كوني غامر.",
            "إمكانية دمج النجوم اللمسي وتشكيل الكويكبات عند الضغط لتسلية الطفل الكوني.",
            "تمثيل خطوط ربط ذكية بين نقاط البيانات تحوّل الشبكة إلى مرصد فلكي.",
            "تأثير تذبذبي تدريجي لشدة الإضاءة النجمية يمنع جفاف الأعين فجراً.",
            "تقليل سرعة حركة الجسيمات لتبدو عميقة وبطيئة كالسباحة في الفضاء العريض.",
            "جعل عدد النجوم قابلاً للتخصيص الكامل ليتناسب مع أجهزة الهاتف الضعيفة والقوية.",
            "تطابق حركة لمعان النجوم مع نبض العداد والمسبحة بشكل متناغم صامت.",
            "إبراز رمز اتجاه القبلة بومضات خفيفة تنبعث تدريجياً من حواف الشاشة.",
            "خطوط اتصال وهمية تربط حصص الأبناء بتوقيت الساعة المحلية للمحافظة.",
            "تأثير تساقط حبات الكريستال الذهبية عند نجاح تصفح والتقاط حكمة ويب.",
            "تدرج غباري رملي للنجوم يعبّر عن كفاح رجال الله وسجلات الطاعات اليومية.",
            "تخصيص مؤشر سرعة الدوران للنجوم الدائرية لتناسب رغبة الهدوء أو الحركة.",
            "ارتباط اللمس على النجوم بإشعاع موجي دائري ينتشر عبر سطح الشاشة.",
            "نجوم خلفية داكنة خافتة للغاية لا تتجاوز 10% عتامة للحفاظ على النص.",
            "محاكاة الفاصل الزمني لتعاقب الليل والنهار ديناميكياً بروعة التلاشي البصري.",
            "شبكة اتصال فضية خفيفة خلف حاسبة المشتريات لتوثيق الطابع الهندسي للحسابات.",
            "انعكاس بريق النور على الأزرار عند النقر باللمس لضمان الحيوية البصرية واليقظة.",
            "بنية جزيئات حركية مستقلة تشبه السحب السديمية خلف المصحف والورد.",
            "مؤشرات دائرية صغيرة مضيئة تحيط بالحافظة لتسهيل مشاركة الأفكار اليومية.",
            "تأثير وهج النجم القطبي يشير دوماً إلى المساعدة والتعلم الذكي للطفل."
        )
        else -> listOf(
            "تطبيقات الشمر الحركي اللامع (Golden Shimmer) لملء المحتوى والبطاقات.",
            "انتقالات ثلاثية الأبعاد مرتدة (Spring Physics) تعطي حيوية لكل نقرة بالأصبع.",
            "تأثير تقليص وتمدد البطاقات عند الضغط لتبدو وكأنها تتنفس حركياً.",
            "موجات مذبذبة بصرية صوتية حية ترمز للتعلم وسرعة الاستجابة التربوية.",
            "تحريك فتح وإغلاق النوافذ بطريقة الصعود التدريجي السلس (Slide Slide Up).",
            "تحولات بطاقة الكتاب ومقرر الدراسة بحركة تقليب ورقية افتراضية سريعة.",
            "اهتزازات حركية فائقة الاستقرار والجمال للأيقونات عند الممارسات والاجتماعات.",
            "بريق مائي ومضات خلف حبات السبحة لتعويض الضغط الفيزيائي بمجال ذكي.",
            "مرونة انتقال كاملة بين تبويبات الخدمات والصحة والصيانة بشكل سلس ممتد.",
            "حركة مؤشر تقدم الورد القرآني بسلاسة انزلاقية لاتوحي بالانقطاع.",
            "توهج هالة زرقاء باردة تلتف بشكل دائري لإبراز فواتير المشتريات المدفوعة.",
            "تلاشي شريط التنبيهات والأخطاء بالقرآن بهدوء لعدم إرباك ذهن الحافظ.",
            "تأثير الارتداد العكسي الخفيف Bounce عند الوصول إلى نهاية قوائم المصروفات والديون.",
            "إظهار بطاقات الأبناء بحركة انزلاق متدرجة متراصة تعطي شعوراً بالتنظيم الراقي.",
            "إدراج وميض مذهب خفيف يظهر على الحافة بشكل دوري لتذكير الطفل بالصلاة.",
            "حركة موجية لطيفة على شريط تمرير القيمة لتقييم المهارات واكتساب العادات.",
            "استعادة الحسابات وقفل رمز PIN بحركة رج خفيفة عند إدخال الرمز غير الصحيح.",
            "تدرج انزلاق الأزرار السفلية لتبسيط تجربة التصفح الفوري بالتطبيقات المباشرة.",
            "محاكاة هبوط قطرات المطر اللامعة خلف شاشة الطقس والعملات اليمنية المحلية.",
            "تأثير الاحتفال البصري بإشراق النور والنجوم المتوهجة عند إكمال الطاعات الدينية."
        )
    }
}

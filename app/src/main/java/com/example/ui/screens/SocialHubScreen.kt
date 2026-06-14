package com.example.ui.screens

import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MidnightBlue
import com.example.ui.theme.WarmGold

fun launchPlatformExternally(context: android.content.Context, platformName: String, fallbackUrl: String) {
    val packageMap = mapOf(
        "واتساب ويب" to "com.whatsapp",
        "تيليجرام" to "org.telegram.messenger",
        "فيسبوك" to "com.facebook.katana",
        "إكس (تويتر)" to "com.twitter.android",
        "ديب سيك" to "com.deepseek.chat"
    )
    val packageName = packageMap[platformName]
    var intent: android.content.Intent? = null
    
    if (packageName != null) {
        intent = context.packageManager.getLaunchIntentForPackage(packageName)
    }
    
    if (intent == null) {
        val url = if (platformName == "واتساب ويب") "https://api.whatsapp.com" else fallbackUrl
        intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "جاري توجيه الرابط لمتصفح الهاتف قياسياً...", android.widget.Toast.LENGTH_SHORT).show()
        try {
            val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(fallbackUrl)).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        } catch (ex: Exception) {
            android.widget.Toast.makeText(context, "عذراً، لم نجد تطبيق يدعم التصفح حالياً", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

data class SocialPlatform(
    val name: String,
    val url: String,
    val icon: ImageVector,
    val color: Color,
    val useMobileUserAgent: Boolean = true
)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SocialHubScreen(
    onBack: () -> Unit,
    onSaveToClipboard: (String) -> Unit
) {
    val context = LocalContext.current

    // Set up global web cookies acceptability safely without causing NullPointerException
    val cookieManager = remember { CookieManager.getInstance() }
    LaunchedEffect(Unit) {
        try {
            cookieManager.setAcceptCookie(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val platforms = listOf(
        SocialPlatform("واتساب ويب", "https://web.whatsapp.com", Icons.Default.Chat, Color(0xFF25D366), useMobileUserAgent = false), // Desktop agent needed for web WA on mobile
        SocialPlatform("تيليجرام", "https://web.telegram.org", Icons.Default.Send, Color(0xFF0088CC), useMobileUserAgent = false),
        SocialPlatform("فيسبوك", "https://m.facebook.com", Icons.Default.Facebook, Color(0xFF1877F2)),
        SocialPlatform("إكس (تويتر)", "https://m.twitter.com", Icons.Default.Tag, Color(0xFF1A1A1A)),
        SocialPlatform("جيمني AI", "https://gemini.google.com", Icons.Default.SmartToy, Color(0xFF4285F4)),
        SocialPlatform("ديب سيك", "https://chat.deepseek.com", Icons.Default.Memory, Color(0xFF005AF0)),
        SocialPlatform("قوقل", "https://www.google.com", Icons.Default.Search, Color(0xFFEA4335)),
        SocialPlatform("نوت بوك", "https://notebooklm.google.com", Icons.Default.MenuBook, Color(0xFF34A853))
    )

    var selectedPlatform by remember { mutableStateOf<SocialPlatform?>(null) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var progressVal by remember { mutableFloatStateOf(0f) }
    var isLoadingPage by remember { mutableStateOf(false) }
    var launchExternally by remember { mutableStateOf(true) } // Enable launch through native apps by default for superior VPN speed and UX

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (selectedPlatform == null) {
            // Main choice screen
            TopAppBar(
                title = { Text("وسائل التواصل والذكاء الاصطناعي", fontWeight = FontWeight.Bold, color = WarmGold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WarmGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )

            // Dynamic header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(WarmGold.copy(alpha = 0.15f), Color.Transparent)))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        "تصفح بخصوصية وجلسة مدمجة",
                        color = WarmGold,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "يتم حفظ جلسات تسجيل الدخول بشكل مستمر لتسهيل دخولك اليومي السريع.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            // VPN optimization Toggle row - High craft
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { launchExternally = !launchExternally }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Launch,
                        contentDescription = null,
                        tint = WarmGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "فتح مباشر بالتطبيقات لتخطي حظر الـ VPN ⚡",
                            color = WarmGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            if (launchExternally) "الوضع السريع: سيقوم النظام بفتح التطبيقات المثبتة بهاتفك مباشرة."
                            else "الوضع العادي: فتح المواقع في متصفح التطبيق المدمج.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = launchExternally,
                        onCheckedChange = { launchExternally = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = WarmGold,
                            checkedTrackColor = WarmGold.copy(0.3f)
                        )
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(platforms) { platform ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clickable {
                                if (launchExternally) {
                                    launchPlatformExternally(context, platform.name, platform.url)
                                } else {
                                    selectedPlatform = platform
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.3f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(platform.color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(platform.icon, null, tint = platform.color, modifier = Modifier.size(26.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                platform.name,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            val platform = selectedPlatform!!

            // Topbar inside selected browser session
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedPlatform = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Browser", tint = WarmGold)
                }

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(platform.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(platform.icon, null, tint = platform.color, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        platform.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                IconButton(onClick = { showSearchBar = !showSearchBar }) {
                    Icon(Icons.Default.Search, null, tint = WarmGold)
                }
                IconButton(onClick = { webViewRef?.reload() }) {
                    Icon(Icons.Default.Refresh, null, tint = WarmGold)
                }
            }

            // Search inputs inside the platform browser
            AnimatedVisibility(
                visible = showSearchBar,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث أو إدخال عنوان URL...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmGold,
                        unfocusedBorderColor = WarmGold.copy(0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = {
                            if (searchQuery.isNotBlank()) {
                                val urlToLoad = when {
                                    searchQuery.startsWith("http://") || searchQuery.startsWith("https://") -> searchQuery
                                    platform.name == "قوقل" -> "https://www.google.com/search?q=$searchQuery"
                                    else -> "https://www.google.com/search?q=site:${platform.url.replace("https://", "")} $searchQuery"
                                }
                                webViewRef?.loadUrl(urlToLoad)
                                showSearchBar = false
                            }
                        }) {
                            Icon(Icons.Default.Search, null, tint = WarmGold)
                        }
                    },
                    singleLine = true
                )
            }

            // Progress loader indicator
            if (isLoadingPage) {
                LinearProgressIndicator(
                    progress = { progressVal },
                    modifier = Modifier.fillMaxWidth(),
                    color = WarmGold
                )
            }

            // High compatibility WebView
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            try {
                                cookieManager.setAcceptThirdPartyCookies(this, true)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                allowFileAccess = true
                                databaseEnabled = true
                                setSupportMultipleWindows(false)
                                javaScriptCanOpenWindowsAutomatically = false
                                useWideViewPort = true
                                loadWithOverviewMode = true

                                // Emulate Desktop agent for specific platforms like WhatsApp web which drops mobile clients
                                userAgentString = if (platform.useMobileUserAgent) {
                                    "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"
                                } else {
                                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/491.76 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/491.76"
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoadingPage = true
                                    progressVal = 0.1f
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoadingPage = false
                                }

                                @Deprecated("Deprecated in Java")
                                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                    if (url == null) return false
                                    if (url.startsWith("http://") || url.startsWith("https://")) {
                                        // Standard web links load directly inside our high-speed WebView
                                        return false
                                    } else {
                                        // Protocols like tg://, whatsapp://, intent://
                                        try {
                                            val intent = android.content.Intent.parseUri(url, android.content.Intent.URI_INTENT_SCHEME)
                                            if (intent != null) {
                                                view?.context?.startActivity(intent)
                                            }
                                        } catch (e: Exception) {
                                            try {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                                view?.context?.startActivity(intent)
                                            } catch (ex: Exception) {
                                                android.widget.Toast.makeText(view?.context, "لا يوجد تطبيق يدعم هذا الاختصار في الهاتف حالياً", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        return true
                                    }
                                }
                            }

                            webChromeClient = object : android.webkit.WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    progressVal = newProgress / 100f
                                    if (newProgress == 100) {
                                        isLoadingPage = false
                                    }
                                }
                            }

                            loadUrl(platform.url)
                            webViewRef = this
                        }
                    },
                    update = {
                        // Keep reference updated
                        webViewRef = it
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Unified browser navigation bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (webViewRef?.canGoBack() == true) {
                        webViewRef?.goBack()
                    } else {
                        Toast.makeText(context, "لا توجد صفحات سابقة", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(Icons.Default.NavigateBefore, contentDescription = "Back page", tint = WarmGold)
                }

                IconButton(onClick = {
                    if (webViewRef?.canGoForward() == true) {
                        webViewRef?.goForward()
                    } else {
                        Toast.makeText(context, "لا توجد صفحات لاحقة", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(Icons.Default.NavigateNext, contentDescription = "Forward page", tint = WarmGold)
                }

                // Smart extraction feature: Grab selected or current page title/URL and save to Clipboard
                Button(
                    onClick = {
                        val currentUrl = webViewRef?.url
                        val currentTitle = webViewRef?.title
                        if (!currentUrl.isNullOrEmpty()) {
                            val msg = "${currentTitle ?: "مقتطف موقع"} : $currentUrl"
                            onSaveToClipboard(msg)
                            Toast.makeText(context, "تم حفظ رابط وجهتك في الحافظة الذكية!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MidnightBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ الصفحة للحافظة", color = MidnightBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

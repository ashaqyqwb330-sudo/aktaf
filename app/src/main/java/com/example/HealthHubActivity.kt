package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.util.*

data class HealthServiceCard(
    val icon: ImageVector,
    val title: String,
    val color: Color,
    val activity: Class<*>? = null
)

class HealthHubActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                HealthHubScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthHubScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour in 5..11 -> "صباح الخير"
        hour in 12..17 -> "مساء الخير"
        else -> "مساء النور"
    }
    
    // Read cached username from app_prefs
    val userName = remember {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("user_name", "العائلة الكريمة") ?: "العائلة الكريمة"
    }

    val services = listOf(
        HealthServiceCard(Icons.Default.People, "المرضى", WarmGold, PatientsListActivity::class.java),
        HealthServiceCard(Icons.Default.Calculate, "حاسبة الجرعات", WarmGold, DosageCalculatorActivity::class.java),
        HealthServiceCard(Icons.Default.MonitorHeart, "العلامات الحيوية", WarmGold, VitalSignsActivity::class.java),
        HealthServiceCard(Icons.Default.Restaurant, "التغذية والماء", WarmGold, NutritionActivity::class.java),
        HealthServiceCard(Icons.Default.SmartToy, "الذكاء الطبي", WarmGold, AiMedicalActivity::class.java),
        HealthServiceCard(Icons.Default.Healing, "الإسعافات الطارئة", WarmGold, FirstAidActivity::class.java),
        HealthServiceCard(Icons.Default.LibraryBooks, "المكتبة الطبية", WarmGold, MedicalLibraryActivity::class.java),
        HealthServiceCard(Icons.Default.Description, "التقارير", WarmGold, HealthReportsActivity::class.java),
        HealthServiceCard(Icons.Default.Warning, "قسم الطوارئ", RedAccent, EmergencyActivity::class.java)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "🏥 البوابة الصحية لأسرتنا",
                        color = WarmGold,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            shadow = Shadow(color = WarmGold.copy(alpha = 0.3f), blurRadius = 8f)
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = WarmGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightBlue
                )
            )
        },
        containerColor = MidnightBlue
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MidnightBlue)
                .padding(horizontal = 16.dp)
        ) {
            // Header Welcome Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(listOf(WarmGold.copy(alpha = 0.08f), Color.Transparent))
                        )
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❤️", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "$greeting، $userName",
                            color = TextLight,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "مرحباً بك في البوابة الصحية لـ اكتفاء. تتبع العلامات، حدد الجرعات، وتلقى تنبيهات دوائية صوتية فورية لصحة أفضل.",
                            color = SilverGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Quick Emergency Panic Trigger
            Button(
                onClick = {
                    val intent = Intent(context, EmergencyActivity::class.java)
                    intent.putExtra("TRIGGER_PANIC", true)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.Emergency, null, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("🚨 نداء طوارئ عاجل وصامت", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Text(
                "الخدمات الطبية المتكاملة (9 أقسام):",
                color = WarmGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
            )

            // Services Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(services) { service ->
                    Card(
                        modifier = Modifier
                            .aspectRatio(0.9f)
                            .clickable {
                                service.activity?.let {
                                    context.startActivity(Intent(context, it))
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(service.color.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(service.icon, null, tint = service.color, modifier = Modifier.size(26.dp))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                service.title,
                                color = TextLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

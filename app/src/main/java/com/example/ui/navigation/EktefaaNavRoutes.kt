package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 🗺️ مسارات التنقل الرئيسية وطرق توجيه شاشات تطبيق "اكتفاء"
 */
sealed class ScreenRoute(val route: String, val title: String, val icon: ImageVector) {
    object Splash : ScreenRoute("SPLASH", "شاشة الترحيب والافتتاحية", Icons.Default.FlashOn)
    object Login : ScreenRoute("LOGIN", "بوابة الاكتفاء الأمنية", Icons.Default.Lock)
    object Dashboard : ScreenRoute("DASHBOARD", "اللوحة الرئيسية للخدمات", Icons.Default.GridView)
    
    // 🏫 الأقسام التعليمية ومتابعة الأبناء
    object ChildrenList : ScreenRoute("CHILDREN_LIST", "بوابة الأبناء والتلاميذ", Icons.Default.People)
    object ChildDashboard : ScreenRoute("CHILD_DASHBOARD", "لوحة تحكم الابن", Icons.Default.Dashboard)
    object ChildGrades : ScreenRoute("CHILD_GRADES", "رصد الدرجات والتحصيل", Icons.Default.Grade)
    object ChildBehavior : ScreenRoute("CHILD_BEHAVIOR", "السلوك والتقييمات", Icons.Default.Star)
    object ChildAttendance : ScreenRoute("CHILD_ATTENDANCE", "الحضور والانضباط اليومي", Icons.Default.CalendarToday)
    object ChildReports : ScreenRoute("CHILD_REPORTS", "التقارير الدراسية الشاملة", Icons.Default.Analytics)

    // 🕌 الأقسام الروحية والثقافية والترفيهية
    object QuranHub : ScreenRoute("QURAN_HUB", "الحافظة الذكية للقرآن الكريم", Icons.Default.Book)
    object PrayerManager : ScreenRoute("PRAYER_MANAGER", "جدول الصلوات والالتزام", Icons.Default.Timer)
    object MenOfGod : ScreenRoute("MEN_OF_GOD", "منبر رجال الله والمسيرة", Icons.Default.WorkspacePremium)
    object CulturalLibrary : ScreenRoute("CULTURAL_LIBRARY", "المكتبة التعليمية التفاعلية", Icons.Default.MenuBook)
    object LeisureHub : ScreenRoute("LEISURE_HUB", "استراحة ومتنفس العائلة", Icons.Default.Spa)

    // 🛠️ الأدوات المساعدة وخدمات الدعم الذاتي
    object Calculator : ScreenRoute("CALCULATOR", "الآلة الحاسبة الذكية", Icons.Default.Calculate)
    object MedicalHub : ScreenRoute("MEDICAL_HUB", "بوابة الرعاية والملف الطبي", Icons.Default.LocalHospital)
    object FinancialManager : ScreenRoute("FINANCIAL_MANAGER", "إدارة الشؤون المالية والنفقة", Icons.Default.AttachMoney)
    object ClipboardManager : ScreenRoute("CLIPBOARD_MANAGER", "حافظة الخواطر والدروس والمفكرة", Icons.Default.ContentCopy)
    object Simulation : ScreenRoute("SIMULATION", "محاكاة المواقف الصعبة والأزمات", Icons.Default.Psychology)
    object Settings : ScreenRoute("SETTINGS", "إعدادات التطبيق والمظهر", Icons.Default.Settings)
}

/**
 * 📦 علاقات التنقل والمجموعات الهيكلية للتطبيق
 */
object EktefaaNavigationConfig {
    val bottomNavItems = listOf(
        ScreenRoute.Dashboard,
        ScreenRoute.ChildrenList,
        ScreenRoute.QuranHub,
        ScreenRoute.Settings
    )

    val coreServicesNetwork = listOf(
        ScreenRoute.ChildrenList,
        ScreenRoute.QuranHub,
        ScreenRoute.CulturalLibrary,
        ScreenRoute.ClipboardManager,
        ScreenRoute.MedicalHub,
        ScreenRoute.FinancialManager,
        ScreenRoute.Simulation,
        ScreenRoute.MenOfGod,
        ScreenRoute.LeisureHub,
        ScreenRoute.Calculator,
        ScreenRoute.PrayerManager,
        ScreenRoute.Settings
    )
}

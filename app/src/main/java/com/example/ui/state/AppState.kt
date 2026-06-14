package com.example.ui.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 🔒 نموذج لبيانات مستخدم الجلسة النشط
 */
data class UserSession(
    val name: String,
    val role: String,
    val token: String = "",
    val isLoggedIn: Boolean = false
)

/**
 * 📊 نظام إدارة الحالة (State Management) المركزي لتطبيق "اكتفاء"
 */
object EktefaaStateManager {

    // 👤 حالة جلسة تسجيل الدخول النشطة للمستخدم
    private val _currentUserSession = MutableStateFlow(UserSession("", "Guest", isLoggedIn = false))
    val currentUserSession: StateFlow<UserSession> = _currentUserSession.asStateFlow()

    // 🌗 خيار المظهر والسمة البصرية (داكن / فاتح)
    private val _isDarkModeEnabled = MutableStateFlow(true)
    val isDarkModeEnabled: StateFlow<Boolean> = _isDarkModeEnabled.asStateFlow()

    // 📡 حالة الاتصال بالشبكة أو وضع الحظر (Offline Mode Indicator)
    private val _isOfflineModeActive = MutableStateFlow(false)
    val isOfflineModeActive: StateFlow<Boolean> = _isOfflineModeActive.asStateFlow()

    // 🛎️ مستودع للإشعارات والتنبيهات المباشرة الواردة
    private val _notificationCount = MutableStateFlow(0)
    val notificationCount: StateFlow<Int> = _notificationCount.asStateFlow()

    /**
     * 🚪 تسجيل دخول مستخدم جديد وتحديث الجلسة
     */
    fun loginUser(name: String, role: String) {
        _currentUserSession.value = UserSession(name = name, role = role, isLoggedIn = true)
    }

    /**
     * 🚪 تسجيل خروج تصفية بيانات الجلسة
     */
    fun logout() {
        _currentUserSession.value = UserSession("", "Guest", isLoggedIn = false)
    }

    /**
     * 🌗 تبديل سمة المظهر واللون العام
     */
    fun toggleTheme() {
        _isDarkModeEnabled.value = !_isDarkModeEnabled.value
    }

    /**
     * 📡 تعيين حالة الاتصال المحلي
     */
    fun setOfflineMode(isActive: Boolean) {
        _isOfflineModeActive.value = isActive
    }

    /**
     * 🛎️ زيادة عدد الإشعارات النشطة
     */
    fun incrementNotifications() {
        _notificationCount.value += 1
    }

    /**
     * 🛎️ مسح التنبيهات
     */
    fun clearNotifications() {
        _notificationCount.value = 0
    }
}

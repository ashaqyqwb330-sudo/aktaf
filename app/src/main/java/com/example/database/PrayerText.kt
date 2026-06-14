package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// النصوص الكاملة للأدعية
@Entity(tableName = "prayer_texts")
data class PrayerText(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,              // عنوان الدعاء
    val type: String,               // day_prayer, special, mujahideen, glorification
    val dayOfWeek: Int? = null,     // 1=الأحد ... 7=السبت (إن كان دعاء يوم)
    val source: String = "",        // المصدر (إمام زين العابدين، قرآن...)
    val fullText: String,           // النص الكامل
    val contemplationQuestions: String = "", // أسئلة التأمل (مفصولة بـ |)
    val audioUrl: String = "",      // رابط صوتي (إن وجد)
    val notificationTime: String = "" // وقت الإشعار (صباحاً، فجراً...)
)

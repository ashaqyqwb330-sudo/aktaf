package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// تتبع قراءة الأدعية اليومية
@Entity(tableName = "daily_prayer_tracking")
data class DailyPrayerTracking(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val date: String,               // yyyy-MM-dd
    val prayerType: String,         // day_prayer, special_prayer, mujahideen_prayer
    val prayerTitle: String,        // اسم الدعاء
    val isRead: Boolean = false,
    val isContemplated: Boolean = false,  // هل تأمل فيه؟
    val answeredPrayerNote: String = "",  // سجل استجابة الدعاء
    val timestamp: Long = System.currentTimeMillis()
)

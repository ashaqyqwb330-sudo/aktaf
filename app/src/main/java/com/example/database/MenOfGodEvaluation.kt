package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// تقييمات برنامج رجال الله
@Entity(tableName = "men_of_god_evaluation")
data class MenOfGodEvaluation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val date: String,           // yyyy-MM-dd
    // الأسئلة الأساسية
    val prayerScore: Int = 0,       // 0-5
    val quranScore: Int = 0,
    val athkarScore: Int = 0,
    // الوعي الإيماني
    val faithAwareness: Int = 0,
    // الوعي الإعلامي
    val mediaAwareness: Int = 0,
    val avoidTrivial: Boolean = true,
    val meaningfulInteraction: Boolean = true,
    // النعمة والمسؤولية
    val gratitudeAwareness: Int = 0,
    val creativityEffort: Boolean = false,
    // الدعاء للمجاهدين
    val prayedForMujahideen: Boolean = false,
    // الإخلاص وترك المنّ
    val sincerityInGiving: Int = 0,
    // الجهاد في سبيل الله
    val jihadSpirit: Int = 0,
    // معرفة الله بأسمائه
    val knowingAllahNames: Int = 0,
    val notes: String = ""
)

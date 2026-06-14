package com.example.database

import androidx.room.*
import android.content.Context
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val role: String, // "Father", "Mother", "Child"
    val pinCode: String
)

@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,            // الاسم الثلاثي واللقب
    val age: Int,                    // العمر
    val diagnosis: String,           // التشخيص المرضي
    val treatingDoctor: String,      // الطبيب المعالج
    val currentComplaint: String,    // الشكوى الحالية
    val medicationsJson: String,     // قائمة الأدوية المخزنة كـ JSON (تتسع حتى 20 دواء)
    val doctorInstructions: String,  // إرشادات الطبيب الخاصة
    val followUpDate: String,        // موعد العودة للطبيب
    val chronicDisease: String = "",
    val allergies: String = "",
    val bloodType: String = "O+"
)

@Entity(tableName = "lecture_details")
data class LectureDetailEntity(
    @PrimaryKey val id: Int,
    val parentId: Int,
    val title: String,
    val details: String?,
    val idlec: Int,
    val level: Int,
    val value: String
)

@Entity(tableName = "safha_sora_ayahs")
data class SafhaSoraAyahEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val safhaNo: Int,
    val soraId: Int,
    val startAyah: Int,
    val endAyah: Int,
    val content: String,
    val reference: Int
)

@Entity(tableName = "children")
data class Child(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val age: Int,
    val grade: String,
    val notes: String = "",
    val className: String = "",      // الفصل (أ، ب، ج...)
    val studentId: String = ""       // رقم الجلوس أو الهوية
)

@Entity(tableName = "attendances")
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val childId: Int,
    val status: String,              // "حاضر", "غائب", "متأخر", "بعذر"
    val dateStr: String,             // "YYYY-MM-DD"
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "behavior_logs")
data class BehaviorLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val childId: Int,
    val type: String,                // "إيجابي" or "سلبي"
    val title: String,
    val desc: String = "",
    val dateStr: String,             // "YYYY-MM-DD"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "assessments")
data class Assessment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val childId: Int,
    val subject: String,
    val type: String,                // "اختبار", "واجب", "نشاط", "مشروع", "مشاركة"
    val score: Double,
    val maxScore: Double,
    val notes: String = "",
    val dateStr: String,             // "YYYY-MM-DD"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "clipboard_items")
data class ClipItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val category: String, // "خطب", "أدعية", "فقه", "نصائح", "وصفات", "تربية", "عام"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "prayer_logs")
data class PrayerLog(
    @PrimaryKey val dateStr: String, // "YYYY-MM-DD"
    val fajr: Boolean = false,
    val dhuhr: Boolean = false,
    val asr: Boolean = false,
    val maghrib: Boolean = false,
    val isha: Boolean = false,
    val qiyam: Boolean = false,
    val azkar: Boolean = false
)

@Entity(tableName = "expense_logs")
data class ExpenseLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "EXPENSE", "INCOME"
    val amount: Double,
    val category: String,
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "debt_items")
data class DebtItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shopName: String,
    val desc: String,
    val amount: Double,
    val paid: Boolean = false,
    val dueDate: String
)

@Entity(tableName = "todo_items")
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val priority: String, // "High", "Medium", "Low"
    val isCompleted: Boolean = false
)

@Entity(tableName = "lesson_feedbacks")
data class LessonFeedback(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val childId: Int,
    val lessonName: String,
    val score: Int, // 1-10
    val difficulties: String,
    val positiveNotes: String,
    val dateStr: String // "YYYY-MM-DD"
)

@Entity(tableName = "house_appliances")
data class HouseAppliance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val serviceDate: String,
    val cycleDays: Int,
    val isCompleted: Boolean = false
)

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Long = 0,
    val patientName: String = "",
    val name: String,
    val scheduleTime: String,
    val dosage: String,
    val isTaken: Boolean = false,
    val prescribedBy: String = "",
    val method: String = "",
    val warnings: String = "",
    val recommendations: String = "",
    val notes: String = ""
)

@Entity(tableName = "quran_logs")
data class QuranLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val surah: String,
    val verses: String,
    val completed: Boolean = false,
    val mistakes: Int = 0
)

@Entity(tableName = "child_quran_wirds")
data class ChildQuranWird(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val childId: Int,
    val surahId: Int,
    val surahName: String,
    val startVerse: Int,
    val endVerse: Int,
    val currentVerse: Int,
    val isCompleted: Boolean = false,
    val assignedDate: String
)

@Entity(tableName = "child_quran_mistakes")
data class ChildQuranMistake(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val childId: Int,
    val surahId: Int,
    val surahName: String,
    val verseNumber: Int,
    val verseText: String,
    val mistakeWord: String,
    val isCorrected: Boolean = false,
    val dateStr: String
)

@Dao
interface EktefaaDao {
    // Users
    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<User>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUser(id: Int)

    // Children
    @Query("SELECT * FROM children")
    fun getAllChildrenFlow(): Flow<List<Child>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChild(child: Child)

    @Query("DELETE FROM children WHERE id = :id")
    suspend fun deleteChild(id: Int)

    // ClipItems
    @Query("SELECT * FROM clipboard_items ORDER BY timestamp DESC")
    fun getAllClipboardFlow(): Flow<List<ClipItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClipItem(item: ClipItem)

    @Query("DELETE FROM clipboard_items WHERE id = :id")
    suspend fun deleteClipItem(id: Int)

    // Prayers
    @Query("SELECT * FROM prayer_logs")
    fun getAllPrayerLogsFlow(): Flow<List<PrayerLog>>

    @Query("SELECT * FROM prayer_logs WHERE dateStr = :dateStr")
    suspend fun getPrayerLogByDate(dateStr: String): PrayerLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerLog(log: PrayerLog)

    // Financial
    @Query("SELECT * FROM expense_logs ORDER BY timestamp DESC")
    fun getAllExpenseLogsFlow(): Flow<List<ExpenseLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(log: ExpenseLog)

    @Query("DELETE FROM expense_logs WHERE id = :id")
    suspend fun deleteExpense(id: Int)

    // Debts
    @Query("SELECT * FROM debt_items ORDER BY id DESC")
    fun getAllDebtsFlow(): Flow<List<DebtItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtItem)

    @Query("DELETE FROM debt_items WHERE id = :id")
    suspend fun deleteDebt(id: Int)

    // Todos
    @Query("SELECT * FROM todo_items ORDER BY id DESC")
    fun getAllTodosFlow(): Flow<List<TodoItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoItem)

    @Query("DELETE FROM todo_items WHERE id = :id")
    suspend fun deleteTodo(id: Int)

    // Lessons
    @Query("SELECT * FROM lesson_feedbacks WHERE childId = :childId ORDER BY id DESC")
    fun getLessonsForChildFlow(childId: Int): Flow<List<LessonFeedback>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessonFeedback(feedback: LessonFeedback)

    // House Appliances
    @Query("SELECT * FROM house_appliances")
    fun getAllAppliancesFlow(): Flow<List<HouseAppliance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppliance(appliance: HouseAppliance)

    @Query("DELETE FROM house_appliances WHERE id = :id")
    suspend fun deleteAppliance(id: Int)

    // Medication
    @Query("SELECT * FROM medications")
    fun getAllMedicationsFlow(): Flow<List<Medication>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(med: Medication)

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteMedication(id: Int)

    // Quran
    @Query("SELECT * FROM quran_logs ORDER BY id DESC")
    fun getAllQuranLogsFlow(): Flow<List<QuranLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuranLog(log: QuranLog)

    @Query("DELETE FROM quran_logs WHERE id = :id")
    suspend fun deleteQuranLog(id: Int)

    // Child Quran Wirds
    @Query("SELECT * FROM child_quran_wirds ORDER BY id DESC")
    fun getAllChildQuranWirdsFlow(): Flow<List<ChildQuranWird>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChildQuranWird(wird: ChildQuranWird)

    @Query("DELETE FROM child_quran_wirds WHERE id = :id")
    suspend fun deleteChildQuranWird(id: Int)

    // Child Quran Mistakes
    @Query("SELECT * FROM child_quran_mistakes ORDER BY id DESC")
    fun getAllChildQuranMistakesFlow(): Flow<List<ChildQuranMistake>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChildQuranMistake(mistake: ChildQuranMistake)

    @Query("DELETE FROM child_quran_mistakes WHERE id = :id")
    suspend fun deleteChildQuranMistake(id: Int)
}

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients ORDER BY id DESC")
    fun getAllPatientsFlow(): Flow<List<Patient>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: Patient)

    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun getPatientById(id: Int): Patient?

    @Query("DELETE FROM patients WHERE id = :id")
    suspend fun deletePatient(id: Int)
}

@Dao
interface LectureDetailDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLectures(lectures: List<LectureDetailEntity>)

    @Query("SELECT COUNT(*) FROM lecture_details")
    suspend fun count(): Int

    @Query("SELECT * FROM lecture_details ORDER BY id ASC")
    suspend fun getAllLectures(): List<LectureDetailEntity>
    
    @Query("SELECT * FROM lecture_details WHERE value LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%'")
    suspend fun searchLectures(query: String): List<LectureDetailEntity>
}

@Dao
interface SafhaSoraAyahDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<SafhaSoraAyahEntity>)

    @Query("SELECT COUNT(*) FROM safha_sora_ayahs")
    suspend fun count(): Int

    @Query("SELECT * FROM safha_sora_ayahs ORDER BY id ASC")
    suspend fun getAllItems(): List<SafhaSoraAyahEntity>
    
    @Query("SELECT * FROM safha_sora_ayahs WHERE content LIKE '%' || :query || '%'")
    suspend fun searchItems(query: String): List<SafhaSoraAyahEntity>
}

@Entity(tableName = "school_books")
data class SchoolBookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,              // اسم الكتاب كما في الفهرس
    val grade: Int,                 // الصف (1-12)
    val subject: String,            // المادة
    val part: Int = 1,              // الجزء (1 أو 2 أو 0 للكتب غير المجزأة)
    val year: Int = 0,              // سنة النشر (2017, 2021, 2022, 2023)
    val telegramUrl: String = "",   // رابط تيليجرام
    val localPath: String = "",     // المسار المحلي بعد التحميل
    val coverPath: String = "",     // مسار غلاف الكتاب المحلي لتسهيل استبداله
    val fileSize: Long = 0,
    val isDownloaded: Boolean = false,
    val downloadDate: Long = 0,
    val source: String = "telegram"
)

@Dao
interface SchoolBookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: SchoolBookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<SchoolBookEntity>)

    @Query("SELECT COUNT(*) FROM school_books")
    suspend fun getBooksCount(): Int

    @Query("SELECT * FROM school_books")
    suspend fun getAllBooksList(): List<SchoolBookEntity>

    @Query("SELECT * FROM school_books ORDER BY grade ASC, subject ASC, part ASC")
    fun getAllBooks(): Flow<List<SchoolBookEntity>>

    @Query("SELECT * FROM school_books WHERE grade = :grade ORDER BY subject ASC, part ASC")
    fun getBooksByGrade(grade: Int): Flow<List<SchoolBookEntity>>

    @Query("SELECT * FROM school_books WHERE subject LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%'")
    fun searchBooks(query: String): Flow<List<SchoolBookEntity>>

    @Query("SELECT * FROM school_books WHERE isDownloaded = 1")
    fun getDownloadedBooks(): Flow<List<SchoolBookEntity>>

    @Query("UPDATE school_books SET localPath = :path, isDownloaded = 1, downloadDate = :date WHERE id = :id")
    suspend fun markAsDownloaded(id: Long, path: String, date: Long = System.currentTimeMillis())

    @Query("DELETE FROM school_books WHERE id = :id")
    suspend fun deleteBook(id: Long)

    @Query("DELETE FROM school_books")
    suspend fun deleteAllBooks()

    @Query("SELECT DISTINCT grade FROM school_books ORDER BY grade ASC")
    suspend fun getAllGrades(): List<Int>

    @Query("SELECT DISTINCT subject FROM school_books WHERE grade = :grade ORDER BY subject ASC")
    suspend fun getSubjectsByGrade(grade: Int): List<String>
}

@Database(
    entities = [
        User::class, Child::class, ClipItem::class, PrayerLog::class,
        ExpenseLog::class, DebtItem::class, TodoItem::class,
        LessonFeedback::class, HouseAppliance::class, Medication::class, QuranLog::class,
        SchoolBookEntity::class, ChildQuranWird::class, ChildQuranMistake::class,
        LibraryItemEntity::class,
        QuizEntity::class, QuestionEntity::class, FamilyScoreEntity::class,
        DailyWisdomEntity::class, SearchHistoryEntity::class,
        RajolAllahAssessmentEntity::class, HeartCheckEntity::class,
        WeeklyChallengeEntity::class, ScenarioResultEntity::class,
        MenOfGodEvaluation::class, DailyPrayerTracking::class, PrayerText::class,
        Patient::class, LectureDetailEntity::class, SafhaSoraAyahEntity::class,
        CulturalBook::class, CulturalArticle::class, CulturalGem::class, ReadingLog::class
    ],
    version = 11,
    exportSchema = false
)
abstract class EktefaaDatabase : RoomDatabase() {
    abstract fun dao(): EktefaaDao
    abstract fun schoolBookDao(): SchoolBookDao
    abstract fun libraryDao(): LibraryDao
    abstract fun quizDao(): QuizDao
    abstract fun questionDao(): QuestionDao
    abstract fun familyScoreDao(): FamilyScoreDao
    abstract fun dailyWisdomDao(): DailyWisdomDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun rajolAllahDao(): RajolAllahDao
    abstract fun menOfGodDao(): MenOfGodDao
    abstract fun dailyPrayerDao(): DailyPrayerDao
    abstract fun prayerTextDao(): PrayerTextDao
    abstract fun patientDao(): PatientDao
    abstract fun lectureDetailDao(): LectureDetailDao
    abstract fun safhaSoraAyahDao(): SafhaSoraAyahDao
    abstract fun culturalLibraryDao(): CulturalLibraryDao

    companion object {
        @Volatile
        private var INSTANCE: EktefaaDatabase? = null

        fun getDatabase(context: Context): EktefaaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EktefaaDatabase::class.java,
                    "ektefaa_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

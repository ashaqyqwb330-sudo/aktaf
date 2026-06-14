package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.*
import com.example.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EktefaaViewModel(application: Application) : AndroidViewModel(application) {
    private val database = EktefaaDatabase.getDatabase(application)
    private val repository = EktefaaRepository(database.dao())

    // UI exposed states
    val allUsers = repository.allUsers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allChildren = repository.allChildren.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allClipboard = repository.allClipboard.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allPrayerLogs = repository.allPrayerLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allExpenseLogs = repository.allExpenseLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allDebts = repository.allDebts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allTodos = repository.allTodos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allAppliances = repository.allAppliances.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allMedications = repository.allMedications.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allQuranLogs = repository.allQuranLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allChildQuranWirds = repository.allChildQuranWirds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allChildQuranMistakes = repository.allChildQuranMistakes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Pre-insert default users: ONLY ONE root admin is default
        viewModelScope.launch {
            repository.allUsers.first().let { users ->
                if (users.isEmpty()) {
                    repository.insertUser(User(name = "الوالد المشرف", role = "Father", pinCode = "1234"))
                }
            }
        }
    }

    // Database updates on coroutines
    fun addUser(name: String, role: String, pin: String) {
        viewModelScope.launch { repository.insertUser(User(name = name, role = role, pinCode = pin)) }
    }

    fun deleteUser(id: Int) {
        viewModelScope.launch { repository.deleteUser(id) }
    }

    fun addChild(name: String, age: Int, grade: String, notes: String) {
        viewModelScope.launch { repository.insertChild(Child(name = name, age = age, grade = grade, notes = notes)) }
    }

    fun deleteChild(id: Int) {
        viewModelScope.launch { repository.deleteChild(id) }
    }

    fun addClipItem(content: String, category: String) {
        viewModelScope.launch { repository.insertClipItem(ClipItem(content = content, category = category)) }
    }

    fun updateClipItem(item: ClipItem) {
        viewModelScope.launch { repository.insertClipItem(item) }
    }

    fun deleteClipItem(id: Int) {
        viewModelScope.launch { repository.deleteClipItem(id) }
    }

    fun logPrayer(dateStr: String, fajr: Boolean, dhuhr: Boolean, asr: Boolean, maghrib: Boolean, isha: Boolean, qiyam: Boolean, azkar: Boolean) {
        viewModelScope.launch {
            repository.insertPrayerLog(
                PrayerLog(
                    dateStr = dateStr,
                    fajr = fajr,
                    dhuhr = dhuhr,
                    asr = asr,
                    maghrib = maghrib,
                    isha = isha,
                    qiyam = qiyam,
                    azkar = azkar
                )
            )
        }
    }

    fun addExpense(type: String, amount: Double, category: String, note: String, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch { repository.insertExpense(ExpenseLog(type = type, amount = amount, category = category, note = note, timestamp = timestamp)) }
    }

    fun deleteExpense(id: Int) {
        viewModelScope.launch { repository.deleteExpense(id) }
    }

    fun addDebt(shopName: String, desc: String, amount: Double, paid: Boolean, dueDate: String) {
        viewModelScope.launch { repository.insertDebt(DebtItem(shopName = shopName, desc = desc, amount = amount, paid = paid, dueDate = dueDate)) }
    }

    fun deleteDebt(id: Int) {
        viewModelScope.launch { repository.deleteDebt(id) }
    }

    fun addTodo(title: String, priority: String) {
        viewModelScope.launch { repository.insertTodo(TodoItem(title = title, priority = priority)) }
    }

    fun toggleTodo(todo: TodoItem) {
        viewModelScope.launch { repository.insertTodo(todo.copy(isCompleted = !todo.isCompleted)) }
    }

    fun deleteTodo(id: Int) {
        viewModelScope.launch { repository.deleteTodo(id) }
    }

    fun addAppliance(name: String, serviceDate: String, cycleDays: Int) {
        viewModelScope.launch { repository.insertAppliance(HouseAppliance(name = name, serviceDate = serviceDate, cycleDays = cycleDays)) }
    }

    fun deleteAppliance(id: Int) {
        viewModelScope.launch { repository.deleteAppliance(id) }
    }

    fun addMedication(
        patientName: String,
        name: String,
        scheduleTime: String,
        dosage: String,
        prescribedBy: String = "",
        method: String = "",
        warnings: String = "",
        recommendations: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.insertMedication(
                Medication(
                    patientName = patientName,
                    name = name,
                    scheduleTime = scheduleTime,
                    dosage = dosage,
                    prescribedBy = prescribedBy,
                    method = method,
                    warnings = warnings,
                    recommendations = recommendations,
                    notes = notes
                )
            )
        }
    }

    fun toggleMedication(med: Medication) {
        viewModelScope.launch { repository.insertMedication(med.copy(isTaken = !med.isTaken)) }
    }

    fun deleteMedication(id: Int) {
        viewModelScope.launch { repository.deleteMedication(id) }
    }

    fun addQuranLog(surah: String, verses: String, completed: Boolean, mistakes: Int) {
        viewModelScope.launch { repository.insertQuranLog(QuranLog(surah = surah, verses = verses, completed = completed, mistakes = mistakes)) }
    }

    fun deleteQuranLog(id: Int) {
        viewModelScope.launch { repository.deleteQuranLog(id) }
    }

    // Child Quran Wirds Action methods
    fun addChildQuranWird(childId: Int, surahId: Int, surahName: String, startVerse: Int, endVerse: Int) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            repository.insertChildQuranWird(
                ChildQuranWird(
                    childId = childId,
                    surahId = surahId,
                    surahName = surahName,
                    startVerse = startVerse,
                    endVerse = endVerse,
                    currentVerse = startVerse,
                    isCompleted = false,
                    assignedDate = dateStr
                )
            )
        }
    }

    fun updateChildQuranWird(wird: ChildQuranWird) {
        viewModelScope.launch {
            repository.insertChildQuranWird(wird)
        }
    }

    fun deleteChildQuranWird(id: Int) {
        viewModelScope.launch {
            repository.deleteChildQuranWird(id)
        }
    }

    // Child Quran Mistakes Action methods
    fun addChildQuranMistake(childId: Int, surahId: Int, surahName: String, verseNumber: Int, verseText: String, mistakeWord: String, isCorrected: Boolean = false) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            repository.insertChildQuranMistake(
                ChildQuranMistake(
                    childId = childId,
                    surahId = surahId,
                    surahName = surahName,
                    verseNumber = verseNumber,
                    verseText = verseText,
                    mistakeWord = mistakeWord,
                    isCorrected = isCorrected,
                    dateStr = dateStr
                )
            )
        }
    }

    fun toggleChildQuranMistake(mistake: ChildQuranMistake) {
        viewModelScope.launch {
            repository.insertChildQuranMistake(mistake.copy(isCorrected = !mistake.isCorrected))
        }
    }

    fun deleteChildQuranMistake(id: Int) {
        viewModelScope.launch {
            repository.deleteChildQuranMistake(id)
        }
    }

    fun getLessonsForChild(childId: Int): Flow<List<LessonFeedback>> = repository.getLessonsForChild(childId)
    
    fun addLessonFeedback(childId: Int, lesson: String, score: Int, diffs: String, positives: String) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            repository.insertLessonFeedback(
                LessonFeedback(
                    childId = childId,
                    lessonName = lesson,
                    score = score,
                    difficulties = diffs,
                    positiveNotes = positives,
                    dateStr = dateStr
                )
            )
        }
    }

    // Hybrid Academic Offline AI
    fun askGemini(prompt: String, apiKey: String, onResult: (String) -> Unit) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            val response = getOfflineAcademicResponse(prompt)
            onResult(response)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = GenerateContentRequest(listOf(Content(listOf(Part(prompt)))))
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                    ?: "لم أتمكن من الحصول على إجابة من نموذج الذكاء الاصطناعي."
                onResult(text)
            } catch (e: Exception) {
                onResult("تم التبديل التلقائي للذكاء الاصطناعي المحلي: ${getOfflineAcademicResponse(prompt)}")
            }
        }
    }

    private fun getOfflineAcademicResponse(prompt: String): String {
        val query = prompt.lowercase()
        return when {
            query.contains("قرآن") || query.contains("تفسير") -> {
                "الذكاء الاصطناعي المحلي (اكتفاء): مراجعة وحفظ القرآن الكريم ركيزة أساسية لأفراد الأسرة. ينصح بالبدء بسورة الملك يومياً قبل النوم، ومتابعة تكرار الآيات والآيات المتشابهة لتجنب تكرار الأخطاء."
            }
            query.contains("رياضيات") || query.contains("حساب") -> {
                "الذكاء الاصطناعي المحلي (اكتفاء): الرياضيات للمرحلة الأساسية تركز على إتقان العمليات الأربع أولاً، ثم الجبر والهندسة البسيطة. لحل المعادلات الصعبة، يمكن عزل المتغير وتطبيق قاعدة توازن الطرفين."
            }
            query.contains("يمن") || query.contains("منهج") -> {
                "الذكاء الاصطناعي المحلي (اكتفاء): منهج وزارة التربية والتعليم في اليمن للصفوف 1-12 يقسم المواد بحسب الخطة الربعية والنصفية. ينصح بجدولة الدروس وحل بنك الاختبارات المتوفر في التطبيق لتعزيز التحصيل العلمي."
            }
            query.contains("علوم") || query.contains("فيزياء") -> {
                "الذكاء الاصطناعي المحلي (اكتفاء): العلوم تشمل الأحياء والكيمياء والفيزياء لمرحلة التعليم العام. التركيز على التجربة والمشاهدة الحية هو مفتاح الفهم العميق للظواهر الطبيعية وقوانين الحركة."
            }
            else -> {
                "تطبيق اكتفاء (مساعدك المحلي الذكي): مرحباً بك! أنا نظام مساعد ذكي مدمج يعمل دون إنترنت لخدمتك. يمكنك سؤالي عن نصائح المناهج اليمنية، خطط الحفظ، طرق ترشيد المصروفات، وأساليب التربية المثمرة."
            }
        }
    }
}

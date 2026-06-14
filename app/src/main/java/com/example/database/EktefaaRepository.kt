package com.example.database

import kotlinx.coroutines.flow.Flow

class EktefaaRepository(private val dao: EktefaaDao) {
    // Users
    val allUsers: Flow<List<User>> = dao.getAllUsersFlow()
    suspend fun insertUser(user: User) = dao.insertUser(user)
    suspend fun deleteUser(id: Int) = dao.deleteUser(id)

    // Children
    val allChildren: Flow<List<Child>> = dao.getAllChildrenFlow()
    suspend fun insertChild(child: Child) = dao.insertChild(child)
    suspend fun deleteChild(id: Int) = dao.deleteChild(id)

    // Clipboard Items
    val allClipboard: Flow<List<ClipItem>> = dao.getAllClipboardFlow()
    suspend fun insertClipItem(item: ClipItem) = dao.insertClipItem(item)
    suspend fun deleteClipItem(id: Int) = dao.deleteClipItem(id)

    // Prayers
    val allPrayerLogs: Flow<List<PrayerLog>> = dao.getAllPrayerLogsFlow()
    suspend fun getPrayerLogByDate(dateStr: String): PrayerLog? = dao.getPrayerLogByDate(dateStr)
    suspend fun insertPrayerLog(log: PrayerLog) = dao.insertPrayerLog(log)

    // Financial
    val allExpenseLogs: Flow<List<ExpenseLog>> = dao.getAllExpenseLogsFlow()
    suspend fun insertExpense(log: ExpenseLog) = dao.insertExpense(log)
    suspend fun deleteExpense(id: Int) = dao.deleteExpense(id)

    // Debts
    val allDebts: Flow<List<DebtItem>> = dao.getAllDebtsFlow()
    suspend fun insertDebt(debt: DebtItem) = dao.insertDebt(debt)
    suspend fun deleteDebt(id: Int) = dao.deleteDebt(id)

    // Todos
    val allTodos: Flow<List<TodoItem>> = dao.getAllTodosFlow()
    suspend fun insertTodo(todo: TodoItem) = dao.insertTodo(todo)
    suspend fun deleteTodo(id: Int) = dao.deleteTodo(id)

    // Lessons
    fun getLessonsForChild(childId: Int): Flow<List<LessonFeedback>> = dao.getLessonsForChildFlow(childId)
    suspend fun insertLessonFeedback(feedback: LessonFeedback) = dao.insertLessonFeedback(feedback)

    // Appliances
    val allAppliances: Flow<List<HouseAppliance>> = dao.getAllAppliancesFlow()
    suspend fun insertAppliance(appliance: HouseAppliance) = dao.insertAppliance(appliance)
    suspend fun deleteAppliance(id: Int) = dao.deleteAppliance(id)

    // Medications
    val allMedications: Flow<List<Medication>> = dao.getAllMedicationsFlow()
    suspend fun insertMedication(med: Medication) = dao.insertMedication(med)
    suspend fun deleteMedication(id: Int) = dao.deleteMedication(id)

    // Quran
    val allQuranLogs: Flow<List<QuranLog>> = dao.getAllQuranLogsFlow()
    suspend fun insertQuranLog(log: QuranLog) = dao.insertQuranLog(log)
    suspend fun deleteQuranLog(id: Int) = dao.deleteQuranLog(id)

    // Child Quran portions (Wird)
    val allChildQuranWirds: Flow<List<ChildQuranWird>> = dao.getAllChildQuranWirdsFlow()
    suspend fun insertChildQuranWird(wird: ChildQuranWird) = dao.insertChildQuranWird(wird)
    suspend fun deleteChildQuranWird(id: Int) = dao.deleteChildQuranWird(id)

    // Child Quran mistakes
    val allChildQuranMistakes: Flow<List<ChildQuranMistake>> = dao.getAllChildQuranMistakesFlow()
    suspend fun insertChildQuranMistake(mistake: ChildQuranMistake) = dao.insertChildQuranMistake(mistake)
    suspend fun deleteChildQuranMistake(id: Int) = dao.deleteChildQuranMistake(id)
}

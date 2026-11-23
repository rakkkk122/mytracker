package com.example.tracker.data

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.tracker.TrackerApplication
import com.example.tracker.reminder.AndroidAlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val TAG = "DataBackupManager"

data class BackupPreferences(
    val notificationsEnabled: Boolean,
    val defaultAlarmTone: String?,
    val darkMode: Int,
    val themeColor: Int
)

data class BackupPlan(
    val title: String,
    val description: String,
    val location: String,
    val startTime: Long,
    val durationMinutes: Int,
    val isCompleted: Boolean,
    val alarmEnabled: Boolean,
    val alarmTone: String?,
    val imageBase64: String?
)

data class BackupExpense(
    val title: String,
    val amount: Double,
    val reason: String,
    val dateMillis: Long,
    val imageBase64: String?
)

data class BackupPayload(
    val version: Int,
    val app: String,
    val exportedAt: Long,
    val exportedDate: String,
    val preferences: BackupPreferences?,
    val plans: List<BackupPlan>,
    val expenses: List<BackupExpense> = emptyList(),
    val budgets: List<BackupBudget> = emptyList()
)

object DataBackupManager {

    /**
     * Mengekspor seluruh data plan, pengeluaran, preferensi, dan budget bulanan ke dalam format JSON.
     * Gambar internal di-encode ke Base64 agar dapat dipulihkan ke perangkat lain.
     */
    suspend fun exportToJson(context: Context): String = withContext(Dispatchers.IO) {
        val app = context.applicationContext as TrackerApplication
        val planRepo = app.container.planRepository
        val expenseRepo = app.container.expenseRepository
        val prefs = UserPreferences(context)

        val allPlans = planRepo.getAllPlansStream().first()
        val allExpenses = expenseRepo.getAllExpensesStream().first()

        val rootJson = JSONObject().apply {
            put("version", 3)
            put("app", "Tracker")
            put("exportedAt", System.currentTimeMillis())
            put("exportedDate", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))

            // Preferensi pengguna
            val prefJson = JSONObject().apply {
                put("notificationsEnabled", prefs.notificationsEnabled)
                put("defaultAlarmTone", prefs.defaultAlarmTone ?: JSONObject.NULL)
                put("darkMode", prefs.darkMode)
                put("themeColor", prefs.themeColor)
            }
            put("preferences", prefJson)

            // Daftar plan
            val plansArray = JSONArray()
            allPlans.forEach { plan ->
                val planObj = JSONObject().apply {
                    put("title", plan.title)
                    put("description", plan.description)
                    put("location", plan.location)
                    put("startTime", plan.startTime)
                    put("durationMinutes", plan.durationMinutes)
                    put("isCompleted", plan.isCompleted)
                    put("alarmEnabled", plan.alarmEnabled)
                    put("alarmTone", plan.alarmTone ?: JSONObject.NULL)

                    val base64Img = plan.imagePath?.let { path ->
                        runCatching {
                            val file = File(path)
                            if (file.exists() && file.length() < 10 * 1024 * 1024) {
                                Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
                            } else null
                        }.getOrNull()
                    }
                    put("imageBase64", base64Img ?: JSONObject.NULL)
                }
                plansArray.put(planObj)
            }
            put("plans", plansArray)

            // Daftar pengeluaran
            val expensesArray = JSONArray()
            allExpenses.forEach { expense ->
                val expenseObj = JSONObject().apply {
                    put("title", expense.title)
                    put("amount", expense.amount)
                    put("reason", expense.reason)
                    put("dateMillis", expense.dateMillis)

                    val base64Img = expense.imagePath?.let { path ->
                        runCatching {
                            val file = File(path)
                            if (file.exists() && file.length() < 10 * 1024 * 1024) {
                                Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
                            } else null
                        }.getOrNull()
                    }
                    put("imageBase64", base64Img ?: JSONObject.NULL)
                }
                expensesArray.put(expenseObj)
            }
            put("expenses", expensesArray)

            // Daftar budget bulanan
            val budgetsArray = JSONArray()
            prefs.getAllMonthlyBudgets().forEach { b ->
                val bObj = JSONObject().apply {
                    put("year", b.year)
                    put("month", b.month)
                    put("amount", b.amount)
                }
                budgetsArray.put(bObj)
            }
            put("budgets", budgetsArray)
        }

        rootJson.toString(2)
    }

    /**
     * Mem-parsing teks JSON backup untuk divalidasi sebelum dilakukan impor.
     */
    fun parseJson(jsonString: String): BackupPayload {
        val root = JSONObject(jsonString)

        val version = root.optInt("version", 1)
        val app = root.optString("app", "Tracker")
        val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())
        val exportedDate = root.optString("exportedDate", "")

        val prefObj = root.optJSONObject("preferences")
        val preferences = prefObj?.let {
            BackupPreferences(
                notificationsEnabled = it.optBoolean("notificationsEnabled", true),
                defaultAlarmTone = if (it.isNull("defaultAlarmTone")) null else it.optString("defaultAlarmTone"),
                darkMode = it.optInt("darkMode", 0),
                themeColor = it.optInt("themeColor", 0)
            )
        }

        val plansList = mutableListOf<BackupPlan>()
        val plansArray = root.optJSONArray("plans") ?: JSONArray()
        for (i in 0 until plansArray.length()) {
            val obj = plansArray.getJSONObject(i)
            plansList.add(
                BackupPlan(
                    title = obj.optString("title", "Plan Tanpa Judul"),
                    description = obj.optString("description", ""),
                    location = obj.optString("location", ""),
                    startTime = obj.optLong("startTime", System.currentTimeMillis()),
                    durationMinutes = obj.optInt("durationMinutes", 30),
                    isCompleted = obj.optBoolean("isCompleted", false),
                    alarmEnabled = obj.optBoolean("alarmEnabled", true),
                    alarmTone = if (obj.isNull("alarmTone")) null else obj.optString("alarmTone"),
                    imageBase64 = if (obj.isNull("imageBase64")) null else obj.optString("imageBase64")
                )
            )
        }

        val expensesList = mutableListOf<BackupExpense>()
        val expensesArray = root.optJSONArray("expenses") ?: JSONArray()
        for (i in 0 until expensesArray.length()) {
            val obj = expensesArray.getJSONObject(i)
            expensesList.add(
                BackupExpense(
                    title = obj.optString("title", "Pengeluaran"),
                    amount = obj.optDouble("amount", 0.0),
                    reason = obj.optString("reason", ""),
                    dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
                    imageBase64 = if (obj.isNull("imageBase64")) null else obj.optString("imageBase64")
                )
            )
        }

        val budgetsList = mutableListOf<BackupBudget>()
        val budgetsArray = root.optJSONArray("budgets") ?: JSONArray()
        for (i in 0 until budgetsArray.length()) {
            val obj = budgetsArray.getJSONObject(i)
            budgetsList.add(
                BackupBudget(
                    year = obj.optInt("year", 0),
                    month = obj.optInt("month", 0),
                    amount = obj.optDouble("amount", 0.0)
                )
            )
        }

        return BackupPayload(
            version = version,
            app = app,
            exportedAt = exportedAt,
            exportedDate = exportedDate,
            preferences = preferences,
            plans = plansList,
            expenses = expensesList,
            budgets = budgetsList
        )
    }

    /**
     * Mengimpor data dari [BackupPayload].
     */
    suspend fun importBackup(
        context: Context,
        payload: BackupPayload,
        replaceExisting: Boolean,
        restorePreferences: Boolean
    ): Int = withContext(Dispatchers.IO) {
        val app = context.applicationContext as TrackerApplication
        val planRepo = app.container.planRepository
        val expenseRepo = app.container.expenseRepository
        val scheduler = AndroidAlarmScheduler(context)
        val imagesDir = File(context.filesDir, "plan_images").apply { mkdirs() }

        if (replaceExisting) {
            planRepo.deleteAllPlans()
            expenseRepo.deleteAllExpenses()
        }

        var importedCount = 0
        val now = System.currentTimeMillis()

        payload.plans.forEachIndexed { index, backupPlan ->
            try {
                var savedImagePath: String? = null
                if (!backupPlan.imageBase64.isNullOrBlank()) {
                    runCatching {
                        val imageBytes = Base64.decode(backupPlan.imageBase64, Base64.DEFAULT)
                        val imageFile = File(imagesDir, "plan_imported_${System.currentTimeMillis()}_$index.jpg")
                        imageFile.writeBytes(imageBytes)
                        savedImagePath = imageFile.absolutePath
                    }.onFailure { Log.e(TAG, "Gagal mendecode gambar plan", it) }
                }

                val planToInsert = Plan(
                    id = 0,
                    title = backupPlan.title,
                    description = backupPlan.description,
                    location = backupPlan.location,
                    startTime = backupPlan.startTime,
                    durationMinutes = backupPlan.durationMinutes,
                    isCompleted = backupPlan.isCompleted,
                    alarmEnabled = backupPlan.alarmEnabled,
                    imagePath = savedImagePath,
                    alarmTone = backupPlan.alarmTone
                )

                val newId = planRepo.insertPlan(planToInsert)
                importedCount++

                if (planToInsert.alarmEnabled && planToInsert.startTime > now && !planToInsert.isCompleted) {
                    scheduler.schedule(planToInsert.copy(id = newId.toInt()))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal mengimpor plan: ${backupPlan.title}", e)
            }
        }

        payload.expenses.forEachIndexed { index, backupExpense ->
            try {
                var savedImagePath: String? = null
                if (!backupExpense.imageBase64.isNullOrBlank()) {
                    runCatching {
                        val imageBytes = Base64.decode(backupExpense.imageBase64, Base64.DEFAULT)
                        val imageFile = File(imagesDir, "expense_imported_${System.currentTimeMillis()}_$index.jpg")
                        imageFile.writeBytes(imageBytes)
                        savedImagePath = imageFile.absolutePath
                    }.onFailure { Log.e(TAG, "Gagal mendecode gambar pengeluaran", it) }
                }

                val expenseToInsert = Expense(
                    id = 0,
                    title = backupExpense.title,
                    amount = backupExpense.amount,
                    reason = backupExpense.reason,
                    dateMillis = backupExpense.dateMillis,
                    imagePath = savedImagePath
                )

                expenseRepo.insertExpense(expenseToInsert)
                importedCount++
            } catch (e: Exception) {
                Log.e(TAG, "Gagal mengimpor pengeluaran: ${backupExpense.title}", e)
            }
        }

        val prefs = UserPreferences(context)
        payload.budgets.forEach { b ->
            if (b.year > 0 && b.month in 1..12 && b.amount >= 0) {
                val yearMonth = java.time.YearMonth.of(b.year, b.month)
                val startDate = b.startDate?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
                    ?: yearMonth.atDay(1)
                val endDate = b.endDate?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
                    ?: yearMonth.atEndOfMonth()

                prefs.setBudgetForMonth(yearMonth, b.amount, startDate, endDate)
            }
        }

        if (restorePreferences && payload.preferences != null) {
            prefs.notificationsEnabled = payload.preferences.notificationsEnabled
            prefs.defaultAlarmTone = payload.preferences.defaultAlarmTone
            prefs.darkMode = payload.preferences.darkMode
            prefs.themeColor = payload.preferences.themeColor
        }

        importedCount
    }
}

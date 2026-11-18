package com.example.tracker.data

import android.content.Context

data class BackupBudget(
    val year: Int,
    val month: Int,
    val amount: Double
)

class UserPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE)

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS, value).apply()

    /** URI nada alarm default untuk plan baru. null = default sistem. */
    var defaultAlarmTone: String?
        get() = prefs.getString(KEY_DEFAULT_TONE, null)
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_DEFAULT_TONE) else putString(KEY_DEFAULT_TONE, value)
            }.apply()
        }

    /** 0: System, 1: Light, 2: Dark */
    var darkMode: Int
        get() = prefs.getInt(KEY_DARK_MODE, 0)
        set(value) = prefs.edit().putInt(KEY_DARK_MODE, value).apply()

    /** 0: Indigo, 1: Emerald, 2: Rose */
    var themeColor: Int
        get() = prefs.getInt(KEY_THEME_COLOR, 0)
        set(value) = prefs.edit().putInt(KEY_THEME_COLOR, value).apply()

    fun getBudgetForMonth(year: Int, month: Int): Double {
        val key = "budget_${year}_$month"
        val valueStr = prefs.getString(key, null) ?: return 0.0
        return valueStr.toDoubleOrNull() ?: 0.0
    }

    fun getBudgetForMonth(yearMonth: java.time.YearMonth): Double {
        return getBudgetForMonth(yearMonth.year, yearMonth.monthValue)
    }

    fun setBudgetForMonth(year: Int, month: Int, amount: Double) {
        val key = "budget_${year}_$month"
        prefs.edit().putString(key, amount.toString()).apply()
    }

    fun setBudgetForMonth(yearMonth: java.time.YearMonth, amount: Double) {
        setBudgetForMonth(yearMonth.year, yearMonth.monthValue, amount)
    }

    fun getAllMonthlyBudgets(): List<BackupBudget> {
        val list = mutableListOf<BackupBudget>()
        prefs.all.forEach { (key, value) ->
            if (key.startsWith("budget_")) {
                val parts = key.removePrefix("budget_").split("_")
                if (parts.size == 2) {
                    val year = parts[0].toIntOrNull()
                    val month = parts[1].toIntOrNull()
                    val amount = (value as? String)?.toDoubleOrNull()
                    if (year != null && month != null && amount != null) {
                        list.add(BackupBudget(year, month, amount))
                    }
                }
            }
        }
        return list
    }

    private companion object {
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_DEFAULT_TONE = "default_alarm_tone"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_THEME_COLOR = "theme_color"
    }
}
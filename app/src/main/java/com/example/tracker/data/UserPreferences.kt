package com.example.tracker.data

import android.content.Context
import java.time.LocalDate
import java.time.YearMonth

data class BackupBudget(
    val year: Int,
    val month: Int,
    val amount: Double,
    val startDate: String? = null,
    val endDate: String? = null
)

data class Budget(
    val amount: Double = 0.0,
    val startDate: LocalDate,
    val endDate: LocalDate
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

    fun getBudgetForMonth(yearMonth: YearMonth): Budget {
        val key = "budget_${yearMonth.year}_${yearMonth.monthValue}"
        val amountStr = prefs.getString(key, null)
        val amount = amountStr?.toDoubleOrNull() ?: 0.0

        val startStr = prefs.getString("${key}_start", null)
        val endStr = prefs.getString("${key}_end", null)

        val startDate = startStr?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: yearMonth.atDay(1)
        val endDate = endStr?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: yearMonth.atEndOfMonth()

        return Budget(
            amount = amount,
            startDate = startDate,
            endDate = endDate
        )
    }

    fun setBudgetForMonth(
        yearMonth: YearMonth,
        amount: Double,
        startDate: LocalDate = yearMonth.atDay(1),
        endDate: LocalDate = yearMonth.atEndOfMonth()
    ) {
        val key = "budget_${yearMonth.year}_${yearMonth.monthValue}"
        prefs.edit().apply {
            putString(key, amount.toString())
            putString("${key}_start", startDate.toString())
            putString("${key}_end", endDate.toString())
        }.apply()
    }

    fun getAllMonthlyBudgets(): List<BackupBudget> {
        val list = mutableListOf<BackupBudget>()
        prefs.all.forEach { (key, value) ->
            if (key.startsWith("budget_") && !key.endsWith("_start") && !key.endsWith("_end")) {
                val parts = key.removePrefix("budget_").split("_")
                if (parts.size == 2) {
                    val year = parts[0].toIntOrNull()
                    val month = parts[1].toIntOrNull()
                    val amount = (value as? String)?.toDoubleOrNull()
                    if (year != null && month != null && amount != null) {
                        val startStr = prefs.getString("${key}_start", null)
                        val endStr = prefs.getString("${key}_end", null)
                        list.add(
                            BackupBudget(
                                year = year,
                                month = month,
                                amount = amount,
                                startDate = startStr,
                                endDate = endStr
                            )
                        )
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
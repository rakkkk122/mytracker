package com.example.tracker.data

import android.content.Context

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

    private companion object {
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_DEFAULT_TONE = "default_alarm_tone"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_THEME_COLOR = "theme_color"
    }
}
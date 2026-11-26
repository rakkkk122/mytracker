package com.example.tracker

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.tracker.data.AppContainer
import com.example.tracker.data.AppDataContainer
import com.example.tracker.data.UserPreferences
import com.example.tracker.reminder.AlarmTones
import com.example.tracker.reminder.SmartDailyReminderWorker
import com.example.tracker.reminder.SmartReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TrackerApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
        setupNotificationChannels()
        initSmartDailyReminder()
    }

    private fun setupNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // Hapus channel versi lama jika ada, agar setelan nada alarm baru digunakan
            notificationManager.deleteNotificationChannel("PLAN_REMINDER_CHANNEL")

            // Inisialisasi channel default dengan setelan USAGE_ALARM dan getaran
            AlarmTones.ensureChannel(this, null)

            // Inisialisasi channel untuk Smart Daily Reminder
            val smartReminderChannel = android.app.NotificationChannel(
                SmartDailyReminderWorker.CHANNEL_ID,
                "Pengingat Refleksi Harian",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Pengingat refleksi cepat kebiasaan dan pengeluaran harian"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(smartReminderChannel)
        }
    }

    private fun initSmartDailyReminder() {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = UserPreferences(this@TrackerApplication)
            val (enabled, hour, minute) = prefs.getDailyReminderSettings()
            if (enabled) {
                SmartReminderScheduler.scheduleNext(this@TrackerApplication, hour, minute)
            }
        }
    }
}

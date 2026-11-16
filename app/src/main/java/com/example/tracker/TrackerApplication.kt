package com.example.tracker

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.tracker.data.AppContainer
import com.example.tracker.data.AppDataContainer
import com.example.tracker.reminder.AlarmTones

class TrackerApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
        setupNotificationChannels()
    }

    private fun setupNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // Hapus channel versi lama jika ada, agar setelan nada alarm baru digunakan
            notificationManager.deleteNotificationChannel("PLAN_REMINDER_CHANNEL")

            // Inisialisasi channel default dengan setelan USAGE_ALARM dan getaran
            AlarmTones.ensureChannel(this, null)
        }
    }
}

package com.example.tracker.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.tracker.MainActivity
import com.example.tracker.TrackerApplication
import com.example.tracker.data.UserPreferences
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SmartDailyReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "SmartDailyReminderWorker running check...")

        val prefs = UserPreferences(context)
        val (enabled, hour, minute) = prefs.getDailyReminderSettings()
        if (!enabled || !prefs.notificationsEnabled) {
            Log.d(TAG, "Daily reminder disabled by user settings.")
            // Reschedule next day just in case settings are toggled
            SmartReminderScheduler.scheduleNext(context, hour, minute)
            return Result.success()
        }

        val app = context.applicationContext as TrackerApplication
        val container = app.container
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val todayStr = today.toString()

        val startOfDay = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        // 1. Cek Check-in
        val checkIn = container.dailyCheckInRepository.getCheckInForDate(todayStr)
        if (checkIn != null && checkIn.isCheckedIn) {
            Log.d(TAG, "Smart Skip: User already completed daily check-in today.")
            SmartReminderScheduler.scheduleNext(context, hour, minute)
            return Result.success()
        }

        // 2. Cek Pengeluaran hari ini atau No-Spend Day
        if (checkIn != null && checkIn.isNoSpendDay) {
            Log.d(TAG, "Smart Skip: Today is already marked as No-Spend Day.")
            SmartReminderScheduler.scheduleNext(context, hour, minute)
            return Result.success()
        }

        val expensesToday = container.expenseRepository.getExpensesForDayStream(startOfDay, endOfDay).first()
        if (expensesToday.isNotEmpty()) {
            Log.d(TAG, "Smart Skip: User already recorded an expense today.")
            SmartReminderScheduler.scheduleNext(context, hour, minute)
            return Result.success()
        }

        // 3. Cek Kebiasaan/Plan: Apakah sudah menyelesaikan minimal 1 habit/plan hari ini?
        val plansToday = container.planRepository.getPlansForDayStream(startOfDay, endOfDay).first()
        val hasCompletedPlan = plansToday.any { it.isCompleted }
        if (hasCompletedPlan) {
            Log.d(TAG, "Smart Skip: User already completed at least 1 habit/plan today.")
            SmartReminderScheduler.scheduleNext(context, hour, minute)
            return Result.success()
        }

        // Jika semua syarat di atas tidak terpenuhi, munculkan notifikasi
        showSmartReminderNotification(todayStr)

        // Jadwalkan untuk hari berikutnya
        SmartReminderScheduler.scheduleNext(context, hour, minute)
        return Result.success()
    }

    private fun showSmartReminderNotification(todayStr: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Ensure Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Pengingat Refleksi Harian",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Pengingat refleksi cepat kebiasaan dan pengeluaran harian"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }

        // Intent buka aplikasi ke Dashboard
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_CHECK_IN", true)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Quick Action "No-Spend Day" broadcast
        val noSpendIntent = Intent(context, QuickActionReceiver::class.java).apply {
            action = QuickActionReceiver.ACTION_MARK_NO_SPEND
            putExtra(QuickActionReceiver.EXTRA_DATE_STR, todayStr)
            putExtra(QuickActionReceiver.EXTRA_NOTIFICATION_ID, NOTIFICATION_ID)
        }
        val noSpendPendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID + 50,
            noSpendIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val messageText = "Refleksi 10 detik: ada pengeluaran hari ini atau berhasil No-Spend Day?"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("✨ Smart Daily Check-in")
            .setContentText(messageText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.checkbox_on_background,
                "No-Spend Day 🛡️",
                noSpendPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_edit,
                "Check-in Sekarang",
                openAppPendingIntent
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "Smart Daily Reminder notification shown!")
    }

    companion object {
        const val TAG = "SmartDailyReminder"
        const val CHANNEL_ID = "SMART_DAILY_REMINDER_CHANNEL"
        const val NOTIFICATION_ID = 888100
    }
}

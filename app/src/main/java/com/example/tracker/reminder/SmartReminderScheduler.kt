package com.example.tracker.reminder

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object SmartReminderScheduler {
    private const val WORK_NAME = "smart_daily_reminder_work"

    /**
     * Menjadwalkan pengingat harian berikutnya pada jam [targetHour]:[targetMinute].
     */
    fun scheduleNext(context: Context, targetHour: Int = 20, targetMinute: Int = 30) {
        val now = LocalDateTime.now()
        var targetTime = now.with(LocalTime.of(targetHour, targetMinute, 0, 0))

        if (!targetTime.isAfter(now)) {
            targetTime = targetTime.plusDays(1)
        }

        val initialDelayMinutes = Duration.between(now, targetTime).toMinutes().coerceAtLeast(1)

        val workRequest = OneTimeWorkRequestBuilder<SmartDailyReminderWorker>()
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .addTag("smart_daily_reminder")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        Log.d(
            "SmartReminderScheduler",
            "Smart reminder scheduled in $initialDelayMinutes minutes (at $targetTime)"
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        Log.d("SmartReminderScheduler", "Smart reminder cancelled")
    }
}

package com.example.tracker.reminder

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.tracker.TrackerApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class QuickActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("QuickActionReceiver", "onReceive triggered with action: $action")

        if (action == ACTION_MARK_NO_SPEND) {
            val dateStr = intent.getStringExtra(EXTRA_DATE_STR) ?: LocalDate.now().toString()
            val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, SmartDailyReminderWorker.NOTIFICATION_ID)

            // Tutup notifikasi
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notifId)

            val pendingResult = goAsync()
            val app = context.applicationContext as TrackerApplication
            val repository = app.container.dailyCheckInRepository

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repository.markNoSpendDay(dateStr, isNoSpend = true)
                    Log.d("QuickActionReceiver", "Marked $dateStr as No-Spend Day from notification action")
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(
                            context,
                            "Hari ini ditandai No-Spend Day! 🛡️ Hebat!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.e("QuickActionReceiver", "Failed to mark no-spend day", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_MARK_NO_SPEND = "com.example.tracker.ACTION_MARK_NO_SPEND"
        const val EXTRA_DATE_STR = "EXTRA_DATE_STR"
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
    }
}

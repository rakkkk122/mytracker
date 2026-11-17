package com.example.tracker.reminder

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.tracker.MainActivity
import com.example.tracker.TrackerApplication
import com.example.tracker.data.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("AlarmReceiver", "onReceive triggered with action: $action")

        // Jadwalkan ulang jika perangkat baru dinyalakan, update app, atau izin alarm tepat waktu diubah
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        ) {
            AlarmPermissionUtils.rescheduleAllActiveAlarms(context)
            return
        }

        // Tangani tombol "Matikan" pada notifikasi alarm
        if (action == "com.example.tracker.ACTION_DISMISS_ALARM") {
            val planId = intent.getIntExtra("PLAN_ID", -1)
            if (planId != -1) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(planId)
                Log.d("AlarmReceiver", "Alarm plan $planId dimatikan oleh user")
            }
            return
        }

        // Tangani tombol "Centang Selesai" pada notifikasi alarm
        if (action == "com.example.tracker.ACTION_COMPLETE_ALARM") {
            val planId = intent.getIntExtra("PLAN_ID", -1)
            if (planId != -1) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(planId)

                val pendingResult = goAsync()
                val appContainer = (context.applicationContext as TrackerApplication).container
                val planRepository = appContainer.planRepository
                val alarmScheduler = AndroidAlarmScheduler(context.applicationContext)

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val plan = planRepository.getPlanById(planId)
                        if (plan != null) {
                            val updatedPlan = plan.copy(isCompleted = true)
                            planRepository.updatePlan(updatedPlan)
                            alarmScheduler.cancel(updatedPlan)
                            Log.d("AlarmReceiver", "Plan $planId ditandai selesai dari notifikasi")
                        }
                    } catch (e: Exception) {
                        Log.e("AlarmReceiver", "Gagal menandai selesai plan $planId", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            return
        }

        // Jika action bukan trigger alarm kami, abaikan
        if (action != "com.example.tracker.ACTION_ALARM_TRIGGER") return

        // Pengguna mematikan notifikasi dari Pengaturan aplikasi
        if (!UserPreferences(context).notificationsEnabled) {
            Log.d("AlarmReceiver", "Notifikasi dinonaktifkan di pengaturan user")
            return
        }

        val planId = intent.getIntExtra("PLAN_ID", -1)
        if (planId == -1) return

        val title = intent.getStringExtra("PLAN_TITLE") ?: "Pengingat plan"
        val location = intent.getStringExtra("PLAN_LOCATION").orEmpty()
        val tone = intent.getStringExtra("PLAN_TONE")

        val channelId = AlarmTones.ensureChannel(context, tone)
        val soundUri = tone?.let { Uri.parse(it) } ?: AlarmTones.defaultAlarmUri()

        val openApp = PendingIntent.getActivity(
            context,
            planId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("FROM_ALARM", true)
                putExtra("PLAN_ID", planId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = "com.example.tracker.ACTION_DISMISS_ALARM"
            putExtra("PLAN_ID", planId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            planId + 100000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = "com.example.tracker.ACTION_COMPLETE_ALARM"
            putExtra("PLAN_ID", planId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            planId + 200000,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
        val contentMessage = if (location.isNotBlank()) {
            "Waktunya mulai di $location"
        } else {
            "Waktunya plan dimulai!"
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ $title")
            .setContentText(contentMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentMessage))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(soundUri, AudioManager.STREAM_ALARM)
            .setVibrate(vibrationPattern)
            .setFullScreenIntent(openApp, true)
            .setContentIntent(openApp)
            .setDeleteIntent(dismissPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Centang Selesai", completePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Matikan", dismissPendingIntent)
            .addAction(android.R.drawable.ic_menu_view, "Buka Plan", openApp)
            .setAutoCancel(true)
            .setOngoing(false)
            .setTimeoutAfter(120_000) // Berhenti berdering otomatis setelah 2 menit jika diabaikan

        val notification = builder.build().apply {
            flags = flags or Notification.FLAG_INSISTENT
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(planId, notification)
        Log.d("AlarmReceiver", "Notifikasi & alarm dibunyikan untuk plan $planId")
    }
}

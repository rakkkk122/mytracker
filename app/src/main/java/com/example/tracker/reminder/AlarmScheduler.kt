package com.example.tracker.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.tracker.MainActivity
import com.example.tracker.data.Plan

interface AlarmScheduler {
    fun schedule(plan: Plan)
    fun cancel(plan: Plan)
}

class AndroidAlarmScheduler(private val context: Context) : AlarmScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(plan: Plan) {
        if (!plan.alarmEnabled) return
        val triggerTime = plan.startTime
        // Waktu yang sudah lewat tidak dijadwalkan, supaya tidak langsung berbunyi
        if (triggerTime <= System.currentTimeMillis()) {
            Log.d("AlarmScheduler", "Plan ${plan.id} (${plan.title}) waktu sudah lewat, lewati.")
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.tracker.ACTION_ALARM_TRIGGER"
            putExtra("PLAN_ID", plan.id)
            putExtra("PLAN_TITLE", plan.title)
            putExtra("PLAN_LOCATION", plan.location)
            putExtra("PLAN_TONE", plan.alarmTone)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            plan.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (canScheduleExact()) {
                val showIntent = PendingIntent.getActivity(
                    context,
                    plan.id,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("FROM_ALARM", true)
                        putExtra("PLAN_ID", plan.id)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                Log.d("AlarmScheduler", "setAlarmClock berhasil untuk plan ${plan.id} pada $triggerTime")
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                Log.d("AlarmScheduler", "setExactAndAllowWhileIdle untuk plan ${plan.id}")
            }
        } catch (e: SecurityException) {
            Log.w("AlarmScheduler", "SecurityException saat setExact, fallback ke inexact", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    override fun cancel(plan: Plan) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.tracker.ACTION_ALARM_TRIGGER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            plan.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("AlarmScheduler", "Alarm dibatalkan untuk plan ${plan.id}")
    }

    private fun canScheduleExact(): Boolean =
        AlarmPermissionUtils.canScheduleExactAlarms(context)
}

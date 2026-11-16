package com.example.tracker.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build

object AlarmTones {

    /** Channel bawaan yang dibuat untuk nada alarm default sistem. Menggunakan v2 agar setelan baru diterapkan. */
    const val DEFAULT_CHANNEL_ID = "PLAN_REMINDER_CHANNEL_V2"

    fun defaultAlarmUri(): Uri =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    fun title(context: Context, toneUri: String?): String {
        if (toneUri == null) return "Default alarm sistem"
        return runCatching {
            RingtoneManager.getRingtone(context, Uri.parse(toneUri))?.getTitle(context)
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Nada kustom"
    }

    /**
     * Sejak Android 8, suara dan getaran notifikasi ditentukan oleh channel.
     * Kita pastikan setiap channel dikonfigurasi dengan USAGE_ALARM, getaran,
     * dan visibilitas publik di lockscreen.
     */
    fun ensureChannel(context: Context, toneUri: String?): String {
        val channelId = if (toneUri == null) {
            DEFAULT_CHANNEL_ID
        } else {
            "PLAN_REMINDER_TONE_V2_" + Integer.toHexString(toneUri.hashCode())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return channelId
            if (manager.getNotificationChannel(channelId) == null) {
                val soundUri = toneUri?.let { Uri.parse(it) } ?: defaultAlarmUri()
                val channelName = if (toneUri == null) {
                    "Pengingat Plan & Alarm"
                } else {
                    "Pengingat: ${title(context, toneUri)}"
                }

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val channel = NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Pengingat dan alarm untuk plan yang dijadwalkan"
                    setSound(soundUri, audioAttributes)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setBypassDnd(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
        return channelId
    }
}

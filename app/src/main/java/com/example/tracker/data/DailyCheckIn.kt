package com.example.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Menyimpan status Check-in harian dan No-Spend Day per tanggal.
 * @param dateStr Format YYYY-MM-DD (e.g. 2026-11-25)
 * @param isCheckedIn Status apakah user sudah menyelesaikan check-in hari ini
 * @param isNoSpendDay Status apakah hari ini adalah No-Spend Day (tidak ada pengeluaran)
 * @param mood Emoji atau deskripsi singkat mood (e.g. 😊 Senang, ⚡ Bersemangat, 🧘 Tenang, dll)
 * @param note Catatan / refleksi singkat harian
 * @param checkedInAt Timestamp millis saat check-in dilakukan
 */
@Entity(tableName = "daily_check_ins")
data class DailyCheckIn(
    @PrimaryKey
    val dateStr: String,
    val isCheckedIn: Boolean = true,
    val isNoSpendDay: Boolean = false,
    val mood: String? = null,
    val note: String? = null,
    val checkedInAt: Long = System.currentTimeMillis()
)

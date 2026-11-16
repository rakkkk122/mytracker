package com.example.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val amount: Double,
    val reason: String,
    val dateMillis: Long, // Epoch millis menyimpan tanggal & waktu pengeluaran
    val imagePath: String? = null // Path file gambar opsional di penyimpanan internal
)

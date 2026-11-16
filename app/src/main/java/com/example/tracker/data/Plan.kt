package com.example.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plans")
data class Plan(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val location: String,
    val startTime: Long, // Epoch millis
    val durationMinutes: Int,
    val isCompleted: Boolean = false,
    val alarmEnabled: Boolean = true,
    val imagePath: String? = null, // Path file gambar di penyimpanan internal aplikasi
    val alarmTone: String? = null  // URI nada alarm; null = default sistem
)
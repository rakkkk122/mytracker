package com.example.tracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyCheckInDao {
    @Query("SELECT * FROM daily_check_ins WHERE dateStr = :dateStr LIMIT 1")
    fun getCheckInForDate(dateStr: String): Flow<DailyCheckIn?>

    @Query("SELECT * FROM daily_check_ins WHERE dateStr = :dateStr LIMIT 1")
    suspend fun getCheckInForDateOnce(dateStr: String): DailyCheckIn?

    @Query("SELECT * FROM daily_check_ins")
    fun getAllCheckIns(): Flow<List<DailyCheckIn>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(checkIn: DailyCheckIn)

    @Query("UPDATE daily_check_ins SET isNoSpendDay = :isNoSpend WHERE dateStr = :dateStr")
    suspend fun updateNoSpendStatus(dateStr: String, isNoSpend: Boolean)

    @Query("DELETE FROM daily_check_ins")
    suspend fun deleteAll()
}

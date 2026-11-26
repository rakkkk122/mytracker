package com.example.tracker.data

import kotlinx.coroutines.flow.Flow

interface DailyCheckInRepository {
    fun getCheckInForDateStream(dateStr: String): Flow<DailyCheckIn?>
    suspend fun getCheckInForDate(dateStr: String): DailyCheckIn?
    fun getAllCheckInsStream(): Flow<List<DailyCheckIn>>
    suspend fun saveCheckIn(checkIn: DailyCheckIn)
    suspend fun markNoSpendDay(dateStr: String, isNoSpend: Boolean = true)
    suspend fun deleteAll()
}

class OfflineDailyCheckInRepository(
    private val dao: DailyCheckInDao
) : DailyCheckInRepository {
    override fun getCheckInForDateStream(dateStr: String): Flow<DailyCheckIn?> =
        dao.getCheckInForDate(dateStr)

    override suspend fun getCheckInForDate(dateStr: String): DailyCheckIn? =
        dao.getCheckInForDateOnce(dateStr)

    override fun getAllCheckInsStream(): Flow<List<DailyCheckIn>> =
        dao.getAllCheckIns()

    override suspend fun saveCheckIn(checkIn: DailyCheckIn) =
        dao.insertOrUpdate(checkIn)

    override suspend fun markNoSpendDay(dateStr: String, isNoSpend: Boolean) {
        val existing = dao.getCheckInForDateOnce(dateStr)
        if (existing == null) {
            dao.insertOrUpdate(
                DailyCheckIn(
                    dateStr = dateStr,
                    isCheckedIn = true,
                    isNoSpendDay = isNoSpend,
                    checkedInAt = System.currentTimeMillis()
                )
            )
        } else {
            dao.insertOrUpdate(
                existing.copy(
                    isCheckedIn = true,
                    isNoSpendDay = isNoSpend
                )
            )
        }
    }

    override suspend fun deleteAll() = dao.deleteAll()
}

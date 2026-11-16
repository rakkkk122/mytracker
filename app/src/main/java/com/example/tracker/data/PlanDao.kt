package com.example.tracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
    @Query("SELECT * FROM plans ORDER BY startTime ASC")
    fun getAllPlans(): Flow<List<Plan>>

    @Query("SELECT * FROM plans WHERE startTime >= :startOfDay AND startTime < :endOfDay ORDER BY startTime ASC")
    fun getPlansForDay(startOfDay: Long, endOfDay: Long): Flow<List<Plan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: Plan): Long

    @Update
    suspend fun updatePlan(plan: Plan)

    @Delete
    suspend fun deletePlan(plan: Plan)

    @Query("SELECT * FROM plans WHERE id = :id")
    suspend fun getPlanById(id: Int): Plan?

    @Query("DELETE FROM plans")
    suspend fun deleteAllPlans()
}

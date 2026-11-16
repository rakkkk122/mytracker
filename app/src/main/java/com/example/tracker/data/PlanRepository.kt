package com.example.tracker.data

import kotlinx.coroutines.flow.Flow

interface PlanRepository {
    fun getAllPlansStream(): Flow<List<Plan>>
    fun getPlansForDayStream(startOfDay: Long, endOfDay: Long): Flow<List<Plan>>
    suspend fun getPlanById(id: Int): Plan?
    suspend fun insertPlan(plan: Plan): Long
    suspend fun deletePlan(plan: Plan)
    suspend fun updatePlan(plan: Plan)
    suspend fun deleteAllPlans()
}

class OfflinePlanRepository(private val planDao: PlanDao) : PlanRepository {
    override fun getAllPlansStream(): Flow<List<Plan>> = planDao.getAllPlans()
    override fun getPlansForDayStream(startOfDay: Long, endOfDay: Long): Flow<List<Plan>> = planDao.getPlansForDay(startOfDay, endOfDay)
    override suspend fun getPlanById(id: Int): Plan? = planDao.getPlanById(id)
    override suspend fun insertPlan(plan: Plan): Long = planDao.insertPlan(plan)
    override suspend fun deletePlan(plan: Plan) = planDao.deletePlan(plan)
    override suspend fun updatePlan(plan: Plan) = planDao.updatePlan(plan)
    override suspend fun deleteAllPlans() = planDao.deleteAllPlans()
}

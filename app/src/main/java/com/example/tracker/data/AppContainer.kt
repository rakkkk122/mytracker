package com.example.tracker.data

import android.content.Context

interface AppContainer {
    val planRepository: PlanRepository
    val expenseRepository: ExpenseRepository
    val dailyCheckInRepository: DailyCheckInRepository
}

class AppDataContainer(private val context: Context) : AppContainer {
    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    override val planRepository: PlanRepository by lazy {
        OfflinePlanRepository(database.planDao())
    }
    override val expenseRepository: ExpenseRepository by lazy {
        OfflineExpenseRepository(database.expenseDao())
    }
    override val dailyCheckInRepository: DailyCheckInRepository by lazy {
        OfflineDailyCheckInRepository(database.dailyCheckInDao())
    }
}

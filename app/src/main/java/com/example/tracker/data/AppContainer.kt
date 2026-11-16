package com.example.tracker.data

import android.content.Context

interface AppContainer {
    val planRepository: PlanRepository
    val expenseRepository: ExpenseRepository
}

class AppDataContainer(private val context: Context) : AppContainer {
    override val planRepository: PlanRepository by lazy {
        OfflinePlanRepository(AppDatabase.getDatabase(context).planDao())
    }
    override val expenseRepository: ExpenseRepository by lazy {
        OfflineExpenseRepository(AppDatabase.getDatabase(context).expenseDao())
    }
}

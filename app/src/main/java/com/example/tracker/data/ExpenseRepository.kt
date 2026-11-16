package com.example.tracker.data

import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getAllExpensesStream(): Flow<List<Expense>>
    fun getExpensesForDayStream(startOfDay: Long, endOfDay: Long): Flow<List<Expense>>
    suspend fun getExpenseById(id: Int): Expense?
    suspend fun insertExpense(expense: Expense): Long
    suspend fun deleteExpense(expense: Expense)
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteAllExpenses()
}

class OfflineExpenseRepository(private val expenseDao: ExpenseDao) : ExpenseRepository {
    override fun getAllExpensesStream(): Flow<List<Expense>> = expenseDao.getAllExpenses()
    override fun getExpensesForDayStream(startOfDay: Long, endOfDay: Long): Flow<List<Expense>> =
        expenseDao.getExpensesForDay(startOfDay, endOfDay)
    override suspend fun getExpenseById(id: Int): Expense? = expenseDao.getExpenseById(id)
    override suspend fun insertExpense(expense: Expense): Long = expenseDao.insertExpense(expense)
    override suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)
    override suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)
    override suspend fun deleteAllExpenses() = expenseDao.deleteAllExpenses()
}

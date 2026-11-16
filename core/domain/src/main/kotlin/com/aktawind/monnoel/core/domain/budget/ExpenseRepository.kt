package com.aktawind.monnoel.core.domain.budget

import com.aktawind.monnoel.core.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun observeExpenses(editionId: String): Flow<List<Expense>>

    fun observeAllExpenses(): Flow<List<Expense>>

    suspend fun getExpense(id: String): Expense?

    suspend fun saveExpense(expense: Expense)

    suspend fun deleteExpense(id: String)
}

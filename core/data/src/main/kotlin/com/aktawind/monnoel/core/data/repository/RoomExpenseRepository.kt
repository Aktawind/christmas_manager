package com.aktawind.monnoel.core.data.repository

import com.aktawind.monnoel.core.database.dao.ExpenseDao
import com.aktawind.monnoel.core.database.entity.asEntity
import com.aktawind.monnoel.core.database.entity.asModel
import com.aktawind.monnoel.core.domain.budget.ExpenseRepository
import com.aktawind.monnoel.core.model.Expense
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class RoomExpenseRepository @Inject constructor(private val dao: ExpenseDao) : ExpenseRepository {
    override fun observeExpenses(editionId: String) = dao.observeExpenses(editionId).map { l -> l.map { it.asModel() } }

    override fun observeAllExpenses() = dao.observeAllExpenses().map { l -> l.map { it.asModel() } }

    override suspend fun getExpense(id: String) = dao.get(id)?.asModel()

    override suspend fun saveExpense(expense: Expense) = dao.upsert(expense.asEntity())

    override suspend fun deleteExpense(id: String) = dao.delete(id)
}

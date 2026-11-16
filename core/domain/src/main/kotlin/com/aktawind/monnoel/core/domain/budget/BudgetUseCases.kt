package com.aktawind.monnoel.core.domain.budget

import com.aktawind.monnoel.core.common.AppError
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.IdGenerator
import com.aktawind.monnoel.core.common.TimeProvider
import com.aktawind.monnoel.core.domain.gifts.EditionGuard
import com.aktawind.monnoel.core.domain.repository.EditionRepository
import com.aktawind.monnoel.core.model.Expense
import com.aktawind.monnoel.core.model.ExpenseCategory
import javax.inject.Inject

data class ExpenseDraft(
    val title: String,
    val amountCents: Long,
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val paid: Boolean = false,
    val notes: String = "",
)

class SaveExpenseUseCase @Inject constructor(
    private val expenses: ExpenseRepository,
    private val guard: EditionGuard,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(id: String?, editionId: String, draft: ExpenseDraft): AppResult<Expense> {
        if (draft.title.isBlank() || draft.amountCents < 0) return AppResult.Failure(AppError.InvalidInput)
        val existing = id?.let { expenses.getExpense(it) }
        guard.check(existing?.editionId ?: editionId)?.let { return AppResult.Failure(it) }
        val now = time.now()
        val expense = Expense(
            id = existing?.id ?: ids.newId(),
            editionId = existing?.editionId ?: editionId,
            title = draft.title.trim(),
            amountCents = draft.amountCents,
            category = draft.category,
            paid = draft.paid,
            notes = draft.notes.trim(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        expenses.saveExpense(expense)
        return AppResult.Success(expense)
    }
}

class SetExpensePaidUseCase @Inject constructor(
    private val expenses: ExpenseRepository,
    private val guard: EditionGuard,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(id: String, paid: Boolean): AppResult<Unit> {
        val expense = expenses.getExpense(id) ?: return AppResult.Failure(AppError.NotFound)
        guard.check(expense.editionId)?.let { return AppResult.Failure(it) }
        expenses.saveExpense(expense.copy(paid = paid, updatedAt = time.now()))
        return AppResult.Success(Unit)
    }
}

class DeleteExpenseUseCase @Inject constructor(private val expenses: ExpenseRepository, private val guard: EditionGuard) {
    suspend operator fun invoke(id: String): AppResult<Unit> {
        val expense = expenses.getExpense(id) ?: return AppResult.Failure(AppError.NotFound)
        guard.check(expense.editionId)?.let { return AppResult.Failure(it) }
        expenses.deleteExpense(id)
        return AppResult.Success(Unit)
    }
}

/** Fixe (ou retire, avec null) le budget global d'un Noël. */
class SetEditionBudgetUseCase @Inject constructor(
    private val editions: EditionRepository,
    private val guard: EditionGuard,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(editionId: String, amountCents: Long?): AppResult<Unit> {
        if ((amountCents ?: 0) < 0) return AppResult.Failure(AppError.InvalidInput)
        val edition = editions.getById(editionId) ?: return AppResult.Failure(AppError.NotFound)
        guard.check(editionId)?.let { return AppResult.Failure(it) }
        editions.update(edition.copy(plannedBudgetCents = amountCents, updatedAt = time.now()))
        return AppResult.Success(Unit)
    }
}

package com.aktawind.monnoel.core.domain.budget

import com.aktawind.monnoel.core.common.AppError
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.edition.FakeEditionRepository
import com.aktawind.monnoel.core.domain.edition.FixedTime
import com.aktawind.monnoel.core.domain.edition.SequentialIds
import com.aktawind.monnoel.core.domain.gifts.EditionGuard
import com.aktawind.monnoel.core.model.AdventBox
import com.aktawind.monnoel.core.model.AdventCalendar
import com.aktawind.monnoel.core.model.AdventSupply
import com.aktawind.monnoel.core.model.BoxStatus
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.model.EditionStatus
import com.aktawind.monnoel.core.model.Expense
import com.aktawind.monnoel.core.model.ExpenseCategory
import com.aktawind.monnoel.core.model.GiftStatus
import com.aktawind.monnoel.core.model.Meal
import com.aktawind.monnoel.core.model.MealType
import com.aktawind.monnoel.core.model.PersonBudget
import com.aktawind.monnoel.core.model.PlannedGift
import com.aktawind.monnoel.core.model.defaultCourses
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest

class FakeExpenses : ExpenseRepository {
    val items = MutableStateFlow<List<Expense>>(emptyList())

    override fun observeExpenses(editionId: String) = items.map { l -> l.filter { it.editionId == editionId } }

    override fun observeAllExpenses() = items

    override suspend fun getExpense(id: String) = items.value.firstOrNull { it.id == id }

    override suspend fun saveExpense(expense: Expense) {
        items.value = items.value.filterNot { it.id == expense.id } + expense
    }

    override suspend fun deleteExpense(id: String) {
        items.value = items.value.filterNot { it.id == id }
    }
}

class YearBudgetTest {
    private val t = Instant.EPOCH
    private val edition = ChristmasEdition("e", 2026, EditionStatus.ACTIVE, 60_000, t, t)

    private fun gift(price: Long, status: GiftStatus, person: String = "lea") =
        PlannedGift("g$price", "e", person, "Léa", null, "Cadeau", "", null, price, null, "", "", "", "", null, status, t, t)

    private fun meal(cost: Long, date: LocalDate?) = Meal(
        "m$cost", "e", "Repas", MealType.CHRISTMAS_EVE, date, "", emptySet(), emptyList(), cost, "",
        MealType.CHRISTMAS_EVE.defaultCourses(), t, t,
    )

    @Test
    fun `le budget se calcule a partir des modules`() {
        val source = BudgetSource(
            gifts = listOf(gift(3_000, GiftStatus.TO_BUY), gift(2_000, GiftStatus.ORDERED), gift(1_000, GiftStatus.WRAPPED)),
            envelopes = listOf(PersonBudget("e", "lea", 5_000)),
            calendars = listOf(AdventCalendar("c", "e", "Tom", emptySet(), "", null, "", 4_000, "", 24, t, t)),
            boxes = listOf(
                AdventBox("b1", "c", 1, null, "Dino", null, 500, "", "", BoxStatus.READY, "", null),
                AdventBox("b2", "c", 2, null, "Livre", null, 800, "", "", BoxStatus.TO_FIND, "", null),
            ),
            supplies = listOf(AdventSupply("s", "c", "Sachets", 24, 600, false, 0)),
            meals = listOf(meal(12_000, LocalDate.of(2026, 12, 24)), meal(8_000, LocalDate.of(2026, 11, 1))),
            expenses = listOf(
                Expense("x1", "e", "Sapin", 4_500, ExpenseCategory.TREE, true, "", t, t),
                Expense("x2", "e", "Guirlandes", 1_500, ExpenseCategory.DECORATION, false, "", t, t),
                Expense("x3", "autre", "Ailleurs", 9_900, ExpenseCategory.OTHER, true, "", t, t),
            ),
        )
        val budget = computeYearBudget(edition, source, LocalDate.of(2026, 11, 16))
        val gifts = budget.post(BudgetPost.GIFTS)
        assertEquals(6_000, gifts.plannedCents)
        assertEquals(3_000, gifts.spentCents)
        assertEquals(5_000, gifts.budgetCents)
        assertEquals(1_000, gifts.overCents)
        val advent = budget.post(BudgetPost.ADVENT)
        assertEquals(1_900, advent.plannedCents)
        assertEquals(500, advent.spentCents)
        assertNull(advent.overCents)
        // Le repas du 24 décembre n'est pas encore « dépensé ».
        assertEquals(20_000, budget.post(BudgetPost.MEALS).plannedCents)
        assertEquals(8_000, budget.post(BudgetPost.MEALS).spentCents)
        assertEquals(6_000, budget.post(BudgetPost.OTHER).plannedCents)
        assertEquals(4_500, budget.post(BudgetPost.OTHER).spentCents)
        assertEquals(33_900, budget.plannedCents)
        assertEquals(26_100, budget.marginCents)
        // Le jour du repas, il compte comme dépensé.
        assertEquals(20_000, computeYearBudget(edition, source, LocalDate.of(2026, 12, 24)).post(BudgetPost.MEALS).spentCents)
    }

    @Test
    fun `autres depenses et budget global`() = runTest {
        val editions = FakeEditionRepository()
        editions.insert(edition)
        editions.insert(ChristmasEdition("old", 2025, EditionStatus.ARCHIVED, null, t, t))
        val guard = EditionGuard(editions)
        val expenses = FakeExpenses()
        val save = SaveExpenseUseCase(expenses, guard, FixedTime(), SequentialIds())
        val sapin = (save(null, "e", ExpenseDraft("Sapin", 4_500, ExpenseCategory.TREE)) as AppResult.Success).value
        SetExpensePaidUseCase(expenses, guard, FixedTime())(sapin.id, true)
        assertEquals(true, expenses.getExpense(sapin.id)?.paid)
        assertEquals(AppResult.Failure(AppError.InvalidInput), save(null, "e", ExpenseDraft(" ", 100)))
        assertEquals(AppResult.Failure(AppError.ReadOnly), save(null, "old", ExpenseDraft("Sapin", 100)))

        val setBudget = SetEditionBudgetUseCase(editions, guard, FixedTime())
        setBudget("e", 80_000)
        assertEquals(80_000, editions.getById("e")?.plannedBudgetCents)
        setBudget("e", null)
        assertNull(editions.getById("e")?.plannedBudgetCents)
    }
}

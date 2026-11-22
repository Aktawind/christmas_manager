package com.aktawind.monnoel.core.domain.budget

import com.aktawind.monnoel.core.common.TimeProvider
import com.aktawind.monnoel.core.domain.advent.AdventRepository
import com.aktawind.monnoel.core.domain.edition.ObserveEditionsUseCase
import com.aktawind.monnoel.core.domain.meals.MealRepository
import com.aktawind.monnoel.core.domain.repository.PlannedGiftRepository
import com.aktawind.monnoel.core.domain.traditions.CraftRepository
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.model.Expense
import com.aktawind.monnoel.core.model.PersonBudget
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/** Le budget du Noël affiché, ses autres dépenses, et le budget de chaque année (comparaison). */
data class BudgetOverview(
    val edition: ChristmasEdition?,
    val current: YearBudget?,
    val expenses: List<Expense>,
    /** Toutes les années, de la plus ancienne à la plus récente. */
    val history: List<YearBudget>,
)

class ObserveBudgetsUseCase @Inject constructor(
    private val observeEditions: ObserveEditionsUseCase,
    private val gifts: PlannedGiftRepository,
    private val advent: AdventRepository,
    private val meals: MealRepository,
    private val expenses: ExpenseRepository,
    private val crafts: CraftRepository,
    private val time: TimeProvider,
) {
    operator fun invoke(): Flow<BudgetOverview> {
        val modules = combine(
            gifts.observeAllGifts(),
            combine(advent.observeAllCalendars(), advent.observeAllBoxes(), advent.observeAllSupplies()) { c, b, s -> Triple(c, b, s) },
            meals.observeAllMeals(),
            expenses.observeAllExpenses(),
            combine(crafts.observeYears(), crafts.observeSupplies(), crafts.observeBought()) { y, s, b -> Triple(y, s, b) },
        ) { g, (c, b, s), m, e, (cy, cs, cb) ->
            BudgetSource(
                gifts = g, calendars = c, boxes = b, supplies = s, meals = m, expenses = e,
                craftYears = cy, craftSupplies = cs, craftBought = cb,
            )
        }
        return observeEditions().flatMapLatest { editions ->
            val current = editions.current
            val envelopes = current?.let { gifts.observeBudgets(it.id) } ?: flowOf(emptyList<PersonBudget>())
            combine(modules, envelopes) { source, env ->
                val today = time.today()
                val full = source.copy(envelopes = env)
                BudgetOverview(
                    edition = current,
                    current = current?.let { computeYearBudget(it, full, today) },
                    expenses = source.expenses.filter { it.editionId == current?.id },
                    history = editions.editions.sortedBy { it.year }.map { computeYearBudget(it, source, today) },
                )
            }
        }
    }
}

package com.aktawind.monnoel.core.domain.budget

import com.aktawind.monnoel.core.domain.advent.summarizeCalendar
import com.aktawind.monnoel.core.domain.gifts.computeGiftBudget
import com.aktawind.monnoel.core.model.AdventBox
import com.aktawind.monnoel.core.model.AdventCalendar
import com.aktawind.monnoel.core.model.AdventSupply
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.model.CraftSupply
import com.aktawind.monnoel.core.model.CraftYear
import com.aktawind.monnoel.core.model.Expense
import com.aktawind.monnoel.core.model.Meal
import com.aktawind.monnoel.core.model.PersonBudget
import com.aktawind.monnoel.core.model.PlannedGift
import java.time.LocalDate

/** Les grands postes du budget de Noël. */
enum class BudgetPost { GIFTS, ADVENT, MEALS, CRAFTS, OTHER }

/** Un poste : prévu, déjà dépensé, et le budget qu'on s'est fixé (s'il y en a un). */
data class PostBudget(val post: BudgetPost, val plannedCents: Long, val spentCents: Long, val budgetCents: Long?) {
    /** De combien le prévu dépasse le budget (null si pas de budget ou pas de dépassement). */
    val overCents: Long? get() = budgetCents?.let { plannedCents - it }?.takeIf { it > 0 }
}

/** Le budget d'un Noël, calculé à partir des autres modules (aucune double saisie). */
data class YearBudget(val editionId: String, val year: Int, val globalBudgetCents: Long?, val posts: List<PostBudget>) {
    val plannedCents: Long get() = posts.sumOf { it.plannedCents }
    val spentCents: Long get() = posts.sumOf { it.spentCents }

    /** Marge restante dans le budget global par rapport au prévu (négative = dépassement). */
    val marginCents: Long? get() = globalBudgetCents?.let { it - plannedCents }

    fun post(post: BudgetPost): PostBudget = posts.first { it.post == post }
}

/** Tout ce qui sert à calculer un budget annuel (pour une année, ou toutes les années). */
data class BudgetSource(
    val gifts: List<PlannedGift> = emptyList(),
    val envelopes: List<PersonBudget> = emptyList(),
    val calendars: List<AdventCalendar> = emptyList(),
    val boxes: List<AdventBox> = emptyList(),
    val supplies: List<AdventSupply> = emptyList(),
    val meals: List<Meal> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val craftYears: List<CraftYear> = emptyList(),
    val craftSupplies: List<CraftSupply> = emptyList(),
    /** Fournitures d'atelier achetées : paires (édition, fourniture). */
    val craftBought: Set<Pair<String, String>> = emptySet(),
)

/**
 * Calcule le budget d'un Noël :
 * - cadeaux : prévu = prix ; dépensé = cadeaux commandés ou achetés ; budget = enveloppes ;
 * - calendriers : cases et fournitures (comme sur l'écran du calendrier) ; budget = budgets des calendriers ;
 * - repas : coût estimé ; dépensé une fois la date du repas arrivée ;
 * - atelier : fournitures des bricolages au programme de l'année ; dépensé = fournitures cochées ;
 * - autres dépenses : montant ; dépensé quand c'est payé.
 */
fun computeYearBudget(edition: ChristmasEdition, source: BudgetSource, today: LocalDate): YearBudget {
    val gifts =
        computeGiftBudget(source.gifts.filter { it.editionId == edition.id }, source.envelopes.filter { it.editionId == edition.id })
    val calendars = source.calendars.filter { it.editionId == edition.id }.map { calendar ->
        // Tout prêt : son prix, dépensé une fois acheté.
        if (calendar.isReadyMade) {
            return@map com.aktawind.monnoel.core.domain.advent.CalendarSummary(
                boxCount = 0,
                ready = 0,
                toPrepare = 0,
                toFind = 0,
                plannedCents = calendar.priceCents ?: 0,
                spentCents = if (calendar.bought) calendar.priceCents ?: 0 else 0,
                budgetCents = null,
            )
        }
        summarizeCalendar(
            calendar.boxCount,
            calendar.budgetCents,
            source.boxes.filter { it.calendarId == calendar.id },
            source.supplies.filter { it.calendarId == calendar.id },
        )
    }
    val calendarBudgets = calendars.mapNotNull { it.budgetCents }
    val meals = source.meals.filter { it.editionId == edition.id }
    val expenses = source.expenses.filter { it.editionId == edition.id }
    val craftIds = source.craftYears.filter { it.editionId == edition.id }.map { it.craftId }.toSet()
    val craftSupplies = source.craftSupplies.filter { it.craftId in craftIds && it.priceCents != null }
    return YearBudget(
        editionId = edition.id,
        year = edition.year,
        globalBudgetCents = edition.plannedBudgetCents,
        posts = listOf(
            PostBudget(BudgetPost.GIFTS, gifts.plannedCents, gifts.spentCents, gifts.envelopesCents.takeIf { it > 0 }),
            PostBudget(
                BudgetPost.ADVENT,
                calendars.sumOf { it.plannedCents },
                calendars.sumOf { it.spentCents },
                calendarBudgets.takeIf { it.isNotEmpty() }?.sum(),
            ),
            PostBudget(
                BudgetPost.MEALS,
                meals.sumOf { it.estimatedCostCents ?: 0 },
                meals.filter { meal -> meal.date?.let { !it.isAfter(today) } == true }.sumOf { it.estimatedCostCents ?: 0 },
                null,
            ),
            PostBudget(
                BudgetPost.CRAFTS,
                craftSupplies.sumOf { it.priceCents ?: 0 },
                craftSupplies.filter { (edition.id to it.id) in source.craftBought }.sumOf { it.priceCents ?: 0 },
                null,
            ),
            PostBudget(BudgetPost.OTHER, expenses.sumOf { it.amountCents }, expenses.filter { it.paid }.sumOf { it.amountCents }, null),
        ),
    )
}

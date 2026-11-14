package com.aktawind.monnoel.core.domain.advent

import com.aktawind.monnoel.core.model.AdventBox
import com.aktawind.monnoel.core.model.AdventSupply
import com.aktawind.monnoel.core.model.BoxStatus
import java.time.LocalDate
import java.time.Month

/** Avancement et budget d'un calendrier. */
data class CalendarSummary(
    val boxCount: Int,
    val ready: Int,
    val toPrepare: Int,
    val toFind: Int,
    /** Coût prévu : cases + fournitures. */
    val plannedCents: Long,
    /** Déjà dépensé : cases trouvées (à préparer ou prêtes) + fournitures achetées. */
    val spentCents: Long,
    val budgetCents: Long?,
) {
    val progress: Float get() = if (boxCount == 0) 0f else ready.toFloat() / boxCount
    val isOverBudget: Boolean get() = budgetCents != null && plannedCents > budgetCents
}

/**
 * Part du prix d'un achat groupé pour une case : le prix est réparti également entre les
 * cases reliées (arrondi au centime, le reste sur les premières cases).
 */
fun supplyShares(supply: AdventSupply, linkedBoxes: List<AdventBox>): Map<String, Long> {
    val price = supply.priceCents ?: return emptyMap()
    if (linkedBoxes.isEmpty()) return emptyMap()
    val sorted = linkedBoxes.sortedBy { it.number }
    val base = price / sorted.size
    val remainder = price % sorted.size
    return sorted.mapIndexed { index, box -> box.id to base + if (index < remainder) 1 else 0 }.toMap()
}

fun summarizeCalendar(
    boxCount: Int,
    budgetCents: Long?,
    boxes: List<AdventBox>,
    supplies: List<AdventSupply>,
): CalendarSummary {
    val found = boxes.filter { it.status != BoxStatus.TO_FIND }
    return CalendarSummary(
        boxCount = boxCount,
        ready = boxes.count { it.status == BoxStatus.READY },
        toPrepare = boxes.count { it.status == BoxStatus.TO_PREPARE },
        toFind = boxes.count { it.status == BoxStatus.TO_FIND },
        plannedCents = boxes.sumOf { it.priceCents ?: 0 } + supplies.sumOf { it.priceCents ?: 0 },
        spentCents = found.sumOf { it.priceCents ?: 0 } + supplies.filter { it.bought }.sumOf { it.priceCents ?: 0 },
        budgetCents = budgetCents,
    )
}

/** Numéro de la case à ouvrir aujourd'hui (1er décembre = case 1), ou null hors période. */
fun boxOfTheDay(today: LocalDate, editionYear: Int, boxCount: Int): Int? {
    if (today.year != editionYear || today.month != Month.DECEMBER) return null
    return today.dayOfMonth.takeIf { it <= boxCount }
}

const val DEFAULT_BOX_COUNT = 24
const val MAX_BOX_COUNT = 31

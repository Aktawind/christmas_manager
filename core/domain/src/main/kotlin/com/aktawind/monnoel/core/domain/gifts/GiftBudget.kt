package com.aktawind.monnoel.core.domain.gifts

import com.aktawind.monnoel.core.model.PersonBudget
import com.aktawind.monnoel.core.model.PlannedGift

/** Budget d'une personne pour un Noël. */
data class PersonGiftBudget(
    val personId: String,
    val plannedCents: Long,
    val spentCents: Long,
    val envelopeCents: Long?,
    val giftCount: Int,
) {
    /** Part de l'enveloppe utilisée par le prévu (0..n), ou null sans enveloppe. */
    val envelopeRatio: Float?
        get() = envelopeCents?.takeIf { it > 0 }?.let { plannedCents.toFloat() / it }

    val isOverEnvelope: Boolean get() = envelopeCents != null && plannedCents > envelopeCents
}

data class GiftBudgetSummary(
    val plannedCents: Long,
    val spentCents: Long,
    val envelopesCents: Long,
    val byPerson: Map<String, PersonGiftBudget>,
)

/**
 * Calcule le budget des cadeaux d'un Noël, à partir des cadeaux eux-mêmes (aucune
 * double saisie). Prévu = somme des prix ; dépensé = cadeaux commandés ou achetés.
 */
fun computeGiftBudget(gifts: List<PlannedGift>, budgets: List<PersonBudget>): GiftBudgetSummary {
    val envelopes = budgets.associate { it.personId to it.amountCents }
    val personIds = gifts.mapNotNull { it.personId }.toSet() + envelopes.keys
    val byPerson = personIds.associateWith { personId ->
        val own = gifts.filter { it.personId == personId }
        PersonGiftBudget(
            personId = personId,
            plannedCents = own.sumOf { it.priceCents ?: 0 },
            spentCents = own.filter { it.status.isPaid }.sumOf { it.priceCents ?: 0 },
            envelopeCents = envelopes[personId],
            giftCount = own.size,
        )
    }
    return GiftBudgetSummary(
        plannedCents = gifts.sumOf { it.priceCents ?: 0 },
        spentCents = gifts.filter { it.status.isPaid }.sumOf { it.priceCents ?: 0 },
        envelopesCents = envelopes.values.sum(),
        byPerson = byPerson,
    )
}

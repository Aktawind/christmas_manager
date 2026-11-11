package com.aktawind.monnoel.core.model

import java.time.Instant

/**
 * Le Noël d'une année : racine de tous les éléments annuels (cadeaux, calendriers, repas…).
 */
data class ChristmasEdition(
    val id: String,
    val year: Int,
    val status: EditionStatus,
    /** Budget global prévu, en centimes. Utilisé à partir de la v0.7. */
    val plannedBudgetCents: Long?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val isArchived: Boolean get() = status == EditionStatus.ARCHIVED

    /** Une année archivée est en lecture seule (sauf journal et souvenirs). */
    val isReadOnly: Boolean get() = isArchived
}

enum class EditionStatus { ACTIVE, ARCHIVED }

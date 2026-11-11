package com.aktawind.monnoel.core.model

import java.time.Instant
import java.time.LocalDate

/** Une personne à qui l'on offre des cadeaux. Indépendante des années. */
data class Person(
    val id: String,
    val firstName: String,
    val lastName: String,
    /** Chemin relatif de la photo dans le dossier des médias. */
    val photoPath: String?,
    val interests: String,
    val notes: String,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val displayName: String get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
    val initials: String
        get() = listOf(firstName, lastName).mapNotNull { it.trim().firstOrNull()?.uppercaseChar() }.joinToString("")
}

/** Une ligne de la liste de souhaits d'une personne. */
data class WishItem(val id: String, val personId: String, val text: String, val done: Boolean, val sortOrder: Int)

data class GiftCategory(val id: String, val name: String, val sortOrder: Int)

/** Une idée de cadeau, conservée d'une année à l'autre. */
data class GiftIdea(
    val id: String,
    val title: String,
    val description: String,
    val photoPath: String?,
    val estimatedPriceCents: Long?,
    val url: String,
    val shop: String,
    val notes: String,
    val categoryId: String?,
    val personIds: Set<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
)

enum class GiftStatus {
    TO_BUY,
    ORDERED,
    RECEIVED,
    TO_WRAP,
    WRAPPED,
    GIVEN,
    ;

    /** Le cadeau est payé (commandé ou acheté) : il compte dans le « dépensé ». */
    val isPaid: Boolean get() = this != TO_BUY

    /** Commandé mais pas encore arrivé. */
    val isAwaitingDelivery: Boolean get() = this == ORDERED
}

/**
 * Un cadeau concret, pour une personne et un Noël. Les champs affichés sont copiés de
 * l'idée au moment de la création (historique figé, ADR 0003).
 */
data class PlannedGift(
    val id: String,
    val editionId: String,
    val personId: String?,
    /** Nom du destinataire au moment de la création (archives). */
    val recipientName: String,
    val ideaId: String?,
    val title: String,
    val description: String,
    val photoPath: String?,
    val priceCents: Long?,
    val purchaseDate: LocalDate?,
    val shop: String,
    val orderUrl: String,
    val trackingNumber: String,
    val notes: String,
    val categoryId: String?,
    val status: GiftStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** Enveloppe budgétaire prévue pour une personne, pour un Noël. */
data class PersonBudget(val editionId: String, val personId: String, val amountCents: Long)

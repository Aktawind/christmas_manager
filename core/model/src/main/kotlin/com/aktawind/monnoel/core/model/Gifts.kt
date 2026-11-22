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

/**
 * Une idée « soufflée » : pour un Noël, une idée de cadeau pour [recipientId], confiée à
 * quelqu'un (une personne de l'application, ou un nom libre) qui pourra l'offrir.
 */
data class WhisperedIdea(
    val id: String,
    val editionId: String,
    /** Pour qui est l'idée. */
    val recipientId: String,
    val title: String,
    val priceCents: Long?,
    val url: String,
    /** À qui on l'a soufflée : une personne de l'application… */
    val givenToPersonId: String?,
    /** …ou un nom libre (« Collègues »). */
    val givenToName: String,
    /** Le cadeau a bien été reçu. */
    val received: Boolean,
    val notes: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val photoPath: String? = null,
)

enum class IdeaRequestKind {
    /** Quelqu'un attend que je lui envoie des idées pour la personne de la fiche. */
    TO_SEND,

    /** J'attends des idées de la part de la personne de la fiche (pour elle-même ou quelqu'un d'autre). */
    TO_RECEIVE,
}

/**
 * Une demande d'idées pour un Noël, rangée dans la fiche de [personId]. L'autre personne est
 * celle qui attend (TO_SEND) ou celle pour qui sont les idées (TO_RECEIVE). Son avancement est
 * celui de la tâche créée dans Ma liste ([taskId]).
 */
data class IdeaRequest(
    val id: String,
    val editionId: String,
    val kind: IdeaRequestKind,
    val personId: String,
    val otherPersonId: String?,
    val otherName: String,
    val taskId: String?,
    val createdAt: Instant,
)

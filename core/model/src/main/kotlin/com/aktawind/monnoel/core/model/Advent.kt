package com.aktawind.monnoel.core.model

import java.time.Instant

/** Un calendrier de l'Avent d'un Noël. */
data class AdventCalendar(
    val id: String,
    val editionId: String,
    val name: String,
    val personIds: Set<String>,
    val description: String,
    val coverPhotoPath: String?,
    val theme: String,
    /** Budget prévu pour ce calendrier, en centimes. */
    val budgetCents: Long?,
    val notes: String,
    val boxCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

enum class SurpriseType {
    BOUGHT,
    HANDMADE,
    TREAT,
    ACTIVITY,
    MESSAGE,
    EXPERIENCE,
    PHOTO,
    GAME,
}

/** État de préparation d'une case : simple, sans pression. */
enum class BoxStatus {
    TO_FIND,
    TO_PREPARE,
    READY,
}

/** Une case d'un calendrier (numéro 1..boxCount). */
data class AdventBox(
    val id: String,
    val calendarId: String,
    val number: Int,
    val type: SurpriseType?,
    val title: String,
    val photoPath: String?,
    val priceCents: Long?,
    val supplier: String,
    val url: String,
    val status: BoxStatus,
    val notes: String,
    /** Achat groupé (fourniture) dont le prix est réparti sur plusieurs cases. */
    val supplyId: String?,
) {
    val isEmpty: Boolean get() = type == null && title.isBlank() && photoPath == null && priceCents == null
}

/** Fourniture ou achat groupé d'un calendrier. */
data class AdventSupply(
    val id: String,
    val calendarId: String,
    val name: String,
    val quantity: Int,
    val priceCents: Long?,
    val bought: Boolean,
    val sortOrder: Int,
)

/** Modèle personnel de calendrier (réutilisable, indépendant des années). */
data class AdventTemplate(
    val id: String,
    val name: String,
    val theme: String,
    val description: String,
    val boxCount: Int,
    val createdAt: Instant,
)

data class AdventTemplateBox(
    val id: String,
    val templateId: String,
    val number: Int,
    val type: SurpriseType?,
    val title: String,
    val priceCents: Long?,
    val supplier: String,
    val url: String,
    val notes: String,
)

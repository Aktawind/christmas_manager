package com.aktawind.monnoel.core.model

import java.time.Instant

enum class TraditionCategory { HOME, OUTINGS, COOKING, FILMS_BOOKS, FAMILY, OTHER }

enum class TraditionPeriod { ADVENT, EVE, DAY, AFTER }

/** Une tradition de la famille, gardée d'une année à l'autre. */
data class Tradition(
    val id: String,
    val name: String,
    val photoPath: String?,
    val category: TraditionCategory,
    val period: TraditionPeriod,
    val personIds: Set<String>,
    val notes: String,
    val favorite: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** Une tradition au programme d'un Noël : faite ou non, avec un souvenir (photo, petit mot). */
data class TraditionYear(
    val id: String,
    val editionId: String,
    val traditionId: String,
    val done: Boolean,
    val memoryPhotoPath: String?,
    val memoryNote: String,
    val createdAt: Instant,
)

enum class InspirationKind { FILM, CARTOON, BOOK, MUSIC, OUTING, ACTIVITY }

/** Une inspiration (film, livre, sortie…), à piocher chaque année. */
data class Inspiration(
    val id: String,
    val kind: InspirationKind,
    val title: String,
    val photoPath: String?,
    val url: String,
    val notes: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

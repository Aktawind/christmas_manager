package com.aktawind.monnoel.core.model

import java.time.Instant
import java.time.LocalDate

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
    /** Date prévue cette année (facultative). */
    val date: LocalDate? = null,
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

enum class CraftCategory { DECORATION, HANDMADE_GIFT, WRAPPING, CARDS, KIDS, OTHER }

/** Avancement d'un bricolage pour un Noël (sans ligne = simple idée). */
enum class CraftStatus { PLANNED, IN_PROGRESS, DONE }

/** Un bricolage (DIY), gardé d'une année à l'autre ; le tutoriel reste sur son site. */
data class Craft(
    val id: String,
    val title: String,
    val photoPath: String?,
    val category: CraftCategory,
    val tutorialUrl: String,
    val notes: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** Une fourniture d'un bricolage (prix facultatif). */
data class CraftSupply(val id: String, val craftId: String, val name: String, val priceCents: Long?, val sortOrder: Int)

/** Un bricolage au programme d'un Noël : où il en est, la photo du résultat. */
data class CraftYear(
    val id: String,
    val editionId: String,
    val craftId: String,
    val status: CraftStatus,
    val resultPhotoPath: String?,
    val createdAt: Instant,
)

enum class DecorPlace { TREE, TABLE, ENTRANCE, WINDOWS, OUTDOOR, OTHER }

/** Une inspiration de décoration (photo, lien, commentaire). */
data class DecorIdea(
    val id: String,
    val title: String,
    val photoPath: String?,
    val url: String,
    val comment: String,
    val place: DecorPlace,
    val favorite: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

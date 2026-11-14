package com.aktawind.monnoel.core.backup.format

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Version du format de sauvegarde (ADR 0004). À incrémenter à chaque changement de
 * structure de `data.json`, en ajoutant la conversion dans [BackupMigrations].
 */
const val BACKUP_FORMAT_VERSION = 3

const val MANIFEST_ENTRY = "manifest.json"
const val DATA_ENTRY = "data.json"
const val MEDIA_PREFIX = "media/"

@Serializable
data class BackupManifest(
    val app: String = APP_ID,
    val formatVersion: Int,
    val appVersion: String,
    val appVersionCode: Long,
    val exportedAt: String,
    val counts: Map<String, Int>,
    val mediaFiles: Int,
    val dataSha256: String,
) {
    companion object {
        const val APP_ID = "mon-noel"
    }
}

/** Contenu de `data.json` : une liste par table. Les DTO sont indépendants des entités Room. */
@Serializable
data class BackupData(
    val editions: List<EditionDto> = emptyList(),
    val giftCategories: List<GiftCategoryDto> = emptyList(),
    val persons: List<PersonDto> = emptyList(),
    val giftIdeas: List<GiftIdeaDto> = emptyList(),
    val ideaPersons: List<IdeaPersonDto> = emptyList(),
    val plannedGifts: List<PlannedGiftDto> = emptyList(),
    val personBudgets: List<PersonBudgetDto> = emptyList(),
    val adventCalendars: List<AdventCalendarDto> = emptyList(),
    val adventCalendarPersons: List<AdventCalendarPersonDto> = emptyList(),
    val adventSupplies: List<AdventSupplyDto> = emptyList(),
    val adventBoxes: List<AdventBoxDto> = emptyList(),
    val adventTemplates: List<AdventTemplateDto> = emptyList(),
    val adventTemplateBoxes: List<AdventTemplateBoxDto> = emptyList(),
) {
    fun counts(): Map<String, Int> = mapOf(
        "editions" to editions.size,
        "giftCategories" to giftCategories.size,
        "persons" to persons.size,
        "giftIdeas" to giftIdeas.size,
        "plannedGifts" to plannedGifts.size,
        "personBudgets" to personBudgets.size,
        "adventCalendars" to adventCalendars.size,
        "adventBoxes" to adventBoxes.size,
        "adventTemplates" to adventTemplates.size,
    )
}

@Serializable
data class EditionDto(
    val id: String,
    val year: Int,
    val status: String,
    val plannedBudgetCents: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class GiftCategoryDto(val id: String, val name: String, val sortOrder: Int)

@Serializable
data class PersonDto(
    val id: String,
    val firstName: String,
    val lastName: String,
    val photoPath: String? = null,
    val interests: String = "",
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class GiftIdeaDto(
    val id: String,
    val title: String,
    val description: String = "",
    val photoPath: String? = null,
    val estimatedPriceCents: Long? = null,
    val url: String = "",
    val shop: String = "",
    val notes: String = "",
    val categoryId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class IdeaPersonDto(val ideaId: String, val personId: String)

@Serializable
data class PlannedGiftDto(
    val id: String,
    val editionId: String,
    val personId: String? = null,
    val recipientName: String = "",
    val ideaId: String? = null,
    val title: String,
    val description: String = "",
    val photoPath: String? = null,
    val priceCents: Long? = null,
    /** Jour d'achat (epoch day). */
    val purchaseDate: Long? = null,
    val shop: String = "",
    val orderUrl: String = "",
    val trackingNumber: String = "",
    val notes: String = "",
    val categoryId: String? = null,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class PersonBudgetDto(val editionId: String, val personId: String, val amountCents: Long)

@Serializable
data class AdventCalendarDto(
    val id: String,
    val editionId: String,
    val name: String,
    val description: String = "",
    val coverPhotoPath: String? = null,
    val theme: String = "",
    val budgetCents: Long? = null,
    val notes: String = "",
    val boxCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class AdventCalendarPersonDto(val calendarId: String, val personId: String)

@Serializable
data class AdventSupplyDto(
    val id: String,
    val calendarId: String,
    val name: String,
    val quantity: Int = 1,
    val priceCents: Long? = null,
    val bought: Boolean = false,
    val sortOrder: Int = 0,
)

@Serializable
data class AdventBoxDto(
    val id: String,
    val calendarId: String,
    val number: Int,
    val type: String? = null,
    val title: String = "",
    val photoPath: String? = null,
    val priceCents: Long? = null,
    val supplier: String = "",
    val url: String = "",
    val status: String,
    val notes: String = "",
    val supplyId: String? = null,
)

@Serializable
data class AdventTemplateDto(
    val id: String,
    val name: String,
    val theme: String = "",
    val description: String = "",
    val boxCount: Int,
    val createdAt: Long,
)

@Serializable
data class AdventTemplateBoxDto(
    val id: String,
    val templateId: String,
    val number: Int,
    val type: String? = null,
    val title: String = "",
    val priceCents: Long? = null,
    val supplier: String = "",
    val url: String = "",
    val notes: String = "",
)

internal val backupJson = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
}

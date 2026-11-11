package com.aktawind.monnoel.core.backup.data

import androidx.room.withTransaction
import com.aktawind.monnoel.core.backup.format.BackupData
import com.aktawind.monnoel.core.backup.format.EditionDto
import com.aktawind.monnoel.core.backup.format.GiftCategoryDto
import com.aktawind.monnoel.core.backup.format.GiftIdeaDto
import com.aktawind.monnoel.core.backup.format.IdeaPersonDto
import com.aktawind.monnoel.core.backup.format.PersonBudgetDto
import com.aktawind.monnoel.core.backup.format.PersonDto
import com.aktawind.monnoel.core.backup.format.PlannedGiftDto
import com.aktawind.monnoel.core.backup.format.WishItemDto
import com.aktawind.monnoel.core.database.MonNoelDatabase
import com.aktawind.monnoel.core.database.entity.EditionEntity
import com.aktawind.monnoel.core.database.entity.GiftCategoryEntity
import com.aktawind.monnoel.core.database.entity.GiftIdeaEntity
import com.aktawind.monnoel.core.database.entity.IdeaPersonEntity
import com.aktawind.monnoel.core.database.entity.PersonBudgetEntity
import com.aktawind.monnoel.core.database.entity.PersonEntity
import com.aktawind.monnoel.core.database.entity.PlannedGiftEntity
import com.aktawind.monnoel.core.database.entity.WishItemEntity
import javax.inject.Inject

/** Lit toute la base pour l'export, et la remplace intégralement lors d'un import. */
class BackupDataSource @Inject constructor(private val db: MonNoelDatabase) {
    suspend fun snapshot(): BackupData = db.withTransaction {
        BackupData(
            editions = db.editionDao().getAll().map { it.toDto() },
            giftCategories = db.giftCategoryDao().getAll().map { GiftCategoryDto(it.id, it.name, it.sortOrder) },
            persons = db.personDao().getAll().map { it.toDto() },
            wishItems = db.personDao().getAllWishes().map {
                WishItemDto(it.id, it.personId, it.text, it.done, it.sortOrder)
            },
            giftIdeas = db.giftIdeaDao().getAll().map { it.toDto() },
            ideaPersons = db.giftIdeaDao().getAllLinks().map { IdeaPersonDto(it.ideaId, it.personId) },
            plannedGifts = db.plannedGiftDao().getAll().map { it.toDto() },
            personBudgets = db.plannedGiftDao().getAllBudgets().map {
                PersonBudgetDto(it.editionId, it.personId, it.amountCents)
            },
        )
    }

    /** Remplacement complet dans une seule transaction : tout ou rien. */
    suspend fun replaceAll(data: BackupData) {
        db.withTransaction {
            // Suppression des enfants vers les parents.
            db.plannedGiftDao().deleteAll()
            db.giftIdeaDao().deleteAll()
            db.personDao().deleteAll()
            db.giftCategoryDao().deleteAll()
            db.editionDao().deleteAll()
            // Insertion des parents vers les enfants.
            db.editionDao().insertAll(data.editions.map { it.toEntity() })
            db.giftCategoryDao().insertAll(data.giftCategories.map { GiftCategoryEntity(it.id, it.name, it.sortOrder) })
            db.personDao().insertAll(data.persons.map { it.toEntity() })
            db.personDao().insertWishes(
                data.wishItems.map {
                    WishItemEntity(it.id, it.personId, it.text, it.done, it.sortOrder)
                },
            )
            db.giftIdeaDao().insertAll(data.giftIdeas.map { it.toEntity() })
            db.giftIdeaDao().insertLinks(data.ideaPersons.map { IdeaPersonEntity(it.ideaId, it.personId) })
            db.plannedGiftDao().insertAll(data.plannedGifts.map { it.toEntity() })
            db.plannedGiftDao().insertBudgets(
                data.personBudgets.map { PersonBudgetEntity(it.editionId, it.personId, it.amountCents) },
            )
        }
    }
}

private fun EditionEntity.toDto() = EditionDto(id, year, status, plannedBudgetCents, createdAt, updatedAt)

private fun EditionDto.toEntity() = EditionEntity(id, year, status, plannedBudgetCents, createdAt, updatedAt)

private fun PersonEntity.toDto() = PersonDto(id, firstName, lastName, photoPath, interests, notes, createdAt, updatedAt)

private fun PersonDto.toEntity() =
    PersonEntity(id, firstName, lastName, photoPath, interests, notes, createdAt, updatedAt)

private fun GiftIdeaEntity.toDto() = GiftIdeaDto(
    id, title, description, photoPath, estimatedPriceCents, url, shop, notes, categoryId, createdAt, updatedAt,
)

private fun GiftIdeaDto.toEntity() = GiftIdeaEntity(
    id, title, description, photoPath, estimatedPriceCents, url, shop, notes, categoryId, createdAt, updatedAt,
)

private fun PlannedGiftEntity.toDto() = PlannedGiftDto(
    id, editionId, personId, recipientName, ideaId, title, description, photoPath, priceCents, purchaseDate, shop,
    orderUrl, trackingNumber, notes, categoryId, status, createdAt, updatedAt,
)

private fun PlannedGiftDto.toEntity() = PlannedGiftEntity(
    id, editionId, personId, recipientName, ideaId, title, description, photoPath, priceCents, purchaseDate, shop,
    orderUrl, trackingNumber, notes, categoryId, status, createdAt, updatedAt,
)

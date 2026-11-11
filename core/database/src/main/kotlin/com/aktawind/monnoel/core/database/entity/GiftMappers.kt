package com.aktawind.monnoel.core.database.entity

import com.aktawind.monnoel.core.model.GiftCategory
import com.aktawind.monnoel.core.model.GiftIdea
import com.aktawind.monnoel.core.model.GiftStatus
import com.aktawind.monnoel.core.model.Person
import com.aktawind.monnoel.core.model.PersonBudget
import com.aktawind.monnoel.core.model.PlannedGift
import com.aktawind.monnoel.core.model.WishItem
import java.time.Instant
import java.time.LocalDate

fun PersonEntity.asModel() = Person(
    id,
    firstName,
    lastName,
    photoPath,
    interests,
    notes,
    Instant.ofEpochMilli(createdAt),
    Instant.ofEpochMilli(updatedAt),
)

fun Person.asEntity() = PersonEntity(
    id,
    firstName,
    lastName,
    photoPath,
    interests,
    notes,
    createdAt.toEpochMilli(),
    updatedAt.toEpochMilli(),
)

fun WishItemEntity.asModel() = WishItem(id, personId, text, done, sortOrder)

fun WishItem.asEntity() = WishItemEntity(id, personId, text, done, sortOrder)

fun GiftCategoryEntity.asModel() = GiftCategory(id, name, sortOrder)

fun GiftCategory.asEntity() = GiftCategoryEntity(id, name, sortOrder)

fun GiftIdeaEntity.asModel(personIds: Set<String>) = GiftIdea(
    id, title, description, photoPath, estimatedPriceCents, url, shop, notes, categoryId, personIds,
    Instant.ofEpochMilli(createdAt), Instant.ofEpochMilli(updatedAt),
)

fun GiftIdea.asEntity() = GiftIdeaEntity(
    id, title, description, photoPath, estimatedPriceCents, url, shop, notes, categoryId,
    createdAt.toEpochMilli(), updatedAt.toEpochMilli(),
)

fun PlannedGiftEntity.asModel() = PlannedGift(
    id = id,
    editionId = editionId,
    personId = personId,
    recipientName = recipientName,
    ideaId = ideaId,
    title = title,
    description = description,
    photoPath = photoPath,
    priceCents = priceCents,
    purchaseDate = purchaseDate?.let(LocalDate::ofEpochDay),
    shop = shop,
    orderUrl = orderUrl,
    trackingNumber = trackingNumber,
    notes = notes,
    categoryId = categoryId,
    status = runCatching { GiftStatus.valueOf(status) }.getOrDefault(GiftStatus.TO_BUY),
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

fun PlannedGift.asEntity() = PlannedGiftEntity(
    id, editionId, personId, recipientName, ideaId, title, description, photoPath, priceCents,
    purchaseDate?.toEpochDay(), shop, orderUrl, trackingNumber, notes, categoryId, status.name,
    createdAt.toEpochMilli(), updatedAt.toEpochMilli(),
)

fun PersonBudgetEntity.asModel() = PersonBudget(editionId, personId, amountCents)

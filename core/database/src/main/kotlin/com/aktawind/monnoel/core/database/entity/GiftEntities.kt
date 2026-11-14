package com.aktawind.monnoel.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "person")
data class PersonEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "first_name") val firstName: String,
    @ColumnInfo(name = "last_name") val lastName: String,
    @ColumnInfo(name = "photo_path") val photoPath: String?,
    val interests: String,
    val notes: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(tableName = "gift_category")
data class GiftCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
)

@Entity(
    tableName = "gift_idea",
    foreignKeys = [
        ForeignKey(
            entity = GiftCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("category_id")],
)
data class GiftIdeaEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    @ColumnInfo(name = "photo_path") val photoPath: String?,
    @ColumnInfo(name = "estimated_price_cents") val estimatedPriceCents: Long?,
    val url: String,
    val shop: String,
    val notes: String,
    @ColumnInfo(name = "category_id") val categoryId: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "idea_person",
    primaryKeys = ["idea_id", "person_id"],
    foreignKeys = [
        ForeignKey(
            entity = GiftIdeaEntity::class,
            parentColumns = ["id"],
            childColumns = ["idea_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["person_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("person_id")],
)
data class IdeaPersonEntity(
    @ColumnInfo(name = "idea_id") val ideaId: String,
    @ColumnInfo(name = "person_id") val personId: String,
)

@Entity(
    tableName = "planned_gift",
    foreignKeys = [
        ForeignKey(
            entity = EditionEntity::class,
            parentColumns = ["id"],
            childColumns = ["edition_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["person_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = GiftIdeaEntity::class,
            parentColumns = ["id"],
            childColumns = ["idea_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = GiftCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("edition_id"), Index("person_id"), Index("idea_id"), Index("category_id")],
)
data class PlannedGiftEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "edition_id") val editionId: String,
    @ColumnInfo(name = "person_id") val personId: String?,
    @ColumnInfo(name = "recipient_name") val recipientName: String,
    @ColumnInfo(name = "idea_id") val ideaId: String?,
    val title: String,
    val description: String,
    @ColumnInfo(name = "photo_path") val photoPath: String?,
    @ColumnInfo(name = "price_cents") val priceCents: Long?,
    /** Jour d'achat (epoch day). */
    @ColumnInfo(name = "purchase_date") val purchaseDate: Long?,
    val shop: String,
    @ColumnInfo(name = "order_url") val orderUrl: String,
    @ColumnInfo(name = "tracking_number") val trackingNumber: String,
    val notes: String,
    @ColumnInfo(name = "category_id") val categoryId: String?,
    val status: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "person_budget",
    primaryKeys = ["edition_id", "person_id"],
    foreignKeys = [
        ForeignKey(
            entity = EditionEntity::class,
            parentColumns = ["id"],
            childColumns = ["edition_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["person_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("person_id")],
)
data class PersonBudgetEntity(
    @ColumnInfo(name = "edition_id") val editionId: String,
    @ColumnInfo(name = "person_id") val personId: String,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
)

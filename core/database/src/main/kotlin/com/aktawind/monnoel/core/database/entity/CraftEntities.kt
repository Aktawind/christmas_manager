package com.aktawind.monnoel.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aktawind.monnoel.core.model.Craft
import com.aktawind.monnoel.core.model.CraftCategory
import com.aktawind.monnoel.core.model.CraftStatus
import com.aktawind.monnoel.core.model.CraftSupply
import com.aktawind.monnoel.core.model.CraftYear
import com.aktawind.monnoel.core.model.DecorIdea
import com.aktawind.monnoel.core.model.DecorPlace
import java.time.Instant

@Entity(tableName = "craft")
data class CraftEntity(
    @PrimaryKey val id: String,
    val title: String,
    @ColumnInfo(name = "photo_path") val photoPath: String?,
    val category: String,
    @ColumnInfo(name = "tutorial_url") val tutorialUrl: String,
    val notes: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "craft_supply",
    foreignKeys = [
        ForeignKey(
            entity = CraftEntity::class,
            parentColumns = ["id"],
            childColumns = ["craft_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("craft_id")],
)
data class CraftSupplyEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "craft_id") val craftId: String,
    val name: String,
    @ColumnInfo(name = "price_cents") val priceCents: Long?,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
)

@Entity(
    tableName = "craft_year",
    foreignKeys = [
        ForeignKey(entity = EditionEntity::class, parentColumns = ["id"], childColumns = ["edition_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CraftEntity::class, parentColumns = ["id"], childColumns = ["craft_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["edition_id", "craft_id"], unique = true), Index("craft_id")],
)
data class CraftYearEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "edition_id") val editionId: String,
    @ColumnInfo(name = "craft_id") val craftId: String,
    val status: String,
    @ColumnInfo(name = "result_photo_path") val resultPhotoPath: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/** Fourniture achetée pour un Noël. */
@Entity(
    tableName = "craft_supply_bought",
    primaryKeys = ["edition_id", "supply_id"],
    foreignKeys = [
        ForeignKey(entity = EditionEntity::class, parentColumns = ["id"], childColumns = ["edition_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CraftSupplyEntity::class, parentColumns = ["id"], childColumns = ["supply_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("supply_id")],
)
data class CraftSupplyBoughtEntity(
    @ColumnInfo(name = "edition_id") val editionId: String,
    @ColumnInfo(name = "supply_id") val supplyId: String,
)

@Entity(tableName = "decor_idea")
data class DecorIdeaEntity(
    @PrimaryKey val id: String,
    val title: String,
    @ColumnInfo(name = "photo_path") val photoPath: String?,
    val url: String,
    val comment: String,
    val place: String,
    val favorite: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

fun CraftEntity.asModel() = Craft(
    id,
    title,
    photoPath,
    runCatching { CraftCategory.valueOf(category) }.getOrDefault(CraftCategory.OTHER),
    tutorialUrl,
    notes,
    Instant.ofEpochMilli(createdAt),
    Instant.ofEpochMilli(updatedAt),
)

fun Craft.asEntity() = CraftEntity(
    id,
    title,
    photoPath,
    category.name,
    tutorialUrl,
    notes,
    createdAt.toEpochMilli(),
    updatedAt.toEpochMilli(),
)

fun CraftSupplyEntity.asModel() = CraftSupply(id, craftId, name, priceCents, sortOrder)

fun CraftSupply.asEntity() = CraftSupplyEntity(id, craftId, name, priceCents, sortOrder)

fun CraftYearEntity.asModel() = CraftYear(
    id,
    editionId,
    craftId,
    runCatching { CraftStatus.valueOf(status) }.getOrDefault(CraftStatus.PLANNED),
    resultPhotoPath,
    Instant.ofEpochMilli(createdAt),
)

fun CraftYear.asEntity() = CraftYearEntity(id, editionId, craftId, status.name, resultPhotoPath, createdAt.toEpochMilli())

fun DecorIdeaEntity.asModel() = DecorIdea(
    id, title, photoPath, url, comment, runCatching { DecorPlace.valueOf(place) }.getOrDefault(DecorPlace.OTHER), favorite,
    Instant.ofEpochMilli(createdAt), Instant.ofEpochMilli(updatedAt),
)

fun DecorIdea.asEntity() =
    DecorIdeaEntity(id, title, photoPath, url, comment, place.name, favorite, createdAt.toEpochMilli(), updatedAt.toEpochMilli())

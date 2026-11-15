package com.aktawind.monnoel.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aktawind.monnoel.core.model.Production
import com.aktawind.monnoel.core.model.ProductionStatus
import com.aktawind.monnoel.core.model.Recipe
import com.aktawind.monnoel.core.model.RecipeCategory
import java.time.Instant

@Entity(tableName = "recipe_category")
data class RecipeCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
)

@Entity(
    tableName = "recipe",
    foreignKeys = [
        ForeignKey(
            entity = RecipeCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("category_id")],
)
data class RecipeEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "photo_path") val photoPath: String?,
    @ColumnInfo(name = "category_id") val categoryId: String?,
    val url: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "production",
    foreignKeys = [
        ForeignKey(entity = EditionEntity::class, parentColumns = ["id"], childColumns = ["edition_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = RecipeEntity::class, parentColumns = ["id"], childColumns = ["recipe_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("edition_id"), Index("recipe_id")],
)
data class ProductionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "edition_id") val editionId: String,
    @ColumnInfo(name = "recipe_id") val recipeId: String?,
    @ColumnInfo(name = "recipe_name") val recipeName: String,
    @ColumnInfo(name = "recipe_photo_path") val recipePhotoPath: String?,
    val quantity: Int,
    val destination: String,
    val status: String,
    @ColumnInfo(name = "photo_path") val photoPath: String?,
    val notes: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

fun RecipeCategoryEntity.asModel() = RecipeCategory(id, name, sortOrder)

fun RecipeCategory.asEntity() = RecipeCategoryEntity(id, name, sortOrder)

fun RecipeEntity.asModel() = Recipe(id, name, photoPath, categoryId, url, Instant.ofEpochMilli(createdAt), Instant.ofEpochMilli(updatedAt))

fun Recipe.asEntity() = RecipeEntity(id, name, photoPath, categoryId, url, createdAt.toEpochMilli(), updatedAt.toEpochMilli())

fun ProductionEntity.asModel() = Production(
    id, editionId, recipeId, recipeName, recipePhotoPath, quantity, destination,
    runCatching { ProductionStatus.valueOf(status) }.getOrDefault(ProductionStatus.TO_DO), photoPath, notes,
    Instant.ofEpochMilli(createdAt), Instant.ofEpochMilli(updatedAt),
)

fun Production.asEntity() = ProductionEntity(
    id, editionId, recipeId, recipeName, recipePhotoPath, quantity, destination, status.name, photoPath, notes,
    createdAt.toEpochMilli(), updatedAt.toEpochMilli(),
)

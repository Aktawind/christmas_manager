package com.aktawind.monnoel.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aktawind.monnoel.core.model.CourseType
import com.aktawind.monnoel.core.model.Meal
import com.aktawind.monnoel.core.model.MealDish
import com.aktawind.monnoel.core.model.MealType
import com.aktawind.monnoel.core.model.defaultCourses
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "meal",
    foreignKeys = [
        ForeignKey(entity = EditionEntity::class, parentColumns = ["id"], childColumns = ["edition_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("edition_id")],
)
data class MealEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "edition_id") val editionId: String,
    val name: String,
    val type: String,
    /** Date ISO-8601 (AAAA-MM-JJ) ou null. */
    val date: String?,
    val place: String,
    /** Invités hors application, un par ligne. */
    val guests: String,
    @ColumnInfo(name = "estimated_cost_cents") val estimatedCostCents: Long?,
    val notes: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    /** Moments du menu, séparés par des virgules ; vide = moments par défaut du type. */
    @ColumnInfo(defaultValue = "") val courses: String = "",
)

@Entity(
    tableName = "meal_person",
    primaryKeys = ["meal_id", "person_id"],
    foreignKeys = [
        ForeignKey(entity = MealEntity::class, parentColumns = ["id"], childColumns = ["meal_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PersonEntity::class, parentColumns = ["id"], childColumns = ["person_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("person_id")],
)
data class MealPersonEntity(
    @ColumnInfo(name = "meal_id") val mealId: String,
    @ColumnInfo(name = "person_id") val personId: String,
)

@Entity(
    tableName = "meal_dish",
    foreignKeys = [
        ForeignKey(entity = MealEntity::class, parentColumns = ["id"], childColumns = ["meal_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = RecipeEntity::class, parentColumns = ["id"], childColumns = ["recipe_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("meal_id"), Index("recipe_id")],
)
data class MealDishEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "meal_id") val mealId: String,
    val course: String,
    @ColumnInfo(name = "recipe_id") val recipeId: String?,
    val name: String,
    @ColumnInfo(name = "photo_path") val photoPath: String?,
    @ColumnInfo(name = "brought_by") val broughtBy: String,
    @ColumnInfo(name = "to_order") val toOrder: Boolean,
    val ordered: Boolean,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

fun MealEntity.asModel(personIds: Set<String>): Meal {
    val mealType = runCatching { MealType.valueOf(type) }.getOrDefault(MealType.OTHER)
    val chosen = courses.split(',').mapNotNull { c -> runCatching { CourseType.valueOf(c.trim()) }.getOrNull() }
    return Meal(
        id = id,
        editionId = editionId,
        name = name,
        type = mealType,
        date = date?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        place = place,
        personIds = personIds,
        guests = guests.split('\n').map { it.trim() }.filter { it.isNotEmpty() },
        estimatedCostCents = estimatedCostCents,
        notes = notes,
        courses = chosen.ifEmpty { mealType.defaultCourses() }.distinct().sorted(),
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )
}

fun Meal.asEntity() = MealEntity(
    id, editionId, name, type.name, date?.toString(), place, guests.joinToString("\n"), estimatedCostCents, notes,
    createdAt.toEpochMilli(), updatedAt.toEpochMilli(), courses.joinToString(",") { it.name },
)

fun MealDishEntity.asModel() = MealDish(
    id, mealId, runCatching { CourseType.valueOf(course) }.getOrDefault(CourseType.MAIN), recipeId, name, photoPath, broughtBy,
    toOrder, ordered, sortOrder, Instant.ofEpochMilli(createdAt),
)

fun MealDish.asEntity() = MealDishEntity(
    id, mealId, course.name, recipeId, name, photoPath, broughtBy, toOrder, ordered, sortOrder, createdAt.toEpochMilli(),
)

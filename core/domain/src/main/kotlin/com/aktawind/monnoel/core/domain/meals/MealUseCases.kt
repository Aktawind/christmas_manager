package com.aktawind.monnoel.core.domain.meals

import com.aktawind.monnoel.core.common.AppError
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.IdGenerator
import com.aktawind.monnoel.core.common.TimeProvider
import com.aktawind.monnoel.core.domain.cooking.CookingRepository
import com.aktawind.monnoel.core.domain.gifts.EditionGuard
import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import com.aktawind.monnoel.core.model.CourseType
import com.aktawind.monnoel.core.model.Meal
import com.aktawind.monnoel.core.model.MealDish
import com.aktawind.monnoel.core.model.MealType
import java.time.LocalDate
import javax.inject.Inject

data class MealDraft(
    val name: String,
    val type: MealType = MealType.OTHER,
    val date: LocalDate? = null,
    val place: String = "",
    val personIds: Set<String> = emptySet(),
    val guests: List<String> = emptyList(),
    val estimatedCostCents: Long? = null,
    val notes: String = "",
)

class SaveMealUseCase @Inject constructor(
    private val meals: MealRepository,
    private val guard: EditionGuard,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(id: String?, editionId: String, draft: MealDraft): AppResult<Meal> {
        if (draft.name.isBlank() || (draft.estimatedCostCents ?: 0) < 0) return AppResult.Failure(AppError.InvalidInput)
        val existing = id?.let { meals.getMeal(it) }
        guard.check(existing?.editionId ?: editionId)?.let { return AppResult.Failure(it) }
        val now = time.now()
        val meal = Meal(
            id = existing?.id ?: ids.newId(),
            editionId = existing?.editionId ?: editionId,
            name = draft.name.trim(),
            type = draft.type,
            date = draft.date,
            place = draft.place.trim(),
            personIds = draft.personIds,
            guests = draft.guests.map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
            estimatedCostCents = draft.estimatedCostCents,
            notes = draft.notes.trim(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        meals.saveMeal(meal)
        return AppResult.Success(meal)
    }
}

class DeleteMealUseCase @Inject constructor(
    private val meals: MealRepository,
    private val photos: PhotoRepository,
    private val guard: EditionGuard,
) {
    suspend operator fun invoke(id: String): AppResult<Unit> {
        val meal = meals.getMeal(id) ?: return AppResult.Failure(AppError.NotFound)
        guard.check(meal.editionId)?.let { return AppResult.Failure(it) }
        val dishPhotos = meals.getDishesOfMeal(id).mapNotNull { it.photoPath }
        meals.deleteMeal(id)
        dishPhotos.forEach { photos.deletePhoto(it) }
        return AppResult.Success(Unit)
    }
}

data class DishDraft(
    val course: CourseType,
    val recipeId: String? = null,
    val name: String,
    val broughtBy: String = "",
    val toOrder: Boolean = false,
    val ordered: Boolean = false,
)

/**
 * Ajoute ou modifie un plat. Un plat tiré de la bibliothèque copie le nom et la photo de la
 * recette (historique figé, ADR 0003).
 */
class SaveDishUseCase @Inject constructor(
    private val meals: MealRepository,
    private val cooking: CookingRepository,
    private val photos: PhotoRepository,
    private val guard: EditionGuard,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(id: String?, mealId: String, draft: DishDraft): AppResult<MealDish> {
        if (draft.name.isBlank()) return AppResult.Failure(AppError.InvalidInput)
        val existing = id?.let { meals.getDish(it) }
        val meal = meals.getMeal(existing?.mealId ?: mealId) ?: return AppResult.Failure(AppError.NotFound)
        guard.check(meal.editionId)?.let { return AppResult.Failure(it) }
        val photo = when {
            existing != null && existing.recipeId == draft.recipeId -> existing.photoPath
            else -> {
                existing?.photoPath?.let { photos.deletePhoto(it) }
                draft.recipeId?.let { cooking.getRecipe(it) }?.photoPath?.let { photos.copyPhoto(it) }
            }
        }
        val order = existing?.sortOrder ?: ((meals.getDishesOfMeal(meal.id).maxOfOrNull { it.sortOrder } ?: -1) + 1)
        val dish = MealDish(
            id = existing?.id ?: ids.newId(),
            mealId = meal.id,
            course = draft.course,
            recipeId = draft.recipeId,
            name = draft.name.trim(),
            photoPath = photo,
            broughtBy = draft.broughtBy.trim(),
            toOrder = draft.toOrder,
            ordered = draft.toOrder && draft.ordered,
            sortOrder = order,
            createdAt = existing?.createdAt ?: time.now(),
        )
        meals.saveDish(dish)
        return AppResult.Success(dish)
    }
}

class SetDishOrderedUseCase @Inject constructor(private val meals: MealRepository, private val guard: EditionGuard) {
    suspend operator fun invoke(id: String, ordered: Boolean): AppResult<Unit> {
        val dish = meals.getDish(id) ?: return AppResult.Failure(AppError.NotFound)
        val meal = meals.getMeal(dish.mealId) ?: return AppResult.Failure(AppError.NotFound)
        guard.check(meal.editionId)?.let { return AppResult.Failure(it) }
        meals.saveDish(dish.copy(ordered = ordered))
        return AppResult.Success(Unit)
    }
}

class DeleteDishUseCase @Inject constructor(
    private val meals: MealRepository,
    private val photos: PhotoRepository,
    private val guard: EditionGuard,
) {
    suspend operator fun invoke(id: String): AppResult<Unit> {
        val dish = meals.getDish(id) ?: return AppResult.Failure(AppError.NotFound)
        val meal = meals.getMeal(dish.mealId) ?: return AppResult.Failure(AppError.NotFound)
        guard.check(meal.editionId)?.let { return AppResult.Failure(it) }
        meals.deleteDish(id)
        dish.photoPath?.let { photos.deletePhoto(it) }
        return AppResult.Success(Unit)
    }
}

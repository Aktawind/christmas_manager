package com.aktawind.monnoel.core.domain.cooking

import com.aktawind.monnoel.core.common.AppError
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.IdGenerator
import com.aktawind.monnoel.core.common.TimeProvider
import com.aktawind.monnoel.core.domain.gifts.EditionGuard
import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import com.aktawind.monnoel.core.model.Production
import com.aktawind.monnoel.core.model.ProductionStatus
import com.aktawind.monnoel.core.model.Recipe
import javax.inject.Inject

class SaveRecipeUseCase @Inject constructor(
    private val cooking: CookingRepository,
    private val photos: PhotoRepository,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(id: String?, name: String, photoPath: String?, categoryId: String?, url: String): AppResult<Recipe> {
        if (name.isBlank()) return AppResult.Failure(AppError.InvalidInput)
        val existing = id?.let { cooking.getRecipe(it) }
        existing?.photoPath?.takeIf { it != photoPath }?.let { photos.deletePhoto(it) }
        val now = time.now()
        val recipe = Recipe(existing?.id ?: ids.newId(), name.trim(), photoPath, categoryId, url.trim(), existing?.createdAt ?: now, now)
        cooking.saveRecipe(recipe)
        return AppResult.Success(recipe)
    }
}

class DeleteRecipeUseCase @Inject constructor(private val cooking: CookingRepository, private val photos: PhotoRepository) {
    /** Les productions déjà faites gardent leur copie du nom et de la photo. */
    suspend operator fun invoke(id: String): AppResult<Unit> {
        val recipe = cooking.getRecipe(id) ?: return AppResult.Failure(AppError.NotFound)
        cooking.deleteRecipe(id)
        recipe.photoPath?.let { photos.deletePhoto(it) }
        return AppResult.Success(Unit)
    }
}

data class ProductionDraft(
    val recipeId: String?,
    val recipeName: String,
    val quantity: Int = 1,
    val destination: String = "",
    val status: ProductionStatus = ProductionStatus.TO_DO,
    val photoPath: String? = null,
    val notes: String = "",
)

/**
 * Crée ou modifie une production. À la création depuis une recette, le nom et la photo de la
 * recette sont copiés (historique figé, ADR 0003).
 */
class SaveProductionUseCase @Inject constructor(
    private val cooking: CookingRepository,
    private val photos: PhotoRepository,
    private val guard: EditionGuard,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(id: String?, editionId: String, draft: ProductionDraft): AppResult<Production> {
        if (draft.recipeName.isBlank() || draft.quantity < 1) return AppResult.Failure(AppError.InvalidInput)
        guard.check(editionId)?.let { return AppResult.Failure(it) }
        val existing = id?.let { cooking.getProduction(it) }
        existing?.photoPath?.takeIf { it != draft.photoPath }?.let { photos.deletePhoto(it) }
        val recipe = draft.recipeId?.let { cooking.getRecipe(it) }
        val recipePhoto = when {
            existing != null && existing.recipeId == draft.recipeId -> existing.recipePhotoPath
            else -> {
                existing?.recipePhotoPath?.let { photos.deletePhoto(it) }
                recipe?.photoPath?.let { photos.copyPhoto(it) }
            }
        }
        val now = time.now()
        val production = Production(
            id = existing?.id ?: ids.newId(),
            editionId = existing?.editionId ?: editionId,
            recipeId = recipe?.id ?: draft.recipeId,
            recipeName = draft.recipeName.trim(),
            recipePhotoPath = recipePhoto,
            quantity = draft.quantity,
            destination = draft.destination.trim(),
            status = draft.status,
            photoPath = draft.photoPath,
            notes = draft.notes.trim(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        cooking.saveProduction(production)
        return AppResult.Success(production)
    }
}

class SetProductionStatusUseCase @Inject constructor(
    private val cooking: CookingRepository,
    private val guard: EditionGuard,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(id: String, status: ProductionStatus): AppResult<Unit> {
        val production = cooking.getProduction(id) ?: return AppResult.Failure(AppError.NotFound)
        guard.check(production.editionId)?.let { return AppResult.Failure(it) }
        cooking.saveProduction(production.copy(status = status, updatedAt = time.now()))
        return AppResult.Success(Unit)
    }
}

class DeleteProductionUseCase @Inject constructor(
    private val cooking: CookingRepository,
    private val photos: PhotoRepository,
    private val guard: EditionGuard,
) {
    suspend operator fun invoke(id: String): AppResult<Unit> {
        val production = cooking.getProduction(id) ?: return AppResult.Failure(AppError.NotFound)
        guard.check(production.editionId)?.let { return AppResult.Failure(it) }
        cooking.deleteProduction(id)
        listOfNotNull(production.photoPath, production.recipePhotoPath).forEach { photos.deletePhoto(it) }
        return AppResult.Success(Unit)
    }
}

package com.aktawind.monnoel.core.domain.cooking

import com.aktawind.monnoel.core.common.AppError
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.edition.FakeEditionRepository
import com.aktawind.monnoel.core.domain.edition.FixedTime
import com.aktawind.monnoel.core.domain.edition.SequentialIds
import com.aktawind.monnoel.core.domain.gifts.EditionGuard
import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import com.aktawind.monnoel.core.domain.tasks.SuggestionSource
import com.aktawind.monnoel.core.domain.tasks.currentSuggestions
import com.aktawind.monnoel.core.domain.tasks.suggestionStates
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.model.EditionStatus
import com.aktawind.monnoel.core.model.Production
import com.aktawind.monnoel.core.model.ProductionStatus
import com.aktawind.monnoel.core.model.Recipe
import com.aktawind.monnoel.core.model.RecipeCategory
import com.aktawind.monnoel.core.model.SuggestionKind
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest

class FakeCookingRepository : CookingRepository {
    val recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val categories = MutableStateFlow<List<RecipeCategory>>(emptyList())
    val productions = MutableStateFlow<List<Production>>(emptyList())

    override fun observeRecipes() = recipes

    override suspend fun getRecipe(id: String) = recipes.value.firstOrNull { it.id == id }

    override suspend fun saveRecipe(recipe: Recipe) {
        recipes.value = recipes.value.filterNot { it.id == recipe.id } + recipe
    }

    override suspend fun deleteRecipe(id: String) {
        recipes.value = recipes.value.filterNot { it.id == id }
    }

    override fun observeCategories() = categories

    override suspend fun saveCategory(category: RecipeCategory) {
        categories.value = categories.value.filterNot { it.id == category.id } + category
    }

    override suspend fun deleteCategory(id: String) {
        categories.value = categories.value.filterNot { it.id == id }
    }

    override fun observeProductions(editionId: String) = productions.map { l -> l.filter { it.editionId == editionId } }

    override fun observeAllProductions() = productions

    override suspend fun getProduction(id: String) = productions.value.firstOrNull { it.id == id }

    override suspend fun saveProduction(production: Production) {
        productions.value = productions.value.filterNot { it.id == production.id } + production
    }

    override suspend fun deleteProduction(id: String) {
        productions.value = productions.value.filterNot { it.id == id }
    }
}

private class CopyPhotos : PhotoRepository {
    var n = 0
    val deleted = mutableListOf<String>()

    override suspend fun importPhoto(sourceUri: String) = AppResult.Success("photos/new.jpg")

    override suspend fun copyPhoto(relativePath: String) = "photos/copy-${n++}.jpg"

    override suspend fun deletePhoto(relativePath: String) {
        deleted += relativePath
    }
}

class CookingUseCasesTest {
    private val editions = FakeEditionRepository()
    private val cooking = FakeCookingRepository()
    private val photos = CopyPhotos()
    private val ids = SequentialIds()
    private val time = FixedTime()
    private val guard = EditionGuard(editions)
    private val saveRecipe = SaveRecipeUseCase(cooking, photos, time, ids)
    private val saveProduction = SaveProductionUseCase(cooking, photos, guard, time, ids)

    private suspend fun edition(id: String = "e", archived: Boolean = false) = editions.insert(
        ChristmasEdition(id, 2026, if (archived) EditionStatus.ARCHIVED else EditionStatus.ACTIVE, null, Instant.EPOCH, Instant.EPOCH),
    )

    @Test
    fun `une production fige le nom et la photo de la recette`() = runTest {
        edition()
        val recipe = (saveRecipe(null, "Bûche chocolat-noisette", "photos/buche.jpg", null, "") as AppResult.Success).value
        val production = (
            saveProduction(null, "e", ProductionDraft(recipe.id, recipe.name, quantity = 2, destination = "Réveillon"))
                as AppResult.Success
            ).value
        assertEquals("Bûche chocolat-noisette", production.recipeName)
        assertNotEquals(recipe.photoPath, production.recipePhotoPath)
        // Renommer la recette ne change pas la production.
        saveRecipe(recipe.id, "Bûche revisitée", "photos/buche.jpg", null, "")
        assertEquals("Bûche chocolat-noisette", cooking.getProduction(production.id)?.recipeName)
    }

    @Test
    fun `nom obligatoire, quantite positive, annee archivee`() = runTest {
        edition()
        edition("old", archived = true)
        assertEquals(AppResult.Failure(AppError.InvalidInput), saveRecipe(null, " ", null, null, ""))
        assertEquals(AppResult.Failure(AppError.InvalidInput), saveProduction(null, "e", ProductionDraft(null, "Sablés", quantity = 0)))
        assertEquals(AppResult.Failure(AppError.ReadOnly), saveProduction(null, "old", ProductionDraft(null, "Sablés")))
    }

    @Test
    fun `le lutin suggere de preparer les productions et coche quand c'est fait`() = runTest {
        edition()
        val production = (saveProduction(null, "e", ProductionDraft(null, "Sablés", quantity = 3)) as AppResult.Success).value
        val source = SuggestionSource(emptyList(), emptyList(), emptyList(), emptyList(), cooking.productions.value)
        val suggestion = currentSuggestions(source, emptySet(), emptySet(), emptyList()).single()
        assertEquals(SuggestionKind.PRODUCTION_MAKE, suggestion.kind)
        assertEquals(3, suggestion.count)
        assertEquals(false, suggestionStates(source)["PRODUCTION_MAKE:${production.id}"])

        SetProductionStatusUseCase(cooking, guard, time)(production.id, ProductionStatus.DONE)
        val done = SuggestionSource(emptyList(), emptyList(), emptyList(), emptyList(), cooking.productions.value)
        assertTrue(currentSuggestions(done, emptySet(), emptySet(), emptyList()).isEmpty())
        assertEquals(true, suggestionStates(done)["PRODUCTION_MAKE:${production.id}"])
    }
}

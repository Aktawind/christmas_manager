package com.aktawind.monnoel.core.domain.meals

import com.aktawind.monnoel.core.common.AppError
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.cooking.FakeCookingRepository
import com.aktawind.monnoel.core.domain.edition.FakeEditionRepository
import com.aktawind.monnoel.core.domain.edition.FixedTime
import com.aktawind.monnoel.core.domain.edition.SequentialIds
import com.aktawind.monnoel.core.domain.gifts.EditionGuard
import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import com.aktawind.monnoel.core.domain.search.SearchData
import com.aktawind.monnoel.core.domain.search.searchAll
import com.aktawind.monnoel.core.domain.tasks.SuggestionSource
import com.aktawind.monnoel.core.domain.tasks.currentSuggestions
import com.aktawind.monnoel.core.domain.tasks.suggestionStates
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.model.CourseType
import com.aktawind.monnoel.core.model.EditionStatus
import com.aktawind.monnoel.core.model.LinkType
import com.aktawind.monnoel.core.model.Meal
import com.aktawind.monnoel.core.model.MealDish
import com.aktawind.monnoel.core.model.MealType
import com.aktawind.monnoel.core.model.Recipe
import com.aktawind.monnoel.core.model.SuggestionKind
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest

class FakeMealRepository : MealRepository {
    val meals = MutableStateFlow<List<Meal>>(emptyList())
    val dishes = MutableStateFlow<List<MealDish>>(emptyList())

    override fun observeMeals(editionId: String) = meals.map { l -> l.filter { it.editionId == editionId } }

    override fun observeAllMeals() = meals

    override fun observeMeal(id: String) = meals.map { l -> l.firstOrNull { it.id == id } }

    override suspend fun getMeal(id: String) = meals.value.firstOrNull { it.id == id }

    override suspend fun saveMeal(meal: Meal) {
        meals.value = meals.value.filterNot { it.id == meal.id } + meal
    }

    override suspend fun deleteMeal(id: String) {
        meals.value = meals.value.filterNot { it.id == id }
        dishes.value = dishes.value.filterNot { it.mealId == id }
    }

    override fun observeDishes(editionId: String) = dishes

    override fun observeAllDishes() = dishes

    override fun observeDishesOfMeal(mealId: String) = dishes.map { l -> l.filter { it.mealId == mealId } }

    override suspend fun getDishesOfMeal(mealId: String) = dishes.value.filter { it.mealId == mealId }

    override suspend fun getDish(id: String) = dishes.value.firstOrNull { it.id == id }

    override suspend fun saveDish(dish: MealDish) {
        dishes.value = dishes.value.filterNot { it.id == dish.id } + dish
    }

    override suspend fun deleteDish(id: String) {
        dishes.value = dishes.value.filterNot { it.id == id }
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

class MealUseCasesTest {
    private val editions = FakeEditionRepository()
    private val meals = FakeMealRepository()
    private val cooking = FakeCookingRepository()
    private val photos = CopyPhotos()
    private val ids = SequentialIds()
    private val time = FixedTime()
    private val guard = EditionGuard(editions)
    private val saveMeal = SaveMealUseCase(meals, guard, time, ids)
    private val saveDish = SaveDishUseCase(meals, cooking, photos, guard, time, ids)

    private suspend fun edition(id: String = "e", archived: Boolean = false) = editions.insert(
        ChristmasEdition(id, 2026, if (archived) EditionStatus.ARCHIVED else EditionStatus.ACTIVE, null, Instant.EPOCH, Instant.EPOCH),
    )

    private suspend fun reveillon() = (
        saveMeal(
            null,
            "e",
            MealDraft(
                "Réveillon",
                MealType.CHRISTMAS_EVE,
                LocalDate.of(2026, 12, 24),
                "Chez nous",
                setOf("lea"),
                listOf(" Oncle Paul ", "", "Oncle Paul", "Tata Jo"),
                12_000,
            ),
        ) as AppResult.Success
        ).value

    @Test
    fun `un repas compte les personnes et les invites libres`() = runTest {
        edition()
        val meal = reveillon()
        assertEquals(listOf("Oncle Paul", "Tata Jo"), meal.guests)
        assertEquals(3, meal.guestCount)
        assertEquals(12_000, meal.estimatedCostCents)
    }

    @Test
    fun `nom obligatoire, cout positif, annee archivee`() = runTest {
        edition()
        edition("old", archived = true)
        assertEquals(AppResult.Failure(AppError.InvalidInput), saveMeal(null, "e", MealDraft(" ")))
        assertEquals(AppResult.Failure(AppError.InvalidInput), saveMeal(null, "e", MealDraft("Brunch", estimatedCostCents = -1)))
        assertEquals(AppResult.Failure(AppError.ReadOnly), saveMeal(null, "old", MealDraft("Brunch")))
    }

    @Test
    fun `un plat tire d'une recette fige son nom et sa photo`() = runTest {
        edition()
        val meal = reveillon()
        cooking.saveRecipe(Recipe("r", "Bûche", "photos/buche.jpg", null, "", Instant.EPOCH, Instant.EPOCH))
        val dish = (saveDish(null, meal.id, DishDraft(CourseType.DESSERT, "r", "Bûche", broughtBy = "Mamie")) as AppResult.Success).value
        assertEquals("photos/copy-0.jpg", dish.photoPath)
        assertEquals("Mamie", dish.broughtBy)
        // Supprimer le repas supprime ses plats et leurs photos copiées.
        DeleteMealUseCase(meals, photos, guard)(meal.id)
        assertTrue(meals.dishes.value.isEmpty())
        assertEquals(listOf("photos/copy-0.jpg"), photos.deleted)
    }

    @Test
    fun `le lutin propose le menu puis la commande, et coche quand c'est fait`() = runTest {
        edition()
        val meal = reveillon()
        fun source() = SuggestionSource(
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            meals = meals.meals.value,
            dishes = meals.dishes.value,
        )
        assertEquals(SuggestionKind.MEAL_MENU, currentSuggestions(source(), emptySet(), emptySet(), emptyList()).single().kind)

        val dish = (saveDish(null, meal.id, DishDraft(CourseType.STARTER, null, "Huîtres", toOrder = true)) as AppResult.Success).value
        val suggestion = currentSuggestions(source(), emptySet(), emptySet(), emptyList()).single()
        assertEquals(SuggestionKind.DISH_ORDER, suggestion.kind)
        assertEquals("Huîtres", suggestion.subject)
        assertEquals(true, suggestionStates(source())["MEAL_MENU:${meal.id}"])
        assertEquals(false, suggestionStates(source())["DISH_ORDER:${dish.id}"])

        SetDishOrderedUseCase(meals, guard)(dish.id, true)
        assertTrue(currentSuggestions(source(), emptySet(), emptySet(), emptyList()).isEmpty())
        assertEquals(true, suggestionStates(source())["DISH_ORDER:${dish.id}"])
    }

    @Test
    fun `la recherche trouve un repas par l'un de ses plats`() = runTest {
        edition()
        val meal = reveillon()
        saveDish(null, meal.id, DishDraft(CourseType.STARTER, null, "Huîtres"))
        val data = SearchData(
            emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), mapOf("e" to 2026),
            meals = meals.meals.value, dishes = meals.dishes.value,
        )
        val result = searchAll("huitres", data)[LinkType.MEAL]!!.single()
        assertEquals("Réveillon", result.title)
        assertEquals("Huîtres · Noël 2026", result.subtitle)
    }

    @Test
    fun `un gouter n'a que ses moments, et on peut en ajouter ou en retirer`() = runTest {
        edition()
        val snack = (saveMeal(null, "e", MealDraft("Goûter", MealType.SNACK)) as AppResult.Success).value
        assertEquals(listOf(CourseType.SWEET, CourseType.DRINKS), snack.courses)
        val setCourse = SetMealCourseUseCase(meals, guard, time)
        setCourse(snack.id, CourseType.SAVORY, true)
        assertEquals(listOf(CourseType.SAVORY, CourseType.SWEET, CourseType.DRINKS), meals.getMeal(snack.id)?.courses)
        saveDish(null, snack.id, DishDraft(CourseType.SWEET, null, "Sablés"))
        // Un moment avec des plats ne se retire pas ; un moment vide, si.
        assertEquals(AppResult.Failure(AppError.InvalidInput), setCourse(snack.id, CourseType.SWEET, false))
        setCourse(snack.id, CourseType.DRINKS, false)
        assertEquals(listOf(CourseType.SAVORY, CourseType.SWEET), meals.getMeal(snack.id)?.courses)
        // Modifier le repas garde ses moments.
        saveMeal(snack.id, "e", MealDraft("Goûter des enfants", MealType.SNACK))
        assertEquals(listOf(CourseType.SAVORY, CourseType.SWEET), meals.getMeal(snack.id)?.courses)
    }
}

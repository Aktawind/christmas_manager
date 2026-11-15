package com.aktawind.monnoel.core.data.repository

import com.aktawind.monnoel.core.database.dao.MealDao
import com.aktawind.monnoel.core.database.entity.MealEntity
import com.aktawind.monnoel.core.database.entity.MealPersonEntity
import com.aktawind.monnoel.core.database.entity.asEntity
import com.aktawind.monnoel.core.database.entity.asModel
import com.aktawind.monnoel.core.domain.meals.MealRepository
import com.aktawind.monnoel.core.model.Meal
import com.aktawind.monnoel.core.model.MealDish
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoomMealRepository @Inject constructor(private val dao: MealDao) : MealRepository {
    private fun List<MealPersonEntity>.personsOf(id: String) = filter { it.mealId == id }.map { it.personId }.toSet()

    private fun withPersons(meals: Flow<List<MealEntity>>): Flow<List<Meal>> =
        combine(meals, dao.observeMealPersons()) { list, links -> list.map { it.asModel(links.personsOf(it.id)) } }

    override fun observeMeals(editionId: String) = withPersons(dao.observeMeals(editionId))

    override fun observeAllMeals() = withPersons(dao.observeAllMeals())

    override fun observeMeal(id: String): Flow<Meal?> =
        combine(dao.observeMeal(id), dao.observeMealPersons()) { m, links -> m?.asModel(links.personsOf(id)) }

    override suspend fun getMeal(id: String) = dao.getMeal(id)?.asModel(dao.getPersonIds(id).toSet())

    override suspend fun saveMeal(meal: Meal) = dao.saveMeal(meal.asEntity(), meal.personIds)

    override suspend fun deleteMeal(id: String) = dao.deleteMeal(id)

    override fun observeDishes(editionId: String) = dao.observeDishes(editionId).map { l -> l.map { it.asModel() } }

    override fun observeAllDishes() = dao.observeAllDishes().map { l -> l.map { it.asModel() } }

    override fun observeDishesOfMeal(mealId: String) = dao.observeDishesOfMeal(mealId).map { l -> l.map { it.asModel() } }

    override suspend fun getDishesOfMeal(mealId: String) = dao.getDishesOfMeal(mealId).map { it.asModel() }

    override suspend fun getDish(id: String) = dao.getDish(id)?.asModel()

    override suspend fun saveDish(dish: MealDish) = dao.upsertDish(dish.asEntity())

    override suspend fun deleteDish(id: String) = dao.deleteDish(id)
}

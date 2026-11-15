package com.aktawind.monnoel.core.domain.meals

import com.aktawind.monnoel.core.model.Meal
import com.aktawind.monnoel.core.model.MealDish
import kotlinx.coroutines.flow.Flow

interface MealRepository {
    fun observeMeals(editionId: String): Flow<List<Meal>>

    fun observeAllMeals(): Flow<List<Meal>>

    fun observeMeal(id: String): Flow<Meal?>

    suspend fun getMeal(id: String): Meal?

    suspend fun saveMeal(meal: Meal)

    suspend fun deleteMeal(id: String)

    /** Tous les plats des repas d'une année. */
    fun observeDishes(editionId: String): Flow<List<MealDish>>

    fun observeAllDishes(): Flow<List<MealDish>>

    fun observeDishesOfMeal(mealId: String): Flow<List<MealDish>>

    suspend fun getDishesOfMeal(mealId: String): List<MealDish>

    suspend fun getDish(id: String): MealDish?

    suspend fun saveDish(dish: MealDish)

    suspend fun deleteDish(id: String)
}

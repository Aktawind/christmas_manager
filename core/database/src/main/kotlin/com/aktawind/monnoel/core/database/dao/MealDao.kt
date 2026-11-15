package com.aktawind.monnoel.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.aktawind.monnoel.core.database.entity.MealDishEntity
import com.aktawind.monnoel.core.database.entity.MealEntity
import com.aktawind.monnoel.core.database.entity.MealPersonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Query("SELECT * FROM meal WHERE edition_id = :editionId ORDER BY date IS NULL, date, created_at")
    fun observeMeals(editionId: String): Flow<List<MealEntity>>

    @Query("SELECT * FROM meal ORDER BY date IS NULL, date, created_at")
    fun observeAllMeals(): Flow<List<MealEntity>>

    @Query("SELECT * FROM meal")
    suspend fun getAllMeals(): List<MealEntity>

    @Query("SELECT * FROM meal WHERE id = :id")
    suspend fun getMeal(id: String): MealEntity?

    @Query("SELECT * FROM meal WHERE id = :id")
    fun observeMeal(id: String): Flow<MealEntity?>

    @Query("SELECT * FROM meal_person")
    fun observeMealPersons(): Flow<List<MealPersonEntity>>

    @Query("SELECT * FROM meal_person")
    suspend fun getAllMealPersons(): List<MealPersonEntity>

    @Query("SELECT person_id FROM meal_person WHERE meal_id = :mealId")
    suspend fun getPersonIds(mealId: String): List<String>

    @Upsert
    suspend fun upsertMeal(entity: MealEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMeals(entities: List<MealEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMealPersons(links: List<MealPersonEntity>)

    @Query("DELETE FROM meal_person WHERE meal_id = :mealId")
    suspend fun deleteMealPersons(mealId: String)

    @Transaction
    suspend fun saveMeal(entity: MealEntity, personIds: Set<String>) {
        upsertMeal(entity)
        deleteMealPersons(entity.id)
        insertMealPersons(personIds.map { MealPersonEntity(entity.id, it) })
    }

    @Query("DELETE FROM meal WHERE id = :id")
    suspend fun deleteMeal(id: String)

    @Query("DELETE FROM meal")
    suspend fun deleteAllMeals()

    @Query("SELECT d.* FROM meal_dish d JOIN meal m ON d.meal_id = m.id WHERE m.edition_id = :editionId ORDER BY d.sort_order, d.created_at")
    fun observeDishes(editionId: String): Flow<List<MealDishEntity>>

    @Query("SELECT * FROM meal_dish ORDER BY sort_order, created_at")
    fun observeAllDishes(): Flow<List<MealDishEntity>>

    @Query("SELECT * FROM meal_dish WHERE meal_id = :mealId ORDER BY sort_order, created_at")
    fun observeDishesOfMeal(mealId: String): Flow<List<MealDishEntity>>

    @Query("SELECT * FROM meal_dish WHERE meal_id = :mealId")
    suspend fun getDishesOfMeal(mealId: String): List<MealDishEntity>

    @Query("SELECT * FROM meal_dish")
    suspend fun getAllDishes(): List<MealDishEntity>

    @Query("SELECT * FROM meal_dish WHERE id = :id")
    suspend fun getDish(id: String): MealDishEntity?

    @Upsert
    suspend fun upsertDish(entity: MealDishEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDishes(entities: List<MealDishEntity>)

    @Query("DELETE FROM meal_dish WHERE id = :id")
    suspend fun deleteDish(id: String)

    @Query("DELETE FROM meal_dish")
    suspend fun deleteAllDishes()
}

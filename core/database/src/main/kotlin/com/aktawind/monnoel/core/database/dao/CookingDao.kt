package com.aktawind.monnoel.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.aktawind.monnoel.core.database.entity.ProductionEntity
import com.aktawind.monnoel.core.database.entity.RecipeCategoryEntity
import com.aktawind.monnoel.core.database.entity.RecipeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CookingDao {
    @Query("SELECT * FROM recipe ORDER BY name COLLATE NOCASE")
    fun observeRecipes(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipe")
    suspend fun getAllRecipes(): List<RecipeEntity>

    @Query("SELECT * FROM recipe WHERE id = :id")
    suspend fun getRecipe(id: String): RecipeEntity?

    @Upsert
    suspend fun upsertRecipe(entity: RecipeEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecipes(entities: List<RecipeEntity>)

    @Query("DELETE FROM recipe WHERE id = :id")
    suspend fun deleteRecipe(id: String)

    @Query("DELETE FROM recipe")
    suspend fun deleteAllRecipes()

    @Query("SELECT * FROM recipe_category ORDER BY sort_order, name COLLATE NOCASE")
    fun observeCategories(): Flow<List<RecipeCategoryEntity>>

    @Query("SELECT * FROM recipe_category")
    suspend fun getAllCategories(): List<RecipeCategoryEntity>

    @Upsert
    suspend fun upsertCategory(entity: RecipeCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategories(entities: List<RecipeCategoryEntity>)

    @Query("DELETE FROM recipe_category WHERE id = :id")
    suspend fun deleteCategory(id: String)

    @Query("DELETE FROM recipe_category")
    suspend fun deleteAllCategories()

    @Query("SELECT * FROM production WHERE edition_id = :editionId ORDER BY created_at")
    fun observeProductions(editionId: String): Flow<List<ProductionEntity>>

    @Query("SELECT * FROM production ORDER BY created_at")
    fun observeAllProductions(): Flow<List<ProductionEntity>>

    @Query("SELECT * FROM production")
    suspend fun getAllProductions(): List<ProductionEntity>

    @Query("SELECT * FROM production WHERE id = :id")
    suspend fun getProduction(id: String): ProductionEntity?

    @Upsert
    suspend fun upsertProduction(entity: ProductionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProductions(entities: List<ProductionEntity>)

    @Query("DELETE FROM production WHERE id = :id")
    suspend fun deleteProduction(id: String)

    @Query("DELETE FROM production")
    suspend fun deleteAllProductions()
}

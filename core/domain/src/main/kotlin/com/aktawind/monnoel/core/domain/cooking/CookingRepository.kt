package com.aktawind.monnoel.core.domain.cooking

import com.aktawind.monnoel.core.model.Production
import com.aktawind.monnoel.core.model.Recipe
import com.aktawind.monnoel.core.model.RecipeCategory
import kotlinx.coroutines.flow.Flow

interface CookingRepository {
    fun observeRecipes(): Flow<List<Recipe>>

    suspend fun getRecipe(id: String): Recipe?

    suspend fun saveRecipe(recipe: Recipe)

    suspend fun deleteRecipe(id: String)

    fun observeCategories(): Flow<List<RecipeCategory>>

    suspend fun saveCategory(category: RecipeCategory)

    suspend fun deleteCategory(id: String)

    fun observeProductions(editionId: String): Flow<List<Production>>

    fun observeAllProductions(): Flow<List<Production>>

    suspend fun getProduction(id: String): Production?

    suspend fun saveProduction(production: Production)

    suspend fun deleteProduction(id: String)
}

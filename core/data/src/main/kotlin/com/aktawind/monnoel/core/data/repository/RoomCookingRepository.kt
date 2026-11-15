package com.aktawind.monnoel.core.data.repository

import com.aktawind.monnoel.core.database.dao.CookingDao
import com.aktawind.monnoel.core.database.entity.asEntity
import com.aktawind.monnoel.core.database.entity.asModel
import com.aktawind.monnoel.core.domain.cooking.CookingRepository
import com.aktawind.monnoel.core.model.Production
import com.aktawind.monnoel.core.model.Recipe
import com.aktawind.monnoel.core.model.RecipeCategory
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class RoomCookingRepository @Inject constructor(private val dao: CookingDao) : CookingRepository {
    override fun observeRecipes() = dao.observeRecipes().map { l -> l.map { it.asModel() } }

    override suspend fun getRecipe(id: String) = dao.getRecipe(id)?.asModel()

    override suspend fun saveRecipe(recipe: Recipe) = dao.upsertRecipe(recipe.asEntity())

    override suspend fun deleteRecipe(id: String) = dao.deleteRecipe(id)

    override fun observeCategories() = dao.observeCategories().map { l -> l.map { it.asModel() } }

    override suspend fun saveCategory(category: RecipeCategory) = dao.upsertCategory(category.asEntity())

    override suspend fun deleteCategory(id: String) = dao.deleteCategory(id)

    override fun observeProductions(editionId: String) = dao.observeProductions(editionId).map { l -> l.map { it.asModel() } }

    override fun observeAllProductions() = dao.observeAllProductions().map { l -> l.map { it.asModel() } }

    override suspend fun getProduction(id: String) = dao.getProduction(id)?.asModel()

    override suspend fun saveProduction(production: Production) = dao.upsertProduction(production.asEntity())

    override suspend fun deleteProduction(id: String) = dao.deleteProduction(id)
}

package com.aktawind.monnoel.core.data.repository

import com.aktawind.monnoel.core.database.dao.CraftDao
import com.aktawind.monnoel.core.database.entity.CraftSupplyBoughtEntity
import com.aktawind.monnoel.core.database.entity.asEntity
import com.aktawind.monnoel.core.database.entity.asModel
import com.aktawind.monnoel.core.domain.traditions.CraftRepository
import com.aktawind.monnoel.core.model.Craft
import com.aktawind.monnoel.core.model.CraftSupply
import com.aktawind.monnoel.core.model.CraftYear
import com.aktawind.monnoel.core.model.DecorIdea
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class RoomCraftRepository @Inject constructor(private val dao: CraftDao) : CraftRepository {
    override fun observeCrafts() = dao.observeCrafts().map { l -> l.map { it.asModel() } }

    override suspend fun getCraft(id: String) = dao.getCraft(id)?.asModel()

    override suspend fun saveCraft(craft: Craft) = dao.upsertCraft(craft.asEntity())

    override suspend fun deleteCraft(id: String) = dao.deleteCraft(id)

    override fun observeSupplies() = dao.observeSupplies().map { l -> l.map { it.asModel() } }

    override suspend fun getSupplies(craftId: String) = dao.getSupplies(craftId).map { it.asModel() }

    override suspend fun saveSupply(supply: CraftSupply) = dao.upsertSupply(supply.asEntity())

    override suspend fun deleteSupply(id: String) = dao.deleteSupply(id)

    override fun observeYears() = dao.observeAllYears().map { l -> l.map { it.asModel() } }

    override suspend fun getYear(editionId: String, craftId: String) = dao.getYear(editionId, craftId)?.asModel()

    override suspend fun getYearsOf(craftId: String) = dao.getYearsOf(craftId).map { it.asModel() }

    override suspend fun saveYear(year: CraftYear) = dao.upsertYear(year.asEntity())

    override suspend fun deleteYear(id: String) = dao.deleteYear(id)

    override fun observeBought() = dao.observeBought().map { l -> l.map { it.editionId to it.supplyId }.toSet() }

    override suspend fun setBought(editionId: String, supplyId: String, bought: Boolean) {
        if (bought) dao.insertBought(listOf(CraftSupplyBoughtEntity(editionId, supplyId))) else dao.deleteBought(editionId, supplyId)
    }

    override fun observeDecor() = dao.observeDecor().map { l -> l.map { it.asModel() } }

    override suspend fun getDecor(id: String) = dao.getDecor(id)?.asModel()

    override suspend fun saveDecor(decor: DecorIdea) = dao.upsertDecor(decor.asEntity())

    override suspend fun deleteDecor(id: String) = dao.deleteDecor(id)
}

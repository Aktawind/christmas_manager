package com.aktawind.monnoel.core.domain.traditions

import com.aktawind.monnoel.core.model.Craft
import com.aktawind.monnoel.core.model.CraftSupply
import com.aktawind.monnoel.core.model.CraftYear
import com.aktawind.monnoel.core.model.DecorIdea
import kotlinx.coroutines.flow.Flow

interface CraftRepository {
    fun observeCrafts(): Flow<List<Craft>>

    suspend fun getCraft(id: String): Craft?

    suspend fun saveCraft(craft: Craft)

    suspend fun deleteCraft(id: String)

    fun observeSupplies(): Flow<List<CraftSupply>>

    suspend fun getSupplies(craftId: String): List<CraftSupply>

    suspend fun saveSupply(supply: CraftSupply)

    suspend fun deleteSupply(id: String)

    fun observeYears(): Flow<List<CraftYear>>

    suspend fun getYear(editionId: String, craftId: String): CraftYear?

    suspend fun getYearsOf(craftId: String): List<CraftYear>

    suspend fun saveYear(year: CraftYear)

    suspend fun deleteYear(id: String)

    /** Fournitures achetées, par Noël : paires (édition, fourniture). */
    fun observeBought(): Flow<Set<Pair<String, String>>>

    suspend fun setBought(editionId: String, supplyId: String, bought: Boolean)

    fun observeDecor(): Flow<List<DecorIdea>>

    suspend fun getDecor(id: String): DecorIdea?

    suspend fun saveDecor(decor: DecorIdea)

    suspend fun deleteDecor(id: String)
}

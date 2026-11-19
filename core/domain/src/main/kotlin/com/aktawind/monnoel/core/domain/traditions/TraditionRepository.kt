package com.aktawind.monnoel.core.domain.traditions

import com.aktawind.monnoel.core.model.Inspiration
import com.aktawind.monnoel.core.model.Tradition
import com.aktawind.monnoel.core.model.TraditionYear
import kotlinx.coroutines.flow.Flow

interface TraditionRepository {
    fun observeTraditions(): Flow<List<Tradition>>

    suspend fun getTradition(id: String): Tradition?

    suspend fun saveTradition(tradition: Tradition)

    suspend fun deleteTradition(id: String)

    /** Programme d'un Noël : les traditions retenues cette année-là. */
    fun observeYears(editionId: String): Flow<List<TraditionYear>>

    fun observeAllYears(): Flow<List<TraditionYear>>

    suspend fun getYearsOf(traditionId: String): List<TraditionYear>

    suspend fun getYear(editionId: String, traditionId: String): TraditionYear?

    suspend fun saveYear(year: TraditionYear)

    suspend fun deleteYear(id: String)

    fun observeInspirations(): Flow<List<Inspiration>>

    suspend fun getInspiration(id: String): Inspiration?

    suspend fun saveInspiration(inspiration: Inspiration)

    suspend fun deleteInspiration(id: String)

    /** Inspirations vues / faites pendant un Noël. */
    fun observeDone(editionId: String): Flow<Set<String>>

    suspend fun setDone(editionId: String, inspirationId: String, done: Boolean)
}

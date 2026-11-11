package com.aktawind.monnoel.core.domain.repository

import com.aktawind.monnoel.core.model.ChristmasEdition
import kotlinx.coroutines.flow.Flow

interface EditionRepository {
    /** Toutes les années, de la plus récente à la plus ancienne. */
    fun observeEditions(): Flow<List<ChristmasEdition>>

    suspend fun getById(id: String): ChristmasEdition?

    suspend fun getByYear(year: Int): ChristmasEdition?

    suspend fun insert(edition: ChristmasEdition)

    suspend fun update(edition: ChristmasEdition)

    suspend fun delete(id: String)
}

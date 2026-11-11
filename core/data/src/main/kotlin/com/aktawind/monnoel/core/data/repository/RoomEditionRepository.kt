package com.aktawind.monnoel.core.data.repository

import com.aktawind.monnoel.core.database.dao.EditionDao
import com.aktawind.monnoel.core.database.entity.asEntity
import com.aktawind.monnoel.core.database.entity.asModel
import com.aktawind.monnoel.core.domain.repository.EditionRepository
import com.aktawind.monnoel.core.model.ChristmasEdition
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomEditionRepository @Inject constructor(private val dao: EditionDao) : EditionRepository {
    override fun observeEditions(): Flow<List<ChristmasEdition>> =
        dao.observeAll().map { list -> list.map { it.asModel() } }

    override suspend fun getById(id: String) = dao.getById(id)?.asModel()

    override suspend fun getByYear(year: Int) = dao.getByYear(year)?.asModel()

    override suspend fun insert(edition: ChristmasEdition) = dao.insert(edition.asEntity())

    override suspend fun update(edition: ChristmasEdition) = dao.update(edition.asEntity())

    override suspend fun delete(id: String) = dao.delete(id)
}

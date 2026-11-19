package com.aktawind.monnoel.core.data.repository

import com.aktawind.monnoel.core.database.dao.TraditionDao
import com.aktawind.monnoel.core.database.entity.InspirationDoneEntity
import com.aktawind.monnoel.core.database.entity.asEntity
import com.aktawind.monnoel.core.database.entity.asModel
import com.aktawind.monnoel.core.domain.traditions.TraditionRepository
import com.aktawind.monnoel.core.model.Inspiration
import com.aktawind.monnoel.core.model.Tradition
import com.aktawind.monnoel.core.model.TraditionYear
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoomTraditionRepository @Inject constructor(private val dao: TraditionDao) : TraditionRepository {
    override fun observeTraditions(): Flow<List<Tradition>> = combine(dao.observeTraditions(), dao.observeTraditionPersons()) {
            list,
            links,
        ->
        val byTradition = links.groupBy({ it.traditionId }, { it.personId })
        list.map { it.asModel(byTradition[it.id].orEmpty().toSet()) }
    }

    override suspend fun getTradition(id: String) = dao.getTradition(id)?.asModel(dao.getPersonIds(id).toSet())

    override suspend fun saveTradition(tradition: Tradition) = dao.saveTradition(tradition.asEntity(), tradition.personIds)

    override suspend fun deleteTradition(id: String) = dao.deleteTradition(id)

    override fun observeYears(editionId: String) = dao.observeYears(editionId).map { l -> l.map { it.asModel() } }

    override fun observeAllYears() = dao.observeAllYears().map { l -> l.map { it.asModel() } }

    override suspend fun getYearsOf(traditionId: String) = dao.getYearsOf(traditionId).map { it.asModel() }

    override suspend fun getYear(editionId: String, traditionId: String) = dao.getYear(editionId, traditionId)?.asModel()

    override suspend fun saveYear(year: TraditionYear) = dao.upsertYear(year.asEntity())

    override suspend fun deleteYear(id: String) = dao.deleteYear(id)

    override fun observeInspirations() = dao.observeInspirations().map { l -> l.map { it.asModel() } }

    override suspend fun getInspiration(id: String) = dao.getInspiration(id)?.asModel()

    override suspend fun saveInspiration(inspiration: Inspiration) = dao.upsertInspiration(inspiration.asEntity())

    override suspend fun deleteInspiration(id: String) = dao.deleteInspiration(id)

    override fun observeDone(editionId: String) = dao.observeDone(editionId).map { it.toSet() }

    override suspend fun setDone(editionId: String, inspirationId: String, done: Boolean) {
        if (done) dao.insertDone(listOf(InspirationDoneEntity(editionId, inspirationId))) else dao.deleteDone(editionId, inspirationId)
    }
}

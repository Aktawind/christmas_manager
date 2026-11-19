package com.aktawind.monnoel.core.data.repository

import com.aktawind.monnoel.core.database.dao.SpecialDateDao
import com.aktawind.monnoel.core.database.entity.asEntity
import com.aktawind.monnoel.core.database.entity.asModel
import com.aktawind.monnoel.core.domain.countdown.SpecialDateRepository
import com.aktawind.monnoel.core.model.SpecialDate
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class RoomSpecialDateRepository @Inject constructor(private val dao: SpecialDateDao) : SpecialDateRepository {
    override fun observeDates() = dao.observeAll().map { l -> l.map { it.asModel() } }

    override suspend fun saveDate(date: SpecialDate) = dao.upsert(date.asEntity())

    override suspend fun deleteDate(id: String) = dao.delete(id)
}

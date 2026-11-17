package com.aktawind.monnoel.core.data.repository

import com.aktawind.monnoel.core.database.dao.WhisperDao
import com.aktawind.monnoel.core.database.entity.asEntity
import com.aktawind.monnoel.core.database.entity.asModel
import com.aktawind.monnoel.core.domain.gifts.WhisperRepository
import com.aktawind.monnoel.core.model.WhisperedIdea
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class RoomWhisperRepository @Inject constructor(private val dao: WhisperDao) : WhisperRepository {
    override fun observeWhispers(editionId: String) = dao.observeWhispers(editionId).map { l -> l.map { it.asModel() } }

    override fun observeAllWhispers() = dao.observeAll().map { l -> l.map { it.asModel() } }

    override suspend fun getWhisper(id: String) = dao.get(id)?.asModel()

    override suspend fun saveWhisper(whisper: WhisperedIdea) = dao.upsert(whisper.asEntity())

    override suspend fun deleteWhisper(id: String) = dao.delete(id)
}

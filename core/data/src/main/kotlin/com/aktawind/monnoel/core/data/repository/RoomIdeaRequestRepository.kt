package com.aktawind.monnoel.core.data.repository

import com.aktawind.monnoel.core.database.dao.IdeaRequestDao
import com.aktawind.monnoel.core.database.entity.asEntity
import com.aktawind.monnoel.core.database.entity.asModel
import com.aktawind.monnoel.core.domain.gifts.IdeaRequestRepository
import com.aktawind.monnoel.core.model.IdeaRequest
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class RoomIdeaRequestRepository @Inject constructor(private val dao: IdeaRequestDao) : IdeaRequestRepository {
    override fun observeRequests(editionId: String) = dao.observeRequests(editionId).map { l -> l.map { it.asModel() } }

    override suspend fun getRequest(id: String) = dao.get(id)?.asModel()

    override suspend fun saveRequest(request: IdeaRequest) = dao.upsert(request.asEntity())

    override suspend fun deleteRequest(id: String) = dao.delete(id)
}

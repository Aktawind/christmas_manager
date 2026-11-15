package com.aktawind.monnoel.core.data.repository

import com.aktawind.monnoel.core.database.dao.TaskDao
import com.aktawind.monnoel.core.database.entity.DismissedSuggestionEntity
import com.aktawind.monnoel.core.database.entity.asEntity
import com.aktawind.monnoel.core.database.entity.asModel
import com.aktawind.monnoel.core.domain.tasks.TaskRepository
import com.aktawind.monnoel.core.model.LutinTask
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTaskRepository @Inject constructor(private val dao: TaskDao) : TaskRepository {
    override fun observeTasks(
        editionId: String,
    ): Flow<List<LutinTask>> = dao.observeForEdition(editionId).map { l -> l.map { it.asModel() } }

    override fun observeAllTasks(): Flow<List<LutinTask>> = dao.observeAll().map { l -> l.map { it.asModel() } }

    override fun observeTask(id: String): Flow<LutinTask?> = dao.observe(id).map { it?.asModel() }

    override suspend fun getTask(id: String) = dao.get(id)?.asModel()

    override suspend fun getTasks(editionId: String) = dao.getForEdition(editionId).map { it.asModel() }

    override suspend fun save(task: LutinTask) = dao.upsert(listOf(task.asEntity()))

    override suspend fun saveAll(tasks: List<LutinTask>) = dao.upsert(tasks.map { it.asEntity() })

    override suspend fun delete(id: String) = dao.delete(id)

    override fun observeDismissed(editionId: String): Flow<Set<String>> = dao.observeDismissed(editionId).map { it.toSet() }

    override suspend fun dismiss(editionId: String, key: String) = dao.insertDismissed(listOf(DismissedSuggestionEntity(editionId, key)))
}

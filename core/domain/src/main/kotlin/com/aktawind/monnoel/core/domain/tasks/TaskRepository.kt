package com.aktawind.monnoel.core.domain.tasks

import com.aktawind.monnoel.core.model.LutinTask
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(editionId: String): Flow<List<LutinTask>>

    fun observeAllTasks(): Flow<List<LutinTask>>

    fun observeTask(id: String): Flow<LutinTask?>

    suspend fun getTask(id: String): LutinTask?

    suspend fun getTasks(editionId: String): List<LutinTask>

    suspend fun save(task: LutinTask)

    suspend fun saveAll(tasks: List<LutinTask>)

    suspend fun delete(id: String)

    /** Suggestions supprimées par l'utilisatrice pour un Noël (elles ne reviennent pas). */
    fun observeDismissed(editionId: String): Flow<Set<String>>

    suspend fun dismiss(editionId: String, key: String)
}

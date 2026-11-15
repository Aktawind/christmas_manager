package com.aktawind.monnoel.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.aktawind.monnoel.core.database.entity.DismissedSuggestionEntity
import com.aktawind.monnoel.core.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM lutin_task WHERE edition_id = :editionId ORDER BY created_at")
    fun observeForEdition(editionId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM lutin_task ORDER BY created_at")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM lutin_task WHERE id = :id")
    fun observe(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM lutin_task WHERE id = :id")
    suspend fun get(id: String): TaskEntity?

    @Query("SELECT * FROM lutin_task WHERE edition_id = :editionId")
    suspend fun getForEdition(editionId: String): List<TaskEntity>

    @Query("SELECT * FROM lutin_task")
    suspend fun getAll(): List<TaskEntity>

    @Upsert
    suspend fun upsert(entities: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<TaskEntity>)

    @Query("DELETE FROM lutin_task WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM lutin_task")
    suspend fun deleteAll()

    @Query("SELECT suggestion_key FROM dismissed_suggestion WHERE edition_id = :editionId")
    fun observeDismissed(editionId: String): Flow<List<String>>

    @Query("SELECT * FROM dismissed_suggestion")
    suspend fun getAllDismissed(): List<DismissedSuggestionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDismissed(entities: List<DismissedSuggestionEntity>)

    @Query("DELETE FROM dismissed_suggestion")
    suspend fun deleteAllDismissed()
}

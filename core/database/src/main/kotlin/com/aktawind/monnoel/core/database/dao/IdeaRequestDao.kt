package com.aktawind.monnoel.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.aktawind.monnoel.core.database.entity.IdeaRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeaRequestDao {
    @Query("SELECT * FROM idea_request WHERE edition_id = :editionId ORDER BY created_at")
    fun observeRequests(editionId: String): Flow<List<IdeaRequestEntity>>

    @Query("SELECT * FROM idea_request")
    suspend fun getAll(): List<IdeaRequestEntity>

    @Query("SELECT * FROM idea_request WHERE id = :id")
    suspend fun get(id: String): IdeaRequestEntity?

    @Upsert
    suspend fun upsert(entity: IdeaRequestEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<IdeaRequestEntity>)

    @Query("DELETE FROM idea_request WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM idea_request")
    suspend fun deleteAll()
}

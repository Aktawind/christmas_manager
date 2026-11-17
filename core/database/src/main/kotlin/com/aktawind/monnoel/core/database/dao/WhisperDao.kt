package com.aktawind.monnoel.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.aktawind.monnoel.core.database.entity.WhisperEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WhisperDao {
    @Query("SELECT * FROM whispered_idea WHERE edition_id = :editionId ORDER BY created_at")
    fun observeWhispers(editionId: String): Flow<List<WhisperEntity>>

    @Query("SELECT * FROM whispered_idea ORDER BY created_at")
    fun observeAll(): Flow<List<WhisperEntity>>

    @Query("SELECT * FROM whispered_idea")
    suspend fun getAll(): List<WhisperEntity>

    @Query("SELECT * FROM whispered_idea WHERE id = :id")
    suspend fun get(id: String): WhisperEntity?

    @Upsert
    suspend fun upsert(entity: WhisperEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<WhisperEntity>)

    @Query("DELETE FROM whispered_idea WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM whispered_idea")
    suspend fun deleteAll()
}

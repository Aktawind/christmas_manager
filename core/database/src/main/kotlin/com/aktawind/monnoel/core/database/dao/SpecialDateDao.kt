package com.aktawind.monnoel.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.aktawind.monnoel.core.database.entity.SpecialDateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpecialDateDao {
    @Query("SELECT * FROM special_date")
    fun observeAll(): Flow<List<SpecialDateEntity>>

    @Query("SELECT * FROM special_date")
    suspend fun getAll(): List<SpecialDateEntity>

    @Upsert
    suspend fun upsert(entity: SpecialDateEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<SpecialDateEntity>)

    @Query("DELETE FROM special_date WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM special_date")
    suspend fun deleteAll()
}

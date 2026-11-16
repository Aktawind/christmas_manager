package com.aktawind.monnoel.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.aktawind.monnoel.core.database.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expense WHERE edition_id = :editionId ORDER BY created_at")
    fun observeExpenses(editionId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expense ORDER BY created_at")
    fun observeAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expense")
    suspend fun getAll(): List<ExpenseEntity>

    @Query("SELECT * FROM expense WHERE id = :id")
    suspend fun get(id: String): ExpenseEntity?

    @Upsert
    suspend fun upsert(entity: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<ExpenseEntity>)

    @Query("DELETE FROM expense WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM expense")
    suspend fun deleteAll()
}

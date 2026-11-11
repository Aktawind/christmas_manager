package com.aktawind.monnoel.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aktawind.monnoel.core.database.entity.EditionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EditionDao {
    @Query("SELECT * FROM christmas_edition ORDER BY year DESC")
    fun observeAll(): Flow<List<EditionEntity>>

    @Query("SELECT * FROM christmas_edition ORDER BY year DESC")
    suspend fun getAll(): List<EditionEntity>

    @Query("SELECT * FROM christmas_edition WHERE id = :id")
    suspend fun getById(id: String): EditionEntity?

    @Query("SELECT * FROM christmas_edition WHERE year = :year")
    suspend fun getByYear(year: Int): EditionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: EditionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<EditionEntity>)

    @Update
    suspend fun update(entity: EditionEntity)

    @Query("DELETE FROM christmas_edition WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM christmas_edition")
    suspend fun deleteAll()
}

package com.aktawind.monnoel.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.aktawind.monnoel.core.database.entity.InspirationDoneEntity
import com.aktawind.monnoel.core.database.entity.InspirationEntity
import com.aktawind.monnoel.core.database.entity.TraditionEntity
import com.aktawind.monnoel.core.database.entity.TraditionPersonEntity
import com.aktawind.monnoel.core.database.entity.TraditionYearEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TraditionDao {
    @Query("SELECT * FROM tradition ORDER BY favorite DESC, name COLLATE NOCASE")
    fun observeTraditions(): Flow<List<TraditionEntity>>

    @Query("SELECT * FROM tradition")
    suspend fun getAllTraditions(): List<TraditionEntity>

    @Query("SELECT * FROM tradition WHERE id = :id")
    suspend fun getTradition(id: String): TraditionEntity?

    @Query("SELECT * FROM tradition_person")
    fun observeTraditionPersons(): Flow<List<TraditionPersonEntity>>

    @Query("SELECT * FROM tradition_person")
    suspend fun getAllTraditionPersons(): List<TraditionPersonEntity>

    @Query("SELECT person_id FROM tradition_person WHERE tradition_id = :id")
    suspend fun getPersonIds(id: String): List<String>

    @Upsert
    suspend fun upsertTradition(entity: TraditionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTraditions(entities: List<TraditionEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTraditionPersons(links: List<TraditionPersonEntity>)

    @Query("DELETE FROM tradition_person WHERE tradition_id = :id")
    suspend fun deleteTraditionPersons(id: String)

    @Transaction
    suspend fun saveTradition(entity: TraditionEntity, personIds: Set<String>) {
        upsertTradition(entity)
        deleteTraditionPersons(entity.id)
        insertTraditionPersons(personIds.map { TraditionPersonEntity(entity.id, it) })
    }

    @Query("DELETE FROM tradition WHERE id = :id")
    suspend fun deleteTradition(id: String)

    @Query("DELETE FROM tradition")
    suspend fun deleteAllTraditions()

    @Query("SELECT * FROM tradition_year WHERE edition_id = :editionId")
    fun observeYears(editionId: String): Flow<List<TraditionYearEntity>>

    @Query("SELECT * FROM tradition_year")
    fun observeAllYears(): Flow<List<TraditionYearEntity>>

    @Query("SELECT * FROM tradition_year")
    suspend fun getAllYears(): List<TraditionYearEntity>

    @Query("SELECT * FROM tradition_year WHERE tradition_id = :traditionId")
    suspend fun getYearsOf(traditionId: String): List<TraditionYearEntity>

    @Query("SELECT * FROM tradition_year WHERE edition_id = :editionId AND tradition_id = :traditionId")
    suspend fun getYear(editionId: String, traditionId: String): TraditionYearEntity?

    @Upsert
    suspend fun upsertYear(entity: TraditionYearEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertYears(entities: List<TraditionYearEntity>)

    @Query("DELETE FROM tradition_year WHERE id = :id")
    suspend fun deleteYear(id: String)

    @Query("DELETE FROM tradition_year")
    suspend fun deleteAllYears()

    @Query("SELECT * FROM inspiration ORDER BY title COLLATE NOCASE")
    fun observeInspirations(): Flow<List<InspirationEntity>>

    @Query("SELECT * FROM inspiration")
    suspend fun getAllInspirations(): List<InspirationEntity>

    @Query("SELECT * FROM inspiration WHERE id = :id")
    suspend fun getInspiration(id: String): InspirationEntity?

    @Upsert
    suspend fun upsertInspiration(entity: InspirationEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInspirations(entities: List<InspirationEntity>)

    @Query("DELETE FROM inspiration WHERE id = :id")
    suspend fun deleteInspiration(id: String)

    @Query("DELETE FROM inspiration")
    suspend fun deleteAllInspirations()

    @Query("SELECT inspiration_id FROM inspiration_done WHERE edition_id = :editionId")
    fun observeDone(editionId: String): Flow<List<String>>

    @Query("SELECT * FROM inspiration_done")
    suspend fun getAllDone(): List<InspirationDoneEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDone(entities: List<InspirationDoneEntity>)

    @Query("DELETE FROM inspiration_done WHERE edition_id = :editionId AND inspiration_id = :inspirationId")
    suspend fun deleteDone(editionId: String, inspirationId: String)

    @Query("DELETE FROM inspiration_done")
    suspend fun deleteAllDone()
}

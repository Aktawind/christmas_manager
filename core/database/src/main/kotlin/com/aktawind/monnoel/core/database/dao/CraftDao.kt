package com.aktawind.monnoel.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.aktawind.monnoel.core.database.entity.CraftEntity
import com.aktawind.monnoel.core.database.entity.CraftSupplyBoughtEntity
import com.aktawind.monnoel.core.database.entity.CraftSupplyEntity
import com.aktawind.monnoel.core.database.entity.CraftYearEntity
import com.aktawind.monnoel.core.database.entity.DecorIdeaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CraftDao {
    @Query("SELECT * FROM craft ORDER BY title COLLATE NOCASE")
    fun observeCrafts(): Flow<List<CraftEntity>>

    @Query("SELECT * FROM craft")
    suspend fun getAllCrafts(): List<CraftEntity>

    @Query("SELECT * FROM craft WHERE id = :id")
    suspend fun getCraft(id: String): CraftEntity?

    @Upsert
    suspend fun upsertCraft(entity: CraftEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCrafts(entities: List<CraftEntity>)

    @Query("DELETE FROM craft WHERE id = :id")
    suspend fun deleteCraft(id: String)

    @Query("DELETE FROM craft")
    suspend fun deleteAllCrafts()

    @Query("SELECT * FROM craft_supply ORDER BY sort_order")
    fun observeSupplies(): Flow<List<CraftSupplyEntity>>

    @Query("SELECT * FROM craft_supply")
    suspend fun getAllSupplies(): List<CraftSupplyEntity>

    @Query("SELECT * FROM craft_supply WHERE craft_id = :craftId ORDER BY sort_order")
    suspend fun getSupplies(craftId: String): List<CraftSupplyEntity>

    @Upsert
    suspend fun upsertSupply(entity: CraftSupplyEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSupplies(entities: List<CraftSupplyEntity>)

    @Query("DELETE FROM craft_supply WHERE id = :id")
    suspend fun deleteSupply(id: String)

    @Query("SELECT * FROM craft_year")
    fun observeAllYears(): Flow<List<CraftYearEntity>>

    @Query("SELECT * FROM craft_year")
    suspend fun getAllYears(): List<CraftYearEntity>

    @Query("SELECT * FROM craft_year WHERE edition_id = :editionId AND craft_id = :craftId")
    suspend fun getYear(editionId: String, craftId: String): CraftYearEntity?

    @Query("SELECT * FROM craft_year WHERE craft_id = :craftId")
    suspend fun getYearsOf(craftId: String): List<CraftYearEntity>

    @Upsert
    suspend fun upsertYear(entity: CraftYearEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertYears(entities: List<CraftYearEntity>)

    @Query("DELETE FROM craft_year WHERE id = :id")
    suspend fun deleteYear(id: String)

    @Query("SELECT * FROM craft_supply_bought")
    fun observeBought(): Flow<List<CraftSupplyBoughtEntity>>

    @Query("SELECT * FROM craft_supply_bought")
    suspend fun getAllBought(): List<CraftSupplyBoughtEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBought(entities: List<CraftSupplyBoughtEntity>)

    @Query("DELETE FROM craft_supply_bought WHERE edition_id = :editionId AND supply_id = :supplyId")
    suspend fun deleteBought(editionId: String, supplyId: String)

    @Query("SELECT * FROM decor_idea ORDER BY favorite DESC, created_at DESC")
    fun observeDecor(): Flow<List<DecorIdeaEntity>>

    @Query("SELECT * FROM decor_idea")
    suspend fun getAllDecor(): List<DecorIdeaEntity>

    @Query("SELECT * FROM decor_idea WHERE id = :id")
    suspend fun getDecor(id: String): DecorIdeaEntity?

    @Upsert
    suspend fun upsertDecor(entity: DecorIdeaEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDecor(entities: List<DecorIdeaEntity>)

    @Query("DELETE FROM decor_idea WHERE id = :id")
    suspend fun deleteDecor(id: String)

    @Query("DELETE FROM decor_idea")
    suspend fun deleteAllDecor()
}

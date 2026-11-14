package com.aktawind.monnoel.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.aktawind.monnoel.core.database.entity.GiftCategoryEntity
import com.aktawind.monnoel.core.database.entity.GiftIdeaEntity
import com.aktawind.monnoel.core.database.entity.IdeaPersonEntity
import com.aktawind.monnoel.core.database.entity.PersonBudgetEntity
import com.aktawind.monnoel.core.database.entity.PersonEntity
import com.aktawind.monnoel.core.database.entity.PlannedGiftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query("SELECT * FROM person ORDER BY first_name COLLATE NOCASE, last_name COLLATE NOCASE")
    fun observeAll(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM person")
    suspend fun getAll(): List<PersonEntity>

    @Query("SELECT * FROM person WHERE id = :id")
    fun observe(id: String): Flow<PersonEntity?>

    @Query("SELECT * FROM person WHERE id = :id")
    suspend fun get(id: String): PersonEntity?

    @Upsert
    suspend fun upsert(entity: PersonEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<PersonEntity>)

    @Query("DELETE FROM person WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM person")
    suspend fun deleteAll()
}

@Dao
interface GiftCategoryDao {
    @Query("SELECT * FROM gift_category ORDER BY sort_order, name COLLATE NOCASE")
    fun observeAll(): Flow<List<GiftCategoryEntity>>

    @Query("SELECT * FROM gift_category")
    suspend fun getAll(): List<GiftCategoryEntity>

    @Upsert
    suspend fun upsert(entity: GiftCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<GiftCategoryEntity>)

    @Query("DELETE FROM gift_category WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM gift_category")
    suspend fun deleteAll()
}

@Dao
interface GiftIdeaDao {
    @Query("SELECT * FROM gift_idea ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<GiftIdeaEntity>>

    @Query("SELECT * FROM gift_idea")
    suspend fun getAll(): List<GiftIdeaEntity>

    @Query("SELECT * FROM gift_idea WHERE id = :id")
    fun observe(id: String): Flow<GiftIdeaEntity?>

    @Query("SELECT * FROM gift_idea WHERE id = :id")
    suspend fun get(id: String): GiftIdeaEntity?

    @Query("SELECT * FROM idea_person")
    fun observeLinks(): Flow<List<IdeaPersonEntity>>

    @Query("SELECT * FROM idea_person")
    suspend fun getAllLinks(): List<IdeaPersonEntity>

    @Query("SELECT person_id FROM idea_person WHERE idea_id = :ideaId")
    suspend fun getPersonIds(ideaId: String): List<String>

    @Upsert
    suspend fun upsert(entity: GiftIdeaEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<GiftIdeaEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLinks(links: List<IdeaPersonEntity>)

    @Query("DELETE FROM idea_person WHERE idea_id = :ideaId")
    suspend fun deleteLinks(ideaId: String)

    @Transaction
    suspend fun save(entity: GiftIdeaEntity, personIds: Set<String>) {
        upsert(entity)
        deleteLinks(entity.id)
        insertLinks(personIds.map { IdeaPersonEntity(entity.id, it) })
    }

    @Query("DELETE FROM gift_idea WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM gift_idea")
    suspend fun deleteAll()
}

@Dao
interface PlannedGiftDao {
    @Query("SELECT * FROM planned_gift WHERE edition_id = :editionId ORDER BY created_at")
    fun observeForEdition(editionId: String): Flow<List<PlannedGiftEntity>>

    @Query(
        """
        SELECT planned_gift.* FROM planned_gift
        JOIN christmas_edition ON christmas_edition.id = planned_gift.edition_id
        WHERE person_id = :personId ORDER BY christmas_edition.year DESC, planned_gift.created_at
        """,
    )
    fun observeForPerson(personId: String): Flow<List<PlannedGiftEntity>>

    @Query("SELECT * FROM planned_gift")
    suspend fun getAll(): List<PlannedGiftEntity>

    @Query("SELECT * FROM planned_gift WHERE id = :id")
    fun observe(id: String): Flow<PlannedGiftEntity?>

    @Query("SELECT * FROM planned_gift WHERE id = :id")
    suspend fun get(id: String): PlannedGiftEntity?

    @Upsert
    suspend fun upsert(entity: PlannedGiftEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<PlannedGiftEntity>)

    @Query("DELETE FROM planned_gift WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM planned_gift")
    suspend fun deleteAll()

    @Query("SELECT * FROM person_budget WHERE edition_id = :editionId")
    fun observeBudgets(editionId: String): Flow<List<PersonBudgetEntity>>

    @Query("SELECT * FROM person_budget")
    suspend fun getAllBudgets(): List<PersonBudgetEntity>

    @Upsert
    suspend fun upsertBudget(entity: PersonBudgetEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBudgets(entities: List<PersonBudgetEntity>)

    @Query("DELETE FROM person_budget WHERE edition_id = :editionId AND person_id = :personId")
    suspend fun deleteBudget(editionId: String, personId: String)
}

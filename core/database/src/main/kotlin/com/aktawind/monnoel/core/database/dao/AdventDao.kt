package com.aktawind.monnoel.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.aktawind.monnoel.core.database.entity.AdventBoxEntity
import com.aktawind.monnoel.core.database.entity.AdventCalendarEntity
import com.aktawind.monnoel.core.database.entity.AdventCalendarPersonEntity
import com.aktawind.monnoel.core.database.entity.AdventSupplyEntity
import com.aktawind.monnoel.core.database.entity.AdventTemplateBoxEntity
import com.aktawind.monnoel.core.database.entity.AdventTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdventDao {
    // --- Calendriers
    @Query("SELECT * FROM advent_calendar WHERE edition_id = :editionId ORDER BY created_at")
    fun observeCalendars(editionId: String): Flow<List<AdventCalendarEntity>>

    @Query("SELECT * FROM advent_calendar ORDER BY created_at")
    fun observeAllCalendars(): Flow<List<AdventCalendarEntity>>

    @Query("SELECT * FROM advent_calendar")
    suspend fun getAllCalendars(): List<AdventCalendarEntity>

    @Query("SELECT * FROM advent_calendar WHERE id = :id")
    fun observeCalendar(id: String): Flow<AdventCalendarEntity?>

    @Query("SELECT * FROM advent_calendar WHERE id = :id")
    suspend fun getCalendar(id: String): AdventCalendarEntity?

    @Query("SELECT * FROM advent_calendar_person")
    fun observeCalendarPersons(): Flow<List<AdventCalendarPersonEntity>>

    @Query("SELECT * FROM advent_calendar_person")
    suspend fun getAllCalendarPersons(): List<AdventCalendarPersonEntity>

    @Query("SELECT person_id FROM advent_calendar_person WHERE calendar_id = :calendarId")
    suspend fun getPersonIds(calendarId: String): List<String>

    @Upsert
    suspend fun upsertCalendar(entity: AdventCalendarEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCalendars(entities: List<AdventCalendarEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCalendarPersons(links: List<AdventCalendarPersonEntity>)

    @Query("DELETE FROM advent_calendar_person WHERE calendar_id = :calendarId")
    suspend fun deleteCalendarPersons(calendarId: String)

    @Transaction
    suspend fun saveCalendar(entity: AdventCalendarEntity, personIds: Set<String>) {
        upsertCalendar(entity)
        deleteCalendarPersons(entity.id)
        insertCalendarPersons(personIds.map { AdventCalendarPersonEntity(entity.id, it) })
    }

    @Query("DELETE FROM advent_calendar WHERE id = :id")
    suspend fun deleteCalendar(id: String)

    @Query("DELETE FROM advent_calendar")
    suspend fun deleteAllCalendars()

    // --- Cases
    @Query("SELECT * FROM advent_box WHERE calendar_id = :calendarId ORDER BY number")
    fun observeBoxes(calendarId: String): Flow<List<AdventBoxEntity>>

    @Query(
        "SELECT advent_box.* FROM advent_box JOIN advent_calendar ON advent_calendar.id = advent_box.calendar_id " +
            "WHERE advent_calendar.edition_id = :editionId ORDER BY number",
    )
    fun observeBoxesForEdition(editionId: String): Flow<List<AdventBoxEntity>>

    @Query("SELECT * FROM advent_box ORDER BY number")
    fun observeAllBoxes(): Flow<List<AdventBoxEntity>>

    @Query("SELECT * FROM advent_box")
    suspend fun getAllBoxes(): List<AdventBoxEntity>

    @Query("SELECT * FROM advent_box WHERE calendar_id = :calendarId ORDER BY number")
    suspend fun getBoxes(calendarId: String): List<AdventBoxEntity>

    @Query("SELECT * FROM advent_box WHERE id = :id")
    suspend fun getBox(id: String): AdventBoxEntity?

    @Upsert
    suspend fun upsertBoxes(entities: List<AdventBoxEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBoxes(entities: List<AdventBoxEntity>)

    @Query("DELETE FROM advent_box WHERE id IN (:ids)")
    suspend fun deleteBoxes(ids: List<String>)

    @Query("UPDATE advent_box SET number = :number WHERE id = :id")
    suspend fun setNumber(id: String, number: Int)

    /** Échange les numéros de deux cases : leur contenu change donc de place. */
    @Transaction
    suspend fun swap(first: AdventBoxEntity, second: AdventBoxEntity) {
        setNumber(first.id, second.number)
        setNumber(second.id, first.number)
    }

    @Query("UPDATE advent_box SET supply_id = NULL WHERE supply_id = :supplyId")
    suspend fun unlinkSupply(supplyId: String)

    @Query("UPDATE advent_box SET supply_id = :supplyId WHERE id IN (:boxIds)")
    suspend fun linkSupply(supplyId: String, boxIds: List<String>)

    // --- Fournitures
    @Query("SELECT * FROM advent_supply WHERE calendar_id = :calendarId ORDER BY sort_order")
    fun observeSupplies(calendarId: String): Flow<List<AdventSupplyEntity>>

    @Query(
        "SELECT advent_supply.* FROM advent_supply JOIN advent_calendar ON advent_calendar.id = advent_supply.calendar_id " +
            "WHERE advent_calendar.edition_id = :editionId ORDER BY sort_order",
    )
    fun observeSuppliesForEdition(editionId: String): Flow<List<AdventSupplyEntity>>

    @Query("SELECT * FROM advent_supply WHERE calendar_id = :calendarId ORDER BY sort_order")
    suspend fun getSupplies(calendarId: String): List<AdventSupplyEntity>

    @Query("SELECT * FROM advent_supply")
    suspend fun getAllSupplies(): List<AdventSupplyEntity>

    @Upsert
    suspend fun upsertSupply(entity: AdventSupplyEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSupplies(entities: List<AdventSupplyEntity>)

    @Transaction
    suspend fun saveSupply(entity: AdventSupplyEntity, linkedBoxIds: Set<String>) {
        upsertSupply(entity)
        unlinkSupply(entity.id)
        if (linkedBoxIds.isNotEmpty()) linkSupply(entity.id, linkedBoxIds.toList())
    }

    @Query("DELETE FROM advent_supply WHERE id = :id")
    suspend fun deleteSupply(id: String)

    // --- Modèles
    @Query("SELECT * FROM advent_template ORDER BY name COLLATE NOCASE")
    fun observeTemplates(): Flow<List<AdventTemplateEntity>>

    @Query("SELECT * FROM advent_template")
    suspend fun getAllTemplates(): List<AdventTemplateEntity>

    @Query("SELECT * FROM advent_template WHERE id = :id")
    suspend fun getTemplate(id: String): AdventTemplateEntity?

    @Query("SELECT * FROM advent_template_box ORDER BY number")
    fun observeAllTemplateBoxes(): Flow<List<AdventTemplateBoxEntity>>

    @Query("SELECT * FROM advent_template_box")
    suspend fun getAllTemplateBoxes(): List<AdventTemplateBoxEntity>

    @Query("SELECT * FROM advent_template_box WHERE template_id = :templateId ORDER BY number")
    suspend fun getTemplateBoxes(templateId: String): List<AdventTemplateBoxEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTemplates(entities: List<AdventTemplateEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTemplateBoxes(entities: List<AdventTemplateBoxEntity>)

    @Transaction
    suspend fun saveTemplate(template: AdventTemplateEntity, boxes: List<AdventTemplateBoxEntity>) {
        insertTemplates(listOf(template))
        insertTemplateBoxes(boxes)
    }

    @Query("DELETE FROM advent_template WHERE id = :id")
    suspend fun deleteTemplate(id: String)

    @Query("DELETE FROM advent_template")
    suspend fun deleteAllTemplates()
}

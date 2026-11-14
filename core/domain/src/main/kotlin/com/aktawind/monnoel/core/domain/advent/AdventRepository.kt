package com.aktawind.monnoel.core.domain.advent

import com.aktawind.monnoel.core.model.AdventBox
import com.aktawind.monnoel.core.model.AdventCalendar
import com.aktawind.monnoel.core.model.AdventSupply
import com.aktawind.monnoel.core.model.AdventTemplate
import com.aktawind.monnoel.core.model.AdventTemplateBox
import kotlinx.coroutines.flow.Flow

interface AdventRepository {
    fun observeCalendars(editionId: String): Flow<List<AdventCalendar>>

    /** Tous les calendriers de tous les Noëls (pour copier ou reprendre des cases). */
    fun observeAllCalendars(): Flow<List<AdventCalendar>>

    fun observeCalendar(id: String): Flow<AdventCalendar?>

    suspend fun getCalendar(id: String): AdventCalendar?

    suspend fun saveCalendar(calendar: AdventCalendar)

    suspend fun deleteCalendar(id: String)

    fun observeBoxes(calendarId: String): Flow<List<AdventBox>>

    /** Toutes les cases de tous les calendriers d'un Noël (accueil, budget). */
    fun observeBoxesForEdition(editionId: String): Flow<List<AdventBox>>

    fun observeAllBoxes(): Flow<List<AdventBox>>

    suspend fun getBoxes(calendarId: String): List<AdventBox>

    suspend fun getBox(id: String): AdventBox?

    suspend fun saveBoxes(boxes: List<AdventBox>)

    suspend fun deleteBoxes(ids: List<String>)

    /** Échange le contenu de deux cases d'un même calendrier (les numéros restent en place). */
    suspend fun swapBoxes(firstId: String, secondId: String)

    fun observeSupplies(calendarId: String): Flow<List<AdventSupply>>

    fun observeSuppliesForEdition(editionId: String): Flow<List<AdventSupply>>

    suspend fun getSupplies(calendarId: String): List<AdventSupply>

    suspend fun saveSupply(supply: AdventSupply, linkedBoxIds: Set<String>)

    suspend fun deleteSupply(id: String)

    fun observeTemplates(): Flow<List<AdventTemplate>>

    fun observeAllTemplateBoxes(): Flow<List<AdventTemplateBox>>

    suspend fun getTemplate(id: String): AdventTemplate?

    suspend fun getTemplateBoxes(templateId: String): List<AdventTemplateBox>

    suspend fun saveTemplate(template: AdventTemplate, boxes: List<AdventTemplateBox>)

    suspend fun deleteTemplate(id: String)
}

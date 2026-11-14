package com.aktawind.monnoel.core.data.repository

import com.aktawind.monnoel.core.database.dao.AdventDao
import com.aktawind.monnoel.core.database.entity.AdventCalendarEntity
import com.aktawind.monnoel.core.database.entity.AdventCalendarPersonEntity
import com.aktawind.monnoel.core.database.entity.asEntity
import com.aktawind.monnoel.core.database.entity.asModel
import com.aktawind.monnoel.core.domain.advent.AdventRepository
import com.aktawind.monnoel.core.model.AdventBox
import com.aktawind.monnoel.core.model.AdventCalendar
import com.aktawind.monnoel.core.model.AdventSupply
import com.aktawind.monnoel.core.model.AdventTemplate
import com.aktawind.monnoel.core.model.AdventTemplateBox
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoomAdventRepository @Inject constructor(private val dao: AdventDao) : AdventRepository {
    private fun withPersons(calendars: Flow<List<AdventCalendarEntity>>): Flow<List<AdventCalendar>> =
        combine(calendars, dao.observeCalendarPersons()) { list, links ->
            list.map { it.asModel(links.personsOf(it.id)) }
        }

    private fun List<AdventCalendarPersonEntity>.personsOf(id: String) = filter {
        it.calendarId == id
    }.map { it.personId }.toSet()

    override fun observeCalendars(editionId: String) = withPersons(dao.observeCalendars(editionId))

    override fun observeAllCalendars() = withPersons(dao.observeAllCalendars())

    override fun observeCalendar(id: String): Flow<AdventCalendar?> =
        combine(dao.observeCalendar(id), dao.observeCalendarPersons()) { c, links -> c?.asModel(links.personsOf(id)) }

    override suspend fun getCalendar(id: String) = dao.getCalendar(id)?.asModel(dao.getPersonIds(id).toSet())

    override suspend fun saveCalendar(calendar: AdventCalendar) = dao.saveCalendar(calendar.asEntity(), calendar.personIds)

    override suspend fun deleteCalendar(id: String) = dao.deleteCalendar(id)

    override fun observeBoxes(calendarId: String) = dao.observeBoxes(calendarId).map { l -> l.map { it.asModel() } }

    override fun observeBoxesForEdition(editionId: String) = dao.observeBoxesForEdition(editionId).map { l ->
        l.map { it.asModel() }
    }

    override fun observeAllBoxes() = dao.observeAllBoxes().map { l -> l.map { it.asModel() } }

    override suspend fun getBoxes(calendarId: String) = dao.getBoxes(calendarId).map { it.asModel() }

    override suspend fun getBox(id: String) = dao.getBox(id)?.asModel()

    override suspend fun saveBoxes(boxes: List<AdventBox>) = dao.upsertBoxes(boxes.map { it.asEntity() })

    override suspend fun deleteBoxes(ids: List<String>) = dao.deleteBoxes(ids)

    override suspend fun swapBoxes(firstId: String, secondId: String) {
        val first = dao.getBox(firstId) ?: return
        val second = dao.getBox(secondId) ?: return
        dao.swap(first, second)
    }

    override fun observeSupplies(calendarId: String) = dao.observeSupplies(calendarId).map { l ->
        l.map { it.asModel() }
    }

    override fun observeSuppliesForEdition(editionId: String) = dao.observeSuppliesForEdition(editionId).map { l -> l.map { it.asModel() } }

    override suspend fun getSupplies(calendarId: String) = dao.getSupplies(calendarId).map { it.asModel() }

    override suspend fun saveSupply(supply: AdventSupply, linkedBoxIds: Set<String>) = dao.saveSupply(supply.asEntity(), linkedBoxIds)

    override suspend fun deleteSupply(id: String) = dao.deleteSupply(id)

    override fun observeTemplates() = dao.observeTemplates().map { l -> l.map { it.asModel() } }

    override fun observeAllTemplateBoxes() = dao.observeAllTemplateBoxes().map { l -> l.map { it.asModel() } }

    override suspend fun getTemplate(id: String) = dao.getTemplate(id)?.asModel()

    override suspend fun getTemplateBoxes(templateId: String) = dao.getTemplateBoxes(templateId).map { it.asModel() }

    override suspend fun saveTemplate(template: AdventTemplate, boxes: List<AdventTemplateBox>) =
        dao.saveTemplate(template.asEntity(), boxes.map { it.asEntity() })

    override suspend fun deleteTemplate(id: String) = dao.deleteTemplate(id)
}

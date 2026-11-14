package com.aktawind.monnoel.core.data.repository

import com.aktawind.monnoel.core.database.dao.GiftCategoryDao
import com.aktawind.monnoel.core.database.dao.GiftIdeaDao
import com.aktawind.monnoel.core.database.dao.PersonDao
import com.aktawind.monnoel.core.database.dao.PlannedGiftDao
import com.aktawind.monnoel.core.database.entity.PersonBudgetEntity
import com.aktawind.monnoel.core.database.entity.asEntity
import com.aktawind.monnoel.core.database.entity.asModel
import com.aktawind.monnoel.core.domain.repository.GiftCategoryRepository
import com.aktawind.monnoel.core.domain.repository.GiftIdeaRepository
import com.aktawind.monnoel.core.domain.repository.PersonRepository
import com.aktawind.monnoel.core.domain.repository.PlannedGiftRepository
import com.aktawind.monnoel.core.model.GiftCategory
import com.aktawind.monnoel.core.model.GiftIdea
import com.aktawind.monnoel.core.model.Person
import com.aktawind.monnoel.core.model.PlannedGift
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoomPersonRepository @Inject constructor(private val dao: PersonDao) : PersonRepository {
    override fun observePersons(): Flow<List<Person>> = dao.observeAll().map { list -> list.map { it.asModel() } }

    override fun observePerson(id: String): Flow<Person?> = dao.observe(id).map { it?.asModel() }

    override suspend fun getPerson(id: String): Person? = dao.get(id)?.asModel()

    override suspend fun upsert(person: Person) = dao.upsert(person.asEntity())

    override suspend fun delete(id: String) = dao.delete(id)
}

class RoomGiftCategoryRepository @Inject constructor(private val dao: GiftCategoryDao) : GiftCategoryRepository {
    override fun observeCategories(): Flow<List<GiftCategory>> = dao.observeAll().map { list ->
        list.map { it.asModel() }
    }

    override suspend fun upsert(category: GiftCategory) = dao.upsert(category.asEntity())

    override suspend fun delete(id: String) = dao.delete(id)
}

class RoomGiftIdeaRepository @Inject constructor(private val dao: GiftIdeaDao) : GiftIdeaRepository {
    override fun observeIdeas(): Flow<List<GiftIdea>> = combine(dao.observeAll(), dao.observeLinks()) { ideas, links ->
        val byIdea = links.groupBy({ it.ideaId }, { it.personId })
        ideas.map { it.asModel(byIdea[it.id].orEmpty().toSet()) }
    }

    override fun observeIdea(id: String): Flow<GiftIdea?> = combine(dao.observe(id), dao.observeLinks()) {
            idea,
            links,
        ->
        idea?.asModel(links.filter { it.ideaId == id }.map { it.personId }.toSet())
    }

    override suspend fun getIdea(id: String): GiftIdea? = dao.get(id)?.asModel(dao.getPersonIds(id).toSet())

    override suspend fun upsert(idea: GiftIdea) = dao.save(idea.asEntity(), idea.personIds)

    override suspend fun delete(id: String) = dao.delete(id)
}

class RoomPlannedGiftRepository @Inject constructor(private val dao: PlannedGiftDao) : PlannedGiftRepository {
    override fun observeGifts(editionId: String): Flow<List<PlannedGift>> =
        dao.observeForEdition(editionId).map { list -> list.map { it.asModel() } }

    override fun observeGiftsForPerson(personId: String): Flow<List<PlannedGift>> =
        dao.observeForPerson(personId).map { list -> list.map { it.asModel() } }

    override fun observeGift(id: String): Flow<PlannedGift?> = dao.observe(id).map { it?.asModel() }

    override suspend fun getGift(id: String): PlannedGift? = dao.get(id)?.asModel()

    override suspend fun upsert(gift: PlannedGift) = dao.upsert(gift.asEntity())

    override suspend fun delete(id: String) = dao.delete(id)

    override fun observeBudgets(editionId: String) = dao.observeBudgets(editionId).map { list ->
        list.map { it.asModel() }
    }

    override suspend fun setBudget(editionId: String, personId: String, amountCents: Long?) {
        if (amountCents == null) {
            dao.deleteBudget(editionId, personId)
        } else {
            dao.upsertBudget(PersonBudgetEntity(editionId, personId, amountCents))
        }
    }
}

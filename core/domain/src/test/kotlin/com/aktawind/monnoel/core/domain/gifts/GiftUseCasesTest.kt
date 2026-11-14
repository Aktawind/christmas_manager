package com.aktawind.monnoel.core.domain.gifts

import com.aktawind.monnoel.core.common.AppError
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.edition.FakeEditionRepository
import com.aktawind.monnoel.core.domain.edition.FixedTime
import com.aktawind.monnoel.core.domain.edition.SequentialIds
import com.aktawind.monnoel.core.domain.repository.GiftIdeaRepository
import com.aktawind.monnoel.core.domain.repository.PersonRepository
import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import com.aktawind.monnoel.core.domain.repository.PlannedGiftRepository
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.model.EditionStatus
import com.aktawind.monnoel.core.model.GiftIdea
import com.aktawind.monnoel.core.model.GiftStatus
import com.aktawind.monnoel.core.model.Person
import com.aktawind.monnoel.core.model.PersonBudget
import com.aktawind.monnoel.core.model.PlannedGift
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest

private class FakePersons : PersonRepository {
    val items = MutableStateFlow<List<Person>>(emptyList())

    override fun observePersons() = items

    override fun observePerson(id: String) = items.map { l -> l.firstOrNull { it.id == id } }

    override suspend fun getPerson(id: String) = items.value.firstOrNull { it.id == id }

    override suspend fun upsert(person: Person) {
        items.value = items.value.filterNot { it.id == person.id } + person
    }

    override suspend fun delete(id: String) {
        items.value = items.value.filterNot { it.id == id }
    }
}

private class FakeIdeas : GiftIdeaRepository {
    val items = MutableStateFlow<List<GiftIdea>>(emptyList())

    override fun observeIdeas() = items

    override fun observeIdea(id: String) = items.map { l -> l.firstOrNull { it.id == id } }

    override suspend fun getIdea(id: String) = items.value.firstOrNull { it.id == id }

    override suspend fun upsert(idea: GiftIdea) {
        items.value = items.value.filterNot { it.id == idea.id } + idea
    }

    override suspend fun delete(id: String) {
        items.value = items.value.filterNot { it.id == id }
    }
}

private class FakeGifts : PlannedGiftRepository {
    val items = MutableStateFlow<List<PlannedGift>>(emptyList())
    val budgets = MutableStateFlow<List<PersonBudget>>(emptyList())

    override fun observeGifts(editionId: String) = items.map { l -> l.filter { it.editionId == editionId } }

    override fun observeGiftsForPerson(personId: String) = items.map { l -> l.filter { it.personId == personId } }

    override fun observeGift(id: String) = items.map { l -> l.firstOrNull { it.id == id } }

    override suspend fun getGift(id: String) = items.value.firstOrNull { it.id == id }

    override suspend fun upsert(gift: PlannedGift) {
        items.value = items.value.filterNot { it.id == gift.id } + gift
    }

    override suspend fun delete(id: String) {
        items.value = items.value.filterNot { it.id == id }
    }

    override fun observeBudgets(editionId: String) = budgets

    override suspend fun setBudget(editionId: String, personId: String, amountCents: Long?) {
        budgets.value = budgets.value.filterNot { it.personId == personId } +
            listOfNotNull(amountCents?.let { PersonBudget(editionId, personId, it) })
    }
}

private class FakePhotos : PhotoRepository {
    val deleted = mutableListOf<String>()
    var copies = 0

    override suspend fun importPhoto(sourceUri: String) = AppResult.Success("photos/new.jpg")

    override suspend fun copyPhoto(relativePath: String) = "photos/copy-${copies++}.jpg"

    override suspend fun deletePhoto(relativePath: String) {
        deleted += relativePath
    }
}

class GiftUseCasesTest {
    private val editions = FakeEditionRepository()
    private val persons = FakePersons()
    private val ideas = FakeIdeas()
    private val gifts = FakeGifts()
    private val photos = FakePhotos()
    private val time = FixedTime()
    private val ids = SequentialIds()
    private val guard = EditionGuard(editions)
    private val saveGift = SaveGiftUseCase(gifts, persons, photos, guard, time, ids)
    private val saveIdea = SaveIdeaUseCase(ideas, photos, time, ids)

    private suspend fun setUp(archived: Boolean = false) {
        editions.insert(
            ChristmasEdition(
                "e",
                2026,
                if (archived) EditionStatus.ARCHIVED else EditionStatus.ACTIVE,
                null,
                Instant.EPOCH,
                Instant.EPOCH,
            ),
        )
        persons.upsert(Person("lea", "Léa", "Martin", null, "", "", Instant.EPOCH, Instant.EPOCH))
    }

    @Test
    fun `offrir une idee fige une copie independante`() = runTest {
        setUp()
        val idea = (
            saveIdea(
                null,
                IdeaDraft(title = "Roman", estimatedPriceCents = 1_500, photoPath = "photos/idea.jpg"),
            ) as AppResult.Success
            ).value

        val gift = (OfferIdeaUseCase(ideas, photos, saveGift)(idea.id, "lea", "e") as AppResult.Success).value
        assertEquals("Roman", gift.title)
        assertEquals(1_500, gift.priceCents)
        assertEquals("Léa Martin", gift.recipientName)
        assertEquals(idea.id, gift.ideaId)
        assertNotEquals(idea.photoPath, gift.photoPath)

        // Modifier l'idée ne change pas le cadeau (historique figé).
        saveIdea(idea.id, IdeaDraft(title = "Autre titre", estimatedPriceCents = 9_900, photoPath = "photos/idea.jpg"))
        assertEquals("Roman", gifts.getGift(gift.id)?.title)
        assertEquals(1_500, gifts.getGift(gift.id)?.priceCents)
    }

    @Test
    fun `une annee archivee est en lecture seule`() = runTest {
        setUp(archived = true)
        assertEquals(
            AppResult.Failure(AppError.ReadOnly),
            saveGift(null, "e", GiftDraft(personId = "lea", title = "Livre")),
        )
        assertEquals(AppResult.Failure(AppError.ReadOnly), SetPersonBudgetUseCase(gifts, guard)("e", "lea", 5_000))
    }

    @Test
    fun `passer a commande renseigne la date d'achat`() = runTest {
        setUp()
        val gift = (saveGift(null, "e", GiftDraft(personId = "lea", title = "Livre")) as AppResult.Success).value
        assertEquals(null, gift.purchaseDate)
        val ordered = (
            ChangeGiftStatusUseCase(
                gifts,
                guard,
                time,
            )(gift.id, GiftStatus.ORDERED) as AppResult.Success
            ).value
        assertEquals(LocalDate.of(2026, 11, 11), ordered.purchaseDate)
    }

    @Test
    fun `remplacer la photo supprime l'ancienne`() = runTest {
        setUp()
        val idea = (saveIdea(null, IdeaDraft(title = "Jeu", photoPath = "photos/a.jpg")) as AppResult.Success).value
        saveIdea(idea.id, IdeaDraft(title = "Jeu", photoPath = "photos/b.jpg"))
        assertTrue("photos/a.jpg" in photos.deleted)
    }

    @Test
    fun `titre obligatoire et enveloppe`() = runTest {
        setUp()
        assertEquals(
            AppResult.Failure(AppError.InvalidInput),
            saveGift(null, "e", GiftDraft(personId = "lea", title = " ")),
        )
        assertIs<AppResult.Success<Unit>>(SetPersonBudgetUseCase(gifts, guard)("e", "lea", 5_000))
        assertEquals(5_000, gifts.budgets.value.single().amountCents)
        SetPersonBudgetUseCase(gifts, guard)("e", "lea", null)
        assertTrue(gifts.budgets.value.isEmpty())
    }
}

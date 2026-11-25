package com.aktawind.monnoel.core.domain.traditions

import com.aktawind.monnoel.core.common.AppError
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.edition.FakeEditionRepository
import com.aktawind.monnoel.core.domain.edition.FixedTime
import com.aktawind.monnoel.core.domain.edition.SequentialIds
import com.aktawind.monnoel.core.domain.gifts.EditionGuard
import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.model.EditionStatus
import com.aktawind.monnoel.core.model.Inspiration
import com.aktawind.monnoel.core.model.InspirationKind
import com.aktawind.monnoel.core.model.Tradition
import com.aktawind.monnoel.core.model.TraditionYear
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest

class FakeTraditionRepository : TraditionRepository {
    val traditions = MutableStateFlow<List<Tradition>>(emptyList())
    val years = MutableStateFlow<List<TraditionYear>>(emptyList())
    val inspirations = MutableStateFlow<List<Inspiration>>(emptyList())
    val done = MutableStateFlow<Set<Pair<String, String>>>(emptySet())

    override fun observeTraditions() = traditions

    override suspend fun getTradition(id: String) = traditions.value.firstOrNull { it.id == id }

    override suspend fun saveTradition(tradition: Tradition) {
        traditions.value = traditions.value.filterNot { it.id == tradition.id } + tradition
    }

    override suspend fun deleteTradition(id: String) {
        traditions.value = traditions.value.filterNot { it.id == id }
        years.value = years.value.filterNot { it.traditionId == id }
    }

    override fun observeYears(editionId: String) = years.map { l -> l.filter { it.editionId == editionId } }

    override fun observeAllYears() = years

    override suspend fun getYearsOf(traditionId: String) = years.value.filter { it.traditionId == traditionId }

    override suspend fun getYear(editionId: String, traditionId: String) =
        years.value.firstOrNull { it.editionId == editionId && it.traditionId == traditionId }

    override suspend fun saveYear(year: TraditionYear) {
        years.value = years.value.filterNot { it.id == year.id } + year
    }

    override suspend fun deleteYear(id: String) {
        years.value = years.value.filterNot { it.id == id }
    }

    override fun observeInspirations() = inspirations

    override suspend fun getInspiration(id: String) = inspirations.value.firstOrNull { it.id == id }

    override suspend fun saveInspiration(inspiration: Inspiration) {
        inspirations.value = inspirations.value.filterNot { it.id == inspiration.id } + inspiration
    }

    override suspend fun deleteInspiration(id: String) {
        inspirations.value = inspirations.value.filterNot { it.id == id }
    }

    override fun observeDone(editionId: String) = done.map { s -> s.filter { it.first == editionId }.map { it.second }.toSet() }

    override suspend fun setDone(editionId: String, inspirationId: String, done: Boolean) {
        this.done.value = if (done) this.done.value + (editionId to inspirationId) else this.done.value - (editionId to inspirationId)
    }

    val dates = MutableStateFlow<Map<Pair<String, String>, java.time.LocalDate>>(emptyMap())

    override fun observeInspirationDates(editionId: String) =
        dates.map { m -> m.filterKeys { it.first == editionId }.mapKeys { it.key.second } }

    override suspend fun setInspirationDate(editionId: String, inspirationId: String, date: java.time.LocalDate?) {
        dates.value = if (date == null) dates.value - (editionId to inspirationId) else dates.value + ((editionId to inspirationId) to date)
    }
}

private class Photos : PhotoRepository {
    val deleted = mutableListOf<String>()

    override suspend fun importPhoto(sourceUri: String) = AppResult.Success("photos/new.jpg")

    override suspend fun copyPhoto(relativePath: String) = "photos/copy.jpg"

    override suspend fun deletePhoto(relativePath: String) {
        deleted += relativePath
    }
}

class TraditionUseCasesTest {
    private val t = Instant.EPOCH
    private val editions = FakeEditionRepository()
    private val repo = FakeTraditionRepository()
    private val photos = Photos()
    private val ids = SequentialIds()
    private val time = FixedTime()
    private val guard = EditionGuard(editions)
    private val save = SaveTraditionUseCase(repo, photos, time, ids)

    private suspend fun setUp() {
        editions.insert(ChristmasEdition("e", 2026, EditionStatus.ACTIVE, null, t, t))
        editions.insert(ChristmasEdition("old", 2025, EditionStatus.ARCHIVED, null, t, t))
    }

    @Test
    fun `une cinquantaine d'idees a piocher, sans doublon avec le catalogue`() = runTest {
        assertTrue(traditionIdeas.size >= 50)
        // Pas deux fois la même idée.
        assertEquals(traditionIdeas.size, traditionIdeas.map { it.name }.toSet().size)
        save(null, TraditionDraft("le lutin FARCEUR"))
        val available = availableTraditionIdeas(repo.traditions.value)
        assertTrue(available.none { it.name == "Le lutin farceur" })
        assertEquals(traditionIdeas.size - 1, available.size)
        assertEquals(2, AddTraditionIdeasUseCase(save)(available.take(2)))
        assertEquals(3, repo.traditions.value.size)
        assertEquals(AppResult.Failure(AppError.InvalidInput), save(null, TraditionDraft(" ")))
    }

    @Test
    fun `programme de l'annee, faite avec un souvenir, retiree`() = runTest {
        setUp()
        val sapin = (save(null, TraditionDraft("Décorer le sapin")) as AppResult.Success).value
        val plan = SetTraditionPlannedUseCase(repo, photos, guard, time, ids)
        plan("e", sapin.id, true)
        plan("e", sapin.id, true)
        assertEquals(1, repo.years.value.size)
        SaveTraditionMemoryUseCase(repo, photos, guard, time, ids)("e", sapin.id, true, "photos/sapin.jpg", " Magique ")
        val year = repo.getYear("e", sapin.id)!!
        assertTrue(year.done)
        assertEquals("Magique", year.memoryNote)
        assertEquals(AppResult.Failure(AppError.ReadOnly), plan("old", sapin.id, true))
        plan("e", sapin.id, false)
        assertTrue(repo.years.value.isEmpty())
        assertEquals(listOf("photos/sapin.jpg"), photos.deleted)
    }

    @Test
    fun `une inspiration se coche pour chaque Noel`() = runTest {
        setUp()
        editions.insert(ChristmasEdition("next", 2027, EditionStatus.ACTIVE, null, t, t))
        val film = (
            SaveInspirationUseCase(
                repo,
                photos,
                time,
                ids,
            )(null, InspirationDraft("Maman, j'ai raté l'avion", InspirationKind.FILM)) as AppResult.Success
            ).value
        val setDone = SetInspirationDoneUseCase(repo, guard)
        setDone("e", film.id, true)
        assertEquals(setOf(film.id), repo.done.value.filter { it.first == "e" }.map { it.second }.toSet())
        assertTrue(repo.done.value.none { it.first == "next" })
        setDone("e", film.id, false)
        assertTrue(repo.done.value.isEmpty())
    }

    @Test
    fun `date prevue d'une tradition et d'une inspiration`() = runTest {
        setUp()
        val lutin = (save(null, TraditionDraft("Le lutin")) as AppResult.Success).value
        val setDate = SetTraditionDateUseCase(repo, guard, time, ids)
        val date = java.time.LocalDate.of(2026, 12, 6)
        setDate("e", lutin.id, date)
        // La tradition passe au programme avec sa date ; le souvenir la garde.
        assertEquals(date, repo.getYear("e", lutin.id)?.date)
        SaveTraditionMemoryUseCase(repo, photos, guard, time, ids)("e", lutin.id, true, null, "Magique")
        assertEquals(date, repo.getYear("e", lutin.id)?.date)
        setDate("e", lutin.id, null)
        assertEquals(null, repo.getYear("e", lutin.id)?.date)
        assertEquals(AppResult.Failure(AppError.ReadOnly), setDate("old", lutin.id, date))

        val film = (SaveInspirationUseCase(repo, photos, time, ids)(null, InspirationDraft("Le Grinch"))).let {
            (it as AppResult.Success).value
        }
        val setInspirationDate = SetInspirationDateUseCase(repo, guard)
        setInspirationDate("e", film.id, date)
        assertEquals(mapOf(film.id to date), repo.dates.value.filterKeys { it.first == "e" }.mapKeys { it.key.second })
        setInspirationDate("e", film.id, null)
        assertTrue(repo.dates.value.isEmpty())
    }

    @Test
    fun `dupliquer une tradition et une inspiration`() = runTest {
        val lutin = (save(null, TraditionDraft("Le lutin", photoPath = "photos/l.jpg", favorite = true)) as AppResult.Success).value
        val copy = (DuplicateTraditionUseCase(repo, photos, time, ids)(lutin.id, "Le lutin (copie)") as AppResult.Success).value
        assertEquals("Le lutin (copie)", copy.name)
        assertTrue(copy.id != lutin.id && copy.photoPath != null && copy.photoPath != lutin.photoPath)
        assertEquals(true, copy.favorite)
        assertEquals(2, repo.traditions.value.size)
        val film = (SaveInspirationUseCase(repo, photos, time, ids)(null, InspirationDraft("Le Grinch")) as AppResult.Success).value
        val filmCopy = (DuplicateInspirationUseCase(repo, photos, time, ids)(film.id, "Le Grinch (copie)") as AppResult.Success).value
        assertEquals(film.kind, filmCopy.kind)
        assertEquals(2, repo.inspirations.value.size)
    }
}

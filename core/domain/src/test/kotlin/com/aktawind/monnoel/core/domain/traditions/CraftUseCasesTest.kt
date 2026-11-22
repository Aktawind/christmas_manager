package com.aktawind.monnoel.core.domain.traditions

import com.aktawind.monnoel.core.common.AppError
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.budget.BudgetPost
import com.aktawind.monnoel.core.domain.budget.BudgetSource
import com.aktawind.monnoel.core.domain.budget.computeYearBudget
import com.aktawind.monnoel.core.domain.edition.FakeEditionRepository
import com.aktawind.monnoel.core.domain.edition.FixedTime
import com.aktawind.monnoel.core.domain.edition.SequentialIds
import com.aktawind.monnoel.core.domain.gifts.EditionGuard
import com.aktawind.monnoel.core.domain.gifts.FakePhotos
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.model.Craft
import com.aktawind.monnoel.core.model.CraftCategory
import com.aktawind.monnoel.core.model.CraftStatus
import com.aktawind.monnoel.core.model.CraftSupply
import com.aktawind.monnoel.core.model.CraftYear
import com.aktawind.monnoel.core.model.DecorIdea
import com.aktawind.monnoel.core.model.DecorPlace
import com.aktawind.monnoel.core.model.EditionStatus
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest

class FakeCraftRepository : CraftRepository {
    val crafts = MutableStateFlow<List<Craft>>(emptyList())
    val supplies = MutableStateFlow<List<CraftSupply>>(emptyList())
    val years = MutableStateFlow<List<CraftYear>>(emptyList())
    val bought = MutableStateFlow<Set<Pair<String, String>>>(emptySet())
    val decor = MutableStateFlow<List<DecorIdea>>(emptyList())

    override fun observeCrafts() = crafts

    override suspend fun getCraft(id: String) = crafts.value.firstOrNull { it.id == id }

    override suspend fun saveCraft(craft: Craft) {
        crafts.value = crafts.value.filterNot { it.id == craft.id } + craft
    }

    override suspend fun deleteCraft(id: String) {
        crafts.value = crafts.value.filterNot { it.id == id }
        supplies.value = supplies.value.filterNot { it.craftId == id }
        years.value = years.value.filterNot { it.craftId == id }
    }

    override fun observeSupplies() = supplies

    override suspend fun getSupplies(craftId: String) = supplies.value.filter { it.craftId == craftId }.sortedBy { it.sortOrder }

    override suspend fun saveSupply(supply: CraftSupply) {
        supplies.value = supplies.value.filterNot { it.id == supply.id } + supply
    }

    override suspend fun deleteSupply(id: String) {
        supplies.value = supplies.value.filterNot { it.id == id }
    }

    override fun observeYears() = years

    override suspend fun getYear(editionId: String, craftId: String) =
        years.value.firstOrNull { it.editionId == editionId && it.craftId == craftId }

    override suspend fun getYearsOf(craftId: String) = years.value.filter { it.craftId == craftId }

    override suspend fun saveYear(year: CraftYear) {
        years.value = years.value.filterNot { it.id == year.id } + year
    }

    override suspend fun deleteYear(id: String) {
        years.value = years.value.filterNot { it.id == id }
    }

    override fun observeBought() = bought

    override suspend fun setBought(editionId: String, supplyId: String, bought: Boolean) {
        this.bought.value = if (bought) this.bought.value + (editionId to supplyId) else this.bought.value - (editionId to supplyId)
    }

    override fun observeDecor() = decor

    override suspend fun getDecor(id: String) = decor.value.firstOrNull { it.id == id }

    override suspend fun saveDecor(decor: DecorIdea) {
        this.decor.value = this.decor.value.filterNot { it.id == decor.id } + decor
    }

    override suspend fun deleteDecor(id: String) {
        decor.value = decor.value.filterNot { it.id == id }
    }
}

class CraftUseCasesTest {
    private val t = Instant.EPOCH
    private val editions = FakeEditionRepository()
    private val repo = FakeCraftRepository()
    private val photos = FakePhotos()
    private val time = FixedTime()
    private val ids = SequentialIds()
    private val guard = EditionGuard(editions)
    private val save = SaveCraftUseCase(repo, photos, time, ids)
    private val saveSupply = SaveCraftSupplyUseCase(repo, ids)
    private val setStatus = SetCraftStatusUseCase(repo, photos, guard, time, ids)
    private val setResult = SetCraftResultPhotoUseCase(repo, photos, guard, time, ids)
    private val setBought = SetCraftSupplyBoughtUseCase(repo, guard)

    private suspend fun seedEditions() {
        editions.insert(ChristmasEdition("e", 2026, EditionStatus.ACTIVE, null, t, t))
        editions.insert(ChristmasEdition("old", 2025, EditionStatus.ARCHIVED, null, t, t))
    }

    @Test
    fun `bricolage fournitures avancement et budget atelier`() = runTest {
        seedEditions()
        assertEquals(AppResult.Failure(AppError.InvalidInput), save(null, CraftDraft(" ")))
        val craft = (save(null, CraftDraft(" Couronne ", CraftCategory.DECORATION)) as AppResult.Success).value
        assertEquals("Couronne", craft.title)
        val ruban = (saveSupply(null, craft.id, "Ruban", 450) as AppResult.Success).value
        val mousse = (saveSupply(null, craft.id, "Mousse", 300) as AppResult.Success).value
        saveSupply(null, craft.id, "Ciseaux", null)
        assertEquals(listOf(0, 1, 2), repo.getSupplies(craft.id).map { it.sortOrder })
        assertEquals(AppResult.Failure(AppError.InvalidInput), saveSupply(null, craft.id, "Colle", -1))

        // Pas encore au programme : rien dans le budget.
        val edition = editions.getById("e")!!
        fun budget() = computeYearBudget(
            edition,
            BudgetSource(craftYears = repo.years.value, craftSupplies = repo.supplies.value, craftBought = repo.bought.value),
            LocalDate.of(2026, 11, 22),
        ).post(BudgetPost.CRAFTS)
        assertEquals(0, budget().plannedCents)

        setStatus("e", craft.id, CraftStatus.PLANNED)
        setBought("e", ruban.id, true)
        assertEquals(750, budget().plannedCents)
        assertEquals(450, budget().spentCents)
        setBought("e", ruban.id, false)
        setBought("e", mousse.id, true)
        assertEquals(300, budget().spentCents)

        setStatus("e", craft.id, CraftStatus.DONE)
        assertEquals(CraftStatus.DONE, repo.getYear("e", craft.id)?.status)
        setResult("e", craft.id, "photos/resultat.jpg")
        assertEquals("photos/resultat.jpg", repo.getYear("e", craft.id)?.resultPhotoPath)

        // Retour à l'idée : la ligne de l'année et sa photo disparaissent.
        setStatus("e", craft.id, null)
        assertNull(repo.getYear("e", craft.id))
        assertTrue("photos/resultat.jpg" in photos.deleted)
        assertEquals(0, budget().plannedCents)

        // Une année archivée est en lecture seule.
        assertEquals(AppResult.Failure(AppError.ReadOnly), setStatus("old", craft.id, CraftStatus.PLANNED))
        assertEquals(AppResult.Failure(AppError.ReadOnly), setBought("old", ruban.id, true))
    }

    @Test
    fun `supprimer un bricolage supprime ses photos`() = runTest {
        seedEditions()
        val craft = (save(null, CraftDraft("Bougies", photoPath = "photos/b.jpg")) as AppResult.Success).value
        setResult("e", craft.id, "photos/r.jpg")
        assertEquals(CraftStatus.DONE, repo.getYear("e", craft.id)?.status)
        DeleteCraftUseCase(repo, photos)(craft.id)
        assertTrue(repo.crafts.value.isEmpty())
        assertTrue(photos.deleted.containsAll(listOf("photos/b.jpg", "photos/r.jpg")))
    }

    @Test
    fun `idees a piocher sans doublon`() = runTest {
        assertEquals(craftIdeas.size, craftIdeas.map { it.title }.toSet().size)
        val add = AddCraftIdeasUseCase(save, repo, ids)
        assertEquals(1, add(listOf(craftIdeas.first())))
        assertEquals(craftIdeas.first().supplies.size, repo.supplies.value.size)
        assertEquals(craftIdeas.size - 1, availableCraftIdeas(repo.crafts.value).size)
    }

    @Test
    fun `decoration favorite et suppression`() = runTest {
        val saveDecor = SaveDecorUseCase(repo, photos, time, ids)
        assertEquals(AppResult.Failure(AppError.InvalidInput), saveDecor(null, DecorDraft(" ")))
        val d = (saveDecor(null, DecorDraft("", DecorPlace.TABLE, photoPath = "photos/t.jpg")) as AppResult.Success).value
        saveDecor(d.id, DecorDraft("Table dorée", DecorPlace.TABLE, "photos/t.jpg", favorite = true))
        assertEquals(true, repo.getDecor(d.id)?.favorite)
        DeleteDecorUseCase(repo, photos)(d.id)
        assertTrue(repo.decor.value.isEmpty())
        assertTrue("photos/t.jpg" in photos.deleted)
    }
}

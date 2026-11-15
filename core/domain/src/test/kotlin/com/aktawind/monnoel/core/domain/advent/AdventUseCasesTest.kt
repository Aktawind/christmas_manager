package com.aktawind.monnoel.core.domain.advent

import com.aktawind.monnoel.core.common.AppError
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.edition.FakeEditionRepository
import com.aktawind.monnoel.core.domain.edition.FixedTime
import com.aktawind.monnoel.core.domain.edition.SequentialIds
import com.aktawind.monnoel.core.domain.gifts.EditionGuard
import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import com.aktawind.monnoel.core.model.AdventBox
import com.aktawind.monnoel.core.model.AdventCalendar
import com.aktawind.monnoel.core.model.AdventSupply
import com.aktawind.monnoel.core.model.AdventTemplate
import com.aktawind.monnoel.core.model.AdventTemplateBox
import com.aktawind.monnoel.core.model.BoxStatus
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.model.EditionStatus
import com.aktawind.monnoel.core.model.SurpriseType
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest

class FakeAdventRepository : AdventRepository {
    val calendars = MutableStateFlow<List<AdventCalendar>>(emptyList())
    val boxes = MutableStateFlow<List<AdventBox>>(emptyList())
    val supplies = MutableStateFlow<List<AdventSupply>>(emptyList())
    val templates = MutableStateFlow<List<AdventTemplate>>(emptyList())
    val templateBoxes = MutableStateFlow<List<AdventTemplateBox>>(emptyList())

    override fun observeCalendars(editionId: String) = calendars.map { l -> l.filter { it.editionId == editionId } }

    override fun observeAllCalendars() = calendars

    override fun observeCalendar(id: String) = calendars.map { l -> l.firstOrNull { it.id == id } }

    override suspend fun getCalendar(id: String) = calendars.value.firstOrNull { it.id == id }

    override suspend fun saveCalendar(calendar: AdventCalendar) {
        calendars.value = calendars.value.filterNot { it.id == calendar.id } + calendar
    }

    override suspend fun deleteCalendar(id: String) {
        calendars.value = calendars.value.filterNot { it.id == id }
        boxes.value = boxes.value.filterNot { it.calendarId == id }
    }

    override fun observeBoxes(calendarId: String) = boxes.map { l -> l.filter { it.calendarId == calendarId } }

    override fun observeBoxesForEdition(editionId: String): Flow<List<AdventBox>> = boxes

    override fun observeAllBoxes() = boxes

    override suspend fun getBoxes(calendarId: String) = boxes.value.filter {
        it.calendarId == calendarId
    }.sortedBy { it.number }

    override suspend fun getBox(id: String) = boxes.value.firstOrNull { it.id == id }

    override suspend fun saveBoxes(boxes: List<AdventBox>) {
        val ids = boxes.map { it.id }.toSet()
        this.boxes.value = this.boxes.value.filterNot { it.id in ids } + boxes
    }

    override suspend fun deleteBoxes(ids: List<String>) {
        boxes.value = boxes.value.filterNot { it.id in ids }
    }

    override suspend fun swapBoxes(firstId: String, secondId: String) {
        val a = getBox(firstId)!!
        val b = getBox(secondId)!!
        saveBoxes(listOf(a.copy(number = b.number), b.copy(number = a.number)))
    }

    override fun observeSupplies(calendarId: String) = supplies.map { l -> l.filter { it.calendarId == calendarId } }

    override fun observeSuppliesForEdition(editionId: String) = supplies

    override suspend fun getSupplies(calendarId: String) = supplies.value.filter { it.calendarId == calendarId }

    override suspend fun saveSupply(supply: AdventSupply, linkedBoxIds: Set<String>) {
        supplies.value = supplies.value.filterNot { it.id == supply.id } + supply
        boxes.value = boxes.value.map {
            when {
                it.id in linkedBoxIds -> it.copy(supplyId = supply.id)
                it.supplyId == supply.id -> it.copy(supplyId = null)
                else -> it
            }
        }
    }

    override suspend fun deleteSupply(id: String) {
        supplies.value = supplies.value.filterNot { it.id == id }
    }

    override fun observeTemplates() = templates

    override fun observeAllTemplateBoxes() = templateBoxes

    override suspend fun getTemplate(id: String) = templates.value.firstOrNull { it.id == id }

    override suspend fun getTemplateBoxes(templateId: String) = templateBoxes.value.filter {
        it.templateId == templateId
    }

    override suspend fun saveTemplate(template: AdventTemplate, boxes: List<AdventTemplateBox>) {
        templates.value = templates.value + template
        templateBoxes.value = templateBoxes.value + boxes
    }

    override suspend fun deleteTemplate(id: String) {
        templates.value = templates.value.filterNot { it.id == id }
    }
}

private class NoPhotos : PhotoRepository {
    var copies = 0

    override suspend fun importPhoto(sourceUri: String) = AppResult.Success("photos/x.jpg")

    override suspend fun copyPhoto(relativePath: String) = "photos/copy-${copies++}.jpg"

    override suspend fun deletePhoto(relativePath: String) = Unit
}

class AdventUseCasesTest {
    private val editions = FakeEditionRepository()
    private val advent = FakeAdventRepository()
    private val photos = NoPhotos()
    private val ids = SequentialIds()
    private val time = FixedTime()
    private val guard = EditionGuard(editions)
    private val save = SaveCalendarUseCase(advent, photos, guard, time, ids)

    private suspend fun edition(id: String, year: Int, archived: Boolean = false) = editions.insert(
        ChristmasEdition(
            id,
            year,
            if (archived) EditionStatus.ARCHIVED else EditionStatus.ACTIVE,
            null,
            Instant.EPOCH,
            Instant.EPOCH,
        ),
    )

    @Test
    fun `creer un calendrier cree ses cases, puis ajuster leur nombre`() = runTest {
        edition("e", 2026)
        val cal = (save(null, "e", CalendarDraft(name = "Lucas")) as AppResult.Success).value
        assertEquals((1..24).toList(), advent.getBoxes(cal.id).map { it.number })

        save(cal.id, "e", CalendarDraft(name = "Lucas", boxCount = 25))
        assertEquals(25, advent.getBoxes(cal.id).size)
        save(cal.id, "e", CalendarDraft(name = "Lucas", boxCount = 12))
        assertEquals((1..12).toList(), advent.getBoxes(cal.id).map { it.number })
    }

    @Test
    fun `nombre de cases et annee archivee`() = runTest {
        edition("e", 2026)
        edition("old", 2024, archived = true)
        assertEquals(AppResult.Failure(AppError.InvalidInput), save(null, "e", CalendarDraft(name = "X", boxCount = 0)))
        assertEquals(AppResult.Failure(AppError.ReadOnly), save(null, "old", CalendarDraft(name = "X")))
    }

    @Test
    fun `echanger deux cases`() = runTest {
        edition("e", 2026)
        val cal = (save(null, "e", CalendarDraft(name = "L")) as AppResult.Success).value
        val (one, two) = advent.getBoxes(cal.id).take(2)
        SaveBoxUseCase(advent, photos, guard)(one.id, BoxDraft(type = SurpriseType.TREAT, title = "Chocolat"))
        SwapBoxesUseCase(advent, guard)(one.id, two.id)
        assertEquals("Chocolat", advent.getBoxes(cal.id).first { it.number == 2 }.title)
        assertEquals("", advent.getBoxes(cal.id).first { it.number == 1 }.title)
    }

    @Test
    fun `achat groupe reparti et resume du calendrier`() = runTest {
        edition("e", 2026)
        val cal = (
            save(
                null,
                "e",
                CalendarDraft(name = "L", boxCount = 3, budgetCents = 2_000),
            ) as AppResult.Success
            ).value
        val boxes = advent.getBoxes(cal.id)
        SaveBoxUseCase(
            advent,
            photos,
            guard,
        )(boxes[0].id, BoxDraft(title = "Livre", priceCents = 800, status = BoxStatus.READY))
        val supply = (
            SaveSupplyUseCase(advent, photos, guard, ids)(
                cal.id,
                null,
                SupplyDraft("Sachet de chocolats", priceCents = 1_000, bought = true),
                boxes.drop(1).map { it.id }.toSet(),
            )
                as AppResult.Success
            ).value
        val linked = advent.getBoxes(cal.id).filter { it.supplyId == supply.id }
        assertEquals(mapOf(boxes[1].id to 500L, boxes[2].id to 500L), supplyShares(supply, linked))

        val summary = summarizeCalendar(3, 2_000, advent.getBoxes(cal.id), advent.getSupplies(cal.id))
        assertEquals(1, summary.ready)
        // Achat fait : les cases reliées passent « À préparer » toutes seules.
        assertEquals(2, summary.toPrepare)
        assertEquals(0, summary.toFind)
        assertEquals(1_800, summary.plannedCents)
        assertEquals(1_800, summary.spentCents)
        assertTrue(!summary.isOverBudget)
    }

    @Test
    fun `dupliquer remet les cases a trouver et les fournitures a acheter`() = runTest {
        edition("old", 2025)
        edition("e", 2026)
        val cal = (save(null, "old", CalendarDraft(name = "Lucas", boxCount = 2)) as AppResult.Success).value
        val boxes = advent.getBoxes(cal.id)
        SaveBoxUseCase(
            advent,
            photos,
            guard,
        )(boxes[0].id, BoxDraft(title = "Puzzle", status = BoxStatus.READY, photoPath = "photos/p.jpg"))
        SaveSupplyUseCase(advent, photos, guard, ids)(cal.id, null, SupplyDraft("Étiquettes", 24, 300, true), setOf(boxes[1].id))

        val copy = (DuplicateCalendarUseCase(advent, photos, guard, time, ids)(cal.id, "e") as AppResult.Success).value
        val copied = advent.getBoxes(copy.id)
        assertEquals("e", copy.editionId)
        assertEquals("Puzzle", copied[0].title)
        assertEquals(BoxStatus.TO_FIND, copied[0].status)
        assertTrue(copied[0].photoPath!!.startsWith("photos/copy"))
        val supply = advent.getSupplies(copy.id).single()
        assertEquals(false, supply.bought)
        assertEquals(supply.id, copied[1].supplyId)
    }

    @Test
    fun `modele enregistre puis reutilise`() = runTest {
        edition("e", 2026)
        val cal = (
            save(
                null,
                "e",
                CalendarDraft(name = "Lutin", theme = "Lutin farceur", boxCount = 24),
            ) as AppResult.Success
            ).value
        val box5 = advent.getBoxes(cal.id).first { it.number == 5 }
        SaveBoxUseCase(advent, photos, guard)(box5.id, BoxDraft(type = SurpriseType.GAME, title = "Énigme"))
        val template = (SaveAsTemplateUseCase(advent, time, ids)(cal.id, "Mon lutin") as AppResult.Success).value
        assertEquals(1, advent.getTemplateBoxes(template.id).size)

        val created = (
            CreateFromTemplateUseCase(
                advent,
                save,
            )(template.id, "e", "Lutin 2", emptySet()) as AppResult.Success
            ).value
        val boxes = advent.getBoxes(created.id)
        assertEquals(24, boxes.size)
        assertEquals("Énigme", boxes.first { it.number == 5 }.title)
        assertEquals("Lutin farceur", created.theme)
    }

    @Test
    fun `case du jour`() {
        assertEquals(1, boxOfTheDay(LocalDate.of(2026, 12, 1), 2026, 24))
        assertEquals(24, boxOfTheDay(LocalDate.of(2026, 12, 24), 2026, 24))
        assertNull(boxOfTheDay(LocalDate.of(2026, 12, 25), 2026, 24))
        assertEquals(25, boxOfTheDay(LocalDate.of(2026, 12, 25), 2026, 25))
        assertNull(boxOfTheDay(LocalDate.of(2026, 11, 30), 2026, 24))
        assertNull(boxOfTheDay(LocalDate.of(2026, 12, 3), 2025, 24))
    }

    @Test
    fun `relier un achat groupe remplit les cases`() = runTest {
        edition("e", 2026)
        val cal = (save(null, "e", CalendarDraft(name = "Chocolats", boxCount = 4)) as AppResult.Success).value
        val ids4 = advent.getBoxes(cal.id).map { it.id }
        val draft =
            SupplyDraft("Boîte de 24 chocolats", priceCents = 1_200, type = SurpriseType.TREAT, boxTitle = "Chocolat", supplier = "Lidl")
        val supply = (SaveSupplyUseCase(advent, photos, guard, ids)(cal.id, null, draft, ids4.take(3).toSet()) as AppResult.Success).value
        val boxes = advent.getBoxes(cal.id)
        boxes.take(3).forEach {
            assertEquals(SurpriseType.TREAT, it.type)
            assertEquals("Chocolat", it.title)
            assertEquals("Lidl", it.supplier)
            assertEquals(BoxStatus.TO_FIND, it.status)
        }
        assertTrue(boxes[3].isEmpty)
        // L'achat est fait : les cases passent « À préparer ».
        SaveSupplyUseCase(advent, photos, guard, ids)(cal.id, supply.id, draft.copy(bought = true), ids4.take(3).toSet())
        assertEquals(BoxStatus.TO_PREPARE, advent.getBoxes(cal.id)[0].status)
    }

    @Test
    fun `modifier plusieurs cases d'un coup`() = runTest {
        edition("e", 2026)
        val cal = (save(null, "e", CalendarDraft(name = "L", boxCount = 5)) as AppResult.Success).value
        val ids5 = advent.getBoxes(cal.id).map { it.id }
        val change = BulkBoxChange(type = SurpriseType.TREAT, status = BoxStatus.READY, title = "Chocolat")
        val count = (BulkUpdateBoxesUseCase(advent, guard)(cal.id, ids5.take(4).toSet(), change) as AppResult.Success).value
        assertEquals(4, count)
        val boxes = advent.getBoxes(cal.id)
        assertEquals(4, boxes.count { it.title == "Chocolat" && it.status == BoxStatus.READY && it.type == SurpriseType.TREAT })
        assertTrue(boxes[4].isEmpty)

        val supply = (
            SaveSupplyUseCase(advent, photos, guard, ids)(cal.id, null, SupplyDraft("Sachets", boxTitle = "Bonbon"), emptySet())
                as AppResult.Success
            ).value
        BulkUpdateBoxesUseCase(advent, guard)(cal.id, setOf(ids5[4]), BulkBoxChange(supplyId = supply.id))
        assertEquals("Bonbon", advent.getBoxes(cal.id)[4].title)
        assertEquals(supply.id, advent.getBoxes(cal.id)[4].supplyId)
    }
}

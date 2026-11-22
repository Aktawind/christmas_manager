package com.aktawind.monnoel.core.domain.tasks

import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.edition.FakeEditionRepository
import com.aktawind.monnoel.core.domain.edition.FixedTime
import com.aktawind.monnoel.core.domain.edition.SequentialIds
import com.aktawind.monnoel.core.domain.gifts.EditionGuard
import com.aktawind.monnoel.core.model.AdventBox
import com.aktawind.monnoel.core.model.AdventCalendar
import com.aktawind.monnoel.core.model.AdventSupply
import com.aktawind.monnoel.core.model.BoxStatus
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.model.EditionStatus
import com.aktawind.monnoel.core.model.GiftStatus
import com.aktawind.monnoel.core.model.LutinTask
import com.aktawind.monnoel.core.model.PlannedGift
import com.aktawind.monnoel.core.model.SuggestionKind
import com.aktawind.monnoel.core.model.TaskStatus
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest

class FakeTaskRepository : TaskRepository {
    val tasks = MutableStateFlow<List<LutinTask>>(emptyList())
    val dismissed = MutableStateFlow<Set<Pair<String, String>>>(emptySet())

    override fun observeTasks(editionId: String) = tasks.map { l -> l.filter { it.editionId == editionId } }

    override fun observeAllTasks() = tasks

    override fun observeTask(id: String) = tasks.map { l -> l.firstOrNull { it.id == id } }

    override suspend fun getTask(id: String) = tasks.value.firstOrNull { it.id == id }

    override suspend fun getTasks(editionId: String) = tasks.value.filter { it.editionId == editionId }

    override suspend fun save(task: LutinTask) {
        tasks.value = tasks.value.filterNot { it.id == task.id } + task
    }

    override suspend fun saveAll(tasks: List<LutinTask>) = tasks.forEach { save(it) }

    override suspend fun delete(id: String) {
        tasks.value = tasks.value.filterNot { it.id == id }
    }

    override fun observeDismissed(editionId: String) = dismissed.map { s -> s.filter { it.first == editionId }.map { it.second }.toSet() }

    override suspend fun dismiss(editionId: String, key: String) {
        dismissed.value = dismissed.value + (editionId to key)
    }
}

class SuggestionsTest {
    private fun gift(id: String, status: GiftStatus) = PlannedGift(
        id, "e", "p", "Léa", null, "Puzzle $id", "", null, 1_000, null, "", "", "", "", null, status, Instant.EPOCH, Instant.EPOCH,
    )

    private fun box(id: String, number: Int, status: BoxStatus) =
        AdventBox(id, "c", number, null, "", null, null, "", "", status, "", null)

    private val calendar =
        AdventCalendar("c", "e", "Calendrier de Tom", emptySet(), "", null, "", null, "", 3, Instant.EPOCH, Instant.EPOCH)

    private fun source(vararg gifts: PlannedGift, boxes: List<AdventBox> = emptyList(), supplies: List<AdventSupply> = emptyList()) =
        SuggestionSource(gifts.toList(), listOf(calendar), boxes, supplies)

    @Test
    fun `suggestions selon l'etat des cadeaux et des calendriers`() {
        val src = source(
            gift("a", GiftStatus.ORDERED),
            gift("b", GiftStatus.TO_WRAP),
            gift("c", GiftStatus.GIVEN),
            boxes = listOf(box("1", 1, BoxStatus.TO_FIND), box("2", 2, BoxStatus.TO_PREPARE), box("3", 3, BoxStatus.READY)),
            supplies = listOf(AdventSupply("s", "c", "Sachets", 24, 500, false, 0)),
        )
        val suggestions = currentSuggestions(src, emptySet(), emptySet(), emptyList())
        assertEquals(
            setOf("GIFT_DELIVERY:a", "GIFT_WRAP:b", "SUPPLY_BUY:s", "ADVENT_FIND:c", "ADVENT_PREPARE:c"),
            suggestions.map { it.key }.toSet(),
        )
        assertEquals(1, suggestions.first { it.kind == SuggestionKind.ADVENT_FIND }.count)
    }

    @Test
    fun `types desactives, suggestions supprimees ou deja ajoutees`() {
        val src = source(gift("a", GiftStatus.ORDERED), gift("b", GiftStatus.TO_WRAP))
        val task = LutinTask(
            "t", "e", "Emballer", "", TaskStatus.TODO, com.aktawind.monnoel.core.model.TaskModule.GIFTS, "", null, null, "GIFT_WRAP:b",
            Instant.EPOCH, Instant.EPOCH,
        )
        assertTrue(currentSuggestions(src, setOf(SuggestionKind.GIFT_DELIVERY), emptySet(), listOf(task)).isEmpty())
        assertTrue(currentSuggestions(src, emptySet(), setOf("GIFT_DELIVERY:a"), listOf(task)).isEmpty())
    }

    @Test
    fun `une tache suggeree se coche toute seule`() = runTest {
        val editions = FakeEditionRepository()
        editions.insert(ChristmasEdition("e", 2026, EditionStatus.ACTIVE, null, Instant.EPOCH, Instant.EPOCH))
        val repo = FakeTaskRepository()
        val save = SaveTaskUseCase(repo, EditionGuard(editions), FixedTime(), SequentialIds())
        val suggestion = currentSuggestions(source(gift("a", GiftStatus.ORDERED)), emptySet(), emptySet(), emptyList()).single()
        val task = (AcceptSuggestionUseCase(save)("e", suggestion, "Vérifier la livraison") as AppResult.Success).value
        assertEquals("GIFT_DELIVERY:a", task.suggestionKey)

        val sync = SyncSuggestedTasksUseCase(repo, FixedTime())
        assertEquals(0, sync("e", suggestionStates(source(gift("a", GiftStatus.ORDERED)))))
        assertEquals(1, sync("e", suggestionStates(source(gift("a", GiftStatus.TO_WRAP)))))
        assertEquals(TaskStatus.DONE, repo.getTask(task.id)?.status)
        // Le cadeau revient à « Commandé » : la tâche redevient à faire.
        sync("e", suggestionStates(source(gift("a", GiftStatus.ORDERED))))
        assertEquals(TaskStatus.TODO, repo.getTask(task.id)?.status)

        // Supprimer la tâche : la suggestion ne revient pas.
        DeleteTaskUseCase(repo, EditionGuard(editions))(task.id)
        val again =
            currentSuggestions(
                source(gift("a", GiftStatus.ORDERED)),
                emptySet(),
                repo.dismissed.value.map {
                    it.second
                }.toSet(),
                repo.tasks.value,
            )
        assertTrue(again.isEmpty())
    }
}

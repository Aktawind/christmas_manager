package com.aktawind.monnoel.core.domain.gifts

import com.aktawind.monnoel.core.common.AppError
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.edition.FakeEditionRepository
import com.aktawind.monnoel.core.domain.edition.FixedTime
import com.aktawind.monnoel.core.domain.edition.SequentialIds
import com.aktawind.monnoel.core.domain.tasks.FakeTaskRepository
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.model.EditionStatus
import com.aktawind.monnoel.core.model.IdeaRequest
import com.aktawind.monnoel.core.model.IdeaRequestKind
import com.aktawind.monnoel.core.model.Person
import com.aktawind.monnoel.core.model.TaskStatus
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest

class FakeIdeaRequests : IdeaRequestRepository {
    val items = MutableStateFlow<List<IdeaRequest>>(emptyList())

    override fun observeRequests(editionId: String) = items.map { l -> l.filter { it.editionId == editionId } }

    override suspend fun getRequest(id: String) = items.value.firstOrNull { it.id == id }

    override suspend fun saveRequest(request: IdeaRequest) {
        items.value = items.value.filterNot { it.id == request.id } + request
    }

    override suspend fun deleteRequest(id: String) {
        items.value = items.value.filterNot { it.id == id }
    }
}

class IdeaRequestsTest {
    private val t = Instant.EPOCH
    private val editions = FakeEditionRepository()
    private val persons = FakePersons()
    private val requests = FakeIdeaRequests()
    private val tasks = FakeTaskRepository()
    private val guard = EditionGuard(editions)
    private val ids = SequentialIds()
    private val create = CreateIdeaRequestUseCase(requests, tasks, persons, guard, FixedTime(), ids)

    private suspend fun setUp() {
        editions.insert(ChristmasEdition("e", 2026, EditionStatus.ACTIVE, null, t, t))
        editions.insert(ChristmasEdition("old", 2025, EditionStatus.ARCHIVED, null, t, t))
        persons.upsert(Person("yannick", "Yannick", "", null, "", "", t, t))
        persons.upsert(Person("christine", "Christine", "", null, "", "", t, t))
    }

    @Test
    fun `chaque demande ajoute sa tache dans Ma liste`() = runTest {
        setUp()
        create("e", IdeaRequestKind.TO_SEND, "yannick", "christine")
        create("e", IdeaRequestKind.TO_RECEIVE, "yannick", "christine")
        create("e", IdeaRequestKind.TO_RECEIVE, "yannick", "yannick")
        assertEquals(
            listOf(
                "Envoyer la liste au Père Noël de Yannick à Christine",
                "En attente de la liste au Père Noël de Christine (de la part de Yannick)",
                "En attente de la liste au Père Noël de Yannick",
            ),
            tasks.tasks.value.map { it.title },
        )
        assertEquals(AppResult.Failure(AppError.InvalidInput), create("e", IdeaRequestKind.TO_SEND, "yannick", null, " "))
        assertEquals(AppResult.Failure(AppError.ReadOnly), create("old", IdeaRequestKind.TO_SEND, "yannick", "christine"))
    }

    @Test
    fun `faite, la demande coche sa tache, supprimee, la tache disparait`() = runTest {
        setUp()
        val request = (create("e", IdeaRequestKind.TO_SEND, "yannick", null, "Collègues") as AppResult.Success).value
        val setDone = SetIdeaRequestDoneUseCase(requests, tasks, persons, guard, FixedTime(), ids)
        setDone(request.id, true)
        assertEquals(TaskStatus.DONE, tasks.tasks.value.single().status)
        // Tâche supprimée de Ma liste : elle est recréée au besoin.
        tasks.delete(request.taskId!!)
        setDone(request.id, false)
        assertEquals("Envoyer la liste au Père Noël de Yannick à Collègues", tasks.tasks.value.single().title)
        DeleteIdeaRequestUseCase(requests, tasks, guard)(request.id)
        assertTrue(tasks.tasks.value.isEmpty())
        assertTrue(requests.items.value.isEmpty())
    }
}

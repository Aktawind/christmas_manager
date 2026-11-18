package com.aktawind.monnoel.core.domain.gifts

import com.aktawind.monnoel.core.common.AppError
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.IdGenerator
import com.aktawind.monnoel.core.common.TimeProvider
import com.aktawind.monnoel.core.domain.repository.PersonRepository
import com.aktawind.monnoel.core.domain.tasks.TaskRepository
import com.aktawind.monnoel.core.model.IdeaRequest
import com.aktawind.monnoel.core.model.IdeaRequestKind
import com.aktawind.monnoel.core.model.LinkType
import com.aktawind.monnoel.core.model.LutinTask
import com.aktawind.monnoel.core.model.TaskModule
import com.aktawind.monnoel.core.model.TaskStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

interface IdeaRequestRepository {
    fun observeRequests(editionId: String): Flow<List<IdeaRequest>>

    suspend fun getRequest(id: String): IdeaRequest?

    suspend fun saveRequest(request: IdeaRequest)

    suspend fun deleteRequest(id: String)
}

/**
 * Titre de la tâche ajoutée à Ma liste :
 * - TO_SEND : « Envoyer la liste au Père Noël de Yannick à Christine » ;
 * - TO_RECEIVE : « En attente de la liste au Père Noël de Christine (de la part de Yannick) »,
 *   ou « En attente de la liste au Père Noël de Yannick » s'il s'agit de ses propres idées.
 */
fun ideaRequestTaskTitle(kind: IdeaRequestKind, personName: String, otherName: String, otherIsSelf: Boolean): String = when (kind) {
    IdeaRequestKind.TO_SEND -> "Envoyer la liste au Père Noël de $personName à $otherName"
    IdeaRequestKind.TO_RECEIVE ->
        if (otherIsSelf) {
            "En attente de la liste au Père Noël de $personName"
        } else {
            "En attente de la liste au Père Noël de $otherName (de la part de $personName)"
        }
}

/** Crée une demande d'idées et sa tâche dans Ma liste (ajoutée automatiquement). */
class CreateIdeaRequestUseCase @Inject constructor(
    private val requests: IdeaRequestRepository,
    private val tasks: TaskRepository,
    private val persons: PersonRepository,
    private val guard: EditionGuard,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(
        editionId: String,
        kind: IdeaRequestKind,
        personId: String,
        otherPersonId: String?,
        otherName: String = "",
    ): AppResult<IdeaRequest> {
        guard.check(editionId)?.let { return AppResult.Failure(it) }
        val person = persons.getPerson(personId) ?: return AppResult.Failure(AppError.NotFound)
        val other = otherPersonId?.let { persons.getPerson(it)?.displayName } ?: otherName.trim()
        if (other.isBlank()) return AppResult.Failure(AppError.InvalidInput)
        val now = time.now()
        val task = LutinTask(
            id = ids.newId(),
            editionId = editionId,
            title = ideaRequestTaskTitle(kind, person.displayName, other, otherPersonId == personId),
            description = "",
            status = TaskStatus.TODO,
            module = TaskModule.GIFTS,
            notes = "",
            linkType = LinkType.PERSON,
            linkId = personId,
            suggestionKey = null,
            createdAt = now,
            updatedAt = now,
        )
        tasks.save(task)
        val request = IdeaRequest(ids.newId(), editionId, kind, personId, otherPersonId, other, task.id, now)
        requests.saveRequest(request)
        return AppResult.Success(request)
    }
}

/** Marque une demande comme faite (ou non) : c'est sa tâche de Ma liste qui porte l'état. */
class SetIdeaRequestDoneUseCase @Inject constructor(
    private val requests: IdeaRequestRepository,
    private val tasks: TaskRepository,
    private val persons: PersonRepository,
    private val guard: EditionGuard,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(id: String, done: Boolean): AppResult<Unit> {
        val request = requests.getRequest(id) ?: return AppResult.Failure(AppError.NotFound)
        guard.check(request.editionId)?.let { return AppResult.Failure(it) }
        val status = if (done) TaskStatus.DONE else TaskStatus.TODO
        val now = time.now()
        val task = request.taskId?.let { tasks.getTask(it) }
        if (task != null) {
            tasks.save(task.copy(status = status, updatedAt = now))
        } else {
            // La tâche a été supprimée de Ma liste : on la recrée.
            val name = persons.getPerson(request.personId)?.displayName.orEmpty()
            val recreated = LutinTask(
                ids.newId(), request.editionId,
                ideaRequestTaskTitle(request.kind, name, request.otherName, request.otherPersonId == request.personId),
                "", status, TaskModule.GIFTS, "", LinkType.PERSON, request.personId, null, now, now,
            )
            tasks.save(recreated)
            requests.saveRequest(request.copy(taskId = recreated.id))
        }
        return AppResult.Success(Unit)
    }
}

/** Supprime une demande et sa tâche. */
class DeleteIdeaRequestUseCase @Inject constructor(
    private val requests: IdeaRequestRepository,
    private val tasks: TaskRepository,
    private val guard: EditionGuard,
) {
    suspend operator fun invoke(id: String): AppResult<Unit> {
        val request = requests.getRequest(id) ?: return AppResult.Failure(AppError.NotFound)
        guard.check(request.editionId)?.let { return AppResult.Failure(it) }
        requests.deleteRequest(id)
        request.taskId?.let { tasks.delete(it) }
        return AppResult.Success(Unit)
    }
}

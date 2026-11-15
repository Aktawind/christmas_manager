package com.aktawind.monnoel.core.domain.tasks

import com.aktawind.monnoel.core.common.AppError
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.IdGenerator
import com.aktawind.monnoel.core.common.TimeProvider
import com.aktawind.monnoel.core.domain.gifts.EditionGuard
import com.aktawind.monnoel.core.model.LinkType
import com.aktawind.monnoel.core.model.LutinTask
import com.aktawind.monnoel.core.model.TaskModule
import com.aktawind.monnoel.core.model.TaskStatus
import javax.inject.Inject

data class TaskDraft(
    val title: String,
    val description: String = "",
    val status: TaskStatus = TaskStatus.TODO,
    val module: TaskModule = TaskModule.OTHER,
    val notes: String = "",
    val linkType: LinkType? = null,
    val linkId: String? = null,
)

class SaveTaskUseCase @Inject constructor(
    private val tasks: TaskRepository,
    private val guard: EditionGuard,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(id: String?, editionId: String, draft: TaskDraft, suggestionKey: String? = null): AppResult<LutinTask> {
        if (draft.title.isBlank()) return AppResult.Failure(AppError.InvalidInput)
        guard.check(editionId)?.let { return AppResult.Failure(it) }
        val existing = id?.let { tasks.getTask(it) }
        val now = time.now()
        val task = LutinTask(
            id = existing?.id ?: ids.newId(),
            editionId = existing?.editionId ?: editionId,
            title = draft.title.trim(),
            description = draft.description.trim(),
            status = draft.status,
            module = draft.module,
            notes = draft.notes.trim(),
            linkType = draft.linkType ?: existing?.linkType,
            linkId = draft.linkId ?: existing?.linkId,
            suggestionKey = existing?.suggestionKey ?: suggestionKey,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        tasks.save(task)
        return AppResult.Success(task)
    }
}

class SetTaskStatusUseCase @Inject constructor(
    private val tasks: TaskRepository,
    private val guard: EditionGuard,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(id: String, status: TaskStatus): AppResult<Unit> {
        val task = tasks.getTask(id) ?: return AppResult.Failure(AppError.NotFound)
        guard.check(task.editionId)?.let { return AppResult.Failure(it) }
        tasks.save(task.copy(status = status, updatedAt = time.now()))
        return AppResult.Success(Unit)
    }
}

class DeleteTaskUseCase @Inject constructor(private val tasks: TaskRepository, private val guard: EditionGuard) {
    suspend operator fun invoke(id: String): AppResult<Unit> {
        val task = tasks.getTask(id) ?: return AppResult.Failure(AppError.NotFound)
        guard.check(task.editionId)?.let { return AppResult.Failure(it) }
        tasks.delete(id)
        // Une tâche venue d'une suggestion et supprimée ne doit pas être reproposée.
        task.suggestionKey?.let { tasks.dismiss(task.editionId, it) }
        return AppResult.Success(Unit)
    }
}

/** Ajoute une suggestion à la liste, avec son libellé déjà rédigé par l'écran. */
class AcceptSuggestionUseCase @Inject constructor(private val saveTask: SaveTaskUseCase) {
    suspend operator fun invoke(editionId: String, suggestion: Suggestion, title: String): AppResult<LutinTask> = saveTask(
        id = null,
        editionId = editionId,
        draft = TaskDraft(title = title, module = suggestion.module, linkType = suggestion.linkType, linkId = suggestion.linkId),
        suggestionKey = suggestion.key,
    )
}

class DismissSuggestionUseCase @Inject constructor(private val tasks: TaskRepository) {
    suspend operator fun invoke(editionId: String, key: String) = tasks.dismiss(editionId, key)
}

/**
 * Coche toute seule une tâche venue d'une suggestion quand c'est fait (cadeau reçu, fourniture
 * achetée…), et la remet « à faire » si ça redevient nécessaire. Ne touche pas aux tâches annulées.
 */
class SyncSuggestedTasksUseCase @Inject constructor(private val tasks: TaskRepository, private val time: TimeProvider) {
    suspend operator fun invoke(editionId: String, states: Map<String, Boolean>): Int {
        val now = time.now()
        val changed = tasks.getTasks(editionId).mapNotNull { task ->
            val done = task.suggestionKey?.let { states[it] } ?: return@mapNotNull null
            when {
                done && task.isOpen -> task.copy(status = TaskStatus.DONE, updatedAt = now)
                !done && task.status == TaskStatus.DONE -> task.copy(status = TaskStatus.TODO, updatedAt = now)
                else -> null
            }
        }
        if (changed.isNotEmpty()) tasks.saveAll(changed)
        return changed.size
    }
}

package com.aktawind.monnoel.core.domain.gifts

import com.aktawind.monnoel.core.common.AppError
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.IdGenerator
import com.aktawind.monnoel.core.common.TimeProvider
import com.aktawind.monnoel.core.domain.repository.PersonRepository
import com.aktawind.monnoel.core.domain.search.normalizeForSearch
import com.aktawind.monnoel.core.model.WhisperedIdea
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

interface WhisperRepository {
    fun observeWhispers(editionId: String): Flow<List<WhisperedIdea>>

    fun observeAllWhispers(): Flow<List<WhisperedIdea>>

    suspend fun getWhisper(id: String): WhisperedIdea?

    suspend fun saveWhisper(whisper: WhisperedIdea)

    suspend fun deleteWhisper(id: String)
}

data class WhisperDraft(
    val recipientId: String,
    val title: String,
    val givenToPersonId: String? = null,
    val givenToName: String = "",
    val priceCents: Long? = null,
    val url: String = "",
    val received: Boolean = false,
    val notes: String = "",
)

/**
 * Autres idées soufflées pour la même personne, le même Noël, au même titre (sans tenir compte
 * des accents ni des majuscules) mais à quelqu'un d'autre : risque de doublon.
 */
fun whisperDuplicates(
    all: List<WhisperedIdea>,
    editionId: String,
    recipientId: String,
    title: String,
    givenToPersonId: String?,
    givenToName: String,
    excludeId: String?,
): List<WhisperedIdea> {
    val key = normalizeForSearch(title)
    if (key.isBlank()) return emptyList()
    val giverKey = givenToPersonId ?: normalizeForSearch(givenToName)
    return all.filter {
        it.id != excludeId &&
            it.editionId == editionId &&
            it.recipientId == recipientId &&
            normalizeForSearch(it.title) == key &&
            (it.givenToPersonId ?: normalizeForSearch(it.givenToName)) != giverKey
    }
}

class SaveWhisperUseCase @Inject constructor(
    private val whispers: WhisperRepository,
    private val persons: PersonRepository,
    private val guard: EditionGuard,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(id: String?, editionId: String, draft: WhisperDraft): AppResult<WhisperedIdea> {
        val giverName = draft.givenToPersonId?.let { persons.getPerson(it)?.displayName } ?: draft.givenToName.trim()
        if (draft.title.isBlank() || giverName.isBlank() || (draft.priceCents ?: 0) < 0) return AppResult.Failure(AppError.InvalidInput)
        val existing = id?.let { whispers.getWhisper(it) }
        guard.check(existing?.editionId ?: editionId)?.let { return AppResult.Failure(it) }
        val now = time.now()
        val whisper = WhisperedIdea(
            id = existing?.id ?: ids.newId(),
            editionId = existing?.editionId ?: editionId,
            recipientId = draft.recipientId,
            title = draft.title.trim(),
            priceCents = draft.priceCents,
            url = draft.url.trim(),
            givenToPersonId = draft.givenToPersonId,
            // Le nom est gardé même pour une personne de l'application (si elle est supprimée un jour).
            givenToName = giverName,
            received = draft.received,
            notes = draft.notes.trim(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        whispers.saveWhisper(whisper)
        return AppResult.Success(whisper)
    }
}

class DeleteWhisperUseCase @Inject constructor(private val whispers: WhisperRepository, private val guard: EditionGuard) {
    suspend operator fun invoke(id: String): AppResult<Unit> {
        val whisper = whispers.getWhisper(id) ?: return AppResult.Failure(AppError.NotFound)
        guard.check(whisper.editionId)?.let { return AppResult.Failure(it) }
        whispers.deleteWhisper(id)
        return AppResult.Success(Unit)
    }
}

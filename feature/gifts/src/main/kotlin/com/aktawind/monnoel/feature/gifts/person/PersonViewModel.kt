package com.aktawind.monnoel.feature.gifts.person

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.ApplicationScope
import com.aktawind.monnoel.core.domain.edition.ObserveEditionsUseCase
import com.aktawind.monnoel.core.domain.gifts.ChangeGiftStatusUseCase
import com.aktawind.monnoel.core.domain.gifts.CreateIdeaRequestUseCase
import com.aktawind.monnoel.core.domain.gifts.DeleteIdeaRequestUseCase
import com.aktawind.monnoel.core.domain.gifts.DeleteWhisperUseCase
import com.aktawind.monnoel.core.domain.gifts.IdeaRequestRepository
import com.aktawind.monnoel.core.domain.gifts.OfferIdeaUseCase
import com.aktawind.monnoel.core.domain.gifts.PersonGiftBudget
import com.aktawind.monnoel.core.domain.gifts.SaveWhisperUseCase
import com.aktawind.monnoel.core.domain.gifts.SetIdeaRequestDoneUseCase
import com.aktawind.monnoel.core.domain.gifts.SetPersonBudgetUseCase
import com.aktawind.monnoel.core.domain.gifts.WhisperDraft
import com.aktawind.monnoel.core.domain.gifts.WhisperRepository
import com.aktawind.monnoel.core.domain.gifts.computeGiftBudget
import com.aktawind.monnoel.core.domain.gifts.parseEuroToCents
import com.aktawind.monnoel.core.domain.repository.GiftIdeaRepository
import com.aktawind.monnoel.core.domain.repository.PersonRepository
import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import com.aktawind.monnoel.core.domain.repository.PlannedGiftRepository
import com.aktawind.monnoel.core.domain.tasks.TaskRepository
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.model.GiftIdea
import com.aktawind.monnoel.core.model.GiftStatus
import com.aktawind.monnoel.core.model.IdeaRequest
import com.aktawind.monnoel.core.model.IdeaRequestKind
import com.aktawind.monnoel.core.model.Person
import com.aktawind.monnoel.core.model.PlannedGift
import com.aktawind.monnoel.core.model.TaskStatus
import com.aktawind.monnoel.core.model.WhisperedIdea
import com.aktawind.monnoel.core.ui.UiMessage
import com.aktawind.monnoel.core.ui.toUiMessage
import com.aktawind.monnoel.feature.gifts.R
import com.aktawind.monnoel.feature.gifts.edit.PhotoDrafts
import com.aktawind.monnoel.feature.gifts.navigation.PersonDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PersonUiState(
    val loading: Boolean = true,
    val person: Person? = null,
    val edition: ChristmasEdition? = null,
    val currentGifts: List<PlannedGift> = emptyList(),
    /** Cadeaux des autres années, regroupés par année (la plus récente d'abord). */
    val history: List<Pair<Int, List<PlannedGift>>> = emptyList(),
    val ideas: List<GiftIdea> = emptyList(),
    val budget: PersonGiftBudget? = null,
    /** Idées soufflées pour cette personne, pour le Noël affiché. */
    val whispersFor: List<WhisperedIdea> = emptyList(),
    /** Toutes les idées soufflées du Noël (repérage des doublons). */
    val editionWhispers: List<WhisperedIdea> = emptyList(),
    val persons: List<Person> = emptyList(),
    /** Demandes d'idées rangées dans cette fiche, avec leur avancement (celui de leur tâche). */
    val requests: List<Pair<IdeaRequest, Boolean>> = emptyList(),
    val message: UiMessage? = null,
) {
    val readOnly: Boolean get() = edition?.isReadOnly == true
}

@HiltViewModel
class PersonViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val persons: PersonRepository,
    private val gifts: PlannedGiftRepository,
    ideas: GiftIdeaRepository,
    observeEditions: ObserveEditionsUseCase,
    private val setBudget: SetPersonBudgetUseCase,
    private val changeStatus: ChangeGiftStatusUseCase,
    private val offerIdea: OfferIdeaUseCase,
    whispers: WhisperRepository,
    private val saveWhisper: SaveWhisperUseCase,
    private val deleteWhisper: DeleteWhisperUseCase,
    ideaRequests: IdeaRequestRepository,
    tasks: TaskRepository,
    private val createRequest: CreateIdeaRequestUseCase,
    private val setRequestDone: SetIdeaRequestDoneUseCase,
    private val deleteRequest: DeleteIdeaRequestUseCase,
    private val photos: PhotoRepository,
    @ApplicationScope appScope: CoroutineScope,
) : ViewModel() {
    private val drafts = PhotoDrafts(photos, appScope)

    private val personId = savedStateHandle.toRoute<PersonDestination>().personId
    private val message = MutableStateFlow<UiMessage?>(null)

    private val editionsState = observeEditions()

    private val budgetFlow = editionsState.flatMapLatest { state ->
        val edition = state.current ?: return@flatMapLatest flowOf(null)
        combine(gifts.observeGifts(edition.id), gifts.observeBudgets(edition.id)) { g, b ->
            computeGiftBudget(g, b).byPerson[personId]
        }
    }

    private val baseState = combine(
        persons.observePerson(personId),
        gifts.observeGiftsForPerson(personId),
        ideas.observeIdeas(),
        editionsState,
    ) { person, allGifts, allIdeas, editions ->
        val current = editions.current
        val years = editions.editions.associate { it.id to it.year }
        PersonUiState(
            loading = false,
            person = person,
            edition = current,
            currentGifts = allGifts.filter { it.editionId == current?.id },
            history = allGifts.filter { it.editionId != current?.id }
                .groupBy { years[it.editionId] ?: 0 }
                .toList()
                .sortedByDescending { it.first },
            ideas = allIdeas.filter { personId in it.personIds },
        )
    }

    private val whisperFlow = editionsState.flatMapLatest { state ->
        val edition = state.current ?: return@flatMapLatest flowOf(emptyList<WhisperedIdea>() to emptyList<Pair<IdeaRequest, Boolean>>())
        combine(whispers.observeWhispers(edition.id), ideaRequests.observeRequests(edition.id), tasks.observeTasks(edition.id)) { w, r, t ->
            val done = t.associate { it.id to (it.status == TaskStatus.DONE) }
            w to r.filter { it.personId == personId }.map { it to (it.taskId?.let(done::get) ?: false) }
        }
    }

    val uiState: StateFlow<PersonUiState> = combine(baseState, budgetFlow, whisperFlow, persons.observePersons(), message) {
            base,
            budget,
            (edition, requestRows),
            people,
            msg,
        ->
        base.copy(
            budget = budget,
            message = msg,
            whispersFor = edition.filter { it.recipientId == personId },
            editionWhispers = edition,
            persons = people,
            requests = requestRows,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonUiState())

    fun saveEnvelope(input: String) = viewModelScope.launch {
        val edition = uiState.value.edition ?: return@launch
        val cents = if (input.isBlank()) null else parseEuroToCents(input) ?: return@launch
        message.value = when (val result = setBudget(edition.id, personId, cents)) {
            is AppResult.Success -> UiMessage(R.string.person_envelope_saved)
            is AppResult.Failure -> result.error.toUiMessage()
        }
    }

    fun setStatus(giftId: String, status: GiftStatus) = viewModelScope.launch {
        (changeStatus(giftId, status) as? AppResult.Failure)?.let { message.value = it.error.toUiMessage() }
    }

    /** L'idée devient un cadeau de l'année pour cette personne (l'idée reste dans la liste). */
    fun offer(idea: GiftIdea) = viewModelScope.launch {
        val edition = uiState.value.edition ?: return@launch
        message.value = when (val result = offerIdea(idea.id, personId, edition.id)) {
            is AppResult.Success -> UiMessage(R.string.gifts_offered, listOf(idea.title))
            is AppResult.Failure -> result.error.toUiMessage()
        }
    }

    fun saveWhisper(id: String?, draft: WhisperDraft) = viewModelScope.launch {
        val edition = uiState.value.edition ?: return@launch
        when (val result = saveWhisper.invoke(id, edition.id, draft)) {
            is AppResult.Success -> drafts.committed(draft.photoPath.takeUnless { draft.photoFromIdea })
            is AppResult.Failure -> message.value = result.error.toUiMessage()
        }
    }

    /** Importe une photo pour une idée soufflée ; [onImported] reçoit son chemin. */
    fun importWhisperPhoto(uri: String, onImported: (String) -> Unit) = viewModelScope.launch {
        when (val result = photos.importPhoto(uri)) {
            is AppResult.Success -> {
                drafts.added(result.value)
                onImported(result.value)
            }
            is AppResult.Failure -> message.value = UiMessage(com.aktawind.monnoel.core.ui.R.string.photo_error)
        }
    }

    /** Fenêtre fermée sans enregistrer : les photos importées sont jetées. */
    fun discardWhisperPhotos() = drafts.discard()

    override fun onCleared() {
        drafts.discard()
    }

    fun deleteWhisper(id: String) = viewModelScope.launch {
        (deleteWhisper.invoke(id) as? AppResult.Failure)?.let { message.value = it.error.toUiMessage() }
    }

    fun addRequest(kind: IdeaRequestKind, otherPersonId: String?, otherName: String) = viewModelScope.launch {
        val edition = uiState.value.edition ?: return@launch
        (createRequest(edition.id, kind, personId, otherPersonId, otherName) as? AppResult.Failure)?.let {
            message.value = it.error.toUiMessage()
        }
    }

    fun setRequestDone(id: String, done: Boolean) = viewModelScope.launch {
        (setRequestDone.invoke(id, done) as? AppResult.Failure)?.let { message.value = it.error.toUiMessage() }
    }

    fun removeRequest(id: String) = viewModelScope.launch {
        (deleteRequest(id) as? AppResult.Failure)?.let { message.value = it.error.toUiMessage() }
    }

    fun messageShown() {
        message.value = null
    }
}

package com.aktawind.monnoel.feature.traditions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.ApplicationScope
import com.aktawind.monnoel.core.domain.edition.ObserveEditionsUseCase
import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import com.aktawind.monnoel.core.domain.traditions.AddTraditionIdeasUseCase
import com.aktawind.monnoel.core.domain.traditions.DeleteInspirationUseCase
import com.aktawind.monnoel.core.domain.traditions.InspirationDraft
import com.aktawind.monnoel.core.domain.traditions.SaveInspirationUseCase
import com.aktawind.monnoel.core.domain.traditions.SaveTraditionMemoryUseCase
import com.aktawind.monnoel.core.domain.traditions.SetInspirationDoneUseCase
import com.aktawind.monnoel.core.domain.traditions.SetTraditionFavoriteUseCase
import com.aktawind.monnoel.core.domain.traditions.SetTraditionPlannedUseCase
import com.aktawind.monnoel.core.domain.traditions.TraditionIdea
import com.aktawind.monnoel.core.domain.traditions.TraditionRepository
import com.aktawind.monnoel.core.domain.traditions.availableTraditionIdeas
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.model.Inspiration
import com.aktawind.monnoel.core.model.Tradition
import com.aktawind.monnoel.core.model.TraditionYear
import com.aktawind.monnoel.core.ui.UiMessage
import com.aktawind.monnoel.core.ui.toUiMessage
import com.aktawind.monnoel.feature.traditions.navigation.TraditionsDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TraditionsUiState(
    val loading: Boolean = true,
    val edition: ChristmasEdition? = null,
    val traditions: List<Tradition> = emptyList(),
    /** Programme du Noël affiché, par tradition. */
    val program: Map<String, TraditionYear> = emptyMap(),
    /** Années où chaque tradition a été faite (« faite en 2025, 2026 »). */
    val doneYears: Map<String, List<Int>> = emptyMap(),
    val ideas: List<TraditionIdea> = emptyList(),
    val inspirations: List<Inspiration> = emptyList(),
    /** Inspirations vues / faites ce Noël. */
    val inspirationsDone: Set<String> = emptySet(),
    val message: UiMessage? = null,
) {
    val readOnly: Boolean get() = edition?.isReadOnly == true
}

private data class YearData(val edition: ChristmasEdition?, val program: List<TraditionYear>, val done: Set<String>)

@HiltViewModel
class TraditionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeEditions: ObserveEditionsUseCase,
    private val traditions: TraditionRepository,
    private val photos: PhotoRepository,
    private val setFavorite: SetTraditionFavoriteUseCase,
    private val setPlanned: SetTraditionPlannedUseCase,
    private val saveMemory: SaveTraditionMemoryUseCase,
    private val addIdeas: AddTraditionIdeasUseCase,
    private val saveInspiration: SaveInspirationUseCase,
    private val deleteInspiration: DeleteInspirationUseCase,
    private val setInspirationDone: SetInspirationDoneUseCase,
    @ApplicationScope appScope: CoroutineScope,
) : ViewModel() {
    /** Inspiration à ouvrir à l'arrivée (recherche). */
    val initialInspirationId: String? = savedStateHandle.toRoute<TraditionsDestination>().inspirationId
    private val message = MutableStateFlow<UiMessage?>(null)
    private val drafts = PhotoDrafts(photos, appScope)
    private val editions = observeEditions()

    private val yearData = editions.map { it.current }.flatMapLatest { edition ->
        if (edition == null) {
            flowOf(YearData(null, emptyList(), emptySet()))
        } else {
            combine(traditions.observeYears(edition.id), traditions.observeDone(edition.id)) { p, d -> YearData(edition, p, d) }
        }
    }

    private val history = combine(traditions.observeAllYears(), editions) { years, state ->
        val yearOf = state.editions.associate { it.id to it.year }
        years.filter { it.done }.groupBy { it.traditionId }.mapValues { (_, l) -> l.mapNotNull { yearOf[it.editionId] }.sorted() }
    }

    val uiState: StateFlow<TraditionsUiState> = combine(
        traditions.observeTraditions(),
        yearData,
        history,
        traditions.observeInspirations(),
        message,
    ) { list, year, done, inspirations, msg ->
        TraditionsUiState(
            loading = false,
            edition = year.edition,
            traditions = list,
            program = year.program.associateBy { it.traditionId },
            doneYears = done,
            ideas = availableTraditionIdeas(list),
            inspirations = inspirations,
            inspirationsDone = year.done,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TraditionsUiState())

    private fun report(result: AppResult<*>) {
        (result as? AppResult.Failure)?.let { message.value = it.error.toUiMessage() }
    }

    fun toggleFavorite(tradition: Tradition) = viewModelScope.launch { report(setFavorite(tradition.id, !tradition.favorite)) }

    fun setPlanned(traditionId: String, planned: Boolean) = viewModelScope.launch {
        val edition = uiState.value.edition ?: return@launch
        report(setPlanned.invoke(edition.id, traditionId, planned))
    }

    fun saveMemory(traditionId: String, done: Boolean, photoPath: String?, note: String) = viewModelScope.launch {
        val edition = uiState.value.edition ?: return@launch
        val result = saveMemory.invoke(edition.id, traditionId, done, photoPath, note)
        if (result is AppResult.Success) drafts.committed(photoPath)
        report(result)
    }

    fun addIdeas(ideas: List<TraditionIdea>) = viewModelScope.launch {
        val count = addIdeas.invoke(ideas)
        if (count > 0) message.value = UiMessage(R.string.ideas_added, listOf(count))
    }

    fun saveInspiration(id: String?, draft: InspirationDraft) = viewModelScope.launch {
        val result = saveInspiration.invoke(id, draft)
        if (result is AppResult.Success) drafts.committed(draft.photoPath)
        report(result)
    }

    fun deleteInspiration(id: String) = viewModelScope.launch { report(deleteInspiration.invoke(id)) }

    fun setInspirationDone(id: String, done: Boolean) = viewModelScope.launch {
        val edition = uiState.value.edition ?: return@launch
        report(setInspirationDone.invoke(edition.id, id, done))
    }

    fun importPhoto(uri: String, onImported: (String) -> Unit) = viewModelScope.launch {
        when (val result = photos.importPhoto(uri)) {
            is AppResult.Success -> {
                drafts.added(result.value)
                onImported(result.value)
            }
            is AppResult.Failure -> message.value = UiMessage(com.aktawind.monnoel.core.ui.R.string.photo_error)
        }
    }

    fun discardPhotos() = drafts.discard()

    fun messageShown() {
        message.value = null
    }

    override fun onCleared() {
        drafts.discard()
    }
}

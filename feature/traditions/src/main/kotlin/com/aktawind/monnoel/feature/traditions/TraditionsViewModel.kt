package com.aktawind.monnoel.feature.traditions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.ApplicationScope
import com.aktawind.monnoel.core.domain.edition.ObserveEditionsUseCase
import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import com.aktawind.monnoel.core.domain.traditions.AddCraftIdeasUseCase
import com.aktawind.monnoel.core.domain.traditions.AddTraditionIdeasUseCase
import com.aktawind.monnoel.core.domain.traditions.CraftIdea
import com.aktawind.monnoel.core.domain.traditions.CraftRepository
import com.aktawind.monnoel.core.domain.traditions.DecorDraft
import com.aktawind.monnoel.core.domain.traditions.DeleteDecorUseCase
import com.aktawind.monnoel.core.domain.traditions.DeleteInspirationUseCase
import com.aktawind.monnoel.core.domain.traditions.InspirationDraft
import com.aktawind.monnoel.core.domain.traditions.SaveDecorUseCase
import com.aktawind.monnoel.core.domain.traditions.SaveInspirationUseCase
import com.aktawind.monnoel.core.domain.traditions.SaveTraditionMemoryUseCase
import com.aktawind.monnoel.core.domain.traditions.SetCraftStatusUseCase
import com.aktawind.monnoel.core.domain.traditions.SetInspirationDoneUseCase
import com.aktawind.monnoel.core.domain.traditions.SetTraditionFavoriteUseCase
import com.aktawind.monnoel.core.domain.traditions.SetTraditionPlannedUseCase
import com.aktawind.monnoel.core.domain.traditions.TraditionIdea
import com.aktawind.monnoel.core.domain.traditions.TraditionRepository
import com.aktawind.monnoel.core.domain.traditions.availableCraftIdeas
import com.aktawind.monnoel.core.domain.traditions.availableTraditionIdeas
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.model.Craft
import com.aktawind.monnoel.core.model.CraftStatus
import com.aktawind.monnoel.core.model.CraftSupply
import com.aktawind.monnoel.core.model.CraftYear
import com.aktawind.monnoel.core.model.DecorIdea
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
    val crafts: List<Craft> = emptyList(),
    val craftSupplies: Map<String, List<CraftSupply>> = emptyMap(),
    /** Avancement de chaque bricolage pour le Noël affiché (absent = simple idée). */
    val craftProgram: Map<String, CraftYear> = emptyMap(),
    /** Fournitures achetées pour le Noël affiché. */
    val craftBought: Set<String> = emptySet(),
    val craftDoneYears: Map<String, List<Int>> = emptyMap(),
    val craftIdeas: List<CraftIdea> = emptyList(),
    val decor: List<DecorIdea> = emptyList(),
    val message: UiMessage? = null,
) {
    val readOnly: Boolean get() = edition?.isReadOnly == true
}

private data class Workshop(
    val crafts: List<Craft>,
    val supplies: List<CraftSupply>,
    val years: List<CraftYear>,
    val bought: Set<Pair<String, String>>,
    val decor: List<DecorIdea>,
)

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
    crafts: CraftRepository,
    private val setCraftStatus: SetCraftStatusUseCase,
    private val addCraftIdeas: AddCraftIdeasUseCase,
    private val saveDecor: SaveDecorUseCase,
    private val deleteDecor: DeleteDecorUseCase,
    @ApplicationScope appScope: CoroutineScope,
) : ViewModel() {
    /** Inspiration à ouvrir à l'arrivée (recherche). */
    private val route = savedStateHandle.toRoute<TraditionsDestination>()
    val initialInspirationId: String? = route.inspirationId
    val initialDecorId: String? = route.decorId
    val initialTab: Int = when {
        route.inspirationId != null -> TAB_INSPIRATIONS
        route.decorId != null -> TAB_DECOR
        else -> route.tab ?: TAB_PROGRAM
    }
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

    private val workshop = combine(
        crafts.observeCrafts(),
        crafts.observeSupplies(),
        crafts.observeYears(),
        crafts.observeBought(),
        crafts.observeDecor(),
    ) { c, s, y, b, d -> Workshop(c, s, y, b, d) }

    val uiState: StateFlow<TraditionsUiState> = combine(
        combine(traditions.observeTraditions(), yearData, history) { list, year, done -> Triple(list, year, done) },
        traditions.observeInspirations(),
        workshop,
        editions,
        message,
    ) { (list, year, done), inspirations, shop, state, msg ->
        val editionId = year.edition?.id
        val yearOf = state.editions.associate { it.id to it.year }
        TraditionsUiState(
            loading = false,
            edition = year.edition,
            traditions = list,
            program = year.program.associateBy { it.traditionId },
            doneYears = done,
            ideas = availableTraditionIdeas(list),
            inspirations = inspirations,
            inspirationsDone = year.done,
            crafts = shop.crafts,
            craftSupplies = shop.supplies.groupBy { it.craftId }.mapValues { (_, l) -> l.sortedBy { it.sortOrder } },
            craftProgram = shop.years.filter { it.editionId == editionId }.associateBy { it.craftId },
            craftBought = shop.bought.filter { it.first == editionId }.map { it.second }.toSet(),
            craftDoneYears = shop.years.filter { it.status == CraftStatus.DONE }.groupBy { it.craftId }
                .mapValues { (_, l) -> l.mapNotNull { yearOf[it.editionId] }.sorted() },
            craftIdeas = availableCraftIdeas(shop.crafts),
            decor = shop.decor,
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

    fun setCraftStatus(craftId: String, status: CraftStatus?) = viewModelScope.launch {
        val edition = uiState.value.edition ?: return@launch
        report(setCraftStatus.invoke(edition.id, craftId, status))
    }

    fun addCraftIdeas(ideas: List<CraftIdea>) = viewModelScope.launch {
        val count = addCraftIdeas.invoke(ideas)
        if (count > 0) message.value = UiMessage(R.string.craft_ideas_added, listOf(count))
    }

    fun saveDecor(id: String?, draft: DecorDraft) = viewModelScope.launch {
        val result = saveDecor.invoke(id, draft)
        if (result is AppResult.Success) drafts.committed(draft.photoPath)
        report(result)
    }

    fun toggleDecorFavorite(decor: DecorIdea) = viewModelScope.launch {
        report(saveDecor.invoke(decor.id, DecorDraft(decor.title, decor.place, decor.photoPath, decor.url, decor.comment, !decor.favorite)))
    }

    fun deleteDecor(id: String) = viewModelScope.launch { report(deleteDecor.invoke(id)) }

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

internal const val TAB_PROGRAM = 0
internal const val TAB_TRADITIONS = 1
internal const val TAB_INSPIRATIONS = 2
internal const val TAB_CRAFTS = 3
internal const val TAB_DECOR = 4

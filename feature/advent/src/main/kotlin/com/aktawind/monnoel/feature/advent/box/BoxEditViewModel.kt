package com.aktawind.monnoel.feature.advent.box

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.ApplicationScope
import com.aktawind.monnoel.core.domain.advent.AdventRepository
import com.aktawind.monnoel.core.domain.advent.BoxDraft
import com.aktawind.monnoel.core.domain.advent.SaveBoxUseCase
import com.aktawind.monnoel.core.domain.gifts.centsToInput
import com.aktawind.monnoel.core.domain.gifts.parseEuroToCents
import com.aktawind.monnoel.core.domain.repository.EditionRepository
import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import com.aktawind.monnoel.core.model.AdventSupply
import com.aktawind.monnoel.core.model.BoxStatus
import com.aktawind.monnoel.core.model.SurpriseType
import com.aktawind.monnoel.core.ui.UiMessage
import com.aktawind.monnoel.core.ui.toUiMessage
import com.aktawind.monnoel.feature.advent.navigation.BoxEditDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BoxForm(
    val type: SurpriseType? = null,
    val title: String = "",
    val photoPath: String? = null,
    val price: String = "",
    val supplier: String = "",
    val url: String = "",
    val status: BoxStatus = BoxStatus.TO_FIND,
    val notes: String = "",
    val supplyId: String? = null,
)

/** Contenu de case réutilisable (case d'un autre calendrier ou d'un modèle). */
data class BoxSource(
    val key: String,
    val origin: String,
    val number: Int,
    val type: SurpriseType?,
    val title: String,
    val priceCents: Long?,
    val supplier: String,
    val url: String,
    val notes: String,
    val photoPath: String?,
)

data class BoxEditUiState(
    val loading: Boolean = true,
    val number: Int = 0,
    val calendarName: String = "",
    val readOnly: Boolean = false,
    val form: BoxForm = BoxForm(),
    val supplies: List<AdventSupply> = emptyList(),
    val sources: List<BoxSource> = emptyList(),
    val busy: Boolean = false,
    val message: UiMessage? = null,
    val done: Boolean = false,
)

@HiltViewModel
class BoxEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val advent: AdventRepository,
    private val editions: EditionRepository,
    private val photos: PhotoRepository,
    private val saveBox: SaveBoxUseCase,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {
    private val boxId = savedStateHandle.toRoute<BoxEditDestination>().boxId
    private val state = MutableStateFlow(BoxEditUiState())
    private val addedPhotos = mutableSetOf<String>()

    private val sources = combine(
        advent.observeAllCalendars(),
        advent.observeAllBoxes(),
        advent.observeTemplates(),
        advent.observeAllTemplateBoxes(),
        editions.observeEditions(),
    ) { calendars, boxes, templates, templateBoxes, editionList ->
        val years = editionList.associate { it.id to it.year }
        val calendarLabels = calendars.associate { it.id to "${it.name} · ${years[it.editionId] ?: ""}" }
        val templateNames = templates.associate { it.id to it.name }
        boxes.filter { it.id != boxId && !it.isEmpty }.map {
            BoxSource(
                "b-${it.id}", calendarLabels[it.calendarId].orEmpty(), it.number, it.type, it.title, it.priceCents, it.supplier, it.url,
                it.notes, it.photoPath,
            )
        } + templateBoxes.map {
            BoxSource(
                "t-${it.id}", templateNames[it.templateId].orEmpty(), it.number, it.type, it.title, it.priceCents, it.supplier, it.url,
                it.notes, null,
            )
        }
    }

    val uiState: StateFlow<BoxEditUiState> = combine(state, sources) { s, src -> s.copy(sources = src) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), state.value)

    init {
        viewModelScope.launch {
            val box = advent.getBox(boxId) ?: return@launch state.update { it.copy(done = true) }
            val calendar = advent.getCalendar(box.calendarId)
            val edition = calendar?.let { c -> editions.observeEditions().first().firstOrNull { it.id == c.editionId } }
            state.update {
                it.copy(
                    loading = false,
                    number = box.number,
                    calendarName = calendar?.name.orEmpty(),
                    readOnly = edition?.isReadOnly == true,
                    supplies = advent.getSupplies(box.calendarId),
                    form = BoxForm(
                        box.type, box.title, box.photoPath, centsToInput(box.priceCents), box.supplier, box.url, box.status,
                        box.notes, box.supplyId,
                    ),
                )
            }
        }
    }

    fun update(transform: BoxForm.() -> BoxForm) = state.update { it.copy(form = it.form.transform()) }

    /** Reprend le contenu d'une autre case (photo copiée), sans changer l'état ni le numéro. */
    fun useSource(source: BoxSource) {
        viewModelScope.launch {
            val photo = source.photoPath?.let { photos.copyPhoto(it) }?.also { addedPhotos += it }
            update {
                copy(
                    type = source.type,
                    title = source.title,
                    photoPath = photo ?: photoPath,
                    price = centsToInput(source.priceCents),
                    supplier = source.supplier,
                    url = source.url,
                    notes = source.notes,
                )
            }
        }
    }

    fun importPhoto(uri: String) {
        viewModelScope.launch {
            when (val result = photos.importPhoto(uri)) {
                is AppResult.Success -> {
                    addedPhotos += result.value
                    update { copy(photoPath = result.value) }
                }
                is AppResult.Failure -> state.update { it.copy(message = UiMessage(com.aktawind.monnoel.core.ui.R.string.photo_error)) }
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            val f = state.value.form
            if (f.price.isNotBlank() && parseEuroToCents(f.price) == null) return@launch
            state.update { it.copy(busy = true) }
            val draft = BoxDraft(f.type, f.title, f.photoPath, parseEuroToCents(f.price), f.supplier, f.url, f.status, f.notes, f.supplyId)
            val result = saveBox(boxId, draft)
            state.update {
                when (result) {
                    is AppResult.Success -> {
                        addedPhotos -= setOfNotNull(f.photoPath)
                        it.copy(busy = false, done = true)
                    }
                    is AppResult.Failure -> it.copy(busy = false, message = result.error.toUiMessage())
                }
            }
        }
    }

    fun messageShown() = state.update { it.copy(message = null) }

    override fun onCleared() {
        val leftovers = addedPhotos.toList()
        if (leftovers.isNotEmpty()) appScope.launch { leftovers.forEach { photos.deletePhoto(it) } }
    }
}

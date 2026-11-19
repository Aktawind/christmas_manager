package com.aktawind.monnoel.feature.traditions.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.ApplicationScope
import com.aktawind.monnoel.core.domain.edition.ObserveEditionsUseCase
import com.aktawind.monnoel.core.domain.repository.PersonRepository
import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import com.aktawind.monnoel.core.domain.traditions.DeleteTraditionUseCase
import com.aktawind.monnoel.core.domain.traditions.SaveTraditionUseCase
import com.aktawind.monnoel.core.domain.traditions.TraditionDraft
import com.aktawind.monnoel.core.domain.traditions.TraditionRepository
import com.aktawind.monnoel.core.model.Person
import com.aktawind.monnoel.core.ui.UiMessage
import com.aktawind.monnoel.core.ui.toUiMessage
import com.aktawind.monnoel.feature.traditions.PhotoDrafts
import com.aktawind.monnoel.feature.traditions.navigation.TraditionEditDestination
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

data class TraditionEditUiState(
    val isNew: Boolean = true,
    val loading: Boolean = true,
    val form: TraditionDraft = TraditionDraft(""),
    val persons: List<Person> = emptyList(),
    /** Années où elle a été faite, avec le petit mot du souvenir. */
    val history: List<Pair<Int, String>> = emptyList(),
    val busy: Boolean = false,
    val message: UiMessage? = null,
    val done: Boolean = false,
)

@HiltViewModel
class TraditionEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val traditions: TraditionRepository,
    private val observeEditions: ObserveEditionsUseCase,
    persons: PersonRepository,
    private val photos: PhotoRepository,
    private val saveTradition: SaveTraditionUseCase,
    private val deleteTradition: DeleteTraditionUseCase,
    @ApplicationScope appScope: CoroutineScope,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<TraditionEditDestination>()
    private val state = MutableStateFlow(TraditionEditUiState(isNew = route.traditionId == null))
    private val drafts = PhotoDrafts(photos, appScope)

    val uiState: StateFlow<TraditionEditUiState> = combine(state, persons.observePersons()) { s, p -> s.copy(persons = p) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), state.value)

    init {
        viewModelScope.launch {
            val tradition = route.traditionId?.let { traditions.getTradition(it) }
            val years = observeEditions().first().editions.associate { it.id to it.year }
            val history = route.traditionId?.let { id -> traditions.getYearsOf(id) }.orEmpty()
                .filter { it.done }
                .mapNotNull { y -> years[y.editionId]?.let { it to y.memoryNote } }
                .sortedByDescending { it.first }
            state.update {
                it.copy(
                    loading = false,
                    history = history,
                    form = tradition?.let { t ->
                        TraditionDraft(t.name, t.category, t.period, t.photoPath, t.personIds, t.notes, t.favorite)
                    } ?: TraditionDraft(""),
                )
            }
        }
    }

    fun update(transform: TraditionDraft.() -> TraditionDraft) = state.update { it.copy(form = it.form.transform()) }

    fun togglePerson(id: String) = update { copy(personIds = if (id in personIds) personIds - id else personIds + id) }

    fun importPhoto(uri: String) = viewModelScope.launch {
        when (val result = photos.importPhoto(uri)) {
            is AppResult.Success -> {
                drafts.added(result.value)
                update { copy(photoPath = result.value) }
            }
            is AppResult.Failure -> state.update { it.copy(message = UiMessage(com.aktawind.monnoel.core.ui.R.string.photo_error)) }
        }
    }

    fun save() = viewModelScope.launch {
        val form = state.value.form
        state.update { it.copy(busy = true) }
        val result = saveTradition(route.traditionId, form)
        state.update {
            when (result) {
                is AppResult.Success -> {
                    drafts.committed(form.photoPath)
                    it.copy(busy = false, done = true)
                }
                is AppResult.Failure -> it.copy(busy = false, message = result.error.toUiMessage())
            }
        }
    }

    fun delete() = viewModelScope.launch {
        val id = route.traditionId ?: return@launch
        when (val result = deleteTradition(id)) {
            is AppResult.Success -> state.update { it.copy(done = true) }
            is AppResult.Failure -> state.update { it.copy(message = result.error.toUiMessage()) }
        }
    }

    fun messageShown() = state.update { it.copy(message = null) }

    override fun onCleared() {
        drafts.discard()
    }
}

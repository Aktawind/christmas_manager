package com.aktawind.monnoel.feature.advent.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.ApplicationScope
import com.aktawind.monnoel.core.domain.advent.AdventRepository
import com.aktawind.monnoel.core.domain.advent.CalendarDraft
import com.aktawind.monnoel.core.domain.advent.DEFAULT_BOX_COUNT
import com.aktawind.monnoel.core.domain.advent.SaveCalendarUseCase
import com.aktawind.monnoel.core.domain.edition.ObserveEditionsUseCase
import com.aktawind.monnoel.core.domain.gifts.centsToInput
import com.aktawind.monnoel.core.domain.gifts.parseEuroToCents
import com.aktawind.monnoel.core.domain.repository.PersonRepository
import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import com.aktawind.monnoel.core.model.AdventBox
import com.aktawind.monnoel.core.model.Person
import com.aktawind.monnoel.core.ui.UiMessage
import com.aktawind.monnoel.core.ui.toUiMessage
import com.aktawind.monnoel.feature.advent.navigation.CalendarEditDestination
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

data class CalendarForm(
    val name: String = "",
    val personIds: Set<String> = emptySet(),
    val description: String = "",
    val coverPhotoPath: String? = null,
    val theme: String = "",
    val budget: String = "",
    val notes: String = "",
    val boxCount: Int = DEFAULT_BOX_COUNT,
)

data class CalendarEditUiState(
    val isNew: Boolean = true,
    val loading: Boolean = true,
    val form: CalendarForm = CalendarForm(),
    val persons: List<Person> = emptyList(),
    val boxes: List<AdventBox> = emptyList(),
    val busy: Boolean = false,
    val message: UiMessage? = null,
    val doneWithId: String? = null,
) {
    /** Cases non vides qui disparaîtraient si l'on réduit le nombre de cases. */
    val boxesLost: Int get() = boxes.count { it.number > form.boxCount && !it.isEmpty }
}

@HiltViewModel
class CalendarEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val advent: AdventRepository,
    private val observeEditions: ObserveEditionsUseCase,
    persons: PersonRepository,
    private val photos: PhotoRepository,
    private val saveCalendar: SaveCalendarUseCase,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<CalendarEditDestination>()
    private val state = MutableStateFlow(CalendarEditUiState(isNew = route.calendarId == null))
    private val addedPhotos = mutableSetOf<String>()

    val uiState: StateFlow<CalendarEditUiState> = combine(state, persons.observePersons()) { s, p -> s.copy(persons = p) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), state.value)

    init {
        viewModelScope.launch {
            val calendar = route.calendarId?.let { advent.getCalendar(it) }
            val boxes = route.calendarId?.let { advent.getBoxes(it) }.orEmpty()
            state.update {
                it.copy(
                    loading = false,
                    boxes = boxes,
                    form = calendar?.let { c ->
                        CalendarForm(
                            c.name,
                            c.personIds,
                            c.description,
                            c.coverPhotoPath,
                            c.theme,
                            centsToInput(c.budgetCents),
                            c.notes,
                            c.boxCount,
                        )
                    } ?: CalendarForm(),
                )
            }
        }
    }

    fun update(transform: CalendarForm.() -> CalendarForm) = state.update { it.copy(form = it.form.transform()) }

    fun togglePerson(id: String) = update { copy(personIds = if (id in personIds) personIds - id else personIds + id) }

    fun importPhoto(uri: String) {
        viewModelScope.launch {
            when (val result = photos.importPhoto(uri)) {
                is AppResult.Success -> {
                    addedPhotos += result.value
                    update { copy(coverPhotoPath = result.value) }
                }
                is AppResult.Failure -> state.update { it.copy(message = UiMessage(com.aktawind.monnoel.core.ui.R.string.photo_error)) }
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            val f = state.value.form
            if (f.budget.isNotBlank() && parseEuroToCents(f.budget) == null) return@launch
            val editionId = route.calendarId?.let { advent.getCalendar(it)?.editionId }
                ?: observeEditions().first().current?.id ?: return@launch
            state.update { it.copy(busy = true) }
            val draft =
                CalendarDraft(
                    f.name,
                    f.personIds,
                    f.description,
                    f.coverPhotoPath,
                    f.theme,
                    parseEuroToCents(f.budget),
                    f.notes,
                    f.boxCount,
                )
            val result = saveCalendar(route.calendarId, editionId, draft)
            state.update {
                when (result) {
                    is AppResult.Success -> {
                        addedPhotos -= setOfNotNull(f.coverPhotoPath)
                        it.copy(busy = false, doneWithId = result.value.id)
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

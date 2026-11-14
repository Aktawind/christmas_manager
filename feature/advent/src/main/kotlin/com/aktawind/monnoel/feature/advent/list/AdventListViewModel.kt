package com.aktawind.monnoel.feature.advent.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.advent.AdventRepository
import com.aktawind.monnoel.core.domain.advent.CalendarSummary
import com.aktawind.monnoel.core.domain.advent.CreateFromTemplateUseCase
import com.aktawind.monnoel.core.domain.advent.DuplicateCalendarUseCase
import com.aktawind.monnoel.core.domain.advent.summarizeCalendar
import com.aktawind.monnoel.core.domain.edition.ObserveEditionsUseCase
import com.aktawind.monnoel.core.domain.repository.PersonRepository
import com.aktawind.monnoel.core.model.AdventBox
import com.aktawind.monnoel.core.model.AdventCalendar
import com.aktawind.monnoel.core.model.AdventSupply
import com.aktawind.monnoel.core.model.AdventTemplate
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.model.Person
import com.aktawind.monnoel.core.ui.UiMessage
import com.aktawind.monnoel.core.ui.toUiMessage
import com.aktawind.monnoel.feature.advent.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private data class EditionAdvent(
    val edition: ChristmasEdition?,
    val calendars: List<AdventCalendar>,
    val boxes: List<AdventBox>,
    val supplies: List<AdventSupply>,
)

data class CalendarCard(val calendar: AdventCalendar, val summary: CalendarSummary, val personNames: List<String>)

/** Calendrier d'un autre Noël, proposé à la copie. */
data class OtherCalendar(val calendar: AdventCalendar, val year: Int)

data class AdventListUiState(
    val loading: Boolean = true,
    val edition: ChristmasEdition? = null,
    val cards: List<CalendarCard> = emptyList(),
    val others: List<OtherCalendar> = emptyList(),
    val templates: List<AdventTemplate> = emptyList(),
    val persons: List<Person> = emptyList(),
    val message: UiMessage? = null,
) {
    val readOnly: Boolean get() = edition?.isReadOnly == true
}

@HiltViewModel
class AdventListViewModel @Inject constructor(
    observeEditions: ObserveEditionsUseCase,
    private val advent: AdventRepository,
    persons: PersonRepository,
    private val duplicate: DuplicateCalendarUseCase,
    private val createFromTemplate: CreateFromTemplateUseCase,
) : ViewModel() {
    private val message = MutableStateFlow<UiMessage?>(null)
    private val editions = observeEditions()

    private val currentData = editions.flatMapLatest { state ->
        val edition = state.current ?: return@flatMapLatest flowOf(EditionAdvent(null, emptyList(), emptyList(), emptyList()))
        combine(
            advent.observeCalendars(edition.id),
            advent.observeBoxesForEdition(edition.id),
            advent.observeSuppliesForEdition(edition.id),
        ) { calendars, boxes, supplies -> EditionAdvent(edition, calendars, boxes, supplies) }
    }

    val uiState: StateFlow<AdventListUiState> = combine(
        currentData,
        editions,
        advent.observeAllCalendars(),
        advent.observeTemplates(),
        combine(persons.observePersons(), message) { p, m -> p to m },
    ) { current, editionsState, all, templates, (personList, msg) ->
        val names = personList.associate { it.id to it.firstName.ifBlank { it.displayName } }
        val years = editionsState.editions.associate { it.id to it.year }
        AdventListUiState(
            loading = false,
            edition = current.edition,
            cards = current.calendars.map { c ->
                CalendarCard(
                    calendar = c,
                    summary = summarizeCalendar(
                        c.boxCount,
                        c.budgetCents,
                        current.boxes.filter { it.calendarId == c.id },
                        current.supplies.filter { it.calendarId == c.id },
                    ),
                    personNames = c.personIds.mapNotNull(names::get).sorted(),
                )
            },
            others = all.filter { it.editionId != current.edition?.id }
                .map { OtherCalendar(it, years[it.editionId] ?: 0) }
                .sortedWith(compareByDescending<OtherCalendar> { it.year }.thenBy { it.calendar.name }),
            templates = templates,
            persons = personList,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdventListUiState())

    fun copyCalendar(sourceId: String) {
        val editionId = uiState.value.edition?.id ?: return
        viewModelScope.launch {
            message.value = when (val result = duplicate(sourceId, editionId)) {
                is AppResult.Success -> UiMessage(R.string.advent_copied, listOf(result.value.name))
                is AppResult.Failure -> result.error.toUiMessage()
            }
        }
    }

    fun createFromTemplate(templateId: String, name: String, personIds: Set<String>) {
        val editionId = uiState.value.edition?.id ?: return
        viewModelScope.launch {
            message.value = when (val result = createFromTemplate(templateId, editionId, name, personIds)) {
                is AppResult.Success -> UiMessage(R.string.advent_created_from_template, listOf(result.value.name))
                is AppResult.Failure -> result.error.toUiMessage()
            }
        }
    }

    fun deleteTemplate(id: String) {
        viewModelScope.launch { advent.deleteTemplate(id) }
    }

    fun messageShown() {
        message.value = null
    }
}

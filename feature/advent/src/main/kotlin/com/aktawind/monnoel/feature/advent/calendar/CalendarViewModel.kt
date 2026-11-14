package com.aktawind.monnoel.feature.advent.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.advent.AdventRepository
import com.aktawind.monnoel.core.domain.advent.CalendarSummary
import com.aktawind.monnoel.core.domain.advent.DeleteCalendarUseCase
import com.aktawind.monnoel.core.domain.advent.DeleteSupplyUseCase
import com.aktawind.monnoel.core.domain.advent.SaveAsTemplateUseCase
import com.aktawind.monnoel.core.domain.advent.SaveSupplyUseCase
import com.aktawind.monnoel.core.domain.advent.SetBoxStatusUseCase
import com.aktawind.monnoel.core.domain.advent.SwapBoxesUseCase
import com.aktawind.monnoel.core.domain.advent.summarizeCalendar
import com.aktawind.monnoel.core.domain.advent.supplyShares
import com.aktawind.monnoel.core.domain.repository.EditionRepository
import com.aktawind.monnoel.core.domain.repository.PersonRepository
import com.aktawind.monnoel.core.model.AdventBox
import com.aktawind.monnoel.core.model.AdventCalendar
import com.aktawind.monnoel.core.model.AdventSupply
import com.aktawind.monnoel.core.model.BoxStatus
import com.aktawind.monnoel.core.ui.UiMessage
import com.aktawind.monnoel.core.ui.toUiMessage
import com.aktawind.monnoel.feature.advent.R
import com.aktawind.monnoel.feature.advent.navigation.CalendarDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CalendarUiState(
    val loading: Boolean = true,
    val calendar: AdventCalendar? = null,
    val year: Int? = null,
    val readOnly: Boolean = false,
    val boxes: List<AdventBox> = emptyList(),
    val supplies: List<AdventSupply> = emptyList(),
    /** Part d'achat groupé de chaque case (id de case -> centimes). */
    val shares: Map<String, Long> = emptyMap(),
    val summary: CalendarSummary? = null,
    val personNames: List<String> = emptyList(),
    val message: UiMessage? = null,
    val deleted: Boolean = false,
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val advent: AdventRepository,
    editions: EditionRepository,
    persons: PersonRepository,
    private val swapBoxes: SwapBoxesUseCase,
    private val setStatus: SetBoxStatusUseCase,
    private val saveSupply: SaveSupplyUseCase,
    private val deleteSupply: DeleteSupplyUseCase,
    private val saveAsTemplate: SaveAsTemplateUseCase,
    private val deleteCalendar: DeleteCalendarUseCase,
) : ViewModel() {
    private val calendarId = savedStateHandle.toRoute<CalendarDestination>().calendarId
    private val message = MutableStateFlow<UiMessage?>(null)
    private val deleted = MutableStateFlow(false)

    private val edition = advent.observeCalendar(calendarId).flatMapLatest { c ->
        if (c == null) flowOf(null) else editions.observeEditions().map { list -> list.firstOrNull { it.id == c.editionId } }
    }

    val uiState: StateFlow<CalendarUiState> = combine(
        combine(advent.observeCalendar(calendarId), edition) { c, e -> c to e },
        advent.observeBoxes(calendarId),
        advent.observeSupplies(calendarId),
        persons.observePersons(),
        combine(message, deleted) { m, d -> m to d },
    ) { (calendar, ed), boxes, supplies, personList, (msg, isDeleted) ->
        val names = personList.associate { it.id to it.firstName.ifBlank { it.displayName } }
        CalendarUiState(
            loading = false,
            calendar = calendar,
            year = ed?.year,
            readOnly = ed?.isReadOnly == true,
            boxes = boxes,
            supplies = supplies,
            shares = supplies.flatMap { s -> supplyShares(s, boxes.filter { it.supplyId == s.id }).entries }
                .associate { it.key to it.value },
            summary = calendar?.let { summarizeCalendar(it.boxCount, it.budgetCents, boxes, supplies) },
            personNames = calendar?.personIds?.mapNotNull(names::get)?.sorted().orEmpty(),
            message = msg,
            deleted = isDeleted,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    fun swap(firstId: String, secondId: String) = report { swapBoxes(firstId, secondId) }

    fun setBoxStatus(boxId: String, status: BoxStatus) = report { setStatus(boxId, status) }

    fun saveSupply(supplyId: String?, name: String, quantity: Int, priceCents: Long?, bought: Boolean, boxIds: Set<String>) =
        report { saveSupply(calendarId, supplyId, name, quantity, priceCents, bought, boxIds) }

    fun toggleBought(supply: AdventSupply) = report {
        val linked = uiState.value.boxes.filter { it.supplyId == supply.id }.map { it.id }.toSet()
        saveSupply(calendarId, supply.id, supply.name, supply.quantity, supply.priceCents, !supply.bought, linked)
    }

    fun removeSupply(supplyId: String) = report { deleteSupply(calendarId, supplyId) }

    fun saveTemplate(name: String) = report(success = UiMessage(R.string.calendar_template_saved, listOf(name))) {
        saveAsTemplate(calendarId, name)
    }

    fun delete() {
        viewModelScope.launch {
            when (val result = deleteCalendar(calendarId)) {
                is AppResult.Success -> deleted.value = true
                is AppResult.Failure -> message.value = result.error.toUiMessage()
            }
        }
    }

    fun messageShown() {
        message.value = null
    }

    private fun report(success: UiMessage? = null, action: suspend () -> AppResult<*>) {
        viewModelScope.launch {
            when (val result = action()) {
                is AppResult.Success -> success?.let { message.value = it }
                is AppResult.Failure -> message.value = result.error.toUiMessage()
            }
        }
    }
}

package com.aktawind.monnoel.feature.gifts.person

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.edition.ObserveEditionsUseCase
import com.aktawind.monnoel.core.domain.gifts.ChangeGiftStatusUseCase
import com.aktawind.monnoel.core.domain.gifts.PersonGiftBudget
import com.aktawind.monnoel.core.domain.gifts.SetPersonBudgetUseCase
import com.aktawind.monnoel.core.domain.gifts.computeGiftBudget
import com.aktawind.monnoel.core.domain.gifts.parseEuroToCents
import com.aktawind.monnoel.core.domain.repository.GiftIdeaRepository
import com.aktawind.monnoel.core.domain.repository.PersonRepository
import com.aktawind.monnoel.core.domain.repository.PlannedGiftRepository
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.model.GiftIdea
import com.aktawind.monnoel.core.model.GiftStatus
import com.aktawind.monnoel.core.model.Person
import com.aktawind.monnoel.core.model.PlannedGift
import com.aktawind.monnoel.core.ui.UiMessage
import com.aktawind.monnoel.core.ui.toUiMessage
import com.aktawind.monnoel.feature.gifts.R
import com.aktawind.monnoel.feature.gifts.navigation.PersonDestination
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

data class PersonUiState(
    val loading: Boolean = true,
    val person: Person? = null,
    val edition: ChristmasEdition? = null,
    val currentGifts: List<PlannedGift> = emptyList(),
    /** Cadeaux des autres années, regroupés par année (la plus récente d'abord). */
    val history: List<Pair<Int, List<PlannedGift>>> = emptyList(),
    val ideas: List<GiftIdea> = emptyList(),
    val budget: PersonGiftBudget? = null,
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
) : ViewModel() {
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

    val uiState: StateFlow<PersonUiState> = combine(baseState, budgetFlow, message) { base, budget, msg ->
        base.copy(budget = budget, message = msg)
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

    fun messageShown() {
        message.value = null
    }
}

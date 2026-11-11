package com.aktawind.monnoel.feature.gifts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.edition.ObserveEditionsUseCase
import com.aktawind.monnoel.core.domain.gifts.ChangeGiftStatusUseCase
import com.aktawind.monnoel.core.domain.gifts.GiftBudgetSummary
import com.aktawind.monnoel.core.domain.gifts.OfferIdeaUseCase
import com.aktawind.monnoel.core.domain.gifts.computeGiftBudget
import com.aktawind.monnoel.core.domain.repository.GiftCategoryRepository
import com.aktawind.monnoel.core.domain.repository.GiftIdeaRepository
import com.aktawind.monnoel.core.domain.repository.PersonRepository
import com.aktawind.monnoel.core.domain.repository.PlannedGiftRepository
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.model.GiftCategory
import com.aktawind.monnoel.core.model.GiftIdea
import com.aktawind.monnoel.core.model.GiftStatus
import com.aktawind.monnoel.core.model.Person
import com.aktawind.monnoel.core.model.PlannedGift
import com.aktawind.monnoel.core.ui.UiMessage
import com.aktawind.monnoel.core.ui.toUiMessage
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

data class GiftsUiState(
    val loading: Boolean = true,
    val edition: ChristmasEdition? = null,
    val gifts: List<PlannedGift> = emptyList(),
    val persons: List<Person> = emptyList(),
    val ideas: List<GiftIdea> = emptyList(),
    val categories: List<GiftCategory> = emptyList(),
    val budget: GiftBudgetSummary = computeGiftBudget(emptyList(), emptyList()),
    val message: UiMessage? = null,
) {
    val readOnly: Boolean get() = edition?.isReadOnly == true
}

private data class EditionGifts(
    val edition: ChristmasEdition?,
    val gifts: List<PlannedGift>,
    val budget: GiftBudgetSummary,
)

@HiltViewModel
class GiftsViewModel @Inject constructor(
    observeEditions: ObserveEditionsUseCase,
    persons: PersonRepository,
    ideas: GiftIdeaRepository,
    categories: GiftCategoryRepository,
    private val gifts: PlannedGiftRepository,
    private val changeStatus: ChangeGiftStatusUseCase,
    private val offerIdea: OfferIdeaUseCase,
) : ViewModel() {
    private val message = MutableStateFlow<UiMessage?>(null)

    private val editionGifts = observeEditions().flatMapLatest { state ->
        val edition =
            state.current
                ?: return@flatMapLatest flowOf(
                    EditionGifts(null, emptyList(), computeGiftBudget(emptyList(), emptyList())),
                )
        combine(gifts.observeGifts(edition.id), gifts.observeBudgets(edition.id)) { list, budgets ->
            EditionGifts(edition, list, computeGiftBudget(list, budgets))
        }
    }

    val uiState: StateFlow<GiftsUiState> = combine(
        editionGifts,
        persons.observePersons(),
        ideas.observeIdeas(),
        categories.observeCategories(),
        message,
    ) { eg, personList, ideaList, categoryList, msg ->
        GiftsUiState(
            loading = false,
            edition = eg.edition,
            gifts = eg.gifts,
            persons = personList,
            ideas = ideaList,
            categories = categoryList,
            budget = eg.budget,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GiftsUiState())

    fun setStatus(giftId: String, status: GiftStatus) = viewModelScope.launch {
        (changeStatus(giftId, status) as? AppResult.Failure)?.let { message.value = it.error.toUiMessage() }
    }

    fun offer(ideaId: String, personId: String?) = viewModelScope.launch {
        val editionId = uiState.value.edition?.id ?: return@launch
        message.value = when (val result = offerIdea(ideaId, personId, editionId)) {
            is AppResult.Success -> UiMessage(R.string.gifts_offered, listOf(result.value.title))
            is AppResult.Failure -> result.error.toUiMessage()
        }
    }

    fun messageShown() {
        message.value = null
    }
}

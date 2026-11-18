package com.aktawind.monnoel.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.TimeProvider
import com.aktawind.monnoel.core.domain.advent.AdventRepository
import com.aktawind.monnoel.core.domain.advent.boxOfTheDay
import com.aktawind.monnoel.core.domain.budget.ObserveBudgetsUseCase
import com.aktawind.monnoel.core.domain.budget.YearBudget
import com.aktawind.monnoel.core.domain.cooking.CookingRepository
import com.aktawind.monnoel.core.domain.countdown.CountdownState
import com.aktawind.monnoel.core.domain.countdown.PathStep
import com.aktawind.monnoel.core.domain.countdown.christmasCountdown
import com.aktawind.monnoel.core.domain.countdown.christmasPath
import com.aktawind.monnoel.core.domain.edition.ObserveEditionsUseCase
import com.aktawind.monnoel.core.domain.gifts.IdeaDraft
import com.aktawind.monnoel.core.domain.gifts.SaveIdeaUseCase
import com.aktawind.monnoel.core.domain.meals.MealRepository
import com.aktawind.monnoel.core.domain.repository.GiftIdeaRepository
import com.aktawind.monnoel.core.domain.repository.PersonRepository
import com.aktawind.monnoel.core.domain.repository.PlannedGiftRepository
import com.aktawind.monnoel.core.domain.tasks.ObserveLutinUseCase
import com.aktawind.monnoel.core.model.AdventBox
import com.aktawind.monnoel.core.model.BoxStatus
import com.aktawind.monnoel.core.model.GiftStatus
import com.aktawind.monnoel.core.model.Meal
import com.aktawind.monnoel.core.model.Person
import com.aktawind.monnoel.core.model.PlannedGift
import com.aktawind.monnoel.core.model.ProductionStatus
import com.aktawind.monnoel.core.model.TaskStatus
import com.aktawind.monnoel.core.ui.QuickIdeaInput
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data object NoEdition : HomeUiState

    data class Ready(
        val year: Int,
        val isArchived: Boolean,
        val countdown: CountdownState,
        /** Jour de l'année, pour choisir le message du jour. */
        val dayOfYear: Int,
        val gifts: GiftsSummary = GiftsSummary(),
        val calendars: List<CalendarProgress> = emptyList(),
        val cooking: CookingSummary = CookingSummary(),
        val table: TableSummary = TableSummary(),
        val budget: YearBudget? = null,
        val path: List<PathStep> = emptyList(),
        val openTasks: List<String> = emptyList(),
        val doneTasks: Int = 0,
        val suggestionCount: Int = 0,
        val message: UiMessage? = null,
    ) : HomeUiState
}

/** Avancement d'un calendrier de l'Avent, et la case du jour en décembre. */
data class CalendarProgress(
    val name: String,
    val ready: Int,
    val boxCount: Int,
    val todayNumber: Int?,
    val todayBox: AdventBox?,
    /** Calendrier acheté tout prêt : null pour un calendrier fait maison, sinon acheté ou non. */
    val readyMadeBought: Boolean? = null,
)

/** Avancement des pâtisseries et gourmandises de l'année. */
data class CookingSummary(val total: Int = 0, val done: Int = 0, val inProgress: Int = 0, val next: List<String> = emptyList())

/** Les repas de l'année : le prochain, avec ses convives et son menu. */
data class TableSummary(val total: Int = 0, val next: Meal? = null, val nextDishes: Int = 0, val toOrder: Int = 0)

/** Résumé des cadeaux de l'année pour l'accueil. */
data class GiftsSummary(
    val total: Int = 0,
    val toBuy: Int = 0,
    val awaiting: Int = 0,
    val toWrap: Int = 0,
    val given: Int = 0,
    val wrapped: Int = 0,
    val plannedCents: Long = 0,
    val ideas: Int = 0,
)

/** Calendriers, cuisine, repas et budget, regroupés pour l'accueil. */
private data class Kitchen(
    val calendars: List<CalendarProgress>,
    val cooking: CookingSummary,
    val table: TableSummary,
    val budget: YearBudget?,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeEditions: ObserveEditionsUseCase,
    gifts: PlannedGiftRepository,
    ideas: GiftIdeaRepository,
    private val saveIdea: SaveIdeaUseCase,
    private val time: TimeProvider,
    personRepository: PersonRepository,
    private val advent: AdventRepository,
    private val observeLutin: ObserveLutinUseCase,
    cooking: CookingRepository,
    meals: MealRepository,
    observeBudgets: ObserveBudgetsUseCase,
) : ViewModel() {
    val persons: StateFlow<List<Person>> =
        personRepository.observePersons().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val message = MutableStateFlow<UiMessage?>(null)

    private val editionGifts = observeEditions().flatMapLatest { state ->
        val edition = state.current ?: return@flatMapLatest flowOf(null to emptyList<PlannedGift>())
        gifts.observeGifts(edition.id).map { edition to it }
    }

    private val editionCalendars = observeEditions().flatMapLatest { state ->
        val edition = state.current ?: return@flatMapLatest flowOf(emptyList())
        combine(advent.observeCalendars(edition.id), advent.observeBoxesForEdition(edition.id)) { calendars, boxes ->
            val today = time.today()
            calendars.map { c ->
                val own = boxes.filter { it.calendarId == c.id }
                val number = boxOfTheDay(today, edition.year, c.boxCount)
                CalendarProgress(
                    c.name,
                    own.count { it.status == BoxStatus.READY },
                    c.boxCount,
                    number,
                    own.firstOrNull {
                        it.number ==
                            number
                    },
                    readyMadeBought = if (c.isReadyMade) c.bought else null,
                )
            }
        }
    }

    private val editionCooking = observeEditions().flatMapLatest { state ->
        val edition = state.current ?: return@flatMapLatest flowOf(CookingSummary())
        cooking.observeProductions(edition.id).map { list ->
            CookingSummary(
                total = list.size,
                done = list.count { it.status == ProductionStatus.DONE },
                inProgress = list.count { it.status == ProductionStatus.IN_PROGRESS },
                next = list.filter { it.status != ProductionStatus.DONE }.sortedByDescending { it.status.ordinal }.map { it.recipeName },
            )
        }
    }

    private val editionTable = observeEditions().flatMapLatest { state ->
        val edition = state.current ?: return@flatMapLatest flowOf(TableSummary())
        combine(meals.observeMeals(edition.id), meals.observeDishes(edition.id)) { list, dishes ->
            val today = time.today()
            // Le prochain repas daté, sinon le premier de la liste.
            val next = list.firstOrNull { it.date != null && !it.date!!.isBefore(today) } ?: list.firstOrNull { it.date == null }
            TableSummary(
                total = list.size,
                next = next,
                nextDishes = dishes.count { it.mealId == next?.id },
                toOrder = dishes.count { it.toOrder && !it.ordered },
            )
        }
    }

    private val editionAdventAndCooking =
        combine(editionCalendars, editionCooking, editionTable, observeBudgets()) { calendars, summary, table, budgets ->
            Kitchen(calendars, summary, table, budgets.current)
        }

    val uiState: StateFlow<HomeUiState> = combine(editionGifts, ideas.observeIdeas(), editionAdventAndCooking, observeLutin(), message) {
            (edition, list),
            ideaList,
            (calendars, cookingSummary, tableSummary, yearBudget),
            lutin,
            msg,
        ->
        if (edition == null) return@combine HomeUiState.NoEdition
        val today = time.today()
        HomeUiState.Ready(
            year = edition.year,
            isArchived = edition.isArchived,
            countdown = christmasCountdown(today, edition.year),
            dayOfYear = today.dayOfYear,
            gifts = GiftsSummary(
                total = list.size,
                toBuy = list.count { it.status == GiftStatus.TO_BUY },
                awaiting = list.count { it.status.isAwaitingDelivery },
                toWrap = list.count { it.status == GiftStatus.TO_WRAP || it.status == GiftStatus.RECEIVED },
                given = list.count { it.status == GiftStatus.GIVEN },
                wrapped = list.count { it.status == GiftStatus.WRAPPED },
                plannedCents = list.sumOf { it.priceCents ?: 0 },
                ideas = ideaList.size,
            ),
            calendars = calendars,
            cooking = cookingSummary,
            table = tableSummary,
            budget = yearBudget,
            path = christmasPath(today, edition.year),
            openTasks = lutin.tasks.filter { it.isOpen }.map { it.title },
            doneTasks = lutin.tasks.count { it.status == TaskStatus.DONE },
            suggestionCount = lutin.suggestions.size,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    fun quickIdea(input: QuickIdeaInput) {
        viewModelScope.launch {
            val draft = IdeaDraft(
                title = input.title,
                url = input.url,
                estimatedPriceCents = input.priceCents,
                personIds = input.personIds,
            )
            message.value = when (val result = saveIdea(null, draft)) {
                is AppResult.Success -> UiMessage(com.aktawind.monnoel.core.ui.R.string.quick_idea_saved)
                is AppResult.Failure -> result.error.toUiMessage()
            }
        }
    }

    fun messageShown() {
        message.value = null
    }
}

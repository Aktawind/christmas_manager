package com.aktawind.monnoel.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktawind.monnoel.core.domain.advent.AdventRepository
import com.aktawind.monnoel.core.domain.budget.ExpenseRepository
import com.aktawind.monnoel.core.domain.cooking.CookingRepository
import com.aktawind.monnoel.core.domain.gifts.WhisperRepository
import com.aktawind.monnoel.core.domain.meals.MealRepository
import com.aktawind.monnoel.core.domain.repository.EditionRepository
import com.aktawind.monnoel.core.domain.repository.GiftIdeaRepository
import com.aktawind.monnoel.core.domain.repository.PersonRepository
import com.aktawind.monnoel.core.domain.repository.PlannedGiftRepository
import com.aktawind.monnoel.core.domain.search.SearchData
import com.aktawind.monnoel.core.domain.search.SearchResult
import com.aktawind.monnoel.core.domain.search.searchAll
import com.aktawind.monnoel.core.domain.tasks.TaskRepository
import com.aktawind.monnoel.core.domain.traditions.TraditionRepository
import com.aktawind.monnoel.core.model.LinkType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn

data class SearchUiState(val query: String = "", val results: Map<LinkType, List<SearchResult>> = emptyMap())

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private data class Kitchen(
    val recipes: List<com.aktawind.monnoel.core.model.Recipe>,
    val productions: List<com.aktawind.monnoel.core.model.Production>,
    val meals: List<com.aktawind.monnoel.core.model.Meal>,
    val dishes: List<com.aktawind.monnoel.core.model.MealDish>,
)

@OptIn(kotlinx.coroutines.FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    persons: PersonRepository,
    ideas: GiftIdeaRepository,
    gifts: PlannedGiftRepository,
    advent: AdventRepository,
    tasks: TaskRepository,
    editions: EditionRepository,
    cooking: CookingRepository,
    meals: MealRepository,
    expenses: ExpenseRepository,
    whispers: WhisperRepository,
    traditions: TraditionRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")

    private val data = combine(
        combine(persons.observePersons(), ideas.observeIdeas(), gifts.observeAllGifts()) { p, i, g -> Triple(p, i, g) },
        combine(advent.observeAllCalendars(), advent.observeAllBoxes(), traditions.observeTraditions(), traditions.observeInspirations()) {
                c,
                b,
                tr,
                ins,
            ->
            Quad(c, b, tr, ins)
        },
        combine(tasks.observeAllTasks(), expenses.observeAllExpenses(), whispers.observeAllWhispers()) { t, x, w -> Triple(t, x, w) },
        editions.observeEditions(),
        combine(cooking.observeRecipes(), cooking.observeAllProductions(), meals.observeAllMeals(), meals.observeAllDishes()) {
                r,
                pr,
                m,
                d,
            ->
            Kitchen(r, pr, m, d)
        },
    ) { (p, i, g), (c, b, tr, ins), (t, x, w), e, k ->
        SearchData(p, i, g, c, b, t, e.associate { it.id to it.year }, k.recipes, k.productions, k.meals, k.dishes, x, w, tr, ins)
    }

    val uiState: StateFlow<SearchUiState> = combine(query.debounce(150), data) { q, d ->
        SearchUiState(query = q, results = searchAll(q, d))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun setQuery(text: String) {
        query.value = text
    }
}

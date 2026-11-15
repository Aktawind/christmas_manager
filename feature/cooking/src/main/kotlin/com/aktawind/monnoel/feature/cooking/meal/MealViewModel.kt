package com.aktawind.monnoel.feature.cooking.meal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.edition.ObserveEditionsUseCase
import com.aktawind.monnoel.core.domain.meals.MealRepository
import com.aktawind.monnoel.core.domain.meals.SetDishOrderedUseCase
import com.aktawind.monnoel.core.domain.repository.PersonRepository
import com.aktawind.monnoel.core.model.Meal
import com.aktawind.monnoel.core.model.MealDish
import com.aktawind.monnoel.core.ui.UiMessage
import com.aktawind.monnoel.core.ui.toUiMessage
import com.aktawind.monnoel.feature.cooking.navigation.MealDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MealUiState(
    val loading: Boolean = true,
    val meal: Meal? = null,
    val dishes: List<MealDish> = emptyList(),
    /** Noms des convives présents dans l'application. */
    val personNames: List<String> = emptyList(),
    val readOnly: Boolean = false,
    val message: UiMessage? = null,
)

@HiltViewModel
class MealViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    meals: MealRepository,
    persons: PersonRepository,
    observeEditions: ObserveEditionsUseCase,
    private val setOrdered: SetDishOrderedUseCase,
) : ViewModel() {
    private val mealId = savedStateHandle.toRoute<MealDestination>().mealId
    private val message = MutableStateFlow<UiMessage?>(null)

    val uiState: StateFlow<MealUiState> = combine(
        meals.observeMeal(mealId),
        meals.observeDishesOfMeal(mealId),
        persons.observePersons(),
        observeEditions(),
        message,
    ) { meal, dishes, people, editions, msg ->
        val edition = meal?.let { m -> editions.editions.firstOrNull { it.id == m.editionId } }
        MealUiState(
            loading = false,
            meal = meal,
            dishes = dishes,
            personNames = people.filter { meal != null && it.id in meal.personIds }.map { it.displayName },
            readOnly = edition?.isReadOnly == true,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MealUiState())

    fun setOrdered(dishId: String, ordered: Boolean) {
        viewModelScope.launch { (setOrdered(dishId, ordered) as? AppResult.Failure)?.let { message.value = it.error.toUiMessage() } }
    }

    fun messageShown() {
        message.value = null
    }
}

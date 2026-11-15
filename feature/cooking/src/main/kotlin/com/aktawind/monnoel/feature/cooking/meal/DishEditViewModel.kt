package com.aktawind.monnoel.feature.cooking.meal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.cooking.CookingRepository
import com.aktawind.monnoel.core.domain.edition.ObserveEditionsUseCase
import com.aktawind.monnoel.core.domain.meals.DeleteDishUseCase
import com.aktawind.monnoel.core.domain.meals.DishDraft
import com.aktawind.monnoel.core.domain.meals.MealRepository
import com.aktawind.monnoel.core.domain.meals.SaveDishUseCase
import com.aktawind.monnoel.core.model.CourseType
import com.aktawind.monnoel.core.model.Recipe
import com.aktawind.monnoel.core.ui.UiMessage
import com.aktawind.monnoel.core.ui.toUiMessage
import com.aktawind.monnoel.feature.cooking.navigation.DishEditDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DishForm(
    val course: CourseType = CourseType.MAIN,
    val recipeId: String? = null,
    val name: String = "",
    val broughtBy: String = "",
    val toOrder: Boolean = false,
    val ordered: Boolean = false,
)

data class DishEditUiState(
    val isNew: Boolean = true,
    val loading: Boolean = true,
    val mealName: String = "",
    val readOnly: Boolean = false,
    val form: DishForm = DishForm(),
    val photoPath: String? = null,
    val recipes: List<Recipe> = emptyList(),
    val busy: Boolean = false,
    val message: UiMessage? = null,
    val done: Boolean = false,
)

@HiltViewModel
class DishEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val meals: MealRepository,
    cooking: CookingRepository,
    private val observeEditions: ObserveEditionsUseCase,
    private val saveDish: SaveDishUseCase,
    private val deleteDish: DeleteDishUseCase,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<DishEditDestination>()
    private val state = MutableStateFlow(DishEditUiState(isNew = route.dishId == null))

    val uiState: StateFlow<DishEditUiState> = combine(state, cooking.observeRecipes()) { s, r -> s.copy(recipes = r) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), state.value)

    init {
        viewModelScope.launch {
            val dish = route.dishId?.let { meals.getDish(it) }
            val meal = meals.getMeal(dish?.mealId ?: route.mealId)
            val edition = observeEditions().first().editions.firstOrNull { it.id == meal?.editionId }
            val initialCourse = route.course?.let { runCatching { CourseType.valueOf(it) }.getOrNull() } ?: CourseType.MAIN
            state.update {
                it.copy(
                    loading = false,
                    mealName = meal?.name.orEmpty(),
                    readOnly = edition?.isReadOnly == true,
                    photoPath = dish?.photoPath,
                    form = dish?.let { d -> DishForm(d.course, d.recipeId, d.name, d.broughtBy, d.toOrder, d.ordered) }
                        ?: DishForm(course = initialCourse),
                )
            }
        }
    }

    fun update(transform: DishForm.() -> DishForm) = state.update { it.copy(form = it.form.transform()) }

    fun selectRecipe(recipe: Recipe?) = state.update {
        it.copy(form = it.form.copy(recipeId = recipe?.id, name = recipe?.name ?: it.form.name), photoPath = recipe?.photoPath)
    }

    fun save() = viewModelScope.launch {
        val form = state.value.form
        state.update { it.copy(busy = true) }
        val result =
            saveDish(
                route.dishId,
                route.mealId,
                DishDraft(form.course, form.recipeId, form.name, form.broughtBy, form.toOrder, form.ordered),
            )
        state.update {
            when (result) {
                is AppResult.Success -> it.copy(busy = false, done = true)
                is AppResult.Failure -> it.copy(busy = false, message = result.error.toUiMessage())
            }
        }
    }

    fun delete() = viewModelScope.launch {
        val id = route.dishId ?: return@launch
        when (val result = deleteDish(id)) {
            is AppResult.Success -> state.update { it.copy(done = true) }
            is AppResult.Failure -> state.update { it.copy(message = result.error.toUiMessage()) }
        }
    }

    fun messageShown() = state.update { it.copy(message = null) }
}

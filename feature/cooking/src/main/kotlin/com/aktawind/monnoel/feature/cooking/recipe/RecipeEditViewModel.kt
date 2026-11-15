package com.aktawind.monnoel.feature.cooking.recipe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.ApplicationScope
import com.aktawind.monnoel.core.domain.cooking.CookingRepository
import com.aktawind.monnoel.core.domain.cooking.DeleteRecipeUseCase
import com.aktawind.monnoel.core.domain.cooking.SaveRecipeUseCase
import com.aktawind.monnoel.core.domain.edition.ObserveEditionsUseCase
import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.model.RecipeCategory
import com.aktawind.monnoel.core.ui.UiMessage
import com.aktawind.monnoel.core.ui.toUiMessage
import com.aktawind.monnoel.feature.cooking.PhotoDrafts
import com.aktawind.monnoel.feature.cooking.navigation.RecipeEditDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecipeForm(val name: String = "", val photoPath: String? = null, val categoryId: String? = null, val url: String = "")

/** Après l'enregistrement : revenir, ou ouvrir une production pour cette recette. */
sealed interface RecipeEditOutcome {
    data object Closed : RecipeEditOutcome

    data class Make(val recipeId: String) : RecipeEditOutcome
}

data class RecipeEditUiState(
    val isNew: Boolean = true,
    val loading: Boolean = true,
    val form: RecipeForm = RecipeForm(),
    val categories: List<RecipeCategory> = emptyList(),
    val edition: ChristmasEdition? = null,
    val busy: Boolean = false,
    val message: UiMessage? = null,
    val outcome: RecipeEditOutcome? = null,
) {
    /** « Faire pour Noël AAAA » n'a de sens que pour une année modifiable. */
    val canMake: Boolean get() = edition?.isReadOnly == false
}

@HiltViewModel
class RecipeEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cooking: CookingRepository,
    observeEditions: ObserveEditionsUseCase,
    private val photos: PhotoRepository,
    private val saveRecipe: SaveRecipeUseCase,
    private val deleteRecipe: DeleteRecipeUseCase,
    @ApplicationScope appScope: CoroutineScope,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<RecipeEditDestination>()
    private val state = MutableStateFlow(RecipeEditUiState(isNew = route.recipeId == null))
    private val drafts = PhotoDrafts(photos, appScope)

    val uiState: StateFlow<RecipeEditUiState> = combine(state, cooking.observeCategories(), observeEditions()) { s, c, e ->
        s.copy(categories = c, edition = e.current)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), state.value)

    init {
        viewModelScope.launch {
            val recipe = route.recipeId?.let { cooking.getRecipe(it) }
            state.update {
                it.copy(
                    loading = false,
                    form = recipe?.let { r -> RecipeForm(r.name, r.photoPath, r.categoryId, r.url) } ?: RecipeForm(),
                )
            }
        }
    }

    fun update(transform: RecipeForm.() -> RecipeForm) = state.update { it.copy(form = it.form.transform()) }

    fun importPhoto(uri: String) = viewModelScope.launch {
        when (val result = photos.importPhoto(uri)) {
            is AppResult.Success -> {
                drafts.added(result.value)
                update { copy(photoPath = result.value) }
            }
            is AppResult.Failure -> state.update { it.copy(message = UiMessage(com.aktawind.monnoel.core.ui.R.string.photo_error)) }
        }
    }

    /** Enregistre ; si [make], ouvre ensuite une nouvelle production pour cette recette. */
    fun save(make: Boolean = false) = viewModelScope.launch {
        val form = state.value.form
        state.update { it.copy(busy = true) }
        val result = saveRecipe(route.recipeId, form.name, form.photoPath, form.categoryId, form.url)
        state.update {
            when (result) {
                is AppResult.Success -> {
                    drafts.committed(form.photoPath)
                    val outcome = if (make) RecipeEditOutcome.Make(result.value.id) else RecipeEditOutcome.Closed
                    it.copy(busy = false, outcome = outcome)
                }
                is AppResult.Failure -> it.copy(busy = false, message = result.error.toUiMessage())
            }
        }
    }

    fun delete() = viewModelScope.launch {
        val id = route.recipeId ?: return@launch
        when (val result = deleteRecipe(id)) {
            is AppResult.Success -> state.update { it.copy(outcome = RecipeEditOutcome.Closed) }
            is AppResult.Failure -> state.update { it.copy(message = result.error.toUiMessage()) }
        }
    }

    fun messageShown() = state.update { it.copy(message = null) }

    override fun onCleared() {
        drafts.discard()
    }
}

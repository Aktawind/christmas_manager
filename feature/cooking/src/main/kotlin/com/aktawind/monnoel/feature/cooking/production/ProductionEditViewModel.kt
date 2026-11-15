package com.aktawind.monnoel.feature.cooking.production

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.ApplicationScope
import com.aktawind.monnoel.core.domain.cooking.CookingRepository
import com.aktawind.monnoel.core.domain.cooking.DeleteProductionUseCase
import com.aktawind.monnoel.core.domain.cooking.ProductionDraft
import com.aktawind.monnoel.core.domain.cooking.SaveProductionUseCase
import com.aktawind.monnoel.core.domain.edition.ObserveEditionsUseCase
import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.model.ProductionStatus
import com.aktawind.monnoel.core.model.Recipe
import com.aktawind.monnoel.core.ui.UiMessage
import com.aktawind.monnoel.core.ui.toUiMessage
import com.aktawind.monnoel.feature.cooking.PhotoDrafts
import com.aktawind.monnoel.feature.cooking.navigation.ProductionEditDestination
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

data class ProductionForm(
    val recipeId: String? = null,
    val name: String = "",
    val quantity: Int = 1,
    val destination: String = "",
    val status: ProductionStatus = ProductionStatus.TO_DO,
    val photoPath: String? = null,
    val notes: String = "",
)

data class ProductionEditUiState(
    val isNew: Boolean = true,
    val loading: Boolean = true,
    val edition: ChristmasEdition? = null,
    val form: ProductionForm = ProductionForm(),
    /** Photo copiée de la recette, affichée tant qu'il n'y a pas de photo du résultat. */
    val recipePhotoPath: String? = null,
    val recipes: List<Recipe> = emptyList(),
    val busy: Boolean = false,
    val message: UiMessage? = null,
    val done: Boolean = false,
) {
    val readOnly: Boolean get() = edition?.isReadOnly == true
}

@HiltViewModel
class ProductionEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cooking: CookingRepository,
    private val observeEditions: ObserveEditionsUseCase,
    private val photos: PhotoRepository,
    private val saveProduction: SaveProductionUseCase,
    private val deleteProduction: DeleteProductionUseCase,
    @ApplicationScope appScope: CoroutineScope,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<ProductionEditDestination>()
    private val state = MutableStateFlow(ProductionEditUiState(isNew = route.productionId == null))
    private val drafts = PhotoDrafts(photos, appScope)

    val uiState: StateFlow<ProductionEditUiState> = combine(state, cooking.observeRecipes()) { s, r -> s.copy(recipes = r) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), state.value)

    init {
        viewModelScope.launch {
            val editions = observeEditions().first()
            val production = route.productionId?.let { cooking.getProduction(it) }
            val edition = production?.let { p -> editions.editions.firstOrNull { it.id == p.editionId } } ?: editions.current
            val recipe = (production?.recipeId ?: route.recipeId)?.let { cooking.getRecipe(it) }
            state.update {
                it.copy(
                    loading = false,
                    edition = edition,
                    recipePhotoPath = production?.recipePhotoPath ?: recipe?.photoPath,
                    form = production?.let { p ->
                        ProductionForm(p.recipeId, p.recipeName, p.quantity, p.destination, p.status, p.photoPath, p.notes)
                    } ?: ProductionForm(recipeId = recipe?.id, name = recipe?.name.orEmpty()),
                )
            }
        }
    }

    fun update(transform: ProductionForm.() -> ProductionForm) = state.update { it.copy(form = it.form.transform()) }

    /** Choisir une recette reprend son nom ; « aucune » laisse le nom libre. */
    fun selectRecipe(recipe: Recipe?) = state.update {
        it.copy(
            form = it.form.copy(recipeId = recipe?.id, name = recipe?.name ?: it.form.name),
            recipePhotoPath = recipe?.photoPath,
        )
    }

    fun importPhoto(uri: String) = viewModelScope.launch {
        when (val result = photos.importPhoto(uri)) {
            is AppResult.Success -> {
                drafts.added(result.value)
                update { copy(photoPath = result.value) }
            }
            is AppResult.Failure -> state.update { it.copy(message = UiMessage(com.aktawind.monnoel.core.ui.R.string.photo_error)) }
        }
    }

    fun save() = viewModelScope.launch {
        val current = state.value
        val edition = current.edition ?: return@launch
        val form = current.form
        state.update { it.copy(busy = true) }
        val result = saveProduction(
            route.productionId,
            edition.id,
            ProductionDraft(form.recipeId, form.name, form.quantity, form.destination, form.status, form.photoPath, form.notes),
        )
        state.update {
            when (result) {
                is AppResult.Success -> {
                    drafts.committed(form.photoPath)
                    it.copy(busy = false, done = true)
                }
                is AppResult.Failure -> it.copy(busy = false, message = result.error.toUiMessage())
            }
        }
    }

    fun delete() = viewModelScope.launch {
        val id = route.productionId ?: return@launch
        when (val result = deleteProduction(id)) {
            is AppResult.Success -> state.update { it.copy(done = true) }
            is AppResult.Failure -> state.update { it.copy(message = result.error.toUiMessage()) }
        }
    }

    fun messageShown() = state.update { it.copy(message = null) }

    override fun onCleared() {
        drafts.discard()
    }
}

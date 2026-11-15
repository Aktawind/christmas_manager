package com.aktawind.monnoel.feature.cooking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.cooking.CookingRepository
import com.aktawind.monnoel.core.domain.cooking.SetProductionStatusUseCase
import com.aktawind.monnoel.core.domain.edition.ObserveEditionsUseCase
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.model.Production
import com.aktawind.monnoel.core.model.ProductionStatus
import com.aktawind.monnoel.core.model.Recipe
import com.aktawind.monnoel.core.model.RecipeCategory
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

data class CookingUiState(
    val loading: Boolean = true,
    val edition: ChristmasEdition? = null,
    val recipes: List<Recipe> = emptyList(),
    val categories: List<RecipeCategory> = emptyList(),
    val productions: List<Production> = emptyList(),
    val message: UiMessage? = null,
) {
    val readOnly: Boolean get() = edition?.isReadOnly == true
}

@HiltViewModel
class CookingViewModel @Inject constructor(
    observeEditions: ObserveEditionsUseCase,
    cooking: CookingRepository,
    private val setStatus: SetProductionStatusUseCase,
) : ViewModel() {
    private val message = MutableStateFlow<UiMessage?>(null)

    private val editionProductions = observeEditions().map { it.current }.flatMapLatest { edition ->
        if (edition == null) flowOf(null to emptyList()) else cooking.observeProductions(edition.id).map { edition to it }
    }

    val uiState: StateFlow<CookingUiState> = combine(
        editionProductions,
        cooking.observeRecipes(),
        cooking.observeCategories(),
        message,
    ) { (edition, productions), recipes, categories, msg ->
        CookingUiState(false, edition, recipes, categories, productions, msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CookingUiState())

    fun setProductionStatus(id: String, status: ProductionStatus) {
        viewModelScope.launch { (setStatus(id, status) as? AppResult.Failure)?.let { message.value = it.error.toUiMessage() } }
    }

    fun messageShown() {
        message.value = null
    }
}

package com.aktawind.monnoel.feature.budget

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.budget.BudgetOverview
import com.aktawind.monnoel.core.domain.budget.DeleteExpenseUseCase
import com.aktawind.monnoel.core.domain.budget.ExpenseDraft
import com.aktawind.monnoel.core.domain.budget.ObserveBudgetsUseCase
import com.aktawind.monnoel.core.domain.budget.SaveExpenseUseCase
import com.aktawind.monnoel.core.domain.budget.SetEditionBudgetUseCase
import com.aktawind.monnoel.core.domain.budget.SetExpensePaidUseCase
import com.aktawind.monnoel.core.domain.gifts.parseEuroToCents
import com.aktawind.monnoel.core.ui.UiMessage
import com.aktawind.monnoel.core.ui.toUiMessage
import com.aktawind.monnoel.feature.budget.navigation.BudgetDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BudgetUiState(
    val loading: Boolean = true,
    val overview: BudgetOverview? = null,
    val message: UiMessage? = null,
) {
    val readOnly: Boolean get() = overview?.edition?.isReadOnly == true
}

@HiltViewModel
class BudgetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeBudgets: ObserveBudgetsUseCase,
    private val saveExpense: SaveExpenseUseCase,
    private val setPaid: SetExpensePaidUseCase,
    private val deleteExpense: DeleteExpenseUseCase,
    private val setEditionBudget: SetEditionBudgetUseCase,
) : ViewModel() {
    /** Dépense à ouvrir à l'arrivée (recherche). */
    val initialExpenseId: String? = savedStateHandle.toRoute<BudgetDestination>().expenseId
    private val message = MutableStateFlow<UiMessage?>(null)

    val uiState: StateFlow<BudgetUiState> = combine(observeBudgets(), message) { overview, msg ->
        BudgetUiState(loading = false, overview = overview, message = msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetUiState())

    private fun report(result: AppResult<*>) {
        (result as? AppResult.Failure)?.let { message.value = it.error.toUiMessage() }
    }

    fun setGlobalBudget(input: String) {
        val edition = uiState.value.overview?.edition ?: return
        val cents = if (input.isBlank()) null else parseEuroToCents(input) ?: return
        viewModelScope.launch { report(setEditionBudget(edition.id, cents)) }
    }

    fun saveExpense(id: String?, draft: ExpenseDraft) {
        val edition = uiState.value.overview?.edition ?: return
        viewModelScope.launch { report(saveExpense(id, edition.id, draft)) }
    }

    fun setPaid(id: String, paid: Boolean) {
        viewModelScope.launch { report(setPaid.invoke(id, paid)) }
    }

    fun delete(id: String) {
        viewModelScope.launch { report(deleteExpense(id)) }
    }

    fun messageShown() {
        message.value = null
    }
}

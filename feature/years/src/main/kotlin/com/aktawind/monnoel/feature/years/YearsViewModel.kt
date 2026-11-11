package com.aktawind.monnoel.feature.years

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.TimeProvider
import com.aktawind.monnoel.core.domain.edition.CreateEditionUseCase
import com.aktawind.monnoel.core.domain.edition.DeleteEditionUseCase
import com.aktawind.monnoel.core.domain.edition.ObserveEditionsUseCase
import com.aktawind.monnoel.core.domain.edition.SelectEditionUseCase
import com.aktawind.monnoel.core.domain.edition.SetEditionArchivedUseCase
import com.aktawind.monnoel.core.domain.edition.suggestedNewYear
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.ui.UiMessage
import com.aktawind.monnoel.core.ui.toUiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class YearsUiState(
    val loading: Boolean = true,
    val editions: List<ChristmasEdition> = emptyList(),
    val currentId: String? = null,
    val suggestedYear: Int = 0,
    val message: UiMessage? = null,
)

@HiltViewModel
class YearsViewModel @Inject constructor(
    observeEditions: ObserveEditionsUseCase,
    private val createEdition: CreateEditionUseCase,
    private val setArchived: SetEditionArchivedUseCase,
    private val deleteEdition: DeleteEditionUseCase,
    private val selectEdition: SelectEditionUseCase,
    private val time: TimeProvider,
) : ViewModel() {
    private val message = MutableStateFlow<UiMessage?>(null)

    val uiState: StateFlow<YearsUiState> = combine(observeEditions(), message) { state, msg ->
        YearsUiState(
            loading = false,
            editions = state.editions,
            currentId = state.current?.id,
            suggestedYear = suggestedNewYear(time.today(), state.editions.map { it.year }),
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), YearsUiState())

    fun create(year: Int) = launchAndReport({
        createEdition(year)
    }, success = UiMessage(R.string.years_created, listOf(year)))

    fun select(id: String) = launchAndReport({ selectEdition(id) })

    fun setArchived(edition: ChristmasEdition, archived: Boolean) = launchAndReport(
        { setArchived(edition.id, archived) },
        success = UiMessage(if (archived) R.string.years_archived else R.string.years_unarchived, listOf(edition.year)),
    )

    fun delete(edition: ChristmasEdition) = launchAndReport(
        { deleteEdition(edition.id) },
        success = UiMessage(R.string.years_deleted, listOf(edition.year)),
    )

    fun messageShown() = message.update { null }

    private fun launchAndReport(action: suspend () -> AppResult<*>, success: UiMessage? = null) {
        viewModelScope.launch {
            when (val result = action()) {
                is AppResult.Success -> if (success != null) message.value = success
                is AppResult.Failure -> message.value = result.error.toUiMessage()
            }
        }
    }
}

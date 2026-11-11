package com.aktawind.monnoel.feature.years

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.TimeProvider
import com.aktawind.monnoel.core.domain.edition.CreateEditionUseCase
import com.aktawind.monnoel.core.domain.edition.christmasYearFor
import com.aktawind.monnoel.core.domain.repository.BackupRepository
import com.aktawind.monnoel.core.model.BackupSummary
import com.aktawind.monnoel.core.ui.UiMessage
import com.aktawind.monnoel.core.ui.toUiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WelcomeUiState(
    val year: Int,
    val busy: Boolean = false,
    val pendingImport: Pair<String, BackupSummary>? = null,
    val message: UiMessage? = null,
)

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val createEdition: CreateEditionUseCase,
    private val backups: BackupRepository,
    time: TimeProvider,
) : ViewModel() {
    private val state = MutableStateFlow(WelcomeUiState(year = christmasYearFor(time.today())))
    val uiState: StateFlow<WelcomeUiState> = state.asStateFlow()

    fun start() = viewModelScope.launch {
        state.update { it.copy(busy = true) }
        val result = createEdition(state.value.year)
        state.update {
            it.copy(busy = false, message = (result as? AppResult.Failure)?.error?.toUiMessage())
        }
    }

    fun inspect(uri: String) = viewModelScope.launch {
        state.update { it.copy(busy = true) }
        when (val result = backups.inspect(uri)) {
            is AppResult.Success -> state.update { it.copy(busy = false, pendingImport = uri to result.value) }
            is AppResult.Failure -> state.update { it.copy(busy = false, message = result.error.toUiMessage()) }
        }
    }

    fun confirmImport() = viewModelScope.launch {
        val uri = state.value.pendingImport?.first ?: return@launch
        state.update { it.copy(busy = true, pendingImport = null) }
        val result = backups.importFrom(uri)
        state.update {
            it.copy(busy = false, message = (result as? AppResult.Failure)?.error?.toUiMessage())
        }
    }

    fun cancelImport() = state.update { it.copy(pendingImport = null) }

    fun messageShown() = state.update { it.copy(message = null) }
}

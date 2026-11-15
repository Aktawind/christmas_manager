package com.aktawind.monnoel.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.tasks.AcceptSuggestionUseCase
import com.aktawind.monnoel.core.domain.tasks.DismissSuggestionUseCase
import com.aktawind.monnoel.core.domain.tasks.LutinState
import com.aktawind.monnoel.core.domain.tasks.ObserveLutinUseCase
import com.aktawind.monnoel.core.domain.tasks.SetTaskStatusUseCase
import com.aktawind.monnoel.core.domain.tasks.Suggestion
import com.aktawind.monnoel.core.model.TaskStatus
import com.aktawind.monnoel.core.ui.UiMessage
import com.aktawind.monnoel.core.ui.toUiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TasksUiState(
    val loading: Boolean = true,
    val lutin: LutinState = LutinState(null, emptyList(), emptyList(), emptyMap()),
    val message: UiMessage? = null,
) {
    val readOnly: Boolean get() = lutin.edition?.isReadOnly == true
}

@HiltViewModel
class TasksViewModel @Inject constructor(
    observeLutin: ObserveLutinUseCase,
    private val setStatus: SetTaskStatusUseCase,
    private val accept: AcceptSuggestionUseCase,
    private val dismiss: DismissSuggestionUseCase,
) : ViewModel() {
    private val message = MutableStateFlow<UiMessage?>(null)

    val uiState: StateFlow<TasksUiState> = combine(observeLutin(), message) { lutin, msg ->
        TasksUiState(loading = false, lutin = lutin, message = msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksUiState())

    fun toggle(taskId: String, done: Boolean) = launchReport { setStatus(taskId, if (done) TaskStatus.DONE else TaskStatus.TODO) }

    fun accept(suggestion: Suggestion, title: String) {
        val editionId = uiState.value.lutin.edition?.id ?: return
        launchReport { accept(editionId, suggestion, title) }
    }

    fun acceptAll(labeled: List<Pair<Suggestion, String>>) {
        val editionId = uiState.value.lutin.edition?.id ?: return
        viewModelScope.launch { labeled.forEach { (s, title) -> accept(editionId, s, title) } }
    }

    fun dismiss(suggestion: Suggestion) {
        val editionId = uiState.value.lutin.edition?.id ?: return
        viewModelScope.launch { dismiss(editionId, suggestion.key) }
    }

    fun messageShown() {
        message.value = null
    }

    private fun launchReport(action: suspend () -> AppResult<*>) {
        viewModelScope.launch { (action() as? AppResult.Failure)?.let { message.value = it.error.toUiMessage() } }
    }
}

package com.aktawind.monnoel.feature.tasks.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.edition.ObserveEditionsUseCase
import com.aktawind.monnoel.core.domain.tasks.DeleteTaskUseCase
import com.aktawind.monnoel.core.domain.tasks.SaveTaskUseCase
import com.aktawind.monnoel.core.domain.tasks.TaskDraft
import com.aktawind.monnoel.core.domain.tasks.TaskRepository
import com.aktawind.monnoel.core.model.LinkType
import com.aktawind.monnoel.core.model.TaskModule
import com.aktawind.monnoel.core.model.TaskStatus
import com.aktawind.monnoel.core.ui.UiMessage
import com.aktawind.monnoel.core.ui.toUiMessage
import com.aktawind.monnoel.feature.tasks.navigation.TaskEditDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskForm(
    val title: String = "",
    val description: String = "",
    val status: TaskStatus = TaskStatus.TODO,
    val module: TaskModule = TaskModule.OTHER,
    val notes: String = "",
)

data class TaskEditUiState(
    val isNew: Boolean = true,
    val loading: Boolean = true,
    val readOnly: Boolean = false,
    val form: TaskForm = TaskForm(),
    val linkType: LinkType? = null,
    val linkId: String? = null,
    val automatic: Boolean = false,
    val busy: Boolean = false,
    val message: UiMessage? = null,
    val done: Boolean = false,
)

@HiltViewModel
class TaskEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tasks: TaskRepository,
    private val observeEditions: ObserveEditionsUseCase,
    private val saveTask: SaveTaskUseCase,
    private val deleteTask: DeleteTaskUseCase,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<TaskEditDestination>()
    private val state = MutableStateFlow(TaskEditUiState(isNew = route.taskId == null))
    val uiState: StateFlow<TaskEditUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            val task = route.taskId?.let { tasks.getTask(it) }
            val editions = observeEditions().first()
            val edition = task?.let { t -> editions.editions.firstOrNull { it.id == t.editionId } } ?: editions.current
            state.update {
                it.copy(
                    loading = false,
                    readOnly = edition?.isReadOnly == true,
                    form = task?.let { t -> TaskForm(t.title, t.description, t.status, t.module, t.notes) } ?: TaskForm(),
                    linkType = task?.linkType,
                    linkId = task?.linkId,
                    automatic = task?.suggestionKey != null,
                )
            }
        }
    }

    fun update(transform: TaskForm.() -> TaskForm) = state.update { it.copy(form = it.form.transform()) }

    fun save() {
        viewModelScope.launch {
            val editionId = route.taskId?.let { tasks.getTask(it)?.editionId } ?: observeEditions().first().current?.id ?: return@launch
            val f = state.value.form
            state.update { it.copy(busy = true) }
            val result = saveTask(route.taskId, editionId, TaskDraft(f.title, f.description, f.status, f.module, f.notes))
            state.update {
                when (result) {
                    is AppResult.Success -> it.copy(busy = false, done = true)
                    is AppResult.Failure -> it.copy(busy = false, message = result.error.toUiMessage())
                }
            }
        }
    }

    fun delete() {
        val id = route.taskId ?: return
        viewModelScope.launch {
            when (val result = deleteTask(id)) {
                is AppResult.Success -> state.update { it.copy(done = true) }
                is AppResult.Failure -> state.update { it.copy(message = result.error.toUiMessage()) }
            }
        }
    }

    fun messageShown() = state.update { it.copy(message = null) }
}

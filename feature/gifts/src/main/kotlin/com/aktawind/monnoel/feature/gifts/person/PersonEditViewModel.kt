package com.aktawind.monnoel.feature.gifts.person

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.ApplicationScope
import com.aktawind.monnoel.core.domain.gifts.DeletePersonUseCase
import com.aktawind.monnoel.core.domain.gifts.SavePersonUseCase
import com.aktawind.monnoel.core.domain.repository.PersonRepository
import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import com.aktawind.monnoel.core.ui.UiMessage
import com.aktawind.monnoel.core.ui.toUiMessage
import com.aktawind.monnoel.feature.gifts.edit.PhotoDrafts
import com.aktawind.monnoel.feature.gifts.navigation.PersonEditDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PersonForm(
    val firstName: String = "",
    val lastName: String = "",
    val photoPath: String? = null,
    val interests: String = "",
    val notes: String = "",
)

data class PersonEditUiState(
    val isNew: Boolean = true,
    val loading: Boolean = true,
    val form: PersonForm = PersonForm(),
    val busy: Boolean = false,
    val message: UiMessage? = null,
    /** Identifiant enregistré (pour ouvrir la fiche), ou "" après suppression. */
    val doneWithId: String? = null,
)

@HiltViewModel
class PersonEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val persons: PersonRepository,
    private val photos: PhotoRepository,
    private val savePerson: SavePersonUseCase,
    private val deletePerson: DeletePersonUseCase,
    @ApplicationScope appScope: CoroutineScope,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<PersonEditDestination>()
    private val state = MutableStateFlow(PersonEditUiState(isNew = route.personId == null))
    val uiState: StateFlow<PersonEditUiState> = state.asStateFlow()
    private val drafts = PhotoDrafts(photos, appScope)

    init {
        viewModelScope.launch {
            val person = route.personId?.let { persons.getPerson(it) }
            state.update {
                it.copy(
                    loading = false,
                    form =
                    person?.let { p -> PersonForm(p.firstName, p.lastName, p.photoPath, p.interests, p.notes) }
                        ?: PersonForm(),
                )
            }
        }
    }

    fun update(transform: PersonForm.() -> PersonForm) = state.update { it.copy(form = it.form.transform()) }

    fun importPhoto(uri: String) = viewModelScope.launch {
        when (val result = photos.importPhoto(uri)) {
            is AppResult.Success -> {
                drafts.added(result.value)
                update { copy(photoPath = result.value) }
            }
            is AppResult.Failure -> state.update {
                it.copy(message = UiMessage(com.aktawind.monnoel.core.ui.R.string.photo_error))
            }
        }
    }

    fun save() = viewModelScope.launch {
        val f = state.value.form
        state.update { it.copy(busy = true) }
        val result = savePerson(route.personId, f.firstName, f.lastName, f.photoPath, f.interests, f.notes)
        state.update {
            when (result) {
                is AppResult.Success -> {
                    drafts.committed(f.photoPath)
                    it.copy(busy = false, doneWithId = result.value.id)
                }
                is AppResult.Failure -> it.copy(busy = false, message = result.error.toUiMessage())
            }
        }
    }

    fun delete() = viewModelScope.launch {
        val id = route.personId ?: return@launch
        when (val result = deletePerson(id)) {
            is AppResult.Success -> state.update { it.copy(doneWithId = "") }
            is AppResult.Failure -> state.update { it.copy(message = result.error.toUiMessage()) }
        }
    }

    fun messageShown() = state.update { it.copy(message = null) }

    override fun onCleared() {
        drafts.discard()
    }
}

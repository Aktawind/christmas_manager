package com.aktawind.monnoel.feature.gifts.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.ApplicationScope
import com.aktawind.monnoel.core.domain.gifts.DeleteIdeaUseCase
import com.aktawind.monnoel.core.domain.gifts.IdeaDraft
import com.aktawind.monnoel.core.domain.gifts.SaveIdeaUseCase
import com.aktawind.monnoel.core.domain.gifts.centsToInput
import com.aktawind.monnoel.core.domain.gifts.parseEuroToCents
import com.aktawind.monnoel.core.domain.repository.GiftCategoryRepository
import com.aktawind.monnoel.core.domain.repository.GiftIdeaRepository
import com.aktawind.monnoel.core.domain.repository.PersonRepository
import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import com.aktawind.monnoel.core.model.GiftCategory
import com.aktawind.monnoel.core.model.Person
import com.aktawind.monnoel.core.ui.UiMessage
import com.aktawind.monnoel.core.ui.toUiMessage
import com.aktawind.monnoel.feature.gifts.navigation.IdeaEditDestination
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

data class IdeaForm(
    val title: String = "",
    val description: String = "",
    val photoPath: String? = null,
    val price: String = "",
    val url: String = "",
    val shop: String = "",
    val notes: String = "",
    val categoryId: String? = null,
    val personIds: Set<String> = emptySet(),
)

data class IdeaEditUiState(
    val isNew: Boolean = true,
    val loading: Boolean = true,
    val form: IdeaForm = IdeaForm(),
    val persons: List<Person> = emptyList(),
    val categories: List<GiftCategory> = emptyList(),
    val busy: Boolean = false,
    val message: UiMessage? = null,
    val done: Boolean = false,
)

@HiltViewModel
class IdeaEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val ideas: GiftIdeaRepository,
    persons: PersonRepository,
    categories: GiftCategoryRepository,
    private val photos: PhotoRepository,
    private val saveIdea: SaveIdeaUseCase,
    private val deleteIdea: DeleteIdeaUseCase,
    @ApplicationScope appScope: CoroutineScope,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<IdeaEditDestination>()
    private val state = MutableStateFlow(IdeaEditUiState(isNew = route.ideaId == null))
    private val drafts = PhotoDrafts(photos, appScope)

    val uiState: StateFlow<IdeaEditUiState> = combine(state, persons.observePersons(), categories.observeCategories()) {
            s,
            p,
            c,
        ->
        s.copy(persons = p, categories = c)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), state.value)

    init {
        viewModelScope.launch {
            val idea = route.ideaId?.let { ideas.getIdea(it) }
            state.update {
                it.copy(
                    loading = false,
                    form = idea?.let { i ->
                        IdeaForm(
                            i.title, i.description, i.photoPath, centsToInput(i.estimatedPriceCents), i.url, i.shop,
                            i.notes, i.categoryId, i.personIds,
                        )
                    } ?: IdeaForm(personIds = setOfNotNull(route.personId)),
                )
            }
        }
    }

    fun update(transform: IdeaForm.() -> IdeaForm) = state.update { it.copy(form = it.form.transform()) }

    fun togglePerson(id: String) = update { copy(personIds = if (id in personIds) personIds - id else personIds + id) }

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
        val form = state.value.form
        if (form.price.isNotBlank() && parseEuroToCents(form.price) == null) return@launch
        state.update { it.copy(busy = true) }
        val result = saveIdea(
            route.ideaId,
            IdeaDraft(
                title = form.title,
                description = form.description,
                photoPath = form.photoPath,
                estimatedPriceCents = parseEuroToCents(form.price),
                url = form.url,
                shop = form.shop,
                notes = form.notes,
                categoryId = form.categoryId,
                personIds = form.personIds,
            ),
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
        val id = route.ideaId ?: return@launch
        when (val result = deleteIdea(id)) {
            is AppResult.Success -> state.update { it.copy(done = true) }
            is AppResult.Failure -> state.update { it.copy(message = result.error.toUiMessage()) }
        }
    }

    fun messageShown() = state.update { it.copy(message = null) }

    override fun onCleared() {
        drafts.discard()
    }
}

package com.aktawind.monnoel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.ApplicationScope
import com.aktawind.monnoel.core.domain.edition.ObserveEditionsUseCase
import com.aktawind.monnoel.core.domain.gifts.IdeaDraft
import com.aktawind.monnoel.core.domain.gifts.PrepareSharedIdeaUseCase
import com.aktawind.monnoel.core.domain.gifts.SaveIdeaUseCase
import com.aktawind.monnoel.core.domain.gifts.SharedIdea
import com.aktawind.monnoel.core.domain.gifts.parseSharedText
import com.aktawind.monnoel.core.domain.repository.PersonRepository
import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import com.aktawind.monnoel.core.domain.repository.UserPreferencesRepository
import com.aktawind.monnoel.core.model.Person
import com.aktawind.monnoel.core.model.ThemePreference
import com.aktawind.monnoel.core.ui.QuickIdeaInput
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MainUiState {
    data object Loading : MainUiState

    data class Ready(val theme: ThemePreference, val hasEditions: Boolean) : MainUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    preferences: UserPreferencesRepository,
    observeEditions: ObserveEditionsUseCase,
    private val saveIdea: SaveIdeaUseCase,
    personRepository: PersonRepository,
    private val prepareSharedIdea: PrepareSharedIdeaUseCase,
    private val photos: PhotoRepository,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {
    val persons: StateFlow<List<Person>> =
        personRepository.observePersons().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<MainUiState> = combine(preferences.theme, observeEditions()) { theme, editions ->
        MainUiState.Ready(theme = theme, hasEditions = editions.editions.isNotEmpty())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState.Loading)

    /** Idée reçue par le partage Android, en attente de confirmation. */
    private val _sharedIdea = MutableStateFlow<SharedIdea?>(null)
    val sharedIdea: StateFlow<SharedIdea?> = _sharedIdea.asStateFlow()

    /** Lecture de la page partagée en cours (titre, photo, prix). */
    private val _sharedLoading = MutableStateFlow(false)
    val sharedLoading: StateFlow<Boolean> = _sharedLoading.asStateFlow()
    private var preparing: Job? = null

    private val _ideaSaved = MutableStateFlow(false)
    val ideaSaved: StateFlow<Boolean> = _ideaSaved.asStateFlow()

    fun onShared(text: String?, subject: String?) {
        val shared = parseSharedText(text, subject) ?: return
        discardSharedPhoto()
        preparing?.cancel()
        _sharedIdea.value = null
        _sharedLoading.value = true
        preparing = viewModelScope.launch {
            val prepared = prepareSharedIdea(shared)
            _sharedLoading.value = false
            _sharedIdea.value = prepared
        }
        pendingRaw = shared
    }

    private var pendingRaw: SharedIdea? = null

    /** « Passer » : on n'attend pas la page, on garde le texte partagé. */
    fun skipSharedLoading() {
        preparing?.cancel()
        _sharedLoading.value = false
        _sharedIdea.value = pendingRaw
    }

    fun saveSharedIdea(input: QuickIdeaInput) {
        val shared = _sharedIdea.value ?: return
        _sharedIdea.value = null
        viewModelScope.launch {
            val draft = IdeaDraft(
                title = input.title,
                url = input.url,
                estimatedPriceCents = input.priceCents,
                shop = shared.shop,
                photoPath = shared.photoPath,
                personIds = input.personIds,
            )
            if (saveIdea(null, draft) is AppResult.Success) _ideaSaved.value = true
        }
    }

    fun dismissSharedIdea() {
        discardSharedPhoto()
        _sharedIdea.value = null
    }

    private fun discardSharedPhoto() {
        val photo = _sharedIdea.value?.photoPath ?: return
        appScope.launch { photos.deletePhoto(photo) }
    }

    fun ideaSavedShown() {
        _ideaSaved.value = false
    }
}

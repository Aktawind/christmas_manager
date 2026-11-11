package com.aktawind.monnoel.feature.gifts.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.ApplicationScope
import com.aktawind.monnoel.core.domain.edition.ObserveEditionsUseCase
import com.aktawind.monnoel.core.domain.gifts.DeleteGiftUseCase
import com.aktawind.monnoel.core.domain.gifts.GiftDraft
import com.aktawind.monnoel.core.domain.gifts.SaveGiftUseCase
import com.aktawind.monnoel.core.domain.gifts.centsToInput
import com.aktawind.monnoel.core.domain.gifts.parseEuroToCents
import com.aktawind.monnoel.core.domain.repository.EditionRepository
import com.aktawind.monnoel.core.domain.repository.GiftCategoryRepository
import com.aktawind.monnoel.core.domain.repository.GiftIdeaRepository
import com.aktawind.monnoel.core.domain.repository.PersonRepository
import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import com.aktawind.monnoel.core.domain.repository.PlannedGiftRepository
import com.aktawind.monnoel.core.model.GiftCategory
import com.aktawind.monnoel.core.model.GiftIdea
import com.aktawind.monnoel.core.model.GiftStatus
import com.aktawind.monnoel.core.model.Person
import com.aktawind.monnoel.core.ui.UiMessage
import com.aktawind.monnoel.core.ui.toUiMessage
import com.aktawind.monnoel.feature.gifts.navigation.GiftEditDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
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

data class GiftForm(
    val personId: String? = null,
    val title: String = "",
    val description: String = "",
    val photoPath: String? = null,
    val price: String = "",
    val purchaseDate: LocalDate? = null,
    val shop: String = "",
    val orderUrl: String = "",
    val trackingNumber: String = "",
    val notes: String = "",
    val categoryId: String? = null,
    val status: GiftStatus = GiftStatus.TO_BUY,
    val ideaId: String? = null,
)

data class GiftEditUiState(
    val isNew: Boolean = true,
    val loading: Boolean = true,
    val readOnly: Boolean = false,
    val year: Int? = null,
    val form: GiftForm = GiftForm(),
    val persons: List<Person> = emptyList(),
    val categories: List<GiftCategory> = emptyList(),
    val ideas: List<GiftIdea> = emptyList(),
    val busy: Boolean = false,
    val message: UiMessage? = null,
    val done: Boolean = false,
)

@HiltViewModel
class GiftEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gifts: PlannedGiftRepository,
    private val editions: EditionRepository,
    private val observeEditions: ObserveEditionsUseCase,
    persons: PersonRepository,
    categories: GiftCategoryRepository,
    ideas: GiftIdeaRepository,
    private val photos: PhotoRepository,
    private val saveGift: SaveGiftUseCase,
    private val deleteGift: DeleteGiftUseCase,
    @ApplicationScope appScope: CoroutineScope,
) : ViewModel() {
    private val drafts = PhotoDrafts(photos, appScope)
    private val route = savedStateHandle.toRoute<GiftEditDestination>()
    private val state = MutableStateFlow(GiftEditUiState(isNew = route.giftId == null))
    private var editionId: String? = null

    val uiState: StateFlow<GiftEditUiState> = combine(
        state,
        persons.observePersons(),
        categories.observeCategories(),
        ideas.observeIdeas(),
    ) { s, p, c, i -> s.copy(persons = p, categories = c, ideas = i) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), state.value)

    init {
        viewModelScope.launch {
            val existing = route.giftId?.let { gifts.getGift(it) }
            val edition = existing?.let { editions.getById(it.editionId) } ?: observeEditions().first().current
            editionId = edition?.id
            state.update {
                it.copy(
                    loading = false,
                    readOnly = edition?.isReadOnly == true,
                    year = edition?.year,
                    form = existing?.let { g ->
                        GiftForm(
                            g.personId, g.title, g.description, g.photoPath, centsToInput(g.priceCents), g.purchaseDate,
                            g.shop, g.orderUrl, g.trackingNumber, g.notes, g.categoryId, g.status, g.ideaId,
                        )
                    } ?: GiftForm(personId = route.personId),
                )
            }
        }
    }

    fun update(transform: GiftForm.() -> GiftForm) = state.update { it.copy(form = it.form.transform()) }

    /** Pré-remplit le cadeau à partir d'une idée du catalogue. */
    fun useIdea(idea: GiftIdea) = viewModelScope.launch {
        val photo = idea.photoPath?.let { photos.copyPhoto(it) }
        photo?.let(drafts::added)
        update {
            copy(
                title = idea.title,
                description = idea.description,
                photoPath = photo ?: photoPath,
                price = centsToInput(idea.estimatedPriceCents),
                shop = idea.shop,
                orderUrl = idea.url,
                notes = idea.notes,
                categoryId = idea.categoryId,
                ideaId = idea.id,
                personId = personId ?: idea.personIds.singleOrNull(),
            )
        }
    }

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
        val id = editionId ?: return@launch
        val form = state.value.form
        if (form.price.isNotBlank() && parseEuroToCents(form.price) == null) return@launch
        state.update { it.copy(busy = true) }
        val result = saveGift(
            id = route.giftId,
            editionId = id,
            draft = GiftDraft(
                personId = form.personId,
                title = form.title,
                description = form.description,
                photoPath = form.photoPath,
                priceCents = parseEuroToCents(form.price),
                purchaseDate = form.purchaseDate,
                shop = form.shop,
                orderUrl = form.orderUrl,
                trackingNumber = form.trackingNumber,
                notes = form.notes,
                categoryId = form.categoryId,
                status = form.status,
                ideaId = form.ideaId,
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
        val id = route.giftId ?: return@launch
        when (val result = deleteGift(id)) {
            is AppResult.Success -> state.update { it.copy(done = true) }
            is AppResult.Failure -> state.update { it.copy(message = result.error.toUiMessage()) }
        }
    }

    fun messageShown() = state.update { it.copy(message = null) }

    override fun onCleared() {
        drafts.discard()
    }
}

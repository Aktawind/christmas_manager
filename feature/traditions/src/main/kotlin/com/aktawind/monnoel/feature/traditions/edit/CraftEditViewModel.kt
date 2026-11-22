package com.aktawind.monnoel.feature.traditions.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.ApplicationScope
import com.aktawind.monnoel.core.common.IdGenerator
import com.aktawind.monnoel.core.domain.edition.ObserveEditionsUseCase
import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import com.aktawind.monnoel.core.domain.traditions.CraftDraft
import com.aktawind.monnoel.core.domain.traditions.CraftRepository
import com.aktawind.monnoel.core.domain.traditions.DeleteCraftSupplyUseCase
import com.aktawind.monnoel.core.domain.traditions.DeleteCraftUseCase
import com.aktawind.monnoel.core.domain.traditions.SaveCraftSupplyUseCase
import com.aktawind.monnoel.core.domain.traditions.SaveCraftUseCase
import com.aktawind.monnoel.core.domain.traditions.SetCraftResultPhotoUseCase
import com.aktawind.monnoel.core.domain.traditions.SetCraftStatusUseCase
import com.aktawind.monnoel.core.domain.traditions.SetCraftSupplyBoughtUseCase
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.model.CraftStatus
import com.aktawind.monnoel.core.ui.UiMessage
import com.aktawind.monnoel.core.ui.toUiMessage
import com.aktawind.monnoel.feature.traditions.PhotoDrafts
import com.aktawind.monnoel.feature.traditions.navigation.CraftEditDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Une fourniture en cours de saisie (l'identifiant est créé dès l'ajout). */
data class SupplyForm(val id: String, val name: String, val priceCents: Long?)

data class CraftEditUiState(
    val isNew: Boolean = true,
    val loading: Boolean = true,
    val form: CraftDraft = CraftDraft(""),
    val supplies: List<SupplyForm> = emptyList(),
    /** Le Noël affiché (null s'il n'y en a pas). */
    val edition: ChristmasEdition? = null,
    val status: CraftStatus? = null,
    val resultPhotoPath: String? = null,
    /** Fournitures achetées pour ce Noël. */
    val bought: Set<String> = emptySet(),
    /** Années où il a été terminé. */
    val history: List<Int> = emptyList(),
    val busy: Boolean = false,
    val message: UiMessage? = null,
    val done: Boolean = false,
) {
    val canEditYear: Boolean get() = edition != null && !edition.isReadOnly
}

@HiltViewModel
class CraftEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val crafts: CraftRepository,
    private val observeEditions: ObserveEditionsUseCase,
    private val photos: PhotoRepository,
    private val ids: IdGenerator,
    private val saveCraft: SaveCraftUseCase,
    private val deleteCraft: DeleteCraftUseCase,
    private val saveSupply: SaveCraftSupplyUseCase,
    private val deleteSupply: DeleteCraftSupplyUseCase,
    private val setStatus: SetCraftStatusUseCase,
    private val setResultPhoto: SetCraftResultPhotoUseCase,
    private val setBought: SetCraftSupplyBoughtUseCase,
    @ApplicationScope appScope: CoroutineScope,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<CraftEditDestination>()
    private val state = MutableStateFlow(CraftEditUiState(isNew = route.craftId == null))
    private val drafts = PhotoDrafts(photos, appScope)
    private var originalBought: Set<String> = emptySet()
    private var originalSupplies: Set<String> = emptySet()

    val uiState: StateFlow<CraftEditUiState> = state

    init {
        viewModelScope.launch {
            val editions = observeEditions().first()
            val edition = editions.current
            val craft = route.craftId?.let { crafts.getCraft(it) }
            val supplies = route.craftId?.let { crafts.getSupplies(it) }.orEmpty()
            val year = if (craft != null && edition != null) crafts.getYear(edition.id, craft.id) else null
            val bought = edition?.let { e -> crafts.observeBought().first().filter { it.first == e.id }.map { it.second }.toSet() }
                .orEmpty()
                .intersect(supplies.map { it.id }.toSet())
            val yearOf = editions.editions.associate { it.id to it.year }
            val history = route.craftId?.let { crafts.getYearsOf(it) }.orEmpty()
                .filter { it.status == CraftStatus.DONE }
                .mapNotNull { yearOf[it.editionId] }
                .sortedDescending()
            originalBought = bought
            originalSupplies = supplies.map { it.id }.toSet()
            state.update {
                it.copy(
                    loading = false,
                    edition = edition,
                    form = craft?.let { c -> CraftDraft(c.title, c.category, c.photoPath, c.tutorialUrl, c.notes) } ?: CraftDraft(""),
                    supplies = supplies.map { s -> SupplyForm(s.id, s.name, s.priceCents) },
                    status = year?.status,
                    resultPhotoPath = year?.resultPhotoPath,
                    bought = bought,
                    history = history,
                )
            }
        }
    }

    fun update(transform: CraftDraft.() -> CraftDraft) = state.update { it.copy(form = it.form.transform()) }

    fun setStatus(status: CraftStatus?) = state.update { it.copy(status = status) }

    fun addSupply(name: String, priceCents: Long?) {
        if (name.isBlank()) return
        state.update { it.copy(supplies = it.supplies + SupplyForm(ids.newId(), name.trim(), priceCents)) }
    }

    fun removeSupply(id: String) = state.update { it.copy(supplies = it.supplies.filterNot { s -> s.id == id }, bought = it.bought - id) }

    fun toggleBought(id: String) = state.update { it.copy(bought = if (id in it.bought) it.bought - id else it.bought + id) }

    fun importPhoto(uri: String) = import(uri) { path -> update { copy(photoPath = path) } }

    fun importResultPhoto(uri: String) = import(uri) { path -> state.update { it.copy(resultPhotoPath = path) } }

    fun removeResultPhoto() = state.update { it.copy(resultPhotoPath = null) }

    private fun import(uri: String, onImported: (String) -> Unit) = viewModelScope.launch {
        when (val result = photos.importPhoto(uri)) {
            is AppResult.Success -> {
                drafts.added(result.value)
                onImported(result.value)
            }
            is AppResult.Failure -> state.update { it.copy(message = UiMessage(com.aktawind.monnoel.core.ui.R.string.photo_error)) }
        }
    }

    fun save() = viewModelScope.launch {
        val current = state.value
        state.update { it.copy(busy = true) }
        val result = saveCraft(route.craftId, current.form)
        if (result is AppResult.Failure) {
            state.update { it.copy(busy = false, message = result.error.toUiMessage()) }
            return@launch
        }
        val craft = (result as AppResult.Success).value
        val kept = current.supplies.map { it.id }.toSet()
        (originalSupplies - kept).forEach { deleteSupply(it) }
        current.supplies.forEach { saveSupply(it.id, craft.id, it.name, it.priceCents) }
        val edition = current.edition
        if (edition != null && current.canEditYear) {
            setStatus(edition.id, craft.id, current.status)
            if (current.status != null) setResultPhoto(edition.id, craft.id, current.resultPhotoPath)
            val bought = current.bought.intersect(kept)
            (bought - originalBought).forEach { setBought(edition.id, it, true) }
            (originalBought.intersect(kept) - bought).forEach { setBought(edition.id, it, false) }
        }
        drafts.committed(current.form.photoPath, current.resultPhotoPath.takeIf { current.status != null && current.canEditYear })
        state.update { it.copy(busy = false, done = true) }
    }

    fun delete() = viewModelScope.launch {
        val id = route.craftId ?: return@launch
        when (val result = deleteCraft(id)) {
            is AppResult.Success -> state.update { it.copy(done = true) }
            is AppResult.Failure -> state.update { it.copy(message = result.error.toUiMessage()) }
        }
    }

    fun messageShown() = state.update { it.copy(message = null) }

    override fun onCleared() {
        drafts.discard()
    }
}

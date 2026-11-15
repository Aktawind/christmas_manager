package com.aktawind.monnoel.feature.cooking.meal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.edition.ObserveEditionsUseCase
import com.aktawind.monnoel.core.domain.gifts.centsToInput
import com.aktawind.monnoel.core.domain.gifts.parseEuroToCents
import com.aktawind.monnoel.core.domain.meals.DeleteMealUseCase
import com.aktawind.monnoel.core.domain.meals.MealDraft
import com.aktawind.monnoel.core.domain.meals.MealRepository
import com.aktawind.monnoel.core.domain.meals.SaveMealUseCase
import com.aktawind.monnoel.core.domain.repository.PersonRepository
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.model.MealType
import com.aktawind.monnoel.core.model.Person
import com.aktawind.monnoel.core.ui.UiMessage
import com.aktawind.monnoel.core.ui.toUiMessage
import com.aktawind.monnoel.feature.cooking.navigation.MealEditDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MealForm(
    val name: String = "",
    val type: MealType = MealType.CHRISTMAS_EVE,
    val date: LocalDate? = null,
    val place: String = "",
    val personIds: Set<String> = emptySet(),
    val guests: List<String> = emptyList(),
    val cost: String = "",
    val notes: String = "",
)

data class MealEditUiState(
    val isNew: Boolean = true,
    val loading: Boolean = true,
    val edition: ChristmasEdition? = null,
    val form: MealForm = MealForm(),
    val persons: List<Person> = emptyList(),
    val busy: Boolean = false,
    val message: UiMessage? = null,
    /** Repas enregistré (id) ou supprimé (chaîne vide). */
    val savedId: String? = null,
) {
    val readOnly: Boolean get() = edition?.isReadOnly == true
    val costValid: Boolean get() = form.cost.isBlank() || parseEuroToCents(form.cost) != null
}

@HiltViewModel
class MealEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val meals: MealRepository,
    private val observeEditions: ObserveEditionsUseCase,
    persons: PersonRepository,
    private val saveMeal: SaveMealUseCase,
    private val deleteMeal: DeleteMealUseCase,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<MealEditDestination>()
    private val state = MutableStateFlow(MealEditUiState(isNew = route.mealId == null))

    val uiState: StateFlow<MealEditUiState> = combine(state, persons.observePersons()) { s, p -> s.copy(persons = p) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), state.value)

    init {
        viewModelScope.launch {
            val editions = observeEditions().first()
            val meal = route.mealId?.let { meals.getMeal(it) }
            val edition = meal?.let { m -> editions.editions.firstOrNull { it.id == m.editionId } } ?: editions.current
            state.update {
                it.copy(
                    loading = false,
                    edition = edition,
                    form = meal?.let { m ->
                        MealForm(m.name, m.type, m.date, m.place, m.personIds, m.guests, centsToInput(m.estimatedCostCents), m.notes)
                    } ?: suggestedFor(MealType.CHRISTMAS_EVE, edition?.year, MealForm()),
                )
            }
        }
    }

    fun update(transform: MealForm.() -> MealForm) = state.update { it.copy(form = it.form.transform()) }

    /** Choisir le type propose un nom et une date quand ils ne sont pas encore personnalisés. */
    fun selectType(type: MealType) = state.update { s ->
        val year = s.edition?.year
        val previous = suggestedFor(s.form.type, year, MealForm())
        val nameUntouched = s.form.name.isBlank() || s.form.name == previous.name
        val dateUntouched = s.form.date == null || s.form.date == previous.date
        val proposal = suggestedFor(type, year, MealForm())
        s.copy(
            form = s.form.copy(
                type = type,
                name = if (nameUntouched) proposal.name else s.form.name,
                date = if (dateUntouched) proposal.date else s.form.date,
            ),
        )
    }

    private fun suggestedFor(type: MealType, year: Int?, form: MealForm): MealForm {
        val date = when (type) {
            MealType.CHRISTMAS_EVE -> year?.let { LocalDate.of(it, 12, 24) }
            MealType.CHRISTMAS_DAY -> year?.let { LocalDate.of(it, 12, 25) }
            else -> null
        }
        return form.copy(type = type, name = defaultNames[type].orEmpty(), date = date)
    }

    fun togglePerson(id: String) = update { copy(personIds = if (id in personIds) personIds - id else personIds + id) }

    fun addGuest(name: String) {
        val clean = name.trim()
        if (clean.isNotEmpty()) update { copy(guests = (guests + clean).distinct()) }
    }

    fun removeGuest(name: String) = update { copy(guests = guests - name) }

    fun save() = viewModelScope.launch {
        val current = state.value
        val edition = current.edition ?: return@launch
        if (!current.costValid) return@launch
        val form = current.form
        state.update { it.copy(busy = true) }
        val draft =
            MealDraft(form.name, form.type, form.date, form.place, form.personIds, form.guests, parseEuroToCents(form.cost), form.notes)
        val result = saveMeal(route.mealId, edition.id, draft)
        state.update {
            when (result) {
                is AppResult.Success -> it.copy(busy = false, savedId = result.value.id)
                is AppResult.Failure -> it.copy(busy = false, message = result.error.toUiMessage())
            }
        }
    }

    fun delete() = viewModelScope.launch {
        val id = route.mealId ?: return@launch
        when (val result = deleteMeal(id)) {
            is AppResult.Success -> state.update { it.copy(savedId = "") }
            is AppResult.Failure -> state.update { it.copy(message = result.error.toUiMessage()) }
        }
    }

    fun messageShown() = state.update { it.copy(message = null) }

    companion object {
        /** Noms proposés selon le type (modifiables). */
        val defaultNames = mapOf(
            MealType.CHRISTMAS_EVE to "Réveillon",
            MealType.CHRISTMAS_DAY to "Déjeuner de Noël",
            MealType.BRUNCH to "Brunch",
            MealType.SNACK to "Goûter",
            MealType.FRIENDS to "Repas entre amis",
        )
    }
}

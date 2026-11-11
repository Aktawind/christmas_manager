package com.aktawind.monnoel.core.domain.edition

import com.aktawind.monnoel.core.common.IdGenerator
import com.aktawind.monnoel.core.common.TimeProvider
import com.aktawind.monnoel.core.domain.repository.EditionRepository
import com.aktawind.monnoel.core.domain.repository.UserPreferencesRepository
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.model.ThemePreference
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeEditionRepository : EditionRepository {
    val items = MutableStateFlow<List<ChristmasEdition>>(emptyList())

    override fun observeEditions(): Flow<List<ChristmasEdition>> = items.map { list ->
        list.sortedByDescending { it.year }
    }

    override suspend fun getById(id: String) = items.value.firstOrNull { it.id == id }

    override suspend fun getByYear(year: Int) = items.value.firstOrNull { it.year == year }

    override suspend fun insert(edition: ChristmasEdition) {
        items.value = items.value + edition
    }

    override suspend fun update(edition: ChristmasEdition) {
        items.value = items.value.map { if (it.id == edition.id) edition else it }
    }

    override suspend fun delete(id: String) {
        items.value = items.value.filterNot { it.id == id }
    }
}

class FakePreferences : UserPreferencesRepository {
    override val theme = MutableStateFlow(ThemePreference.SYSTEM)
    override val selectedEditionId = MutableStateFlow<String?>(null)

    override suspend fun setTheme(theme: ThemePreference) {
        this.theme.value = theme
    }

    override suspend fun setSelectedEditionId(id: String?) {
        selectedEditionId.value = id
    }
}

class FixedTime(private val instant: Instant = Instant.parse("2026-11-11T10:00:00Z")) : TimeProvider {
    override fun now() = instant

    override fun today(): LocalDate = LocalDate.of(2026, 11, 11)
}

class SequentialIds : IdGenerator {
    private var next = 0

    override fun newId() = "id-${next++}"
}

package com.aktawind.monnoel.core.domain.countdown

import com.aktawind.monnoel.core.common.AppError
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.IdGenerator
import com.aktawind.monnoel.core.model.SpecialDate
import java.time.MonthDay
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

interface SpecialDateRepository {
    fun observeDates(): Flow<List<SpecialDate>>

    suspend fun saveDate(date: SpecialDate)

    suspend fun deleteDate(id: String)
}

/** Mois proposés : novembre, décembre, janvier. */
val specialDateMonths = listOf(11, 12, 1)

class SaveSpecialDateUseCase @Inject constructor(private val dates: SpecialDateRepository, private val ids: IdGenerator) {
    suspend operator fun invoke(id: String?, month: Int, day: Int, label: String, emoji: String): AppResult<SpecialDate> {
        val valid = month in specialDateMonths && runCatching { MonthDay.of(month, day) }.isSuccess
        if (!valid || label.isBlank()) return AppResult.Failure(AppError.InvalidInput)
        val date = SpecialDate(id ?: ids.newId(), month, day, label.trim(), emoji.ifBlank { "⭐" })
        dates.saveDate(date)
        return AppResult.Success(date)
    }
}

class DeleteSpecialDateUseCase @Inject constructor(private val dates: SpecialDateRepository) {
    suspend operator fun invoke(id: String) = dates.deleteDate(id)
}

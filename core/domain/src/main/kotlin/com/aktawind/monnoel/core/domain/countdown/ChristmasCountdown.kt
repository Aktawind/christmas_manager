package com.aktawind.monnoel.core.domain.countdown

import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit

/** Ce que l'accueil affiche au sujet de Noël, selon la date du jour et l'année affichée. */
sealed interface CountdownState {
    /** Noël approche. [adventDay] vaut 1 à 23 pendant l'Avent (du 1er au 23 décembre). */
    data class DaysLeft(val days: Long, val adventDay: Int?) : CountdownState

    /** Le 24 décembre. */
    data object ChristmasEve : CountdownState

    /** Le 25 décembre. */
    data object ChristmasDay : CountdownState

    /** Le Noël de cette année est passé : place aux souvenirs. */
    data class Past(val year: Int) : CountdownState
}

fun christmasCountdown(today: LocalDate, editionYear: Int): CountdownState {
    val christmas = LocalDate.of(editionYear, Month.DECEMBER, 25)
    return when {
        today.isEqual(christmas) -> CountdownState.ChristmasDay
        today.isEqual(christmas.minusDays(1)) -> CountdownState.ChristmasEve
        today.isAfter(christmas) -> CountdownState.Past(editionYear)
        else -> {
            val days = ChronoUnit.DAYS.between(today, christmas)
            val inAdvent = today.year == editionYear && today.month == Month.DECEMBER
            CountdownState.DaysLeft(days = days, adventDay = if (inAdvent) today.dayOfMonth else null)
        }
    }
}

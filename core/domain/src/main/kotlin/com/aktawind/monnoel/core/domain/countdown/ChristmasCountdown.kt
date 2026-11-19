package com.aktawind.monnoel.core.domain.countdown

import com.aktawind.monnoel.core.model.SpecialDate
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

/** Étapes datées « naturellement » sur le chemin vers Noël (jamais des échéances). */
enum class PathStepKind { ADVENT_START, CHRISTMAS_EVE, CHRISTMAS, CUSTOM }

data class PathStep(
    val kind: PathStepKind,
    val date: LocalDate,
    val passed: Boolean,
    val isNext: Boolean,
    /** Date personnelle : son nom et son emoji. */
    val label: String? = null,
    val emoji: String? = null,
)

/**
 * Le chemin vers Noël : 1er décembre, 24, 25, et les dates qui comptent pour la famille
 * (novembre et décembre de l'année du Noël, janvier de l'année suivante), dans l'ordre.
 */
fun christmasPath(today: LocalDate, editionYear: Int, specials: List<SpecialDate> = emptyList()): List<PathStep> {
    val fixed = listOf(
        PathStep(PathStepKind.ADVENT_START, LocalDate.of(editionYear, Month.DECEMBER, 1), false, false),
        PathStep(PathStepKind.CHRISTMAS_EVE, LocalDate.of(editionYear, Month.DECEMBER, 24), false, false),
        PathStep(PathStepKind.CHRISTMAS, LocalDate.of(editionYear, Month.DECEMBER, 25), false, false),
    )
    val custom = specials.mapNotNull { s ->
        val year = if (s.month == 1) editionYear + 1 else editionYear
        runCatching { LocalDate.of(year, s.month, s.day) }.getOrNull()?.let {
            PathStep(PathStepKind.CUSTOM, it, false, false, s.label, s.emoji)
        }
    }
    val steps = (fixed + custom).sortedBy { it.date }
    val next = steps.firstOrNull { !it.date.isBefore(today) }
    return steps.map { it.copy(passed = it.date.isBefore(today), isNext = it === next) }
}

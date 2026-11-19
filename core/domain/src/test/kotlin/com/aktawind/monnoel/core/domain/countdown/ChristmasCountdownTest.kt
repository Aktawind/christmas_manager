package com.aktawind.monnoel.core.domain.countdown

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ChristmasCountdownTest {
    @Test
    fun `compte les jours avant Noel`() {
        assertEquals(
            CountdownState.DaysLeft(days = 44, adventDay = null),
            christmasCountdown(LocalDate.of(2026, 11, 11), 2026),
        )
    }

    @Test
    fun `indique le jour de l'Avent en decembre`() {
        assertEquals(
            CountdownState.DaysLeft(days = 24, adventDay = 1),
            christmasCountdown(LocalDate.of(2026, 12, 1), 2026),
        )
    }

    @Test
    fun `reveillon et jour de Noel`() {
        assertEquals(CountdownState.ChristmasEve, christmasCountdown(LocalDate.of(2026, 12, 24), 2026))
        assertEquals(CountdownState.ChristmasDay, christmasCountdown(LocalDate.of(2026, 12, 25), 2026))
    }

    @Test
    fun `noel passe`() {
        assertEquals(CountdownState.Past(2025), christmasCountdown(LocalDate.of(2026, 11, 11), 2025))
        assertEquals(CountdownState.Past(2026), christmasCountdown(LocalDate.of(2026, 12, 26), 2026))
    }

    @Test
    fun `annee future sans jour de l'Avent`() {
        val state = christmasCountdown(LocalDate.of(2026, 12, 5), 2027)
        assertEquals(CountdownState.DaysLeft(days = 385, adventDay = null), state)
    }

    @Test
    fun `chemin vers Noel`() {
        val before = christmasPath(LocalDate.of(2026, 11, 15), 2026)
        assertEquals(listOf(true, false, false), before.map { it.isNext })
        assertEquals(listOf(false, false, false), before.map { it.passed })
        val eve = christmasPath(LocalDate.of(2026, 12, 24), 2026)
        assertEquals(listOf(true, false, false), eve.map { it.passed })
        assertEquals(PathStepKind.CHRISTMAS_EVE, eve.single { it.isNext }.kind)
        assertEquals(0, christmasPath(LocalDate.of(2026, 12, 26), 2026).count { it.isNext })
    }

    @Test
    fun `les dates qui comptent prennent place dans le chemin`() {
        val specials = listOf(
            com.aktawind.monnoel.core.model.SpecialDate("a", 12, 13, "Sainte-Lucie", "🕯️"),
            com.aktawind.monnoel.core.model.SpecialDate("b", 1, 6, "Galette des rois", "👑"),
            com.aktawind.monnoel.core.model.SpecialDate("c", 11, 30, "Arrivée de Mamie", "🚗"),
        )
        val path = christmasPath(java.time.LocalDate.of(2026, 12, 2), 2026, specials)
        assertEquals(
            listOf("Arrivée de Mamie", null, "Sainte-Lucie", null, null, "Galette des rois"),
            path.map { it.label },
        )
        assertEquals(java.time.LocalDate.of(2027, 1, 6), path.last().date)
        // Le 2 décembre : le 30 novembre et le 1er décembre sont passés, la prochaine étape est la Sainte-Lucie.
        assertEquals("Sainte-Lucie", path.single { it.isNext }.label)
        assertEquals(2, path.count { it.passed })
    }
}

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
}

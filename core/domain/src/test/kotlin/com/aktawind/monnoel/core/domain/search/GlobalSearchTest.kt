package com.aktawind.monnoel.core.domain.search

import com.aktawind.monnoel.core.model.AdventBox
import com.aktawind.monnoel.core.model.AdventCalendar
import com.aktawind.monnoel.core.model.BoxStatus
import com.aktawind.monnoel.core.model.GiftIdea
import com.aktawind.monnoel.core.model.GiftStatus
import com.aktawind.monnoel.core.model.LinkType
import com.aktawind.monnoel.core.model.Person
import com.aktawind.monnoel.core.model.PlannedGift
import com.aktawind.monnoel.core.model.SurpriseType
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GlobalSearchTest {
    private val t = Instant.EPOCH
    private val data = SearchData(
        persons = listOf(Person("p", "Léa", "Martin", null, "licornes, dessin", "", t, t)),
        ideas = listOf(GiftIdea("i", "Coffret de feutres", "", null, 2_490, "", "Cultura", "", null, setOf("p"), t, t)),
        gifts = listOf(
            PlannedGift("g", "e", "p", "Léa Martin", null, "Livre de contes de Noël", "", null, 1_800, null, "", "", "", "", null, GiftStatus.TO_BUY, t, t),
        ),
        calendars = listOf(AdventCalendar("c", "e", "Calendrier de Tom", emptySet(), "", null, "Dinosaures", null, "", 24, t, t)),
        boxes = listOf(AdventBox("b", "c", 3, SurpriseType.TREAT, "Chocolat noël", null, null, "", "", BoxStatus.READY, "", null)),
        tasks = emptyList(),
        years = mapOf("e" to 2026),
    )

    @Test
    fun `sans accents et sans majuscules, regroupe par type`() {
        val results = searchAll("noel", data)
        assertEquals(setOf(LinkType.GIFT, LinkType.BOX), results.keys)
        assertEquals("Léa Martin · Noël 2026", results.getValue(LinkType.GIFT).single().subtitle)
        assertEquals("Calendrier de Tom · case 3 · Noël 2026", results.getValue(LinkType.BOX).single().subtitle)
    }

    @Test
    fun `tous les mots doivent correspondre`() {
        assertEquals(listOf("p"), searchAll("lea licornes", data)[LinkType.PERSON]?.map { it.id })
        assertTrue(searchAll("lea dinosaures", data)[LinkType.PERSON] == null)
        assertEquals("Calendrier de Tom", searchAll("dino", data).getValue(LinkType.CALENDAR).single().title)
        assertEquals("Léa Martin · Cultura", searchAll("feutres", data).getValue(LinkType.IDEA).single().subtitle)
        assertTrue(searchAll("   ", data).isEmpty())
    }
}

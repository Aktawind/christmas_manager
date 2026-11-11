package com.aktawind.monnoel.core.domain.gifts

import com.aktawind.monnoel.core.model.GiftStatus
import com.aktawind.monnoel.core.model.PersonBudget
import com.aktawind.monnoel.core.model.PlannedGift
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GiftBudgetTest {
    private fun gift(person: String?, price: Long?, status: GiftStatus) = PlannedGift(
        id = "$person-$price-$status", editionId = "e", personId = person, recipientName = "", ideaId = null,
        title = "t", description = "", photoPath = null, priceCents = price, purchaseDate = null, shop = "",
        orderUrl = "", trackingNumber = "", notes = "", categoryId = null, status = status,
        createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH,
    )

    @Test
    fun `prevu, depense et enveloppes`() {
        val gifts = listOf(
            gift("lea", 2_000, GiftStatus.TO_BUY),
            gift("lea", 3_000, GiftStatus.ORDERED),
            gift("tom", 1_500, GiftStatus.GIVEN),
            gift("tom", null, GiftStatus.TO_BUY),
        )
        val budget = computeGiftBudget(gifts, listOf(PersonBudget("e", "lea", 4_000), PersonBudget("e", "zoe", 2_000)))

        assertEquals(6_500, budget.plannedCents)
        assertEquals(4_500, budget.spentCents)
        assertEquals(6_000, budget.envelopesCents)

        val lea = budget.byPerson.getValue("lea")
        assertEquals(5_000, lea.plannedCents)
        assertEquals(3_000, lea.spentCents)
        assertTrue(lea.isOverEnvelope)
        assertEquals(1.25f, lea.envelopeRatio)

        val tom = budget.byPerson.getValue("tom")
        assertNull(tom.envelopeCents)
        assertFalse(tom.isOverEnvelope)
        assertEquals(2, tom.giftCount)

        val zoe = budget.byPerson.getValue("zoe")
        assertEquals(0, zoe.plannedCents)
        assertEquals(0f, zoe.envelopeRatio)
    }

    @Test
    fun `saisie des montants`() {
        assertEquals(1_250, parseEuroToCents("12,50"))
        assertEquals(1_250, parseEuroToCents("12.5 €"))
        assertEquals(123_400, parseEuroToCents("1 234"))
        assertNull(parseEuroToCents(""))
        assertNull(parseEuroToCents("abc"))
        assertNull(parseEuroToCents("-3"))
        assertEquals("12,50", centsToInput(1_250))
        assertEquals("12", centsToInput(1_200))
    }
}

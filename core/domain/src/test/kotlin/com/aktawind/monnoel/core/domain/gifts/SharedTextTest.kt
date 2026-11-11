package com.aktawind.monnoel.core.domain.gifts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SharedTextTest {
    @Test
    fun `texte et lien`() {
        assertEquals(
            SharedIdea("Super jouet en bois", "https://boutique.fr/jouet?id=3"),
            parseSharedText("Super jouet en bois https://boutique.fr/jouet?id=3", null),
        )
    }

    @Test
    fun `le sujet sert de titre`() {
        assertEquals(
            SharedIdea("Livre de contes", "https://x.fr/l"),
            parseSharedText("https://x.fr/l", "Livre de contes"),
        )
    }

    @Test
    fun `lien seul ou rien`() {
        assertEquals(SharedIdea("", "https://x.fr"), parseSharedText("https://x.fr", null))
        assertNull(parseSharedText("   ", null))
    }
}

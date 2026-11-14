package com.aktawind.monnoel.core.domain.gifts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LinkPreviewTest {
    @Test
    fun `balises open graph`() {
        val html = """
            <html><head>
            <title>Ignoré</title>
            <meta property="og:title" content="Coffret de feutres &amp; crayons" />
            <meta property="og:image" content="/img/coffret.jpg">
            <meta property="og:site_name" content="Cultura">
            <meta property="product:price:amount" content="24.90">
            <meta property="product:price:currency" content="EUR">
            </head></html>
        """.trimIndent()
        val preview = parseLinkPreview(html, "https://www.cultura.com/p/coffret.html")
        assertEquals("Coffret de feutres & crayons", preview.title)
        assertEquals("https://www.cultura.com/img/coffret.jpg", preview.imageUrl)
        assertEquals(2_490, preview.priceCents)
        assertEquals("Cultura", preview.siteName)
    }

    @Test
    fun `donnees produit json-ld et titre de secours`() {
        val html = """
            <html><head><title>Théière   en fonte – Boutique</title>
            <script type="application/ld+json">{"@type":"Product","image":["https:\/\/cdn.x.fr\/t.webp"],
            "offers":{"price":"35,00","priceCurrency":"EUR"}}</script></head></html>
        """.trimIndent()
        val preview = parseLinkPreview(html, "https://x.fr/theiere")
        assertEquals("Théière en fonte – Boutique", preview.title)
        assertEquals("https://cdn.x.fr/t.webp", preview.imageUrl)
        assertEquals(3_500, preview.priceCents)
    }

    @Test
    fun `prix dans une autre devise ignore, page vide`() {
        val html = """<meta property="og:price:amount" content="30"><meta property="og:price:currency" content="USD">"""
        assertNull(parseLinkPreview(html, "https://x.com").priceCents)
        val empty = parseLinkPreview("<html></html>", "https://x.com")
        assertNull(empty.title)
        assertNull(empty.imageUrl)
    }

    @Test
    fun `images de secours et passage en https`() {
        val itemprop = """<img class="cover" itemprop="image" src="http://cdn.x.fr/cover.jpg" alt="">"""
        assertEquals("https://cdn.x.fr/cover.jpg", parseLinkPreview(itemprop, "https://x.fr/p").imageUrl)
        val link = """<link rel="image_src" href="/img/a.png">"""
        assertEquals("https://x.fr/img/a.png", parseLinkPreview(link, "https://x.fr/p/1").imageUrl)
        val json = """{"@type":"Product","image":{"@type":"ImageObject","url":"https://x.fr/b.jpg"}}"""
        assertEquals("https://x.fr/b.jpg", parseLinkPreview(json, "https://x.fr").imageUrl)
        val amp = """<meta property="og:image" content="https://x.fr/i.jpg?w=1&amp;h=2">"""
        assertEquals("https://x.fr/i.jpg?w=1&h=2", parseLinkPreview(amp, "https://x.fr").imageUrl)
    }
}

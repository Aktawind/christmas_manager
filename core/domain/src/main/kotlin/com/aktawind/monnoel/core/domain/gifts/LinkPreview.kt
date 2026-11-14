package com.aktawind.monnoel.core.domain.gifts

import java.net.URI

/** Informations lues dans une page web (balises Open Graph, données produit). */
data class LinkPreview(val title: String?, val imageUrl: String?, val priceCents: Long?, val siteName: String?)

private val metaTag = Regex("""<meta\b[^>]*>""", RegexOption.IGNORE_CASE)
private val attr = Regex("""([a-zA-Z:_-]+)\s*=\s*("([^"]*)"|'([^']*)')""")
private val titleTag =
    Regex("""<title[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val jsonLdPrice = Regex(""""price"\s*:\s*"?([0-9]+(?:[.,][0-9]{1,2})?)"?""")
private val jsonLdCurrency = Regex(""""priceCurrency"\s*:\s*"([A-Z]{3})"""")
private val jsonLdImage = Regex(""""image"\s*:\s*(?:\[\s*)?"([^"]+)"""")
private val itempropPrice =
    Regex("""itemprop\s*=\s*["']price["'][^>]*content\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)

/**
 * Extrait titre, image, prix et nom du site d'une page HTML. Ne lève jamais d'exception :
 * ce qui n'est pas trouvé reste null.
 */
fun parseLinkPreview(html: String, pageUrl: String): LinkPreview {
    val metas = mutableMapOf<String, String>()
    metaTag.findAll(html).forEach { match ->
        val attrs = attr.findAll(match.value).associate { m ->
            m.groupValues[1].lowercase() to (m.groups[3]?.value ?: m.groups[4]?.value.orEmpty())
        }
        val key = (attrs["property"] ?: attrs["name"] ?: attrs["itemprop"])?.lowercase() ?: return@forEach
        val content = attrs["content"] ?: return@forEach
        if (content.isNotBlank() && key !in metas) metas[key] = decodeHtml(content).trim()
    }

    val title = metas["og:title"] ?: metas["twitter:title"]
        ?: titleTag.find(html)?.groupValues?.get(1)?.let { decodeHtml(it).replace(Regex("\\s+"), " ").trim() }
    val image = metas["og:image:secure_url"] ?: metas["og:image"] ?: metas["og:image:url"]
        ?: metas["twitter:image"] ?: metas["twitter:image:src"]
        ?: jsonLdImage.find(html)?.groupValues?.get(1)

    val currency = metas["product:price:currency"] ?: metas["og:price:currency"]
        ?: jsonLdCurrency.find(html)?.groupValues?.get(1)
    val rawPrice = metas["product:price:amount"] ?: metas["og:price:amount"] ?: metas["price"]
        ?: itempropPrice.find(html)?.groupValues?.get(1)
        ?: jsonLdPrice.find(html)?.groupValues?.get(1)
    val price = rawPrice?.takeIf { currency == null || currency.equals("EUR", ignoreCase = true) }
        ?.let { parseEuroToCents(it) }
        ?.takeIf { it > 0 }

    return LinkPreview(
        title = title?.takeIf { it.isNotBlank() }?.take(160),
        imageUrl = image?.let { resolveUrl(pageUrl, it.replace("\\/", "/")) },
        priceCents = price,
        siteName = metas["og:site_name"]?.takeIf { it.isNotBlank() },
    )
}

private fun resolveUrl(base: String, href: String): String? = runCatching {
    when {
        href.startsWith("//") -> "https:$href"
        else -> URI(base).resolve(href.trim().replace(" ", "%20")).toString()
    }
}.getOrNull()?.takeIf { it.startsWith("http") }

private val namedEntities = mapOf(
    "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
    "eacute" to "é", "egrave" to "è", "ecirc" to "ê", "agrave" to "à", "acirc" to "â", "ccedil" to "ç",
    "ocirc" to "ô", "ucirc" to "û", "ugrave" to "ù", "icirc" to "î", "iuml" to "ï", "euml" to "ë",
    "Eacute" to "É", "euro" to "€", "rsquo" to "’", "lsquo" to "‘", "laquo" to "«", "raquo" to "»",
    "hellip" to "…", "ndash" to "–", "mdash" to "—", "oelig" to "œ",
)
private val entity = Regex("""&(#x?[0-9a-fA-F]+|[a-zA-Z]+);""")

internal fun decodeHtml(text: String): String = entity.replace(text) { m ->
    val code = m.groupValues[1]
    when {
        code.startsWith("#x") || code.startsWith("#X") -> code.drop(2).toIntOrNull(16)?.let {
            String(Character.toChars(it))
        }
        code.startsWith("#") -> code.drop(1).toIntOrNull()?.let { String(Character.toChars(it)) }
        else -> namedEntities[code]
    } ?: m.value
}

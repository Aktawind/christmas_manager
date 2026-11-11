package com.aktawind.monnoel.core.domain.gifts

/** Idée reçue par le partage Android (titre et lien proposés). */
data class SharedIdea(val title: String, val url: String)

private val urlRegex = Regex("""https?://\S+""")

/**
 * Extrait un titre et un lien d'un texte partagé par un navigateur ou une application de
 * boutique (ex. « Super jouet https://boutique.fr/jouet »).
 */
fun parseSharedText(text: String?, subject: String?): SharedIdea? {
    val content = text.orEmpty().trim()
    val url = urlRegex.find(content)?.value?.trimEnd('.', ',', ')', ';').orEmpty()
    val rest = content.replace(urlRegex, " ").replace(Regex("\\s+"), " ").trim().trim('-', '–', ':', '|').trim()
    val title = subject?.trim()?.takeIf { it.isNotBlank() } ?: rest
    if (title.isBlank() && url.isBlank()) return null
    return SharedIdea(title = title.take(120), url = url)
}

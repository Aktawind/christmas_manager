package com.aktawind.monnoel.core.domain.gifts

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/** Conversion des montants saisis (« 12,50 », « 12.5 », « 12 € ») en centimes. */
fun parseEuroToCents(input: String): Long? {
    val cleaned = input.replace("€", "").replace(" ", "").replace(" ", "").replace(',', '.').trim()
    if (cleaned.isEmpty()) return null
    return cleaned.toBigDecimalOrNull()
        ?.takeIf { it >= BigDecimal.ZERO }
        ?.setScale(2, RoundingMode.HALF_UP)
        ?.movePointRight(2)
        ?.toLong()
}

/** « 1 234,50 € » */
fun formatEuro(cents: Long): String = NumberFormat.getCurrencyInstance(Locale.FRANCE).format(cents / 100.0)

/** Valeur à afficher dans un champ de saisie : « 12,5 » -> « 12,50 », « 12,00 » -> « 12 ». */
fun centsToInput(cents: Long?): String {
    if (cents == null) return ""
    val euros = cents / 100
    val rest = cents % 100
    return if (rest == 0L) euros.toString() else "%d,%02d".format(euros, rest)
}

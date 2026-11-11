package com.aktawind.monnoel.core.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy 'à' HH'h'mm", Locale.FRENCH)

/** « 11 novembre 2026 à 10h30 » */
fun Instant.formatDateTime(): String = dateTimeFormatter.format(atZone(ZoneId.systemDefault()))

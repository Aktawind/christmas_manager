package com.aktawind.monnoel.feature.cooking

internal fun String.withScheme(): String = if (startsWith("http://") || startsWith("https://")) this else "https://$this"

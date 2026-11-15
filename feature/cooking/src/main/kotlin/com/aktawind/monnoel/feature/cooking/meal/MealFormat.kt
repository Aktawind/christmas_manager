package com.aktawind.monnoel.feature.cooking.meal

import java.time.format.DateTimeFormatter
import java.util.Locale

/** « mercredi 24 décembre ». */
internal val mealDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)

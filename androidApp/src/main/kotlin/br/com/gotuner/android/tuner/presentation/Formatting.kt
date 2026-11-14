package br.com.gotuner.android.tuner.presentation

import java.util.Locale

/** Formats Hz and cents with one decimal using the device locale (e.g. "110,0" in Portuguese). */
internal fun Double.formatOneDecimal(): String = String.format(Locale.getDefault(), "%.1f", this)

/** Cents with an explicit sign for sharp values, e.g. "+3.2" or "-7.0". */
internal fun Double.formatSignedCents(): String = (if (this > 0) "+" else "") + formatOneDecimal()

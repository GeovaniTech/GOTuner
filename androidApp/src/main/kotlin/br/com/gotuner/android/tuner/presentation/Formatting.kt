package br.com.gotuner.android.tuner.presentation

import java.util.Locale

/** Formats Hz with one decimal using the device locale (e.g. "110,0" in Portuguese). */
internal fun Double.formatOneDecimal(): String = String.format(Locale.getDefault(), "%.1f", this)

package br.com.gotuner.tuner.domain.tuning

import br.com.gotuner.tuner.domain.model.*
import kotlin.math.*

class TuningCalculator(
    private val config: TuningConfig = TuningConfig(),
    val strings: List<GuitarString> = StandardTuning.strings
) {
    fun string(number: Int?): GuitarString? = strings.firstOrNull { it.number == number }

    /**
     * [previous] enables hysteresis: a string already reported in tune stays in tune
     * while it remains within [TuningConfig.releaseCents] of the same target.
     * [selected] is the string the user chose to tune; without it the closest string is the target.
     */
    fun calculate(frequency: Double, previous: TuningResult? = null, selected: GuitarString? = null): TuningResult? {
        if (!frequency.isFinite() || frequency <= 0) return null
        val target = selected ?: strings.minBy { abs(1200 * log2(frequency / it.frequency)) }
        val cents = 1200 * log2(frequency / target.frequency)
        if (abs(cents) > (if (selected != null) config.manualMaxDistance else config.maxTargetDistance)) return null
        val wasInTune = previous?.status == TuningStatus.IN_TUNE && previous.target == target
        val inTuneWindow = if (wasInTune) config.releaseCents else config.greenCents
        val inTune = abs(cents) <= inTuneWindow + 1e-9
        val status = when { inTune -> TuningStatus.IN_TUNE; cents < 0 -> TuningStatus.FLAT; else -> TuningStatus.SHARP }
        val color = when { inTune -> AccuracyColor.GREEN; abs(cents) <= config.yellowCents + 1e-9 -> AccuracyColor.YELLOW; else -> AccuracyColor.RED }
        return TuningResult(target, cents, status, color)
    }
}

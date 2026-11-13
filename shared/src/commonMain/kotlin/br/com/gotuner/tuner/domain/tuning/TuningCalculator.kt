package br.com.gotuner.tuner.domain.tuning

import br.com.gotuner.tuner.domain.model.*
import kotlin.math.*

class TuningCalculator(private val config: TuningConfig = TuningConfig()) {
    val strings = listOf(GuitarString(6,"E2",82.41), GuitarString(5,"A2",110.0), GuitarString(4,"D3",146.83), GuitarString(3,"G3",196.0), GuitarString(2,"B3",246.94), GuitarString(1,"E4",329.63))

    /**
     * [previous] enables hysteresis: a string already reported in tune stays in tune
     * while it remains within [TuningConfig.releaseCents] of the same target.
     */
    fun calculate(frequency: Double, previous: TuningResult? = null): TuningResult? {
        if (!frequency.isFinite() || frequency <= 0) return null
        val target = strings.minBy { abs(1200 * log2(frequency / it.frequency)) }
        val cents = 1200 * log2(frequency / target.frequency)
        if (abs(cents) > config.maxTargetDistance) return null
        val wasInTune = previous?.status == TuningStatus.IN_TUNE && previous.target == target
        val inTuneWindow = if (wasInTune) config.releaseCents else config.greenCents
        val inTune = abs(cents) <= inTuneWindow + 1e-9
        val status = when { inTune -> TuningStatus.IN_TUNE; cents < 0 -> TuningStatus.FLAT; else -> TuningStatus.SHARP }
        val color = when { inTune -> AccuracyColor.GREEN; abs(cents) <= config.yellowCents + 1e-9 -> AccuracyColor.YELLOW; else -> AccuracyColor.RED }
        return TuningResult(target, cents, status, color)
    }
}

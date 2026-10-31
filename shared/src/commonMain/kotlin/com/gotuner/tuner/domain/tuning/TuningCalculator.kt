package com.gotuner.tuner.domain.tuning

import com.gotuner.tuner.domain.model.*
import kotlin.math.*

class TuningCalculator(private val config: TuningConfig = TuningConfig()) {
    val strings = listOf(GuitarString(6,"E2",82.41), GuitarString(5,"A2",110.0), GuitarString(4,"D3",146.83), GuitarString(3,"G3",196.0), GuitarString(2,"B3",246.94), GuitarString(1,"E4",329.63))
    fun calculate(frequency: Double): TuningResult? {
        if (!frequency.isFinite() || frequency <= 0) return null
        val target = strings.minBy { abs(1200 * log2(frequency / it.frequency)) }
        val cents = 1200 * log2(frequency / target.frequency)
        if (abs(cents) > config.maxTargetDistance) return null
        return TuningResult(target, cents, when { abs(cents) <= config.greenCents + 1e-9 -> TuningStatus.IN_TUNE; cents < 0 -> TuningStatus.FLAT; else -> TuningStatus.SHARP }, when { abs(cents) <= config.greenCents + 1e-9 -> AccuracyColor.GREEN; abs(cents) <= config.yellowCents + 1e-9 -> AccuracyColor.YELLOW; else -> AccuracyColor.RED })
    }
}

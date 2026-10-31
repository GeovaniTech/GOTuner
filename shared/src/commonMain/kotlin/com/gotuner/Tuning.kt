package com.gotuner

import kotlin.math.*

enum class TuningStatus { FLAT, IN_TUNE, SHARP }
enum class AccuracyColor { RED, YELLOW, GREEN }
enum class MicrophonePermissionState { UNKNOWN, GRANTED, DENIED }
data class TuningConfig(val greenCents: Double = 5.0, val yellowCents: Double = 15.0, val maxTargetDistance: Double = 100.0) {
    init { require(greenCents >= 0 && yellowCents >= greenCents && maxTargetDistance >= yellowCents) }
}
data class GuitarString(val number: Int, val note: String, val frequency: Double)
data class TuningResult(val target: GuitarString, val cents: Double, val status: TuningStatus, val color: AccuracyColor)
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

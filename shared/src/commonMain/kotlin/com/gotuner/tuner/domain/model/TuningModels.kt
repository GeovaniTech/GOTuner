package com.gotuner.tuner.domain.model

enum class TuningStatus { FLAT, IN_TUNE, SHARP }
enum class AccuracyColor { RED, YELLOW, GREEN }
data class TuningConfig(val greenCents: Double = 5.0, val yellowCents: Double = 15.0, val maxTargetDistance: Double = 100.0) {
    init { require(greenCents >= 0 && yellowCents >= greenCents && maxTargetDistance >= yellowCents) }
}
data class GuitarString(val number: Int, val note: String, val frequency: Double)
data class TuningResult(val target: GuitarString, val cents: Double, val status: TuningStatus, val color: AccuracyColor)

package br.com.gotuner.tuner.domain.model

enum class TuningStatus { FLAT, IN_TUNE, SHARP }
enum class AccuracyColor { RED, YELLOW, GREEN }

/**
 * [greenCents] is the window a reading must enter to be considered in tune.
 * [releaseCents] is the wider window an in-tune string may drift within before it is
 * reported as flat/sharp again (hysteresis), so harder or softer plucks do not flip the result.
 */
data class TuningConfig(
    val greenCents: Double = 7.0,
    val releaseCents: Double = 12.0,
    val yellowCents: Double = 15.0,
    val maxTargetDistance: Double = 100.0
) {
    init { require(greenCents >= 0 && releaseCents >= greenCents && yellowCents >= releaseCents && maxTargetDistance >= yellowCents) }
}
data class GuitarString(val number: Int, val note: String, val frequency: Double)
data class TuningResult(val target: GuitarString, val cents: Double, val status: TuningStatus, val color: AccuracyColor)

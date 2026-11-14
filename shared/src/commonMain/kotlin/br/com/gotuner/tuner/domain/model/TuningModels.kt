package br.com.gotuner.tuner.domain.model

enum class TuningStatus { FLAT, IN_TUNE, SHARP }
enum class AccuracyColor { RED, YELLOW, GREEN }

/**
 * [greenCents] is the window a reading must enter to be considered in tune.
 * [releaseCents] is the wider window an in-tune string may drift within before it is
 * reported as flat/sharp again (hysteresis), so harder or softer plucks do not flip the result.
 * [maxTargetDistance] limits automatic string detection. [manualMaxDistance] is the wider limit
 * used when the user picked the string; it stays below an octave (1200 cents) so a reading of
 * the string's harmonic is never turned into "loosen" advice.
 */
data class TuningConfig(
    val greenCents: Double = 7.0,
    val releaseCents: Double = 12.0,
    val yellowCents: Double = 15.0,
    val maxTargetDistance: Double = 100.0,
    val manualMaxDistance: Double = 900.0
) {
    init { require(greenCents >= 0 && releaseCents >= greenCents && yellowCents >= releaseCents && maxTargetDistance >= yellowCents && manualMaxDistance >= maxTargetDistance) }
}
data class GuitarString(val number: Int, val note: String, val frequency: Double)
data class TuningResult(val target: GuitarString, val cents: Double, val status: TuningStatus, val color: AccuracyColor)

/** Standard six-string tuning, from the thickest string (6) to the thinnest (1). */
object StandardTuning {
    val strings: List<GuitarString> = listOf(GuitarString(6,"E2",82.41), GuitarString(5,"A2",110.0), GuitarString(4,"D3",146.83), GuitarString(3,"G3",196.0), GuitarString(2,"B3",246.94), GuitarString(1,"E4",329.63))
}

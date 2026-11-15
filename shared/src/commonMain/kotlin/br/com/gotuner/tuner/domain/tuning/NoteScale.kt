package br.com.gotuner.tuner.domain.tuning

import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Equal-tempered chromatic scale with A4 = 440 Hz. It places any frequency among all the notes,
 * not only the six strings, so the note strip can show which note the sound is closest to.
 * Notes are numbered in semitones as in MIDI: E2 is 40, A4 is 69.
 */
object NoteScale {
    private const val A4_SEMITONE = 69
    private const val A4_FREQUENCY = 440.0
    private val names = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    /**
     * Where [frequency] falls on the scale, in semitones. The fraction is the distance from the
     * note below: 40.0 is exactly E2 and 40.5 is halfway to F2. Null for an invalid frequency.
     */
    fun position(frequency: Double): Double? =
        if (frequency.isFinite() && frequency > 0) A4_SEMITONE + 12 * log2(frequency / A4_FREQUENCY) else null

    /** Number of the note closest to [frequency], or null for an invalid frequency. */
    fun closestSemitone(frequency: Double): Int? = position(frequency)?.roundToInt()

    /** Name with octave of the note numbered [semitone], e.g. "E2" for 40 or "D#2" for 39. */
    fun name(semitone: Int): String = names[semitone.mod(12)] + (semitone.floorDiv(12) - 1)

    fun frequency(semitone: Int): Double = A4_FREQUENCY * 2.0.pow((semitone - A4_SEMITONE) / 12.0)
}

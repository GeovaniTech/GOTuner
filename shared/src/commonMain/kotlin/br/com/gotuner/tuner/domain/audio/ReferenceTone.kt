package br.com.gotuner.tuner.domain.audio

import kotlin.math.*

/**
 * Platform audio output for the reference tone. Calls arrive on the main thread.
 * [play] replaces any tone still sounding and invokes onFinished on the main thread when the
 * tone ends by itself (or cannot be played); it is not invoked after [stop] or a newer [play].
 */
interface TonePlayer {
    fun play(samples: FloatArray, sampleRate: Int, onFinished: () -> Unit)
    fun stop()
}

/**
 * Synthesizes a plucked-string tone at an exact pitch. The pitch comes from additive harmonics
 * rather than a sine: phone speakers barely reproduce the 82–110 Hz fundamentals of the low
 * strings, but the ear still hears the note from its overtones.
 */
object ReferenceTone {
    const val SAMPLE_RATE = 44100
    const val DURATION_SECONDS = 2.0
    private const val MAX_HARMONICS = 12
    private const val MAX_PARTIAL_HZ = 6000.0
    /** Plucking a fifth of the way along the string; shapes the harmonic amplitudes. */
    private const val PLUCK_POSITION = 0.2
    private const val BASE_DECAY = 1.1
    private const val DECAY_PER_HARMONIC = 0.1
    private const val PEAK = 0.8
    private const val ATTACK_SECONDS = 0.004
    private const val RELEASE_SECONDS = 0.08

    fun synthesize(frequency: Double, sampleRate: Int = SAMPLE_RATE): FloatArray {
        require(frequency > 0 && sampleRate > 0)
        val count = (DURATION_SECONDS * sampleRate).toInt()
        val mix = DoubleArray(count)
        val harmonics = min(MAX_HARMONICS, (min(MAX_PARTIAL_HZ, sampleRate / 2.0 - 1) / frequency).toInt()).coerceAtLeast(1)
        for (n in 1..harmonics) {
            val amplitude = abs(sin(PI * n * PLUCK_POSITION)) / n
            if (amplitude < 1e-6) continue
            // Decaying rotating phasor: one exact partial per harmonic without per-sample sin/exp.
            val angle = 2 * PI * n * frequency / sampleRate
            val decay = exp(-BASE_DECAY * (1 + DECAY_PER_HARMONIC * (n - 1)) / sampleRate)
            val cosStep = cos(angle) * decay
            val sinStep = sin(angle) * decay
            var s = 0.0
            var c = amplitude
            for (i in 0 until count) {
                mix[i] += s
                val next = s * cosStep + c * sinStep
                c = c * cosStep - s * sinStep
                s = next
            }
        }
        val scale = PEAK / mix.maxOf { abs(it) }.coerceAtLeast(1e-9)
        val attack = (ATTACK_SECONDS * sampleRate).toInt().coerceAtLeast(1)
        val release = (RELEASE_SECONDS * sampleRate).toInt().coerceAtLeast(1)
        return FloatArray(count) { i ->
            val fade = min(1.0, min((i + 1.0) / attack, (count - i).toDouble() / release))
            (mix[i] * scale * fade).toFloat()
        }
    }
}

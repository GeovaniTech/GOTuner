package br.com.gotuner.tuner.domain.audio

import kotlin.math.*

data class PitchEstimate(val frequency: Double, val confidence: Double)
/** Single-worker YIN detector. Reuses its difference buffers; input must be normalized mono PCM. */
class PitchDetector(private val frameSize: Int = 4096, private val threshold: Double = 0.15) {
    init { require(frameSize >= 32 && frameSize % 2 == 0); require(threshold > 0 && threshold < 1) }
    private val difference = DoubleArray(frameSize / 2)
    fun detect(samples: FloatArray, sampleRate: Int): PitchEstimate? {
        if (samples.size < frameSize || sampleRate !in 8000..192000) return null
        var energy = 0.0
        for (i in 0 until frameSize) { if (!samples[i].isFinite()) return null; energy += samples[i] * samples[i] }
        if (sqrt(energy / frameSize) < 0.001) return null
        val maxLag = min(difference.lastIndex - 1, sampleRate / 65)
        val minLag = max(2, sampleRate / 2000)
        var cumulative = 0.0
        difference[0] = 1.0
        for (lag in 1..maxLag) {
            var sum = 0.0
            for (i in 0 until frameSize / 2) { val delta = samples[i] - samples[i + lag]; sum += delta * delta }
            cumulative += sum
            difference[lag] = if (cumulative > 0) sum * lag / cumulative else 1.0
        }
        var lag = minLag
        while (lag < maxLag) {
            if (difference[lag] < threshold) {
                while (lag + 1 < maxLag && difference[lag + 1] < difference[lag]) lag++
                val confidence = 1.0 - difference[lag]
                if (confidence < 0.85) return null
                val a = difference[lag - 1]; val b = difference[lag]; val c = difference[lag + 1]
                val denominator = 2 * (2*b-a-c)
                val offset = if (abs(denominator) > 1e-12) (c-a)/denominator else 0.0
                val frequency = sampleRate / (lag + offset.coerceIn(-1.0, 1.0))
                // Inspect higher pitches first, then reject them; starting the search
                // at 380 Hz incorrectly folded 440/659 Hz tones down into guitar targets.
                if (frequency !in 65.0..380.0) return null
                return PitchEstimate(frequency, confidence)
            }
            lag++
        }
        return null
    }
}

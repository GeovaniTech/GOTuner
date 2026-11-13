package br.com.gotuner.tuner.presentation

import br.com.gotuner.tuner.domain.audio.*
import br.com.gotuner.tuner.domain.model.TuningResult
import br.com.gotuner.tuner.domain.tuning.TuningCalculator

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.*

/** Call processing methods from one audio worker; state publication is thread safe. */
class TunerViewModel(
    private val detector: PitchDetector,
    private val calculator: TuningCalculator
) : AudioInputObserver {

    companion object {
        /** Raw pitches kept for the median filter that rejects pluck-attack outliers. */
        internal const val MEDIAN_WINDOW = 5
        /** A jump larger than this is treated as a different string being played. */
        internal const val NEW_NOTE_CENTS = 80.0
    }

    private val mutableState = MutableStateFlow(TunerUiState())
    val state: StateFlow<TunerUiState> = mutableState.asStateFlow()
    private var smoothed = 0.0
    private var stable = 0
    private var hasReading = false
    private var lastResult: TuningResult? = null
    private val recent = ArrayDeque<Double>(MEDIAN_WINDOW)

    fun observe(onState: (TunerUiState) -> Unit): StateSubscription = StateSubscription(CoroutineScope(Dispatchers.Main).launch { state.collect { onState(it) } })
    override fun permission(granted: Boolean) { mutableState.update { it.copy(microphonePermissionState = if (granted) MicrophonePermissionState.GRANTED else MicrophonePermissionState.DENIED, errorMessage = null) } }
    override fun listening(active: Boolean) { mutableState.update { TunerUiState(isListening = active, microphonePermissionState = it.microphonePermissionState) } }
    override fun fail(message: String) { mutableState.update { TunerUiState(errorMessage = message, microphonePermissionState = it.microphonePermissionState) } }

    override fun process(samples: FloatArray, sampleRate: Int) {
        if (!state.value.isListening) return
        // listening()/fail() clear the reading; a new capture session must establish its own stable pitch.
        if (state.value.detectedFrequency == null && hasReading) resetTracking()
        // Silence or an undetectable frame keeps the last reading on screen indefinitely,
        // so the user can keep adjusting the peg and plucking again.
        val pitch = detector.detect(samples, sampleRate) ?: return
        if (smoothed == 0.0 || abs(1200 * log2(pitch.frequency / smoothed)) > NEW_NOTE_CENTS) {
            smoothed = pitch.frequency; stable = 0; recent.clear(); lastResult = null
        }
        recent.addLast(pitch.frequency)
        if (recent.size > MEDIAN_WINDOW) recent.removeFirst()
        // Median then EMA: a hard pluck briefly reads sharp and a soft one fades flat;
        // the median discards those transients, the EMA calms the remaining jitter.
        smoothed = 0.7 * smoothed + 0.3 * median(recent)
        if (++stable < 2) return
        hasReading = true
        val result = calculator.calculate(smoothed, lastResult)
        lastResult = result
        mutableState.update { if (!it.isListening) it else it.copy(detectedFrequency = smoothed, targetFrequency = result?.target?.frequency, targetNote = result?.target?.note, deviationCents = result?.cents, tuningStatus = result?.status, accuracyColor = result?.color, detectionConfidence = pitch.confidence, errorMessage = null) }
    }

    private fun resetTracking() { smoothed = 0.0; stable = 0; hasReading = false; lastResult = null; recent.clear() }
}

private fun median(values: Collection<Double>): Double {
    val sorted = values.sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
}

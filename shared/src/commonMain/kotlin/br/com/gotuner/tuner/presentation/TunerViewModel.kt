package br.com.gotuner.tuner.presentation

import br.com.gotuner.tuner.domain.audio.*
import br.com.gotuner.tuner.domain.tuning.TuningCalculator

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.*
import kotlin.time.TimeSource

/** Call processing methods from one audio worker; state publication is thread safe. */
class TunerViewModel internal constructor(
    private val detector: PitchDetector,
    private val calculator: TuningCalculator,
    private val nowMillis: () -> Long
) : AudioInputObserver {
    constructor(detector: PitchDetector, calculator: TuningCalculator) : this(detector, calculator, monotonicClock())

    companion object {
        const val PITCH_HOLD_MILLIS = 1_500L
    }

    private val mutableState = MutableStateFlow(TunerUiState())
    val state: StateFlow<TunerUiState> = mutableState.asStateFlow()
    private var smoothed = 0.0
    private var lastPitchAt: Long? = null
    private var stable = 0
    fun observe(onState: (TunerUiState) -> Unit): StateSubscription = StateSubscription(CoroutineScope(Dispatchers.Main).launch { state.collect { onState(it) } })
    override fun permission(granted: Boolean) { mutableState.update { it.copy(microphonePermissionState = if (granted) MicrophonePermissionState.GRANTED else MicrophonePermissionState.DENIED, errorMessage = null) } }
    override fun listening(active: Boolean) { mutableState.update { TunerUiState(isListening = active, microphonePermissionState = it.microphonePermissionState) } }
    override fun fail(message: String) { mutableState.update { TunerUiState(errorMessage = message, microphonePermissionState = it.microphonePermissionState) } }
    override fun process(samples: FloatArray, sampleRate: Int) {
        if (!state.value.isListening) return
        // A new capture session must establish its own stable pitch.
        if (state.value.detectedFrequency == null && lastPitchAt != null) {
            smoothed = 0.0; stable = 0; lastPitchAt = null
        }
        val pitch = detector.detect(samples, sampleRate)
        if (pitch == null) {
            // Keep the last reading through the decay of a plucked string.
            // Elapsed time makes this independent of sample rate and callback cadence.
            val last = lastPitchAt
            if (last != null && nowMillis() - last >= PITCH_HOLD_MILLIS) {
                smoothed = 0.0; stable = 0; lastPitchAt = null
                mutableState.update { TunerUiState(isListening = it.isListening, microphonePermissionState = it.microphonePermissionState) }
            }
            return
        }
        if (smoothed == 0.0 || abs(1200 * log2(pitch.frequency / smoothed)) > 80) { smoothed = pitch.frequency; stable = 0 }
        smoothed = 0.65 * smoothed + 0.35 * pitch.frequency
        if (++stable < 2) return
        lastPitchAt = nowMillis()
        val result = calculator.calculate(smoothed)
        mutableState.update { if (!it.isListening) it else it.copy(detectedFrequency = smoothed, targetFrequency = result?.target?.frequency, targetNote = result?.target?.note, deviationCents = result?.cents, tuningStatus = result?.status, accuracyColor = result?.color, detectionConfidence = pitch.confidence, errorMessage = null) }
    }
}

private fun monotonicClock(): () -> Long {
    val origin = TimeSource.Monotonic.markNow()
    return { origin.elapsedNow().inWholeMilliseconds }
}

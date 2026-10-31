package com.gotuner

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.*

data class TunerUiState(
    val detectedFrequency: Double? = null, val targetFrequency: Double? = null,
    val targetNote: String? = null, val deviationCents: Double? = null,
    val tuningStatus: TuningStatus? = null, val accuracyColor: AccuracyColor? = null,
    val isListening: Boolean = false,
    val microphonePermissionState: MicrophonePermissionState = MicrophonePermissionState.UNKNOWN,
    val detectionConfidence: Double = 0.0, val errorMessage: String? = null
)
interface AudioSource { fun start(); fun stop() }
class StateSubscription(private val job: Job) { fun cancel() = job.cancel() }
/** Call processing methods from one audio worker; state publication is thread safe. */
class TunerViewModel {
    private val detector = PitchDetector()
    private val calculator = TuningCalculator()
    private val mutableState = MutableStateFlow(TunerUiState())
    val state: StateFlow<TunerUiState> = mutableState.asStateFlow()
    private var smoothed = 0.0
    private var missed = 0
    private var stable = 0
    fun observe(onState: (TunerUiState) -> Unit): StateSubscription = StateSubscription(CoroutineScope(Dispatchers.Main).launch { state.collect { onState(it) } })
    fun permission(granted: Boolean) { mutableState.update { it.copy(microphonePermissionState = if (granted) MicrophonePermissionState.GRANTED else MicrophonePermissionState.DENIED, errorMessage = null) } }
    fun listening(active: Boolean) { mutableState.update { TunerUiState(isListening = active, microphonePermissionState = it.microphonePermissionState) } }
    fun fail(message: String) { mutableState.update { TunerUiState(errorMessage = message, microphonePermissionState = it.microphonePermissionState) } }
    fun process(samples: FloatArray, sampleRate: Int) {
        if (!state.value.isListening) return
        val pitch = detector.detect(samples, sampleRate)
        if (pitch == null) {
            missed++
            if (missed >= 3) { smoothed = 0.0; stable = 0; mutableState.update { TunerUiState(isListening = it.isListening, microphonePermissionState = it.microphonePermissionState) } }
            return
        }
        missed = 0
        if (smoothed == 0.0 || abs(1200 * log2(pitch.frequency / smoothed)) > 80) { smoothed = pitch.frequency; stable = 0 }
        smoothed = 0.65 * smoothed + 0.35 * pitch.frequency
        if (++stable < 2) return
        val result = calculator.calculate(smoothed)
        mutableState.update { it.copy(detectedFrequency = smoothed, targetFrequency = result?.target?.frequency, targetNote = result?.target?.note, deviationCents = result?.cents, tuningStatus = result?.status, accuracyColor = result?.color, detectionConfidence = pitch.confidence, errorMessage = null) }
    }
}

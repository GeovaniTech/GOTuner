package br.com.gotuner.tuner.presentation

import br.com.gotuner.tuner.domain.audio.*
import br.com.gotuner.tuner.domain.model.TuningResult
import br.com.gotuner.tuner.domain.model.TuningStatus
import br.com.gotuner.tuner.domain.tuning.TuningCalculator

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.*

/**
 * Call processing methods from one audio worker; state publication is thread safe.
 * String selection and the reference tone are driven from the main thread.
 */
class TunerViewModel(
    private val detector: PitchDetector,
    private val calculator: TuningCalculator,
    private val tonePlayer: TonePlayer? = null
) : AudioInputObserver {

    companion object {
        /** Raw pitches kept for the median filter that rejects pluck-attack outliers. */
        internal const val MEDIAN_WINDOW = 5
        /** A jump larger than this is treated as a different string being played. */
        internal const val NEW_NOTE_CENTS = 80.0
        /** Consecutive readings with the same verdict before a string is marked (or unmarked) as tuned. */
        internal const val TUNED_FRAMES = 4
        /** Frames ignored after the reference tone ends, so its tail in the room is not read as the guitar. */
        internal const val REFERENCE_TAIL_FRAMES = 8
    }

    private val mutableState = MutableStateFlow(TunerUiState())
    val state: StateFlow<TunerUiState> = mutableState.asStateFlow()
    /** Strings of the instrument, thickest first, for the string selector. */
    val strings get() = calculator.strings
    private var smoothed = 0.0
    private var stable = 0
    private var hasReading = false
    private var lastResult: TuningResult? = null
    private val recent = ArrayDeque<Double>(MEDIAN_WINDOW)
    private var verdictString = 0
    private var verdictInTune = false
    private var verdictFrames = 0
    private var mutedFrames = 0
    private val tones = HashMap<Int, FloatArray>()
    private var toneGeneration = 0

    fun observe(onState: (TunerUiState) -> Unit): StateSubscription = StateSubscription(CoroutineScope(Dispatchers.Main).launch { state.collect { onState(it) } })
    override fun permission(granted: Boolean) { mutableState.update { it.copy(microphonePermissionState = if (granted) MicrophonePermissionState.GRANTED else MicrophonePermissionState.DENIED, errorMessage = null) } }
    override fun listening(active: Boolean) { mutableState.update { it.withoutReading().copy(isListening = active) } }
    override fun fail(message: String) { mutableState.update { it.withoutReading().copy(isListening = false, errorMessage = message) } }

    /** Locks the tuner onto one string (by its number), or returns to automatic detection with null. */
    fun selectString(number: Int?) {
        val selected = calculator.string(number)?.number
        if (selected == state.value.selectedString) return
        stopReferenceTone()
        mutableState.update { it.copy(selectedString = selected).withoutReading().copy(isListening = it.isListening, errorMessage = it.errorMessage) }
    }

    /** Plays the correct pitch of the selected (or currently detected) string; a second call stops it. */
    fun toggleReferenceTone() {
        val current = state.value
        if (current.isPlayingReference) { stopReferenceTone(); return }
        val player = tonePlayer ?: return
        val string = calculator.string(current.selectedString ?: current.targetString) ?: return
        val token = ++toneGeneration
        mutableState.update { it.copy(isPlayingReference = true) }
        player.play(tones.getOrPut(string.number) { ReferenceTone.synthesize(string.frequency) }, ReferenceTone.SAMPLE_RATE) {
            if (token == toneGeneration) mutableState.update { it.copy(isPlayingReference = false) }
        }
    }

    fun stopReferenceTone() {
        toneGeneration++
        if (!state.value.isPlayingReference) return
        tonePlayer?.stop()
        mutableState.update { it.copy(isPlayingReference = false) }
    }

    override fun process(samples: FloatArray, sampleRate: Int) {
        val current = state.value
        if (!current.isListening) return
        // The speaker's reference tone must not be measured as if it were the guitar.
        if (current.isPlayingReference) { mutedFrames = REFERENCE_TAIL_FRAMES; return }
        if (mutedFrames > 0) { if (--mutedFrames == 0) resetTracking(); return }
        // listening()/fail()/selectString() clear the reading; the next one must establish its own stable pitch.
        if (current.detectedFrequency == null && hasReading) resetTracking()
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
        val selected = calculator.string(current.selectedString)
        val result = calculator.calculate(smoothed, lastResult, selected)
        lastResult = result
        val target = result?.target ?: selected
        val verdict = confirmedVerdict(result)
        mutableState.update {
            // Drop a reading that raced with a stop, a string change or the reference tone.
            if (!it.isListening || it.isPlayingReference || it.selectedString != current.selectedString) it
            else it.copy(
                detectedFrequency = smoothed, targetFrequency = target?.frequency, targetNote = target?.note,
                targetString = target?.number, deviationCents = result?.cents, tuningStatus = result?.status,
                accuracyColor = result?.color, detectionConfidence = pitch.confidence, errorMessage = null,
                tunedStrings = when (verdict) {
                    null -> it.tunedStrings
                    true -> it.tunedStrings + result!!.target.number
                    false -> it.tunedStrings - result!!.target.number
                }
            )
        }
    }

    /** True/false once the same in-tune/out-of-tune verdict held for [TUNED_FRAMES] readings of one string. */
    private fun confirmedVerdict(result: TuningResult?): Boolean? {
        if (result == null) { verdictFrames = 0; return null }
        val inTune = result.status == TuningStatus.IN_TUNE
        if (result.target.number != verdictString || inTune != verdictInTune) {
            verdictString = result.target.number; verdictInTune = inTune; verdictFrames = 0
        }
        return if (++verdictFrames >= TUNED_FRAMES) inTune else null
    }

    private fun resetTracking() { smoothed = 0.0; stable = 0; hasReading = false; lastResult = null; recent.clear(); verdictFrames = 0 }

    /** Clears the reading but keeps what the user set up: permission, chosen string, tuned strings and the tone. */
    private fun TunerUiState.withoutReading(): TunerUiState {
        val selected = calculator.string(selectedString)
        return TunerUiState(
            microphonePermissionState = microphonePermissionState,
            selectedString = selected?.number, targetString = selected?.number,
            targetNote = selected?.note, targetFrequency = selected?.frequency,
            tunedStrings = tunedStrings, isPlayingReference = isPlayingReference
        )
    }
}

private fun median(values: Collection<Double>): Double {
    val sorted = values.sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
}

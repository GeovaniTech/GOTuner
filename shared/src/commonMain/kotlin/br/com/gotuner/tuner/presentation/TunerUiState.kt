package br.com.gotuner.tuner.presentation

import br.com.gotuner.tuner.domain.model.*

enum class MicrophonePermissionState { UNKNOWN, GRANTED, DENIED }

data class TunerUiState(
    val detectedFrequency: Double? = null, val targetFrequency: Double? = null,
    val targetNote: String? = null, val deviationCents: Double? = null,
    val tuningStatus: TuningStatus? = null, val accuracyColor: AccuracyColor? = null,
    val isListening: Boolean = false,
    val microphonePermissionState: MicrophonePermissionState = MicrophonePermissionState.UNKNOWN,
    val detectionConfidence: Double = 0.0, val errorMessage: String? = null,
    /** String number the user chose to tune; null means the closest string is detected automatically. */
    val selectedString: Int? = null,
    /** String number the reading is compared against: the selected one, or the detected one in automatic mode. */
    val targetString: Int? = null,
    /** String numbers confirmed in tune since the app was opened. */
    val tunedStrings: Set<Int> = emptySet(),
    /** True while the reference tone sounds; microphone readings are paused meanwhile. */
    val isPlayingReference: Boolean = false
)

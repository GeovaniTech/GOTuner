package com.gotuner.tuner.presentation

import com.gotuner.tuner.domain.model.*

enum class MicrophonePermissionState { UNKNOWN, GRANTED, DENIED }

data class TunerUiState(
    val detectedFrequency: Double? = null, val targetFrequency: Double? = null,
    val targetNote: String? = null, val deviationCents: Double? = null,
    val tuningStatus: TuningStatus? = null, val accuracyColor: AccuracyColor? = null,
    val isListening: Boolean = false,
    val microphonePermissionState: MicrophonePermissionState = MicrophonePermissionState.UNKNOWN,
    val detectionConfidence: Double = 0.0, val errorMessage: String? = null
)

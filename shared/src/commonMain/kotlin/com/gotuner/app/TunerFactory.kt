package com.gotuner.app

import com.gotuner.tuner.domain.audio.PitchDetector
import com.gotuner.tuner.domain.tuning.TuningCalculator
import com.gotuner.tuner.presentation.TunerViewModel

/** Shared composition root; platforms supply their native AudioSource separately. */
object TunerFactory {
    fun createViewModel(): TunerViewModel = TunerViewModel(PitchDetector(), TuningCalculator())
}

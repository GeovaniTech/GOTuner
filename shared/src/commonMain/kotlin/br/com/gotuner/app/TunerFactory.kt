package br.com.gotuner.app

import br.com.gotuner.tuner.domain.audio.PitchDetector
import br.com.gotuner.tuner.domain.tuning.TuningCalculator
import br.com.gotuner.tuner.presentation.TunerViewModel

/** Shared composition root; platforms supply their native AudioSource separately. */
object TunerFactory {
    fun createViewModel(): TunerViewModel = TunerViewModel(PitchDetector(), TuningCalculator())
}

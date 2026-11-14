package br.com.gotuner.app

import br.com.gotuner.tuner.domain.audio.PitchDetector
import br.com.gotuner.tuner.domain.audio.TonePlayer
import br.com.gotuner.tuner.domain.tuning.TuningCalculator
import br.com.gotuner.tuner.presentation.TunerViewModel

/** Shared composition root; platforms supply their native AudioSource and TonePlayer. */
object TunerFactory {
    fun createViewModel(tonePlayer: TonePlayer? = null): TunerViewModel = TunerViewModel(PitchDetector(), TuningCalculator(), tonePlayer)
}

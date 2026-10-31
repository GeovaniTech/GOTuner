package br.com.gotuner.tuner.domain.audio

import kotlin.test.*
import kotlin.math.*
import br.com.gotuner.tuner.wave
import br.com.gotuner.tuner.domain.audio.*
import br.com.gotuner.tuner.domain.tuning.*
import br.com.gotuner.tuner.domain.model.*
import br.com.gotuner.tuner.presentation.*
import br.com.gotuner.app.TunerFactory

class PitchDetectorTest {
 private val calculator = TuningCalculator()
 @Test fun sineWaves() { for(rate in listOf(44100,48000)) calculator.strings.forEach { val pitch=PitchDetector().detect(wave(it.frequency,rate),rate)!!; assertEquals(it.frequency,pitch.frequency,0.5); assertTrue(pitch.confidence>0.9) } }
 @Test fun harmonics() { calculator.strings.forEach { val pitch=PitchDetector().detect(wave(it.frequency,harmonic=true),48000)!!; assertEquals(it.frequency,pitch.frequency,0.6) } }
 @Test fun silenceNoiseAndBadInput() {
  val detector=PitchDetector(); assertNull(detector.detect(FloatArray(4096),48000)); assertNull(detector.detect(FloatArray(4096){0.0001f},48000)); assertNull(detector.detect(FloatArray(10),48000)); assertNull(detector.detect(wave(110.0),0))
  val random=kotlin.random.Random(42); assertNull(detector.detect(FloatArray(4096){random.nextFloat()-0.5f},48000))
 }
 @Test fun realOctaveIsNotFolded() { val result=PitchDetector().detect(wave(329.63),48000)!!; assertEquals(329.63,result.frequency,0.5); assertEquals("E4",calculator.calculate(result.frequency)!!.target.note) }
 @Test fun highPureTonesAreNotFoldedIntoGuitarTargets() {
  for (rate in listOf(44100, 48000)) for (frequency in listOf(440.0, 659.26, 880.0)) {
   assertNull(PitchDetector().detect(wave(frequency, rate), rate), "$frequency Hz at $rate")
  }
 }
 @Test fun invalidSamplesAndConfigurationAreRejected() {
  assertNull(PitchDetector().detect(FloatArray(4096) { Float.NaN }, 48000))
  assertNull(PitchDetector().detect(FloatArray(4096) { Float.POSITIVE_INFINITY }, 48000))
  assertFailsWith<IllegalArgumentException> { PitchDetector(frameSize = 0) }
  assertFailsWith<IllegalArgumentException> { PitchDetector(threshold = 1.0) }
 }

}

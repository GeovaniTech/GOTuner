package br.com.gotuner.tuner.presentation

import kotlin.test.*
import kotlin.math.*
import br.com.gotuner.tuner.wave
import br.com.gotuner.tuner.domain.audio.*
import br.com.gotuner.tuner.domain.tuning.*
import br.com.gotuner.tuner.domain.model.*
import br.com.gotuner.tuner.presentation.*
import br.com.gotuner.app.TunerFactory

class TunerViewModelTest {
 private fun cents(c: Double) = 110.0 * 2.0.pow(c / 1200)

 @Test fun readingIsNeverClearedBySilence() {
  val model = TunerViewModel(PitchDetector(), TuningCalculator()); model.permission(true); model.listening(true)
  repeat(3) { model.process(wave(110.0),48000) }
  val reading = model.state.value
  assertNotNull(reading.detectedFrequency)
  repeat(500) { model.process(FloatArray(4096),48000) }
  assertEquals(reading, model.state.value)
  assertEquals("A2", model.state.value.targetNote)
  assertNotNull(model.state.value.deviationCents)
  assertTrue(model.state.value.isListening)
 }
 @Test fun newPitchUpdatesAfterSilence() {
  val model = TunerViewModel(PitchDetector(), TuningCalculator()); model.listening(true)
  repeat(3) { model.process(wave(110.0),48000) }
  repeat(50) { model.process(FloatArray(4096),48000) }
  repeat(3) { model.process(wave(146.83),48000) }
  assertEquals("D3",model.state.value.targetNote)
  repeat(50) { model.process(FloatArray(4096),48000) }
  assertEquals("D3",model.state.value.targetNote)
 }
 @Test fun inTuneSurvivesHarderAndSofterPlucks() {
  val model = TunerFactory.createViewModel(); model.listening(true)
  repeat(6) { model.process(wave(110.0),48000) }
  assertEquals(TuningStatus.IN_TUNE, model.state.value.tuningStatus)
  // Hard pluck reads sharp, soft decay reads flat; both stay inside the release window.
  for (offset in listOf(10.0, 9.0, -8.0, -10.0, 6.0, -4.0)) {
   model.process(wave(cents(offset)),48000)
   assertEquals(TuningStatus.IN_TUNE, model.state.value.tuningStatus, "offset $offset")
   assertEquals(AccuracyColor.GREEN, model.state.value.accuracyColor)
  }
 }
 @Test fun singleAttackSpikeIsIgnored() {
  val model = TunerFactory.createViewModel(); model.listening(true)
  repeat(6) { model.process(wave(110.0),48000) }
  model.process(wave(cents(40.0)),48000)
  assertEquals(TuningStatus.IN_TUNE, model.state.value.tuningStatus)
 }
 @Test fun reachingTargetWhileTighteningShowsInTune() {
  val model = TunerFactory.createViewModel(); model.listening(true)
  repeat(6) { model.process(wave(cents(-30.0)),48000) }
  assertEquals(TuningStatus.FLAT, model.state.value.tuningStatus)
  repeat(12) { model.process(wave(cents(-2.0)),48000) }
  assertEquals(TuningStatus.IN_TUNE, model.state.value.tuningStatus)
  repeat(100) { model.process(FloatArray(4096),48000) }
  assertEquals(TuningStatus.IN_TUNE, model.state.value.tuningStatus)
 }
 @Test fun clearlyDetunedStringLeavesInTune() {
  val model = TunerFactory.createViewModel(); model.listening(true)
  repeat(6) { model.process(wave(110.0),48000) }
  repeat(12) { model.process(wave(cents(30.0)),48000) }
  assertEquals(TuningStatus.SHARP, model.state.value.tuningStatus)
 }
 @Test fun stopAndErrorClearHeldReadingImmediately() {
  val model = TunerViewModel(PitchDetector(), TuningCalculator()); model.listening(true)
  repeat(3) { model.process(wave(110.0),48000) }
  model.listening(false)
  assertNull(model.state.value.detectedFrequency)
  model.process(wave(110.0),48000)
  assertFalse(model.state.value.isListening)
  model.listening(true)
  repeat(3) { model.process(wave(110.0),48000) }
  model.fail("Microphone unavailable")
  assertNull(model.state.value.detectedFrequency)
  assertEquals("Microphone unavailable",model.state.value.errorMessage)
 }
 @Test fun quietMicrophoneSignalStillUpdatesFrequency() {
  val model = TunerFactory.createViewModel(); model.listening(true)
  val quiet = FloatArray(4096) { (0.003 * sin(2 * PI * 110 * it / 48000)).toFloat() }
  repeat(4) { model.process(quiet, 48000) }
  assertEquals(110.0, assertNotNull(model.state.value.detectedFrequency), 0.5)
  assertEquals("A2", model.state.value.targetNote)
 }
 @Test fun unsupportedPitchShowsHertzWithoutTuningAdvice() {
  val model = TunerFactory.createViewModel(); model.listening(true)
  repeat(4) { model.process(wave(110.0),48000) }
  repeat(4) { model.process(wave(180.0),48000) }
  assertEquals(180.0, assertNotNull(model.state.value.detectedFrequency), 0.5)
  assertNull(model.state.value.targetNote)
  assertNull(model.state.value.tuningStatus)
  assertNull(model.state.value.deviationCents)
  assertNull(model.state.value.accuracyColor)
 }
 @Test fun injectedCalculatorIsUsedThroughAudioPort() {
  val model = TunerViewModel(PitchDetector(), TuningCalculator(TuningConfig(greenCents = 25.0, releaseCents = 30.0, yellowCents = 35.0)))
  val input: AudioInputObserver = model
  input.permission(true)
  input.listening(true)
  repeat(3) { input.process(wave(111.28), 48000) }
  assertEquals(MicrophonePermissionState.GRANTED, model.state.value.microphonePermissionState)
  assertEquals(TuningStatus.IN_TUNE, model.state.value.tuningStatus)
  assertEquals(AccuracyColor.GREEN, model.state.value.accuracyColor)
  input.fail("Capture failed")
  assertEquals("Capture failed", model.state.value.errorMessage)
  assertNull(model.state.value.detectedFrequency)
 }

 @Test fun restartMustEstablishStablePitchAgain() {
  val model = TunerFactory.createViewModel()
  model.listening(true)
  repeat(3) { model.process(wave(110.0),48000) }
  assertNotNull(model.state.value.detectedFrequency)
  model.listening(false)
  model.listening(true)
  model.process(wave(110.0),48000)
  assertNull(model.state.value.detectedFrequency)
  model.process(wave(110.0),48000)
  assertNotNull(model.state.value.detectedFrequency)
 }

}

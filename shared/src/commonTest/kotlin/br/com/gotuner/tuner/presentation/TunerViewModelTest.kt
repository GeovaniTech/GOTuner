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

 private class FakeTonePlayer : TonePlayer {
  var samples: FloatArray? = null; var sampleRate = 0; var stops = 0; var finish: (() -> Unit)? = null
  override fun play(samples: FloatArray, sampleRate: Int, onFinished: () -> Unit) { this.samples = samples; this.sampleRate = sampleRate; finish = onFinished }
  override fun stop() { stops++ }
 }

 @Test fun selectedStringStaysTheTargetForAnotherPitch() {
  val model = TunerFactory.createViewModel(); model.listening(true)
  model.selectString(6)
  assertEquals(6, model.state.value.selectedString)
  assertEquals("E2", model.state.value.targetNote)
  assertEquals(82.41, assertNotNull(model.state.value.targetFrequency), 1e-9)
  assertNull(model.state.value.tuningStatus)
  // 100 Hz is closer to A2, but the user asked for the 6th string: it reads as far too sharp.
  repeat(4) { model.process(wave(100.0),48000) }
  val state = model.state.value
  assertEquals("E2", state.targetNote); assertEquals(6, state.targetString)
  assertEquals(TuningStatus.SHARP, state.tuningStatus); assertEquals(AccuracyColor.RED, state.accuracyColor)
  assertEquals(335.0, assertNotNull(state.deviationCents), 2.0)
 }
 @Test fun selectedStringIgnoresItsOwnHarmonic() {
  val model = TunerFactory.createViewModel(); model.listening(true)
  model.selectString(6)
  repeat(4) { model.process(wave(164.82),48000) }
  val state = model.state.value
  assertEquals(164.82, assertNotNull(state.detectedFrequency), 0.5)
  assertEquals("E2", state.targetNote)
  assertNull(state.tuningStatus); assertNull(state.deviationCents); assertNull(state.accuracyColor)
 }
 @Test fun changingStringClearsReadingAndAutoReturns() {
  val model = TunerFactory.createViewModel(); model.listening(true)
  repeat(4) { model.process(wave(110.0),48000) }
  assertEquals(5, model.state.value.targetString); assertNull(model.state.value.selectedString)
  model.selectString(4)
  assertNull(model.state.value.detectedFrequency); assertNull(model.state.value.tuningStatus)
  assertEquals("D3", model.state.value.targetNote); assertTrue(model.state.value.isListening)
  model.process(wave(146.83),48000)
  assertNull(model.state.value.detectedFrequency)
  model.process(wave(146.83),48000)
  assertEquals(TuningStatus.IN_TUNE, model.state.value.tuningStatus)
  model.selectString(null)
  assertNull(model.state.value.selectedString); assertNull(model.state.value.targetNote); assertNull(model.state.value.targetString)
  repeat(4) { model.process(wave(196.0),48000) }
  assertEquals("G3", model.state.value.targetNote)
  model.selectString(9)
  assertNull(model.state.value.selectedString)
 }
 @Test fun selectionAndTunedStringsSurviveRestartAndErrors() {
  val model = TunerFactory.createViewModel(); model.listening(true)
  repeat(8) { model.process(wave(110.0),48000) }
  model.selectString(4)
  model.listening(false); model.listening(true)
  assertEquals(4, model.state.value.selectedString); assertEquals("D3", model.state.value.targetNote)
  assertEquals(setOf(5), model.state.value.tunedStrings)
  model.fail("Capture failed")
  assertEquals(4, model.state.value.selectedString); assertEquals(setOf(5), model.state.value.tunedStrings)
  assertFalse(model.state.value.isListening)
 }
 @Test fun stringIsMarkedTunedOnlyAfterAStableVerdict() {
  val model = TunerFactory.createViewModel(); model.listening(true)
  repeat(4) { model.process(wave(110.0),48000) }
  assertEquals(TuningStatus.IN_TUNE, model.state.value.tuningStatus)
  assertTrue(model.state.value.tunedStrings.isEmpty())
  repeat(2) { model.process(wave(110.0),48000) }
  assertEquals(setOf(5), model.state.value.tunedStrings)
  repeat(8) { model.process(wave(146.83),48000) }
  assertEquals(setOf(5, 4), model.state.value.tunedStrings)
  // One attack spike does not unmark the string; a string that settles flat does.
  model.process(wave(146.83 * 2.0.pow(40.0 / 1200)),48000)
  assertEquals(setOf(5, 4), model.state.value.tunedStrings)
  repeat(12) { model.process(wave(146.83 * 2.0.pow(-30.0 / 1200)),48000) }
  assertEquals(TuningStatus.FLAT, model.state.value.tuningStatus)
  assertEquals(setOf(5), model.state.value.tunedStrings)
 }
 @Test fun referenceTonePlaysTheTargetAndPausesReadings() {
  val player = FakeTonePlayer()
  val model = TunerFactory.createViewModel(player); model.listening(true)
  model.toggleReferenceTone()
  assertNull(player.samples); assertFalse(model.state.value.isPlayingReference)
  model.selectString(5)
  model.toggleReferenceTone()
  assertTrue(model.state.value.isPlayingReference)
  assertEquals(ReferenceTone.SAMPLE_RATE, player.sampleRate)
  val estimate = assertNotNull(PitchDetector().detect(assertNotNull(player.samples).copyOfRange(4096, 8192), player.sampleRate))
  assertEquals(110.0, estimate.frequency, 0.3)
  // The microphone hears the phone's own tone: it must not become a reading.
  repeat(10) { model.process(wave(110.0),48000) }
  assertNull(model.state.value.detectedFrequency); assertTrue(model.state.value.tunedStrings.isEmpty())
  assertNotNull(player.finish)()
  assertFalse(model.state.value.isPlayingReference)
  repeat(TunerViewModel.REFERENCE_TAIL_FRAMES) { model.process(wave(110.0),48000) }
  assertNull(model.state.value.detectedFrequency)
  repeat(2) { model.process(wave(108.0),48000) }
  assertEquals(TuningStatus.FLAT, model.state.value.tuningStatus)
 }
 @Test fun referenceToneStopsOnSecondToggleAndOnStringChange() {
  val player = FakeTonePlayer()
  val model = TunerFactory.createViewModel(player); model.listening(true)
  repeat(4) { model.process(wave(196.0),48000) }
  model.toggleReferenceTone()
  assertTrue(model.state.value.isPlayingReference)
  assertEquals(196.0, assertNotNull(PitchDetector().detect(assertNotNull(player.samples).copyOfRange(4096, 8192), player.sampleRate)).frequency, 0.5)
  assertEquals("G3", model.state.value.targetNote)
  val staleFinish = assertNotNull(player.finish)
  model.toggleReferenceTone()
  assertFalse(model.state.value.isPlayingReference); assertEquals(1, player.stops)
  model.toggleReferenceTone()
  staleFinish()
  assertTrue(model.state.value.isPlayingReference)
  model.selectString(1)
  assertFalse(model.state.value.isPlayingReference); assertEquals(2, player.stops)
 }

}

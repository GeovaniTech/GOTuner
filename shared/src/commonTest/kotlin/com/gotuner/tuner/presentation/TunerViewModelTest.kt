package com.gotuner.tuner.presentation

import kotlin.test.*
import kotlin.math.*
import com.gotuner.tuner.wave
import com.gotuner.tuner.domain.audio.*
import com.gotuner.tuner.domain.tuning.*
import com.gotuner.tuner.domain.model.*
import com.gotuner.tuner.presentation.*
import com.gotuner.app.TunerFactory

class TunerViewModelTest {
 @Test fun readingRemainsUntilHoldDeadline() {
  var time = 0L
  val model = TunerViewModel(PitchDetector(), TuningCalculator()) { time }; model.permission(true); model.listening(true)
  repeat(3) { model.process(wave(110.0),48000) }
  val reading = model.state.value
  time = 130
  repeat(3) { model.process(FloatArray(4096),48000) }
  assertEquals(reading, model.state.value)
  time = TunerViewModel.PITCH_HOLD_MILLIS - 1
  model.process(FloatArray(4096),44100)
  assertEquals(reading, model.state.value)
  time++
  model.process(FloatArray(4096),44100)
  assertNull(model.state.value.detectedFrequency)
  assertNull(model.state.value.targetNote)
  assertNull(model.state.value.deviationCents)
  assertTrue(model.state.value.isListening)
 }
 @Test fun newPitchUpdatesImmediatelyAndRestartsHold() {
  var time = 0L
  val model = TunerViewModel(PitchDetector(), TuningCalculator()) { time }; model.listening(true)
  repeat(3) { model.process(wave(110.0),48000) }
  time = 1000
  repeat(3) { model.process(wave(146.83),48000) }
  assertEquals("D3",model.state.value.targetNote)
  time = 1500
  model.process(FloatArray(4096),48000)
  assertEquals("D3",model.state.value.targetNote)
  time = 2500
  model.process(FloatArray(4096),48000)
  assertNull(model.state.value.detectedFrequency)
 }
 @Test fun stopAndErrorClearHeldReadingImmediately() {
  val model = TunerViewModel(PitchDetector(), TuningCalculator()) { 0L }; model.listening(true)
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
  val model = TunerViewModel(PitchDetector(), TuningCalculator(TuningConfig(greenCents = 25.0, yellowCents = 35.0)))
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

}

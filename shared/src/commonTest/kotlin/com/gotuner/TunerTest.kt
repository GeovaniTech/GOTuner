package com.gotuner
import kotlin.test.*
import kotlin.math.*

class TunerTest {
 private val calculator = TuningCalculator()
 @Test fun references() { calculator.strings.forEach { val result = calculator.calculate(it.frequency)!!; assertEquals(it, result.target); assertEquals(0.0, result.cents, 1e-9); assertEquals(AccuracyColor.GREEN,result.color) } }
 @Test fun flatSharp() { for ((frequency,cents) in listOf(108.74 to -20.0,111.28 to 20.0)) { val result=calculator.calculate(frequency)!!; assertEquals("A2",result.target.note); assertEquals(cents,result.cents,0.2); assertEquals(if(cents<0) TuningStatus.FLAT else TuningStatus.SHARP,result.status) } }
 @Test fun boundaries() {
  for ((cents,color) in listOf(5.0 to AccuracyColor.GREEN,-5.0 to AccuracyColor.GREEN,15.0 to AccuracyColor.YELLOW,-15.0 to AccuracyColor.YELLOW,4.999 to AccuracyColor.GREEN,5.001 to AccuracyColor.YELLOW,14.999 to AccuracyColor.YELLOW,15.001 to AccuracyColor.RED,-15.001 to AccuracyColor.RED)) assertEquals(color,calculator.calculate(110*2.0.pow(cents/1200))!!.color)
  val configured=TuningCalculator(TuningConfig(greenCents=10.0,yellowCents=20.0)); assertEquals(AccuracyColor.GREEN,configured.calculate(110*2.0.pow(8.0/1200))!!.color)
 }
 @Test fun invalidAndOctaves() { listOf(0.0,-1.0,Double.NaN,Double.POSITIVE_INFINITY,40.0,440.0,659.26,220.0).forEach { assertNull(calculator.calculate(it),"$it") }; assertEquals("E4",calculator.calculate(329.63)!!.target.note) }
 private fun wave(frequency: Double, rate: Int=48000, harmonic: Boolean=false) = FloatArray(4096) { i -> val phase=2*PI*frequency*i/rate; (if(harmonic) 0.13*sin(phase)+0.35*sin(2*phase)+0.18*sin(3*phase) else 0.4*sin(phase)).toFloat() }
 @Test fun sineWaves() { for(rate in listOf(44100,48000)) calculator.strings.forEach { val pitch=PitchDetector().detect(wave(it.frequency,rate),rate)!!; assertEquals(it.frequency,pitch.frequency,0.5); assertTrue(pitch.confidence>0.9) } }
 @Test fun harmonics() { calculator.strings.forEach { val pitch=PitchDetector().detect(wave(it.frequency,harmonic=true),48000)!!; assertEquals(it.frequency,pitch.frequency,0.6) } }
 @Test fun silenceNoiseAndBadInput() {
  val detector=PitchDetector(); assertNull(detector.detect(FloatArray(4096),48000)); assertNull(detector.detect(FloatArray(4096){0.0001f},48000)); assertNull(detector.detect(FloatArray(10),48000)); assertNull(detector.detect(wave(110.0),0))
  val random=kotlin.random.Random(42); assertNull(detector.detect(FloatArray(4096){random.nextFloat()-0.5f},48000))
 }
 @Test fun realOctaveIsNotFolded() { val result=PitchDetector().detect(wave(329.63),48000)!!; assertEquals(329.63,result.frequency,0.5); assertEquals("E4",calculator.calculate(result.frequency)!!.target.note) }
 @Test fun readingRemainsUntilHoldDeadline() {
  var time = 0L
  val model = TunerViewModel { time }; model.permission(true); model.listening(true)
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
  val model = TunerViewModel { time }; model.listening(true)
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
  val model = TunerViewModel { 0L }; model.listening(true)
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
  val model = TunerViewModel(); model.listening(true)
  val quiet = FloatArray(4096) { (0.003 * sin(2 * PI * 110 * it / 48000)).toFloat() }
  repeat(4) { model.process(quiet, 48000) }
  assertEquals(110.0, assertNotNull(model.state.value.detectedFrequency), 0.5)
  assertEquals("A2", model.state.value.targetNote)
 }
 @Test fun unsupportedPitchShowsHertzWithoutTuningAdvice() {
  val model = TunerViewModel(); model.listening(true)
  repeat(4) { model.process(wave(110.0),48000) }
  repeat(4) { model.process(wave(180.0),48000) }
  assertEquals(180.0, assertNotNull(model.state.value.detectedFrequency), 0.5)
  assertNull(model.state.value.targetNote)
  assertNull(model.state.value.tuningStatus)
  assertNull(model.state.value.deviationCents)
  assertNull(model.state.value.accuracyColor)
 }

}

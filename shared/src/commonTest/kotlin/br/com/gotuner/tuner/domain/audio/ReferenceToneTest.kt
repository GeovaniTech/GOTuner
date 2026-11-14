package br.com.gotuner.tuner.domain.audio

import kotlin.test.*
import kotlin.math.*
import br.com.gotuner.tuner.domain.model.StandardTuning

class ReferenceToneTest {
 @Test fun everyStringToneIsDetectedAtItsExactPitch() {
  StandardTuning.strings.forEach { string ->
   val tone = ReferenceTone.synthesize(string.frequency)
   // Early, middle and late in the decay: the pitch must not drift.
   for (start in listOf(2000, 30000, 70000)) {
    val estimate = assertNotNull(PitchDetector().detect(tone.copyOfRange(start, start + 4096), ReferenceTone.SAMPLE_RATE), "${string.note} at $start")
    assertEquals(0.0, 1200 * log2(estimate.frequency / string.frequency), 3.0, "${string.note} at $start")
   }
  }
 }
 @Test fun toneIsBoundedAndFadesWithoutClicks() {
  val tone = ReferenceTone.synthesize(82.41)
  assertEquals((ReferenceTone.DURATION_SECONDS * ReferenceTone.SAMPLE_RATE).toInt(), tone.size)
  assertTrue(tone.all { it.isFinite() && abs(it) <= 0.8f + 1e-6f })
  assertTrue(tone.maxOf { abs(it) } > 0.5f)
  assertTrue(abs(tone.first()) < 0.01f && abs(tone.last()) < 0.01f)
 }
 @Test fun lowStringsCarryAudibleOvertones() {
  // Phone speakers lose the fundamental of E2; energy above 300 Hz must remain to convey the pitch.
  val rate = ReferenceTone.SAMPLE_RATE
  val tone = ReferenceTone.synthesize(82.41)
  fun level(frequency: Double): Double {
   var re = 0.0; var im = 0.0
   for (i in 0 until rate / 2) { re += tone[i] * cos(2 * PI * frequency * i / rate); im += tone[i] * sin(2 * PI * frequency * i / rate) }
   return hypot(re, im)
  }
  val overtones = (4..9).sumOf { level(82.41 * it) }
  assertTrue(overtones > 0.5 * level(82.41), "overtones too weak")
 }
}

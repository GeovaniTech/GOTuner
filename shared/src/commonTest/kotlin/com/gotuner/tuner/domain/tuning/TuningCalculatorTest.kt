package com.gotuner.tuner.domain.tuning

import kotlin.test.*
import kotlin.math.*
import com.gotuner.tuner.wave
import com.gotuner.tuner.domain.audio.*
import com.gotuner.tuner.domain.tuning.*
import com.gotuner.tuner.domain.model.*
import com.gotuner.tuner.presentation.*
import com.gotuner.app.TunerFactory

class TuningCalculatorTest {
 private val calculator = TuningCalculator()
 @Test fun references() { calculator.strings.forEach { val result = calculator.calculate(it.frequency)!!; assertEquals(it, result.target); assertEquals(0.0, result.cents, 1e-9); assertEquals(AccuracyColor.GREEN,result.color) } }
 @Test fun flatSharp() { for ((frequency,cents) in listOf(108.74 to -20.0,111.28 to 20.0)) { val result=calculator.calculate(frequency)!!; assertEquals("A2",result.target.note); assertEquals(cents,result.cents,0.2); assertEquals(if(cents<0) TuningStatus.FLAT else TuningStatus.SHARP,result.status) } }
 @Test fun boundaries() {
  for ((cents,color) in listOf(5.0 to AccuracyColor.GREEN,-5.0 to AccuracyColor.GREEN,15.0 to AccuracyColor.YELLOW,-15.0 to AccuracyColor.YELLOW,4.999 to AccuracyColor.GREEN,5.001 to AccuracyColor.YELLOW,14.999 to AccuracyColor.YELLOW,15.001 to AccuracyColor.RED,-15.001 to AccuracyColor.RED)) assertEquals(color,calculator.calculate(110*2.0.pow(cents/1200))!!.color)
  val configured=TuningCalculator(TuningConfig(greenCents=10.0,yellowCents=20.0)); assertEquals(AccuracyColor.GREEN,configured.calculate(110*2.0.pow(8.0/1200))!!.color)
 }
 @Test fun invalidAndOctaves() { listOf(0.0,-1.0,Double.NaN,Double.POSITIVE_INFINITY,40.0,440.0,659.26,220.0).forEach { assertNull(calculator.calculate(it),"$it") }; assertEquals("E4",calculator.calculate(329.63)!!.target.note) }
}

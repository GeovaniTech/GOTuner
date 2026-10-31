package br.com.gotuner.tuner

import kotlin.math.*

internal fun wave(frequency: Double, rate: Int=48000, harmonic: Boolean=false) = FloatArray(4096) { i -> val phase=2*PI*frequency*i/rate; (if(harmonic) 0.13*sin(phase)+0.35*sin(2*phase)+0.18*sin(3*phase) else 0.4*sin(phase)).toFloat() }

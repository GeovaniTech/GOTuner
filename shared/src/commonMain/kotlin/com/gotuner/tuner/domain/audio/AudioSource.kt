package com.gotuner.tuner.domain.audio

/** Platform capture lifecycle. PCM samples are delivered on a single audio worker. */
interface AudioSource {
    fun start()
    fun stop()
}

/** Output port for capture events; platform recorders never depend on presentation. */
interface AudioInputObserver {
    fun permission(granted: Boolean)
    fun listening(active: Boolean)
    fun fail(message: String)
    fun process(samples: FloatArray, sampleRate: Int)
}

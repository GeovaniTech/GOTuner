package br.com.gotuner.android.tuner.data

import br.com.gotuner.android.R

import android.annotation.SuppressLint
import android.content.Context
import android.media.*
import br.com.gotuner.tuner.domain.audio.*

class AndroidAudioRecorder(context: Context, private val observer: AudioInputObserver) : AudioSource {
    // Unsupported UNPROCESSED falls back to DEFAULT, which may suppress guitar audio.
    private val source = if (context.getSystemService(AudioManager::class.java)
        .getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true")
        MediaRecorder.AudioSource.UNPROCESSED else MediaRecorder.AudioSource.VOICE_RECOGNITION
    private val appContext = context.applicationContext
    private val controller = AudioCaptureController(::createInput, observer, appContext.getString(R.string.microphone_access_error))

    @SuppressLint("MissingPermission")
    private fun createInput(): PcmInput {
        check(appContext.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED)
        for (rate in listOf(48000, 44100)) {
            val minimum = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (minimum <= 0) continue
            val candidate = runCatching { AudioRecord(source, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minimum, 16384)) }.getOrNull() ?: continue
            if (candidate.state == AudioRecord.STATE_INITIALIZED) return NativePcmInput(candidate)
            candidate.release()
        }
        error("Microphone initialization failed")
    }

    override fun start() = controller.start()
    override fun stop() = controller.stop()
}

private class NativePcmInput(private val audio: AudioRecord) : PcmInput {
    override val sampleRate get() = audio.sampleRate
    override fun start() {
        audio.startRecording()
        check(audio.recordingState == AudioRecord.RECORDSTATE_RECORDING)
    }
    override fun read(samples: ShortArray) = audio.read(samples, 0, samples.size, AudioRecord.READ_BLOCKING)
    override fun stop() = audio.stop()
    override fun release() = audio.release()
}

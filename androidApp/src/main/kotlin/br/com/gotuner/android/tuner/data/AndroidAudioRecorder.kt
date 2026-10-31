package br.com.gotuner.android.tuner.data

import br.com.gotuner.android.R

import android.annotation.SuppressLint
import android.content.Context
import android.media.*
import br.com.gotuner.tuner.domain.audio.*
import kotlinx.coroutines.*

class AndroidAudioRecorder(context: Context, private val observer: AudioInputObserver) : AudioSource {
    // Unsupported UNPROCESSED falls back to DEFAULT, which may suppress guitar audio.
    private val source = if (context.getSystemService(AudioManager::class.java)
        .getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true")
        MediaRecorder.AudioSource.UNPROCESSED else MediaRecorder.AudioSource.VOICE_RECOGNITION
    private val appContext = context.applicationContext
    private var recorder: AudioRecord? = null
    private var job: Job? = null
    @SuppressLint("MissingPermission")
    override fun start() {
        if (job != null) return
        try {
            var selected: AudioRecord? = null
            for (rate in listOf(48000, 44100)) {
                val minimum = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                if (minimum <= 0) continue
                val candidate = runCatching { AudioRecord(source, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minimum, 16384)) }.getOrNull() ?: continue
                if (candidate.state == AudioRecord.STATE_INITIALIZED) { selected = candidate; break } else candidate.release()
            }
            val audio = selected ?: error("Microphone initialization failed")
            recorder = audio
            audio.startRecording()
            check(audio.recordingState == AudioRecord.RECORDSTATE_RECORDING)
            observer.listening(true)
            job = CoroutineScope(Dispatchers.Default).launch {
                val pcm = ShortArray(2048); val frame = FloatArray(4096)
                var filled = 0
                try {
                    while (isActive) {
                        val count = audio.read(pcm, 0, pcm.size, AudioRecord.READ_BLOCKING)
                        if (!isActive) break
                        check(count > 0) { "Microphone read failed" }
                        for (i in 0 until count) {
                            frame[filled++] = pcm[i] / 32768f
                            if (filled == frame.size) {
                                observer.process(frame, audio.sampleRate)
                                frame.copyInto(frame, 0, 2048, 4096); filled = 2048
                            }
                        }
                    }
                } catch (e: Exception) { if (isActive) observer.fail(appContext.getString(R.string.microphone_read_error)) }
            }
        } catch (e: Exception) { stop(); observer.fail(appContext.getString(R.string.microphone_access_error)) }
    }
    override fun stop() {
        job?.cancel()
        runCatching { recorder?.stop() }
        // Join before releasing so no read or pitch processing can race a subsequent start.
        job?.let { runBlocking { it.join() } }
        job = null; recorder?.release(); recorder = null
        observer.listening(false)
    }
}

package br.com.gotuner.android.tuner.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import br.com.gotuner.tuner.domain.audio.TonePlayer

/** Plays a synthesized reference tone once through the media stream. Use from the main thread. */
class AndroidTonePlayer : TonePlayer {
    private val main = Handler(Looper.getMainLooper())
    private var track: AudioTrack? = null
    private var pendingFinish: Runnable? = null

    override fun play(samples: FloatArray, sampleRate: Int, onFinished: () -> Unit) {
        stop()
        val started = runCatching { start(samples, sampleRate) }.isSuccess
        if (!started) release()
        val finish = Runnable {
            pendingFinish = null
            release()
            onFinished()
        }
        pendingFinish = finish
        // A timer is more dependable than a marker at the very end of a static track.
        // A tone that could not start still reports back, so the tuner resumes listening.
        main.postDelayed(finish, if (started) samples.size * 1000L / sampleRate + OUTPUT_LATENCY_MS else 0)
    }

    override fun stop() {
        pendingFinish?.let(main::removeCallbacks)
        pendingFinish = null
        release()
    }

    private fun start(samples: FloatArray, sampleRate: Int) {
        val pcm = ShortArray(samples.size) { (samples[it].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort() }
        val audio = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * Short.SIZE_BYTES)
            .build()
        track = audio
        check(audio.write(pcm, 0, pcm.size) == pcm.size) { "Reference tone was not buffered" }
        audio.play()
    }

    private fun release() {
        track?.let { audio ->
            runCatching { audio.stop() }
            runCatching { audio.release() }
        }
        track = null
    }

    private companion object {
        const val OUTPUT_LATENCY_MS = 120L
    }
}

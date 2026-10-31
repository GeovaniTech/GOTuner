package br.com.gotuner.android.tuner.data

import br.com.gotuner.tuner.domain.audio.*
import kotlinx.coroutines.*

internal interface PcmInput {
    val sampleRate: Int
    fun start()
    fun read(samples: ShortArray): Int
    fun stop()
    fun release()
}

/** Owns one native session at a time; cleanup always finishes before a restart. */
internal class AudioCaptureController(
    private val createInput: () -> PcmInput,
    private val observer: AudioInputObserver,
    private val errorMessage: String,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val callbackDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
) : AudioSource {
    @Volatile private var recorder: PcmInput? = null
    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + workerDispatcher)

    override fun start() {
        if (job?.isActive == true) return
        val previous = job
        job = scope.launch {
            // A rapid stop/start waits for cleanup on a worker, never on the UI thread.
            previous?.join()
            ensureActive()
            var audio: PcmInput? = null
            try {
                val capture = createInput()
                audio = capture
                recorder = capture
                ensureActive()
                capture.start()
                withContext(callbackDispatcher) { observer.listening(true) }
                val pcm = ShortArray(2048)
                val frame = FloatArray(4096)
                var filled = 0
                while (isActive) {
                    val count = capture.read(pcm)
                    ensureActive()
                    check(count > 0) { "Microphone read failed" }
                    for (i in 0 until count) {
                        frame[filled++] = pcm[i] / 32768f
                        if (filled == frame.size) {
                            ensureActive()
                            observer.process(frame, capture.sampleRate)
                            frame.copyInto(frame, 0, 2048, 4096)
                            filled = 2048
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                withContext(callbackDispatcher) {
                    observer.fail(errorMessage)
                }
            } finally {
                // Always release, including initialization, read and processing failures.
                runCatching { audio?.stop() }
                runCatching { audio?.release() }
                if (recorder === audio) recorder = null
            }
        }
    }

    override fun stop() {
        job?.cancel()
        // Unblock a native blocking read; ownership/release remains with the worker.
        runCatching { recorder?.stop() }
        observer.listening(false)
    }
}

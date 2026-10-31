package br.com.gotuner.android.tuner.data

import br.com.gotuner.tuner.domain.audio.AudioInputObserver
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class AudioCaptureControllerTest {
    private class Observer : AudioInputObserver {
        val failure = CountDownLatch(1)
        override fun permission(granted: Boolean) {}
        override fun listening(active: Boolean) {}
        override fun fail(message: String) { failure.countDown() }
        override fun process(samples: FloatArray, sampleRate: Int) {}
    }
    private class Device(private val broken: Boolean = false) : PcmInput {
        override val sampleRate = 48000
        val reading = CountDownLatch(1)
        val stopped = CountDownLatch(1)
        val released = CountDownLatch(1)
        override fun start() {}
        override fun read(samples: ShortArray): Int {
            reading.countDown()
            if (broken) return -3
            check(stopped.await(5, TimeUnit.SECONDS))
            return -3
        }
        override fun stop() { stopped.countDown() }
        override fun release() { released.countDown() }
    }
    @Test fun stopUnblocksReadAndReleasesWithoutJoiningCaller() {
        val device = Device()
        val controller = AudioCaptureController({ device }, Observer(), "Unavailable", callbackDispatcher = Dispatchers.Default)
        controller.start()
        assertTrue(device.reading.await(5, TimeUnit.SECONDS))
        controller.stop()
        assertTrue(device.released.await(5, TimeUnit.SECONDS))
    }
    @Test fun readFailureReleasesDevice() {
        val device = Device(broken = true)
        val observer = Observer()
        val controller = AudioCaptureController({ device }, observer, "Unavailable", callbackDispatcher = Dispatchers.Default)
        controller.start()
        assertTrue(observer.failure.await(5, TimeUnit.SECONDS))
        assertTrue(device.released.await(5, TimeUnit.SECONDS))
        controller.stop()
    }
    @Test fun duplicateStartDoesNotOpenTwoSessionsAndRestartWaitsForCleanup() {
        val devices = listOf(Device(), Device())
        val opens = AtomicInteger()
        val controller = AudioCaptureController({
            val i = opens.getAndIncrement()
            if (i > 0) assertEquals(0L, devices[0].released.count)
            devices[i]
        }, Observer(), "Unavailable", callbackDispatcher = Dispatchers.Default)
        controller.start()
        assertTrue(devices[0].reading.await(5, TimeUnit.SECONDS))
        controller.start()
        assertEquals(1, opens.get())
        controller.stop()
        controller.start()
        assertTrue(devices[1].reading.await(5, TimeUnit.SECONDS))
        controller.stop()
        assertTrue(devices[1].released.await(5, TimeUnit.SECONDS))
        assertEquals(2, opens.get())
    }
    @Test fun deniedPermissionOrInitializationFailureIsReported() {
        val observer = Observer()
        val controller = AudioCaptureController({ throw SecurityException("Denied") }, observer, "Unavailable", callbackDispatcher = Dispatchers.Default)
        controller.start()
        assertTrue(observer.failure.await(5, TimeUnit.SECONDS))
        controller.stop()
    }
}

package br.com.gotuner.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private fun assertMicrophoneReleased() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = context.getSystemService(AudioManager::class.java)
    val deadline = System.nanoTime() + 3_000_000_000L
    while (manager.activeRecordingConfigurations.isNotEmpty() && System.nanoTime() < deadline) Thread.sleep(25)
    assertTrue("Microphone must be released", manager.activeRecordingConfigurations.isEmpty())
}

@RunWith(AndroidJUnit4::class)
class PermissionDeniedTest {
    @Test fun launchWithoutPermissionDoesNotRecord() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assumeTrue(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
            assertMicrophoneReleased()
        }
    }
}

@RunWith(AndroidJUnit4::class)
class ReleaseSmokeTest {
    @get:Rule val microphone: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)
    @Test fun repeatedBackgroundAndRecreateReleaseMicrophone() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            repeat(3) {
                scenario.moveToState(Lifecycle.State.CREATED)
                assertMicrophoneReleased()
                scenario.moveToState(Lifecycle.State.RESUMED)
                scenario.recreate()
                assertEquals(Lifecycle.State.RESUMED, scenario.state)
            }
            scenario.moveToState(Lifecycle.State.CREATED)
            assertMicrophoneReleased()
        }
        assertMicrophoneReleased()
    }
}

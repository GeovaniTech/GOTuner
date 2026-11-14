package br.com.gotuner.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.gotuner.tuner.domain.audio.AudioSource
import br.com.gotuner.app.TunerFactory
import br.com.gotuner.android.tuner.data.AndroidAudioRecorder
import br.com.gotuner.android.tuner.data.AndroidTonePlayer
import br.com.gotuner.android.tuner.presentation.TunerScreen
import br.com.gotuner.android.ui.theme.GOTunerTheme
import androidx.core.net.toUri
import androidx.core.content.edit

class MainActivity : ComponentActivity() {
    private val model = TunerFactory.createViewModel(AndroidTonePlayer())
    private val audio: AudioSource by lazy { AndroidAudioRecorder(this, model) }
    private var foreground = false
    private var permissionBlocked by mutableStateOf(false)
    private val preferences by lazy { getPreferences(MODE_PRIVATE) }
    private fun refreshPermission() {
        val granted =
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        model.permission(granted)
        permissionBlocked = !granted && preferences.getBoolean(
            "microphoneRequested",
            false
        ) && !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
    }

    private val permission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> refreshPermission(); if (granted && foreground) audio.start() }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            GOTunerTheme {
                val state by model.state.collectAsStateWithLifecycle()

                TunerScreen(
                    state = state,
                    permissionBlocked = permissionBlocked,
                    strings = model.strings,
                    onSelectString = model::selectString,
                    onToggleReference = model::toggleReferenceTone
                ) {
                    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        audio.stop(); audio.start()
                    } else if (permissionBlocked) startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            "package:$packageName".toUri()
                        )
                    )
                    else {
                        preferences.edit {
                            putBoolean("microphoneRequested", true)
                        }

                        permission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        foreground = true
        refreshPermission()

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            audio.start()
        }
    }

    override fun onPause() {
        foreground = false
        model.stopReferenceTone()
        audio.stop()
        super.onPause()
    }
}

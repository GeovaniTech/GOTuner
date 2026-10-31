package br.com.gotuner.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.colorResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.gotuner.tuner.domain.audio.AudioSource
import br.com.gotuner.app.TunerFactory
import br.com.gotuner.android.tuner.data.AndroidAudioRecorder
import br.com.gotuner.android.tuner.presentation.TunerScreen

class MainActivity : ComponentActivity() {
 private val model = TunerFactory.createViewModel()
 private val audio: AudioSource by lazy { AndroidAudioRecorder(this, model) }
 private var foreground = false
 private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> model.permission(granted); if (granted && foreground) audio.start() }
 override fun onCreate(savedInstanceState: Bundle?) {
  installSplashScreen()
  super.onCreate(savedInstanceState)
  setContent { MaterialTheme(colorScheme = darkColorScheme(background = colorResource(R.color.launcher_background), surface = colorResource(R.color.launcher_background))) {
   val state by model.state.collectAsStateWithLifecycle()
   TunerScreen(state) { if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) { audio.stop(); audio.start() } else permission.launch(Manifest.permission.RECORD_AUDIO) }
  } }
 }
 override fun onStart() { super.onStart(); foreground = true; val granted = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED; model.permission(granted); if (granted) audio.start() }
 override fun onStop() { foreground = false; audio.stop(); super.onStop() }
}

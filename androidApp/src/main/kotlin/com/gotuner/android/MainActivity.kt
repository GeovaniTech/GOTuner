package com.gotuner.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotuner.*
import java.util.Locale

class MainActivity : ComponentActivity() {
 private val model = TunerViewModel()
 private val audio by lazy { AndroidAudioRecorder(this, model) }
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
private fun Double.fmt() = String.format(Locale.getDefault(), "%.1f", this)
@Composable fun TunerScreen(state: TunerUiState, onEnable: () -> Unit) {
 val tint = when(state.accuracyColor) { AccuracyColor.GREEN -> Color(0xFF66E3AA); AccuracyColor.YELLOW -> Color(0xFFFFD166); AccuracyColor.RED -> Color(0xFFFF737A); else -> Color(0xFF9DA6BA) }
 Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
 Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
  Text(stringResource(R.string.app_name), fontSize = 28.sp, fontWeight = FontWeight.Bold)
  ListeningStatus(state.isListening)
  Spacer(Modifier.height(36.dp))
  Text(state.detectedFrequency?.let { it.fmt() } ?: "--.-", fontSize = 72.sp, fontWeight = FontWeight.Bold, color = tint)
  Text(stringResource(R.string.frequency_unit), color = tint)
  state.targetNote?.let { Text(stringResource(R.string.target_reference, it, state.targetFrequency?.fmt().orEmpty()), fontSize = 20.sp) }
  TuningIndicator(state.deviationCents, tint)
  Text(state.deviationCents?.let { stringResource(R.string.cents_value, "${if(it > 0) "+" else ""}${it.fmt()}") } ?: stringResource(R.string.cents_empty), color = tint)
  val instruction = when(state.tuningStatus) {
   TuningStatus.FLAT -> R.string.tighten_string
   TuningStatus.SHARP -> R.string.loosen_string
   else -> null
  }
  instruction?.let {
   Surface(color = tint.copy(alpha = 0.12f), shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
    Text(stringResource(it), modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp), fontSize = 32.sp,
     fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = tint)
   }
  }
  if(state.microphonePermissionState != MicrophonePermissionState.GRANTED) { Text(stringResource(R.string.permission_rationale)); Button(onClick = onEnable) { Text(stringResource(R.string.enable_microphone)) } }
  state.errorMessage?.let { Text(it); Button(onClick = onEnable) { Text(stringResource(R.string.retry)) } }
 }
 }
}
@Composable fun TuningIndicator(cents: Double?, tint: Color) {
 val marker by animateFloatAsState(((cents ?: 0.0).coerceIn(-50.0,50.0) / 50).toFloat(), label = "pitch")
 Column {
 Canvas(Modifier.fillMaxWidth().height(72.dp)) {
  val y = size.height / 2; val left = 12.dp.toPx(); val width = size.width - 2*left
  drawLine(Color(0xFF41495C), Offset(left,y),Offset(left+width,y),4.dp.toPx())
  for (i in 0..10) { val x = left+width*i/10; drawLine(if(i==5) Color(0xFF66E3AA) else Color(0xFF70788A),Offset(x,y-10),Offset(x,y+10),2.dp.toPx()) }
  if(cents != null) drawCircle(tint,8.dp.toPx(),Offset(left+width*(marker+1)/2,y))
 }
 Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(stringResource(R.string.flat)); Text(stringResource(R.string.in_tune)); Text(stringResource(R.string.sharp)) }
 Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(stringResource(R.string.tighten_direction), fontSize = 12.sp); Text(stringResource(R.string.loosen_direction), fontSize = 12.sp) }
 }
}

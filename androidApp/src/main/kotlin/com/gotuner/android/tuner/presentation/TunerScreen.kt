package com.gotuner.android.tuner.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gotuner.tuner.domain.model.*
import com.gotuner.tuner.presentation.*
import com.gotuner.android.R
import com.gotuner.android.tuner.presentation.components.*
import java.util.Locale

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

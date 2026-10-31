package br.com.gotuner.android.tuner.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import br.com.gotuner.android.R

@Composable fun TuningIndicator(cents: Double?, tint: Color) {
 val marker by animateFloatAsState(((cents ?: 0.0).coerceIn(-50.0,50.0) / 50).toFloat(), label = "pitch")
 Column {
 Canvas(Modifier.fillMaxWidth().height(72.dp)) {
  val y = size.height / 2; val left = 12.dp.toPx(); val width = size.width - 2*left
  drawLine(Color(0xFF41495C), Offset(left,y),Offset(left+width,y),4.dp.toPx())
  for (i in 0..10) { val x = left+width*i/10; drawLine(if(i==5) Color(0xFF66E3AA) else Color(0xFF70788A),Offset(x,y-10),Offset(x,y+10),2.dp.toPx()) }
  if(cents != null) drawCircle(tint,8.dp.toPx(),Offset(left+width*(marker+1)/2,y))
 }
 Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(stringResource(R.string.flat), Modifier.weight(1f)); Text(stringResource(R.string.in_tune), Modifier.weight(1f), textAlign = TextAlign.Center); Text(stringResource(R.string.sharp), Modifier.weight(1f), textAlign = TextAlign.End) }
 Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(stringResource(R.string.tighten_direction), fontSize = 12.sp); Text(stringResource(R.string.loosen_direction), fontSize = 12.sp) }
 }
}

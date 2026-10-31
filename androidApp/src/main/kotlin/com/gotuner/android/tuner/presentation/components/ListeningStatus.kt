package com.gotuner.android.tuner.presentation.components

import com.gotuner.android.R

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState

@Composable
fun ListeningStatus(isListening: Boolean) {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val color = if (isListening) Color(0xFF66E3AA) else Color(0xFF9DA6BA)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isListening) {
            // Remove the animation when backgrounded or paused. Compose also respects
            // the system animator duration scale, including disabled animations.
            val animate = lifecycle.isAtLeast(Lifecycle.State.RESUMED)
            val pulse = if (animate) {
                val transition = rememberInfiniteTransition(label = "listening")
                val value by transition.animateFloat(
                    initialValue = 0.65f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "listeningPulse"
                )
                value
            } else 1f
            Box(Modifier.size(12.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(10.dp).scale(pulse).alpha(pulse).background(color, CircleShape))
            }
        }
        Text(stringResource(if (isListening) R.string.listening else R.string.microphone_paused), color = color)
    }
}

package br.com.gotuner.android.tuner.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import br.com.gotuner.android.R
import br.com.gotuner.android.tuner.presentation.preview.PreviewContainer
import br.com.gotuner.android.ui.theme.GOTunerColors

@Composable
fun ListeningStatus(isListening: Boolean) {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val color = if (isListening) GOTunerColors.Green else GOTunerColors.Neutral

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isListening) {
            // Stop the pulse when backgrounded or paused. Compose also respects the
            // system animator duration scale, including disabled animations.
            val animate = lifecycle.isAtLeast(Lifecycle.State.RESUMED)

            val pulse = if (animate) {
                val transition = rememberInfiniteTransition(label = "listening")
                val value by transition.animateFloat(
                    initialValue = 0.65f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        tween(900, easing = FastOutSlowInEasing),
                        RepeatMode.Reverse
                    ),
                    label = "listeningPulse"
                )
                value
            } else 1f

            Box(
                modifier = Modifier.size(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                                .size(10.dp)
                                .scale(pulse)
                                .alpha(pulse)
                                .background(color, CircleShape)
                )
            }
        }

        Text(
            text = stringResource(if (isListening) R.string.listening else R.string.microphone_paused),
            color = color
        )
    }
}

@Preview
@Composable
private fun ListeningStatusPreview() {
    PreviewContainer {
        ListeningStatus(isListening = true)
    }
}

@Preview
@Composable
private fun ListeningStatusPausedPreview() {
    PreviewContainer {
        ListeningStatus(isListening = false)
    }
}

package br.com.gotuner.android.tuner.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.gotuner.android.R
import br.com.gotuner.android.tuner.presentation.preview.PreviewContainer
import br.com.gotuner.android.ui.theme.GOTunerColors

/** Horizontal bar from -50 to +50 cents; the dot shows how far the string is from the target. */
@Composable
fun TuningIndicator(cents: Double?, tint: Color) {
    val marker by animateFloatAsState(
        ((cents ?: 0.0).coerceIn(-50.0, 50.0) / 50).toFloat(),
        label = "pitch"
    )

    Column {
        Canvas(
            modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
        ) {
            val y = size.height / 2
            val left = 12.dp.toPx()
            val width = size.width - 2 * left

            drawLine(
                color = GOTunerColors.Track,
                start = Offset(left, y),
                end = Offset(left + width, y),
                strokeWidth = 4.dp.toPx()
            )

            for (i in 0..10) {
                val x = left + width * i / 10

                drawLine(
                    color = if (i == 5) GOTunerColors.Green else GOTunerColors.Tick,
                    start = Offset(x, y - 10),
                    end = Offset(x, y + 10),
                    strokeWidth = 2.dp.toPx()
                )
            }

            if (cents != null) {
                drawCircle(
                    color = tint,
                    radius = 8.dp.toPx(),
                    center = Offset(left + width * (marker + 1) / 2, y)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.flat),
                modifier = Modifier.weight(1f)
            )

            Text(
                text = stringResource(R.string.in_tune),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.sharp),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.tighten_direction),
                fontSize = 12.sp
            )

            Text(
                text = stringResource(R.string.loosen_direction),
                fontSize = 12.sp
            )
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun TuningIndicatorPreview() {
    PreviewContainer {
        TuningIndicator(cents = -18.0, tint = GOTunerColors.Red)
    }
}

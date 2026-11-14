package br.com.gotuner.android.tuner.presentation.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import br.com.gotuner.android.R
import br.com.gotuner.android.tuner.presentation.formatOneDecimal
import br.com.gotuner.android.tuner.presentation.preview.PreviewContainer
import br.com.gotuner.android.ui.theme.GOTunerColors

private const val MAX_FONT_SIZE = 64f

/** Large detected frequency followed by its unit; shows "--.-" before the first reading. */
@Composable
fun FrequencyDisplay(frequency: Double?, tint: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val text = frequency?.formatOneDecimal() ?: "--.-"
            // Shrink the font so long values never wrap, respecting the user's font scale.
            val size = minOf(
                MAX_FONT_SIZE,
                maxWidth.value / (0.65f * text.length * LocalDensity.current.fontScale)
            )

            Text(
                text = text,
                maxLines = 1,
                fontSize = size.sp,
                fontWeight = FontWeight.Bold,
                color = tint
            )
        }

        Text(text = stringResource(R.string.frequency_unit), color = tint)
    }
}

@Preview
@Composable
private fun FrequencyDisplayPreview() {
    PreviewContainer {
        FrequencyDisplay(frequency = 110.4, tint = GOTunerColors.Green)
    }
}

@Preview
@Composable
private fun FrequencyDisplayEmptyPreview() {
    PreviewContainer {
        FrequencyDisplay(frequency = null, tint = GOTunerColors.Neutral)
    }
}

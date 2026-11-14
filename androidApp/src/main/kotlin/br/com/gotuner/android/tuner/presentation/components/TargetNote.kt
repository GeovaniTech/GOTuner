package br.com.gotuner.android.tuner.presentation.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import br.com.gotuner.android.R
import br.com.gotuner.android.tuner.presentation.formatOneDecimal
import br.com.gotuner.android.tuner.presentation.preview.PreviewContainer
import br.com.gotuner.android.ui.theme.GOTunerColors

/**
 * String being tuned and its reference frequency, e.g. "String 5 · A2 — 110.0 Hz".
 * Before a string is chosen or detected it states that detection is automatic.
 */
@Composable
fun TargetNote(stringNumber: Int?, note: String?, targetFrequency: Double?, modifier: Modifier = Modifier) {
    val known = stringNumber != null && note != null

    Text(
        text = if (known) {
            stringResource(
                R.string.target_reference,
                stringNumber,
                note,
                targetFrequency?.formatOneDecimal().orEmpty()
            )
        } else stringResource(R.string.automatic_detection),
        fontSize = 20.sp,
        color = if (known) Color.Unspecified else GOTunerColors.Neutral,
        modifier = modifier
    )
}

@Preview
@Composable
private fun TargetNotePreview() {
    PreviewContainer {
        TargetNote(stringNumber = 5, note = "A2", targetFrequency = 110.0)
    }
}

@Preview
@Composable
private fun TargetNoteAutomaticPreview() {
    PreviewContainer {
        TargetNote(stringNumber = null, note = null, targetFrequency = null)
    }
}

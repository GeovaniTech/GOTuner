package br.com.gotuner.android.tuner.presentation.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import br.com.gotuner.android.R
import br.com.gotuner.android.tuner.presentation.formatOneDecimal
import br.com.gotuner.android.tuner.presentation.preview.PreviewContainer

/** Closest guitar string and its reference frequency, e.g. "A2 — 110.0 Hz". */
@Composable
fun TargetNote(note: String, targetFrequency: Double?, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(
            R.string.target_reference,
            note,
            targetFrequency?.formatOneDecimal().orEmpty()
        ),
        fontSize = 20.sp,
        modifier = modifier
    )
}

@Preview
@Composable
private fun TargetNotePreview() {
    PreviewContainer {
        TargetNote(note = "A2", targetFrequency = 110.0)
    }
}

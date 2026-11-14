package br.com.gotuner.android.tuner.presentation.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import br.com.gotuner.android.R
import br.com.gotuner.android.tuner.presentation.formatSignedCents
import br.com.gotuner.android.tuner.presentation.preview.PreviewContainer
import br.com.gotuner.android.ui.theme.GOTunerColors

/** Deviation from the target string in cents, or "— cents" when there is no target. */
@Composable
fun CentsReadout(cents: Double?, tint: Color, modifier: Modifier = Modifier) {
    Text(
        text = cents?.let { stringResource(R.string.cents_value, it.formatSignedCents()) }
            ?: stringResource(R.string.cents_empty),
        color = tint,
        modifier = modifier
    )
}

@Preview
@Composable
private fun CentsReadoutPreview() {
    PreviewContainer {
        CentsReadout(cents = 12.4, tint = GOTunerColors.Yellow)
    }
}

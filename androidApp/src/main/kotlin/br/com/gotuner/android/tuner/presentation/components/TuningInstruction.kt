package br.com.gotuner.android.tuner.presentation.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.gotuner.android.R
import br.com.gotuner.android.tuner.presentation.preview.PreviewContainer
import br.com.gotuner.android.ui.theme.GOTunerColors
import br.com.gotuner.tuner.domain.model.TuningStatus

/** Highlighted action for the user: tighten, loosen, or the string is in tune. Hidden without a status. */
@Composable
fun TuningInstruction(status: TuningStatus?, tint: Color, modifier: Modifier = Modifier) {
    val message = status?.messageRes() ?: return

    Surface(
        color = tint.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(message),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = tint
        )
    }
}

@StringRes
private fun TuningStatus.messageRes(): Int = when (this) {
    TuningStatus.FLAT -> R.string.tighten_string
    TuningStatus.SHARP -> R.string.loosen_string
    TuningStatus.IN_TUNE -> R.string.string_in_tune
}

@Preview
@Composable
private fun TuningInstructionInTunePreview() {
    PreviewContainer {
        TuningInstruction(status = TuningStatus.IN_TUNE, tint = GOTunerColors.Green)
    }
}

@Preview
@Composable
private fun TuningInstructionFlatPreview() {
    PreviewContainer {
        TuningInstruction(status = TuningStatus.FLAT, tint = GOTunerColors.Red)
    }
}

package br.com.gotuner.android.tuner.presentation.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

/**
 * Highlighted verdict for the user: tighten, loosen or in tune, each with a one-line reason.
 * While the reference tone sounds it says so instead, because readings are paused. Without a
 * verdict it asks for a string to be plucked, so the card keeps its place and the neck below it
 * does not jump when a reading arrives.
 */
@Composable
fun TuningInstruction(
    status: TuningStatus?,
    tint: Color,
    modifier: Modifier = Modifier,
    selectedString: Int? = null,
    selectedNote: String? = null,
    referenceNote: String? = null
) {
    val verdict = status?.verdict()
    val title: String
    val detail: String
    val color: Color

    when {
        referenceNote != null -> {
            title = stringResource(R.string.playing_reference, referenceNote)
            detail = stringResource(R.string.playing_reference_detail)
            color = GOTunerColors.Neutral
        }
        verdict != null -> {
            title = stringResource(verdict.title)
            detail = stringResource(verdict.detail)
            color = tint
        }
        selectedString != null && selectedNote != null -> {
            title = stringResource(R.string.pluck_selected_string, selectedString)
            detail = stringResource(R.string.pluck_selected_string_detail, selectedNote)
            color = GOTunerColors.Neutral
        }
        else -> {
            title = stringResource(R.string.pluck_any_string)
            detail = stringResource(R.string.pluck_any_string_detail)
            color = GOTunerColors.Neutral
        }
    }
    val icon = verdict?.icon.takeIf { referenceNote == null }

    Surface(
        color = color.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.heightIn(min = 40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = color
                    )
                }

                Text(
                    text = title,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = color
                )
            }

            Text(text = detail, fontSize = 15.sp, textAlign = TextAlign.Center, color = color)
        }
    }
}

private class Verdict(@param:StringRes val title: Int, @param:StringRes val detail: Int, val icon: ImageVector?)

/** Arrows show where the pitch has to go: up when the string is flat, down when it is sharp. */
private fun TuningStatus.verdict(): Verdict = when (this) {
    TuningStatus.FLAT -> Verdict(R.string.tighten_string, R.string.flat_detail, Icons.Filled.KeyboardArrowUp)
    TuningStatus.SHARP -> Verdict(R.string.loosen_string, R.string.sharp_detail, Icons.Filled.KeyboardArrowDown)
    TuningStatus.IN_TUNE -> Verdict(R.string.string_in_tune, R.string.in_tune_detail, null)
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

@Preview
@Composable
private fun TuningInstructionSharpPreview() {
    PreviewContainer {
        TuningInstruction(status = TuningStatus.SHARP, tint = GOTunerColors.Yellow)
    }
}

@Preview
@Composable
private fun TuningInstructionIdlePreview() {
    PreviewContainer {
        TuningInstruction(status = null, tint = GOTunerColors.Neutral)
    }
}

@Preview
@Composable
private fun TuningInstructionStringChosenPreview() {
    PreviewContainer {
        TuningInstruction(status = null, tint = GOTunerColors.Neutral, selectedString = 5, selectedNote = "A2")
    }
}

@Preview
@Composable
private fun TuningInstructionReferencePreview() {
    PreviewContainer {
        TuningInstruction(status = TuningStatus.FLAT, tint = GOTunerColors.Red, referenceNote = "A2")
    }
}

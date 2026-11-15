package br.com.gotuner.android.tuner.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.gotuner.android.R
import br.com.gotuner.android.tuner.presentation.preview.PreviewContainer
import br.com.gotuner.android.ui.theme.GOTunerColors
import br.com.gotuner.tuner.domain.model.GuitarString
import br.com.gotuner.tuner.domain.model.StandardTuning

private val BadgeSize = 44.dp

/**
 * Row of note buttons where the user picks the string to tune, plus the automatic mode switch and
 * the reference tone. [selectedString] is the user's choice; [targetString] is what the tuner is
 * currently comparing against, which in automatic mode is the detected string.
 */
@Composable
fun StringSelector(
    strings: List<GuitarString>,
    selectedString: Int?,
    targetString: Int?,
    tunedStrings: Set<Int>,
    tint: Color,
    isPlayingReference: Boolean,
    onSelectString: (Int?) -> Unit,
    onToggleReference: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeString = selectedString ?: targetString

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedString == null,
                onClick = { onSelectString(null) },
                label = { Text(text = stringResource(R.string.auto_mode)) },
                leadingIcon = if (selectedString == null) {
                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null
            )

            ReferenceToneButton(
                note = strings.firstOrNull { it.number == activeString }?.note,
                isPlaying = isPlayingReference,
                onToggle = onToggleReference
            )
        }

        StringButtons(
            strings = strings,
            selectedString = selectedString,
            activeString = activeString,
            tunedStrings = tunedStrings,
            tint = tint,
            onSelectString = onSelectString
        )
    }
}

/** Plays (or stops) the correct pitch of the active string; disabled until a string is known. */
@Composable
private fun ReferenceToneButton(note: String?, isPlaying: Boolean, onToggle: () -> Unit) {
    OutlinedButton(onClick = onToggle, enabled = isPlaying || note != null) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Close else Icons.Filled.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = when {
                isPlaying -> stringResource(R.string.stop_reference)
                note != null -> stringResource(R.string.hear_note, note)
                else -> stringResource(R.string.hear_reference)
            }
        )
    }
}

/** One button per string, thickest first; the whole column, number and badge, selects its string. */
@Composable
private fun StringButtons(
    strings: List<GuitarString>,
    selectedString: Int?,
    activeString: Int?,
    tunedStrings: Set<Int>,
    tint: Color,
    onSelectString: (Int?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
    ) {
        strings.forEach { string ->
            val tuned = string.number in tunedStrings
            val description = stringResource(R.string.string_description, string.number, string.note)
            val tunedDescription = stringResource(R.string.in_tune)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .selectable(
                        selected = string.number == selectedString,
                        role = Role.RadioButton,
                        onClick = { onSelectString(string.number) }
                    )
                    .semantics {
                        contentDescription = description
                        if (tuned) stateDescription = tunedDescription
                    }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.string_ordinal, string.number),
                    modifier = Modifier.clearAndSetSemantics { },
                    fontSize = 12.sp,
                    color = GOTunerColors.Neutral
                )

                NoteBadge(
                    note = string.note,
                    selected = string.number == selectedString,
                    active = string.number == activeString,
                    tuned = tuned,
                    tint = tint,
                    modifier = Modifier.clearAndSetSemantics { }
                )
            }
        }
    }
}

/**
 * Round note label. Filled when the user picked the string, outlined in the reading's color when
 * the tuner detected it, and marked with a green check once the string was confirmed in tune.
 */
@Composable
private fun NoteBadge(
    note: String,
    selected: Boolean,
    active: Boolean,
    tuned: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val background = MaterialTheme.colorScheme.background
    val container = when {
        selected -> tint
        active -> tint.copy(alpha = 0.16f)
        else -> background
    }
    val border = when {
        active -> tint
        tuned -> GOTunerColors.Green
        else -> GOTunerColors.Track
    }
    val content = when {
        selected -> background
        active -> tint
        tuned -> GOTunerColors.Green
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(BadgeSize)
                .background(container, CircleShape)
                .border(2.dp, border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = note, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = content)
        }

        if (tuned) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 5.dp, y = (-5).dp)
                    .size(18.dp)
                    .background(GOTunerColors.Green, CircleShape)
                    .border(2.dp, background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = background
                )
            }
        }
    }
}

@Preview(name = "Auto, string detected", widthDp = 360)
@Composable
private fun StringSelectorAutoPreview() {
    PreviewContainer {
        StringSelector(
            strings = StandardTuning.strings,
            selectedString = null,
            targetString = 5,
            tunedStrings = setOf(6),
            tint = GOTunerColors.Red,
            isPlayingReference = false,
            onSelectString = {},
            onToggleReference = {}
        )
    }
}

@Preview(name = "String chosen, tone playing", widthDp = 360)
@Composable
private fun StringSelectorManualPreview() {
    PreviewContainer {
        StringSelector(
            strings = StandardTuning.strings,
            selectedString = 3,
            targetString = 3,
            tunedStrings = setOf(6, 5, 4),
            tint = GOTunerColors.Yellow,
            isPlayingReference = true,
            onSelectString = {},
            onToggleReference = {}
        )
    }
}

@Preview(name = "Nothing chosen yet", widthDp = 360)
@Composable
private fun StringSelectorIdlePreview() {
    PreviewContainer {
        StringSelector(
            strings = StandardTuning.strings,
            selectedString = null,
            targetString = null,
            tunedStrings = emptySet(),
            tint = GOTunerColors.Neutral,
            isPlayingReference = false,
            onSelectString = {},
            onToggleReference = {}
        )
    }
}

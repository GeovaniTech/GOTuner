package br.com.gotuner.android.tuner.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.gotuner.android.R
import br.com.gotuner.android.tuner.presentation.formatOneDecimal
import br.com.gotuner.android.tuner.presentation.preview.PreviewContainer
import br.com.gotuner.android.ui.theme.GOTunerColors
import br.com.gotuner.tuner.domain.tuning.NoteScale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

/** Semitones visible across the strip: the closest note plus one neighbour on each side. */
private const val VISIBLE_SEMITONES = 2.5f
/** One tick every 10 cents. */
private const val TICKS_PER_SEMITONE = 10
/** E2, the lowest string: where the strip rests before anything is played or chosen. */
private const val RESTING_POSITION = 40f

private val Padding = 10.dp
private val NoteTickHeight = 50.dp
private val HalfTickHeight = 32.dp
private val TickHeight = 20.dp
private val ArrowHeight = 12.dp
private val ArrowGap = 4.dp

private val Shape = RoundedCornerShape(12.dp)
private val LabelStyle = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold)
private val OctaveStyle = SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium)
private val ReadingStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium)

/**
 * Strip of notes that slides under a fixed pointer, so the note under the pointer is the one the
 * sound is closest to. Every semitone is labelled and each tick between them is 10 cents; the
 * note of [targetFrequency], the string being tuned, stands out from its neighbours. The detected
 * [frequency] is written under the pointer. Without a reading the strip rests on the target note.
 */
@Composable
fun NoteStrip(frequency: Double?, targetFrequency: Double?, tint: Color, modifier: Modifier = Modifier) {
    val position = frequency?.let(NoteScale::position)
    val closest = frequency?.let(NoteScale::closestSemitone)
    val target = targetFrequency?.let(NoteScale::closestSemitone)
    val center by animateFloatAsState(
        targetValue = position?.toFloat() ?: target?.toFloat() ?: RESTING_POSITION,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "note strip"
    )

    val reading = (frequency?.formatOneDecimal() ?: "--.-") + " " + stringResource(R.string.frequency_unit)
    val description = if (closest != null) {
        stringResource(R.string.closest_note, NoteScale.name(closest), reading)
    } else stringResource(R.string.no_note_detected)

    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val highlight = MaterialTheme.colorScheme.onSurface
    val readingLayout = measurer.measure(reading, ReadingStyle)
    // Text follows the user's font scale, so the strip grows with it instead of clipping labels.
    val labelHeight = remember(measurer, density) { measurer.measure(noteLabel("A#2"), LabelStyle).size.height }
    val height = with(density) { (labelHeight + readingLayout.size.height).toDp() } +
        Padding * 2 + NoteTickHeight + ArrowGap * 2 + ArrowHeight

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(Shape)
            .border(1.5.dp, GOTunerColors.Track, Shape)
            .semantics { contentDescription = description }
    ) {
        val centerX = size.width / 2
        val semitoneWidth = size.width / VISIBLE_SEMITONES
        val labelTop = Padding.toPx()
        val ticksBottom = labelTop + labelHeight + NoteTickHeight.toPx()
        // Marks fade towards the edges: the strip continues beyond what is drawn.
        fun fade(x: Float): Float {
            val distance = (abs(x - centerX) / centerX).coerceAtMost(1f)
            return 1f - 0.8f * distance * distance
        }

        val firstTick = floor((center - VISIBLE_SEMITONES / 2) * TICKS_PER_SEMITONE).toInt() - TICKS_PER_SEMITONE
        val lastTick = ceil((center + VISIBLE_SEMITONES / 2) * TICKS_PER_SEMITONE).toInt() + TICKS_PER_SEMITONE

        for (tick in firstTick..lastTick) {
            val x = centerX + (tick.toFloat() / TICKS_PER_SEMITONE - center) * semitoneWidth
            val step = tick.mod(TICKS_PER_SEMITONE)
            val isNote = step == 0
            val semitone = tick.floorDiv(TICKS_PER_SEMITONE)
            val color = when {
                isNote && semitone == target -> highlight
                isNote -> GOTunerColors.Neutral
                else -> GOTunerColors.Tick
            }
            val tickHeight = when {
                isNote -> NoteTickHeight
                step == TICKS_PER_SEMITONE / 2 -> HalfTickHeight
                else -> TickHeight
            }

            drawLine(
                color = color.copy(alpha = fade(x)),
                start = Offset(x, ticksBottom - tickHeight.toPx()),
                end = Offset(x, ticksBottom),
                strokeWidth = (if (isNote) 2.dp else 1.5.dp).toPx()
            )

            if (isNote) {
                val label = measurer.measure(noteLabel(NoteScale.name(semitone)), LabelStyle)

                drawText(
                    textLayoutResult = label,
                    color = color,
                    topLeft = Offset(x - label.size.width / 2f, labelTop),
                    alpha = fade(x)
                )
            }
        }

        // Fixed pointer: the strip moves, the reading is always at the centre.
        drawLine(
            color = tint,
            start = Offset(centerX, ticksBottom - NoteTickHeight.toPx()),
            end = Offset(centerX, ticksBottom),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        val arrowTop = ticksBottom + ArrowGap.toPx()
        val arrowBottom = arrowTop + ArrowHeight.toPx()

        drawPath(
            path = Path().apply {
                moveTo(centerX, arrowTop)
                lineTo(centerX + ArrowHeight.toPx() * 0.7f, arrowBottom)
                lineTo(centerX - ArrowHeight.toPx() * 0.7f, arrowBottom)
                close()
            },
            color = tint
        )

        drawText(
            textLayoutResult = readingLayout,
            color = tint,
            topLeft = Offset(centerX - readingLayout.size.width / 2f, arrowBottom + ArrowGap.toPx())
        )
    }
}

/** "D#2" with the octave smaller than the note, as on sheet music. */
private fun noteLabel(name: String): AnnotatedString {
    val octaveStart = name.indexOfFirst { it == '-' || it.isDigit() }.takeIf { it >= 0 } ?: name.length

    return buildAnnotatedString {
        append(name.substring(0, octaveStart))
        withStyle(OctaveStyle) { append(name.substring(octaveStart)) }
    }
}

@Preview(name = "In tune", widthDp = 360)
@Composable
private fun NoteStripInTunePreview() {
    PreviewContainer {
        NoteStrip(frequency = 82.4, targetFrequency = 82.41, tint = GOTunerColors.Green)
    }
}

@Preview(name = "Flat", widthDp = 360)
@Composable
private fun NoteStripFlatPreview() {
    PreviewContainer {
        NoteStrip(frequency = 107.9, targetFrequency = 110.0, tint = GOTunerColors.Red)
    }
}

@Preview(name = "Chosen string far from its note", widthDp = 360)
@Composable
private fun NoteStripFarPreview() {
    PreviewContainer {
        NoteStrip(frequency = 176.2, targetFrequency = 196.0, tint = GOTunerColors.Red)
    }
}

@Preview(name = "No reading, string chosen", widthDp = 360)
@Composable
private fun NoteStripWaitingPreview() {
    PreviewContainer {
        NoteStrip(frequency = null, targetFrequency = 146.83, tint = GOTunerColors.Neutral)
    }
}

@Preview(name = "Large font", widthDp = 360, fontScale = 1.8f)
@Composable
private fun NoteStripLargeFontPreview() {
    PreviewContainer {
        NoteStrip(frequency = 248.1, targetFrequency = 246.94, tint = GOTunerColors.Yellow)
    }
}

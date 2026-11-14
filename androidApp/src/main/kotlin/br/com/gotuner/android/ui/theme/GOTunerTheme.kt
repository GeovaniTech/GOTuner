package br.com.gotuner.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import br.com.gotuner.tuner.domain.model.AccuracyColor

/** Single source for the app's colors, shared by the screen, its components and previews. */
object GOTunerColors {
    val Background = Color(0xFF10131B)
    val Green = Color(0xFF66E3AA)
    val Yellow = Color(0xFFFFD166)
    val Red = Color(0xFFFF737A)
    val Neutral = Color(0xFF9DA6BA)
    val Track = Color(0xFF41495C)
    val Tick = Color(0xFF70788A)
}

/** Color used for the reading, cents and instruction for a given tuning accuracy. */
fun AccuracyColor?.toTint(): Color = when (this) {
    AccuracyColor.GREEN -> GOTunerColors.Green
    AccuracyColor.YELLOW -> GOTunerColors.Yellow
    AccuracyColor.RED -> GOTunerColors.Red
    null -> GOTunerColors.Neutral
}

@Composable
fun GOTunerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = GOTunerColors.Background,
            surface = GOTunerColors.Background
        ),
        content = content
    )
}

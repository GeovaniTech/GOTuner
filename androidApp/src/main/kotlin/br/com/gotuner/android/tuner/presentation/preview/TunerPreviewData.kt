package br.com.gotuner.android.tuner.presentation.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import br.com.gotuner.android.ui.theme.GOTunerTheme
import br.com.gotuner.tuner.domain.model.AccuracyColor
import br.com.gotuner.tuner.domain.model.TuningStatus
import br.com.gotuner.tuner.presentation.MicrophonePermissionState
import br.com.gotuner.tuner.presentation.TunerUiState

/** Wraps a component preview in the app theme and background. */
@Composable
internal fun PreviewContainer(content: @Composable () -> Unit) {
    GOTunerTheme {
        Surface {
            Box(modifier = Modifier.padding(16.dp)) { content() }
        }
    }
}

/** Representative tuner states, used by the full-screen previews. */
internal object TunerPreviewStates {
    private val granted = TunerUiState(
        isListening = true,
        microphonePermissionState = MicrophonePermissionState.GRANTED
    )

    val waitingForPitch = granted

    val flat = granted.copy(
        detectedFrequency = 108.7, targetFrequency = 110.0, targetNote = "A2",
        deviationCents = -20.6, tuningStatus = TuningStatus.FLAT, accuracyColor = AccuracyColor.RED
    )

    val inTune = granted.copy(
        detectedFrequency = 110.2, targetFrequency = 110.0, targetNote = "A2",
        deviationCents = 3.1, tuningStatus = TuningStatus.IN_TUNE, accuracyColor = AccuracyColor.GREEN
    )

    val sharp = granted.copy(
        detectedFrequency = 331.8, targetFrequency = 329.63, targetNote = "E4",
        deviationCents = 11.4, tuningStatus = TuningStatus.SHARP, accuracyColor = AccuracyColor.YELLOW
    )

    val permissionNeeded = TunerUiState(microphonePermissionState = MicrophonePermissionState.DENIED)

    val error = granted.copy(isListening = false, errorMessage = "Microphone unavailable. Tap Retry.")
}

internal class TunerUiStatePreviewProvider : PreviewParameterProvider<TunerUiState> {
    override val values = sequenceOf(
        TunerPreviewStates.waitingForPitch,
        TunerPreviewStates.flat,
        TunerPreviewStates.inTune,
        TunerPreviewStates.sharp,
        TunerPreviewStates.permissionNeeded,
        TunerPreviewStates.error
    )
}

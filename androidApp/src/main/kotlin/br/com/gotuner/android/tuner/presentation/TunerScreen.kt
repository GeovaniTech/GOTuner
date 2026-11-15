package br.com.gotuner.android.tuner.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import br.com.gotuner.android.BuildConfig
import br.com.gotuner.android.tuner.presentation.components.CentsReadout
import br.com.gotuner.android.tuner.presentation.components.ErrorMessage
import br.com.gotuner.android.tuner.presentation.components.FrequencyDisplay
import br.com.gotuner.android.tuner.presentation.components.PermissionRequest
import br.com.gotuner.android.tuner.presentation.components.PrivacyPolicyLink
import br.com.gotuner.android.tuner.presentation.components.StringSelector
import br.com.gotuner.android.tuner.presentation.components.TopBar
import br.com.gotuner.android.tuner.presentation.components.TuningIndicator
import br.com.gotuner.android.tuner.presentation.components.TuningInstruction
import br.com.gotuner.android.tuner.presentation.preview.TunerPreviewStates
import br.com.gotuner.android.tuner.presentation.preview.TunerUiStatePreviewProvider
import br.com.gotuner.android.ui.theme.GOTunerTheme
import br.com.gotuner.android.ui.theme.toTint
import br.com.gotuner.tuner.domain.model.GuitarString
import br.com.gotuner.tuner.domain.model.StandardTuning
import br.com.gotuner.tuner.presentation.MicrophonePermissionState
import br.com.gotuner.tuner.presentation.TunerUiState

/**
 * Tuner screen: header, then either the permission request or the live reading, then the buttons
 * where the string is chosen and its reference tone played, then errors and links.
 */
@Composable
fun TunerScreen(
    state: TunerUiState,
    permissionBlocked: Boolean = false,
    privacyPolicyUrl: String = BuildConfig.PRIVACY_POLICY_URL,
    strings: List<GuitarString> = StandardTuning.strings,
    onSelectString: (Int?) -> Unit = {},
    onToggleReference: () -> Unit = {},
    onEnable: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        // Surface stacks its children like a Box; the Column lays the screen out top to bottom.
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            TopBar(isListening = state.isListening)

            if (state.microphonePermissionState == MicrophonePermissionState.GRANTED) {
                TuningReading(state = state, tint = state.accuracyColor.toTint())
            } else {
                PermissionRequest(permissionBlocked = permissionBlocked, onEnable = onEnable)
            }

            // Shown even without the microphone: the reference tone still allows tuning by ear.
            StringSelector(
                strings = strings,
                selectedString = state.selectedString,
                targetString = state.targetString,
                tunedStrings = state.tunedStrings,
                // A chosen string with no reading yet is highlighted in the regular text color.
                tint = state.accuracyColor?.toTint() ?: MaterialTheme.colorScheme.onSurface,
                isPlayingReference = state.isPlayingReference,
                onSelectString = onSelectString,
                onToggleReference = onToggleReference
            )

            state.errorMessage?.let { ErrorMessage(message = it, onRetry = onEnable) }

            PrivacyPolicyLink(url = privacyPolicyUrl)
        }
    }
}

/** Live reading: frequency, deviation bar, cents and the verdict on what to do. */
@Composable
private fun TuningReading(state: TunerUiState, tint: Color) {
    FrequencyDisplay(frequency = state.detectedFrequency, tint = tint)

    // TuningIndicator(cents = state.deviationCents, tint = tint)

    CentsReadout(cents = state.deviationCents, tint = tint)

    TuningInstruction(
        status = state.tuningStatus,
        tint = tint,
        selectedString = state.selectedString,
        selectedNote = state.targetNote.takeIf { state.selectedString != null },
        referenceNote = state.targetNote.takeIf { state.isPlayingReference }
    )
}

@Preview(name = "Tuner screen", showSystemUi = true)
@Composable
private fun TunerScreenPreview(
    @PreviewParameter(TunerUiStatePreviewProvider::class) state: TunerUiState
) {
    GOTunerTheme {
        TunerScreen(state = state, privacyPolicyUrl = "", onEnable = {})
    }
}

@Preview(name = "Tuner screen – Português", locale = "pt", showSystemUi = true)
@Composable
private fun TunerScreenPortuguesePreview() {
    GOTunerTheme {
        TunerScreen(state = TunerPreviewStates.inTune, privacyPolicyUrl = "", onEnable = {})
    }
}

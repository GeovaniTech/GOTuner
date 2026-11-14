package br.com.gotuner.android.tuner.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.gotuner.android.BuildConfig
import br.com.gotuner.android.R
import br.com.gotuner.android.tuner.presentation.components.TopBar
import br.com.gotuner.android.tuner.presentation.components.TuningIndicator
import br.com.gotuner.tuner.domain.model.AccuracyColor
import br.com.gotuner.tuner.domain.model.TuningStatus
import br.com.gotuner.tuner.presentation.MicrophonePermissionState
import br.com.gotuner.tuner.presentation.TunerUiState
import java.util.Locale

private fun Double.fmt() = String.format(Locale.getDefault(), "%.1f", this)

@Composable
fun TunerScreen(state: TunerUiState, permissionBlocked: Boolean = false, onEnable: () -> Unit) {
    val tint = when (state.accuracyColor) {
        AccuracyColor.GREEN -> Color(0xFF66E3AA)
        AccuracyColor.YELLOW -> Color(0xFFFFD166)
        AccuracyColor.RED -> Color(0xFFFF737A)
        else -> Color(0xFF9DA6BA)
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        // Surface stacks its children like a Box; the Column lays the screen out top to bottom.
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            TopBar(isListening = state.isListening)

            Spacer(Modifier.height(36.dp))

            if (state.microphonePermissionState != MicrophonePermissionState.GRANTED) {
                Text(
                    text = stringResource(R.string.permission_rationale)
                )

                Button(onClick = onEnable) {
                    Text(
                        text = stringResource(if (permissionBlocked) R.string.open_settings else R.string.enable_microphone)
                    )
                }
            } else {
                BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val frequency = state.detectedFrequency?.fmt() ?: "--.-"
                    val size = minOf(
                        64f,
                        maxWidth.value / (0.65f * frequency.length * LocalDensity.current.fontScale)
                    )
                    Text(
                        frequency,
                        maxLines = 1,
                        fontSize = size.sp,
                        fontWeight = FontWeight.Bold,
                        color = tint
                    )
                }

                Text(
                    text = stringResource(R.string.frequency_unit), color = tint
                )

                state.targetNote?.let {
                    Text(
                        text = stringResource(
                            R.string.target_reference,
                            it,
                            state.targetFrequency?.fmt().orEmpty()
                        ),
                        fontSize = 20.sp
                    )
                }

                TuningIndicator(state.deviationCents, tint)

                Text(state.deviationCents?.let {
                    stringResource(
                        R.string.cents_value,
                        "${if (it > 0) "+" else ""}${it.fmt()}"
                    )
                } ?: stringResource(R.string.cents_empty), color = tint)

                val instruction = when (state.tuningStatus) {
                    TuningStatus.FLAT -> R.string.tighten_string
                    TuningStatus.SHARP -> R.string.loosen_string
                    TuningStatus.IN_TUNE -> R.string.string_in_tune
                    else -> null
                }

                instruction?.let {
                    Surface(
                        color = tint.copy(alpha = 0.12f),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(it),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = tint
                        )
                    }
                }
            }

            state.errorMessage?.let {
                Text(
                    text = it
                )

                Button(onClick = onEnable) {
                    Text(
                        text = stringResource(R.string.retry)
                    )
                }
            }

            if (BuildConfig.PRIVACY_POLICY_URL.isNotBlank()) {
                val uriHandler = LocalUriHandler.current

                TextButton(onClick = { uriHandler.openUri(BuildConfig.PRIVACY_POLICY_URL) }) {
                    Text(
                        stringResource(R.string.privacy_policy)
                    )
                }
            }
        }
    }
}

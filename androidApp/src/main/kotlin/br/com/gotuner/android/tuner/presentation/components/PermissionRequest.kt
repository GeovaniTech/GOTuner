package br.com.gotuner.android.tuner.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.gotuner.android.R
import br.com.gotuner.android.tuner.presentation.preview.PreviewContainer

/**
 * Explains why the microphone is needed. When the user permanently denied it,
 * the button opens the system settings instead of asking again.
 */
@Composable
fun PermissionRequest(permissionBlocked: Boolean, onEnable: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = stringResource(R.string.permission_rationale), textAlign = TextAlign.Center)

        Button(onClick = onEnable) {
            Text(
                text = stringResource(
                    if (permissionBlocked) R.string.open_settings else R.string.enable_microphone
                )
            )
        }
    }
}

@Preview
@Composable
private fun PermissionRequestPreview() {
    PreviewContainer {
        PermissionRequest(permissionBlocked = false, onEnable = {})
    }
}

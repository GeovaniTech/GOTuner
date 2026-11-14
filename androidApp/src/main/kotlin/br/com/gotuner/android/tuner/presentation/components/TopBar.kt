package br.com.gotuner.android.tuner.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import br.com.gotuner.android.R
import br.com.gotuner.android.tuner.presentation.preview.PreviewContainer

/** App name on the left and the microphone status on the right. */
@Composable
fun TopBar(isListening: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.app_name),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        ListeningStatus(isListening = isListening)
    }
}

@Preview(widthDp = 360)
@Composable
private fun TopBarPreview() {
    PreviewContainer {
        TopBar(isListening = true)
    }
}

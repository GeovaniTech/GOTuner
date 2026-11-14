package br.com.gotuner.android.tuner.presentation.components

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import br.com.gotuner.android.R

/** Link to the privacy policy; renders nothing when the build has no policy URL configured. */
@Composable
fun PrivacyPolicyLink(url: String, modifier: Modifier = Modifier) {
    if (url.isBlank()) return
    val uriHandler = LocalUriHandler.current

    TextButton(onClick = { uriHandler.openUri(url) }, modifier = modifier) {
        Text(text = stringResource(R.string.privacy_policy))
    }
}

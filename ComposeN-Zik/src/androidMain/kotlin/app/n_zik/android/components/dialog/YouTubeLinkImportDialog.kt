package app.n_zik.android.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.n_zik.android.R
import app.n_zik.android.colorPalette
import app.n_zik.android.typography
import app.it.fast4x.rimusic.utils.isLandscape
import app.it.fast4x.rimusic.utils.semiBold
import app.n_zik.android.uiRoundnessShape
import androidx.core.net.toUri

/**
 * Centralized YouTube link import dialog.
 * Takes a URL and calls [onImport] with the extracted playlist ID.
 *
 * @param onImport Called with the extracted playlist ID (e.g. "PLxxx...")
 * @param onDismiss Called when the dialog is dismissed
 */
@Composable
fun YouTubeLinkImportDialog(
    onImport: (playlistId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var youTubeUrl by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.wrapContentSize()
                .sizeIn(
                    maxWidth = LocalConfiguration.current.screenWidthDp.dp *
                        if (isLandscape) 0.6f else 0.95f
                )
                .padding(16.dp),
            shape = uiRoundnessShape(),
            colors = CardDefaults.cardColors(
                containerColor = colorPalette().background1
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicText(
                    text = stringResource(R.string.import_via_youtube_link),
                    style = typography().l.semiBold.copy(color = colorPalette().text),
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
                Spacer(modifier = Modifier.height(20.dp))
                TextField(
                    value = youTubeUrl,
                    onValueChange = { youTubeUrl = it },
                    placeholder = { BasicText("https://youtube.com/playlist?list=...", style = typography().xs.semiBold.copy(color = colorPalette().textDisabled)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        val playlistId = extractYouTubePlaylistId(youTubeUrl)
                        if (playlistId != null) {
                            onImport(playlistId)
                            onDismiss()
                        }
                    }),
                    colors = InputDialog.defaultTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp)
                ) {
                    InteractiveDialog.CancelButton(
                        modifier = InteractiveDialog.ButtonModifier()
                            .weight(1f).fillMaxWidth(0.98f)
                            .border(width = 2.dp, color = Color(android.graphics.Color.RED).copy(alpha = 0.3f), shape = uiRoundnessShape())
                            .padding(vertical = 10.dp),
                        onCancel = onDismiss
                    )
                    InteractiveDialog.ConfirmButton(
                        modifier = InteractiveDialog.ButtonModifier()
                            .weight(1f).fillMaxWidth(0.98f)
                            .background(colorPalette().accent)
                            .padding(vertical = 10.dp),
                        onConfirm = {
                            val playlistId = extractYouTubePlaylistId(youTubeUrl)
                            if (playlistId != null) {
                                onImport(playlistId)
                                onDismiss()
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun extractYouTubePlaylistId(url: String): String? {
    val prefixes = listOf(
        "https://www.youtube.com/playlist?",
        "https://youtube.com/playlist?",
        "https://music.youtube.com/playlist?",
        "https://m.youtube.com/playlist?"
    )
    return prefixes.find { url.startsWith(it) }
        ?.let { url.trim().toUri().getQueryParameter("list") }
}

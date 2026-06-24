package app.n_zik.android.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.n_zik.android.R
import app.n_zik.android.colorPalette
import app.n_zik.android.typography
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.utils.isLandscape
import app.it.fast4x.rimusic.utils.medium
import app.it.fast4x.rimusic.utils.semiBold
import app.n_zik.android.uiRoundnessShape

/**
 * Centralized match results dialog showing matched/failed counts
 * and the list of failed songs. Replaces inline implementations
 * in HomeSongsScreen and LocalPlaylistSongs.
 *
 * @param matched Number of successfully matched songs
 * @param failed Number of failed songs
 * @param failedSongs List of songs that failed to match
 * @param onRetry Called when user taps Retry (only shown if failed > 0)
 * @param onDismiss Called when user taps OK or dismisses the dialog
 */
@Composable
fun MatchResultsDialog(
    matched: Int,
    failed: Int,
    merged: Int = 0,
    failedSongs: List<Song>,
    onRetry: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.5f else 0.9f)
                .padding(16.dp),
            shape = uiRoundnessShape(),
            colors = CardDefaults.cardColors(
                containerColor = colorPalette().background1
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                BasicText(
                    text = stringResource(R.string.match_results_title),
                    style = typography().l.semiBold.copy(color = colorPalette().text),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (failed == 0) {
                    BasicText(
                        text = stringResource(R.string.match_results_all_matched),
                        style = typography().s.copy(color = colorPalette().text),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    BasicText(
                        text = stringResource(R.string.match_results_some_failed, matched, failed),
                        style = typography().s.copy(color = colorPalette().text),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (merged > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    BasicText(
                        text = stringResource(R.string.match_results_merged, merged),
                        style = typography().xs.copy(color = colorPalette().textSecondary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (failed > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicText(
                        text = stringResource(R.string.match_results_failed_songs),
                        style = typography().xs.copy(color = colorPalette().textSecondary),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp)
                    ) {
                        items(failedSongs.size) { index ->
                            val song = failedSongs[index]
                            BasicText(
                                text = "${song.title} - ${song.artistsText ?: ""}",
                                style = typography().xs.copy(color = colorPalette().text),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (failed > 0 && onRetry != null) {
                        androidx.compose.foundation.text.BasicText(
                            text = stringResource(R.string.retry_match),
                            style = typography().xs
                                .medium
                                .copy(
                                    color = colorPalette().text,
                                    textAlign = TextAlign.Center
                                ),
                            modifier = InteractiveDialog.ButtonModifier()
                                .weight(1f)
                                .fillMaxWidth(0.98f)
                                .padding(horizontal = 5.dp)
                                .background(colorPalette().background2)
                                .padding(vertical = 10.dp)
                                .clip(uiRoundnessShape())
                                .clickable(onClick = onRetry)
                        )
                    }
                    InteractiveDialog.ConfirmButton(
                        modifier = InteractiveDialog.ButtonModifier()
                            .weight(1f)
                            .fillMaxWidth(0.98f)
                            .padding(horizontal = 5.dp)
                            .background(colorPalette().accent)
                            .padding(vertical = 10.dp),
                        onConfirm = onDismiss
                    )
                }
            }
        }
    }
}

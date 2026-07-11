package app.n_zik.android.components.dialog.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.n_zik.android.R
import app.n_zik.android.colorPalette
import app.n_zik.android.typography
import app.n_zik.android.uiRoundnessShape
import app.it.fast4x.rimusic.utils.medium
import app.it.fast4x.rimusic.utils.semiBold
import app.it.fast4x.rimusic.utils.streamClientWebRemixEnabledKey
import app.it.fast4x.rimusic.utils.streamClientVisionosEnabledKey
import app.it.fast4x.rimusic.utils.streamClientTvEmbeddedEnabledKey
import app.it.fast4x.rimusic.utils.streamClientTvHtml5EnabledKey
import app.it.fast4x.rimusic.utils.streamClientAndroidVrEnabledKey
import app.it.fast4x.rimusic.utils.streamClientAndroidCreatorEnabledKey
import app.it.fast4x.rimusic.utils.streamClientIosEnabledKey
import app.it.fast4x.rimusic.utils.streamClientIpadosEnabledKey
import app.it.fast4x.rimusic.utils.streamClientWebEnabledKey
import app.it.fast4x.rimusic.utils.streamClientWebCreatorEnabledKey
import app.it.fast4x.rimusic.utils.streamClientMobileEnabledKey
import app.it.fast4x.rimusic.utils.streamClientAndroidEnabledKey
import app.n_zik.android.LocalPlayerServiceBinder
import app.n_zik.android.components.dialog.common.Dialog
import app.n_zik.android.components.dialog.common.ToggleItem
import app.n_zik.android.components.dialog.common.ToggleListDialog
import app.it.fast4x.rimusic.utils.streamClientRestartNeededKey
import app.n_zik.android.playback.services.clearStreamCaches
import app.kreate.android.me.knighthat.utils.Toaster

object StreamClientsSettingsDialog : Dialog {

    override val dialogTitle: String
        @Composable
        get() = stringResource(R.string.disabled_stream_clients_title)

    override var isActive: Boolean by mutableStateOf(false)

    private val clientKeys = listOf(
        streamClientWebRemixEnabledKey,
        streamClientVisionosEnabledKey,
        streamClientTvEmbeddedEnabledKey,
        streamClientTvHtml5EnabledKey,
        streamClientAndroidVrEnabledKey,
        streamClientAndroidCreatorEnabledKey,
        streamClientAndroidEnabledKey,
        streamClientIosEnabledKey,
        streamClientIpadosEnabledKey,
        streamClientWebEnabledKey,
        streamClientWebCreatorEnabledKey,
        streamClientMobileEnabledKey
    )

    @Composable
    override fun DialogBody() {
        val context = LocalContext.current
        val binder = LocalPlayerServiceBinder.current
        val prefs = remember { context.getSharedPreferences("preferences", android.content.Context.MODE_PRIVATE) }

        // Save initial state for comparison
        val savedStates = remember {
            clientKeys.associateWith { prefs.getBoolean(it, true) }
        }

        // Local state - not saved until OK is clicked
        val localStates = remember {
            mutableStateMapOf<String, Boolean>().apply {
                savedStates.forEach { (key, value) -> put(key, value) }
            }
        }

        val items = listOf(
            ToggleItem(id = "web_remix", iconRes = R.drawable.ytmusic, label = "Web Remix", preferenceKey = streamClientWebRemixEnabledKey, defaultValue = true, description = stringResource(R.string.client_youtube_music_web_desc)),
            ToggleItem(id = "android_vr", iconRes = R.drawable.musical_notes, label = "Android VR", preferenceKey = streamClientAndroidVrEnabledKey, defaultValue = true, description = stringResource(R.string.client_android_vr_desc)),
            ToggleItem(id = "visionos", iconRes = R.drawable.musical_notes, label = "visionOS", preferenceKey = streamClientVisionosEnabledKey, defaultValue = true),
            ToggleItem(id = "tv_embedded", iconRes = R.drawable.video, label = "TV Embedded", preferenceKey = streamClientTvEmbeddedEnabledKey, defaultValue = true),
            ToggleItem(id = "tv_html5", iconRes = R.drawable.video, label = "TV HTML5", preferenceKey = streamClientTvHtml5EnabledKey, defaultValue = true),
            ToggleItem(id = "android_creator", iconRes = R.drawable.musical_notes, label = "Android Creator", preferenceKey = streamClientAndroidCreatorEnabledKey, defaultValue = true),
            ToggleItem(id = "android", iconRes = R.drawable.musical_notes, label = "Android", preferenceKey = streamClientAndroidEnabledKey, defaultValue = true),
            ToggleItem(id = "ios", iconRes = R.drawable.musical_notes, label = "iOS", preferenceKey = streamClientIosEnabledKey, defaultValue = true),
            ToggleItem(id = "ipados", iconRes = R.drawable.musical_notes, label = "iPadOS", preferenceKey = streamClientIpadosEnabledKey, defaultValue = true),
            ToggleItem(id = "web", iconRes = R.drawable.musical_notes, label = "Web", preferenceKey = streamClientWebEnabledKey, defaultValue = true),
            ToggleItem(id = "web_creator", iconRes = R.drawable.musical_notes, label = "Web Creator", preferenceKey = streamClientWebCreatorEnabledKey, defaultValue = true),
            ToggleItem(id = "mobile", iconRes = R.drawable.musical_notes, label = "Mobile", preferenceKey = streamClientMobileEnabledKey, defaultValue = true)
        )

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            ToggleListDialog(
                items = items,
                modifier = Modifier.weight(1f, fill = false),
                contentHeight = 350.dp,
                enforceMinOneChecked = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                // Reset button (grey) - only changes local state
                BasicText(
                    text = stringResource(R.string.reset),
                    style = typography().xs.medium.copy(
                        color = colorPalette().textDisabled,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .clip(uiRoundnessShape())
                        .clickable {
                            clientKeys.forEach { key ->
                                prefs.edit().putBoolean(key, true).apply()
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )

                Spacer(modifier = Modifier.padding(8.dp))

                // Cancel button (red) - closes without saving
                BasicText(
                    text = stringResource(R.string.cancel),
                    style = typography().xs.medium.copy(
                        color = Color(android.graphics.Color.RED).copy(alpha = 0.3f),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .clip(uiRoundnessShape())
                        .clickable {
                            // Restore saved values
                            savedStates.forEach { (key, value) ->
                                prefs.edit().putBoolean(key, value).apply()
                            }
                            hideDialog()
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )

                Spacer(modifier = Modifier.padding(8.dp))

                // OK button (accent filled) - saves only if changed
                BasicText(
                    text = stringResource(R.string.ok),
                    style = typography().xs.semiBold.copy(
                        color = colorPalette().onAccent,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .clip(uiRoundnessShape())
                        .background(colorPalette().accent)
                        .clickable {
                            val hasChanges = clientKeys.any { key ->
                                prefs.getBoolean(key, true) != savedStates[key]
                            }
                            if (hasChanges) {
                                prefs.edit().putBoolean(streamClientRestartNeededKey, true).apply()
                                clearStreamCaches()
                                // Clear audio cache
                                binder?.cache?.let { cache ->
                                    cache.keys.forEach { song -> cache.removeResource(song) }
                                }
                                Toaster.i(R.string.preferred_stream_client_changed)
                                Toaster.w(R.string.stream_client_redownload_recommendation)
                            }
                            hideDialog()
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }
    }
}

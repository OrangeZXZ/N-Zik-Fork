package app.n_zik.android.components.dialog.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.n_zik.android.R
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
import app.n_zik.android.components.dialog.common.Dialog
import app.n_zik.android.components.dialog.common.ToggleItem
import app.n_zik.android.components.dialog.common.ToggleListDialog

object StreamClientsSettingsDialog : Dialog {

    override val dialogTitle: String
        @Composable
        get() = stringResource(R.string.disabled_stream_clients_title)

    override var isActive: Boolean by mutableStateOf(false)

    @Composable
    override fun DialogBody() {
        val items = listOf(
            ToggleItem(
                id = "web_remix",
                iconRes = R.drawable.ytmusic,
                label = stringResource(R.string.client_youtube_music_web),
                preferenceKey = streamClientWebRemixEnabledKey,
                defaultValue = true
            ),
            ToggleItem(
                id = "visionos",
                iconRes = R.drawable.musical_notes,
                label = stringResource(R.string.client_visionos),
                preferenceKey = streamClientVisionosEnabledKey,
                defaultValue = true
            ),
            ToggleItem(
                id = "tv_embedded",
                iconRes = R.drawable.video,
                label = stringResource(R.string.client_tv_embedded),
                preferenceKey = streamClientTvEmbeddedEnabledKey,
                defaultValue = true
            ),
            ToggleItem(
                id = "tv_html5",
                iconRes = R.drawable.video,
                label = stringResource(R.string.client_tv_html5),
                preferenceKey = streamClientTvHtml5EnabledKey,
                defaultValue = true
            ),
            ToggleItem(
                id = "android_vr",
                iconRes = R.drawable.musical_notes,
                label = stringResource(R.string.client_android_vr),
                preferenceKey = streamClientAndroidVrEnabledKey,
                defaultValue = true
            ),
            ToggleItem(
                id = "android_creator",
                iconRes = R.drawable.musical_notes,
                label = stringResource(R.string.client_android_creator),
                preferenceKey = streamClientAndroidCreatorEnabledKey,
                defaultValue = true
            ),
            ToggleItem(
                id = "ios",
                iconRes = R.drawable.musical_notes,
                label = stringResource(R.string.client_ios),
                preferenceKey = streamClientIosEnabledKey,
                defaultValue = true
            ),
            ToggleItem(
                id = "ipados",
                iconRes = R.drawable.musical_notes,
                label = stringResource(R.string.client_ipados),
                preferenceKey = streamClientIpadosEnabledKey,
                defaultValue = true
            ),
            ToggleItem(
                id = "web",
                iconRes = R.drawable.musical_notes,
                label = stringResource(R.string.client_web),
                preferenceKey = streamClientWebEnabledKey,
                defaultValue = true
            ),
            ToggleItem(
                id = "web_creator",
                iconRes = R.drawable.musical_notes,
                label = stringResource(R.string.client_web_creator),
                preferenceKey = streamClientWebCreatorEnabledKey,
                defaultValue = true
            ),
            ToggleItem(
                id = "mobile",
                iconRes = R.drawable.musical_notes,
                label = stringResource(R.string.client_mobile),
                preferenceKey = streamClientMobileEnabledKey,
                defaultValue = true
            )
        )

        ToggleListDialog(
            items = items,
            contentHeight = 480.dp
        )
    }
}

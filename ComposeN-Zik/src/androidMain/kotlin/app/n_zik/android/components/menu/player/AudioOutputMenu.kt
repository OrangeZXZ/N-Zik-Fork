package app.n_zik.android.components.menu.player

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.n_zik.android.R
import app.n_zik.android.colorPalette
import app.n_zik.android.components.menu.ListMenu
import app.n_zik.android.playback.services.AudioOutputManager
import app.n_zik.android.typography

@Composable
fun AudioOutputMenu(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val audioOutputManager = remember { AudioOutputManager(audioManager) }

    var devices by remember { mutableStateOf(audioOutputManager.getAvailableDevices()) }

    DisposableEffect(Unit) {
        audioOutputManager.registerDeviceChanges { updatedDevices ->
            devices = updatedDevices
        }
        onDispose {
            audioOutputManager.unregisterDeviceChanges()
        }
    }

    ListMenu.Menu(title = stringResource(R.string.audio_output_title)) {
        if (devices.isEmpty()) {
            BasicText(
                text = stringResource(R.string.audio_output_no_devices),
                style = typography().s.copy(
                    color = colorPalette().textSecondary
                ),
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            devices.forEach { device ->
                ListMenu.Entry(
                    text = device.displayName,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.speaker),
                            contentDescription = null,
                            tint = if (device.isCurrentlyActive) colorPalette().accent else colorPalette().text,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = {
                        audioOutputManager.setAudioOutput(device.id)
                        devices = audioOutputManager.getAvailableDevices()
                        onDismiss()
                    },
                    trailingContent = {
                        if (device.isCurrentlyActive) {
                            RadioButton(
                                selected = true,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = colorPalette().accent,
                                    unselectedColor = colorPalette().textSecondary
                                )
                            )
                        }
                    }
                )
            }
        }
    }
}

package app.n_zik.android.components.menu.player

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import app.n_zik.android.components.ui.sliders.SliderControl
import app.n_zik.android.playback.services.AudioOutputManager
import app.n_zik.android.typography
import kotlin.math.roundToInt

@Composable
fun AudioOutputMenu(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val audioOutputManager = remember { AudioOutputManager(audioManager) }

    var devices by remember { mutableStateOf(audioOutputManager.getAvailableDevices()) }
    var currentVolume by remember { mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }

    DisposableEffect(Unit) {
        audioOutputManager.registerDeviceChanges { updatedDevices ->
            devices = updatedDevices
            currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
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
                        val iconRes = when (device.type) {
                            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> R.drawable.bluetooth
                            AudioDeviceInfo.TYPE_WIRED_HEADSET,
                            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                            AudioDeviceInfo.TYPE_USB_HEADSET -> R.drawable.headphones
                            AudioDeviceInfo.TYPE_BUS -> R.drawable.speaker // Fallback to speaker for car for now
                            else -> R.drawable.speaker
                        }
                        Icon(
                            painter = painterResource(iconRes),
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

            Spacer(modifier = Modifier.height(16.dp))
            BasicText(
                text = stringResource(R.string.volume),
                style = typography().s.copy(
                    color = colorPalette().text
                ),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            SliderControl(
                state = currentVolume,
                range = 0f..maxVolume,
                stepSize = 1f,
                onSlide = { vol ->
                    currentVolume = vol
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol.roundToInt(), 0)
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

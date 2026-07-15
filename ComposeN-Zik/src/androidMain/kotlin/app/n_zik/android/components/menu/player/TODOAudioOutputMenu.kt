/*

package app.n_zik.android.components.menu.player

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.compose.runtime.LaunchedEffect
import app.n_zik.android.R
import app.n_zik.android.colorPalette
import app.n_zik.android.components.menu.ListMenu
import app.n_zik.android.components.ui.sliders.SliderControl
import app.n_zik.android.playback.services.AudioOutputManager
import app.n_zik.android.typography
import kotlin.math.roundToInt

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.grid.GridItemSpan
import app.it.fast4x.rimusic.enums.MenuStyle
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.ui.components.MenuState
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Menu
import app.it.fast4x.rimusic.utils.menuStyleKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.n_zik.android.components.menu.GridMenu
import androidx.compose.runtime.MutableState

// TODO: This menu is no longer linked to MiniPlayer. 
// It has been replaced by the native Android Media Output Switcher (Settings.Panel.ACTION_MEDIA_OUTPUT)
// because Android prevents applications from forcing sound to the physical speaker 
// when a Bluetooth A2DP device is connected (the "switch then reswitch" bug).

class AudioOutputMenu private constructor(
    override val menuState: MenuState,
    styleState: MutableState<MenuStyle>
) : Menu {

    companion object {
        @Composable
        operator fun invoke(): AudioOutputMenu =
            AudioOutputMenu(
                menuState = LocalMenuState.current,
                styleState = rememberPreference(menuStyleKey, MenuStyle.List)
            )
    }

    override var menuStyle: MenuStyle by styleState

    private var devices by mutableStateOf<List<app.n_zik.android.playback.services.AudioOutputManager.AudioDevice>>(emptyList())
    private var currentVolume by mutableFloatStateOf(0f)
    private var maxVolume by mutableFloatStateOf(0f)
    private var setVolume: ((Float) -> Unit)? = null
    private var setAudioOutput: ((Int) -> Unit)? = null

    @Composable
    override fun ListMenu() = ListMenu.Menu(title = stringResource(R.string.audio_output_title), showDragHandle = true) {
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
                            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> R.drawable.bluetooth
                            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> R.drawable.bluetooth
                            AudioDeviceInfo.TYPE_WIRED_HEADSET -> R.drawable.headphones
                            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> R.drawable.headphones
                            AudioDeviceInfo.TYPE_USB_HEADSET -> R.drawable.headphones
                            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> R.drawable.speaker
                            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> R.drawable.speaker
                            AudioDeviceInfo.TYPE_BUS -> R.drawable.car
                            else -> R.drawable.speaker
                        }
                        val iconColor = if (device.isCurrentlyActive) colorPalette().accent else colorPalette().text
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = iconColor.copy(alpha = 0.1f),
                                    shape = app.n_zik.android.uiRoundnessShape()
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    onClick = {
                        setAudioOutput?.invoke(device.id)
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
            VolumeRow(
                currentVolume = currentVolume,
                maxVolume = maxVolume,
                onVolumeChange = { setVolume?.invoke(it) }
            )
        }
    }

    @Composable
    override fun GridMenu() = GridMenu.Menu(title = stringResource(R.string.audio_output_title), showDragHandle = true) {
        if (devices.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BasicText(
                    text = stringResource(R.string.audio_output_no_devices),
                    style = typography().s.copy(
                        color = colorPalette().textSecondary
                    ),
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        } else {
            devices.forEach { device ->
                item {
                    val iconRes = when (device.type) {
                        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> R.drawable.bluetooth
                        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> R.drawable.bluetooth
                        AudioDeviceInfo.TYPE_WIRED_HEADSET -> R.drawable.headphones
                        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> R.drawable.headphones
                        AudioDeviceInfo.TYPE_USB_HEADSET -> R.drawable.headphones
                        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> R.drawable.speaker
                        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> R.drawable.speaker
                        AudioDeviceInfo.TYPE_BUS -> R.drawable.car
                        else -> R.drawable.speaker
                    }

                    GridMenu.Entry(
                        text = device.displayName,
                        icon = {
                            val iconColor = if (device.isCurrentlyActive) colorPalette().accent else colorPalette().text
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        color = iconColor.copy(alpha = 0.1f),
                                        shape = app.n_zik.android.uiRoundnessShape()
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(iconRes),
                                    contentDescription = null,
                                    tint = iconColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        modifier = Modifier.run {
                            if (device.isCurrentlyActive) {
                                background(
                                    color = colorPalette().accent.copy(alpha = 0.2f),
                                    shape = app.n_zik.android.gridMenuShape()
                                )
                            } else this
                        },
                        onClick = {
                            setAudioOutput?.invoke(device.id)
                        }
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(16.dp))
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                VolumeRow(
                    currentVolume = currentVolume,
                    maxVolume = maxVolume,
                    onVolumeChange = { setVolume?.invoke(it) }
                )
            }
        }
    }

    @Composable
    override fun MenuComponent() {
        val context = LocalContext.current
        val binder = app.n_zik.android.LocalPlayerServiceBinder.current
        val player = binder?.player as? androidx.media3.exoplayer.ExoPlayer

        val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
        val audioOutputManager = remember { AudioOutputManager(audioManager) }

        var internalDevices by remember { mutableStateOf(audioOutputManager.getAvailableDevices()) }
        var internalVolume by remember { mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
        val internalMaxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                internalDevices = audioOutputManager.getAvailableDevices()
            }
        }

        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
            }
        }

        DisposableEffect(Unit) {
            val receiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                    if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                        internalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                    }
                }
            }
            context.registerReceiver(receiver, android.content.IntentFilter("android.media.VOLUME_CHANGED_ACTION"))

            audioOutputManager.registerDeviceChanges { updatedDevices ->
                internalDevices = updatedDevices
                internalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
            }
            onDispose {
                context.unregisterReceiver(receiver)
                audioOutputManager.unregisterDeviceChanges()
            }
        }

        devices = internalDevices
        currentVolume = internalVolume
        maxVolume = internalMaxVolume
        setVolume = { vol ->
            internalVolume = vol
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol.roundToInt(), AudioManager.FLAG_SHOW_UI)
        }
        setAudioOutput = { id ->
            audioOutputManager.setAudioOutput(id)
            internalDevices = audioOutputManager.getAvailableDevices()
        }

        when (menuStyle) {
            MenuStyle.List -> ListMenu()
            MenuStyle.Grid -> GridMenu()
        }
    }
}

@Composable
private fun VolumeRow(
    currentVolume: Float,
    maxVolume: Float,
    onVolumeChange: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = colorPalette().accent.copy(alpha = 0.1f),
                    shape = app.n_zik.android.uiRoundnessShape()
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.master_volume),
                contentDescription = null,
                tint = colorPalette().accent,
                modifier = Modifier.size(18.dp)
            )
        }

        BasicText(
            text = stringResource(R.string.volume),
            style = typography().s.copy(
                fontWeight = FontWeight.SemiBold,
                color = colorPalette().text
            )
        )

        BasicText(
            text = "${if (maxVolume > 0) ((currentVolume / maxVolume) * 100).roundToInt() else 0}%",
            style = typography().xxs.copy(
                fontWeight = FontWeight.SemiBold,
                color = colorPalette().accent
            )
        )

        SliderControl(
            state = currentVolume,
            range = 0f..maxVolume,
            stepSize = 0f,
            drawValuePoints = false,
            showValue = false,
            onSlide = onVolumeChange,
            modifier = Modifier.weight(1f)
        )
    }
}
*/
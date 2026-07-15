package app.n_zik.android.playback.services

import android.annotation.SuppressLint
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import timber.log.Timber

class AudioOutputManager(private val audioManager: AudioManager) {

    data class AudioDevice(
        val id: Int,
        val type: Int,
        val isCurrentlyActive: Boolean
    ) {
        val icon: Int
            get() = when (type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                26, 27, 30 -> app.n_zik.android.R.drawable.bluetooth // TYPE_BLE_HEADSET, TYPE_BLE_SPEAKER, TYPE_BLE_BROADCAST
                
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_USB_DEVICE -> app.n_zik.android.R.drawable.headphones
                
                AudioDeviceInfo.TYPE_BUS -> app.n_zik.android.R.drawable.car
                
                else -> app.n_zik.android.R.drawable.devices
            }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var deviceCallback: AudioDeviceCallback? = null

    @SuppressLint("NewApi", "deprecation")
    fun getAvailableDevices(): List<AudioDevice> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return emptyList()

        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .filter { it.isSink && it.type != AudioDeviceInfo.TYPE_TELEPHONY }

        val isA2dpOn = audioManager.isBluetoothA2dpOn
        val isWiredOn = audioManager.isWiredHeadsetOn
        
        val activeRouteId = (if (isA2dpOn) devices.firstOrNull { 
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || 
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (it.type == AudioDeviceInfo.TYPE_BLE_HEADSET || it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER || it.type == AudioDeviceInfo.TYPE_BLE_BROADCAST))
        }?.id else null)
            ?: (if (isWiredOn) devices.firstOrNull { 
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || 
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || 
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET 
            }?.id else null)
            ?: devices.firstOrNull { 
                it.type == AudioDeviceInfo.TYPE_BUS || 
                it.type == AudioDeviceInfo.TYPE_USB_DEVICE 
            }?.id 
            ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }?.id 
            ?: -1

        return devices.map { device ->
            AudioDevice(
                id = device.id,
                type = device.type,
                isCurrentlyActive = device.id == activeRouteId
            )
        }.sortedByDescending { it.isCurrentlyActive }
    }

    @SuppressLint("NewApi")
    fun registerDeviceChanges(callback: (List<AudioDevice>) -> Unit) {
        deviceCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                callback(getAvailableDevices())
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                callback(getAvailableDevices())
            }
        }
        audioManager.registerAudioDeviceCallback(deviceCallback, handler)
    }

    @SuppressLint("NewApi")
    fun unregisterDeviceChanges() {
        deviceCallback?.let { audioManager.unregisterAudioDeviceCallback(it) }
        deviceCallback = null
    }
}

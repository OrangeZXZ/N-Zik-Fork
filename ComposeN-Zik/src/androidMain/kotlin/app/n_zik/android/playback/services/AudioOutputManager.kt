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
        val name: String,
        val type: Int,
        val isCurrentlyActive: Boolean
    ) {
        val displayName: String
            get() = when (type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> name.ifEmpty { "Bluetooth" }
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> name.ifEmpty { "Bluetooth SCO" }
                AudioDeviceInfo.TYPE_WIRED_HEADSET -> name.ifEmpty { "Wired headset" }
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> name.ifEmpty { "Wired headphones" }
                AudioDeviceInfo.TYPE_USB_HEADSET -> name.ifEmpty { "USB headset" }
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Speaker"
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Earpiece"
                else -> name.ifEmpty { "Audio device" }
            }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var deviceCallback: AudioDeviceCallback? = null
    private var onDevicesChanged: ((List<AudioDevice>) -> Unit)? = null

    @SuppressLint("NewApi")
    fun getAvailableDevices(): List<AudioDevice> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return emptyList()

        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val activeRouteId = audioManager.communicationDevice?.id ?: -1

        return devices
            .filter { it.isSink }
            .map { device ->
                AudioDevice(
                    id = device.id,
                    name = device.productName?.toString() ?: "",
                    type = device.type,
                    isCurrentlyActive = device.id == activeRouteId
                )
            }
            .sortedByDescending { it.isCurrentlyActive }
    }

    @SuppressLint("NewApi")
    fun setAudioOutput(deviceId: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false

        val targetDevice = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull { it.id == deviceId } ?: return false

        return try {
            audioManager.setCommunicationDevice(targetDevice)
            Timber.tag("AudioOutputManager").i("Audio routed to device: ${targetDevice.productName}")
            true
        } catch (e: Exception) {
            Timber.tag("AudioOutputManager").e(e, "Failed to route audio to device $deviceId")
            false
        }
    }

    @SuppressLint("NewApi")
    fun clearAudioOutput() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
            Timber.tag("AudioOutputManager").i("Audio routing cleared (default output)")
        }
    }

    @SuppressLint("NewApi")
    fun registerDeviceChanges(callback: (List<AudioDevice>) -> Unit) {
        onDevicesChanged = callback
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
        onDevicesChanged = null
    }
}

package app.n_zik.android.playback.services

import android.annotation.SuppressLint
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.content.Context
import android.media.MediaRouter2
import timber.log.Timber

class AudioOutputManager(private val context: Context, private val audioManager: AudioManager) {

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
    private var playbackCallback: Any? = null // AudioManager.AudioPlaybackCallback
    private var mediaRouter2Callback: Any? = null // MediaRouter2.ControllerCallback

    @SuppressLint("NewApi", "deprecation")
    fun getAvailableDevices(): List<AudioDevice> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return emptyList()

        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .filter { it.isSink && it.type != AudioDeviceInfo.TYPE_TELEPHONY }

        val isA2dpOn = audioManager.isBluetoothA2dpOn
        val isWiredOn = audioManager.isWiredHeadsetOn
        
        var activeRouteId: Int? = null
        var routeSource = "none"

        // Try AudioPlaybackConfiguration (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val configs = audioManager.activePlaybackConfigurations
            val activeConfig = configs.firstOrNull()
            if (activeConfig != null) {
                val deviceInfo = activeConfig.audioDeviceInfo
                if (deviceInfo != null) {
                    activeRouteId = devices.firstOrNull { it.id == deviceInfo.id }?.id
                    if (activeRouteId != null) routeSource = "PlaybackConfig(type=${deviceInfo.type})"
                } else {
                    Timber.d("AudioOutputManager: PlaybackConfig present but audioDeviceInfo is null")
                }
            }
        }

        // Fallback to legacy MediaRouter if no active playback (API 24+)
        val mediaRouterDeviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val mediaRouter = context.getSystemService(Context.MEDIA_ROUTER_SERVICE) as android.media.MediaRouter
            mediaRouter.getSelectedRoute(android.media.MediaRouter.ROUTE_TYPE_LIVE_AUDIO)?.deviceType
        } else null

        if (activeRouteId == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            when (mediaRouterDeviceType) {
                android.media.MediaRouter.RouteInfo.DEVICE_TYPE_BLUETOOTH -> {
                    activeRouteId = devices.firstOrNull { 
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || 
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (it.type == AudioDeviceInfo.TYPE_BLE_HEADSET || it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER || it.type == AudioDeviceInfo.TYPE_BLE_BROADCAST))
                    }?.id
                    if (activeRouteId != null) routeSource = "MediaRouterLegacy(BT)"
                }
                android.media.MediaRouter.RouteInfo.DEVICE_TYPE_SPEAKER -> {
                    activeRouteId = devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }?.id
                    if (activeRouteId != null) routeSource = "MediaRouterLegacy(SPEAKER)"
                }
            }
        }

        if (activeRouteId == null) {
            val a2dpId = if (isA2dpOn) devices.firstOrNull { 
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || 
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (it.type == AudioDeviceInfo.TYPE_BLE_HEADSET || it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER || it.type == AudioDeviceInfo.TYPE_BLE_BROADCAST))
            }?.id else null
            
            if (a2dpId != null) {
                activeRouteId = a2dpId
                routeSource = "Fallback(isA2dpOn)"
            } else {
                val wiredId = if (isWiredOn) devices.firstOrNull { 
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || 
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || 
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET 
                }?.id else null
                
                if (wiredId != null) {
                    activeRouteId = wiredId
                    routeSource = "Fallback(isWiredOn)"
                } else {
                    val busId = devices.firstOrNull { 
                        it.type == AudioDeviceInfo.TYPE_BUS || 
                        it.type == AudioDeviceInfo.TYPE_USB_DEVICE 
                    }?.id
                    
                    if (busId != null) {
                        activeRouteId = busId
                        routeSource = "Fallback(Bus/USB)"
                    } else {
                        val speakerId = devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }?.id
                        if (speakerId != null) {
                            activeRouteId = speakerId
                            routeSource = "Fallback(Speaker)"
                        } else {
                            activeRouteId = -1
                        }
                    }
                }
            }
        }

        var mr2RouteId: String? = null
        var mr2RouteName: String? = null
        var mr2IsSystem = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val mr2 = android.media.MediaRouter2.getInstance(context)
            val mr2Route = mr2.systemController.selectedRoutes.firstOrNull()
            
            mr2RouteId = mr2Route?.id
            mr2RouteName = mr2Route?.name?.toString()
            mr2IsSystem = mr2Route?.isSystemRoute ?: false
            
            Timber.d("AudioOutputManager: MR2 SystemRoute id=$mr2RouteId, name=$mr2RouteName, isSystem=$mr2IsSystem")
            mr2.controllers.forEachIndexed { index, controller ->
                val r = controller.selectedRoutes.firstOrNull()
                Timber.d("AudioOutputManager: MR2 Controller $index - Route id=${r?.id}, name=${r?.name}, isSystem=${r?.isSystemRoute}")
            }

            if (mr2RouteName != null) {
                // Try to match the name with connected devices (e.g. Bluetooth device name)
                val match = devices.firstOrNull { 
                    val prodName = it.productName?.toString() ?: ""
                    prodName.isNotEmpty() && (prodName.equals(mr2RouteName, ignoreCase = true) || mr2RouteName.contains(prodName, ignoreCase = true) || prodName.contains(mr2RouteName, ignoreCase = true))
                }
                
                if (match != null) {
                    activeRouteId = match.id
                    routeSource = "MR2(NameMatch=${match.type})"
                } else if (mr2RouteId?.endsWith("WIRED_HEADSET_ROUTE") == true || (isWiredOn && mr2RouteName.contains("casque", ignoreCase = true))) {
                    activeRouteId = devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES }?.id
                    if (activeRouteId != null) routeSource = "MR2(Wired)"
                } else {
                    // No match for external device. On HyperOS, DEFAULT_ROUTE is used for both Speaker and BT, but the name changes.
                    // If the name didn't match any connected BT/external device, it's the internal speaker/earpiece.
                    activeRouteId = devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER || it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }?.id
                    if (activeRouteId != null) routeSource = "MR2(Unmatched->Speaker)"
                }
            }
        }

        Timber.d("AudioOutputManager: getAvailableDevices() - source=$routeSource, mediaRouterLegacyType=$mediaRouterDeviceType, isA2dpOn=$isA2dpOn, isWiredOn=$isWiredOn, activeRouteId=$activeRouteId")

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
                Timber.d("AudioOutputManager: onAudioDevicesAdded - count=${addedDevices.size}")
                callback(getAvailableDevices())
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                Timber.d("AudioOutputManager: onAudioDevicesRemoved - count=${removedDevices.size}")
                callback(getAvailableDevices())
            }
        }
        audioManager.registerAudioDeviceCallback(deviceCallback, handler)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pbCallback = object : AudioManager.AudioPlaybackCallback() {
                override fun onPlaybackConfigChanged(configs: MutableList<android.media.AudioPlaybackConfiguration>?) {
                    Timber.d("AudioOutputManager: onPlaybackConfigChanged - configs=${configs?.size}")
                    callback(getAvailableDevices())
                }
            }
            playbackCallback = pbCallback
            audioManager.registerAudioPlaybackCallback(pbCallback, handler)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val mr2Callback = object : MediaRouter2.ControllerCallback() {
                override fun onControllerUpdated(controller: MediaRouter2.RoutingController) {
                    Timber.d("AudioOutputManager: onControllerUpdated (MediaRouter2) - selectedRoutes=${controller.selectedRoutes.map { it.type }}")
                    callback(getAvailableDevices())
                }
            }
            mediaRouter2Callback = mr2Callback
            val mr2 = MediaRouter2.getInstance(context)
            mr2.registerControllerCallback(context.mainExecutor, mr2Callback)

            val routeCallback = object : MediaRouter2.RouteCallback() {
                override fun onRoutesAdded(routes: MutableList<android.media.MediaRoute2Info>) { callback(getAvailableDevices()) }
                override fun onRoutesRemoved(routes: MutableList<android.media.MediaRoute2Info>) { callback(getAvailableDevices()) }
                override fun onRoutesChanged(routes: MutableList<android.media.MediaRoute2Info>) { callback(getAvailableDevices()) }
            }
            mediaRouter2RouteCallback = routeCallback
            mr2.registerRouteCallback(
                context.mainExecutor,
                routeCallback,
                android.media.RouteDiscoveryPreference.Builder(listOf(android.media.MediaRoute2Info.FEATURE_LIVE_AUDIO), true).build()
            )
        }
    }

    private var mediaRouter2RouteCallback: Any? = null

    @SuppressLint("NewApi")
    fun unregisterDeviceChanges() {
        deviceCallback?.let { audioManager.unregisterAudioDeviceCallback(it) }
        deviceCallback = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (playbackCallback as? AudioManager.AudioPlaybackCallback)?.let {
                audioManager.unregisterAudioPlaybackCallback(it)
            }
            playbackCallback = null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val mr2 = MediaRouter2.getInstance(context)
            (mediaRouter2Callback as? MediaRouter2.ControllerCallback)?.let {
                mr2.unregisterControllerCallback(it)
            }
            mediaRouter2Callback = null

            (mediaRouter2RouteCallback as? MediaRouter2.RouteCallback)?.let {
                mr2.unregisterRouteCallback(it)
            }
            mediaRouter2RouteCallback = null
        }
    }
}
